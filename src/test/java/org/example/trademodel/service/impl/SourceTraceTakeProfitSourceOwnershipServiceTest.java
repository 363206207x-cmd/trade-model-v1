package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceTakeProfitSourceReviewModeEnum;
import org.example.trademodel.service.SourceTraceTakeProfitSourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceTakeProfitSourceOwnershipServiceTest {

    private final SourceTraceTakeProfitSourceOwnershipService service =
            new FailClosedSourceTraceTakeProfitSourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceTakeProfitSourceOwnershipResult result = service.resolveTakeProfitSourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceTakeProfitSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceTakeProfitSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceTakeProfitSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getTpPriceSources()).isEmpty();
        assertThat(result.getTpSourceType()).isNull();
        assertThat(result.getTpSourceTimeframe()).isNull();
        assertThat(result.getTpSourceReason()).isNull();
        assertThat(result.getTpSourceRef()).isNull();
        assertThat(result.getMissingFields()).containsExactly(
                "tpPriceSources",
                "tpSourceType",
                "tpSourceTimeframe",
                "tpSourceReason",
                "tpSourceRef"
        );
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeTakeProfitOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceTakeProfitSourceOwnershipResult result =
                service.resolveTakeProfitSourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceTakeProfitSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceTakeProfitSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceTakeProfitSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getTpPriceSources()).isEmpty();
        assertThat(result.getTpSourceType()).isNull();
        assertThat(result.getTpSourceTimeframe()).isNull();
        assertThat(result.getTpSourceReason()).isNull();
        assertThat(result.getTpSourceRef()).isNull();
        assertThat(result.getMissingFields()).contains(
                "tpPriceSources",
                "tpSourceType",
                "tpSourceTimeframe",
                "tpSourceReason",
                "tpSourceRef"
        );
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void entryAndStopSkeletonsShouldNotBecomeTakeProfitOwnership() {
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                SourceTraceEntrySourceOwnershipResult.missingSource("BTCUSDT", "1m");
        SourceTraceStopSourceOwnershipResult stopOwnership =
                SourceTraceStopSourceOwnershipResult.missingSource("BTCUSDT", "1m");
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol(entryOwnership.getSymbol());
        runtimeKlineContext.setTimeframe(stopOwnership.getTimeframe());

        SourceTraceTakeProfitSourceOwnershipResult result =
                service.resolveTakeProfitSourceOwnership(runtimeKlineContext);

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(stopOwnership.getStopPriceSource()).isNull();
        assertThat(result.getTpPriceSources()).isEmpty();
        assertThat(result.getTpSourceType()).isNull();
        assertThat(result.getTpSourceReason()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceTakeProfitSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceTakeProfitSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceTakeProfitSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceTakeProfitSourceOwnershipService.class,
                FailClosedSourceTraceTakeProfitSourceOwnershipService.class,
                SourceTraceTakeProfitSourceOwnershipResult.class
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
