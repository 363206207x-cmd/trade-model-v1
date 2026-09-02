package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoinGlassProviderHealthService {
    private static final List<String> REQUIRED_CAPABILITIES = List.of(
            CoinGlassV4ResponseValidator.OI_CAPABILITY,
            CoinGlassV4ResponseValidator.FUNDING_CAPABILITY,
            CoinGlassV4ResponseValidator.LIQUIDATION_CAPABILITY,
            CoinGlassV4ResponseValidator.LONG_SHORT_CAPABILITY);

    private final Map<String, CoinGlassEndpointHealth> health = new ConcurrentHashMap<>();
    private final Clock clock;

    public CoinGlassProviderHealthService() {
        this(Clock.systemUTC());
    }

    CoinGlassProviderHealthService(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

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
        List<CoinGlassEndpointHealth> required = REQUIRED_CAPABILITIES.stream()
                .map(health::get)
                .toList();
        if (required.stream().allMatch(value -> value == null)) return UnifiedSourceStatus.WAITING_SYNC;
        if (required.stream().filter(value -> value != null)
                .anyMatch(value -> value.status() == UnifiedSourceStatus.ERROR)) {
            return UnifiedSourceStatus.ERROR;
        }
        long ready = required.stream()
                .filter(value -> value != null && value.status() == UnifiedSourceStatus.READY).count();
        if (ready == REQUIRED_CAPABILITIES.size()) {
            boolean fresh = required.stream().allMatch(value -> isFresh(value, properties));
            return fresh ? UnifiedSourceStatus.READY : UnifiedSourceStatus.STALE;
        }
        if (ready > 0) return UnifiedSourceStatus.DEGRADED;
        return UnifiedSourceStatus.DEGRADED;
    }

    private boolean isFresh(CoinGlassEndpointHealth value, CoinGlassProperties properties) {
        Instant fetchTime = value.fetchTime();
        Instant now = clock.instant();
        if (fetchTime == null || fetchTime.isAfter(now)) return false;
        long ttlSeconds = Math.max(1L, properties.getFreshTtlSeconds());
        return fetchTime.plusSeconds(ttlSeconds).isAfter(now);
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
