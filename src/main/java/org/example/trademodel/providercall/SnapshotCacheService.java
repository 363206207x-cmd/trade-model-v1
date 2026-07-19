package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SnapshotCacheService implements ProviderSnapshotCache {
    private final Map<ProviderSnapshotKey, CacheEntry<?>> entries = new ConcurrentHashMap<>();

    @Override
    public <T> void put(ProviderSnapshotKey key, T payload, ProviderSnapshotMetadata metadata,
                        Duration staleRetention) {
        Instant staleUntil = metadata.fetchTime().plus(staleRetention);
        entries.put(key, new CacheEntry<>(payload, metadata, staleUntil));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> SnapshotLookup<T> lookup(ProviderSnapshotKey key, Instant now) {
        return lookup(key, now, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> SnapshotLookup<T> lookup(ProviderSnapshotKey key, Instant now, Duration requestedFreshTtl) {
        CacheEntry<T> entry = (CacheEntry<T>) entries.get(key);
        if (entry == null) return SnapshotLookup.unavailable();
        Instant requestedExpiry = requestedFreshTtl == null || entry.metadata.fetchTime() == null
                ? entry.metadata.expiresAt() : entry.metadata.fetchTime().plus(requestedFreshTtl);
        if (now.isBefore(requestedExpiry)) {
            return new SnapshotLookup<>(entry.payload, entry.metadata, SnapshotFreshnessStatus.FRESH);
        }
        if (now.isBefore(entry.staleUntil)) {
            return new SnapshotLookup<>(entry.payload, entry.metadata, SnapshotFreshnessStatus.STALE_READABLE);
        }
        entries.remove(key, entry);
        return SnapshotLookup.unavailable();
    }

    @Override
    public int entryCount() {
        return entries.size();
    }

    @Override
    public int purgeExpired(Instant now) {
        if (now == null) throw new IllegalArgumentException("now is required");
        int before = entries.size();
        entries.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().staleUntil));
        return before - entries.size();
    }

    @Override
    public void clear() {
        entries.clear();
    }

    private record CacheEntry<T>(T payload, ProviderSnapshotMetadata metadata, Instant staleUntil) {}

    public record SnapshotLookup<T>(T payload, ProviderSnapshotMetadata metadata, SnapshotFreshnessStatus freshness) {
        static <T> SnapshotLookup<T> unavailable() {
            return new SnapshotLookup<>(null, null, SnapshotFreshnessStatus.UNAVAILABLE);
        }
        public boolean fresh() { return freshness == SnapshotFreshnessStatus.FRESH && metadata != null; }
        public boolean staleReadable() {
            return freshness == SnapshotFreshnessStatus.STALE_READABLE && metadata != null;
        }
    }
}
