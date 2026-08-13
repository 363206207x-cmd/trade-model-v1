package org.example.trademodel.service.support;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.testsupport.FrozenFinalExecutionPlanTestFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionPlanReviewPolicyTest {

    @Test
    void onlyRuleValidatedFinalPlanCanBecomeActive() {
        ExecutionPlanDO plan = validFinal();
        assertThat(ExecutionPlanReviewPolicy.persistedPlanState(plan))
                .isEqualTo(ExecutionPlanReviewPolicy.PersistedPlanState.ACTIVE);

        plan.setFinalPlan(false);
        assertThat(ExecutionPlanReviewPolicy.persistedPlanState(plan))
                .isEqualTo(ExecutionPlanReviewPolicy.PersistedPlanState.INCOMPLETE);

        plan = validFinal();
        plan.setRuleValidationStatus("BLOCKED");
        assertThat(ExecutionPlanReviewPolicy.persistedPlanState(plan))
                .isEqualTo(ExecutionPlanReviewPolicy.PersistedPlanState.INCOMPLETE);

        plan = validFinal();
        plan.setCandidateId(null);
        assertThat(ExecutionPlanReviewPolicy.persistedPlanState(plan))
                .isEqualTo(ExecutionPlanReviewPolicy.PersistedPlanState.INCOMPLETE);
    }

    @Test
    void finalPlanRemainsManualAdviceAndCannotLoseSafetyBoundary() {
        ExecutionPlanDO plan = validFinal();
        plan.setNotTradeInstruction(false);

        assertThat(ExecutionPlanReviewPolicy.persistedPlanState(plan))
                .isEqualTo(ExecutionPlanReviewPolicy.PersistedPlanState.INCOMPLETE);
    }

    @Test
    void currentProjectionRequiresFreshFeasibilityWithoutRewritingHistoricalFinalState() {
        ExecutionPlanDO plan = validFinal();
        LocalDateTime afterFreshness = plan.getExecutionFeasibilityFreshUntil().plusSeconds(1);

        assertThat(ExecutionPlanReviewPolicy.persistedPlanState(plan))
                .isEqualTo(ExecutionPlanReviewPolicy.PersistedPlanState.ACTIVE);
        assertThat(ExecutionPlanReviewPolicy.currentProjectionPlanState(plan, afterFreshness))
                .isEqualTo(ExecutionPlanReviewPolicy.PersistedPlanState.REVALIDATION_REQUIRED);
    }

    private static ExecutionPlanDO validFinal() {
        return FrozenFinalExecutionPlanTestFixture.complete(
                "final-1", "analysis-1", LocalDateTime.now(ZoneOffset.UTC));
    }
}
