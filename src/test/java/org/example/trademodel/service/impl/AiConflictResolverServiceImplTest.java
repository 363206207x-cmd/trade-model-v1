package org.example.trademodel.service.impl;

import org.example.trademodel.enums.AiConflictLevelEnum;
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
}
