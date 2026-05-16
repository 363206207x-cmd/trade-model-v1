package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.ExecutionPlanVO;

public interface RuleEngineService {
    RuleBaseOutput execute(DecisionContext ctx);

    default RuleBaseOutput execute(DecisionContext ctx, SourceTraceDTO sourceTrace) {
        if (sourceTrace == null || !sourceTrace.hasRequiredBoundarySources()) {
            RuleBaseOutput output = new RuleBaseOutput();
            output.setCanExecute(false);
            output.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
            if (sourceTrace != null && sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.WATCH_ONLY) {
                output.setConfidenceLevel(ExecutionPlanVO.READINESS_WATCH_ONLY);
                output.setRiskLevel("SOURCE_TRACE_WATCH_ONLY");
            } else if (sourceTrace != null
                    && sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
                output.setConfidenceLevel(ExecutionPlanVO.READINESS_WATCH_ONLY);
                output.setRiskLevel("SOURCE_TRACE_SAFE_FAIL_CLOSED_ONLY");
            } else {
                output.setConfidenceLevel(ExecutionPlanVO.READINESS_INCOMPLETE);
                output.setRiskLevel("SOURCE_TRACE_INCOMPLETE");
            }
            return output;
        }

        RuleBaseOutput output = execute(ctx);
        if (output == null) {
            output = new RuleBaseOutput();
        }
        output.setCanExecute(false);
        output.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        return output;
    }

    default RuleBaseOutput execute(
            DecisionContext ctx,
            SourceTraceDTO sourceTrace,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        String riskBlockingReason = resolveRiskActionGuardBlockingReason(riskActionGuardDisplay);
        if (riskBlockingReason != null) {
            RuleBaseOutput output = new RuleBaseOutput();
            output.setCanExecute(false);
            output.setPlanMode(ExecutionPlanVO.PLAN_MODE_ADVISORY);
            output.setConfidenceLevel(ExecutionPlanVO.READINESS_WATCH_ONLY);
            output.setRiskLevel(riskBlockingReason);
            return output;
        }
        return execute(ctx, sourceTrace);
    }

    private static String resolveRiskActionGuardBlockingReason(
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    ) {
        if (riskActionGuardDisplay == null) {
            return "RISK_ACTION_GUARD_MISSING";
        }
        if (riskActionGuardDisplay.getRiskActionGuardStatus() == null
                || riskActionGuardDisplay.getRiskActionGuardStatus().trim().isEmpty()
                || "BACKEND_PENDING".equalsIgnoreCase(riskActionGuardDisplay.getRiskActionGuardStatus())) {
            return "RISK_ACTION_GUARD_BACKEND_PENDING";
        }
        if (riskActionGuardDisplay.getLiquidityState() == null
                || riskActionGuardDisplay.getLiquidityState().trim().isEmpty()
                || "BACKEND_PENDING".equalsIgnoreCase(riskActionGuardDisplay.getLiquidityState())) {
            return "LIQUIDITY_CONTEXT_MISSING";
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getStampedeDetected())) {
            return "STAMPEDE_RISK_REVIEW_ONLY";
        }
        if (Boolean.TRUE.equals(riskActionGuardDisplay.getWickOnlyRisk())) {
            return "WICK_ONLY_RISK_REVIEW_ONLY";
        }
        String blockingReason = riskActionGuardDisplay.getRiskActionBlockingReason();
        if (blockingReason != null
                && !blockingReason.trim().isEmpty()
                && !"MANUAL_REVIEW_REQUIRED".equalsIgnoreCase(blockingReason)) {
            return blockingReason;
        }
        return null;
    }
}
