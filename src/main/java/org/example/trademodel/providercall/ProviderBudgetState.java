package org.example.trademodel.providercall;

import java.time.Instant;

public record ProviderBudgetState(
        String provider,
        int advertisedRpm,
        int effectiveRpm,
        double internalBudgetRatio,
        double emergencyReserveRatio,
        int currentWindowUsage,
        int remainingBudget,
        Instant retryAfter,
        ProviderCircuitState circuitState,
        AssetPriority rejectedPriority,
        int regularBudgetUsage,
        int emergencyBudgetUsage,
        int globalBudgetUsage,
        String lastRejectionReason
) {
    public ProviderBudgetState(String provider,
                               int advertisedRpm,
                               int effectiveRpm,
                               double internalBudgetRatio,
                               double emergencyReserveRatio,
                               int currentWindowUsage,
                               int remainingBudget,
                               Instant retryAfter,
                               ProviderCircuitState circuitState,
                               AssetPriority rejectedPriority) {
        this(provider, advertisedRpm, effectiveRpm, internalBudgetRatio, emergencyReserveRatio,
                currentWindowUsage, remainingBudget, retryAfter, circuitState, rejectedPriority,
                currentWindowUsage, 0, currentWindowUsage, null);
    }
}
