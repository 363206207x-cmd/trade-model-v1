package org.example.trademodel.service.impl;

import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvFreshnessStatus;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PersistedOhlcvIngestionServiceImpl implements PersistedOhlcvIngestionService {
    private static final Set<String> SUPPORTED_TIMEFRAMES = Set.of("5m", "15m", "1h", "4h");
    private static final String QUALITY_OK = "OK";

    private final PersistedOhlcvBarMapper mapper;
    private final Clock clock;
    private final long futureToleranceMs;
    private final long freshnessToleranceMs;

    @Autowired
    public PersistedOhlcvIngestionServiceImpl(
            PersistedOhlcvBarMapper mapper,
            @Value("${trade-model.ohlcv.future-tolerance-ms:30000}") long futureToleranceMs,
            @Value("${trade-model.ohlcv.freshness-tolerance-ms:30000}") long freshnessToleranceMs
    ) {
        this(mapper, Clock.systemUTC(), futureToleranceMs, freshnessToleranceMs);
    }

    PersistedOhlcvIngestionServiceImpl(
            PersistedOhlcvBarMapper mapper,
            Clock clock,
            long futureToleranceMs,
            long freshnessToleranceMs
    ) {
        this.mapper = mapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.futureToleranceMs = Math.max(0L, futureToleranceMs);
        this.freshnessToleranceMs = Math.max(0L, freshnessToleranceMs);
    }

    @Override
    @Transactional
    public OhlcvIngestionResult ingest(OhlcvIngestionBatch batch) {
        List<String> batchReasons = validateBatch(batch);
        int requested = batch == null ? 0 : batch.bars().size();
        if (!batchReasons.isEmpty()) {
            OhlcvSourceState state = batch == null || batch.sourceState() == null
                    ? OhlcvSourceState.ERROR : batch.sourceState();
            return rejected(state, requested, batchReasons);
        }
        if (batch.sourceState() != OhlcvSourceState.READY) {
            return rejected(batch.sourceState(), requested, List.of("SOURCE_NOT_READY"));
        }

        OhlcvFreshnessStatus freshness = freshness(batch);
        List<PersistedOhlcvBarDO> candidates = new ArrayList<>();
        List<String> validationReasons = new ArrayList<>();
        Map<String, String> batchHashes = new HashMap<>();
        for (OhlcvBarInput input : batch.bars()) {
            validateBar(input, batch.fetchTime().toEpochMilli(), validationReasons);
            if (!validationReasons.isEmpty()) {
                continue;
            }
            PersistedOhlcvBarDO row = toRow(batch, input, freshness);
            String key = sourceKey(row);
            String previousHash = batchHashes.putIfAbsent(key, row.getRawPayloadHash());
            if (previousHash != null && !previousHash.equals(row.getRawPayloadHash())) {
                validationReasons.add("CONFLICTING_DUPLICATE_IN_BATCH");
            } else if (previousHash == null) {
                candidates.add(row);
            }
        }
        if (!validationReasons.isEmpty()) {
            return rejected(OhlcvSourceState.DEGRADED, requested, dedupe(validationReasons));
        }

        List<PersistedOhlcvBarDO> inserts = new ArrayList<>();
        int idempotentCount = 0;
        for (PersistedOhlcvBarDO candidate : candidates) {
            PersistedOhlcvBarDO existing = mapper.selectBySourceKey(
                    candidate.getSymbol(), candidate.getTimeframe(), candidate.getOpenTimeMs(),
                    candidate.getProvider(), candidate.getProviderMarketType());
            if (existing == null) {
                inserts.add(candidate);
                continue;
            }
            if (!contentHash(existing).equals(candidate.getRawPayloadHash())) {
                return rejected(OhlcvSourceState.DEGRADED, requested,
                        List.of("CONFLICTING_DUPLICATE_CONTENT"));
            }
            idempotentCount++;
        }

        for (PersistedOhlcvBarDO row : inserts) {
            mapper.insert(row);
        }
        OhlcvSourceState state = freshness == OhlcvFreshnessStatus.FRESH
                ? OhlcvSourceState.READY : OhlcvSourceState.STALE;
        List<String> reasons = freshness == OhlcvFreshnessStatus.FRESH
                ? List.of() : List.of("LATEST_CLOSED_BAR_STALE");
        return new OhlcvIngestionResult(state, freshness, inserts.size(), idempotentCount, 0, reasons);
    }

    private List<String> validateBatch(OhlcvIngestionBatch batch) {
        List<String> reasons = new ArrayList<>();
        if (batch == null) {
            reasons.add("BATCH_MISSING");
            return reasons;
        }
        addBlank(batch.provider(), "PROVIDER_MISSING", reasons);
        addBlank(batch.providerMarketType(), "PROVIDER_MARKET_TYPE_MISSING", reasons);
        addBlank(batch.sourceEndpoint(), "SOURCE_ENDPOINT_MISSING", reasons);
        addBlank(batch.provenanceVersion(), "PROVENANCE_VERSION_MISSING", reasons);
        addBlank(batch.traceId(), "TRACE_ID_MISSING", reasons);
        addBlank(batch.ingestionRunId(), "INGESTION_RUN_ID_MISSING", reasons);
        if (batch.fetchTime() == null) {
            reasons.add("FETCH_TIME_MISSING");
        } else if (batch.fetchTime().toEpochMilli() > clock.millis() + futureToleranceMs) {
            reasons.add("FETCH_TIME_IN_FUTURE");
        }
        if (batch.sourceState() == null) {
            reasons.add("SOURCE_STATUS_MISSING");
        }
        if (batch.sourceVersion() <= 0) {
            reasons.add("SOURCE_VERSION_INVALID");
        }
        if (batch.bars().isEmpty()) {
            reasons.add("BARS_EMPTY");
        }
        return reasons;
    }

    private void validateBar(OhlcvBarInput bar, long fetchTimeMs, List<String> reasons) {
        if (bar == null) {
            reasons.add("BAR_MISSING");
            return;
        }
        addBlank(bar.symbol(), "SYMBOL_MISSING", reasons);
        if (!SUPPORTED_TIMEFRAMES.contains(bar.timeframe())) {
            reasons.add("TIMEFRAME_UNSUPPORTED");
        }
        if (bar.openTimeMs() < 0 || bar.closeTimeMs() <= bar.openTimeMs()) {
            reasons.add("TIMESTAMP_ORDER_INVALID");
        }
        if (bar.openTimeMs() > clock.millis() + futureToleranceMs
                || bar.closeTimeMs() > clock.millis() + futureToleranceMs
                || bar.closeTimeMs() > fetchTimeMs + futureToleranceMs) {
            reasons.add("BAR_IN_FUTURE");
        }
        if (!bar.closed()) {
            reasons.add("BAR_NOT_CLOSED");
        }
        if (!positive(bar.open()) || !positive(bar.high()) || !positive(bar.low()) || !positive(bar.close())) {
            reasons.add("PRICE_NON_POSITIVE");
        } else if (bar.high().compareTo(bar.low()) < 0
                || bar.high().compareTo(bar.open()) < 0
                || bar.high().compareTo(bar.close()) < 0
                || bar.low().compareTo(bar.open()) > 0
                || bar.low().compareTo(bar.close()) > 0) {
            reasons.add("OHLC_GEOMETRY_INVALID");
        }
        if (bar.volume() == null || bar.volume().compareTo(BigDecimal.ZERO) < 0) {
            reasons.add("VOLUME_INVALID");
        }
    }

    private OhlcvFreshnessStatus freshness(OhlcvIngestionBatch batch) {
        long latestClose = batch.bars().stream().mapToLong(OhlcvBarInput::closeTimeMs).max().orElse(0L);
        String timeframe = batch.bars().get(0).timeframe();
        long maxAgeMs = timeframeMs(timeframe) + freshnessToleranceMs;
        return batch.fetchTime().toEpochMilli() - latestClose <= maxAgeMs
                ? OhlcvFreshnessStatus.FRESH : OhlcvFreshnessStatus.STALE;
    }

    private PersistedOhlcvBarDO toRow(
            OhlcvIngestionBatch batch,
            OhlcvBarInput input,
            OhlcvFreshnessStatus freshness
    ) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        PersistedOhlcvBarDO row = new PersistedOhlcvBarDO();
        row.setSymbol(input.symbol().trim().toUpperCase(Locale.ROOT));
        row.setTimeframe(input.timeframe());
        row.setOpenTimeMs(input.openTimeMs());
        row.setCloseTimeMs(input.closeTimeMs());
        row.setOpenPrice(input.open());
        row.setHighPrice(input.high());
        row.setLowPrice(input.low());
        row.setClosePrice(input.close());
        row.setVolume(input.volume());
        row.setQuoteVolume(input.quoteVolume());
        row.setTradeCount(input.tradeCount());
        row.setTakerBuyBaseVolume(input.takerBuyBaseVolume());
        row.setTakerBuyQuoteVolume(input.takerBuyQuoteVolume());
        row.setClosed(input.closed());
        row.setProvider(batch.provider().trim().toUpperCase(Locale.ROOT));
        row.setProviderMarketType(batch.providerMarketType().trim().toUpperCase(Locale.ROOT));
        row.setSourceEndpoint(batch.sourceEndpoint().trim());
        row.setSourceBatchId(batch.ingestionRunId().trim());
        row.setSourceTraceId(batch.traceId().trim());
        row.setSourceVersion(batch.sourceVersion());
        row.setFetchTime(LocalDateTime.ofInstant(batch.fetchTime(), ZoneOffset.UTC));
        row.setSourceStatus(OhlcvSourceState.READY.name());
        row.setFreshnessStatus(freshness.name());
        row.setProvenanceVersion(batch.provenanceVersion().trim());
        row.setIngestionRunId(batch.ingestionRunId().trim());
        row.setIngestedAt(now);
        row.setUpdatedAt(now);
        row.setQualityStatus(QUALITY_OK);
        row.setIsDeleted(0);
        row.setRawPayloadHash(contentHash(row));
        return row;
    }

    private String contentHash(PersistedOhlcvBarDO row) {
        String canonical = String.join("|",
                safe(row.getSymbol()), safe(row.getTimeframe()), String.valueOf(row.getOpenTimeMs()),
                String.valueOf(row.getCloseTimeMs()), decimal(row.getOpenPrice()), decimal(row.getHighPrice()),
                decimal(row.getLowPrice()), decimal(row.getClosePrice()), decimal(row.getVolume()),
                decimal(row.getQuoteVolume()), String.valueOf(row.getTradeCount()),
                decimal(row.getTakerBuyBaseVolume()), decimal(row.getTakerBuyQuoteVolume()),
                String.valueOf(row.getClosed()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String sourceKey(PersistedOhlcvBarDO row) {
        return row.getProvider() + "|" + row.getSymbol() + "|" + row.getTimeframe() + "|"
                + row.getOpenTimeMs() + "|" + row.getProviderMarketType();
    }

    private static OhlcvIngestionResult rejected(OhlcvSourceState state, int requested, List<String> reasons) {
        return new OhlcvIngestionResult(state, null, 0, 0, requested, reasons);
    }

    private static List<String> dedupe(List<String> reasons) {
        return reasons.stream().distinct().toList();
    }

    private static void addBlank(String value, String reason, List<String> reasons) {
        if (value == null || value.isBlank()) {
            reasons.add(reason);
        }
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static long timeframeMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 5L * 60_000L;
            case "15m" -> 15L * 60_000L;
            case "1h" -> 60L * 60_000L;
            case "4h" -> 4L * 60L * 60_000L;
            default -> Long.MAX_VALUE;
        };
    }
}
