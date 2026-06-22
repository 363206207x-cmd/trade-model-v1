package org.example.trademodel.dto.planboundary;

import org.example.trademodel.service.impl.PlanServiceImpl;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionPlanSourceGateTest {

    @Test
    void completeSourcesAllowValid() {
        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(validSourceTrace());

        assertThat(result.getStatus()).isEqualTo(ExecutionPlanSourceGateResultDTO.STATUS_VALID);
        assertThat(result.isSourceComplete()).isTrue();
        assertThat(result.getMissingSourceReasons()).isEmpty();
        assertThat(result.getBlockerReasons()).isEmpty();
    }

    @Test
    void missingEntrySourceBlocksValid() {
        assertMissing(trace -> trace.setEntryPriceSource(null), "entry source missing");
    }

    @Test
    void missingStopSourceBlocksValid() {
        assertMissing(trace -> trace.setStopPriceSource(null), "stop source missing");
    }

    @Test
    void missingTakeProfitSourceBlocksValid() {
        assertMissing(trace -> trace.setTpPriceSources(List.of()), "TP source missing");
    }

    @Test
    void missingRiskRewardSourceBlocksValid() {
        assertMissing(trace -> trace.setRrSource(null), "RR source missing");
    }

    @Test
    void missingLiquiditySourceBlocksValid() {
        assertMissing(trace -> trace.setLiquiditySource(null), "liquidity source missing");
    }

    @Test
    void missingWickConfirmationSourceBlocksValid() {
        assertMissing(trace -> trace.setWickSource(null), "wick confirmation source missing");
    }

    @Test
    void missingMultiTimeframeSourceBlocksValid() {
        assertMissing(trace -> trace.setMultiTimeframeSource(null), "multi-timeframe source missing");
    }

    @Test
    void missingEventWindowBlockerSourceBlocksValid() {
        assertMissing(trace -> trace.setEventSource(null), "event window source missing");
    }

    @Test
    void missingSourceTimeframeBlocksValid() {
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setTimeframe(null);
        sourceTrace.setSourceTimeframe(null);

        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(sourceTrace);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMissingSourceReasons()).contains("source timeframe missing");
    }

    @Test
    void missingSourceReasonBlocksValid() {
        assertMissing(trace -> trace.setEntrySourceReason(null), "entry source reason missing");
    }

    @Test
    void fallbackCannotBecomeValid() {
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);

        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(sourceTrace);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo(ExecutionPlanSourceGateResultDTO.STATUS_BLOCKED);
    }

    @Test
    void incompleteCannotBecomeValid() {
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        sourceTrace.setMissingFields(List.of("entryPriceSource"));

        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(sourceTrace);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo(ExecutionPlanSourceGateResultDTO.STATUS_INCOMPLETE);
        assertThat(result.getMissingSourceReasons()).contains("sourceTrace missingFields present");
    }

    @Test
    void reviewOnlyCannotBecomeValid() {
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.WATCH_ONLY);

        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(sourceTrace);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo(ExecutionPlanSourceGateResultDTO.STATUS_REVIEW_ONLY);
    }

    @Test
    void aiOnlyClaimCannotBecomeValid() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setSourceOwner("AI_ONLY_CLAIM");
        sourceTrace.setSourceRef("llm-summary-without-evidence");

        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(sourceTrace);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMissingSourceReasons()).contains(
                "entry source missing",
                "stop source missing",
                "TP source missing",
                "RR source missing"
        );
    }

    @Test
    void numericValuesWithoutSourceCannotBecomeValid() {
        BoundaryEntryDTO entry = validEntry();
        entry.setNumericSourceType(null);
        BoundaryStopDTO stop = validStop();
        BoundaryTakeProfitLevelDTO takeProfit = validTakeProfitLevel();

        ExecutionPlanSourceGateResultDTO result = BoundaryCandidateSourceGate.validate(
                entry,
                stop,
                List.of(takeProfit),
                validSourceFields()
        );

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMissingSourceReasons()).contains("entry numeric source type missing");
        assertThatThrownBy(() -> BoundaryCandidateDTO.valid(
                "BTCUSDT",
                "1h",
                entry,
                stop,
                List.of(takeProfit),
                validSourceFields(),
                BigDecimal.valueOf(90)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BoundaryCandidateSourceGate must be VALID");
    }

    @Test
    void validOutputStillHasSafetyFlags() {
        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(validSourceTrace());

        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
    }

    @Test
    void validOutputDoesNotCreateUserPosition() {
        ExecutionPlanVO plan = validPlanFromService();

        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(plan.getNotUserPositionCreation()).isTrue();
    }

    @Test
    void validOutputDoesNotExecuteOrder() {
        ExecutionPlanVO plan = validPlanFromService();

        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
    }

    @Test
    void serviceExposesSourceGateResult() {
        ExecutionPlanVO plan = validPlanFromService();

        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(plan.getSourceGateComplete()).isTrue();
        assertThat(plan.getMissingSourceReasons()).isEmpty();
        assertThat(plan.getSourceCompletenessSummary()).contains("source gate VALID");
    }

    private void assertMissing(Consumer<SourceTraceDTO> mutation, String expectedReason) {
        SourceTraceDTO sourceTrace = validSourceTrace();
        mutation.accept(sourceTrace);

        ExecutionPlanSourceGateResultDTO result = ExecutionPlanSourceGate.validate(sourceTrace);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo(ExecutionPlanSourceGateResultDTO.STATUS_INCOMPLETE);
        assertThat(result.getMissingSourceReasons()).contains(expectedReason);
    }

    private ExecutionPlanVO validPlanFromService() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        return new PlanServiceImpl().generateExecutionPlan(decision, null, null, null, validSourceTrace());
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
