package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSourceAssemblerTest {

    private final DefaultSourceAssembler assembler = new DefaultSourceAssembler();

    @Test
    void assembleSourceTraceReturnsIncompleteWhenRuntimeContextIsMissing() {
        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(null, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getMissingFields()).contains("runtimeKlineContext");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void assembleSourceTraceReturnsIncompleteWhenStructuralSourceIsMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = validRuntimeKlineContext();
        runtimeKlineContext.setEntryPriceSource(null);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getMissingFields()).contains("entryPriceSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assembleSourceTraceReturnsIncompleteWhenRiskRewardSourceIsMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = validRuntimeKlineContext();
        runtimeKlineContext.setRrSource(null);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getMissingFields()).contains("rrSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assembleSourceTraceReturnsSafeFailWhenLiquiditySourceIsMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = validRuntimeKlineContext();
        runtimeKlineContext.setLiquiditySource(null);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        assertThat(sourceTrace.getMissingFields()).contains("liquiditySource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assembleSourceTraceReturnsWatchOnlyWhenEventSourceIsMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = validRuntimeKlineContext();
        runtimeKlineContext.setEventSource(null);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(sourceTrace.getMissingFields()).contains("eventSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assembleSourceTraceReturnsWatchOnlyWhenMultiTimeframeSourceIsMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = validRuntimeKlineContext();
        runtimeKlineContext.setMultiTimeframeSource(null);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(sourceTrace.getMissingFields()).contains("multiTimeframeSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assembleSourceTraceReturnsWatchOnlyWhenWickSourceIsMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = validRuntimeKlineContext();
        runtimeKlineContext.setWickSource(null);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(sourceTrace.getMissingFields()).contains("wickSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assembleSourceTraceReturnsCompleteTraceWhenAllSourcesArePresent() {
        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(validRuntimeKlineContext(), validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isNull();
        assertThat(sourceTrace.getMissingFields()).isEmpty();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isTrue();
        assertThat(sourceTrace.getEntryPriceSource()).isEqualByComparingTo("68000");
        assertThat(sourceTrace.getStopPriceSource()).isEqualByComparingTo("66800");
        assertThat(sourceTrace.getTpPriceSources()).containsExactly(BigDecimal.valueOf(70400));
        assertThat(sourceTrace.getRrSource()).isEqualByComparingTo("2");
        assertThat(sourceTrace.getLiquiditySource()).isEqualTo("liquidity-ok");
        assertThat(sourceTrace.getMultiTimeframeSource()).isEqualTo("multi-timeframe-aligned");
        assertThat(sourceTrace.getEventSource()).isEqualTo("no-event-window");
        assertThat(sourceTrace.getWickSource()).isEqualTo("wick-confirmed");
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsAloneShouldNotPopulateBoundarySources() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(BigDecimal.valueOf(68100));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30")));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getEntryPriceSource()).isNull();
        assertThat(sourceTrace.getStopPriceSource()).isNull();
        assertThat(sourceTrace.getTpPriceSources()).isEmpty();
        assertThat(sourceTrace.getRrSource()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains(
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef",
                "stopPriceSource",
                "stopSourceType",
                "stopSourceTimeframe",
                "stopSourceReason",
                "stopSourceRef",
                "tpPriceSources",
                "tpSourceType",
                "tpSourceTimeframe",
                "tpSourceReason",
                "tpSourceRef",
                "rrSource",
                "rrRuleRef",
                "liquiditySource",
                "multiTimeframeSource",
                "eventSource",
                "wickSource"
        );
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void entryOwnershipSkeletonDefaultShouldLeaveSourceTraceEntryMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getOwnershipStatus()).isEqualTo(SourceTraceEntrySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(entryOwnership.getMissingReason()).isEqualTo(SourceTraceEntrySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(entryOwnership.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(entryOwnership.isManualReviewRequired()).isTrue();
        assertThat(entryOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getEntryPriceSource()).isNull();
        assertThat(sourceTrace.getEntrySourceType()).isNull();
        assertThat(sourceTrace.getEntrySourceTimeframe()).isNull();
        assertThat(sourceTrace.getEntrySourceReason()).isNull();
        assertThat(sourceTrace.getEntrySourceRef()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains(
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef"
        );
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void assembleSourceTracePropagatesDerivativesRiskMissingFields() {
        DerivativesRiskContextDTO derivativesRiskContext = validDerivativesRiskContext();
        derivativesRiskContext.setMissingFields(List.of("openInterestHistory"));

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(validRuntimeKlineContext(), derivativesRiskContext);

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(sourceTrace.getMissingFields()).contains("derivativesRiskContext.openInterestHistory");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assembleSourceTraceMarksMissingDerivativesRiskContextAsWatchOnly() {
        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(validRuntimeKlineContext(), null);

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(sourceTrace.getMissingFields()).contains("derivativesRiskContext");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
    }

    @Test
    void assemblerShouldNotExposeTradingExecutionMethods() {
        List<String> methodNames = List.of(DefaultSourceAssembler.class.getDeclaredMethods())
                .stream()
                .map(Method::getName)
                .map(String::toLowerCase)
                .toList();

        assertThat(methodNames).noneMatch(name -> name.contains("execute"));
        assertThat(methodNames).noneMatch(name -> name.contains("order"));
        assertThat(methodNames).noneMatch(name -> name.contains("close"));
        assertThat(methodNames).noneMatch(name -> name.contains("reverse"));
    }

    private RuntimeKlineContextDTO validRuntimeKlineContext() {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        context.setSymbol("BTCUSDT");
        context.setTimeframe("1h");
        context.setLatestPrice(BigDecimal.valueOf(68100));
        context.setDataQualityScore(BigDecimal.valueOf(90));
        context.setEntryPriceSource(BigDecimal.valueOf(68000));
        context.setEntrySourceType("support");
        context.setEntrySourceTimeframe("1h");
        context.setEntrySourceReason("support retest");
        context.setEntrySourceRef("entry-1");
        context.setStopPriceSource(BigDecimal.valueOf(66800));
        context.setStopSourceType("swing_low");
        context.setStopSourceTimeframe("1h");
        context.setStopSourceReason("recent swing low");
        context.setStopSourceRef("stop-1");
        context.setTpPriceSources(List.of(BigDecimal.valueOf(70400)));
        context.setTpSourceType("rr_ladder");
        context.setTpSourceTimeframe("1h");
        context.setTpSourceReason("2R target");
        context.setTpSourceRef("tp-1");
        context.setRrSource(BigDecimal.valueOf(2));
        context.setRrRuleRef("min_rr_2");
        context.setLiquiditySource("liquidity-ok");
        context.setMultiTimeframeSource("multi-timeframe-aligned");
        context.setEventSource("no-event-window");
        context.setWickSource("wick-confirmed");
        return context;
    }

    private DerivativesRiskContextDTO validDerivativesRiskContext() {
        DerivativesRiskContextDTO context = new DerivativesRiskContextDTO();
        context.setSymbol("BTCUSDT");
        context.setTimeframe("1h");
        context.setOpenInterestHistory(List.of(BigDecimal.valueOf(1000), BigDecimal.valueOf(1020)));
        context.setFundingHistory(List.of(new BigDecimal("0.0001"), new BigDecimal("0.0002")));
        context.setLiquidationCluster(List.of(BigDecimal.valueOf(66500)));
        context.setLeverageDistribution(Map.of("1-5x", BigDecimal.valueOf(0.6)));
        context.setLongShortRatio(BigDecimal.valueOf(1.1));
        context.setLiquidityStress("LOW");
        context.setEventWindowBlockers(List.of("none"));
        context.setWickConfirmationSources(List.of("wick-confirmed"));
        context.setDataQualityScore(BigDecimal.valueOf(90));
        return context;
    }

    private RuntimeKlineItemDTO klineItem(String closePrice) {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setClosePrice(new BigDecimal(closePrice));
        return item;
    }
}
