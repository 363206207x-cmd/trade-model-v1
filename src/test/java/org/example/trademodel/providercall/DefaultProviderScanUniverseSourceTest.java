package org.example.trademodel.providercall;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.providercall.profile.ScanProfileTransitionService;
import org.example.trademodel.providercall.profile.ProfileTransitionResult;
import org.example.trademodel.providercall.profile.ProviderCallProfilePreferenceService;
import org.example.trademodel.providercall.scan.DefaultProviderScanUniverseSource;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ScanUniverseInput;
import org.example.trademodel.providercall.candidate.AutoCandidateRegistry;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.universe.DiscoveryUniverseSource;
import org.example.trademodel.providercall.universe.WatchlistAssetSource;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.UserConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProviderScanUniverseSourceTest {
    private UserPositionMapper positionMapper;
    private AssetStateMapper stateMapper;
    private PushSnapshotMapper pushSnapshotMapper;
    private WatchlistAssetSource watchlistSource;
    private DiscoveryUniverseSource discoverySource;
    private ProviderCallProfilePreferenceService profilePreferenceService;
    private ProviderCallProperties properties;
    private UserConfigService userConfigService;
    private ScanProfileTransitionService transitions;
    private DefaultProviderScanUniverseSource source;

    @BeforeEach void setUp() {
        properties = new ProviderCallProperties();
        properties.setProfileEscalationEnabled(false);
        positionMapper = mock(UserPositionMapper.class);
        stateMapper = mock(AssetStateMapper.class);
        pushSnapshotMapper = mock(PushSnapshotMapper.class);
        watchlistSource = mock(WatchlistAssetSource.class);
        discoverySource = mock(DiscoveryUniverseSource.class);
        profilePreferenceService = mock(ProviderCallProfilePreferenceService.class);
        when(profilePreferenceService.getBaseProfile()).thenReturn(UserScanProfile.AUTO);
        when(watchlistSource.currentWatchlist()).thenReturn(instruments("BTCUSDT", "ETHUSDT"));
        when(discoverySource.currentDiscoveryUniverse()).thenReturn(instruments("SOLUSDT", "BNBUSDT"));
        userConfigService = mock(UserConfigService.class);
        transitions = mock(ScanProfileTransitionService.class);
        when(userConfigService.getUserConfig("admin")).thenReturn(new UserConfigDO());
        source = new DefaultProviderScanUniverseSource(properties, positionMapper, stateMapper,
                mock(DecisionResultMapper.class), pushSnapshotMapper, mock(PositionMonitorLogService.class), userConfigService,
                transitions, new ProviderRefreshStateRegistry(), watchlistSource,
                discoverySource, new AutoCandidateRegistry(), ProviderCallTestFixtures.binanceRegistry(
                        "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "ADAUSDT", "LINKUSDT"),
                profilePreferenceService);
    }

    @Test void realScanUniverseUsesReplaceableManualWatchlist() {
        emptySources();
        assertThat(source.currentUniverse().watchlistAssets()).containsExactlyElementsOf(
                instruments("BTCUSDT", "ETHUSDT"));
    }

    @Test void realScanUniverseIncludesManualOpenPositions() {
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(position("BTCUSDT", "OPEN"),
                position("ETHUSDT", "PARTIALLY_CLOSED")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        assertThat(source.currentUniverse().positions()).extracting(item -> item.symbol())
                .containsExactly("BTCUSDT", "ETHUSDT");
    }

    @Test void realScanUniverseExcludesClosedPositions() {
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(position("BTCUSDT", "CLOSED")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        assertThat(source.currentUniverse().positions()).isEmpty();
    }

    @Test void realScanUniverseIncludesCandidateAndWaitingTrigger() {
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of(
                state("SOLUSDT", AssetStateEnum.CANDIDATE), state("BNBUSDT", AssetStateEnum.WAITING_TRIGGER)));
        when(watchlistSource.currentWatchlist()).thenReturn(instruments("SOLUSDT", "BNBUSDT"));
        assertThat(source.currentUniverse().candidateAssets()).containsExactlyElementsOf(
                instruments("SOLUSDT", "BNBUSDT"));
    }

    @Test void realScanUniverseRemainsBounded() {
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(discoverySource.currentDiscoveryUniverse()).thenReturn(instruments("ADAUSDT", "LINKUSDT"));
        assertThat(source.currentUniverse().discoveryAssets()).containsExactlyElementsOf(
                instruments("ADAUSDT", "LINKUSDT"));
    }

    @Test void pushRecheckInvalidationRaisesOnlyAffectedAssetProfile() {
        properties.setProfileEscalationEnabled(true);
        when(transitions.evaluate(anyString(), any(), any(), anyString())).thenAnswer(invocation -> {
            String symbol = invocation.getArgument(0);
            var signal = (org.example.trademodel.providercall.profile.ProfileTransitionSignal) invocation.getArgument(2);
            RuntimeScanProfile profile = signal.highImpactEvent() ? RuntimeScanProfile.HIGH : RuntimeScanProfile.LOW;
            return new ProfileTransitionResult(symbol, RuntimeScanProfile.LOW, profile,
                    signal.highImpactEvent() ? "PUSH_RECHECK_BLOCKED" : "RECOVERY_SIGNAL",
                    Instant.parse("2026-07-10T00:00:00Z"), null, "v-test", true, "trace-test");
        });
        TmPushSnapshotDO push = new TmPushSnapshotDO();
        push.setSymbol("ADAUSDT");
        push.setPushStatus("RECHECK_INVALIDATED");
        when(pushSnapshotMapper.listRecent(anyInt())).thenReturn(List.of(push));
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(watchlistSource.currentWatchlist()).thenReturn(instruments("ADAUSDT"));
        DefaultProviderScanUniverseSource eventSource = new DefaultProviderScanUniverseSource(properties,
                positionMapper, stateMapper, mock(DecisionResultMapper.class), pushSnapshotMapper,
                mock(PositionMonitorLogService.class), userConfigService, transitions,
                new ProviderRefreshStateRegistry(), watchlistSource, discoverySource,
                new AutoCandidateRegistry(), ProviderCallTestFixtures.binanceRegistry(
                        "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "ADAUSDT", "LINKUSDT"),
                profilePreferenceService);

        ScanUniverseInput input = eventSource.evaluateUniverseForExecution("scan-cycle-push");

        CanonicalInstrumentId ada = ProviderCallTestFixtures.perpetual("ADAUSDT");
        assertThat(input.candidateAssets()).contains(ada);
        assertThat(input.symbolEscalations()).containsEntry(ada, RuntimeScanProfile.HIGH);
        assertThat(input.automaticProfile()).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test void currentUniverseNeverCallsEvaluateAndUsesCurrentTransitionStateOnly() {
        properties.setProfileEscalationEnabled(true);
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(position("BTCUSDT", "OPEN")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(transitions.current("BTCUSDT", "provider-universe-read-only"))
                .thenReturn(transition("BTCUSDT", RuntimeScanProfile.HIGH, "NEAR_USER_STOP", "read"));

        ScanUniverseInput input = source.currentUniverse();

        assertThat(input.symbolEscalations()).containsEntry(
                ProviderCallTestFixtures.perpetual("BTCUSDT"), RuntimeScanProfile.HIGH);
        assertThat(input.escalationReasons()).containsEntry(
                ProviderCallTestFixtures.perpetual("BTCUSDT"), "NEAR_USER_STOP");
        verify(transitions).current("BTCUSDT", "provider-universe-read-only");
        verify(transitions, never()).evaluate(anyString(), any(), any(), anyString());
    }

    @Test void executionUniverseCallsEvaluateOncePerRelevantInstrumentAndUsesSingleCycleTrace() {
        properties.setProfileEscalationEnabled(true);
        UserPositionDO btcPosition = position("BTCUSDT", "OPEN");
        AssetStateDO btcState = state("BTCUSDT", AssetStateEnum.CANDIDATE);
        TmPushSnapshotDO btcPush = new TmPushSnapshotDO();
        btcPush.setSymbol("BTCUSDT");
        btcPush.setPushStatus("RECHECK_INVALIDATED");
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(btcPosition));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of(btcState));
        when(pushSnapshotMapper.listRecent(anyInt())).thenReturn(List.of(btcPush));
        when(transitions.evaluate(eq("BTCUSDT"), any(), any(), anyString()))
                .thenReturn(transition("BTCUSDT", RuntimeScanProfile.HIGH, "HIGH_RISK", "execution"));

        source.evaluateUniverseForExecution("scan-cycle-1");

        verify(transitions, times(1)).evaluate(eq("BTCUSDT"), any(), any(),
                argThat(trace -> trace.startsWith("scan-cycle-1:") && trace.endsWith(":BTC/USDT")));
        verify(transitions, never()).current(anyString(), anyString());
    }

    @Test void readOnlyAndExecutionUniverseMembershipMatch() {
        properties.setProfileEscalationEnabled(true);
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(position("BTCUSDT", "OPEN")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of(
                state("ETHUSDT", AssetStateEnum.CANDIDATE)));
        when(transitions.current(anyString(), anyString())).thenAnswer(invocation ->
                transition(invocation.getArgument(0), RuntimeScanProfile.LOW,
                        "NO_RUNTIME_ESCALATION", "read"));
        when(transitions.evaluate(anyString(), any(), any(), anyString())).thenAnswer(invocation ->
                transition(invocation.getArgument(0), RuntimeScanProfile.LOW,
                        "RECOVERY_SIGNAL", "execution"));

        ScanUniverseInput readOnly = source.currentUniverse();
        ScanUniverseInput execution = source.evaluateUniverseForExecution("scan-cycle-membership");

        assertThat(execution.watchlistAssets()).isEqualTo(readOnly.watchlistAssets());
        assertThat(execution.positions()).isEqualTo(readOnly.positions());
        assertThat(execution.candidateAssets()).isEqualTo(readOnly.candidateAssets());
        assertThat(execution.discoveryAssets()).isEqualTo(readOnly.discoveryAssets());
    }

    @Test void executionUniverseRequiresScanCycleTrace() {
        assertThatThrownBy(() -> source.evaluateUniverseForExecution(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("scanCycleTraceId is required");
        verify(transitions, never()).evaluate(anyString(), any(), any(), anyString());
    }

    private void emptySources() {
        when(positionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
    }

    private static UserPositionDO position(String symbol, String status) {
        UserPositionDO row = new UserPositionDO();
        row.setAssetSymbol(symbol);
        row.setStatus(status);
        row.setSourceType("MANUAL");
        return row;
    }
    private static AssetStateDO state(String symbol, AssetStateEnum status) {
        AssetStateDO row = new AssetStateDO(); row.setSymbol(symbol); row.setState(status); return row;
    }

    private static List<CanonicalInstrumentId> instruments(String... symbols) {
        return java.util.Arrays.stream(symbols).map(ProviderCallTestFixtures::perpetual).toList();
    }

    private static ProfileTransitionResult transition(String symbol,
                                                       RuntimeScanProfile profile,
                                                       String reason,
                                                       String traceId) {
        return new ProfileTransitionResult(symbol, profile, profile, reason,
                Instant.parse("2026-07-10T00:00:00Z"), null, "v-test", false, traceId);
    }
}
