package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceLiquiditySourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceLiquiditySourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceLiquiditySourceOwnershipServiceTest {

    private final SourceTraceLiquiditySourceOwnershipService service =
            new FailClosedSourceTraceLiquiditySourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceLiquiditySourceOwnershipResult result = service.resolveLiquiditySourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceLiquiditySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceLiquiditySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceLiquiditySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getLiquiditySource()).isNull();
        assertThat(result.getMissingFields()).containsExactly("liquiditySource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeLiquidityOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceLiquiditySourceOwnershipResult result =
                service.resolveLiquiditySourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceLiquiditySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceLiquiditySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceLiquiditySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getLiquiditySource()).isNull();
        assertThat(result.getMissingFields()).contains("liquiditySource");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void quoteLatestPriceShouldNotBecomeLiquidityOwnership() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1m");
        sourceTrace.setQuoteLatestPrice(new BigDecimal("102.30"));
        sourceTrace.setQuoteLatestPriceSource("DecisionResultVO.latestPrice");

        SourceTraceLiquiditySourceOwnershipResult result = service.resolveLiquiditySourceOwnership(null);

        assertThat(sourceTrace.getQuoteLatestPrice()).isEqualByComparingTo("102.30");
        assertThat(sourceTrace.getQuoteLatestPriceSource()).isEqualTo("DecisionResultVO.latestPrice");
        assertThat(sourceTrace.getLiquiditySource()).isNull();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(result.getLiquiditySource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceLiquiditySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceLiquiditySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceLiquiditySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void entryStopTakeProfitAndRiskRewardSkeletonsShouldNotBecomeLiquidityOwnership() {
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                SourceTraceEntrySourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceStopSourceOwnershipResult stopOwnership =
                SourceTraceStopSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                SourceTraceTakeProfitSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceRiskRewardSourceOwnershipResult riskRewardOwnership =
                SourceTraceRiskRewardSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol(entryOwnership.getSymbol());
        runtimeKlineContext.setTimeframe(riskRewardOwnership.getTimeframe());

        SourceTraceLiquiditySourceOwnershipResult result =
                service.resolveLiquiditySourceOwnership(runtimeKlineContext);

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(riskRewardOwnership.getRrSource()).isNull();
        assertThat(riskRewardOwnership.getRrRuleRef()).isNull();
        assertThat(result.getLiquiditySource()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceLiquiditySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceLiquiditySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceLiquiditySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceLiquiditySourceOwnershipService.class,
                FailClosedSourceTraceLiquiditySourceOwnershipService.class,
                SourceTraceLiquiditySourceOwnershipResult.class
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

    private RuntimeKlineItemDTO klineItem(String closePrice) {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setClosePrice(new BigDecimal(closePrice));
        return item;
    }
}
