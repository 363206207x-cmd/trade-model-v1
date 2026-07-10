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
        AssetPriority rejectedPriority
) {
}
