package org.example.trademodel.service.impl;

import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.service.AiConflictResult;
import org.example.trademodel.service.DecisionContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiConflictResolverServiceImplTest {

    private final AiConflictResolverServiceImpl service = new AiConflictResolverServiceImpl();

    @Test
    void alignedConflictPreservesDirectionAndConfidence() {
        DecisionContext context = baseContext();

        AiConflictResult result = service.resolve(context);

        assertThat(result.getLevel()).isEqualTo(AiConflictLevelEnum.LEVEL_1_CONSISTENT);
        assertThat(result.getBaseMarketBias()).isEqualTo("BULLISH");
        assertThat(result.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(result.getAdjustedConfidence()).isEqualTo("HIGH");
        assertThat(result.getRiskAdjustment()).isEqualTo("UNCHANGED");
        assertThat(result.getPlanMode()).isEqualTo("CONFIRM");
        assertThat(result.getAiObjectionCount()).isZero();
    }

    @Test
    void minorConflictSingleAiObjectionCannotForceIndefiniteWaitingOrConfused() {
        DecisionContext context = baseContext();
        context.setGeminiConsistentWithRule(false);
        context.setAiObjectionCount(1);
        context.setAiSupportCount(2);

        AiConflictResult result = service.resolve(context);

        assertThat(result.getLevel()).isEqualTo(AiConflictLevelEnum.LEVEL_2_LIGHT_DIVERGENCE);
        assertThat(result.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(result.getAdjustedConfidence()).isEqualTo("MEDIUM");
        assertThat(result.getPlanMode()).isEqualTo("REDUCED");
        assertThat(result.isSingleObjectionOnly()).isTrue();
        assertThat(result.getAiConflictScore()).isLessThanOrEqualTo(35);
        assertThat(result.getPlanMode()).isNotEqualTo("CONFUSED");
    }

    @Test
    void majorConflictRaisesRiskButDoesNotOverrideStateMachine() {
        DecisionContext context = baseContext();
        context.setGeminiConsistentWithRule(false);
        context.setGrokConsistentWithRule(false);
        context.setAiObjectionCount(2);
        context.setAiSupportCount(1);
        context.setMultiTimeframeAligned(false);
        context.setRiskTier("MEDIUM");
        context.setWorthOpening(false);

        AiConflictResult result = service.resolve(context);

        assertThat(result.getLevel()).isEqualTo(AiConflictLevelEnum.LEVEL_3_SIGNIFICANT_DIVERGENCE);
        assertThat(result.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(result.getAdjustedConfidence()).isEqualTo("LOW");
        assertThat(result.getRiskAdjustment()).isEqualTo("RAISED");
        assertThat(result.getPlanMode()).isEqualTo("PREPARE_ONLY");
        assertThat(result.isNotStateMachineOverride()).isTrue();
    }

    @Test
    void extremeConflictKeepsRuleDirectionAndOnlyContributesToConfusedState() {
        DecisionContext context = baseContext();
        context.setGptConsistentWithRule(false);
        context.setGeminiConsistentWithRule(false);
        context.setGrokConsistentWithRule(false);
        context.setAiObjectionCount(3);
        context.setAiSupportCount(0);
        context.setMultiTimeframeAligned(false);
        context.setRiskTier("HIGH");
        context.setWorthOpening(false);

        AiConflictResult result = service.resolve(context);

        assertThat(result.getLevel()).isEqualTo(AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE);
        assertThat(result.getBaseMarketBias()).isEqualTo("BULLISH");
        assertThat(result.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(result.getRiskAdjustment()).isEqualTo("HIGH");
        assertThat(result.getPlanMode()).isEqualTo("CONFUSED");
        assertThat(result.getConfusedContribution()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void missingRuleBaseOutputFailsClosed() {
        DecisionContext context = baseContext();
        context.setHasRuleBaseOutput(false);

        assertThatThrownBy(() -> service.resolve(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("规则层必须先产出基础方向");
    }

    @Test
    void eachAiRoleObjectionCannotChangeRuleDirection() {
        for (String role : Arrays.asList("GPT", "Gemini", "Grok")) {
            DecisionContext context = baseContext();
            if ("GPT".equals(role)) {
                context.setGptConsistentWithRule(false);
            } else if ("Gemini".equals(role)) {
                context.setGeminiConsistentWithRule(false);
            } else {
                context.setGrokConsistentWithRule(false);
            }
            context.setAiObjectionCount(1);
            context.setAiSupportCount(2);

            AiConflictResult result = service.resolve(context);

            assertThat(result.getFinalMarketBias()).as(role).isEqualTo("BULLISH");
            assertThat(result.isRuleDirectionPreserved()).isTrue();
            assertThat(result.isNotRuleBypass()).isTrue();
        }
    }

    @Test
    void aiConsensusCannotUpgradeRuleRejectedWorthOpening() {
        DecisionContext context = baseContext();
        context.setWorthOpening(false);

        AiConflictResult result = service.resolve(context);

        assertThat(result.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(context.getWorthOpening()).isFalse();
    }

    @Test
    void noSuccessfulAiRoleIsNotTreatedAsSupportOrConsistency() {
        DecisionContext context = baseContext();
        context.setAiSuccessfulProviderCount(0);
        context.setAiSupportCount(0);
        context.setAiObjectionCount(0);

        AiConflictResult result = service.resolve(context);

        assertThat(result.getLevel()).isNull();
        assertThat(result.getPlanMode()).isNull();
        assertThat(result.getAiConflictScore()).isZero();
        assertThat(context.getAiSupportCount()).isZero();
    }

    @Test
    void successfulAbstainIsNotTreatedAsSupport() {
        DecisionContext context = baseContext();
        context.setAiSuccessfulProviderCount(1);
        context.setAiSupportCount(0);
        context.setAiObjectionCount(0);

        AiConflictResult result = service.resolve(context);

        assertThat(result.getLevel()).isNull();
        assertThat(result.getPlanMode()).isNull();
        assertThat(context.getAiSupportCount()).isZero();
    }

    @Test
    void resultContainsOnlyReviewOnlySafetySemantics() {
        AiConflictResult result = service.resolve(baseContext());

        assertThat(result.isRuleDirectionPreserved()).isTrue();
        assertThat(result.isNotRuleBypass()).isTrue();
        assertThat(result.isNotStateMachineOverride()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(Arrays.stream(AiConflictResult.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .doesNotContain("overrideDirection", "forcedDirection", "triggeredState",
                        "orderAction", "executionAction", "positionAction",
                        "autoTradingAction", "providerPayload", "executablePayload");
    }

    @Test
    void unavailableAiRolesStayOnExplicitRuleFallbackWithoutFabricatedConflict() {
        ConflictResolverResultDO result = service.resolveDecisionChain(
                candidate(),
                "{\"fallback\":true,\"role\":\"GEMINI_REVIEW\",\"fallbackReason\":\"TIMEOUT\"}",
                "{\"fallback\":true,\"role\":\"GROK_CHALLENGE\",\"fallbackReason\":\"NOT_CONFIGURED\"}",
                90, 10, "READY");

        assertThat(result.getConflictScore()).isZero();
        assertThat(result.getConflictLevel()).isEqualTo("NONE");
        assertThat(result.getPlanModeAfter()).isEqualTo("CONFIRM");
        assertThat(result.getConfidenceAfter()).isEqualTo("HIGH");
        assertThat(result.getRiskAfter()).isEqualTo("LOW");
        assertThat(result.getDowngradeReason())
                .contains("GEMINI_UNAVAILABLE_RULE_FALLBACK", "GROK_UNAVAILABLE_RULE_FALLBACK");
        assertThat(result.getConfusedDecision()).isFalse();
    }

    @Test
    void reviewAndChallengeCanOnlyDowngradeConfidenceRiskAndPlanMode() {
        ConflictResolverResultDO result = service.resolveDecisionChain(
                candidate(),
                """
                {"verdict":"DOWNGRADE","conflictLevel":"MINOR","confidenceAdjustment":"DOWNGRADE_ONE",
                 "riskAdjustment":"RAISE_ONE","planModeAdjustment":"DOWNGRADE_ONE","reasons":["weak"],"summary":"review"}
                """,
                """
                {"opposingView":"event","riskLevel":"HIGH","challengeLevel":"MAJOR",
                 "majorCounterEvidence":true,"planModeImpact":"DOWNGRADE_TWO","reasons":["event"],"summary":"challenge"}
                """,
                90, 10, "READY");

        assertThat(result.getRuleDirection()).isEqualTo("BULLISH");
        assertThat(result.getRuleDirectionPreserved()).isTrue();
        assertThat(result.getConfidenceAfter()).isEqualTo("LOW");
        assertThat(result.getRiskAfter()).isEqualTo("EXTREME");
        assertThat(result.getPlanModeAfter()).isEqualTo("BLOCKED");
        assertThat(result.getDowngradeReason())
                .contains("GEMINI_DOWNGRADE", "GROK_MAJOR_COUNTER_EVIDENCE");
    }

    @Test
    void candidateDirectionMismatchProducesRuleVetoAndCannotChangeRuleDirection() {
        ExecutionPlanCandidateDO candidate = candidate();
        candidate.setCandidateDirection("BEARISH");

        ConflictResolverResultDO result = service.resolveDecisionChain(
                candidate,
                "{\"fallback\":true}",
                "{\"fallback\":true}",
                90, 10, "READY");

        assertThat(result.getRuleDirection()).isEqualTo("BULLISH");
        assertThat(result.getRuleVetoReason()).isEqualTo("CANDIDATE_DIRECTION_DIFFERS_FROM_RULE");
        assertThat(result.getPlanModeAfter()).isEqualTo("BLOCKED");
        assertThat(result.getConfusedDecision()).isFalse();
    }

    @Test
    void blockedPlanDoesNotChangeOpportunityToConfusedWithoutConfusedEvidence() {
        ConflictResolverResultDO result = service.resolveDecisionChain(
                candidate(),
                """
                {"verdict":"RISK_WARNING","conflictLevel":"NONE","confidenceAdjustment":"UNCHANGED",
                 "riskAdjustment":"UNCHANGED","planModeAdjustment":"BLOCKED","reasons":["wait"],"summary":"review"}
                """,
                "{\"fallback\":true}",
                90, 10, "READY");

        assertThat(result.getPlanModeAfter()).isEqualTo("BLOCKED");
        assertThat(result.getConfusedDecision()).isFalse();
    }

    private static DecisionContext baseContext() {
        DecisionContext context = new DecisionContext();
        context.setRuleMarketBias("BULLISH");
        context.setRuleConfidenceLevel("HIGH");
        context.setHasRuleBaseOutput(true);
        context.setGptConsistentWithRule(true);
        context.setGeminiConsistentWithRule(true);
        context.setGrokConsistentWithRule(true);
        context.setAiSuccessfulProviderCount(3);
        context.setAiSupportCount(3);
        context.setAiObjectionCount(0);
        context.setMultiTimeframeAligned(true);
        context.setRiskTier("LOW");
        context.setWorthOpening(true);
        return context;
    }

    private static ExecutionPlanCandidateDO candidate() {
        ExecutionPlanCandidateDO candidate = new ExecutionPlanCandidateDO();
        candidate.setCandidateId("candidate-1");
        candidate.setAnalysisId("analysis-1");
        candidate.setTraceId("trace-1");
        candidate.setRuleDirection("BULLISH");
        candidate.setRuleConfidence("HIGH");
        candidate.setRuleRisk("LOW");
        candidate.setCandidateDirection("BULLISH");
        candidate.setPlanMode("CONFIRM");
        candidate.setConfidenceLevel("HIGH");
        candidate.setRiskLevel("LOW");
        return candidate;
    }
}
