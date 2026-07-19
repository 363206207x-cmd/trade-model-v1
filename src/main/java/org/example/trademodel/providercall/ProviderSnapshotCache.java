package org.example.trademodel.providercall;

import java.time.Duration;
import java.time.Instant;

public interface ProviderSnapshotCache {
    <T> void put(ProviderSnapshotKey key, T payload, ProviderSnapshotMetadata metadata, Duration staleRetention);
    <T> SnapshotCacheService.SnapshotLookup<T> lookup(ProviderSnapshotKey key, Instant now);
    <T> SnapshotCacheService.SnapshotLookup<T> lookup(
            ProviderSnapshotKey key, Instant now, Duration requestedFreshTtl);
    int entryCount();
    int purgeExpired(Instant now);
    void clear();
}
