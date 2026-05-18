package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceRiskRewardSourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.service.SourceTraceRiskRewardSourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceRiskRewardSourceOwnershipServiceTest {

    private final SourceTraceRiskRewardSourceOwnershipService service =
            new FailClosedSourceTraceRiskRewardSourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceRiskRewardSourceOwnershipResult result = service.resolveRiskRewardSourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceRiskRewardSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceRiskRewardSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceRiskRewardSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getRrSource()).isNull();
        assertThat(result.getRrRuleRef()).isNull();
        assertThat(result.getMissingFields()).containsExactly(
                "rrSource",
                "rrRuleRef"
        );
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeRiskRewardOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceRiskRewardSourceOwnershipResult result =
                service.resolveRiskRewardSourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceRiskRewardSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceRiskRewardSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceRiskRewardSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getRrSource()).isNull();
        assertThat(result.getRrRuleRef()).isNull();
        assertThat(result.getMissingFields()).contains("rrSource", "rrRuleRef");
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void entryStopAndTakeProfitSkeletonsShouldNotBecomeRiskRewardOwnership() {
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                SourceTraceEntrySourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceStopSourceOwnershipResult stopOwnership =
                SourceTraceStopSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceTakeProfitSourceOwnershipResult takeProfitOwnership =
                SourceTraceTakeProfitSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol(entryOwnership.getSymbol());
        runtimeKlineContext.setTimeframe(takeProfitOwnership.getTimeframe());

        SourceTraceRiskRewardSourceOwnershipResult result =
                service.resolveRiskRewardSourceOwnership(runtimeKlineContext);

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(takeProfitOwnership.getTpPriceSources()).isEmpty();
        assertThat(result.getRrSource()).isNull();
        assertThat(result.getRrRuleRef()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceRiskRewardSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceRiskRewardSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceRiskRewardSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceRiskRewardSourceOwnershipService.class,
                FailClosedSourceTraceRiskRewardSourceOwnershipService.class,
                SourceTraceRiskRewardSourceOwnershipResult.class
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
