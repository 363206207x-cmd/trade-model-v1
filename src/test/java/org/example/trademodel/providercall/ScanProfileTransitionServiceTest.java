package org.example.trademodel.providercall;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.providercall.profile.ProfileTransitionResult;
import org.example.trademodel.providercall.profile.ProfileTransitionSignal;
import org.example.trademodel.providercall.profile.ScanProfileTransitionService;
import org.example.trademodel.service.RuleConfigService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScanProfileTransitionServiceTest {

    @Test
    void downgradeRequiresRecoveryCycles() {
        Fixture fixture = fixture();
        ProfileTransitionResult emergency = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                hotReset(), "trace-1");
        fixture.clock.advance(Duration.ofSeconds(301));

        ProfileTransitionResult first = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-2");
        ProfileTransitionResult second = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-3");

        assertThat(emergency.effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(first.effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(second.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void downgradeMovesOneLevelAtATime() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, hotReset(), "trace-1");
        fixture.clock.advance(Duration.ofSeconds(301));
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, ProfileTransitionSignal.recovery(), "trace-2");
        ProfileTransitionResult result = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-3");
        assertThat(result.previousProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(result.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void profileDoesNotFlapInsideCooldown() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highSignal(), "trace-1");
        fixture.clock.advance(Duration.ofSeconds(100));
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, ProfileTransitionSignal.recovery(), "trace-2");
        ProfileTransitionResult held = fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO,
                ProfileTransitionSignal.recovery(), "trace-3");
        assertThat(held.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(held.effectiveReason()).isEqualTo("HYSTERESIS_HOLD");
    }

    @Test
    void scanProfileRuntimeEndpointExplainsEffectiveReason() {
        Fixture fixture = fixture();
        ProfileTransitionResult result = fixture.service.evaluate("BTCUSDT", UserScanProfile.STANDARD,
                hotReset(), "trace-1");
        assertThat(result.effectiveReason()).isEqualTo("HOT_RESET");
        assertThat(result.ruleVersion()).isEqualTo("v-test");
        assertThat(result.traceId()).isEqualTo("trace-1");
        verify(fixture.mapper, times(1)).insert(any());
    }

    @Test
    void manualHighIsNotAutoDowngraded() {
        Fixture fixture = fixture();
        ProfileTransitionResult result = fixture.service.evaluate("BTCUSDT", UserScanProfile.HIGH,
                ProfileTransitionSignal.recovery(), "trace-1");
        assertThat(result.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void missingThresholdConfigFailsClosedWithoutTransition() {
        RuleConfigService rules = mock(RuleConfigService.class);
        when(rules.getRuleConfigMap()).thenReturn(Map.of());
        RuleVersionLogMapper mapper = mock(RuleVersionLogMapper.class);
        ScanProfileTransitionService service = new ScanProfileTransitionService(rules, mapper,
                Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC));
        ProfileTransitionResult result = service.evaluate("BTCUSDT", UserScanProfile.LOW, hotReset(), "trace-1");
        assertThat(result.changed()).isFalse();
        assertThat(result.effectiveReason()).isEqualTo("PROFILE_RULE_CONFIG_UNAVAILABLE");
    }

    @Test
    void nearStopRaisesOnlyAffectedPositionProfile() {
        Fixture fixture = fixture();
        ProfileTransitionSignal nearStop = new ProfileTransitionSignal(null, null, null, null,
                BigDecimal.ZERO, null, null, null, null, false, null, false, false, null);
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, nearStop, "trace-stop")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(fixture.service.currentProfile("ETHUSDT")).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void nearTargetRaisesOnlyAffectedPositionProfile() {
        Fixture fixture = fixture();
        ProfileTransitionSignal nearTarget = new ProfileTransitionSignal(null, null, null, null,
                null, BigDecimal.ZERO, null, null, null, false, null, false, false, null);
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, nearTarget, "trace-target")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(fixture.service.currentProfile("ETHUSDT")).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void hotResetRaisesAffectedAssetsToEmergency() {
        Fixture fixture = fixture();
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, hotReset(), "trace-hot")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(fixture.service.currentProfile("ETHUSDT")).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void highRiskRaisesAffectedAssetProfile() {
        Fixture fixture = fixture();
        ProfileTransitionSignal highRisk = new ProfileTransitionSignal(null, null, null, null,
                null, null, null, null, null, true, null, false, true, null);
        assertThat(fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, highRisk, "trace-risk")
                .effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void profileTransitionIsAudited() {
        Fixture fixture = fixture();
        fixture.service.evaluate("BTCUSDT", UserScanProfile.AUTO, hotReset(), "trace-audit");
        verify(fixture.mapper).insert(any());
    }

    private static Fixture fixture() {
        RuleConfigService rules = mock(RuleConfigService.class);
        when(rules.getRuleConfigMap()).thenReturn(ruleMap());
        when(rules.resolveActiveRuleVersion()).thenReturn("v-test");
        RuleVersionLogMapper mapper = mock(RuleVersionLogMapper.class);
        when(mapper.insert(any())).thenReturn(1);
        MutableClock clock = new MutableClock(Instant.parse("2026-07-10T10:00:00Z"));
        return new Fixture(new ScanProfileTransitionService(rules, mapper, clock), mapper, clock);
    }

    private static Map<String, RuleConfigDO> ruleMap() {
        Map<String, RuleConfigDO> rules = new LinkedHashMap<>();
        value(rules, "emergency_price_movement_1m", "0.05");
        value(rules, "emergency_liquidation_spike", "90");
        value(rules, "emergency_confused_score", "85");
        value(rules, "high_price_movement_1m", "0.02");
        value(rules, "high_atr_multiple_5m", "2");
        value(rules, "high_volume_spike", "2.5");
        value(rules, "high_spread_spike", "2");
        value(rules, "high_open_interest_change", "0.10");
        value(rules, "high_funding_extremity", "80");
        value(rules, "near_boundary_distance", "0.01");
        value(rules, "data_quality_deterioration_score", "60");
        value(rules, "standard_confused_score", "55");
        value(rules, "high_min_hold_seconds", "300");
        value(rules, "emergency_min_hold_seconds", "120");
        value(rules, "recovery_confirm_cycles", "2");
        value(rules, "downgrade_cooldown_seconds", "300");
        return rules;
    }

    private static void value(Map<String, RuleConfigDO> rules, String suffix, String value) {
        RuleConfigDO row = new RuleConfigDO();
        row.setRuleKey("provider.scan." + suffix);
        row.setRuleValue(value);
        rules.put(row.getRuleKey(), row);
    }

    private static ProfileTransitionSignal hotReset() {
        return new ProfileTransitionSignal(null, null, null, null, null, null, null, null, null,
                false, null, true, false, null);
    }

    private static ProfileTransitionSignal highSignal() {
        return new ProfileTransitionSignal(new BigDecimal("0.03"), null, null, null, null, null, null,
                null, null, false, null, false, false, null);
    }

    private record Fixture(ScanProfileTransitionService service, RuleVersionLogMapper mapper, MutableClock clock) {}

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
