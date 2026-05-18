package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceMultiTimeframeSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceEventSourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceEventSourceOwnershipServiceTest {

    private final SourceTraceEventSourceOwnershipService service =
            new FailClosedSourceTraceEventSourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceEventSourceOwnershipResult result = service.resolveEventSourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getEventSource()).isNull();
        assertThat(result.getMissingFields()).containsExactly("eventSource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeEventOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceEventSourceOwnershipResult result =
                service.resolveEventSourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getEventSource()).isNull();
        assertThat(result.getMissingFields()).contains("eventSource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void quoteLatestPriceShouldNotBecomeEventOwnership() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1m");
        sourceTrace.setQuoteLatestPrice(new BigDecimal("102.30"));
        sourceTrace.setQuoteLatestPriceSource("DecisionResultVO.latestPrice");

        SourceTraceEventSourceOwnershipResult result = service.resolveEventSourceOwnership(null);

        assertThat(sourceTrace.getQuoteLatestPrice()).isEqualByComparingTo("102.30");
        assertThat(sourceTrace.getQuoteLatestPriceSource()).isEqualTo("DecisionResultVO.latestPrice");
        assertThat(sourceTrace.getEventSource()).isNull();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(result.getEventSource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void earlierSkeletonsShouldNotBecomeEventOwnership() {
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
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol(entryOwnership.getSymbol());
        runtimeKlineContext.setTimeframe(multiTimeframeOwnership.getTimeframe());

        SourceTraceEventSourceOwnershipResult result =
                service.resolveEventSourceOwnership(runtimeKlineContext);

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(liquidityOwnership.getLiquiditySource()).isNull();
        assertThat(multiTimeframeOwnership.getMultiTimeframeSource()).isNull();
        assertThat(result.getEventSource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void missingEventSourceAndEventUncertaintyShouldRemainReviewOnly() {
        RuntimeKlineContextDTO noEventContext = new RuntimeKlineContextDTO();
        noEventContext.setSymbol("BTCUSDT");
        noEventContext.setTimeframe("1h");
        noEventContext.setEventSource("no-event-window");
        RuntimeKlineContextDTO uncertainContext = new RuntimeKlineContextDTO();
        uncertainContext.setSymbol("BTCUSDT");
        uncertainContext.setTimeframe("1h");
        uncertainContext.setEventSource("event-uncertain");
        RuntimeKlineContextDTO missingInputContext = new RuntimeKlineContextDTO();
        missingInputContext.setSymbol("BTCUSDT");

        assertReviewOnlyMissing(service.resolveEventSourceOwnership(noEventContext));
        assertReviewOnlyMissing(service.resolveEventSourceOwnership(uncertainContext));
        assertReviewOnlyMissing(service.resolveEventSourceOwnership(missingInputContext));

        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setEventSource("no-event-window");

        assertThat(sourceTrace.getEventSource()).isEqualTo("no-event-window");
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceEventSourceOwnershipService.class,
                FailClosedSourceTraceEventSourceOwnershipService.class,
                SourceTraceEventSourceOwnershipResult.class
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

    private void assertReviewOnlyMissing(SourceTraceEventSourceOwnershipResult result) {
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEventSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEventSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getEventSource()).isNull();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    private RuntimeKlineItemDTO klineItem(String closePrice) {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setClosePrice(new BigDecimal(closePrice));
        return item;
    }
}
