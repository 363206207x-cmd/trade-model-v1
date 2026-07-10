package org.example.trademodel.service.impl;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeKlineContextAssemblyServiceImplTest {

    private final RuntimeKlineContextAssemblyServiceImpl service =
            new RuntimeKlineContextAssemblyServiceImpl();

    @Test
    void shouldMapFreshReadinessToSafeRuntimeFieldsOnly() {
        PersistedOhlcvBarDO older = bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10");
        PersistedOhlcvBarDO latest = bar(60_000L, 119_999L, "101.10", "130.00", "100.50", "102.30");
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(latest, older));

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(context.getTimeframe()).isEqualTo("1m");
        assertThat(context.getLatestPrice()).isEqualByComparingTo("102.30");
        assertThat(context.getKlineItems()).hasSize(2);
        assertThat(context.getKlineItems().get(0).getClosePrice()).isEqualByComparingTo("102.30");
        assertThat(context.getKlineItems().get(0).getProvider()).isEqualTo("LOCAL_FIXTURE");
        assertThat(context.getPersistedOhlcvReadinessStatus()).isEqualTo("FRESH");
        assertThat(context.getPersistedOhlcvStaleReasonCode()).isEqualTo("NONE");
        assertThat(context.getPersistedOhlcvMissingFields()).isEmpty();
        assertThat(context.getMissingFields()).isEmpty();
        assertThat(context.getFallbackStatus()).isNull();
        assertThat(context.isComplete()).isTrue();
        assertThat(context.getEntryPriceSource()).isNull();
        assertThat(context.getStopPriceSource()).isNull();
        assertThat(context.getTpPriceSources()).isEmpty();
        assertThat(context.getRrSource()).isNull();
        assertThat(context.getLiquiditySource()).isNull();
        assertThat(context.getMultiTimeframeSource()).isNull();
        assertThat(context.getEventSource()).isNull();
        assertThat(context.getWickSource()).isNull();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldUseLatestClosedClosePriceOnlyForLatestPrice() {
        PersistedOhlcvBarDO older = bar(0L, 59_999L, "100.00", "300.00", "80.00", "101.10");
        PersistedOhlcvBarDO latest = bar(60_000L, 119_999L, "101.10", "999.99", "90.00", "102.30");
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(older, latest));

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getLatestPrice()).isEqualByComparingTo("102.30");
        assertThat(context.getLatestPrice()).isNotEqualByComparingTo(latest.getHighPrice());
        assertThat(context.getLatestPrice()).isNotEqualByComparingTo(latest.getOpenPrice());
        assertThat(context.getLatestPrice()).isNotEqualByComparingTo(older.getClosePrice());
        assertThat(context.getEntryPriceSource()).isNull();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldFailClosedForStaleReadiness() {
        RuntimeKlineContextDTO context = service.assemble(nonFreshReadiness(
                PersistedOhlcvReadinessStatus.STALE,
                PersistedOhlcvStaleReasonCode.LATEST_BAR_TOO_OLD,
                List.of("klineFreshness")
        ));

        assertFailClosed(context, "STALE", "LATEST_BAR_TOO_OLD", "klineFreshness");
    }

    @Test
    void shouldFailClosedForPartialReadiness() {
        RuntimeKlineContextDTO context = service.assemble(nonFreshReadiness(
                PersistedOhlcvReadinessStatus.PARTIAL,
                PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT,
                List.of("requiredClosedBars")
        ));

        assertFailClosed(context, "PARTIAL", "WINDOW_TOO_SHORT", "requiredClosedBars");
    }

    @Test
    void shouldFailClosedForMissingReadiness() {
        RuntimeKlineContextDTO context = service.assemble(nonFreshReadiness(
                PersistedOhlcvReadinessStatus.MISSING,
                PersistedOhlcvStaleReasonCode.NO_BARS_FOR_SYMBOL_TIMEFRAME,
                List.of("persistedOhlcvWindow", "klineItems")
        ));

        assertFailClosed(context, "MISSING", "NO_BARS_FOR_SYMBOL_TIMEFRAME", "persistedOhlcvWindow");
        assertThat(context.getMissingFields()).contains("klineItems");
    }

    @Test
    void shouldFailClosedForUnknownReadiness() {
        RuntimeKlineContextDTO context = service.assemble(nonFreshReadiness(
                PersistedOhlcvReadinessStatus.UNKNOWN,
                PersistedOhlcvStaleReasonCode.SOURCE_OWNER_MISSING,
                List.of("provider", "sourceTraceId")
        ));

        assertFailClosed(context, "UNKNOWN", "SOURCE_OWNER_MISSING", "provider");
        assertThat(context.getMissingFields()).contains("sourceTraceId");
    }

    @Test
    void shouldFailClosedForInvalidReadiness() {
        RuntimeKlineContextDTO context = service.assemble(nonFreshReadiness(
                PersistedOhlcvReadinessStatus.INVALID,
                PersistedOhlcvStaleReasonCode.QUALITY_STATUS_NOT_OK,
                List.of("qualityStatus")
        ));

        assertFailClosed(context, "INVALID", "QUALITY_STATUS_NOT_OK", "qualityStatus");
    }

    @Test
    void shouldFailClosedForNullReadiness() {
        RuntimeKlineContextDTO context = service.assemble(null);

        assertThat(context.getSymbol()).isNull();
        assertThat(context.getTimeframe()).isNull();
        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).containsExactly("persistedOhlcvReadinessResult");
        assertThat(context.getLatestPrice()).isNull();
        assertThat(context.getKlineItems()).isEmpty();
        assertThat(context.isComplete()).isFalse();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldFailClosedWhenFreshReadinessHasNoBars() {
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of());
        readiness.setLatestCloseTimeMs(null);

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getPersistedOhlcvReadinessStatus()).isEqualTo("FRESH");
        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).contains("klineItems");
        assertThat(context.getLatestPrice()).isNull();
        assertThat(context.getKlineItems()).isEmpty();
        assertThat(context.isComplete()).isFalse();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldFailClosedWhenFreshReadinessHasOpenCandle() {
        PersistedOhlcvBarDO open = bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10");
        open.setClosed(false);
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(open));
        readiness.setRequiredWindowSize(1);

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).contains("closed", "latestCloseTimeMs");
        assertThat(context.getLatestPrice()).isNull();
        assertThat(context.getKlineItems()).isEmpty();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldFailClosedWhenFreshReadinessHasDeletedBar() {
        PersistedOhlcvBarDO deleted = bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10");
        deleted.setIsDeleted(1);
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(deleted));
        readiness.setRequiredWindowSize(1);

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).contains("isDeleted");
        assertThat(context.getLatestPrice()).isNull();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldFailClosedWhenFreshReadinessHasMissingSourceOwnership() {
        PersistedOhlcvBarDO bar = bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10");
        bar.setProvider(null);
        bar.setSourceTraceId("");
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(bar));
        readiness.setRequiredWindowSize(1);

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).contains("provider", "sourceTraceId");
        assertThat(context.getLatestPrice()).isNull();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldFailClosedWhenFreshReadinessHasUnsafePriceFields() {
        PersistedOhlcvBarDO bar = bar(0L, 59_999L, "100.00", "95.00", "98.00", "101.10");
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(bar));
        readiness.setRequiredWindowSize(1);

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).contains("ohlcRange");
        assertThat(context.getLatestPrice()).isNull();
        assertSafetyDefaults(context);
    }

    @Test
    void shouldFailClosedWhenFreshReadinessCarriesUnsafeSafetyDefaults() {
        PersistedOhlcvReadinessResult readiness = freshReadiness(List.of(
                bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10")
        ));
        readiness.setRequiredWindowSize(1);
        readiness.setManualReviewRequired(false);
        readiness.setNotTradeInstruction(false);

        RuntimeKlineContextDTO context = service.assemble(readiness);

        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).contains("manualReviewRequired", "notTradeInstruction");
        assertSafetyDefaults(context);
    }

    private PersistedOhlcvReadinessResult freshReadiness(List<PersistedOhlcvBarDO> bars) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setSymbol("BTCUSDT");
        result.setTimeframe("1m");
        result.setRequiredWindowSize(2);
        result.setStatus(PersistedOhlcvReadinessStatus.FRESH);
        result.setStaleReasonCode(PersistedOhlcvStaleReasonCode.NONE);
        result.setStaleReasonText("Persisted OHLCV window is fresh.");
        result.setMissingFields(List.of());
        result.setBars(bars);
        result.setLatestCloseTimeMs(bars.stream()
                .filter(bar -> bar.getCloseTimeMs() != null)
                .map(PersistedOhlcvBarDO::getCloseTimeMs)
                .max(Long::compareTo)
                .orElse(null));
        result.setLatestIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }

    private PersistedOhlcvReadinessResult nonFreshReadiness(
            PersistedOhlcvReadinessStatus status,
            PersistedOhlcvStaleReasonCode reasonCode,
            List<String> missingFields
    ) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setSymbol("BTCUSDT");
        result.setTimeframe("1m");
        result.setRequiredWindowSize(2);
        result.setStatus(status);
        result.setStaleReasonCode(reasonCode);
        result.setStaleReasonText("readiness failed closed");
        result.setMissingFields(missingFields);
        result.setBars(List.of(bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10")));
        result.setLatestCloseTimeMs(59_999L);
        result.setLatestIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }

    private PersistedOhlcvBarDO bar(
            Long openTimeMs,
            Long closeTimeMs,
            String openPrice,
            String highPrice,
            String lowPrice,
            String closePrice
    ) {
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setSymbol("BTCUSDT");
        bar.setTimeframe("1m");
        bar.setOpenTimeMs(openTimeMs);
        bar.setCloseTimeMs(closeTimeMs);
        bar.setOpenPrice(new BigDecimal(openPrice));
        bar.setHighPrice(new BigDecimal(highPrice));
        bar.setLowPrice(new BigDecimal(lowPrice));
        bar.setClosePrice(new BigDecimal(closePrice));
        bar.setVolume(new BigDecimal("123.45"));
        bar.setClosed(true);
        bar.setProvider("LOCAL_FIXTURE");
        bar.setProviderMarketType("USDT_PERP");
        bar.setSourceEndpoint("persisted-ohlcv-fixture");
        bar.setSourceBatchId("batch-1");
        bar.setSourceTraceId("trace-1");
        bar.setSourceVersion(1);
        bar.setFetchTime(LocalDateTime.of(2026, 5, 17, 10, 0));
        bar.setSourceStatus("READY");
        bar.setFreshnessStatus("FRESH");
        bar.setProvenanceVersion("runtime-test-v1");
        bar.setIngestionRunId("run-fixture");
        bar.setIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        bar.setQualityStatus("OK");
        bar.setIsDeleted(0);
        return bar;
    }

    private void assertFailClosed(
            RuntimeKlineContextDTO context,
            String status,
            String reasonCode,
            String expectedMissingField
    ) {
        assertThat(context.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(context.getTimeframe()).isEqualTo("1m");
        assertThat(context.getPersistedOhlcvReadinessStatus()).isEqualTo(status);
        assertThat(context.getPersistedOhlcvStaleReasonCode()).isEqualTo(reasonCode);
        assertThat(context.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(context.getMissingFields()).contains(expectedMissingField);
        assertThat(context.getLatestPrice()).isNull();
        assertThat(context.getKlineItems()).isEmpty();
        assertThat(context.getEntryPriceSource()).isNull();
        assertThat(context.getStopPriceSource()).isNull();
        assertThat(context.getTpPriceSources()).isEmpty();
        assertThat(context.getRrSource()).isNull();
        assertThat(context.isComplete()).isFalse();
        assertSafetyDefaults(context);
    }

    private void assertSafetyDefaults(RuntimeKlineContextDTO context) {
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
    }
}
