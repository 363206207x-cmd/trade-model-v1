package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoinGlassProviderHealthServiceTest {
    @Test
    void durableRuntimeReadinessUsesProviderTimeAndRetainsEveryRequiredCapability() throws Exception {
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(
                new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        "jdbc:h2:mem:cg-runtime-readiness;DB_CLOSE_DELAY=-1", "sa", ""));
        String sql = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/db/migration/V23__coinglass_runtime_snapshot.sql"));
        jdbc.execute(sql.substring(0, sql.indexOf("-- New-table-only privileges")));
        CoinGlassProviderHealthService first = health();
        first.setRuntimeJdbcTemplate(jdbc);
        CAPABILITIES.forEach(capability -> first.recordDataset(capability, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null,
                NOW.minusSeconds(1), NOW.minusSeconds(100), java.time.Duration.ofSeconds(120)));
        CoinGlassProviderHealthService restarted = health();
        restarted.setRuntimeJdbcTemplate(jdbc);
        assertThat(restarted.snapshot()).isEmpty();
        assertThat(restarted.configurationStatus(properties())).isEqualTo(UnifiedSourceStatus.READY);
        assertThat(restarted.runtimeState()).isEqualTo("FRESH");
        restarted.recordDataset(CoinGlassV4ResponseValidator.FUNDING_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null,
                NOW, NOW.minusSeconds(500), java.time.Duration.ofSeconds(120));
        assertThat(restarted.configurationStatus(properties())).isEqualTo(UnifiedSourceStatus.STALE);
        assertThat(restarted.runtimeState()).isEqualTo("STALE");
    }

    @Test
    void cacheCadenceAloneDoesNotInventAScheduledProviderCheck() {
        CoinGlassProviderHealthService health = health();
        health.noteRefreshCadence(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                java.time.Duration.ofSeconds(120));
        health.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null,
                NOW, NOW.minusSeconds(1), java.time.Duration.ofSeconds(120));
        assertThat(health.runtimeSnapshot().values().iterator().next().nextCheckAt()).isNull();
        health.noteRefreshSchedule("BTCUSDT", java.time.Duration.ofSeconds(120));
        health.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null,
                NOW, NOW.minusSeconds(1), java.time.Duration.ofSeconds(120));
        assertThat(health.runtimeSnapshot().values().iterator().next().nextCheckAt())
                .isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void failedAttemptPreservesRealLastSuccessAndUsesActualRetryAfter() {
        CoinGlassProviderHealthService health = health();
        health.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null,
                NOW.minusSeconds(20), NOW.minusSeconds(25), java.time.Duration.ofSeconds(120));
        health.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.DEGRADED, 429, "429", "RATE_LIMITED",
                new CoinGlassRateLimitMetadata(300, 300, 45L), NOW, null, java.time.Duration.ofSeconds(120));

        var state = health.runtimeSnapshot().get(CoinGlassV4ResponseValidator.OI_CAPABILITY + "|BTCUSDT");
        assertThat(state.runtimeState()).isEqualTo("RATE_LIMITED");
        assertThat(state.lastAttemptAt()).isEqualTo(NOW);
        assertThat(state.lastSuccessAt()).isEqualTo(NOW.minusSeconds(20));
        assertThat(state.providerDataAt()).isEqualTo(NOW.minusSeconds(25));
        assertThat(state.nextCheckAt()).isEqualTo(NOW.plusSeconds(45));
        assertThat(state.stateVersion()).isEqualTo(2);
    }

    @Test
    void providerObservationNotFetchTimeControlsFreshnessAndLateResponsesCannotOverwrite() {
        CoinGlassProviderHealthService health = health();
        health.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null, NOW,
                NOW.minusSeconds(500), java.time.Duration.ofSeconds(120));
        assertThat(health.runtimeSnapshot().values().iterator().next().runtimeState()).isEqualTo("STALE");
        health.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null, NOW.minusSeconds(1),
                NOW.minusSeconds(1), java.time.Duration.ofSeconds(120));
        assertThat(health.runtimeSnapshot().values().iterator().next().providerDataAt())
                .isEqualTo(NOW.minusSeconds(500));
    }

    @Test
    void emptyStoreIsNotStartedAndActualAttemptBecomesRunningWithoutInventingSuccess() {
        CoinGlassProviderHealthService health = health();
        assertThat(health.runtimeSnapshot()).isEmpty();
        health.beginDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                java.time.Duration.ofSeconds(120));
        var running = health.runtimeSnapshot().values().iterator().next();
        assertThat(running.runtimeState()).isEqualTo("RUNNING");
        assertThat(running.lastAttemptAt()).isEqualTo(NOW);
        assertThat(running.lastSuccessAt()).isNull();
        assertThat(running.providerDataAt()).isNull();
    }

    @Test
    void durableLatestRowsSurviveRestartWithoutGrowingARequestLog() throws Exception {
        var source = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:cg-runtime-restart;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(source);
        String sql = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/db/migration/V23__coinglass_runtime_snapshot.sql"));
        jdbc.execute(sql.substring(0, sql.indexOf("-- New-table-only privileges")));
        CoinGlassProviderHealthService first = health();
        first.setRuntimeJdbcTemplate(jdbc);
        first.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.READY, 200, "0", "READY", null,
                NOW.minusSeconds(2), NOW.minusSeconds(3), java.time.Duration.ofSeconds(120));
        first.recordDataset(CoinGlassV4ResponseValidator.OI_CAPABILITY, "BTCUSDT",
                UnifiedSourceStatus.ERROR, 503, "503", "UPSTREAM_UNAVAILABLE", null,
                NOW, null, java.time.Duration.ofSeconds(120));
        CoinGlassProviderHealthService restarted = health();
        restarted.setRuntimeJdbcTemplate(jdbc);
        assertThat(restarted.runtimeSnapshot()).isEqualTo(first.runtimeSnapshot());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tm_coinglass_runtime_snapshot", Integer.class)).isEqualTo(1);
        assertThat(restarted.runtimeSnapshot().values().iterator().next().lastSuccessAt()).isEqualTo(NOW.minusSeconds(2));
    }

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
