package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CoordinatedProviderSnapshotQueryService implements ProviderSnapshotQueryService {
    private final ProviderCallCoordinator coordinator;

    public CoordinatedProviderSnapshotQueryService(ProviderCallCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public <T> ProviderCallResult<T> query(ProviderRequestKey key,
                                           AssetPriority priority,
                                           Duration freshTtl,
                                           String traceId) {
        return coordinator.peek(key, priority, freshTtl, traceId);
    }
}
