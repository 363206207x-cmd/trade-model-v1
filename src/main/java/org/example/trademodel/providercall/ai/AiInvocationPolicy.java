package org.example.trademodel.providercall.ai;

import org.springframework.stereotype.Service;

@Service
public class AiInvocationPolicy {
    public AiInvocationStatus decide(AiInvocationRequest request) {
        if (request == null || request.checkpoints().isEmpty()) return AiInvocationStatus.SKIP_NOT_TRIGGERED;
        if (request.dataQualityScore() == null || request.minimumDataQualityScore() == null
                || request.dataQualityScore().compareTo(request.minimumDataQualityScore()) < 0) {
            return AiInvocationStatus.SKIP_DATA_QUALITY;
        }
        if (request.evidenceHash() == null || request.evidenceHash().isBlank()
                || request.evidenceHash().equals(request.previousEvidenceHash())) {
            return AiInvocationStatus.SKIP_SAME_EVIDENCE;
        }
        if (!request.budgetAvailable()) return AiInvocationStatus.SKIP_BUDGET;
        if (!request.rateLimitAvailable()) return AiInvocationStatus.SKIP_RATE_LIMIT;
        if (!request.providerAvailable()) return AiInvocationStatus.SKIP_PROVIDER_UNAVAILABLE;
        if (request.grokChallengeRequired()) return AiInvocationStatus.RUN_GROK;
        if (request.geminiReviewRequired()) return AiInvocationStatus.RUN_GEMINI;
        return AiInvocationStatus.RUN_GPT;
    }
}
