package org.example.trademodel.providercall;

import java.time.Duration;
import java.time.Instant;

public interface ProviderSnapshotCache {
    <T> void put(ProviderRequestKey key, T payload, ProviderSnapshotMetadata metadata, Duration staleTtl);
    <T> SnapshotCacheService.SnapshotLookup<T> lookup(ProviderRequestKey key, Instant now);
    <T> SnapshotCacheService.SnapshotLookup<T> lookup(
            ProviderRequestKey key, Instant now, Duration requestedFreshTtl);
    void clear();
}
