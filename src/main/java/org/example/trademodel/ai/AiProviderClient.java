package org.example.trademodel.ai;

public interface AiProviderClient {
    AiProviderName provider();
    AiProviderRole role();
    AiProviderReadiness readiness();
    AiProviderReviewResult review(AiProviderRequest request);
    AiProviderProperties providerProperties();
}
