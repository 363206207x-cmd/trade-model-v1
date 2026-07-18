package org.example.trademodel.providercall;

public interface ProviderSnapshotRefreshService {
    <T> ProviderCallResult<T> refresh(ProviderCallRequest<T> request);
}
