package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.PlanService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class PlanServiceImpl implements PlanService {
    private static final String PLACEHOLDER_NOT_AVAILABLE = "暂无";
    private static final String DEFAULT_OBSERVE_ACTION = "观望";
    private static final String SOURCE_TRACE_MISSING = "SOURCE_TRACE_MISSING";
    private static final String SOURCE_TRACE_INCOMPLETE = "SOURCE_TRACE_INCOMPLETE";
    private static final String SOURCE_TRACE_WATCH_ONLY = "SOURCE_TRACE_WATCH_ONLY";
    private static final String SOURCE_TRACE_SAFE_FAIL_CLOSED = "SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY";
    private static final String MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";
    private static final String RISK_ACTION_GUARD_MISSING = "RISK_ACTION_GUARD_MISSING";
    private static final String RISK_ACTION_GUARD_BACKEND_PENDING = "RISK_ACTION_GUARD_BACKEND_PENDING";
    private static final String LIQUIDITY_CONTEXT_MISSING = "LIQUIDITY_CONTEXT_MISSING";
    private static final String STAMPEDE_RISK_REVIEW_ONLY = "STAMPEDE_RISK_REVIEW_ONLY";
    private static final String WICK_ONLY_RISK_REVIEW_ONLY = "WICK_ONLY_RISK_REVIEW_ONLY";
    private static final String RISK_ACTION_GUARD_BLOCKED = "RISK_ACTION_GUARD_BLOCKED";

    @Override
    public ExecutionPlanVO generateExecutionPlan(DecisionBundleVO decisionBundle, List<ScoreItemVO> scoreList,
                                                 MarketEnvironmentVO marketEnv, AssetAnalysisVO assetAnalysis) {
        return generateExecutionPlan(decisionBundle, scoreList, marketEnv, assetAnalysis, null);
    }

    @Override
    public ExecutionPlanVO generateExecutionPlan(
            DecisionBundleVO decisionBundle,
            List<ScoreItemVO> scoreList,
            MarketEnvironmentVO marketEnv,
            AssetAnalysisVO assetAnalysis,
            SourceTraceDTO sourceTrace
    ) {
        return generateExecutionPlan(decisionBundle, scoreList, marketEnv, assetAnalysis, sourceTrace, null);
    }

    @Override
    public ExecutionPlanVO generateExecutionPlan(
            DecisionBundleVO decisionBundle,
            List<ScoreItemVO> scoreList,
            MarketEnvironmentVO marketEnv,
            AssetAnalysisVO assetAnalysis,
            SourceTraceDTO sourceTrace,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setPlanId("plan-" + UUID.randomUUID().toString().substring(0, 8));
        plan.setRecommendedAction(DEFAULT_OBSERVE_ACTION);
        plan.setEntryZone(PLACEHOLDER_NOT_AVAILABLE);
        plan.setStopLoss(PLACEHOLDER_NOT_AVAILABLE);
        plan.setTakeProfitRules(PLACEHOLDER_NOT_AVAILABLE);
        plan.setLeverageSuggestion("1-5x");
        plan.setPositionSuggestion("单笔风险不超过总资金 2%");
        if (decisionBundle.getPushInvalidationSummary() != null && !decisionBundle.getPushInvalidationSummary().isBlank()) {
            plan.setInvalidCondition(decisionBundle.getPushInvalidationSummary());
        }
        plan.setPlanMode(resolvePlanMode(plan, decisionBundle));
        applySourceTraceReadiness(plan, sourceTrace);
        applyRiskActionGuardReadiness(plan, riskActionGuardDisplay);
        return plan;
    }

    @Override
    public ExecutionPlanVO buildExecutionPlanFromEnvironment(MarketEnvironmentVO env) {
        return new ExecutionPlanVO();
    }

    private static String resolvePlanMode(ExecutionPlanVO plan, DecisionBundleVO decisionBundle) {
        boolean hasConcreteExecutionFields = hasConcrete(plan.getEntryZone())
                && hasConcrete(plan.getStopLoss())
                && hasConcrete(plan.getTakeProfitRules());
        if (Boolean.TRUE.equals(decisionBundle.getIsWorthOpening()) && hasConcreteExecutionFields) {
            return ExecutionPlanVO.PLAN_MODE_SEMI_STRUCTURED;
        }
        return ExecutionPlanVO.PLAN_MODE_ADVISORY;
    }

    private static void applySourceTraceReadiness(ExecutionPlanVO plan, SourceTraceDTO sourceTrace) {
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        if (sourceTrace == null) {
            plan.setSourceTraceComplete(false);
            plan.setSourceTraceStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
            plan.setNotExecutableReason(SOURCE_TRACE_MISSING);
            return;
        }
        if (sourceTrace.hasRequiredBoundarySources()) {
            plan.setSourceTraceComplete(true);
            plan.setSourceTraceStatus("VALID");
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_READY_REVIEW_ONLY);
            plan.setNotExecutableReason(MANUAL_REVIEW_REQUIRED);
            return;
        }
        plan.setSourceTraceComplete(false);
        if (sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.WATCH_ONLY) {
            plan.setSourceTraceStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
            plan.setNotExecutableReason(SOURCE_TRACE_WATCH_ONLY);
            return;
        }
        if (sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
            plan.setSourceTraceStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
            plan.setNotExecutableReason(SOURCE_TRACE_SAFE_FAIL_CLOSED);
            return;
        }
        plan.setSourceTraceStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
        plan.setReadinessStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
        plan.setNotExecutableReason(SOURCE_TRACE_INCOMPLETE);
    }

    private static void applyRiskActionGuardReadiness(
            ExecutionPlanVO plan,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        plan.setRiskActionGuardReady(false);
        if (riskActionGuardDisplay == null) {
            return;
        }
        plan.setRiskActionGuardStatus(riskActionGuardDisplay.getRiskActionGuardStatus());
        String reason = resolveRiskActionGuardReason(riskActionGuardDisplay);
        plan.setRiskActionGuardBlockingReason(reason);
        if (reason == null) {
            plan.setRiskActionGuardReady(true);
            return;
        }
        plan.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        plan.setSourceTraceComplete(false);
        if (RISK_ACTION_GUARD_MISSING.equals(reason) || RISK_ACTION_GUARD_BACKEND_PENDING.equals(reason)) {
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
        } else {
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
        }
        plan.setNotExecutableReason(reason);
    }

    private static String resolveRiskActionGuardReason(
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        if (riskActionGuardDisplay == null) {
            return RISK_ACTION_GUARD_MISSING;
        }
        if (isBlankStatic(riskActionGuardDisplay.getRiskActionGuardStatus())
                || "BACKEND_PENDING".equalsIgnoreCase(riskActionGuardDisplay.getRiskActionGuardStatus())) {
            return RISK_ACTION_GUARD_BACKEND_PENDING;
        }
        if (isBlankStatic(riskActionGuardDisplay.getLiquidityState())
                || "BACKEND_PENDING".equalsIgnoreCase(riskActionGuardDisplay.getLiquidityState())) {
            return LIQUIDITY_CONTEXT_MISSING;
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getStampedeDetected())) {
            return STAMPEDE_RISK_REVIEW_ONLY;
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getWickOnlyRisk())) {
            return WICK_ONLY_RISK_REVIEW_ONLY;
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getOpportunityPushAllowed())
                || Boolean.TRUE.equals(riskActionGuardDisplay.getReverseTradeAllowed())
                || Boolean.TRUE.equals(riskActionGuardDisplay.getNewPositionAllowed())
                || Boolean.TRUE.equals(riskActionGuardDisplay.getMarketOrderExitAllowed())) {
            return RISK_ACTION_GUARD_BLOCKED;
        }
        String blockingReason = riskActionGuardDisplay.getRiskActionBlockingReason();
        if (!isBlankStatic(blockingReason) && !MANUAL_REVIEW_REQUIRED.equalsIgnoreCase(blockingReason)) {
            return blockingReason;
        }
        return null;
    }

    private static boolean hasConcrete(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && !PLACEHOLDER_NOT_AVAILABLE.equals(trimmed);
    }

    private static boolean isBlankStatic(String value) {
        return value == null || value.trim().isEmpty();
    }
}
