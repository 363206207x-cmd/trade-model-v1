package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SnapshotCacheService {
    private final Map<ProviderRequestKey, CacheEntry<?>> entries = new ConcurrentHashMap<>();

    public <T> void put(ProviderRequestKey key, T payload, ProviderSnapshotMetadata metadata, Duration staleTtl) {
        entries.put(key, new CacheEntry<>(payload, metadata, metadata.expiresAt().plus(staleTtl)));
    }

    @SuppressWarnings("unchecked")
    public <T> SnapshotLookup<T> lookup(ProviderRequestKey key, Instant now) {
        return lookup(key, now, null);
    }

    @SuppressWarnings("unchecked")
    public <T> SnapshotLookup<T> lookup(ProviderRequestKey key, Instant now, Duration requestedFreshTtl) {
        CacheEntry<T> entry = (CacheEntry<T>) entries.get(key);
        if (entry == null) return SnapshotLookup.unavailable();
        Instant requestedExpiry = requestedFreshTtl == null || entry.metadata.fetchTime() == null
                ? entry.metadata.expiresAt() : entry.metadata.fetchTime().plus(requestedFreshTtl);
        if (now.isBefore(requestedExpiry)) {
            return new SnapshotLookup<>(entry.payload, entry.metadata, SnapshotFreshnessStatus.FRESH);
        }
        if (now.isBefore(entry.staleUntil)) {
            return new SnapshotLookup<>(entry.payload, entry.metadata, SnapshotFreshnessStatus.STALE);
        }
        entries.remove(key, entry);
        return SnapshotLookup.unavailable();
    }

    public void clear() {
        entries.clear();
    }

    private record CacheEntry<T>(T payload, ProviderSnapshotMetadata metadata, Instant staleUntil) {}

    public record SnapshotLookup<T>(T payload, ProviderSnapshotMetadata metadata, SnapshotFreshnessStatus freshness) {
        static <T> SnapshotLookup<T> unavailable() {
            return new SnapshotLookup<>(null, null, SnapshotFreshnessStatus.UNAVAILABLE);
        }
        public boolean fresh() { return freshness == SnapshotFreshnessStatus.FRESH && metadata != null; }
        public boolean staleReadable() { return freshness == SnapshotFreshnessStatus.STALE && metadata != null; }
    }
}
