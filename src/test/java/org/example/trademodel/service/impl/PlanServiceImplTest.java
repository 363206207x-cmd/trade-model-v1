package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.SourceTraceBoundaryProducerResult;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanServiceImplTest {

    private final PlanServiceImpl service = new PlanServiceImpl();

    @Test
    void buildRuleExecutionAssessmentNeverCreatesDetailedCandidateOrFinalFields() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        SourceTraceBoundaryProducerResult boundary = readyBoundaryOnlyResult();
        boundary.setSourceTrace(validSourceTrace());
        boundary.setSourceTraceReady(true);
        boundary.setMissingFields(List.of());
        boundary.setBlockingReasons(List.of());

        ExecutionPlanVO assessment = service.buildRuleExecutionAssessment(decision, boundary);

        assertThat(assessment.getChainStatus()).isEqualTo("RULE_BASE_ASSESSMENT");
        assertThat(assessment.getFinalPlan()).isFalse();
        assertThat(assessment.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(assessment.getSourceGateComplete()).isTrue();
        assertThat(assessment.getExecutionPlanStatus())
                .isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(assessment.getRecommendedAction()).isNull();
        assertThat(assessment.getEntryZone()).isNull();
        assertThat(assessment.getStopLoss()).isNull();
        assertThat(assessment.getTakeProfitRules()).isNull();
        assertThat(assessment.getLeverageSuggestion()).isNull();
        assertThat(assessment.getPositionSuggestion()).isNull();
        assertThat(assessment.getInvalidCondition()).isNull();
        assertThat(assessment.getNotAutoTrading()).isTrue();
        assertThat(assessment.getNotOrderExecution()).isTrue();
        assertThat(assessment.getNotUserPositionCreation()).isTrue();
    }

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
    void generateExecutionPlan_withCompleteSourceTraceButNoBoundaryProducerIsIncomplete() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(decision, null, null, null, validSourceTrace());

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(plan.getSourceTraceStatus()).isEqualTo("VALID");
        assertThat(plan.getSourceTraceComplete()).isTrue();
        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(plan.getSourceGateComplete()).isTrue();
        assertThat(plan.getSourceCompletenessSummary()).contains("source gate VALID");
        assertThat(plan.getNotExecutableReason()).isEqualTo("PLAN_BOUNDARY_INCOMPLETE");
        assertThat(plan.getMissingSourceReasons()).contains("executionBoundary");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();
    }

    @Test
    void generateExecutionPlan_withCompleteSourceTraceAndReadyRiskGuardStillNeedsBoundaries() {
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
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
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

    @Test
    void generateExecutionPlan_externalBlockingWindowBlocksSourceGateAndPlan() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        decision.setExternalContextBlocked(true);
        decision.setExternalContextRiskLevel("HIGH");
        decision.setExternalContextSourceHealth(ExternalContextPolicy.SOURCE_HEALTH_OK);
        decision.setExternalEventIds(List.of("NEWS:major-event"));

        ExecutionPlanVO plan = service.generateExecutionPlan(decision, null, null, null, validSourceTrace(), readyRiskActionGuard());

        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED);
        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED);
        assertThat(plan.getSourceGateComplete()).isFalse();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotExecutableReason()).isEqualTo(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        assertThat(plan.getSourceBlockerReasons()).contains(ExternalContextPolicy.REASON_WINDOW_BLOCKED, "NEWS:major-event");
    }

    @Test
    void generateExecutionPlan_withBoundaryReadyProducerMapsReviewOnlyBoundaries() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        MarketEnvironmentVO marketEnv = new MarketEnvironmentVO();
        marketEnv.setLeverageSuggestion("2x");

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                marketEnv,
                null,
                readyBoundaryOnlyResult()
        );

        assertThat(plan.getEntryZone()).contains("入场区间", "62800", "63100");
        assertThat(plan.getStopLoss()).contains("止损参考", "62300");
        assertThat(plan.getTakeProfitRules()).contains("分批止盈", "64000", "65500", "RR 阶梯");
        assertThat(plan.getInvalidCondition()).contains("失效条件", "structure break below 62300", "62300");
        assertThat(plan.getLeverageSuggestion()).isEqualTo("2x");
        assertThat(plan.getPositionSuggestion()).isEqualTo("单笔风险不超过总资金 2%");
        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertNoUnsafeActionWording(plan.getEntryZone(), plan.getStopLoss(), plan.getTakeProfitRules(), plan.getInvalidCondition());
    }

    @Test
    void generateExecutionPlan_boundaryOnlyIncompleteKeepsFullSourceReadinessIncomplete() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                readyBoundaryOnlyResult()
        );

        assertThat(plan.getEntryZone()).contains("62800", "63100");
        assertThat(plan.getStopLoss()).contains("62300");
        assertThat(plan.getTakeProfitRules()).contains("64000", "65500");
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(plan.getSourceTraceComplete()).isFalse();
        assertThat(plan.getSourceGateComplete()).isFalse();
        assertThat(plan.getMissingSourceReasons()).contains(
                "sourceTrace missingFields present",
                "liquiditySource",
                "multiTimeframeSource",
                "eventSource",
                "wickSource"
        );
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();
    }

    @Test
    void generateExecutionPlan_failClosedProducerDoesNotMapPreciseBoundariesOrDecisionInvalidationFallback() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        decision.setPushInvalidationSummary("decision invalidation fallback");
        SourceTraceBoundaryProducerResult failClosed = readyBoundaryOnlyResult();
        failClosed.setBoundaryReady(false);
        failClosed.setBlockingReasons(List.of("boundaryReady=false"));
        failClosed.getSourceTrace().setFallbackStatus(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
        failClosed.getSourceTrace().setBlockingReasons(List.of("boundaryReady=false"));

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                failClosed
        );

        assertThat(plan.getEntryZone()).isEqualTo("暂无");
        assertThat(plan.getStopLoss()).isEqualTo("暂无");
        assertThat(plan.getTakeProfitRules()).isEqualTo("暂无");
        assertThat(plan.getInvalidCondition()).isNull();
        assertThat(plan.getSourceBlockerReasons()).contains("boundaryReady=false");
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_WATCH_ONLY);
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
    }

    @Test
    void validSourceTraceWithoutReadyBoundaryProducerCannotRemainBoundaryComplete() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        SourceTraceBoundaryProducerResult unavailable = new SourceTraceBoundaryProducerResult();
        unavailable.setSourceTrace(validSourceTrace());
        unavailable.setSourceTraceReady(true);
        unavailable.setBoundaryReady(false);
        unavailable.setMissingFields(List.of("entry", "stop", "takeProfitLevels"));
        unavailable.setBlockingReasons(List.of("BOUNDARY_PRODUCER_NOT_READY"));

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision, null, null, null, unavailable, readyRiskActionGuard());

        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        assertThat(plan.getSourceGateComplete()).isTrue();
        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getReadinessStatus()).isEqualTo(ExecutionPlanVO.READINESS_INCOMPLETE);
        assertThat(plan.getEntryZone()).isEqualTo("暂无");
        assertThat(plan.getStopLoss()).isEqualTo("暂无");
        assertThat(plan.getTakeProfitRules()).isEqualTo("暂无");
        assertThat(plan.getNotExecutableReason()).isEqualTo("PLAN_BOUNDARY_INCOMPLETE");
    }

    @Test
    void generateExecutionPlan_boundaryReadyWithoutMarketLeverageKeepsConservativeFallback() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);

        ExecutionPlanVO plan = service.generateExecutionPlan(
                decision,
                null,
                null,
                null,
                readyBoundaryOnlyResult()
        );

        assertThat(plan.getLeverageSuggestion()).isEqualTo("1-5x");
        assertThat(plan.getPositionSuggestion()).isEqualTo("单笔风险不超过总资金 2%");
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

    private SourceTraceBoundaryProducerResult readyBoundaryOnlyResult() {
        SourceTraceBoundaryProducerResult result = new SourceTraceBoundaryProducerResult();
        result.setBoundaryReady(true);
        result.setSourceTraceReady(false);
        result.setEntry(entry("62800", "63100"));
        result.setStop(stop("62300", "structure break below 62300"));
        result.setTakeProfitLevels(List.of(
                takeProfit(1, "64000", "STRUCTURE_TARGET", null, "prior resistance target"),
                takeProfit(2, "65500", "RR_LADDER", "2.0", "2R ladder target")
        ));
        result.setMissingFields(List.of("liquiditySource", "multiTimeframeSource", "eventSource", "wickSource"));

        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setEntryPriceSource(new BigDecimal("62950"));
        sourceTrace.setEntrySourceType("STRUCTURE_SUPPORT");
        sourceTrace.setEntrySourceTimeframe("1h");
        sourceTrace.setEntrySourceReason("support retest");
        sourceTrace.setEntrySourceRef("entry-support-1");
        sourceTrace.setStopPriceSource(new BigDecimal("62300"));
        sourceTrace.setStopSourceType("STRUCTURE_SWING_LOW");
        sourceTrace.setStopSourceTimeframe("1h");
        sourceTrace.setStopSourceReason("structure break below 62300");
        sourceTrace.setStopSourceRef("stop-swing-low-1");
        sourceTrace.setTpPriceSources(List.of(new BigDecimal("64000"), new BigDecimal("65500")));
        sourceTrace.setTpSourceType("MARKET_STRUCTURE_TARGET_SET");
        sourceTrace.setTpSourceTimeframe("1h");
        sourceTrace.setTpSourceReason("market structure target set");
        sourceTrace.setTpSourceRef("tp-structure-1|tp-rr-2");
        sourceTrace.setRrSource(new BigDecimal("2.0"));
        sourceTrace.setRrRuleRef("RR_LADDER");
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        sourceTrace.setMissingFields(List.of("liquiditySource", "multiTimeframeSource", "eventSource", "wickSource"));
        result.setSourceTrace(sourceTrace);
        return result;
    }

    private BoundaryEntryDTO entry(String low, String high) {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryZoneLow(new BigDecimal(low));
        entry.setEntryZoneHigh(new BigDecimal(high));
        entry.setEntryPrice(new BigDecimal("62950"));
        entry.setReason("support retest");
        return entry;
    }

    private BoundaryStopDTO stop(String price, String reason) {
        BoundaryStopDTO stop = new BoundaryStopDTO();
        stop.setStopPrice(new BigDecimal(price));
        stop.setReason(reason);
        return stop;
    }

    private BoundaryTakeProfitLevelDTO takeProfit(Integer level, String price, String source, String rr, String reason) {
        BoundaryTakeProfitLevelDTO takeProfit = new BoundaryTakeProfitLevelDTO();
        takeProfit.setLevel(level);
        takeProfit.setPrice(new BigDecimal(price));
        takeProfit.setSource(source);
        if (rr != null) {
            takeProfit.setRr(new BigDecimal(rr));
        }
        takeProfit.setReason(reason);
        return takeProfit;
    }

    private void assertNoUnsafeActionWording(String... values) {
        assertThat(String.join(" ", values)).doesNotContain("买入", "卖出", "下单", "执行下单", "自动开仓", "自动平仓", "一键平仓", "交易执行");
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
