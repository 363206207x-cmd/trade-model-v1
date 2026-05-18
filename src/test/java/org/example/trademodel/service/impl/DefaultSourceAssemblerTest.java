package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceReviewModeEnum;
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
    void stopOwnershipSkeletonDefaultShouldLeaveSourceTraceStopMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);
        SourceTraceStopSourceOwnershipResult stopOwnership =
                new FailClosedSourceTraceStopSourceOwnershipService()
                        .resolveStopSourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getOwnershipStatus()).isEqualTo(SourceTraceStopSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(stopOwnership.getMissingReason()).isEqualTo(SourceTraceStopSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(stopOwnership.getReviewMode()).isEqualTo(SourceTraceStopSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(stopOwnership.isManualReviewRequired()).isTrue();
        assertThat(stopOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getStopPriceSource()).isNull();
        assertThat(sourceTrace.getStopSourceType()).isNull();
        assertThat(sourceTrace.getStopSourceTimeframe()).isNull();
        assertThat(sourceTrace.getStopSourceReason()).isNull();
        assertThat(sourceTrace.getStopSourceRef()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains(
                "stopPriceSource",
                "stopSourceType",
                "stopSourceTimeframe",
                "stopSourceReason",
                "stopSourceRef"
        );
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void takeProfitOwnershipSkeletonDefaultShouldLeaveSourceTraceTakeProfitMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);
        SourceTraceStopSourceOwnershipResult stopOwnership =
                new FailClosedSourceTraceStopSourceOwnershipService()
                        .resolveStopSourceOwnership(runtimeKlineContext);
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                new FailClosedSourceTraceTakeProfitSourceOwnershipService()
                        .resolveTakeProfitSourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getOwnershipStatus())
                .isEqualTo(SourceTraceTakeProfitSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(takeProfitOwnership.getMissingReason())
                .isEqualTo(SourceTraceTakeProfitSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(takeProfitOwnership.getReviewMode())
                .isEqualTo(SourceTraceTakeProfitSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(takeProfitOwnership.isManualReviewRequired()).isTrue();
        assertThat(takeProfitOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getTpPriceSources()).isEmpty();
        assertThat(sourceTrace.getTpSourceType()).isNull();
        assertThat(sourceTrace.getTpSourceTimeframe()).isNull();
        assertThat(sourceTrace.getTpSourceReason()).isNull();
        assertThat(sourceTrace.getTpSourceRef()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains(
                "tpPriceSources",
                "tpSourceType",
                "tpSourceTimeframe",
                "tpSourceReason",
                "tpSourceRef"
        );
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void riskRewardOwnershipSkeletonDefaultShouldLeaveSourceTraceRiskRewardMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);
        SourceTraceStopSourceOwnershipResult stopOwnership =
                new FailClosedSourceTraceStopSourceOwnershipService()
                        .resolveStopSourceOwnership(runtimeKlineContext);
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                new FailClosedSourceTraceTakeProfitSourceOwnershipService()
                        .resolveTakeProfitSourceOwnership(runtimeKlineContext);
        SourceTraceRiskRewardSourceOwnershipResult riskRewardOwnership =
                new FailClosedSourceTraceRiskRewardSourceOwnershipService()
                        .resolveRiskRewardSourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getOwnershipStatus())
                .isEqualTo(SourceTraceRiskRewardSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(riskRewardOwnership.getMissingReason())
                .isEqualTo(SourceTraceRiskRewardSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(riskRewardOwnership.getReviewMode())
                .isEqualTo(SourceTraceRiskRewardSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(riskRewardOwnership.isManualReviewRequired()).isTrue();
        assertThat(riskRewardOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getRrSource()).isNull();
        assertThat(sourceTrace.getRrRuleRef()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains("rrSource", "rrRuleRef");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void liquidityOwnershipSkeletonDefaultShouldLeaveSourceTraceLiquidityMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);
        SourceTraceStopSourceOwnershipResult stopOwnership =
                new FailClosedSourceTraceStopSourceOwnershipService()
                        .resolveStopSourceOwnership(runtimeKlineContext);
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                new FailClosedSourceTraceTakeProfitSourceOwnershipService()
                        .resolveTakeProfitSourceOwnership(runtimeKlineContext);
        SourceTraceRiskRewardSourceOwnershipResult riskRewardOwnership =
                new FailClosedSourceTraceRiskRewardSourceOwnershipService()
                        .resolveRiskRewardSourceOwnership(runtimeKlineContext);
        SourceTraceLiquiditySourceOwnershipResult liquidityOwnership =
                new FailClosedSourceTraceLiquiditySourceOwnershipService()
                        .resolveLiquiditySourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(liquidityOwnership.getOwnershipStatus())
                .isEqualTo(SourceTraceLiquiditySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(liquidityOwnership.getMissingReason())
                .isEqualTo(SourceTraceLiquiditySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(liquidityOwnership.getReviewMode())
                .isEqualTo(SourceTraceLiquiditySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(liquidityOwnership.getLiquiditySource()).isNull();
        assertThat(liquidityOwnership.isManualReviewRequired()).isTrue();
        assertThat(liquidityOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getLiquiditySource()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains("liquiditySource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void multiTimeframeOwnershipSkeletonDefaultShouldLeaveSourceTraceMultiTimeframeMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);
        SourceTraceStopSourceOwnershipResult stopOwnership =
                new FailClosedSourceTraceStopSourceOwnershipService()
                        .resolveStopSourceOwnership(runtimeKlineContext);
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                new FailClosedSourceTraceTakeProfitSourceOwnershipService()
                        .resolveTakeProfitSourceOwnership(runtimeKlineContext);
        SourceTraceRiskRewardSourceOwnershipResult riskRewardOwnership =
                new FailClosedSourceTraceRiskRewardSourceOwnershipService()
                        .resolveRiskRewardSourceOwnership(runtimeKlineContext);
        SourceTraceLiquiditySourceOwnershipResult liquidityOwnership =
                new FailClosedSourceTraceLiquiditySourceOwnershipService()
                        .resolveLiquiditySourceOwnership(runtimeKlineContext);
        SourceTraceMultiTimeframeSourceOwnershipResult multiTimeframeOwnership =
                new FailClosedSourceTraceMultiTimeframeSourceOwnershipService()
                        .resolveMultiTimeframeSourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(liquidityOwnership.getLiquiditySource()).isNull();
        assertThat(multiTimeframeOwnership.getOwnershipStatus())
                .isEqualTo(SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(multiTimeframeOwnership.getMissingReason())
                .isEqualTo(SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(multiTimeframeOwnership.getReviewMode())
                .isEqualTo(SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(multiTimeframeOwnership.getMultiTimeframeSource()).isNull();
        assertThat(multiTimeframeOwnership.isManualReviewRequired()).isTrue();
        assertThat(multiTimeframeOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getMultiTimeframeSource()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains("multiTimeframeSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void eventOwnershipSkeletonDefaultShouldLeaveSourceTraceEventMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);
        SourceTraceStopSourceOwnershipResult stopOwnership =
                new FailClosedSourceTraceStopSourceOwnershipService()
                        .resolveStopSourceOwnership(runtimeKlineContext);
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                new FailClosedSourceTraceTakeProfitSourceOwnershipService()
                        .resolveTakeProfitSourceOwnership(runtimeKlineContext);
        SourceTraceRiskRewardSourceOwnershipResult riskRewardOwnership =
                new FailClosedSourceTraceRiskRewardSourceOwnershipService()
                        .resolveRiskRewardSourceOwnership(runtimeKlineContext);
        SourceTraceLiquiditySourceOwnershipResult liquidityOwnership =
                new FailClosedSourceTraceLiquiditySourceOwnershipService()
                        .resolveLiquiditySourceOwnership(runtimeKlineContext);
        SourceTraceMultiTimeframeSourceOwnershipResult multiTimeframeOwnership =
                new FailClosedSourceTraceMultiTimeframeSourceOwnershipService()
                        .resolveMultiTimeframeSourceOwnership(runtimeKlineContext);
        SourceTraceEventSourceOwnershipResult eventOwnership =
                new FailClosedSourceTraceEventSourceOwnershipService()
                        .resolveEventSourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(liquidityOwnership.getLiquiditySource()).isNull();
        assertThat(multiTimeframeOwnership.getMultiTimeframeSource()).isNull();
        assertThat(eventOwnership.getOwnershipStatus())
                .isEqualTo(SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(eventOwnership.getMissingReason())
                .isEqualTo(SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(eventOwnership.getReviewMode())
                .isEqualTo(SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(eventOwnership.getEventSource()).isNull();
        assertThat(eventOwnership.isManualReviewRequired()).isTrue();
        assertThat(eventOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getEventSource()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains("eventSource");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void wickOwnershipSkeletonDefaultShouldLeaveSourceTraceWickMissing() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                new FailClosedSourceTraceEntrySourceOwnershipService()
                        .resolveEntrySourceOwnership(runtimeKlineContext);
        SourceTraceStopSourceOwnershipResult stopOwnership =
                new FailClosedSourceTraceStopSourceOwnershipService()
                        .resolveStopSourceOwnership(runtimeKlineContext);
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                new FailClosedSourceTraceTakeProfitSourceOwnershipService()
                        .resolveTakeProfitSourceOwnership(runtimeKlineContext);
        SourceTraceRiskRewardSourceOwnershipResult riskRewardOwnership =
                new FailClosedSourceTraceRiskRewardSourceOwnershipService()
                        .resolveRiskRewardSourceOwnership(runtimeKlineContext);
        SourceTraceLiquiditySourceOwnershipResult liquidityOwnership =
                new FailClosedSourceTraceLiquiditySourceOwnershipService()
                        .resolveLiquiditySourceOwnership(runtimeKlineContext);
        SourceTraceMultiTimeframeSourceOwnershipResult multiTimeframeOwnership =
                new FailClosedSourceTraceMultiTimeframeSourceOwnershipService()
                        .resolveMultiTimeframeSourceOwnership(runtimeKlineContext);
        SourceTraceEventSourceOwnershipResult eventOwnership =
                new FailClosedSourceTraceEventSourceOwnershipService()
                        .resolveEventSourceOwnership(runtimeKlineContext);
        SourceTraceWickSourceOwnershipResult wickOwnership =
                new FailClosedSourceTraceWickSourceOwnershipService()
                        .resolveWickSourceOwnership(runtimeKlineContext);

        SourceTraceDTO sourceTrace = assembler.assembleSourceTrace(runtimeKlineContext, validDerivativesRiskContext());

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(liquidityOwnership.getLiquiditySource()).isNull();
        assertThat(multiTimeframeOwnership.getMultiTimeframeSource()).isNull();
        assertThat(eventOwnership.getEventSource()).isNull();
        assertThat(wickOwnership.getOwnershipStatus())
                .isEqualTo(SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(wickOwnership.getMissingReason())
                .isEqualTo(SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(wickOwnership.getReviewMode())
                .isEqualTo(SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(wickOwnership.getWickSource()).isNull();
        assertThat(wickOwnership.isManualReviewRequired()).isTrue();
        assertThat(wickOwnership.isNotTradeInstruction()).isTrue();
        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.getWickSource()).isNull();
        assertThat(sourceTrace.getMissingFields()).contains("wickSource");
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
