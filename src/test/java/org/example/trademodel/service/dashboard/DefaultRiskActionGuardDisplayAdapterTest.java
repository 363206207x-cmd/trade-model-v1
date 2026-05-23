package org.example.trademodel.service.dashboard;

import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRiskActionGuardDisplayAdapterTest {
    private final DefaultRiskActionGuardDisplayAdapter adapter = new DefaultRiskActionGuardDisplayAdapter();

    @Test
    void shouldReturnBackendPendingAndFailClosedWhenInputsAreMissing() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(null, null, null, null);

        assertEquals("BACKEND_PENDING", display.getRiskActionGuardStatus());
        assertEquals("BACKEND_PENDING", display.getLiquidityState());
        assertEquals("DECISION_MISSING", display.getRiskActionBlockingReason());
        assertAdviceContains(display, "只读风险展示", "不是交易指令", "强反转 / 移动止损仍未自动化");
        assertFailClosed(display);
    }

    @Test
    void shouldPreserveFallbackObjectAndForceSafetyFlags() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setOpportunityPushAllowed(true);
        fallback.setReverseTradeAllowed(true);
        fallback.setNewPositionAllowed(true);
        fallback.setMarketOrderExitAllowed(true);
        fallback.setManualRiskReviewRequired(false);
        fallback.setNotTradeInstruction(false);

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(null, null, null, fallback);

        assertSame(fallback, display);
        assertEquals("DECISION_MISSING", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldBlockWhenPlanBoundaryIsNotValid() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("INCOMPLETE");
        DashboardDetailResponseVO.ExecutionPlanDisplayVO execution = readyReviewExecutionPlan();

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(decision, boundary, execution, null);

        assertEquals("PLAN_BOUNDARY_NOT_VALID", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldBlockWhenExecutionPlanIsNotReadyReviewOnly() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = validBoundary();
        DashboardDetailResponseVO.ExecutionPlanDisplayVO execution = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        execution.setExecutionPlanStatus("BOUNDARY_PENDING");

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(decision, boundary, execution, null);

        assertEquals("EXECUTION_PLAN_NOT_READY", display.getRiskActionBlockingReason());
        assertFailClosed(display);
    }

    @Test
    void shouldRequireManualReviewWhenBoundaryAndExecutionPlanAreReady() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                null
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionGuardStatus());
        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionBlockingReason());
        assertAdviceContains(display, "只读风险展示", "不是交易指令", "只能人工复核");
        assertFailClosed(display);
    }

    @Test
    void shouldNotTurnHighRiskIntoActionWithoutLiquidityContext() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setLiquidityState(null);

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionGuardStatus());
        assertEquals("LIQUIDITY_CONTEXT_MISSING", display.getRiskActionBlockingReason());
        assertAdviceContains(display, "流动性状态缺失", "失败关闭", "只能人工复核");
        assertFailClosed(display);
    }

    @Test
    void shouldKeepHighRiskReadOnlyWhenLiquidityIsNormal() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setLiquidityState("NORMAL");

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionGuardStatus());
        assertEquals("HIGH_RISK_REVIEW_ONLY", display.getRiskActionBlockingReason());
        assertAdviceContains(display, "高风险仅触发人工复核", "移动止损", "不自动止损、反手或开仓");
        assertFailClosed(display);
    }

    @Test
    void shouldFailClosedWhenLiquidityDeteriorates() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setLiquidityState("LIQUIDITY_DETERIORATING");

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionGuardStatus());
        assertEquals("LIQUIDITY_DETERIORATION_REVIEW_ONLY", display.getRiskActionBlockingReason());
        assertAdviceContains(display, "流动性恶化", "不做市价一次性砍仓", "只降杠杆");
        assertFailClosed(display);
    }

    @Test
    void shouldLockStampedeRiskAndKeepAllActionFlagsBlocked() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setLiquidityState("NORMAL");
        fallback.setStampedeDetected(true);

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertTrue(display.getStampedeDetected());
        assertEquals("STAMPEDE_REVIEW_ONLY", display.getRiskActionBlockingReason());
        assertAdviceContains(display, "踩踏风险", "禁止机会推送", "先保护本金");
        assertFailClosed(display);
    }

    @Test
    void shouldTreatWickOnlyRiskAsReviewOnlyAndNotTrendReversal() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("LOW");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setLiquidityState("NORMAL");
        fallback.setWickOnlyRisk(true);

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertTrue(display.getWickOnlyRisk());
        assertEquals("WICK_ONLY_REVIEW_ONLY", display.getRiskActionBlockingReason());
        assertAdviceContains(display, "仅插针风险不等于趋势反转", "不生成反向开仓计划", "等待确认");
        assertFailClosed(display);
    }

    @Test
    void shouldExposeStrongReversalAndMovingStopAsReviewOnlyWithoutActions() {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setRiskLevel("HIGH");
        DashboardDetailResponseVO.RiskActionGuardDisplayVO fallback = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        fallback.setLiquidityState("NORMAL");

        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = adapter.build(
                decision,
                validBoundary(),
                readyReviewExecutionPlan(),
                fallback
        );

        assertEquals("MANUAL_REVIEW_REQUIRED", display.getRiskActionGuardStatus());
        assertEquals("HIGH_RISK_REVIEW_ONLY", display.getRiskActionBlockingReason());
        assertAdviceContains(display,
                "强反转待确认",
                "原入场逻辑疑似失效",
                "移动止损需要人工复核",
                "强反转不等于反手",
                "自动平仓",
                "移动止损不等于自动改止损",
                "自动平仓 / 自动反手 / 自动修改止损均关闭",
                "不生成真实 entry / stop / TP / RR",
                "不升级 Readiness"
        );
        assertFailClosed(display);
    }

    @Test
    void shouldNotExposePointGenerationReadinessOrProductionActionMethods() {
        for (java.lang.reflect.Method method : DashboardDetailResponseVO.RiskActionGuardDisplayVO.class.getMethods()) {
            String name = method.getName().toLowerCase();
            assertFalse(name.contains("entry"));
            assertFalse(name.contains("stoploss"));
            assertFalse(name.contains("modifystop"));
            assertFalse(name.contains("takeprofit"));
            assertFalse(name.contains("tp"));
            assertFalse(name.contains("rr"));
            assertFalse(name.contains("readiness"));
            assertFalse(name.contains("execution"));
            assertFalse(name.contains("autotrading"));
            assertFalse(name.contains("automation"));
            assertFalse(name.contains("placeorder"));
            assertFalse(name.contains("closeposition"));
            assertFalse(name.contains("reverseposition"));
            assertFalse(name.contains("buybutton"));
            assertFalse(name.contains("sellbutton"));
            assertFalse(name.equals("buy"));
            assertFalse(name.equals("sell"));
        }
    }

    private DashboardDetailResponseVO.PlanBoundaryDisplayVO validBoundary() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO boundary = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        boundary.setPlanBoundaryStatus("VALID");
        return boundary;
    }

    private DashboardDetailResponseVO.ExecutionPlanDisplayVO readyReviewExecutionPlan() {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO execution = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        execution.setExecutionPlanStatus("READY_REVIEW_ONLY");
        return execution;
    }

    private void assertFailClosed(DashboardDetailResponseVO.RiskActionGuardDisplayVO display) {
        assertFalse(display.getOpportunityPushAllowed());
        assertFalse(display.getReverseTradeAllowed());
        assertFalse(display.getNewPositionAllowed());
        assertFalse(display.getMarketOrderExitAllowed());
        assertTrue(display.getManualRiskReviewRequired());
        assertTrue(display.getNotTradeInstruction());
    }

    private void assertAdviceContains(DashboardDetailResponseVO.RiskActionGuardDisplayVO display, String... parts) {
        String advice = display.getRiskActionAdvice();
        for (String part : parts) {
            assertTrue(advice.contains(part), "Expected advice to contain: " + part + ", actual: " + advice);
        }
    }
}
