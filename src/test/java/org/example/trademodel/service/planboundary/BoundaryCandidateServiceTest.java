package org.example.trademodel.service.planboundary;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.BoundaryStatusEnum;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextStatusEnum;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BoundaryCandidateServiceTest {
    private final BoundaryCandidateService service = new BoundaryCandidateServiceImpl();

    @Test
    void nullRuntimeKlineContextReturnsIncomplete() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                null,
                new BigDecimal("100"),
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void missingRuntimeKlineContextReturnsIncomplete() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                RuntimeKlineContextDTO.missing("BTCUSDT", "15m", "MISSING_KLINE_WINDOW"),
                new BigDecimal("100"),
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void staleRuntimeKlineContextReturnsIncomplete() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "ETHUSDT",
                "1h",
                RuntimeKlineContextDTO.stale("ETHUSDT", "1h", "KLINE_STALE"),
                new BigDecimal("100"),
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void unknownStatusReturnsIncomplete() {
        RuntimeKlineContextDTO context = completeFreshContext();
        context.setStaleStatus(RuntimeKlineContextStatusEnum.UNKNOWN);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "SOLUSDT",
                "4h",
                context,
                new BigDecimal("100"),
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void nullLatestPriceReturnsIncomplete() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                completeFreshContext(),
                null,
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void nonPositiveLatestPriceReturnsIncomplete() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                completeFreshContext(),
                BigDecimal.ZERO,
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void nullDataQualityScoreReturnsIncomplete() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                completeFreshContext(),
                new BigDecimal("100"),
                null
        );

        assertIncomplete(candidate);
    }

    @Test
    void lowDataQualityScoreReturnsIncomplete() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                completeFreshContext(),
                new BigDecimal("100"),
                new BigDecimal("69.99")
        );

        assertIncomplete(candidate);
    }

    @Test
    void missingOhlcvReturnsIncomplete() {
        RuntimeKlineContextDTO context = RuntimeKlineContextDTO.fresh("BTCUSDT", "15m");
        context.setKlineItems(List.of(completeKlineItem()));

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                context,
                new BigDecimal("100"),
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void missingKlineItemsReturnsIncomplete() {
        RuntimeKlineContextDTO context = completeFreshContext();
        context.setKlineItems(List.of());

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                context,
                new BigDecimal("100"),
                new BigDecimal("80")
        );

        assertIncomplete(candidate);
    }

    @Test
    void completeFreshContextReturnsWatchOnlyBecauseValidFactoryIsDeferred() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "15m",
                completeFreshContext(),
                new BigDecimal("100"),
                new BigDecimal("80")
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
    }

    @Test
    void serviceInterfaceDoesNotExposeActionMethodNames() {
        List<String> methodNames = Stream.of(BoundaryCandidateService.class.getDeclaredMethods())
                .map(Method::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        assertThat(methodNames).noneMatch(name -> name.contains("order"));
        assertThat(methodNames).noneMatch(name -> name.contains("trade"));
        assertThat(methodNames).noneMatch(name -> name.contains("execute"));
        assertThat(methodNames).noneMatch(name -> name.contains("close"));
        assertThat(methodNames).noneMatch(name -> name.contains("reverse"));
    }

    private void assertIncomplete(BoundaryCandidateDTO candidate) {
        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
    }

    private RuntimeKlineContextDTO completeFreshContext() {
        RuntimeKlineContextDTO context = RuntimeKlineContextDTO.fresh("BTCUSDT", "15m");
        context.setLatestOpen(new BigDecimal("100.00"));
        context.setLatestHigh(new BigDecimal("102.00"));
        context.setLatestLow(new BigDecimal("99.00"));
        context.setLatestClose(new BigDecimal("101.00"));
        context.setLatestVolume(new BigDecimal("1000.00"));
        context.setKlineItems(List.of(completeKlineItem()));
        return context;
    }

    private RuntimeKlineItemDTO completeKlineItem() {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setOpen(new BigDecimal("100.00"));
        item.setHigh(new BigDecimal("102.00"));
        item.setLow(new BigDecimal("99.00"));
        item.setClose(new BigDecimal("101.00"));
        item.setVolume(new BigDecimal("1000.00"));
        return item;
    }
}
