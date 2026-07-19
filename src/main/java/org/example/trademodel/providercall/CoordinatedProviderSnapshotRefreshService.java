package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

@Service
public class CoordinatedProviderSnapshotRefreshService implements ProviderSnapshotRefreshService {
    private final ProviderCallCoordinator coordinator;

    public CoordinatedProviderSnapshotRefreshService(ProviderCallCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public <T> ProviderCallResult<T> refresh(ProviderCallRequest<T> request) {
        return coordinator.execute(request);
    }
}
