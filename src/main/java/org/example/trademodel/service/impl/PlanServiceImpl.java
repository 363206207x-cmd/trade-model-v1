package org.example.trademodel.service.impl;

import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.ExecutionPlanSourceGate;
import org.example.trademodel.dto.planboundary.ExecutionPlanSourceGateResultDTO;
import org.example.trademodel.dto.planboundary.SourceTraceBoundaryProducerResult;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy;
import org.example.trademodel.service.support.ExecutionFeasibilityContract;
import org.example.trademodel.service.PlanService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PlanServiceImpl implements PlanService {
    private static final String PLACEHOLDER_NOT_AVAILABLE = "暂无";
    private static final String DEFAULT_OBSERVE_ACTION = "观望";
    private static final String SOURCE_TRACE_MISSING = "SOURCE_TRACE_MISSING";
    private static final String SOURCE_TRACE_INCOMPLETE = "SOURCE_TRACE_INCOMPLETE";
    private static final String SOURCE_TRACE_WATCH_ONLY = "SOURCE_TRACE_WATCH_ONLY";
    private static final String SOURCE_TRACE_BLOCKED = "SOURCE_TRACE_BLOCKED";
    private static final String SOURCE_TRACE_SAFE_FAIL_CLOSED = "SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY";
    private static final String MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";
    private static final String PLAN_BOUNDARY_INCOMPLETE = "PLAN_BOUNDARY_INCOMPLETE";
    private static final String RISK_ACTION_GUARD_MISSING = "RISK_ACTION_GUARD_MISSING";
    private static final String RISK_ACTION_GUARD_BACKEND_PENDING = "RISK_ACTION_GUARD_BACKEND_PENDING";
    private static final String LIQUIDITY_CONTEXT_MISSING = "LIQUIDITY_CONTEXT_MISSING";
    private static final String STAMPEDE_RISK_REVIEW_ONLY = "STAMPEDE_RISK_REVIEW_ONLY";
    private static final String WICK_ONLY_RISK_REVIEW_ONLY = "WICK_ONLY_RISK_REVIEW_ONLY";
    private static final String RISK_ACTION_GUARD_BLOCKED = "RISK_ACTION_GUARD_BLOCKED";

    @Override
    public ExecutionPlanVO buildRuleExecutionAssessment(
            DecisionBundleVO decisionBundle,
            SourceTraceBoundaryProducerResult boundaryResult
    ) {
        ExecutionPlanVO assessment = new ExecutionPlanVO();
        assessment.setPlanId("rule-assessment-" + UUID.randomUUID());
        assessment.setManualReviewRequired(true);
        assessment.setNotTradeInstruction(true);
        assessment.setNotExecutable(true);
        assessment.setNotAutoTrading(true);
        assessment.setNotOrderExecution(true);
        assessment.setNotUserPositionCreation(true);
        assessment.setFinalPlan(false);
        assessment.setChainStatus("RULE_BASE_ASSESSMENT");
        assessment.setRuleValidationStatus("NOT_RUN");
        ExecutionFeasibilityContract.initializeUnavailable(assessment,
                ExecutionFeasibilityContract.DEFAULT_REASON);

        SourceTraceDTO sourceTrace = boundaryResult == null ? null : boundaryResult.getSourceTrace();
        applySourceTraceReadiness(assessment, sourceTrace);
        if (boundaryResult != null) {
            appendUnique(assessment.getMissingSourceReasons(), boundaryResult.getMissingFields());
            appendUnique(assessment.getSourceBlockerReasons(), boundaryResult.getBlockingReasons());
        }
        if (boundaryResult == null
                || !boundaryResult.isBoundaryReady()
                || !boundaryResult.isSourceTraceReady()
                || !hasRequiredBoundaryEvidence(boundaryResult)) {
            assessment.setSourceGateComplete(false);
            if (!assessment.getSourceBlockerReasons().isEmpty()) {
                assessment.setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED);
                assessment.setSourceCompletenessSummary(
                        "rule source gate BLOCKED: " + String.join("; ", assessment.getSourceBlockerReasons()));
            } else {
                assessment.setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
                assessment.setSourceCompletenessSummary("rule source gate INCOMPLETE");
            }
        }
        // This object is a non-final rule assessment, never a detailed plan.
        assessment.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        applyExternalContextReadiness(assessment, decisionBundle);
        return assessment;
    }

    @Override
    public ExecutionPlanVO generateExecutionPlan(DecisionBundleVO decisionBundle, List<ScoreItemVO> scoreList,
                                                 MarketEnvironmentVO marketEnv, AssetAnalysisVO assetAnalysis) {
        return generateExecutionPlan(decisionBundle, scoreList, marketEnv, assetAnalysis, (SourceTraceDTO) null);
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
        ExecutionPlanVO plan = buildBaseExecutionPlan(decisionBundle, sourceTrace, riskActionGuardDisplay);
        enforceBoundaryCompleteness(plan);
        return plan;
    }

    private ExecutionPlanVO buildBaseExecutionPlan(
            DecisionBundleVO decisionBundle,
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
        ExecutionFeasibilityContract.initializeUnavailable(plan,
                ExecutionFeasibilityContract.DEFAULT_REASON);
        plan.setPlanMode(resolvePlanMode(plan, decisionBundle));
        applySourceTraceReadiness(plan, sourceTrace);
        applyRiskActionGuardReadiness(plan, riskActionGuardDisplay);
        applyExternalContextReadiness(plan, decisionBundle);
        return plan;
    }

    @Override
    public ExecutionPlanVO generateExecutionPlan(
            DecisionBundleVO decisionBundle,
            List<ScoreItemVO> scoreList,
            MarketEnvironmentVO marketEnv,
            AssetAnalysisVO assetAnalysis,
            SourceTraceBoundaryProducerResult boundaryResult
    ) {
        return generateExecutionPlan(decisionBundle, scoreList, marketEnv, assetAnalysis, boundaryResult, null);
    }

    @Override
    public ExecutionPlanVO generateExecutionPlan(
            DecisionBundleVO decisionBundle,
            List<ScoreItemVO> scoreList,
            MarketEnvironmentVO marketEnv,
            AssetAnalysisVO assetAnalysis,
            SourceTraceBoundaryProducerResult boundaryResult,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        SourceTraceDTO sourceTrace = boundaryResult == null ? null : boundaryResult.getSourceTrace();
        ExecutionPlanVO plan = buildBaseExecutionPlan(decisionBundle, sourceTrace, riskActionGuardDisplay);
        applyBoundaryProducerResult(plan, boundaryResult, marketEnv);
        enforceBoundaryCompleteness(plan);
        return plan;
    }

    @Override
    public ExecutionPlanVO buildExecutionPlanFromEnvironment(MarketEnvironmentVO env) {
        return new ExecutionPlanVO();
    }

    private static String resolvePlanMode(ExecutionPlanVO plan, DecisionBundleVO decisionBundle) {
        boolean hasConcreteExecutionFields = ExecutionPlanReviewPolicy.hasCompleteBoundaries(plan);
        if (Boolean.TRUE.equals(decisionBundle.getIsWorthOpening()) && hasConcreteExecutionFields) {
            return ExecutionPlanVO.PLAN_MODE_SEMI_STRUCTURED;
        }
        return ExecutionPlanVO.PLAN_MODE_ADVISORY;
    }

    private static void applySourceTraceReadiness(ExecutionPlanVO plan, SourceTraceDTO sourceTrace) {
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        plan.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);

        ExecutionPlanSourceGateResultDTO sourceGate = ExecutionPlanSourceGate.validate(sourceTrace);
        applySourceGateResult(plan, sourceGate);

        if (sourceGate.isValid()) {
            plan.setSourceTraceComplete(true);
            plan.setSourceTraceStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_READY_REVIEW_ONLY);
            plan.setNotExecutableReason(MANUAL_REVIEW_REQUIRED);
            return;
        }

        if (sourceTrace == null) {
            plan.setSourceTraceComplete(false);
            plan.setSourceTraceStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
            plan.setNotExecutableReason(SOURCE_TRACE_MISSING);
            return;
        }
        plan.setSourceTraceComplete(false);
        if (ExecutionPlanSourceGateResultDTO.STATUS_REVIEW_ONLY.equals(sourceGate.getStatus())) {
            plan.setSourceTraceStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_REVIEW_ONLY);
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
            plan.setNotExecutableReason(SOURCE_TRACE_WATCH_ONLY);
            return;
        }
        if (ExecutionPlanSourceGateResultDTO.STATUS_BLOCKED.equals(sourceGate.getStatus())
                || ExecutionPlanSourceGateResultDTO.STATUS_INVALID.equals(sourceGate.getStatus())) {
            plan.setSourceTraceStatus(sourceGate.getStatus());
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
            plan.setNotExecutableReason(SOURCE_TRACE_BLOCKED);
            return;
        }
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

    private static void applySourceGateResult(ExecutionPlanVO plan, ExecutionPlanSourceGateResultDTO sourceGate) {
        plan.setExecutionPlanStatus(sourceGate.getStatus());
        plan.setSourceGateStatus(sourceGate.getStatus());
        plan.setSourceGateComplete(sourceGate.isSourceComplete());
        plan.setSourceCompletenessSummary(sourceGate.getSourceCompletenessSummary());
        plan.setMissingSourceReasons(sourceGate.getMissingSourceReasons());
        plan.setSourceBlockerReasons(sourceGate.getBlockerReasons());
        plan.setManualReviewRequired(sourceGate.isManualReviewRequired());
        plan.setNotTradeInstruction(sourceGate.isNotTradeInstruction());
        plan.setNotExecutable(sourceGate.isNotExecutable());
        plan.setNotAutoTrading(sourceGate.isNotAutoTrading());
        plan.setNotOrderExecution(sourceGate.isNotOrderExecution());
        plan.setNotUserPositionCreation(sourceGate.isNotUserPositionCreation());
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

    private static void applyExternalContextReadiness(ExecutionPlanVO plan, DecisionBundleVO decisionBundle) {
        if (plan == null || decisionBundle == null) {
            return;
        }
        boolean sourceBlocked = ExternalContextPolicy.SOURCE_HEALTH_BLOCKED.equalsIgnoreCase(
                decisionBundle.getExternalContextSourceHealth());
        boolean blocked = Boolean.TRUE.equals(decisionBundle.getExternalContextBlocked()) || sourceBlocked;
        if (!blocked) {
            return;
        }
        String reason = sourceBlocked
                ? ExternalContextPolicy.REASON_MISSING_SOURCE
                : ExternalContextPolicy.REASON_WINDOW_BLOCKED;
        plan.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED);
        plan.setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED);
        plan.setSourceGateComplete(false);
        plan.setSourceTraceComplete(false);
        plan.setSourceTraceStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED);
        plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
        plan.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        plan.setNotExecutableReason(reason);
        List<String> blockers = new ArrayList<>(plan.getSourceBlockerReasons());
        blockers.add(reason);
        if (decisionBundle.getExternalEventIds() != null) {
            blockers.addAll(decisionBundle.getExternalEventIds());
        }
        plan.setSourceBlockerReasons(blockers);
        plan.setSourceCompletenessSummary("external context gate BLOCKED");
    }

    private static void applyBoundaryProducerResult(
            ExecutionPlanVO plan,
            SourceTraceBoundaryProducerResult boundaryResult,
            MarketEnvironmentVO marketEnv
    ) {
        if (plan == null || boundaryResult == null) {
            return;
        }
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        appendUnique(plan.getMissingSourceReasons(), boundaryResult.getMissingFields());
        appendUnique(plan.getSourceBlockerReasons(), boundaryResult.getBlockingReasons());
        if (!boundaryResult.isBoundaryReady() || !hasRequiredBoundaryEvidence(boundaryResult)) {
            return;
        }

        plan.setEntryZone(formatEntryZone(boundaryResult.getEntry()));
        plan.setStopLoss(formatStopLoss(boundaryResult.getStop()));
        plan.setTakeProfitRules(formatTakeProfitRules(boundaryResult.getTakeProfitLevels()));
        plan.setInvalidCondition(formatInvalidCondition(boundaryResult.getStop()));
        if (marketEnv != null && !isBlankStatic(marketEnv.getLeverageSuggestion())) {
            plan.setLeverageSuggestion(marketEnv.getLeverageSuggestion().trim());
        }
        plan.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
    }

    private static void enforceBoundaryCompleteness(ExecutionPlanVO plan) {
        if (plan == null || ExecutionPlanReviewPolicy.hasCompleteBoundaries(plan)) {
            return;
        }
        String status = plan.getExecutionPlanStatus();
        if (ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED.equalsIgnoreCase(status)
                || "INVALID".equalsIgnoreCase(status)
                || ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE.equalsIgnoreCase(status)) {
            return;
        }
        plan.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        if (!ExecutionPlanVO.READINESS_WATCH_ONLY.equalsIgnoreCase(plan.getReadinessStatus())) {
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_INCOMPLETE);
        }
        plan.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        if (plan.getNotExecutableReason() == null
                || MANUAL_REVIEW_REQUIRED.equalsIgnoreCase(plan.getNotExecutableReason())) {
            plan.setNotExecutableReason(PLAN_BOUNDARY_INCOMPLETE);
        }
        appendUnique(plan.getMissingSourceReasons(), List.of("executionBoundary"));
    }

    private static boolean hasRequiredBoundaryEvidence(SourceTraceBoundaryProducerResult boundaryResult) {
        BoundaryEntryDTO entry = boundaryResult.getEntry();
        BoundaryStopDTO stop = boundaryResult.getStop();
        List<BoundaryTakeProfitLevelDTO> takeProfitLevels = boundaryResult.getTakeProfitLevels();
        return entry != null
                && entry.getEntryZoneLow() != null
                && entry.getEntryZoneHigh() != null
                && stop != null
                && stop.getStopPrice() != null
                && takeProfitLevels != null
                && !takeProfitLevels.isEmpty()
                && takeProfitLevels.stream().allMatch(level -> level != null && level.getPrice() != null);
    }

    private static String formatEntryZone(BoundaryEntryDTO entry) {
        StringBuilder builder = new StringBuilder("入场区间 ");
        builder.append(formatDecimal(entry.getEntryZoneLow()))
                .append("-")
                .append(formatDecimal(entry.getEntryZoneHigh()));
        appendReason(builder, entry.getReason());
        return builder.toString();
    }

    private static String formatStopLoss(BoundaryStopDTO stop) {
        StringBuilder builder = new StringBuilder("止损参考 ");
        builder.append(formatDecimal(stop.getStopPrice()));
        appendReason(builder, stop.getReason());
        return builder.toString();
    }

    private static String formatTakeProfitRules(List<BoundaryTakeProfitLevelDTO> takeProfitLevels) {
        List<String> parts = new ArrayList<>();
        for (BoundaryTakeProfitLevelDTO level : takeProfitLevels) {
            StringBuilder builder = new StringBuilder();
            builder.append("TP").append(level.getLevel() == null ? parts.size() + 1 : level.getLevel())
                    .append(" ")
                    .append(resolveTakeProfitLabel(level.getSource()))
                    .append(" ")
                    .append(formatDecimal(level.getPrice()));
            if (level.getRr() != null) {
                builder.append(" RR ").append(formatDecimal(level.getRr()));
            }
            appendReason(builder, level.getReason());
            parts.add(builder.toString());
        }
        return "分批止盈：" + String.join("；", parts);
    }

    private static String formatInvalidCondition(BoundaryStopDTO stop) {
        StringBuilder builder = new StringBuilder("失效条件：结构边界失效或触及止损参考 ");
        builder.append(formatDecimal(stop.getStopPrice()));
        appendReason(builder, stop.getReason());
        return builder.toString();
    }

    private static String resolveTakeProfitLabel(String source) {
        if (!isBlankStatic(source) && "RR_LADDER".equalsIgnoreCase(source.trim())) {
            return "RR 阶梯";
        }
        return "目标参考";
    }

    private static void appendReason(StringBuilder builder, String reason) {
        if (!isBlankStatic(reason)) {
            builder.append("（").append(reason.trim()).append("）");
        }
    }

    private static String formatDecimal(BigDecimal value) {
        if (value == null) {
            return PLACEHOLDER_NOT_AVAILABLE;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static void appendUnique(List<String> target, List<String> source) {
        if (target == null || source == null) {
            return;
        }
        for (String value : source) {
            if (!isBlankStatic(value) && !target.contains(value)) {
                target.add(value);
            }
        }
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

    private static boolean isBlankStatic(String value) {
        return value == null || value.trim().isEmpty();
    }
}
