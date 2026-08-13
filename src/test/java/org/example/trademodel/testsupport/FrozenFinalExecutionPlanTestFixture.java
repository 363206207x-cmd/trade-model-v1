package org.example.trademodel.testsupport;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.service.support.ExecutionFeasibilityContract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Complete, non-executable Final Execution Plan fixture for contract-bound tests. */
public final class FrozenFinalExecutionPlanTestFixture {
    private FrozenFinalExecutionPlanTestFixture() {
    }

    public static ExecutionPlanDO complete(String planId, String analysisId, LocalDateTime activeAt) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(planId);
        plan.setAnalysisId(analysisId);
        plan.setCandidateId("candidate-" + analysisId);
        plan.setOpportunityId("opportunity-" + analysisId);
        plan.setResolverResultId("resolver-" + analysisId);
        plan.setValidationResultId("validation-" + analysisId);
        plan.setTraceId("trace-" + analysisId);
        plan.setAssetId(1L);
        plan.setRuleVersion("FUNDAMENTAL_AI_V4_1_TEST");

        plan.setFinalPlan(true);
        plan.setRuleValidationStatus("PASS");
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setSourceCompletenessSummary("complete controlled test sources");
        plan.setSourceStatus("VALID");

        plan.setPlanMode("CONFIRMATION");
        plan.setRuleMarketBias("BULLISH");
        plan.setFinalMarketBias("BULLISH");
        plan.setCandidatePlanMode("CONFIRMATION");
        plan.setFinalPlanMode("CONFIRMATION");
        plan.setBiasAdjustmentReason("RULE_DIRECTION_PRESERVED");
        plan.setPlanModeAdjustmentReason("RULE_DIRECTION_PRESERVED");
        plan.setAdjustmentReason("RULE_DIRECTION_PRESERVED");
        plan.setOpportunityType("TREND_CONTINUATION");
        plan.setRecommendedAction("MANUAL_REVIEW");

        plan.setEntryLogic("verified continuation entry logic");
        plan.setEntryZone("100-101");
        plan.setEntrySource("test://entry-source");
        plan.setEntryReason("controlled entry boundary");
        plan.setTriggerCondition("manual confirmation after fresh analysis");
        plan.setStopLogic("rule invalidation boundary");
        plan.setStopLoss("95");
        plan.setStopSource("test://stop-source");
        plan.setStopReason("controlled stop boundary");
        plan.setTargetLogic("validated risk reward target");
        plan.setTakeProfitRules("110 then 120");
        plan.setTargetSource("test://target-source");
        plan.setTargetReason("controlled target boundary");
        plan.setAddPositionCondition("manual review confirms stronger evidence");
        plan.setReducePositionCondition("risk increases after manual review");
        plan.setAbandonCondition("verified source becomes unavailable");
        plan.setInvalidCondition("validated structure is invalidated");
        plan.setInvalidationSource("test://invalidation-source");
        plan.setInvalidationReason("controlled invalidation boundary");

        plan.setRiskExplanation("bounded manual decision risk");
        plan.setLeverageSuggestion("2x");
        plan.setLeverageLimit("2x");
        plan.setPositionSuggestion("manual risk reviewed size");
        plan.setPositionLimit("manual risk reviewed size");
        plan.setRiskLimit(new BigDecimal("0.10"));
        plan.setExpectedRiskReward(new BigDecimal("2.00"));
        plan.setExpectedRiskRewardSource("test://risk-reward-source");
        plan.setExpectedRiskRewardReason("validated entry stop target relation");
        plan.setAccountRiskSnapshotId(1L);
        plan.setAccountRiskJson("{\"riskAllowed\":true}");

        plan.setAnalysisTimeframesJson("[\"4h\",\"1h\",\"15m\",\"5m\"]");
        plan.setTriggerTimeframe("5m");
        plan.setValidFrom(activeAt.minusHours(1));
        plan.setValidUntil(activeAt.plusHours(12));
        plan.setHoldingHorizon("INTRADAY");
        plan.setRevalidationRule("refresh all verified evidence");
        plan.setDataQuality(90);
        plan.setSourceRefsJson("[\"test://analysis-source\"]");
        plan.setEvidenceRefsJson("[\"evidence-test-1\"]");
        plan.setScoreRefsJson("[\"score-test-1\"]");
        plan.setValidationReasons("[\"SOURCE_GATE_PASS\",\"RULE_DIRECTION_PRESERVED\"]");

        LocalDateTime feasibilityObservedAt = activeAt.minusMinutes(1);
        plan.setExecutionFeasibilityStatus(ExecutionFeasibilityContract.VERIFIED);
        plan.setSlippageStatus(ExecutionFeasibilityContract.VERIFIED);
        plan.setDepthStatus(ExecutionFeasibilityContract.VERIFIED);
        plan.setEntryDriftStatus(ExecutionFeasibilityContract.VERIFIED);
        plan.setTriggerStatus(ExecutionFeasibilityContract.VERIFIED);
        plan.setExecutionFeasibilityReason("CONTROLLED_VERIFIED_EXECUTION_CONTEXT");
        plan.setExecutionFeasibilityObservedAt(feasibilityObservedAt);
        plan.setExecutionFeasibilityFreshUntil(activeAt.plusHours(1));
        plan.setExecutionFeasibilitySourceRefsJson("[\"test://execution-feasibility\"]");

        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        plan.setNeedsRevalidation(false);
        plan.setFinalizedAt(activeAt.minusMinutes(1));
        plan.setCreateTime(activeAt.minusMinutes(2));
        return plan;
    }
}
