package org.example.trademodel.service.impl;

import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class DecisionChainRuleValidatorImplTest {

    private final DecisionChainRuleValidatorImpl validator = new DecisionChainRuleValidatorImpl();

    @Test
    void validatedCandidateMustRemainInsideEveryRuleBoundary() {
        Fixture fixture = fixture();

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.passed()).isTrue();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void candidateCannotBecomeFinalMutateStateOrCreatePosition() {
        Fixture fixture = fixture();
        fixture.candidate().setNotFinalPlan(false);
        fixture.candidate().setRecommendedAction("AUTO_ORDER");

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.passed()).isFalse();
        assertThat(result.reasons()).contains(
                "CANDIDATE_SAFETY_BOUNDARY_VIOLATION",
                "AUTOMATIC_TRADING_ACTION_FORBIDDEN");
    }

    @Test
    void candidateAndResolverCannotOverrideRuleDirectionOrBoundaries() {
        Fixture fixture = fixture();
        fixture.candidate().setCandidateDirection("BEARISH");
        fixture.candidate().setStopLoss("90");
        fixture.conflict().setRuleDirectionPreserved(false);

        RuleValidationResult result = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "RULE_DIRECTION_MISMATCH",
                "CONFLICT_RESOLVER_RULE_DIRECTION_NOT_PRESERVED",
                "STOP_LOSS_NOT_RULE_VALIDATED");
    }

    @Test
    void unknownOrMorePermissiveResolverOutputsFailClosed() {
        Fixture fixture = fixture();
        fixture.conflict().setPlanModeAfter("UNRECOGNIZED");
        fixture.conflict().setConfidenceAfter("SUPER_HIGH");
        fixture.conflict().setRiskAfter("NONE");

        RuleValidationResult unknown = validator.validate(
                fixture.input(), fixture.opportunity(), fixture.candidate(), fixture.conflict());
        assertThat(unknown.reasons()).contains(
                "FINAL_PLAN_MODE_UNKNOWN",
                "FINAL_CONFIDENCE_UNKNOWN",
                "FINAL_RISK_UNKNOWN");

        Fixture watchOnly = fixture();
        watchOnly.input().decision().setIsWorthOpening(false);
        watchOnly.candidate().setWorthOpening(false);
        watchOnly.candidate().setPlanMode("WATCH");
        watchOnly.conflict().setPlanModeBefore("WATCH");
        watchOnly.conflict().setPlanModeAfter("PREPARE");
        RuleValidationResult permissive = validator.validate(
                watchOnly.input(), watchOnly.opportunity(), watchOnly.candidate(), watchOnly.conflict());
        assertThat(permissive.reasons()).contains("FINAL_PLAN_MODE_MORE_PERMISSIVE_THAN_RULE");
    }

    @Test
    void confusedOrIneligibleOpportunityCannotProduceFinalPlan() {
        Fixture fixture = fixture();
        fixture.input().decision().setConfusedScore(80);
        OpportunityTransitionResult confused = new OpportunityTransitionResult(
                "opp-btc", "BTCUSDT", AssetStateEnum.CANDIDATE, AssetStateEnum.CONFUSED,
                true, false, "CONFLICT", "CONFUSED", "BLOCKED", LocalDateTime.now());

        RuleValidationResult result = validator.validate(
                fixture.input(), confused, fixture.candidate(), fixture.conflict());

        assertThat(result.reasons()).contains(
                "OPPORTUNITY_STATE_NOT_ELIGIBLE",
                "OPPORTUNITY_EXECUTION_PERMISSION_BLOCKED",
                "CONFUSED_BLOCKED");
    }

    private static Fixture fixture() {
        OffsetDateTime expiresAt = OffsetDateTime.of(2026, 8, 12, 0, 0, 0, 0, ZoneOffset.UTC);
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setMarketBiasHierarchy("BULLISH");
        decision.setConfidenceLevel("HIGH");
        decision.setRiskLevel("MEDIUM");
        decision.setIsWorthOpening(true);
        decision.setConfusedScore(20);
        decision.setExpiresAt(expiresAt);

        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setPlanId("plan-1");
        plan.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        plan.setSourceGateStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID);
        plan.setSourceGateComplete(true);
        plan.setRecommendedAction("MANUAL_REVIEW");
        plan.setEntryZone("100-101");
        plan.setStopLoss("95");
        plan.setTakeProfitRules("110 then 120");
        plan.setLeverageSuggestion("1x");
        plan.setPositionSuggestion("small");
        plan.setInvalidCondition("close below 95");

        DecisionChainBuildInput input = new DecisionChainBuildInput(
                "analysis-1", "trace-1", "BTCUSDT", "5m", 90,
                decision, plan, List.of(), List.of(), AnalysisRunTriggerType.ASSET_POOL_SCAN);
        OpportunityTransitionResult opportunity = new OpportunityTransitionResult(
                "opp-btc", "BTCUSDT", AssetStateEnum.OBSERVING, AssetStateEnum.CANDIDATE,
                true, false, "PROMOTED", "ASSET_POOL_SCAN", "ADVISORY_ALLOWED", LocalDateTime.now());

        ExecutionPlanCandidateDO candidate = new ExecutionPlanCandidateDO();
        candidate.setCandidateId("candidate-1");
        candidate.setOpportunityId("opp-btc");
        candidate.setAnalysisId("analysis-1");
        candidate.setTraceId("trace-1");
        candidate.setRuleDirection("BULLISH");
        candidate.setRuleConfidence("HIGH");
        candidate.setRuleRisk("MEDIUM");
        candidate.setCandidateDirection("BULLISH");
        candidate.setPlanMode("CONFIRM");
        candidate.setConfidenceLevel("HIGH");
        candidate.setRiskLevel("MEDIUM");
        candidate.setWorthOpening(true);
        candidate.setRecommendedAction("MANUAL_REVIEW");
        candidate.setEntryZone("100-101");
        candidate.setStopLoss("95");
        candidate.setTakeProfitRules("110 then 120");
        candidate.setLeverageSuggestion("1x");
        candidate.setPositionSuggestion("small");
        candidate.setInvalidCondition("close below 95");
        candidate.setValidity(expiresAt.toString());
        candidate.setNotFinalPlan(true);
        candidate.setNotStateMachineMutation(true);
        candidate.setNotUserPositionCreation(true);

        ConflictResolverResultDO conflict = new ConflictResolverResultDO();
        conflict.setRuleDirection("BULLISH");
        conflict.setRuleConfidence("HIGH");
        conflict.setRuleRisk("MEDIUM");
        conflict.setPlanModeBefore("CONFIRM");
        conflict.setPlanModeAfter("CONFIRM");
        conflict.setConfidenceBefore("HIGH");
        conflict.setConfidenceAfter("HIGH");
        conflict.setRiskBefore("MEDIUM");
        conflict.setRiskAfter("MEDIUM");
        conflict.setRuleDirectionPreserved(true);
        conflict.setConfusedDecision(false);
        return new Fixture(input, opportunity, candidate, conflict);
    }

    private record Fixture(DecisionChainBuildInput input,
                           OpportunityTransitionResult opportunity,
                           ExecutionPlanCandidateDO candidate,
                           ConflictResolverResultDO conflict) {
    }
}
