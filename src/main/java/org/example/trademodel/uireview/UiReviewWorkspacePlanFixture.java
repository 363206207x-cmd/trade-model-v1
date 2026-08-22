package org.example.trademodel.uireview;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("ui-review")
public class UiReviewWorkspacePlanFixture {
    private static final String CURRENT_PLAN_ID = "ui-review-final-btc-001";
    private static final String REVALIDATION_PLAN_ID = "ui-review-final-btc-needs-revalidation";

    public ExecutionPlanDO find(String planId) {
        if (CURRENT_PLAN_ID.equals(planId)) {
            return plan(CURRENT_PLAN_ID, "CURRENT", false);
        }
        if (REVALIDATION_PLAN_ID.equals(planId)) {
            return plan(REVALIDATION_PLAN_ID, "NEEDS_REVALIDATION", true);
        }
        return null;
    }

    private ExecutionPlanDO plan(String planId, String lifecycle, boolean needsRevalidation) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(planId);
        plan.setAnalysisId("ui-review-analysis-btc-001");
        plan.setCandidateId("ui-review-candidate-btc-001");
        plan.setResolverResultId("ui-review-resolver-btc-001");
        plan.setValidationResultId("ui-review-validation-btc-001");
        plan.setTraceId("ui-review-trace-btc-001");
        plan.setFinalPlan(true);
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setRuleValidationStatus("PASS");
        plan.setFinalMarketBias("WEAK_BULLISH");
        plan.setPlanMode("PREPARATION");
        plan.setFinalPlanMode("PREPARATION");
        plan.setRecommendedAction("等待触发；触发后重新校验，通过后再进入人工确认");
        plan.setEntryZone("62,800–63,200");
        plan.setTriggerCondition("15m 放量站稳 63,200");
        plan.setTriggerTimeframe("15m");
        plan.setInvalidCondition("4h 收盘跌破 61,500");
        plan.setStopLogic("4h 结构失效后停止等待");
        plan.setStopLoss("61,500 下方失效");
        plan.setTakeProfitRules("65,800 / 68,200 分批");
        plan.setTargetLogic("按阻力区间分批止盈");
        plan.setHoldingHorizon("24 小时");
        plan.setLeverageSuggestion("不高于 2×");
        plan.setPositionSuggestion("账户风险预算 12%");
        plan.setRiskExplanation("轻微冲突限制仓位与杠杆");
        plan.setRevalidationRule("结构或数据变化后重新校验");
        plan.setPlanLifecycleState(lifecycle);
        plan.setNeedsRevalidation(needsRevalidation);
        plan.setRevalidationReason(needsRevalidation ? "触发条件或数据状态发生变化" : null);
        plan.setPlanVersion(3);
        plan.setValidUntil(LocalDateTime.of(2026, 8, 22, 12, 0));
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        return plan;
    }
}
