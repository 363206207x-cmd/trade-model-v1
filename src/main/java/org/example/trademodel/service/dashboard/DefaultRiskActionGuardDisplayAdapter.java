package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.stereotype.Component;

/**
 * Fail-closed dashboard display adapter for Risk Action Guard status.
 * The first phase only maps read-only safety state and never triggers trading actions.
 */
@Component
public class DefaultRiskActionGuardDisplayAdapter implements RiskActionGuardDisplayAdapter {
    private static final String BACKEND_PENDING = "BACKEND_PENDING";
    private static final String VALID = "VALID";
    private static final String READY_REVIEW_ONLY = "READY_REVIEW_ONLY";
    private static final String MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";

    private static final String LABEL_BACKEND_PENDING = "后端未接入";
    private static final String LABEL_MANUAL_REVIEW_REQUIRED = "需要人工复核";

    private static final String DECISION_MISSING = "DECISION_MISSING";
    private static final String LIQUIDITY_CONTEXT_MISSING = "LIQUIDITY_CONTEXT_MISSING";
    private static final String LIQUIDITY_DETERIORATION_REVIEW_ONLY = "LIQUIDITY_DETERIORATION_REVIEW_ONLY";
    private static final String PLAN_BOUNDARY_NOT_VALID = "PLAN_BOUNDARY_NOT_VALID";
    private static final String EXECUTION_PLAN_NOT_READY = "EXECUTION_PLAN_NOT_READY";
    private static final String STAMPEDE_REVIEW_ONLY = "STAMPEDE_REVIEW_ONLY";
    private static final String WICK_ONLY_REVIEW_ONLY = "WICK_ONLY_REVIEW_ONLY";
    private static final String HIGH_RISK_REVIEW_ONLY = "HIGH_RISK_REVIEW_ONLY";

    private static final String ADVICE_READ_ONLY =
            "只读风险展示：仅提示人工复核，不是交易指令。";
    private static final String ADVICE_HIGH_RISK =
            "高风险仅触发人工复核，可复核减仓 / 移动止损 / 降低杠杆，不自动止损、反手或开仓。";
    private static final String ADVICE_LIQUIDITY_MISSING =
            "流动性状态缺失，风险动作保持失败关闭，只能人工复核。";
    private static final String ADVICE_LIQUIDITY_DETERIORATION =
            "流动性恶化时不做市价一次性砍仓，只能人工复核分批降风险、等待流动性恢复或只降杠杆。";
    private static final String ADVICE_STAMPEDE =
            "踩踏风险进入极端压力锁定，禁止机会推送、反手和新开仓，先保护本金。";
    private static final String ADVICE_WICK_ONLY =
            "仅插针风险不等于趋势反转，不生成反向开仓计划，只能等待确认。";
    private static final String ADVICE_STRONG_REVERSAL_MOVING_STOP_REVIEW_ONLY =
            "强反转 / 移动止损仍未自动化，只能人工复核；"
                    + "强反转待确认，原入场逻辑疑似失效也只能进入复核；"
                    + "移动止损需要人工复核；"
                    + "强反转不等于反手或自动平仓；"
                    + "移动止损不等于自动改止损；"
                    + "自动平仓 / 自动反手 / 自动修改止损均关闭；"
                    + "不生成真实 entry / stop / TP / RR，不升级 Readiness。";

    @Override
    public DashboardDetailResponseVO.RiskActionGuardDisplayVO build(
            DecisionResultVO decision,
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay,
            DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO fallbackDisplay
    ) {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = fallbackDisplay != null
                ? fallbackDisplay
                : new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        enforceFailClosed(display);

        if (decision == null) {
            display.setRiskActionBlockingReason(DECISION_MISSING);
            return display;
        }

        if (!isPlanBoundaryValid(planBoundaryDisplay)) {
            display.setRiskActionBlockingReason(PLAN_BOUNDARY_NOT_VALID);
            return display;
        }

        if (!isExecutionPlanReviewOnly(executionPlanDisplay)) {
            display.setRiskActionBlockingReason(EXECUTION_PLAN_NOT_READY);
            return display;
        }

        if (Boolean.TRUE.equals(display.getStampedeDetected())) {
            markManualReview(display, STAMPEDE_REVIEW_ONLY, advice(ADVICE_STAMPEDE));
            return display;
        }

        if (isHighRisk(decision) && isLiquidityDeteriorating(display)) {
            markManualReview(display, LIQUIDITY_DETERIORATION_REVIEW_ONLY,
                    advice(ADVICE_LIQUIDITY_DETERIORATION));
            return display;
        }

        if (isHighRisk(decision) && isLiquidityContextMissing(display)) {
            markManualReview(display, LIQUIDITY_CONTEXT_MISSING, advice(ADVICE_LIQUIDITY_MISSING));
            return display;
        }

        if (Boolean.TRUE.equals(display.getWickOnlyRisk())) {
            markManualReview(display, WICK_ONLY_REVIEW_ONLY, advice(ADVICE_WICK_ONLY));
            return display;
        }

        if (isHighRisk(decision)) {
            markManualReview(display, HIGH_RISK_REVIEW_ONLY, advice(ADVICE_HIGH_RISK));
            return display;
        }

        markManualReview(display, MANUAL_REVIEW_REQUIRED, advice(ADVICE_READ_ONLY));
        return display;
    }

    private boolean isPlanBoundaryValid(DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundaryDisplay) {
        return planBoundaryDisplay != null && VALID.equalsIgnoreCase(planBoundaryDisplay.getPlanBoundaryStatus());
    }

    private boolean isExecutionPlanReviewOnly(DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlanDisplay) {
        return executionPlanDisplay != null
                && READY_REVIEW_ONLY.equalsIgnoreCase(executionPlanDisplay.getExecutionPlanStatus());
    }

    private boolean isHighRisk(DecisionResultVO decision) {
        return "HIGH".equalsIgnoreCase(decision.getRiskLevel())
                || "EXTREME".equalsIgnoreCase(decision.getRiskLevel());
    }

    private boolean isLiquidityContextMissing(DashboardDetailResponseVO.RiskActionGuardDisplayVO display) {
        return display == null
                || isBlank(display.getLiquidityState())
                || BACKEND_PENDING.equalsIgnoreCase(display.getLiquidityState());
    }

    private boolean isLiquidityDeteriorating(DashboardDetailResponseVO.RiskActionGuardDisplayVO display) {
        if (display == null || isBlank(display.getLiquidityState())) {
            return false;
        }
        String normalized = display.getLiquidityState().trim().toUpperCase();
        return normalized.contains("DETERIOR")
                || normalized.contains("WORSE")
                || normalized.contains("STRESS")
                || normalized.contains("恶化")
                || normalized.contains("紧张");
    }

    private void markManualReview(
            DashboardDetailResponseVO.RiskActionGuardDisplayVO display,
            String reason,
            String advice
    ) {
        display.setRiskActionGuardStatus(MANUAL_REVIEW_REQUIRED);
        display.setRiskActionGuardStatusLabel(LABEL_MANUAL_REVIEW_REQUIRED);
        display.setRiskActionBlockingReason(reason);
        display.setRiskActionAdvice(advice);
        enforceFailClosed(display);
    }

    private void enforceFailClosed(DashboardDetailResponseVO.RiskActionGuardDisplayVO display) {
        if (isBlank(display.getRiskActionGuardStatus())) {
            display.setRiskActionGuardStatus(BACKEND_PENDING);
        }
        if (isBlank(display.getRiskActionGuardStatusLabel())) {
            display.setRiskActionGuardStatusLabel(LABEL_BACKEND_PENDING);
        }
        if (isBlank(display.getLiquidityState())) {
            display.setLiquidityState(BACKEND_PENDING);
        }
        if (isBlank(display.getRiskActionAdvice())) {
            display.setRiskActionAdvice(advice(ADVICE_READ_ONLY));
        }
        display.setStampedeDetected(Boolean.TRUE.equals(display.getStampedeDetected()));
        display.setWickOnlyRisk(Boolean.TRUE.equals(display.getWickOnlyRisk()));
        display.setOpportunityPushAllowed(false);
        display.setReverseTradeAllowed(false);
        display.setNewPositionAllowed(false);
        display.setMarketOrderExitAllowed(false);
        display.setManualRiskReviewRequired(true);
        display.setNotTradeInstruction(true);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String advice(String mainAdvice) {
        return mainAdvice + " " + ADVICE_STRONG_REVERSAL_MOVING_STOP_REVIEW_ONLY;
    }
}
