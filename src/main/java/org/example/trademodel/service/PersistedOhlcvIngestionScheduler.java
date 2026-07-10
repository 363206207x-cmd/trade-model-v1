package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionHealth;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
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
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PersistedOhlcvIngestionScheduler {
    private static final Logger log = LoggerFactory.getLogger(PersistedOhlcvIngestionScheduler.class);
    private static final Set<String> PRODUCT_TIMEFRAMES = Set.of("5m", "15m", "1h", "4h");
    private static final int MAX_SYMBOLS = 2;

    private final PublicOhlcvProvider provider;
    private final PersistedOhlcvIngestionService ingestionService;
    private final boolean globalSchedulersEnabled;
    private final boolean schedulerEnabled;
    private final List<String> symbols;
    private final List<String> timeframes;
    private final int barLimit;
    private final Set<String> activeKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, OhlcvIngestionHealth> health = new ConcurrentHashMap<>();

    public PersistedOhlcvIngestionScheduler(
            PublicOhlcvProvider provider,
            PersistedOhlcvIngestionService ingestionService,
            @Value("${trade-model.schedulers.enabled:true}") boolean globalSchedulersEnabled,
            @Value("${trade-model.schedulers.ohlcv-ingestion.enabled:false}") boolean schedulerEnabled,
            @Value("${trade-model.schedulers.ohlcv-ingestion.symbols:}") String symbols,
            @Value("${trade-model.schedulers.ohlcv-ingestion.timeframes:5m,15m,1h,4h}") String timeframes,
            @Value("${trade-model.schedulers.ohlcv-ingestion.bar-limit:100}") int barLimit
    ) {
        this.provider = provider;
        this.ingestionService = ingestionService;
        this.globalSchedulersEnabled = globalSchedulersEnabled;
        this.schedulerEnabled = schedulerEnabled;
        this.symbols = csv(symbols, true);
        this.timeframes = csv(timeframes, false);
        this.barLimit = Math.min(Math.max(barLimit, 1), 500);
    }

    @Scheduled(
            initialDelayString = "${trade-model.schedulers.ohlcv-ingestion.initial-delay-ms:60000}",
            fixedDelayString = "${trade-model.schedulers.ohlcv-ingestion.fixed-delay-ms:60000}")
    public void ingestScheduled() {
        if (!globalSchedulersEnabled || !schedulerEnabled) {
            return;
        }
        if (symbols.isEmpty() || symbols.size() > MAX_SYMBOLS || !PRODUCT_TIMEFRAMES.equals(Set.copyOf(timeframes))) {
            log.error("OHLCV ingestion schedule rejected: allowlist must contain 1-2 symbols and exactly product timeframes");
            return;
        }
        for (String symbol : symbols) {
            for (String timeframe : timeframes) {
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
            OhlcvIngestionResult result = ingestionService.ingest(fetched.batch());
            if (result.ready()) {
                Instant now = Instant.now();
                OhlcvIngestionHealth previous = health.get(key);
                health.put(key, new OhlcvIngestionHealth(normalizedSymbol, timeframe, result.sourceState(), now,
                        previous == null ? null : previous.lastFailureAt(), null, OhlcvSourceState.WAITING_SYNC));
            } else {
                String reason = result.reasonCodes().isEmpty() ? "INGESTION_NOT_READY" : result.reasonCodes().get(0);
                updateFailure(key, normalizedSymbol, timeframe, result.sourceState(), reason);
            }
            return result;
        } finally {
            activeKeys.remove(key);
        }
    }

    public OhlcvIngestionHealth health(String symbol, String timeframe) {
        String key = (symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT)) + "|" + timeframe;
        return health.getOrDefault(key, new OhlcvIngestionHealth(symbol, timeframe,
                OhlcvSourceState.WAITING_SYNC, null, null, null, OhlcvSourceState.WAITING_SYNC));
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
