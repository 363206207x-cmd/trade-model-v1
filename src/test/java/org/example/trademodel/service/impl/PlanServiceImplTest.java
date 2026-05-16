package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanServiceImplTest {

    private final PlanServiceImpl service = new PlanServiceImpl();

    @Test
    void generateExecutionPlan_setsPlanModeNonNullAndWithinAllowedValues() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(false);

        ExecutionPlanVO plan = service.generateExecutionPlan(decision, null, null, null);

        assertThat(plan.getPlanMode()).isNotBlank();
        assertThat(plan.getPlanMode()).isIn(
                ExecutionPlanVO.PLAN_MODE_ADVISORY,
                ExecutionPlanVO.PLAN_MODE_SEMI_STRUCTURED
        );
    }

    @Test
    void generateExecutionPlan_withoutSourceTraceRemainsAdvisoryAndIncomplete() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(decision, null, null, null);

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(plan.getSourceTraceStatus()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(plan.getSourceTraceComplete()).isFalse();
        assertThat(plan.getNotExecutableReason()).isEqualTo("SOURCE_TRACE_MISSING");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
    }

    @Test
    void generateExecutionPlan_withIncompleteSourceTraceRemainsIncomplete() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        SourceTraceDTO sourceTrace = validSourceTrace();
        sourceTrace.setEntryPriceSource(null);
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        sourceTrace.setMissingFields(List.of("entryPriceSource"));

        ExecutionPlanVO plan = service.generateExecutionPlan(decision, null, null, null, sourceTrace);

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(plan.getSourceTraceComplete()).isFalse();
        assertThat(plan.getNotExecutableReason()).isEqualTo("SOURCE_TRACE_INCOMPLETE");
    }

    @Test
    void generateExecutionPlan_withCompleteSourceTraceIsReadyReviewOnlyButStillAdvisory() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(decision, null, null, null, validSourceTrace());

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_READY_REVIEW_ONLY);
        assertThat(plan.getSourceTraceStatus()).isEqualTo("VALID");
        assertThat(plan.getSourceTraceComplete()).isTrue();
        assertThat(plan.getNotExecutableReason()).isEqualTo("MANUAL_REVIEW_REQUIRED");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
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
}
