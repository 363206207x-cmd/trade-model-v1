package org.example.trademodel.providercall.ai;

import org.example.trademodel.enums.AiRoleEnum;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class AiCheckpointDuePolicy {
    private static final Set<AiRoleEnum> GPT = Set.of(AiRoleEnum.GPT_FINAL);
    private static final Set<AiRoleEnum> GPT_GROK = Set.of(
            AiRoleEnum.GPT_FINAL, AiRoleEnum.GROK_CHALLENGE);
    private static final Set<AiRoleEnum> ALL = Set.of(
            AiRoleEnum.GPT_FINAL, AiRoleEnum.GEMINI_REVIEW, AiRoleEnum.GROK_CHALLENGE);

    private final ProviderCallProperties properties;

    public AiCheckpointDuePolicy(ProviderCallProperties properties) {
        this.properties = properties;
    }

    public AiCheckpointDueResult decide(AiCheckpointDueRequest request) {
        AiParticipationDepth depth = depth(request);
        if (depth == AiParticipationDepth.RULE_ONLY) {
            return result(AiCheckpointDueStatus.RULE_ONLY, depth, Set.of(),
                    "DISCOVERY_RULE_ONLY", null);
        }
        if (!request.providerConfigured()) {
            return result(AiCheckpointDueStatus.NOT_CONFIGURED, depth, Set.of(),
                    "AI_PROVIDER_NOT_CONFIGURED", null);
        }
        if (!request.budgetAvailable()) {
            return result(AiCheckpointDueStatus.BUDGET_BLOCKED, depth, Set.of(),
                    "AI_BUDGET_BLOCKED", null);
        }
        if (same(request.evidenceHash(), request.previousEvidenceHash())
                && same(request.ruleVersion(), request.previousRuleVersion())) {
            return result(AiCheckpointDueStatus.SAME_EVIDENCE, depth, Set.of(),
                    "SAME_EVIDENCE_AND_RULE_VERSION", null);
        }
        int debounce = properties.intervalSeconds(request.effectiveProfile(), request.priority(),
                ProviderDatasetType.AI_REVIEW);
        Instant next = request.lastEvaluatedAt() == null
                ? request.now() : request.lastEvaluatedAt().plusSeconds(debounce);
        if (request.now().isBefore(next)) {
            return result(AiCheckpointDueStatus.DEBOUNCED, depth, Set.of(),
                    "AI_CHECKPOINT_DEBOUNCE_ACTIVE", next);
        }
        return result(AiCheckpointDueStatus.DUE, depth, roles(depth), "AI_CHECKPOINT_DUE", next);
    }

    private static AiParticipationDepth depth(AiCheckpointDueRequest request) {
        if (request.stage() == AiCheckpointStage.DISCOVERY_SCAN) return AiParticipationDepth.RULE_ONLY;
        if (request.stage() == AiCheckpointStage.TRIGGERED) return AiParticipationDepth.THREE_ROLE_REVIEW;
        if (request.stage() == AiCheckpointStage.POSITION_EVENT
                && majorPositionEvent(request.checkpoints())) return AiParticipationDepth.THREE_ROLE_REVIEW;
        if (request.stage() == AiCheckpointStage.CANDIDATE_PROMOTION) {
            return switch (request.effectiveProfile()) {
                case LOW -> AiParticipationDepth.GPT_FINAL_PRELIMINARY;
                case STANDARD -> AiParticipationDepth.GPT_FINAL_AND_GROK_CHALLENGE;
                case HIGH, EMERGENCY -> AiParticipationDepth.THREE_ROLE_REVIEW;
            };
        }
        return request.effectiveProfile() == RuntimeScanProfile.LOW
                ? AiParticipationDepth.GPT_FINAL_AND_GROK_CHALLENGE
                : AiParticipationDepth.THREE_ROLE_REVIEW;
    }

    private static boolean majorPositionEvent(Set<AiCheckpoint> checkpoints) {
        return checkpoints.contains(AiCheckpoint.HOT_RESET)
                || checkpoints.contains(AiCheckpoint.CONFUSED_SCORE_INCREASED)
                || checkpoints.contains(AiCheckpoint.POSITION_PLAN_INVALIDATED)
                || checkpoints.contains(AiCheckpoint.STRONG_REVERSAL)
                || checkpoints.contains(AiCheckpoint.RISK_INCREASED)
                || checkpoints.contains(AiCheckpoint.PUSH_RECHECK);
    }

    private static Set<AiRoleEnum> roles(AiParticipationDepth depth) {
        return switch (depth) {
            case RULE_ONLY -> Set.of();
            case GPT_FINAL_PRELIMINARY -> GPT;
            case GPT_FINAL_AND_GROK_CHALLENGE -> GPT_GROK;
            case THREE_ROLE_REVIEW -> ALL;
        };
    }

    private static AiCheckpointDueResult result(AiCheckpointDueStatus status,
                                                AiParticipationDepth depth,
                                                Set<AiRoleEnum> roles,
                                                String reason,
                                                Instant nextEligibleAt) {
        return new AiCheckpointDueResult(status, depth, roles, List.of(reason), nextEligibleAt);
    }

    private static boolean same(String left, String right) {
        return left != null && !left.isBlank() && left.equals(right);
    }
}
