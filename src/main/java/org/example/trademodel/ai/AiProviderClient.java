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

    default AiProviderReviewResult verifyExactModel(String model, long timeoutOverrideMs) {
        return AiProviderReviewResult.skipped(provider(), role(), AiProviderCallStatus.FAILED,
                "EXACT_MODEL_VERIFICATION_NOT_SUPPORTED");
    }

    default AiDecisionChainResult executeDecisionChain(AiDecisionChainRequest request, long timeoutOverrideMs) {
        return AiDecisionChainResult.failed(provider(), request == null ? null : request.getRole(),
                AiProviderCallStatus.FAILED, "DECISION_CHAIN_NOT_SUPPORTED");
    }

    default boolean supportsNativeBackgroundDecisionChain() {
        return false;
    }

    default AiDecisionChainResult submitDecisionChainBackground(AiDecisionChainRequest request,
                                                                 long timeoutOverrideMs) {
        return AiDecisionChainResult.failed(provider(), request == null ? null : request.getRole(),
                AiProviderCallStatus.FAILED, "BACKGROUND_DECISION_CHAIN_NOT_SUPPORTED");
    }

    default AiDecisionChainResult pollDecisionChainBackground(AiDecisionChainRequest request,
                                                               String providerResponseId,
                                                               long timeoutOverrideMs) {
        return AiDecisionChainResult.failed(provider(), request == null ? null : request.getRole(),
                AiProviderCallStatus.FAILED, "BACKGROUND_DECISION_CHAIN_NOT_SUPPORTED");
    }

    default boolean cancelDecisionChainBackground(String providerResponseId, long timeoutOverrideMs) {
        return false;
    }
}
