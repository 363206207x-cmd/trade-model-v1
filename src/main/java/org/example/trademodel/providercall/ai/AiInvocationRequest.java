package org.example.trademodel.providercall.ai;

import java.math.BigDecimal;
import java.util.Set;

public record AiInvocationRequest(
        Set<AiCheckpoint> checkpoints,
        BigDecimal dataQualityScore,
        BigDecimal minimumDataQualityScore,
        String evidenceHash,
        String previousEvidenceHash,
        boolean budgetAvailable,
        boolean rateLimitAvailable,
        boolean providerAvailable,
        boolean geminiReviewRequired,
        boolean grokChallengeRequired
) {
    public AiInvocationRequest {
        checkpoints = checkpoints == null ? Set.of() : Set.copyOf(checkpoints);
    }
}
