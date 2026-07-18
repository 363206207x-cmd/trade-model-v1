package org.example.trademodel.providercall.ai;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.RuntimeScanProfile;

import java.time.Instant;
import java.util.Set;

public record AiCheckpointDueRequest(
        AssetPriority priority,
        RuntimeScanProfile effectiveProfile,
        AiCheckpointStage stage,
        Set<AiCheckpoint> checkpoints,
        boolean providerConfigured,
        boolean budgetAvailable,
        String evidenceHash,
        String previousEvidenceHash,
        String ruleVersion,
        String previousRuleVersion,
        Instant lastEvaluatedAt,
        Instant now
) {
    public AiCheckpointDueRequest {
        checkpoints = checkpoints == null ? Set.of() : Set.copyOf(checkpoints);
        if (priority == null || effectiveProfile == null || stage == null || now == null) {
            throw new IllegalArgumentException("priority, effectiveProfile, stage and now are required");
        }
    }
}
