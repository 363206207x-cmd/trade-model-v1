package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DashboardDetailResponseVO;
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
        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getSourceGateComplete()).isFalse();
        assertThat(plan.getMissingSourceReasons()).contains("sourceTrace missing");
        assertThat(plan.getNotExecutableReason()).isEqualTo("SOURCE_TRACE_MISSING");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();
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
        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getMissingSourceReasons()).contains("sourceTrace missingFields present");
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
        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(plan.getSourceGateComplete()).isTrue();
        assertThat(plan.getSourceCompletenessSummary()).contains("source gate VALID");
        assertThat(plan.getNotExecutableReason()).isEqualTo("MANUAL_REVIEW_REQUIRED");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();
    }

    @Test
    void generateExecutionPlan_withCompleteSourceTraceAndReadyRiskGuardStaysReviewOnly() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                validSourceTrace(),
                readyRiskActionGuard()
        );

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_READY_REVIEW_ONLY);
        assertThat(plan.getRiskActionGuardReady()).isTrue();
        assertThat(plan.getRiskActionGuardBlockingReason()).isNull();
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
    }

    @Test
    void generateExecutionPlan_withStampedeRiskGuardFallsBackToWatchOnly() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setStampedeDetected(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                validSourceTrace(),
                risk
        );

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_WATCH_ONLY);
        assertThat(plan.getRiskActionGuardReady()).isFalse();
        assertThat(plan.getNotExecutableReason()).isEqualTo("STAMPEDE_RISK_REVIEW_ONLY");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
    }

    @Test
    void generateExecutionPlan_withLiquidityContextMissingFallsBackToWatchOnly() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setLiquidityState("BACKEND_PENDING");

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                validSourceTrace(),
                risk
        );

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_WATCH_ONLY);
        assertThat(plan.getRiskActionGuardReady()).isFalse();
        assertThat(plan.getNotExecutableReason()).isEqualTo("LIQUIDITY_CONTEXT_MISSING");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
    }

    @Test
    void generateExecutionPlan_withWickOnlyRiskFallsBackToWatchOnly() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setWickOnlyRisk(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                validSourceTrace(),
                risk
        );

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_WATCH_ONLY);
        assertThat(plan.getRiskActionGuardReady()).isFalse();
        assertThat(plan.getNotExecutableReason()).isEqualTo("WICK_ONLY_RISK_REVIEW_ONLY");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
    }

    @Test
    void generateExecutionPlan_withRiskActionGuardActionFlagFallsBackToWatchOnly() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = readyRiskActionGuard();
        risk.setOpportunityPushAllowed(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                validSourceTrace(),
                risk
        );

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_WATCH_ONLY);
        assertThat(plan.getRiskActionGuardReady()).isFalse();
        assertThat(plan.getNotExecutableReason()).isEqualTo("RISK_ACTION_GUARD_BLOCKED");
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

    private DashboardDetailResponseVO.RiskActionGuardDisplayVO readyRiskActionGuard() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO risk = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        risk.setRiskActionGuardStatus("MANUAL_REVIEW_REQUIRED");
        risk.setRiskActionBlockingReason("MANUAL_REVIEW_REQUIRED");
        risk.setLiquidityState("NORMAL");
        risk.setStampedeDetected(false);
        risk.setWickOnlyRisk(false);
        risk.setOpportunityPushAllowed(false);
        risk.setReverseTradeAllowed(false);
        risk.setNewPositionAllowed(false);
        risk.setMarketOrderExitAllowed(false);
        risk.setManualRiskReviewRequired(true);
        risk.setNotTradeInstruction(true);
        return risk;
    }
}
