package org.example.trademodel.providercall;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.RuleConfigDO;
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
import org.example.trademodel.providercall.scan.DefaultProviderScanUniverseSource;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ScanUniverseInput;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.RuleConfigService;
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
    private RuleConfigService ruleConfigService;
    private PushSnapshotMapper pushSnapshotMapper;
    private DefaultProviderScanUniverseSource source;

    @BeforeEach void setUp() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setProfileEscalationEnabled(false);
        positionMapper = mock(UserPositionMapper.class);
        stateMapper = mock(AssetStateMapper.class);
        pushSnapshotMapper = mock(PushSnapshotMapper.class);
        ruleConfigService = mock(RuleConfigService.class);
        UserConfigService userConfigService = mock(UserConfigService.class);
        when(userConfigService.getUserConfig("admin")).thenReturn(new UserConfigDO());
        source = new DefaultProviderScanUniverseSource(properties, positionMapper, stateMapper,
                mock(DecisionResultMapper.class), pushSnapshotMapper, mock(PositionMonitorLogService.class), userConfigService,
                ruleConfigService, mock(ScanProfileTransitionService.class), new ProviderRefreshStateRegistry());
    }

    @Test void realScanUniverseIncludesSixCoreAssets() {
        emptySources();
        assertThat(source.currentUniverse().coreAssets()).containsExactly(
                "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");
    }

    @Test void realScanUniverseIncludesManualOpenPositions() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of(position("BTCUSDT", "OPEN"),
                position("ETHUSDT", "PARTIALLY_CLOSED")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of());
        assertThat(source.currentUniverse().positions()).extracting(item -> item.symbol())
                .containsExactly("BTCUSDT", "ETHUSDT");
    }

    @Test void realScanUniverseExcludesClosedPositions() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of(position("BTCUSDT", "CLOSED")));
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of());
        assertThat(source.currentUniverse().positions()).isEmpty();
    }

    @Test void realScanUniverseIncludesCandidateAndWaitingTrigger() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of(
                state("SOLUSDT", AssetStateEnum.CANDIDATE), state("BNBUSDT", AssetStateEnum.WAITING_TRIGGER)));
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of());
        assertThat(source.currentUniverse().candidateAssets()).containsExactly("SOLUSDT", "BNBUSDT");
    }

    @Test void realScanUniverseRemainsBounded() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        RuleConfigDO row = new RuleConfigDO();
        row.setRuleValue("ADAUSDT,ADAUSDT,INVALID,LINKUSDT");
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of("push.watchlist.symbols", row));
        assertThat(source.currentUniverse().poolAssets()).containsExactly("ADAUSDT", "LINKUSDT");
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
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of());
        DefaultProviderScanUniverseSource eventSource = new DefaultProviderScanUniverseSource(properties,
                positionMapper, stateMapper, mock(DecisionResultMapper.class), pushSnapshotMapper,
                mock(PositionMonitorLogService.class), userConfigService, ruleConfigService, transitions,
                new ProviderRefreshStateRegistry());

        ScanUniverseInput input = eventSource.currentUniverse();

        assertThat(input.candidateAssets()).contains("ADAUSDT");
        assertThat(input.symbolEscalations()).containsEntry("ADAUSDT", RuntimeScanProfile.HIGH);
        assertThat(input.automaticProfile()).isEqualTo(RuntimeScanProfile.LOW);
    }

    private void emptySources() {
        when(positionMapper.listOpenPositions()).thenReturn(List.of());
        when(stateMapper.listCandidateOrWaitingTrigger(anyInt())).thenReturn(List.of());
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of());
    }

    private static UserPositionDO position(String symbol, String status) {
        UserPositionDO row = new UserPositionDO(); row.setAssetSymbol(symbol); row.setStatus(status); return row;
    }
    private static AssetStateDO state(String symbol, AssetStateEnum status) {
        AssetStateDO row = new AssetStateDO(); row.setSymbol(symbol); row.setState(status); return row;
    }
}
