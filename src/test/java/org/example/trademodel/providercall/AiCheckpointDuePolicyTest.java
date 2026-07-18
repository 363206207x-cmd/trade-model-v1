package org.example.trademodel.providercall;

import org.example.trademodel.enums.AiRoleEnum;
import org.example.trademodel.providercall.adapter.AiReviewRequest;
import org.example.trademodel.providercall.adapter.NoCallAiReviewProviderAdapter;
import org.example.trademodel.providercall.ai.AiCheckpoint;
import org.example.trademodel.providercall.ai.AiCheckpointDuePolicy;
import org.example.trademodel.providercall.ai.AiCheckpointDueRequest;
import org.example.trademodel.providercall.ai.AiCheckpointDueResult;
import org.example.trademodel.providercall.ai.AiCheckpointDueStatus;
import org.example.trademodel.providercall.ai.AiCheckpointStage;
import org.example.trademodel.providercall.ai.AiParticipationDepth;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiCheckpointDuePolicyTest {
    private static final Instant NOW = Instant.parse("2026-07-19T10:00:00Z");
    private final AiCheckpointDuePolicy policy = new AiCheckpointDuePolicy(new ProviderCallProperties());

    @Test
    void ordinaryDiscoveryScanIsRuleOnly() {
        AiCheckpointDueResult result = policy.decide(request(AiCheckpointStage.DISCOVERY_SCAN,
                RuntimeScanProfile.HIGH, true, true, Set.of()));
        assertThat(result.status()).isEqualTo(AiCheckpointDueStatus.RULE_ONLY);
        assertThat(result.depth()).isEqualTo(AiParticipationDepth.RULE_ONLY);
        assertThat(result.roles()).isEmpty();
    }

    @Test
    void candidatePromotionDepthVariesByProfile() {
        assertThat(policy.decide(request(AiCheckpointStage.CANDIDATE_PROMOTION,
                RuntimeScanProfile.LOW, true, true, Set.of())).roles())
                .containsExactly(AiRoleEnum.GPT_FINAL);
        assertThat(policy.decide(request(AiCheckpointStage.CANDIDATE_PROMOTION,
                RuntimeScanProfile.STANDARD, true, true, Set.of())).roles())
                .containsExactlyInAnyOrder(AiRoleEnum.GPT_FINAL, AiRoleEnum.GROK_CHALLENGE);
        assertThat(policy.decide(request(AiCheckpointStage.CANDIDATE_PROMOTION,
                RuntimeScanProfile.HIGH, true, true, Set.of())).roles())
                .containsExactlyInAnyOrder(AiRoleEnum.GPT_FINAL, AiRoleEnum.GEMINI_REVIEW,
                        AiRoleEnum.GROK_CHALLENGE);
    }

    @Test
    void triggeredAndMajorPositionEventsUseThreeRoleReview() {
        assertThat(policy.decide(request(AiCheckpointStage.TRIGGERED,
                RuntimeScanProfile.LOW, true, true, Set.of())).depth())
                .isEqualTo(AiParticipationDepth.THREE_ROLE_REVIEW);
        assertThat(policy.decide(request(AiCheckpointStage.POSITION_EVENT,
                RuntimeScanProfile.LOW, true, true, Set.of(AiCheckpoint.HOT_RESET))).roles())
                .containsExactlyInAnyOrder(AiRoleEnum.GPT_FINAL, AiRoleEnum.GEMINI_REVIEW,
                        AiRoleEnum.GROK_CHALLENGE);
    }

    @Test
    void missingConfigurationAndBudgetFailClosed() {
        assertThat(policy.decide(request(AiCheckpointStage.CANDIDATE,
                RuntimeScanProfile.STANDARD, false, true, Set.of())).status())
                .isEqualTo(AiCheckpointDueStatus.NOT_CONFIGURED);
        assertThat(policy.decide(request(AiCheckpointStage.CANDIDATE,
                RuntimeScanProfile.STANDARD, true, false, Set.of())).status())
                .isEqualTo(AiCheckpointDueStatus.BUDGET_BLOCKED);
    }

    @Test
    void sameEvidenceAndRuleVersionAreNotInvokedAgain() {
        AiCheckpointDueRequest request = new AiCheckpointDueRequest(AssetPriority.P2_CANDIDATE,
                RuntimeScanProfile.HIGH, AiCheckpointStage.CANDIDATE, Set.of(), true, true,
                "same", "same", "rule-v1", "rule-v1", NOW.minusSeconds(500), NOW);
        assertThat(policy.decide(request).status()).isEqualTo(AiCheckpointDueStatus.SAME_EVIDENCE);
        assertThat(policy.decide(request).roles()).isEmpty();
    }

    @Test
    void noCallAiAdapterNeverUsesNetwork() {
        NoCallAiReviewProviderAdapter adapter = new NoCallAiReviewProviderAdapter();
        var response = adapter.review(new AiReviewRequest(ProviderCallTestFixtures.spot("BTCUSDT"),
                Set.of(AiRoleEnum.GPT_FINAL), "evidence", "rule-v1", "trace-1"));
        assertThat(response.sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
        assertThat(adapter.networkCallCount()).isZero();
    }

    private static AiCheckpointDueRequest request(AiCheckpointStage stage,
                                                  RuntimeScanProfile profile,
                                                  boolean configured,
                                                  boolean budget,
                                                  Set<AiCheckpoint> checkpoints) {
        return new AiCheckpointDueRequest(AssetPriority.P2_CANDIDATE, profile, stage, checkpoints,
                configured, budget, "new-evidence", "old-evidence", "rule-v2", "rule-v1",
                null, NOW);
    }
}
