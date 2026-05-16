package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.PlanService;
import org.example.trademodel.vo.AssetAnalysisVO;
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

    private static boolean hasConcrete(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && !PLACEHOLDER_NOT_AVAILABLE.equals(trimmed);
    }
}
