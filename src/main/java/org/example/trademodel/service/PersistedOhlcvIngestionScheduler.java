package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionHealth;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.market.util.BinanceUsdtSymbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PersistedOhlcvIngestionScheduler {
    private static final Logger log = LoggerFactory.getLogger(PersistedOhlcvIngestionScheduler.class);
    private static final Set<String> PRODUCT_TIMEFRAMES = Set.of("5m", "15m", "1h", "4h");

    private final PublicOhlcvProvider provider;
    private final PersistedOhlcvIngestionService ingestionService;
    private final boolean globalSchedulersEnabled;
    private final boolean schedulerEnabled;
    private final List<String> timeframes;
    private final int barLimit;
    private final int maxSymbols;
    private final int retryBaseSeconds;
    private final int retryMaxSeconds;
    private AssetPoolService assetPoolService;
    private UserPositionMapper userPositionMapper;
    private final AtomicInteger nextSymbolOffset = new AtomicInteger();
    private final Set<String> activeKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, OhlcvIngestionHealth> health = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();
    private final Map<String, Instant> retryAfter = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public PersistedOhlcvIngestionScheduler(
            PublicOhlcvProvider provider,
            PersistedOhlcvIngestionService ingestionService,
            @Value("${trade-model.schedulers.enabled:false}") boolean globalSchedulersEnabled,
            @Value("${trade-model.schedulers.ohlcv-ingestion.enabled:false}") boolean schedulerEnabled,
            @Value("${trade-model.schedulers.ohlcv-ingestion.symbols:}") String symbols,
            @Value("${trade-model.schedulers.ohlcv-ingestion.timeframes:5m,15m,1h,4h}") String timeframes,
            @Value("${trade-model.schedulers.ohlcv-ingestion.bar-limit:100}") int barLimit,
            @Value("${trade-model.schedulers.ohlcv-ingestion.max-symbols:2}") int maxSymbols,
            @Value("${trade-model.schedulers.ohlcv-ingestion.retry-base-seconds:15}") int retryBaseSeconds,
            @Value("${trade-model.schedulers.ohlcv-ingestion.retry-max-seconds:300}") int retryMaxSeconds
    ) {
        this.provider = provider;
        this.ingestionService = ingestionService;
        this.globalSchedulersEnabled = globalSchedulersEnabled;
        this.schedulerEnabled = schedulerEnabled;
        this.timeframes = csv(timeframes, false);
        this.barLimit = Math.min(Math.max(barLimit, 1), 500);
        this.maxSymbols = Math.min(Math.max(maxSymbols, 1), 20);
        this.retryBaseSeconds = Math.max(1, retryBaseSeconds);
        this.retryMaxSeconds = Math.max(this.retryBaseSeconds, retryMaxSeconds);
    }

    public PersistedOhlcvIngestionScheduler(PublicOhlcvProvider provider,
                                            PersistedOhlcvIngestionService ingestionService,
                                            boolean globalSchedulersEnabled,
                                            boolean schedulerEnabled,
                                            String symbols,
                                            String timeframes,
                                            int barLimit,
                                            int maxSymbols) {
        this(provider, ingestionService, globalSchedulersEnabled, schedulerEnabled, symbols, timeframes,
                barLimit, maxSymbols, 15, 300);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAssetPoolService(AssetPoolService assetPoolService) {
        this.assetPoolService = assetPoolService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setUserPositionMapper(UserPositionMapper userPositionMapper) {
        this.userPositionMapper = userPositionMapper;
    }

    @Scheduled(
            initialDelayString = "${trade-model.schedulers.ohlcv-ingestion.initial-delay-ms:60000}",
            fixedDelayString = "${trade-model.schedulers.ohlcv-ingestion.fixed-delay-ms:60000}")
    public void ingestScheduled() {
        if (!globalSchedulersEnabled || !schedulerEnabled) {
            return;
        }
        List<String> scheduledSymbols = scheduledSymbols();
        if (scheduledSymbols.isEmpty() || !PRODUCT_TIMEFRAMES.equals(Set.copyOf(timeframes))) {
            log.error("OHLCV ingestion schedule rejected: Asset Pool must be non-empty and timeframes must match product contract");
            return;
        }
        for (String symbol : scheduledSymbols) {
            for (String timeframe : timeframes) {
                if (!retryEligible(symbol, timeframe)) continue;
                ingestOne(symbol, timeframe);
            }
        }
    }

    public OhlcvIngestionResult ingestOne(String symbol, String timeframe) {
        String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        String key = normalizedSymbol + "|" + timeframe;
        if (!activeKeys.add(key)) {
            updateFailure(key, normalizedSymbol, timeframe, OhlcvSourceState.WAITING_SYNC, "INGESTION_ALREADY_RUNNING");
            return new OhlcvIngestionResult(OhlcvSourceState.WAITING_SYNC, null, 0, 0, 0,
                    List.of("INGESTION_ALREADY_RUNNING"));
        }
        try {
            String runId = "ohlcv-" + UUID.randomUUID();
            PublicOhlcvProviderResult fetched = provider.fetchClosedBars(normalizedSymbol, timeframe, barLimit, runId);
            if (fetched == null || fetched.sourceState() != OhlcvSourceState.READY || fetched.batch() == null) {
                OhlcvSourceState state = fetched == null ? OhlcvSourceState.ERROR : fetched.sourceState();
                String reason = fetched == null ? "PROVIDER_RESULT_MISSING" : fetched.reasonCode();
                updateFailure(key, normalizedSymbol, timeframe, state, reason);
                return new OhlcvIngestionResult(state, null, 0, 0, 0,
                        reason == null ? List.of() : List.of(reason));
            }
            if (!trustedBinanceBatch(fetched.batch(), normalizedSymbol, timeframe)) {
                updateFailure(key, normalizedSymbol, timeframe, OhlcvSourceState.ERROR,
                        "BINANCE_SOURCE_OWNERSHIP_REQUIRED");
                return new OhlcvIngestionResult(OhlcvSourceState.ERROR, null, 0, 0, 0,
                        List.of("BINANCE_SOURCE_OWNERSHIP_REQUIRED"));
            }
            OhlcvIngestionResult result = ingestionService.ingest(fetched.batch());
            if (result.ready()) {
                Instant now = Instant.now();
                OhlcvIngestionHealth previous = health.get(key);
                health.put(key, new OhlcvIngestionHealth(normalizedSymbol, timeframe, result.sourceState(), now,
                        previous == null ? null : previous.lastFailureAt(), null, OhlcvSourceState.WAITING_SYNC));
                consecutiveFailures.remove(key);
                retryAfter.remove(key);
            } else {
                String reason = result.reasonCodes().isEmpty() ? "INGESTION_NOT_READY" : result.reasonCodes().get(0);
                updateFailure(key, normalizedSymbol, timeframe, result.sourceState(), reason);
            }
            return result;
        } catch (RuntimeException failure) {
            String reason = "PROVIDER_EXCEPTION:" + failure.getClass().getSimpleName();
            updateFailure(key, normalizedSymbol, timeframe, OhlcvSourceState.ERROR, reason);
            log.warn("OHLCV ingestion failed for symbol={} timeframe={} failureType={}",
                    normalizedSymbol, timeframe, failure.getClass().getSimpleName());
            return new OhlcvIngestionResult(OhlcvSourceState.ERROR, null, 0, 0, 0, List.of(reason));
        } finally {
            activeKeys.remove(key);
        }
    }

    public OhlcvIngestionHealth health(String symbol, String timeframe) {
        String key = (symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT)) + "|" + timeframe;
        return health.getOrDefault(key, new OhlcvIngestionHealth(symbol, timeframe,
                OhlcvSourceState.WAITING_SYNC, null, null, null, OhlcvSourceState.WAITING_SYNC));
    }

    private List<String> scheduledSymbols() {
        List<String> poolSymbols = assetPoolService == null ? List.of() : assetPoolService.listScanSymbols();
        LinkedHashSet<String> coverage = new LinkedHashSet<>();
        addSymbols(coverage, poolSymbols);
        if (userPositionMapper != null) {
            List<UserPositionDO> positions = userPositionMapper.listClaimedOpenForSystemMonitoring();
            if (positions != null) {
                addSymbols(coverage, positions.stream()
                        .map(UserPositionDO::getAssetSymbol).toList());
            }
        }
        List<String> normalized = List.copyOf(coverage);
        if (normalized.size() <= maxSymbols) {
            return normalized;
        }
        int start = Math.floorMod(nextSymbolOffset.getAndAdd(maxSymbols), normalized.size());
        java.util.ArrayList<String> batch = new java.util.ArrayList<>(maxSymbols);
        for (int index = 0; index < maxSymbols; index++) {
            batch.add(normalized.get((start + index) % normalized.size()));
        }
        return List.copyOf(batch);
    }

    private void updateFailure(
            String key,
            String symbol,
            String timeframe,
            OhlcvSourceState state,
            String reason
    ) {
        OhlcvIngestionHealth previous = health.get(key);
        health.put(key, new OhlcvIngestionHealth(symbol, timeframe, state,
                previous == null ? null : previous.lastSuccessAt(), Instant.now(), reason,
                OhlcvSourceState.WAITING_SYNC));
        int failures = consecutiveFailures.merge(key, 1, Integer::sum);
        long multiplier = 1L << Math.min(10, Math.max(0, failures - 1));
        long delay = Math.min(retryMaxSeconds, retryBaseSeconds * multiplier);
        retryAfter.put(key, Instant.now().plusSeconds(delay));
    }

    private boolean retryEligible(String symbol, String timeframe) {
        Instant blockedUntil = retryAfter.get(symbol + "|" + timeframe);
        return blockedUntil == null || !Instant.now().isBefore(blockedUntil);
    }

    private static boolean trustedBinanceBatch(OhlcvIngestionBatch batch,
                                               String symbol,
                                               String timeframe) {
        return batch != null
                && "BINANCE_PUBLIC".equalsIgnoreCase(batch.provider())
                && "SPOT".equalsIgnoreCase(batch.providerMarketType())
                && batch.sourceState() == OhlcvSourceState.READY
                && batch.bars() != null && !batch.bars().isEmpty()
                && batch.bars().stream().allMatch(bar -> bar != null && bar.closed()
                && symbol.equalsIgnoreCase(bar.symbol()) && timeframe.equals(bar.timeframe()));
    }

    private static void addSymbols(Set<String> target, List<String> values) {
        if (values == null) return;
        values.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(BinanceUsdtSymbol::toUsdtPair)
                .forEach(target::add);
    }

    private static List<String> csv(String raw, boolean uppercase) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> uppercase ? value.toUpperCase(Locale.ROOT) : value)
                .distinct()
                .toList();
    }
}
