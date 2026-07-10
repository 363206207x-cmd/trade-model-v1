package org.example.trademodel.providercall;

public record ProviderCallResult<T>(
        T payload,
        ProviderSnapshotMetadata metadata,
        ProviderBudgetState budgetState
) {
    public boolean ready() {
        return payload != null && metadata != null && metadata.sourceStatus() == UnifiedSourceStatus.READY;
    }
}
