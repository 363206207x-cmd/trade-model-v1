package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceFieldsDTO;
import org.example.trademodel.dto.planboundary.BoundaryStatusEnum;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoundaryCandidateServiceImplTest {

    private final BoundaryCandidateServiceImpl service = new BoundaryCandidateServiceImpl();

    @Test
    void evaluateBoundaryCandidateReturnsIncompleteWhenSourceTraceMissing() {
        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                null,
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("sourceTrace missing");
    }

    @Test
    void evaluateBoundaryCandidateReturnsWatchOnlyWhenSourceTraceRequestsWatchOnlyFallback() {
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        sourceTrace.setMissingFields(List.of("eventSource"));

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                sourceTrace,
                validEntry(),
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.WATCH_ONLY);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("sourceTrace fallbackStatus=WATCH_ONLY");
    }

    @Test
    void evaluateBoundaryCandidateReturnsIncompleteWhenBoundarySourcesAreMissing() {
        BoundaryEntryDTO entry = validEntry();
        entry.setNumericSourceValue(null);

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                entry,
                validStop(),
                List.of(validTakeProfitLevel()),
                validSourceFields(),
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.INCOMPLETE);
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).contains("entry numeric source value missing");
    }

    @Test
    void evaluateBoundaryCandidateReturnsValidWhenAllSourcesAreTraceable() {
        BoundaryEntryDTO entry = validEntry();
        BoundaryStopDTO stop = validStop();
        BoundaryTakeProfitLevelDTO takeProfit = validTakeProfitLevel();
        BoundarySourceFieldsDTO sourceFields = validSourceFields();

        BoundaryCandidateDTO candidate = service.evaluateBoundaryCandidate(
                "BTCUSDT",
                "1h",
                validSourceTrace(),
                entry,
                stop,
                List.of(takeProfit),
                sourceFields,
                BigDecimal.valueOf(90)
        );

        assertThat(candidate.getBoundaryStatus()).isEqualTo(BoundaryStatusEnum.VALID);
        assertThat(candidate.getEntry()).isEqualTo(entry);
        assertThat(candidate.getStop()).isEqualTo(stop);
        assertThat(candidate.getTakeProfitLevels()).containsExactly(takeProfit);
        assertThat(candidate.getSourceFields()).isEqualTo(sourceFields);
        assertThat(candidate.getDataQualityScore()).isEqualByComparingTo("90");
        assertThat(candidate.isManualReviewRequired()).isTrue();
        assertThat(candidate.isNotTradeInstruction()).isTrue();
        assertThat(candidate.getBlockingReasons()).isEmpty();
    }

    @Test
    void serviceShouldNotExposeTradingExecutionMethods() {
        List<String> methodNames = List.of(BoundaryCandidateServiceImpl.class.getDeclaredMethods())
                .stream()
                .map(Method::getName)
                .map(String::toLowerCase)
                .toList();

        assertThat(methodNames).noneMatch(name -> name.contains("execute"));
        assertThat(methodNames).noneMatch(name -> name.contains("order"));
        assertThat(methodNames).noneMatch(name -> name.contains("close"));
        assertThat(methodNames).noneMatch(name -> name.contains("reverse"));
    }

    private SourceTraceDTO validSourceTrace() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setEntryPriceSource(BigDecimal.valueOf(68000));
        sourceTrace.setEntrySourceType("support");
        sourceTrace.setEntrySourceTimeframe("1h");
        sourceTrace.setEntrySourceReason("support retest");
        sourceTrace.setEntrySourceRef("entry-1");
        sourceTrace.setStopPriceSource(BigDecimal.valueOf(66800));
        sourceTrace.setStopSourceType("swing_low");
        sourceTrace.setStopSourceTimeframe("1h");
        sourceTrace.setStopSourceReason("recent swing low");
        sourceTrace.setStopSourceRef("stop-1");
        sourceTrace.setTpPriceSources(List.of(BigDecimal.valueOf(70400)));
        sourceTrace.setTpSourceType("rr_ladder");
        sourceTrace.setTpSourceTimeframe("1h");
        sourceTrace.setTpSourceReason("2R target");
        sourceTrace.setTpSourceRef("tp-1");
        sourceTrace.setRrSource(BigDecimal.valueOf(2));
        sourceTrace.setRrRuleRef("min_rr_2");
        sourceTrace.setLiquiditySource("liquidity-ok");
        sourceTrace.setMultiTimeframeSource("multi-timeframe-aligned");
        sourceTrace.setEventSource("no-event-window");
        sourceTrace.setWickSource("wick-confirmed");
        return sourceTrace;
    }

    private BoundaryEntryDTO validEntry() {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryType("pullback");
        entry.setEntryPrice(BigDecimal.valueOf(68000));
        entry.setEntryZoneLow(BigDecimal.valueOf(67800));
        entry.setEntryZoneHigh(BigDecimal.valueOf(68200));
        entry.setNumericSourceType("support");
        entry.setNumericSourceValue(BigDecimal.valueOf(68000));
        entry.setSourceTimeframe("1h");
        entry.setReason("support retest");
        return entry;
    }

    private BoundaryStopDTO validStop() {
        BoundaryStopDTO stop = new BoundaryStopDTO();
        stop.setStopType("structure_invalidated");
        stop.setStopPrice(BigDecimal.valueOf(66800));
        stop.setStopZoneLow(BigDecimal.valueOf(66600));
        stop.setStopZoneHigh(BigDecimal.valueOf(67000));
        stop.setNumericSourceType("swing_low");
        stop.setNumericSourceValue(BigDecimal.valueOf(66800));
        stop.setSourceTimeframe("1h");
        stop.setReason("recent swing low");
        return stop;
    }

    private BoundaryTakeProfitLevelDTO validTakeProfitLevel() {
        BoundaryTakeProfitLevelDTO takeProfit = new BoundaryTakeProfitLevelDTO();
        takeProfit.setLevel(1);
        takeProfit.setPrice(BigDecimal.valueOf(70400));
        takeProfit.setRr(BigDecimal.valueOf(2));
        takeProfit.setSource("rr_ladder");
        takeProfit.setNumericSourceType("rr_ladder");
        takeProfit.setNumericSourceValue(BigDecimal.valueOf(70400));
        takeProfit.setSourceTimeframe("1h");
        takeProfit.setSourceRef("tp-1");
        takeProfit.setPartialRatio(BigDecimal.valueOf(0.5));
        takeProfit.setAllocationRatio(BigDecimal.valueOf(0.5));
        takeProfit.setReason("2R target");
        return takeProfit;
    }

    private BoundarySourceFieldsDTO validSourceFields() {
        BoundarySourceFieldsDTO sourceFields = new BoundarySourceFieldsDTO();
        sourceFields.setEntrySourceField("supportLevel");
        sourceFields.setStopSourceField("swingLow");
        sourceFields.setTakeProfitSourceField("rrLadder");
        sourceFields.setRrRule("min_rr_2");
        sourceFields.setDataSource("sourceTrace");
        sourceFields.setDataQualityScore(BigDecimal.valueOf(90));
        sourceFields.setEvidenceRefs(List.of("source-trace-1"));
        return sourceFields;
    }
}
