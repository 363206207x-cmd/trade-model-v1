package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.service.SourceTraceEntrySourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceEntrySourceOwnershipServiceTest {

    private final SourceTraceEntrySourceOwnershipService service =
            new FailClosedSourceTraceEntrySourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceEntrySourceOwnershipResult result = service.resolveEntrySourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceEntrySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEntrySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getEntryPriceSource()).isNull();
        assertThat(result.getEntrySourceType()).isNull();
        assertThat(result.getEntrySourceTimeframe()).isNull();
        assertThat(result.getEntrySourceReason()).isNull();
        assertThat(result.getEntrySourceRef()).isNull();
        assertThat(result.getMissingFields()).containsExactly(
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef"
        );
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeEntryOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceEntrySourceOwnershipResult result =
                service.resolveEntrySourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceEntrySourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEntrySourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getEntryPriceSource()).isNull();
        assertThat(result.getEntrySourceType()).isNull();
        assertThat(result.getEntrySourceTimeframe()).isNull();
        assertThat(result.getEntrySourceReason()).isNull();
        assertThat(result.getEntrySourceRef()).isNull();
        assertThat(result.getMissingFields()).contains(
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef"
        );
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceEntrySourceOwnershipService.class,
                FailClosedSourceTraceEntrySourceOwnershipService.class,
                SourceTraceEntrySourceOwnershipResult.class
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
