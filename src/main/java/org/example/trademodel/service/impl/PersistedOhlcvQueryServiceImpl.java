package org.example.trademodel.service.impl;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
public class PersistedOhlcvQueryServiceImpl implements PersistedOhlcvQueryService {
    private static final String QUALITY_OK = "OK";

    private final PersistedOhlcvBarMapper persistedOhlcvBarMapper;
    private final Clock clock;

    @Autowired
    public PersistedOhlcvQueryServiceImpl(PersistedOhlcvBarMapper persistedOhlcvBarMapper) {
        this(persistedOhlcvBarMapper, Clock.systemUTC());
    }

    PersistedOhlcvQueryServiceImpl(PersistedOhlcvBarMapper persistedOhlcvBarMapper, Clock clock) {
        this.persistedOhlcvBarMapper = persistedOhlcvBarMapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public PersistedOhlcvReadinessResult evaluateReadiness(
            String symbol,
            String timeframe,
            int requiredWindowSize,
            long maxReadLagMs
    ) {
        PersistedOhlcvReadinessResult result = baseResult(symbol, timeframe, requiredWindowSize);
        List<String> missingFields = new ArrayList<>();
        if (!hasText(symbol)) {
            missingFields.add("symbol");
        }
        if (!hasText(timeframe)) {
            missingFields.add("timeframe");
        }
        if (requiredWindowSize <= 0) {
            missingFields.add("requiredWindowSize");
        }
        if (maxReadLagMs <= 0) {
            missingFields.add("maxReadLagMs");
        }
        Long intervalMs = parseTimeframeMs(timeframe);
        if (intervalMs == null) {
            missingFields.add("timeframePolicy");
        }
        if (!missingFields.isEmpty()) {
            return finish(result, PersistedOhlcvReadinessStatus.UNKNOWN,
                    PersistedOhlcvStaleReasonCode.POLICY_NOT_CONFIGURED,
                    "Persisted OHLCV readiness policy is not fully configured.", missingFields, List.of());
        }

        List<PersistedOhlcvBarDO> bars = persistedOhlcvBarMapper
                .selectLatestClosedWindow(symbol, timeframe, requiredWindowSize);
        result.setBars(bars);
        if (bars == null || bars.isEmpty()) {
            return finish(result, PersistedOhlcvReadinessStatus.MISSING,
                    PersistedOhlcvStaleReasonCode.NO_BARS_FOR_SYMBOL_TIMEFRAME,
                    "No closed persisted OHLCV bars exist for symbol/timeframe.",
                    List.of("persistedOhlcvWindow", "klineItems"), bars);
        }

        PersistedOhlcvBarDO latest = bars.get(0);
        result.setLatestCloseTimeMs(latest.getCloseTimeMs());
        result.setLatestIngestedAt(latest.getIngestedAt());

        if (bars.size() < requiredWindowSize) {
            return finish(result, PersistedOhlcvReadinessStatus.PARTIAL,
                    PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT,
                    "Persisted OHLCV window is shorter than required.",
                    List.of("persistedOhlcvWindow", "requiredClosedBars"), bars);
        }

        List<String> ownershipMissing = missingOwnershipFields(bars);
        if (!ownershipMissing.isEmpty()) {
            return finish(result, PersistedOhlcvReadinessStatus.UNKNOWN,
                    PersistedOhlcvStaleReasonCode.SOURCE_OWNER_MISSING,
                    "Persisted OHLCV source ownership is incomplete.", ownershipMissing, bars);
        }

        if (bars.stream().anyMatch(bar -> !"READY".equals(bar.getSourceStatus()))) {
            return finish(result, PersistedOhlcvReadinessStatus.INVALID,
                    PersistedOhlcvStaleReasonCode.SOURCE_STATUS_NOT_READY,
                    "Persisted OHLCV source status is not READY.", List.of("sourceStatus"), bars);
        }

        if (bars.stream().anyMatch(bar -> !"FRESH".equals(bar.getFreshnessStatus()))) {
            return finish(result, PersistedOhlcvReadinessStatus.STALE,
                    PersistedOhlcvStaleReasonCode.PERSISTED_FRESHNESS_STATUS_STALE,
                    "Persisted OHLCV was stale when ingested.", List.of("freshnessStatus"), bars);
        }

        List<String> qualityMissing = nonOkQualityFields(bars);
        if (!qualityMissing.isEmpty()) {
            return finish(result, PersistedOhlcvReadinessStatus.INVALID,
                    PersistedOhlcvStaleReasonCode.QUALITY_STATUS_NOT_OK,
                    "One or more persisted OHLCV bars has non-OK quality status.", qualityMissing, bars);
        }

        List<String> priceMissing = invalidPriceFields(bars);
        if (!priceMissing.isEmpty()) {
            return finish(result, PersistedOhlcvReadinessStatus.INVALID,
                    PersistedOhlcvStaleReasonCode.PRICE_FIELD_INVALID,
                    "One or more persisted OHLCV bars has invalid OHLC price fields.", priceMissing, bars);
        }

        List<String> volumeMissing = invalidVolumeFields(bars);
        if (!volumeMissing.isEmpty()) {
            return finish(result, PersistedOhlcvReadinessStatus.INVALID,
                    PersistedOhlcvStaleReasonCode.VOLUME_FIELD_MISSING,
                    "One or more persisted OHLCV bars has missing or invalid volume.", volumeMissing, bars);
        }

        if (bars.stream().anyMatch(this::invalidTimestampOrder)) {
            return finish(result, PersistedOhlcvReadinessStatus.INVALID,
                    PersistedOhlcvStaleReasonCode.TIMESTAMP_ORDER_INVALID,
                    "One or more persisted OHLCV bars has invalid timestamp order.",
                    List.of("timestampOrder"), bars);
        }

        if (!isContiguous(bars, intervalMs)) {
            return finish(result, PersistedOhlcvReadinessStatus.PARTIAL,
                    PersistedOhlcvStaleReasonCode.WINDOW_NOT_CONTIGUOUS,
                    "Persisted OHLCV window is not contiguous.", List.of("klineWindow"), bars);
        }

        long nowMs = clock.millis();
        if (latest.getCloseTimeMs() == null || nowMs - latest.getCloseTimeMs() > maxReadLagMs) {
            return finish(result, PersistedOhlcvReadinessStatus.STALE,
                    PersistedOhlcvStaleReasonCode.LATEST_BAR_TOO_OLD,
                    "Latest persisted OHLCV bar is older than freshness policy.", List.of("klineFreshness"), bars);
        }

        return finish(result, PersistedOhlcvReadinessStatus.FRESH,
                PersistedOhlcvStaleReasonCode.NONE,
                "Persisted OHLCV window is fresh, closed, contiguous, source-owned, and quality OK.",
                List.of(), bars);
    }

    private PersistedOhlcvReadinessResult baseResult(String symbol, String timeframe, int requiredWindowSize) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setSymbol(symbol);
        result.setTimeframe(timeframe);
        result.setRequiredWindowSize(requiredWindowSize);
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }

    private PersistedOhlcvReadinessResult finish(
            PersistedOhlcvReadinessResult result,
            PersistedOhlcvReadinessStatus status,
            PersistedOhlcvStaleReasonCode reasonCode,
            String reasonText,
            List<String> missingFields,
            List<PersistedOhlcvBarDO> bars
    ) {
        result.setStatus(status);
        result.setStaleReasonCode(reasonCode);
        result.setStaleReasonText(reasonText);
        result.setMissingFields(missingFields);
        result.setBars(bars);
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }

    private boolean isContiguous(List<PersistedOhlcvBarDO> bars, long intervalMs) {
        for (int i = 0; i < bars.size() - 1; i++) {
            PersistedOhlcvBarDO newer = bars.get(i);
            PersistedOhlcvBarDO older = bars.get(i + 1);
            if (newer.getOpenTimeMs() == null || older.getOpenTimeMs() == null) {
                return false;
            }
            if (newer.getOpenTimeMs() - older.getOpenTimeMs() != intervalMs) {
                return false;
            }
        }
        return true;
    }

    private List<String> missingOwnershipFields(List<PersistedOhlcvBarDO> bars) {
        List<String> fields = new ArrayList<>();
        for (PersistedOhlcvBarDO bar : bars) {
            addWhenBlank(bar.getProvider(), "provider", fields);
            addWhenBlank(bar.getProviderMarketType(), "providerMarketType", fields);
            addWhenBlank(bar.getSourceEndpoint(), "sourceEndpoint", fields);
            addWhenBlank(bar.getSourceBatchId(), "sourceBatchId", fields);
            addWhenBlank(bar.getSourceTraceId(), "sourceTraceId", fields);
            addWhenNull(bar.getSourceVersion(), "sourceVersion", fields);
            addWhenNull(bar.getFetchTime(), "fetchTime", fields);
            addWhenBlank(bar.getSourceStatus(), "sourceStatus", fields);
            addWhenBlank(bar.getFreshnessStatus(), "freshnessStatus", fields);
            addWhenBlank(bar.getProvenanceVersion(), "provenanceVersion", fields);
            addWhenBlank(bar.getIngestionRunId(), "ingestionRunId", fields);
            addWhenNull(bar.getIngestedAt(), "ingestedAt", fields);
        }
        return fields;
    }

    private List<String> nonOkQualityFields(List<PersistedOhlcvBarDO> bars) {
        List<String> fields = new ArrayList<>();
        for (PersistedOhlcvBarDO bar : bars) {
            if (!QUALITY_OK.equals(bar.getQualityStatus())) {
                addUnique("qualityStatus", fields);
            }
        }
        return fields;
    }

    private List<String> invalidPriceFields(List<PersistedOhlcvBarDO> bars) {
        List<String> fields = new ArrayList<>();
        for (PersistedOhlcvBarDO bar : bars) {
            addWhenNonPositive(bar.getOpenPrice(), "openPrice", fields);
            addWhenNonPositive(bar.getHighPrice(), "highPrice", fields);
            addWhenNonPositive(bar.getLowPrice(), "lowPrice", fields);
            addWhenNonPositive(bar.getClosePrice(), "closePrice", fields);
            if (bar.getHighPrice() != null && bar.getLowPrice() != null
                    && bar.getHighPrice().compareTo(bar.getLowPrice()) < 0) {
                addUnique("ohlcRange", fields);
            }
            if (bar.getHighPrice() != null && bar.getOpenPrice() != null
                    && bar.getHighPrice().compareTo(bar.getOpenPrice()) < 0) {
                addUnique("ohlcGeometry", fields);
            }
            if (bar.getHighPrice() != null && bar.getClosePrice() != null
                    && bar.getHighPrice().compareTo(bar.getClosePrice()) < 0) {
                addUnique("ohlcGeometry", fields);
            }
            if (bar.getLowPrice() != null && bar.getOpenPrice() != null
                    && bar.getLowPrice().compareTo(bar.getOpenPrice()) > 0) {
                addUnique("ohlcGeometry", fields);
            }
            if (bar.getLowPrice() != null && bar.getClosePrice() != null
                    && bar.getLowPrice().compareTo(bar.getClosePrice()) > 0) {
                addUnique("ohlcGeometry", fields);
            }
        }
        return fields;
    }

    private List<String> invalidVolumeFields(List<PersistedOhlcvBarDO> bars) {
        List<String> fields = new ArrayList<>();
        for (PersistedOhlcvBarDO bar : bars) {
            addWhenNull(bar.getVolume(), "volume", fields);
            if (bar.getVolume() != null && bar.getVolume().compareTo(BigDecimal.ZERO) < 0) {
                addUnique("volume", fields);
            }
        }
        return fields;
    }

    private Long parseTimeframeMs(String timeframe) {
        if (!hasText(timeframe) || timeframe.length() < 2) {
            return null;
        }
        String unit = timeframe.substring(timeframe.length() - 1);
        String amountText = timeframe.substring(0, timeframe.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) {
            return null;
        }
        return switch (unit) {
            case "m" -> amount * 60_000L;
            case "h" -> amount * 60L * 60_000L;
            case "d" -> amount * 24L * 60L * 60_000L;
            default -> null;
        };
    }

    private boolean invalidTimestampOrder(PersistedOhlcvBarDO bar) {
        return bar.getOpenTimeMs() == null || bar.getCloseTimeMs() == null
                || bar.getOpenTimeMs() < 0 || bar.getCloseTimeMs() <= bar.getOpenTimeMs();
    }

    private void addWhenBlank(String value, String field, List<String> fields) {
        if (!hasText(value)) {
            addUnique(field, fields);
        }
    }

    private void addWhenNull(Object value, String field, List<String> fields) {
        if (value == null) {
            addUnique(field, fields);
        }
    }

    private void addWhenNonPositive(BigDecimal value, String field, List<String> fields) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            addUnique(field, fields);
        }
    }

    private void addUnique(String field, List<String> fields) {
        if (!fields.contains(field)) {
            fields.add(field);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
