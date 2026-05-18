package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceStopSourceReviewModeEnum;
import org.example.trademodel.service.SourceTraceStopSourceOwnershipService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTraceStopSourceOwnershipServiceTest {

    private final SourceTraceStopSourceOwnershipService service =
            new FailClosedSourceTraceStopSourceOwnershipService();

    @Test
    void defaultResultShouldFailClosedAsMissingSourceReviewOnly() {
        SourceTraceStopSourceOwnershipResult result = service.resolveStopSourceOwnership(null);

        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceStopSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceStopSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceStopSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getStopPriceSource()).isNull();
        assertThat(result.getStopSourceType()).isNull();
        assertThat(result.getStopSourceTimeframe()).isNull();
        assertThat(result.getStopSourceReason()).isNull();
        assertThat(result.getStopSourceRef()).isNull();
        assertThat(result.getMissingFields()).containsExactly(
                "stopPriceSource",
                "stopSourceType",
                "stopSourceTimeframe",
                "stopSourceReason",
                "stopSourceRef"
        );
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void runtimeLatestPriceAndKlineItemsShouldNotBecomeStopOwnership() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("1m");
        runtimeKlineContext.setLatestPrice(new BigDecimal("102.30"));
        runtimeKlineContext.setKlineItems(List.of(klineItem("102.30"), klineItem("101.10")));
        runtimeKlineContext.setDataQualityScore(BigDecimal.valueOf(90));
        runtimeKlineContext.setManualReviewRequired(true);
        runtimeKlineContext.setNotTradeInstruction(true);

        SourceTraceStopSourceOwnershipResult result =
                service.resolveStopSourceOwnership(runtimeKlineContext);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1m");
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceStopSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceStopSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceStopSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getStopPriceSource()).isNull();
        assertThat(result.getStopSourceType()).isNull();
        assertThat(result.getStopSourceTimeframe()).isNull();
        assertThat(result.getStopSourceReason()).isNull();
        assertThat(result.getStopSourceRef()).isNull();
        assertThat(result.getMissingFields()).contains(
                "stopPriceSource",
                "stopSourceType",
                "stopSourceTimeframe",
                "stopSourceReason",
                "stopSourceRef"
        );
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void entrySourceSkeletonShouldNotBecomeStopOwnership() {
        SourceTraceEntrySourceOwnershipResult entryOwnership =
                SourceTraceEntrySourceOwnershipResult.missingSource("BTCUSDT", "1m");
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol(entryOwnership.getSymbol());
        runtimeKlineContext.setTimeframe(entryOwnership.getTimeframe());

        SourceTraceStopSourceOwnershipResult result =
                service.resolveStopSourceOwnership(runtimeKlineContext);

        assertThat(entryOwnership.getEntryPriceSource()).isNull();
        assertThat(result.getStopPriceSource()).isNull();
        assertThat(result.getStopSourceType()).isNull();
        assertThat(result.getStopSourceReason()).isNull();
        assertThat(result.getOwnershipStatus()).isEqualTo(SourceTraceStopSourceOwnershipStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceStopSourceMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceStopSourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void skeletonShouldNotExposeTradingExecutionMethods() {
        List<Class<?>> types = List.of(
                SourceTraceStopSourceOwnershipService.class,
                FailClosedSourceTraceStopSourceOwnershipService.class,
                SourceTraceStopSourceOwnershipResult.class
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
