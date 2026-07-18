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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultProviderScanUniverseSourceTest {
    private UserPositionMapper positionMapper;
    private AssetStateMapper stateMapper;
    private PushSnapshotMapper pushSnapshotMapper;
    private WatchlistAssetSource watchlistSource;
    private DiscoveryUniverseSource discoverySource;
    private ProviderCallProfilePreferenceService profilePreferenceService;
    private DefaultProviderScanUniverseSource source;

    @BeforeEach void setUp() {
        ProviderCallProperties properties = new ProviderCallProperties();
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
        UserConfigService userConfigService = mock(UserConfigService.class);
        when(userConfigService.getUserConfig("admin")).thenReturn(new UserConfigDO());
        source = new DefaultProviderScanUniverseSource(properties, positionMapper, stateMapper,
                mock(DecisionResultMapper.class), pushSnapshotMapper, mock(PositionMonitorLogService.class), userConfigService,
                mock(ScanProfileTransitionService.class), new ProviderRefreshStateRegistry(), watchlistSource,
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
        when(positionMapper.listOpenPositions()).thenReturn(List.of(position("BTCUSDT", "OPEN"),
                position("ETHUSDT", "PARTIALLY_CLOSED")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        assertThat(source.currentUniverse().positions()).extracting(item -> item.symbol())
                .containsExactly("BTCUSDT", "ETHUSDT");
    }

    @Test void realScanUniverseExcludesClosedPositions() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of(position("BTCUSDT", "CLOSED")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        assertThat(source.currentUniverse().positions()).isEmpty();
    }

    @Test void realScanUniverseIncludesCandidateAndWaitingTrigger() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of(
                state("SOLUSDT", AssetStateEnum.CANDIDATE), state("BNBUSDT", AssetStateEnum.WAITING_TRIGGER)));
        when(watchlistSource.currentWatchlist()).thenReturn(instruments("SOLUSDT", "BNBUSDT"));
        assertThat(source.currentUniverse().candidateAssets()).containsExactlyElementsOf(
                instruments("SOLUSDT", "BNBUSDT"));
    }

    @Test void realScanUniverseRemainsBounded() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(discoverySource.currentDiscoveryUniverse()).thenReturn(instruments("ADAUSDT", "LINKUSDT"));
        assertThat(source.currentUniverse().discoveryAssets()).containsExactlyElementsOf(
                instruments("ADAUSDT", "LINKUSDT"));
    }

    @Test void pushRecheckInvalidationRaisesOnlyAffectedAssetProfile() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setProfileEscalationEnabled(true);
        UserConfigService userConfigService = mock(UserConfigService.class);
        when(userConfigService.getUserConfig("admin")).thenReturn(new UserConfigDO());
        ScanProfileTransitionService transitions = mock(ScanProfileTransitionService.class);
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
        when(positionMapper.listOpenPositions()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(watchlistSource.currentWatchlist()).thenReturn(instruments("ADAUSDT"));
        DefaultProviderScanUniverseSource eventSource = new DefaultProviderScanUniverseSource(properties,
                positionMapper, stateMapper, mock(DecisionResultMapper.class), pushSnapshotMapper,
                mock(PositionMonitorLogService.class), userConfigService, transitions,
                new ProviderRefreshStateRegistry(), watchlistSource, discoverySource,
                new AutoCandidateRegistry(), ProviderCallTestFixtures.binanceRegistry(
                        "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "ADAUSDT", "LINKUSDT"),
                profilePreferenceService);

        ScanUniverseInput input = eventSource.currentUniverse();

        CanonicalInstrumentId ada = ProviderCallTestFixtures.perpetual("ADAUSDT");
        assertThat(input.candidateAssets()).contains(ada);
        assertThat(input.symbolEscalations()).containsEntry(ada, RuntimeScanProfile.HIGH);
        assertThat(input.automaticProfile()).isEqualTo(RuntimeScanProfile.LOW);
    }

    private void emptySources() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of());
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
}
