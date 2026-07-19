package org.example.trademodel.providercall;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.RuleVersionLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.providercall.candidate.AutoCandidateRegistry;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.profile.FrequencyMatrixVersionService;
import org.example.trademodel.providercall.profile.ProfileTransitionResult;
import org.example.trademodel.providercall.profile.ProviderCallProfilePreferenceService;
import org.example.trademodel.providercall.profile.ProviderCallProfileResolver;
import org.example.trademodel.providercall.profile.ProviderDueTimePolicy;
import org.example.trademodel.providercall.profile.ScanProfileTransitionService;
import org.example.trademodel.providercall.scan.AssetPriorityResolver;
import org.example.trademodel.providercall.scan.DefaultProviderScanUniverseSource;
import org.example.trademodel.providercall.scan.ProviderDatasetRefreshPort;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ProviderScanCoordinatorScheduler;
import org.example.trademodel.providercall.scan.ProviderScanPlanServiceImpl;
import org.example.trademodel.providercall.scan.ScanUniverseResolver;
import org.example.trademodel.providercall.universe.DiscoveryUniverseSource;
import org.example.trademodel.providercall.universe.WatchlistAssetSource;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.UserConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderScanReadOnlyExecutionIntegrationTest {

    @Test
    void auditFailurePreventsRefreshExecution() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-19T10:00:00Z"));
        ProviderCallProperties properties = properties();
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(ruleConfigService.getRuleConfigMap()).thenReturn(ruleMap());
        when(ruleConfigService.resolveActiveRuleVersion()).thenReturn("v-test");
        RuleVersionLogMapper auditMapper = mock(RuleVersionLogMapper.class);
        when(auditMapper.insert(any())).thenThrow(new IllegalStateException("transition audit unavailable"));
        ScanProfileTransitionService transitions = new ScanProfileTransitionService(
                ruleConfigService, auditMapper, clock);
        ProviderSymbolMappingRegistry mappings = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        DefaultProviderScanUniverseSource source = source(
                properties, transitions, mappings, new AtomicBoolean(true), clock);
        FrequencyMatrixVersionService versionService = mock(FrequencyMatrixVersionService.class);
        when(versionService.currentVersion()).thenReturn("v-test");
        ScanUniverseResolver resolver = new ScanUniverseResolver(properties,
                new AssetPriorityResolver(mappings), new ProviderCallProfileResolver(),
                new ProviderDueTimePolicy(properties), versionService);
        ProviderScanPlanServiceImpl planService = new ProviderScanPlanServiceImpl(source, resolver);
        ProviderDatasetRefreshPort refreshPort = mock(ProviderDatasetRefreshPort.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ProviderDatasetRefreshPort> refreshPortProvider = mock(ObjectProvider.class);
        when(refreshPortProvider.getIfAvailable()).thenReturn(refreshPort);
        ProviderScanCoordinatorScheduler scheduler = new ProviderScanCoordinatorScheduler(
                properties, true, planService, refreshPortProvider);

        assertThatThrownBy(scheduler::scanOnce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("transition audit unavailable");

        assertThat(transitions.currentProfile("BTCUSDT")).isEqualTo(RuntimeScanProfile.LOW);
        assertThat(transitions.current("BTCUSDT", "read-after-audit-failure").effectiveReason())
                .isEqualTo("NO_RUNTIME_ESCALATION");
        verify(auditMapper).insert(any());
        verifyNoInteractions(refreshPort);
    }

    @Test
    void oneHundredReadOnlyPlansCannotReplaceTwoRealRecoveryScanCycles() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-19T10:00:00Z"));
        ProviderCallProperties properties = properties();
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(ruleConfigService.getRuleConfigMap()).thenReturn(ruleMap());
        when(ruleConfigService.resolveActiveRuleVersion()).thenReturn("v-test");
        RuleVersionLogMapper auditMapper = mock(RuleVersionLogMapper.class);
        when(auditMapper.insert(any())).thenReturn(1);
        ScanProfileTransitionService transitions = spy(new ScanProfileTransitionService(
                ruleConfigService, auditMapper, clock));
        ProviderSymbolMappingRegistry mappings = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        AtomicBoolean pushEscalation = new AtomicBoolean(true);
        DefaultProviderScanUniverseSource source = source(
                properties, transitions, mappings, pushEscalation, clock);
        FrequencyMatrixVersionService versionService = mock(FrequencyMatrixVersionService.class);
        when(versionService.currentVersion()).thenReturn("v-test");
        ScanUniverseResolver resolver = new ScanUniverseResolver(properties,
                new AssetPriorityResolver(mappings), new ProviderCallProfileResolver(),
                new ProviderDueTimePolicy(properties), versionService);
        ProviderScanPlanServiceImpl planService = new ProviderScanPlanServiceImpl(source, resolver);
        ProviderDatasetRefreshPort refreshPort = mock(ProviderDatasetRefreshPort.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ProviderDatasetRefreshPort> refreshPortProvider = mock(ObjectProvider.class);
        when(refreshPortProvider.getIfAvailable()).thenReturn(refreshPort);
        ProviderScanCoordinatorScheduler scheduler = new ProviderScanCoordinatorScheduler(
                properties, true, planService, refreshPortProvider);

        assertThat(scheduler.scanOnce()).isEqualTo(4);
        ProfileTransitionResult high = transitions.current("BTCUSDT", "assert-high");
        assertThat(high.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(high.effectiveReason()).isEqualTo("EXTERNAL_EVENT");
        pushEscalation.set(false);
        clock.advance(Duration.ofSeconds(301));

        for (int index = 0; index < 100; index++) {
            assertThat(planService.currentPlan()).singleElement().satisfies(item -> {
                assertThat(item.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
                assertThat(item.profileReasonCodes()).contains("EXTERNAL_EVENT");
            });
        }
        verify(auditMapper, times(1)).insert(any());

        assertThat(scheduler.scanOnce()).isEqualTo(4);
        assertThat(transitions.current("BTCUSDT", "assert-first-recovery").effectiveProfile())
                .isEqualTo(RuntimeScanProfile.HIGH);
        verify(auditMapper, times(1)).insert(any());

        assertThat(scheduler.scanOnce()).isEqualTo(4);
        assertThat(transitions.current("BTCUSDT", "assert-second-recovery").effectiveProfile())
                .isEqualTo(RuntimeScanProfile.STANDARD);
        verify(transitions, times(3)).evaluate(eq("BTCUSDT"), any(), any(), any());
        verify(auditMapper, times(2)).insert(any());
    }

    private static DefaultProviderScanUniverseSource source(
            ProviderCallProperties properties,
            ScanProfileTransitionService transitions,
            ProviderSymbolMappingRegistry mappings,
            AtomicBoolean pushEscalation,
            Clock clock) {
        UserPositionMapper positionMapper = mock(UserPositionMapper.class);
        UserPositionDO position = new UserPositionDO();
        position.setAssetSymbol("BTCUSDT");
        position.setSourceType("MANUAL");
        position.setStatus("OPEN");
        when(positionMapper.listOpenPositions()).thenReturn(List.of(position));
        AssetStateMapper stateMapper = mock(AssetStateMapper.class);
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        PushSnapshotMapper pushSnapshotMapper = mock(PushSnapshotMapper.class);
        when(pushSnapshotMapper.listRecent(anyInt())).thenAnswer(ignored -> {
            if (!pushEscalation.get()) return List.of();
            TmPushSnapshotDO push = new TmPushSnapshotDO();
            push.setSymbol("BTCUSDT");
            push.setPushStatus("RECHECK_INVALIDATED");
            return List.of(push);
        });
        UserConfigService userConfigService = mock(UserConfigService.class);
        when(userConfigService.getUserConfig("admin")).thenReturn(new UserConfigDO());
        WatchlistAssetSource watchlistSource = mock(WatchlistAssetSource.class);
        when(watchlistSource.currentWatchlist()).thenReturn(List.of(
                ProviderCallTestFixtures.perpetual("BTCUSDT")));
        DiscoveryUniverseSource discoverySource = mock(DiscoveryUniverseSource.class);
        when(discoverySource.currentDiscoveryUniverse()).thenReturn(List.of());
        ProviderCallProfilePreferenceService profilePreferenceService = mock(
                ProviderCallProfilePreferenceService.class);
        when(profilePreferenceService.getBaseProfile()).thenReturn(UserScanProfile.AUTO);
        return new DefaultProviderScanUniverseSource(properties, positionMapper, stateMapper,
                mock(DecisionResultMapper.class), pushSnapshotMapper, mock(PositionMonitorLogService.class),
                userConfigService, transitions, new ProviderRefreshStateRegistry(), watchlistSource,
                discoverySource, new AutoCandidateRegistry(), mappings, profilePreferenceService, clock);
    }

    private static ProviderCallProperties properties() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setSchedulerEnabled(true);
        properties.setProfileEscalationEnabled(true);
        properties.setAutoEscalationEnabled(true);
        return properties;
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

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
