package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.MarketStructureBoundaryDTO;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryRequest;
import org.example.trademodel.dto.planboundary.MarketStructureTakeProfitTargetDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketStructureBoundaryExtractorImplTest {

    private static final long ONE_MINUTE_MS = 60_000L;

    private final MarketStructureBoundaryExtractorImpl extractor =
            new MarketStructureBoundaryExtractorImpl();

    @Test
    void freshContiguousOhlcvWithLongDirectionProducesSourceOwnedBoundaries() {
        MarketStructureBoundaryDTO result = extractor.extract(request("LONG", longBars()));

        assertThat(result.isBoundaryReady()).isTrue();
        assertThat(result.getBlockingReasons()).isEmpty();
        assertThat(result.getSupportLevels()).isNotEmpty();
        assertThat(result.getResistanceLevels()).isNotEmpty();
        assertThat(result.getEntrySourceType()).isEqualTo("SUPPORT_RETEST");
        assertThat(result.getEntrySourceRef()).contains("MARKET_STRUCTURE:SUPPORT");
        assertThat(result.getEntryLower()).isLessThan(result.getEntryUpper());
        assertThat(result.getStopSourceType()).isEqualTo("SWING_LOW_STRUCTURE_BUFFER");
        assertThat(result.getStopSourceRef()).isEqualTo(result.getEntrySourceRef());
        assertThat(result.getStopPrice()).isLessThan(result.getEntryLower());
        assertThat(result.getTakeProfitTargets()).hasSize(1);
        assertThat(result.getTakeProfitTargets().get(0).getTargetType()).isEqualTo("STRUCTURE_RESISTANCE");
        assertThat(result.getTakeProfitTargets().get(0).getSourceRef()).contains("MARKET_STRUCTURE:RESISTANCE");
        assertThat(result.getRrRatio()).isPositive();
        assertThat(result.getSourceRefs()).hasSize(4);
        assertThat(result.getSourceRefs()).allSatisfy(source -> {
            assertThat(source.getSourceType()).isNotBlank();
            assertThat(source.getSourceId()).isNotBlank();
            assertThat(source.getProvider()).isEqualTo("LOCAL_FIXTURE");
            assertThat(source.getAnalysisId()).isEqualTo("analysis-boundary-test");
        });
        assertReviewOnlySafety(result);
    }

    @Test
    void freshContiguousOhlcvWithShortDirectionProducesSourceOwnedBoundaries() {
        MarketStructureBoundaryDTO result = extractor.extract(request("SHORT", shortBars()));

        assertThat(result.isBoundaryReady()).isTrue();
        assertThat(result.getBlockingReasons()).isEmpty();
        assertThat(result.getEntrySourceType()).isEqualTo("RESISTANCE_RETEST");
        assertThat(result.getEntrySourceRef()).contains("MARKET_STRUCTURE:RESISTANCE");
        assertThat(result.getStopSourceType()).isEqualTo("SWING_HIGH_STRUCTURE_BUFFER");
        assertThat(result.getStopPrice()).isGreaterThan(result.getEntryUpper());
        assertThat(result.getTakeProfitTargets()).hasSize(1);
        assertThat(result.getTakeProfitTargets().get(0).getTargetType()).isEqualTo("STRUCTURE_SUPPORT");
        assertThat(result.getTakeProfitTargets().get(0).getTargetPrice()).isLessThan(result.getEntryLower());
        assertThat(result.getRrRatio()).isPositive();
        assertReviewOnlySafety(result);
    }

    @Test
    void noBarsFailsClosed() {
        MarketStructureBoundaryRequest request = request("LONG", List.of());

        MarketStructureBoundaryDTO result = extractor.extract(request);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getBlockingReasons()).contains(
                MarketStructureBoundaryExtractorImpl.REASON_OHLCV_MISSING);
        assertThat(result.getTakeProfitTargets()).isEmpty();
    }

    @Test
    void staleBarsFailClosedWhenFreshnessPolicyIsProvided() {
        MarketStructureBoundaryRequest request = request("LONG", longBars());
        request.setGeneratedAtEpochMs(longBars().get(longBars().size() - 1).getCloseTimeMs() + (10 * ONE_MINUTE_MS));
        request.setFreshnessLimitMs(ONE_MINUTE_MS);

        MarketStructureBoundaryDTO result = extractor.extract(request);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getFreshnessStatus()).isEqualTo("STALE");
        assertThat(result.getBlockingReasons()).contains(
                MarketStructureBoundaryExtractorImpl.REASON_OHLCV_STALE);
    }

    @Test
    void partialWindowFailsClosed() {
        MarketStructureBoundaryRequest request = request("LONG", longBars().subList(0, 3));
        request.setMinBars(7);

        MarketStructureBoundaryDTO result = extractor.extract(request);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getBlockingReasons()).contains(
                MarketStructureBoundaryExtractorImpl.REASON_OHLCV_INSUFFICIENT_BARS);
    }

    @Test
    void latestPriceOnlyCannotBecomeBoundarySource() {
        MarketStructureBoundaryRequest request = request("LONG", List.of(bar(0, "105", "98", "102")));
        request.setMinBars(1);

        MarketStructureBoundaryDTO result = extractor.extract(request);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getEntrySourceRef()).isNull();
        assertThat(result.getStopSourceRef()).isNull();
        assertThat(result.getTakeProfitTargets()).isEmpty();
        assertThat(result.getBlockingReasons()).contains(
                MarketStructureBoundaryExtractorImpl.REASON_STRUCTURE_LEVEL_MISSING);
    }

    @Test
    void pushFieldsAreNotExtractorInputsOrBoundarySources() {
        List<String> requestFields = Arrays.stream(MarketStructureBoundaryRequest.class.getDeclaredFields())
                .map(Field::getName)
                .map(String::toLowerCase)
                .toList();
        List<String> resultFields = Arrays.stream(MarketStructureBoundaryDTO.class.getDeclaredFields())
                .map(Field::getName)
                .map(String::toLowerCase)
                .toList();

        assertThat(requestFields).noneMatch(name -> name.contains("push"));
        assertThat(resultFields).noneMatch(name -> name.contains("push"));
    }

    @Test
    void missingStructureTargetFailsClosedWhenRrLadderIsNotAllowed() {
        MarketStructureBoundaryRequest request = request("LONG", supportOnlyBars());
        request.setAllowRrLadder(false);

        MarketStructureBoundaryDTO result = extractor.extract(request);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getBlockingReasons()).contains(
                MarketStructureBoundaryExtractorImpl.REASON_TP_SOURCE_MISSING);
        assertThat(result.getTakeProfitTargets()).isEmpty();
    }

    @Test
    void explicitRrLadderCanCreateReviewOnlyTargetFromEntryAndStopSources() {
        MarketStructureBoundaryRequest request = request("LONG", supportOnlyBars());
        request.setAllowRrLadder(true);

        MarketStructureBoundaryDTO result = extractor.extract(request);

        assertThat(result.isBoundaryReady()).isTrue();
        assertThat(result.getTakeProfitTargets()).hasSize(1);
        MarketStructureTakeProfitTargetDTO target = result.getTakeProfitTargets().get(0);
        assertThat(target.getTargetType()).isEqualTo("RR_LADDER");
        assertThat(target.getRuleRef()).isEqualTo("RR_LADDER");
        assertThat(target.getSourceRef()).contains(result.getEntrySourceRef(), result.getStopSourceRef());
        assertThat(target.getRr()).isEqualByComparingTo("2.00");
        assertThat(result.getRrRatio()).isEqualByComparingTo("2.0000");
        assertReviewOnlySafety(result);
    }

    @Test
    void conflictingDirectionFailsClosed() {
        MarketStructureBoundaryDTO result = extractor.extract(request("MIXED", longBars()));

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getBlockingReasons()).contains(
                MarketStructureBoundaryExtractorImpl.REASON_DIRECTION_CONFLICTING);
    }

    @Test
    void riskActionGuardBlockedFailsClosed() {
        MarketStructureBoundaryRequest request = request("LONG", longBars());
        request.setRiskActionGuardBlocked(true);
        request.setRiskActionGuardReason("WICK_ONLY_RISK_REVIEW_ONLY");

        MarketStructureBoundaryDTO result = extractor.extract(request);

        assertThat(result.isBoundaryReady()).isFalse();
        assertThat(result.getBlockingReasons()).contains(
                MarketStructureBoundaryExtractorImpl.REASON_RISK_GUARD_BLOCKED,
                "WICK_ONLY_RISK_REVIEW_ONLY");
    }

    @Test
    void extractorHasNoProviderSchedulerPositionOrDeliveryDependencies() {
        String implementation = MarketStructureBoundaryExtractorImpl.class.getName().toLowerCase();

        assertThat(implementation).doesNotContain("provider");
        assertThat(implementation).doesNotContain("scheduler");
        assertThat(implementation).doesNotContain("userposition");
        assertThat(implementation).doesNotContain("telegram");
        assertThat(implementation).doesNotContain("push");
        assertThat(implementation).doesNotContain("recheck");
    }

    private MarketStructureBoundaryRequest request(String direction, List<RuntimeKlineItemDTO> bars) {
        MarketStructureBoundaryRequest request = new MarketStructureBoundaryRequest();
        request.setSymbol("BTCUSDT");
        request.setAnalysisId("analysis-boundary-test");
        request.setDirection(direction);
        request.setTimeframe("1m");
        request.setGeneratedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        request.setGeneratedAtEpochMs(bars.isEmpty() ? 0L : bars.get(bars.size() - 1).getCloseTimeMs() + 1_000L);
        request.setFreshnessLimitMs(5 * ONE_MINUTE_MS);
        request.setBars(bars);
        request.setMinBars(7);
        request.setMaxTargets(1);
        request.setAllowRrLadder(false);
        request.setLeverageSuggestion("low_leverage");
        return request;
    }

    private List<RuntimeKlineItemDTO> longBars() {
        return bars(
                new String[][] {
                        {"106", "99", "104"},
                        {"107", "98", "101"},
                        {"103", "95", "99"},
                        {"105", "96", "102"},
                        {"108", "97", "106"},
                        {"115", "103", "110"},
                        {"112", "104", "106"},
                        {"109", "102", "105"},
                        {"108", "101", "104"}
                });
    }

    private List<RuntimeKlineItemDTO> shortBars() {
        return bars(
                new String[][] {
                        {"101", "94", "96"},
                        {"103", "95", "102"},
                        {"112", "99", "108"},
                        {"106", "100", "101"},
                        {"107", "96", "98"},
                        {"100", "88", "90"},
                        {"98", "89", "94"},
                        {"99", "90", "95"},
                        {"101", "92", "96"}
                });
    }

    private List<RuntimeKlineItemDTO> supportOnlyBars() {
        return bars(
                new String[][] {
                        {"101", "99", "100"},
                        {"102", "98", "101"},
                        {"103", "95", "99"},
                        {"104", "96", "102"},
                        {"105", "97", "103"},
                        {"106", "98", "104"},
                        {"107", "99", "105"},
                        {"108", "100", "106"},
                        {"109", "101", "108"}
                });
    }

    private List<RuntimeKlineItemDTO> bars(String[][] rows) {
        List<RuntimeKlineItemDTO> bars = new java.util.ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            bars.add(bar(i, rows[i][0], rows[i][1], rows[i][2]));
        }
        return bars;
    }

    private RuntimeKlineItemDTO bar(int index, String high, String low, String close) {
        RuntimeKlineItemDTO bar = new RuntimeKlineItemDTO();
        long openTime = index * ONE_MINUTE_MS;
        bar.setOpenTimeMs(openTime);
        bar.setCloseTimeMs(openTime + ONE_MINUTE_MS);
        bar.setOpenPrice(new BigDecimal(close));
        bar.setHighPrice(new BigDecimal(high));
        bar.setLowPrice(new BigDecimal(low));
        bar.setClosePrice(new BigDecimal(close));
        bar.setVolume(new BigDecimal("100.00"));
        bar.setProvider("LOCAL_FIXTURE");
        bar.setProviderMarketType("SPOT");
        bar.setSourceEndpoint("market-structure-boundary-fixture");
        bar.setSourceBatchId("batch-" + index);
        bar.setSourceTraceId("trace-" + index);
        bar.setSourceVersion(1);
        bar.setQualityStatus("OK");
        return bar;
    }

    private void assertReviewOnlySafety(MarketStructureBoundaryDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
        assertThat(result.getPositionSizingStatus()).isEqualTo("POSITION_SIZING_NOT_PRODUCED");
    }
}
