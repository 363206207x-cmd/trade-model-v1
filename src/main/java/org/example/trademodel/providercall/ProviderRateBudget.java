package org.example.trademodel.providercall;

public interface ProviderRateBudget {
    boolean reserve(ProviderRequestKey key, AssetPriority priority, RuntimeScanProfile profile);
    void applyRetryAfter(String provider, long seconds);
    ProviderBudgetState state(String provider, ProviderCircuitState circuitState);
}
