package org.example.trademodel.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HighValueAlertPolicyTest {
    private final HighValueAlertPolicy policy = new HighValueAlertPolicy();

    @Test
    void confirmationWithEveryTrustGatePasses() {
        assertThat(policy.allowsOpportunity(opportunity("CONFIRMATION"))).isTrue();
    }

    @Test
    void explicitlyAllowedHighQualityReducedPasses() {
        assertThat(policy.allowsOpportunity(withMode(opportunity("CONFIRMATION"), "REDUCED", true))).isTrue();
    }

    @Test
    void preparationObservationAndBlockedNeverBecomeDirectionalAlerts() {
        assertThat(policy.allowsOpportunity(withMode(opportunity("CONFIRMATION"), "PREPARATION", false))).isFalse();
        assertThat(policy.allowsOpportunity(withMode(opportunity("CONFIRMATION"), "OBSERVATION", false))).isFalse();
        assertThat(policy.allowsOpportunity(withMode(opportunity("CONFIRMATION"), "BLOCKED", false))).isFalse();
    }

    @Test
    void previewCandidateAndUnvalidatedFinalFailClosed() {
        HighValueAlertPolicy.OpportunityQualification base = opportunity("CONFIRMATION");
        assertThat(policy.allowsOpportunity(copy(base, true, false, true, true, false, "TRIGGERED"))).isFalse();
        assertThat(policy.allowsOpportunity(copy(base, false, true, false, true, false, "TRIGGERED"))).isFalse();
        assertThat(policy.allowsOpportunity(copy(base, false, false, true, false, false, "TRIGGERED"))).isFalse();
    }

    @Test
    void expiredAndConfusedOpportunityFailClosedButSafetyEventsRemainEligible() {
        HighValueAlertPolicy.OpportunityQualification base = opportunity("CONFIRMATION");
        assertThat(policy.allowsOpportunity(copy(base, false, false, true, true, true, "TRIGGERED"))).isFalse();
        assertThat(policy.allowsOpportunity(copy(base, false, false, true, true, false, "CONFUSED"))).isFalse();
        assertThat(policy.allowsOpportunity(copy(base, false, false, true, true, false, "HIGH_RISK"))).isFalse();
        assertThat(policy.allowsOpportunity(copy(base, false, false, true, true, false, "INVALIDATED"))).isFalse();
        assertThat(policy.allowsOpportunity(copy(base, false, false, true, true, false, "COOLING"))).isFalse();
        assertThat(policy.allowsSafetyChange(safety(HighValueAlertPolicy.SafetyChangeType.PLAN_EXPIRED))).isTrue();
        assertThat(policy.allowsSafetyChange(safety(HighValueAlertPolicy.SafetyChangeType.CONFUSED))).isTrue();
        assertThat(policy.allowsSafetyChange(safety(HighValueAlertPolicy.SafetyChangeType.HOT_RESET))).isTrue();
    }

    @Test
    void verifiedFreshMaterialPositionChangesPass() {
        assertThat(policy.allowsPosition(position("WEAKENED", "NO_REVERSAL", "MEDIUM", "STABLE", "LOGIC_WEAKENED"))).isTrue();
        assertThat(policy.allowsPosition(position("INVALIDATED", "NO_REVERSAL", "MEDIUM", "STABLE", "PLAN_INVALIDATED"))).isTrue();
        assertThat(policy.allowsPosition(position("STILL_VALID", "STRONG_REVERSAL", "MEDIUM", "STABLE", "LOGIC_VALID"))).isTrue();
        assertThat(policy.allowsPosition(position("STILL_VALID", "NO_REVERSAL", "HIGH", "INCREASED", "HIGH_RISK_OBSERVATION"))).isTrue();
        assertThat(policy.allowsPosition(position("STILL_VALID", "NO_REVERSAL", "MEDIUM", "STABLE", "NEAR_TAKE_PROFIT"))).isTrue();
    }

    @Test
    void pendingStaleInvalidAndOrdinaryPriceChangeNeverAlert() {
        HighValueAlertPolicy.PositionQualification material =
                position("WEAKENED", "NO_REVERSAL", "HIGH", "INCREASED", "LOGIC_WEAKENED");
        assertThat(policy.allowsPosition(new HighValueAlertPolicy.PositionQualification(
                41L, true, false, true, material.entryLogicStatus(), material.reversalStatus(),
                material.riskLevel(), material.riskTrend(), material.monitorConclusion()))).isFalse();
        assertThat(policy.allowsPosition(new HighValueAlertPolicy.PositionQualification(
                41L, true, true, false, material.entryLogicStatus(), material.reversalStatus(),
                material.riskLevel(), material.riskTrend(), material.monitorConclusion()))).isFalse();
        assertThat(policy.allowsPosition(new HighValueAlertPolicy.PositionQualification(
                41L, false, true, true, material.entryLogicStatus(), material.reversalStatus(),
                material.riskLevel(), material.riskTrend(), material.monitorConclusion()))).isFalse();
        assertThat(policy.allowsPosition(position(
                "STILL_VALID", "NO_REVERSAL", "LOW", "STABLE", "LOGIC_VALID"))).isFalse();
    }

    private static HighValueAlertPolicy.OpportunityQualification opportunity(String mode) {
        return new HighValueAlertPolicy.OpportunityQualification(
                41L, true, true, true, true, mode, false, "TRIGGERED", false,
                true, true, true, true, true, true, false, false, true, true);
    }

    private static HighValueAlertPolicy.OpportunityQualification withMode(
            HighValueAlertPolicy.OpportunityQualification value, String mode, boolean reducedAllowed) {
        return new HighValueAlertPolicy.OpportunityQualification(
                value.userId(), value.assetInPool(), value.persistedOpportunity(), value.finalPlan(),
                value.ruleValidated(), mode, reducedAllowed, value.opportunityState(), value.expired(),
                value.dataQualityPassed(), value.fresh(), value.sourceGatePassed(),
                value.executionFeasibilityPassed(), value.traceable(), value.pushSnapshotPresent(),
                value.preview(), value.candidateOnly(), value.notTradeInstruction(), value.notOrderExecution());
    }

    private static HighValueAlertPolicy.OpportunityQualification copy(
            HighValueAlertPolicy.OpportunityQualification value,
            boolean preview, boolean candidateOnly, boolean finalPlan, boolean ruleValidated,
            boolean expired, String state) {
        return new HighValueAlertPolicy.OpportunityQualification(
                value.userId(), value.assetInPool(), value.persistedOpportunity(), finalPlan,
                ruleValidated, value.finalPlanMode(), value.highQualityReducedAllowed(), state, expired,
                value.dataQualityPassed(), value.fresh(), value.sourceGatePassed(),
                value.executionFeasibilityPassed(), value.traceable(), value.pushSnapshotPresent(),
                preview, candidateOnly, value.notTradeInstruction(), value.notOrderExecution());
    }

    private static HighValueAlertPolicy.SafetyQualification safety(HighValueAlertPolicy.SafetyChangeType type) {
        return new HighValueAlertPolicy.SafetyQualification(41L, type, true, true, true);
    }

    private static HighValueAlertPolicy.PositionQualification position(
            String logic, String reversal, String risk, String trend, String conclusion) {
        return new HighValueAlertPolicy.PositionQualification(
                41L, true, true, true, logic, reversal, risk, trend, conclusion);
    }
}
