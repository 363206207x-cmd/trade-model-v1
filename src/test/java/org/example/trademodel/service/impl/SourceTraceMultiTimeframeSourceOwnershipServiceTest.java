package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceMultiTimeframeSourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceMultiTimeframeSourceOwnershipServiceTest {

    private final SourceTraceMultiTimeframeSourceOwnershipService service =
            new FailClosedSourceTraceMultiTimeframeSourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceMultiTimeframeSourceOwnershipResult result =
                service.resolveMultiTimeframeSourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getMultiTimeframeSource()).isNull();
        assertThat(result.getMissingFields()).containsExactly("multiTimeframeSource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeMultiTimeframeOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceMultiTimeframeSourceOwnershipResult result =
                service.resolveMultiTimeframeSourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getMultiTimeframeSource()).isNull();
        assertThat(result.getMissingFields()).contains("multiTimeframeSource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void quoteLatestPriceShouldNotBecomeMultiTimeframeOwnership() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1m");
        sourceTrace.setQuoteLatestPrice(new BigDecimal("102.30"));
        sourceTrace.setQuoteLatestPriceSource("DecisionResultVO.latestPrice");

        SourceTraceMultiTimeframeSourceOwnershipResult result =
                service.resolveMultiTimeframeSourceOwnership(null);

        assertThat(sourceTrace.getQuoteLatestPrice()).isEqualByComparingTo("102.30");
        assertThat(sourceTrace.getQuoteLatestPriceSource()).isEqualTo("DecisionResultVO.latestPrice");
        assertThat(sourceTrace.getMultiTimeframeSource()).isNull();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(result.getMultiTimeframeSource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void earlierSkeletonsShouldNotBecomeMultiTimeframeOwnership() {
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                SourceTraceEntrySourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceStopSourceOwnershipResult stopOwnership =
                SourceTraceStopSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                SourceTraceTakeProfitSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceRiskRewardSourceOwnershipResult riskRewardOwnership =
                SourceTraceRiskRewardSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceLiquiditySourceOwnershipResult liquidityOwnership =
                SourceTraceLiquiditySourceOwnershipResult.missingSource("BTCUSDT", "1m");
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol(entryOwnership.getSymbol());
        runtimeKlineContext.setTimeframe(liquidityOwnership.getTimeframe());

        SourceTraceMultiTimeframeSourceOwnershipResult result =
                service.resolveMultiTimeframeSourceOwnership(runtimeKlineContext);

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(liquidityOwnership.getLiquiditySource()).isNull();
        assertThat(result.getMultiTimeframeSource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void agreementConflictAndMissingInputsShouldRemainReviewOnly() {
        RuntimeKlineContextDTO alignedContext = new RuntimeKlineContextDTO();
        alignedContext.setSymbol("BTCUSDT");
        alignedContext.setTimeframe("1h");
        alignedContext.setMultiTimeframeSource("multi-timeframe-aligned");
        RuntimeKlineContextDTO conflictContext = new RuntimeKlineContextDTO();
        conflictContext.setSymbol("BTCUSDT");
        conflictContext.setTimeframe("1h");
        conflictContext.setMultiTimeframeSource("multi-timeframe-conflict");
        RuntimeKlineContextDTO missingInputContext = new RuntimeKlineContextDTO();
        missingInputContext.setSymbol("BTCUSDT");

        assertReviewOnlyMissing(service.resolveMultiTimeframeSourceOwnership(alignedContext));
        assertReviewOnlyMissing(service.resolveMultiTimeframeSourceOwnership(conflictContext));
        assertReviewOnlyMissing(service.resolveMultiTimeframeSourceOwnership(missingInputContext));

        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setMultiTimeframeSource("multi-timeframe-aligned");

        assertThat(sourceTrace.getMultiTimeframeSource()).isEqualTo("multi-timeframe-aligned");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceMultiTimeframeSourceOwnershipService.class,
                FailClosedSourceTraceMultiTimeframeSourceOwnershipService.class,
                SourceTraceMultiTimeframeSourceOwnershipResult.class
        );
        List<String> forbiddenFragments = List.of(
                "execute",
                "placeorder",
                "closeposition",
                "reverseposition",
                "autotrade",
                "orderid",
                "orderside",
                "executionid"
        );

        for (Class<?> type : types) {
            for (Method method : type.getMethods()) {
                String name = method.getName().toLowerCase();
                assertThat(forbiddenFragments).noneMatch(name::contains);
            }
        }
    }

    private void assertReviewOnlyMissing(SourceTraceMultiTimeframeSourceOwnershipResult result) {
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceMultiTimeframeSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceMultiTimeframeSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceMultiTimeframeSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getMultiTimeframeSource()).isNull();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    private RuntimeKlineItemDTO klineItem(String closePrice) {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setClosePrice(new BigDecimal(closePrice));
        return item;
    }
}
