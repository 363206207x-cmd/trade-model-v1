package org.example.trademodel.providercall;

public interface ProviderRateBudget {
    boolean reserve(ProviderRequestKey key, AssetPriority priority, RuntimeScanProfile profile);
    default boolean reserveAttempt(ProviderRequestKey key, AssetPriority priority,
                                   RuntimeScanProfile profile, boolean retryAttempt) {
        return reserve(key, priority, profile);
    }
    void applyRetryAfter(String provider, long seconds);
    ProviderBudgetState state(String provider, ProviderCircuitState circuitState);
}
