package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.DateTimeException;
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
        if (key == null) throw new IllegalArgumentException("key is required");
        if (metadata == null) throw new IllegalArgumentException("metadata is required");
        if (metadata.fetchTime() == null) throw new IllegalArgumentException("metadata.fetchTime is required");
        if (metadata.expiresAt() == null) throw new IllegalArgumentException("metadata.expiresAt is required");
        if (staleRetention == null || staleRetention.isZero() || staleRetention.isNegative()) {
            throw new IllegalArgumentException("staleRetention must be positive");
        }
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
        if (key == null) throw new IllegalArgumentException("key is required");
        if (now == null) throw new IllegalArgumentException("now is required");
        CacheEntry<T> entry = (CacheEntry<T>) entries.get(key);
        if (entry == null) return SnapshotLookup.unavailable();
        if (entry.metadata == null || entry.metadata.fetchTime() == null
                || isRetentionExpired(now, entry.staleUntil)) {
            entries.remove(key, entry);
            return SnapshotLookup.unavailable();
        }
        if (requestedFreshTtl != null && (requestedFreshTtl.isZero() || requestedFreshTtl.isNegative())) {
            return new SnapshotLookup<>(entry.payload, entry.metadata, SnapshotFreshnessStatus.STALE_READABLE);
        }
        Instant requestedFreshUntil;
        if (requestedFreshTtl == null) {
            requestedFreshUntil = entry.metadata.expiresAt();
        } else {
            try {
                requestedFreshUntil = entry.metadata.fetchTime().plus(requestedFreshTtl);
            } catch (DateTimeException | ArithmeticException invalidFreshTtl) {
                requestedFreshUntil = entry.staleUntil;
            }
        }
        if (requestedFreshUntil == null) {
            entries.remove(key, entry);
            return SnapshotLookup.unavailable();
        }
        Instant effectiveFreshUntil = requestedFreshUntil.isBefore(entry.staleUntil)
                ? requestedFreshUntil : entry.staleUntil;
        if (now.isBefore(effectiveFreshUntil)) {
            return new SnapshotLookup<>(entry.payload, entry.metadata, SnapshotFreshnessStatus.FRESH);
        }
        return new SnapshotLookup<>(entry.payload, entry.metadata, SnapshotFreshnessStatus.STALE_READABLE);
    }

    @Override
    public int entryCount() {
        return entries.size();
    }

    @Override
    public int purgeExpired(Instant now) {
        if (now == null) throw new IllegalArgumentException("now is required");
        int before = entries.size();
        entries.entrySet().removeIf(entry -> isRetentionExpired(now, entry.getValue().staleUntil));
        return before - entries.size();
    }

    @Override
    public void clear() {
        entries.clear();
    }

    private static boolean isRetentionExpired(Instant now, Instant staleUntil) {
        return staleUntil == null || !now.isBefore(staleUntil);
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
