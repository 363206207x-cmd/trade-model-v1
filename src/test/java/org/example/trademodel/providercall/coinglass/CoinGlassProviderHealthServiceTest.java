package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoinGlassProviderHealthServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-02T08:00:00Z");
    private static final List<String> CAPABILITIES = List.of(
            CoinGlassV4ResponseValidator.OI_CAPABILITY,
            CoinGlassV4ResponseValidator.FUNDING_CAPABILITY,
            CoinGlassV4ResponseValidator.LIQUIDATION_CAPABILITY,
            CoinGlassV4ResponseValidator.LONG_SHORT_CAPABILITY);

    @Test
    void allRequiredCapabilitiesMustBeReadyAndFresh() {
        CoinGlassProviderHealthService health = health();
        CAPABILITIES.forEach(capability -> record(
                health, capability, UnifiedSourceStatus.READY, NOW.minusSeconds(59)));

        assertThat(health.configurationStatus(properties())).isEqualTo(UnifiedSourceStatus.READY);
    }

    @Test
    void expiredOrFutureHealthCannotRemainReady() {
        CoinGlassProviderHealthService expired = health();
        CAPABILITIES.forEach(capability -> record(
                expired, capability, UnifiedSourceStatus.READY, NOW.minusSeconds(60)));
        CoinGlassProviderHealthService future = health();
        CAPABILITIES.forEach(capability -> record(
                future, capability, UnifiedSourceStatus.READY, NOW.plusSeconds(1)));

        assertThat(expired.configurationStatus(properties())).isEqualTo(UnifiedSourceStatus.STALE);
        assertThat(future.configurationStatus(properties())).isEqualTo(UnifiedSourceStatus.STALE);
    }

    @Test
    void missingRequiredCapabilityIsDegradedAndUnknownCapabilityDoesNotSubstitute() {
        CoinGlassProviderHealthService health = health();
        CAPABILITIES.subList(0, 3).forEach(capability -> record(
                health, capability, UnifiedSourceStatus.READY, NOW));
        record(health, "UNREGISTERED_CAPABILITY", UnifiedSourceStatus.READY, NOW);

        assertThat(health.configurationStatus(properties())).isEqualTo(UnifiedSourceStatus.DEGRADED);
    }

    @Test
    void requiredCapabilityErrorFailsClosed() {
        CoinGlassProviderHealthService health = health();
        CAPABILITIES.forEach(capability -> record(health, capability, UnifiedSourceStatus.READY, NOW));
        record(health, CoinGlassV4ResponseValidator.FUNDING_CAPABILITY, UnifiedSourceStatus.ERROR, NOW);

        assertThat(health.configurationStatus(properties())).isEqualTo(UnifiedSourceStatus.ERROR);
    }

    private static CoinGlassProviderHealthService health() {
        return new CoinGlassProviderHealthService(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static CoinGlassProperties properties() {
        CoinGlassProperties properties = new CoinGlassProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setApiKey("test-key");
        properties.setAdvertisedRpm(300);
        properties.setFreshTtlSeconds(60);
        return properties;
    }

    private static void record(CoinGlassProviderHealthService health, String capability,
                               UnifiedSourceStatus status, Instant fetchTime) {
        health.record(capability, status, status == UnifiedSourceStatus.READY ? 200 : 500,
                status == UnifiedSourceStatus.READY ? "0" : "ERROR", status.name(), null, fetchTime);
    }
}
