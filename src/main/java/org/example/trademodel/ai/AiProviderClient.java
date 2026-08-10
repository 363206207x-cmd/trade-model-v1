package org.example.trademodel.ai;

public interface AiProviderClient {
    AiProviderName provider();
    AiProviderRole role();
    AiProviderReadiness readiness();
    AiProviderReviewResult review(AiProviderRequest request);
    default AiProviderReviewResult review(AiProviderRequest request, long timeoutOverrideMs) {
        return review(request);
    }
    AiProviderProperties providerProperties();

    default AiDecisionChainResult executeDecisionChain(AiDecisionChainRequest request, long timeoutOverrideMs) {
        return AiDecisionChainResult.failed(provider(), request == null ? null : request.getRole(),
                AiProviderCallStatus.FAILED, "DECISION_CHAIN_NOT_SUPPORTED");
    }
}
