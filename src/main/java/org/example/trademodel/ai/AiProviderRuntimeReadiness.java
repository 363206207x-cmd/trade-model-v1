package org.example.trademodel.ai;

import java.time.Instant;

public record AiProviderRuntimeReadiness(
        String provider,
        String model,
        AiProviderReadinessState state,
        Instant verifiedAt,
        Instant expiresAt,
        String reasonCode,
        String requestId,
        String configVersion,
        AiConfigurationPresence rpmConfiguration,
        AiConfigurationPresence inputCostConfiguration,
        AiConfigurationPresence outputCostConfiguration,
        AiConfigurationPresence dailyBudgetConfiguration,
        AiConfigurationPresence perAnalysisBudgetConfiguration
) {
    public boolean ready() {
        return state == AiProviderReadinessState.AUTHORIZED;
    }
}
