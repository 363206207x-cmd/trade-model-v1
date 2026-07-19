package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderHealthRegistry {
    private final Clock clock;
    private final Map<String, MutableHealth> states = new ConcurrentHashMap<>();
    private final Map<ProviderSnapshotKey, MutableHealth> snapshotStates = new ConcurrentHashMap<>();

    public ProviderHealthRegistry() {
        this(Clock.systemUTC());
    }

    public ProviderHealthRegistry(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public void recordSuccess(String provider, UnifiedSourceStatus sourceStatus) {
        MutableHealth health = state(provider);
        health.lastSuccessAt = clock.instant();
        health.lastSourceStatus = sourceStatus;
        health.lastReasonCode = null;
    }

    public void recordFailure(String provider, UnifiedSourceStatus sourceStatus, String reasonCode) {
        MutableHealth health = state(provider);
        health.lastFailureAt = clock.instant();
        health.lastSourceStatus = sourceStatus;
        health.lastReasonCode = reasonCode;
    }

    public void recordSuccess(ProviderSnapshotKey key, UnifiedSourceStatus sourceStatus) {
        recordSuccess(key.provider(), sourceStatus);
        MutableHealth health = snapshotStates.computeIfAbsent(key, ignored -> new MutableHealth());
        health.lastSuccessAt = clock.instant();
        health.lastSourceStatus = sourceStatus;
        health.lastReasonCode = null;
    }

    public void recordFailure(ProviderSnapshotKey key, UnifiedSourceStatus sourceStatus, String reasonCode) {
        recordFailure(key.provider(), sourceStatus, reasonCode);
        MutableHealth health = snapshotStates.computeIfAbsent(key, ignored -> new MutableHealth());
        health.lastFailureAt = clock.instant();
        health.lastSourceStatus = sourceStatus;
        health.lastReasonCode = reasonCode;
    }

    public ProviderSnapshotHealthSnapshot get(ProviderSnapshotKey key) {
        MutableHealth health = snapshotStates.computeIfAbsent(key, ignored -> new MutableHealth());
        return new ProviderSnapshotHealthSnapshot(key, health.lastSourceStatus, health.lastSuccessAt,
                health.lastFailureAt, health.lastReasonCode);
    }

    public ProviderHealthSnapshot get(String provider, ProviderCircuitState circuitState) {
        MutableHealth health = state(provider);
        return new ProviderHealthSnapshot(normalize(provider), health.lastSourceStatus,
                circuitState, health.lastSuccessAt, health.lastFailureAt, health.lastReasonCode);
    }

    public Map<String, ProviderHealthSnapshot> snapshot(ProviderCircuitBreaker circuitBreaker) {
        Map<String, ProviderHealthSnapshot> result = new LinkedHashMap<>();
        states.keySet().stream().sorted().forEach(provider -> result.put(provider,
                get(provider, circuitBreaker.state(provider))));
        return Map.copyOf(result);
    }

    private MutableHealth state(String provider) {
        return states.computeIfAbsent(normalize(provider), ignored -> new MutableHealth());
    }

    private static String normalize(String provider) {
        return provider == null ? "UNKNOWN" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private static final class MutableHealth {
        private UnifiedSourceStatus lastSourceStatus = UnifiedSourceStatus.WAITING_SYNC;
        private Instant lastSuccessAt;
        private Instant lastFailureAt;
        private String lastReasonCode;
    }

    public record ProviderHealthSnapshot(
            String provider,
            UnifiedSourceStatus sourceStatus,
            ProviderCircuitState circuitState,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            String lastReasonCode
    ) {
    }

    public record ProviderSnapshotHealthSnapshot(
            ProviderSnapshotKey snapshotKey,
            UnifiedSourceStatus sourceStatus,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            String lastReasonCode
    ) {
    }
}
