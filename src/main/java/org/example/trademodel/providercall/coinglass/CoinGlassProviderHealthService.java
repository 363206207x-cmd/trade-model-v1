package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoinGlassProviderHealthService {
    private final Map<String, CoinGlassEndpointHealth> health = new ConcurrentHashMap<>();

    public void record(String capabilityId, UnifiedSourceStatus status, int httpStatus,
                       String providerStatusCode, String reasonCode,
                       CoinGlassRateLimitMetadata rateLimit, Instant fetchTime) {
        health.put(capabilityId, new CoinGlassEndpointHealth(capabilityId, status, httpStatus,
                providerStatusCode, reasonCode, rateLimit, fetchTime));
    }

    public CoinGlassEndpointHealth get(String capabilityId) {
        return health.get(capabilityId);
    }

    public Map<String, CoinGlassEndpointHealth> snapshot() {
        return Map.copyOf(health);
    }

    public UnifiedSourceStatus configurationStatus(CoinGlassProperties properties) {
        CoinGlassConfigurationState configuration = configurationState(properties);
        if (configuration != CoinGlassConfigurationState.CONFIGURED) {
            return configuration == CoinGlassConfigurationState.INVALID_RPM
                    ? UnifiedSourceStatus.ERROR : UnifiedSourceStatus.NOT_CONFIGURED;
        }
        if (health.isEmpty()) return UnifiedSourceStatus.WAITING_SYNC;
        long ready = health.values().stream()
                .filter(value -> value.status() == UnifiedSourceStatus.READY).count();
        if (ready == 4 && health.size() == 4) return UnifiedSourceStatus.READY;
        if (ready > 0) return UnifiedSourceStatus.DEGRADED;
        if (health.values().stream().anyMatch(value -> value.status() == UnifiedSourceStatus.ERROR)) {
            return UnifiedSourceStatus.ERROR;
        }
        return UnifiedSourceStatus.DEGRADED;
    }

    public CoinGlassConfigurationState configurationState(CoinGlassProperties properties) {
        return properties == null ? CoinGlassConfigurationState.NOT_CONFIGURED : properties.configurationState();
    }

    public record CoinGlassEndpointHealth(
            String capabilityId,
            UnifiedSourceStatus status,
            int httpStatus,
            String providerStatusCode,
            String reasonCode,
            CoinGlassRateLimitMetadata rateLimit,
            Instant fetchTime
    ) {
    }
}
