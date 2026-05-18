package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceWickSourceReviewModeEnum;
import org.example.trademodel.service.SourceTraceWickSourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceWickSourceOwnershipServiceTest {

    private final SourceTraceWickSourceOwnershipService service =
            new FailClosedSourceTraceWickSourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceWickSourceOwnershipResult result = service.resolveWickSourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getWickSource()).isNull();
        assertThat(result.getMissingFields()).containsExactly("wickSource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeWickOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceWickSourceOwnershipResult result =
                service.resolveWickSourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getWickSource()).isNull();
        assertThat(result.getMissingFields()).contains("wickSource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void quoteLatestPriceShouldNotBecomeWickOwnership() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1m");
        sourceTrace.setQuoteLatestPrice(new BigDecimal("102.30"));
        sourceTrace.setQuoteLatestPriceSource("DecisionResultVO.latestPrice");

        SourceTraceWickSourceOwnershipResult result = service.resolveWickSourceOwnership(null);

        assertThat(sourceTrace.getQuoteLatestPrice()).isEqualByComparingTo("102.30");
        assertThat(sourceTrace.getQuoteLatestPriceSource()).isEqualTo("DecisionResultVO.latestPrice");
        assertThat(sourceTrace.getWickSource()).isNull();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(result.getWickSource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void earlierSkeletonsShouldNotBecomeWickOwnership() {
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
        SourceTraceMultiTimeframeSourceOwnershipResult multiTimeframeOwnership =
                SourceTraceMultiTimeframeSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceEventSourceOwnershipResult eventOwnership =
                SourceTraceEventSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol(entryOwnership.getSymbol());
        runtimeKlineContext.setTimeframe(eventOwnership.getTimeframe());

        SourceTraceWickSourceOwnershipResult result =
                service.resolveWickSourceOwnership(runtimeKlineContext);

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(liquidityOwnership.getLiquiditySource()).isNull();
        assertThat(multiTimeframeOwnership.getMultiTimeframeSource()).isNull();
        assertThat(eventOwnership.getEventSource()).isNull();
        assertThat(result.getWickSource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void wickOnlyEvidenceShouldNotConfirmReversalOrTradeActions() {
        RuntimeKlineContextDTO wickConfirmedContext = new RuntimeKlineContextDTO();
        wickConfirmedContext.setSymbol("BTCUSDT");
        wickConfirmedContext.setTimeframe("1h");
        wickConfirmedContext.setWickSource("pin-bar-wick-confirmed");
        RuntimeKlineContextDTO reversalContext = new RuntimeKlineContextDTO();
        reversalContext.setSymbol("BTCUSDT");
        reversalContext.setTimeframe("1h");
        reversalContext.setWickSource("trend-reversal-candidate");
        RuntimeKlineContextDTO missingInputContext = new RuntimeKlineContextDTO();
        missingInputContext.setSymbol("BTCUSDT");

        assertReviewOnlyMissing(service.resolveWickSourceOwnership(wickConfirmedContext));
        assertReviewOnlyMissing(service.resolveWickSourceOwnership(reversalContext));
        assertReviewOnlyMissing(service.resolveWickSourceOwnership(missingInputContext));

        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setWickSource("pin-bar-wick-confirmed");

        assertThat(sourceTrace.getWickSource()).isEqualTo("pin-bar-wick-confirmed");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceWickSourceOwnershipService.class,
                FailClosedSourceTraceWickSourceOwnershipService.class,
                SourceTraceWickSourceOwnershipResult.class
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

    private void assertReviewOnlyMissing(SourceTraceWickSourceOwnershipResult result) {
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceWickSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceWickSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceWickSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getWickSource()).isNull();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    private RuntimeKlineItemDTO klineItem(String closePrice) {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setClosePrice(new BigDecimal(closePrice));
        return item;
    }
}
