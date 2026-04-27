package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class MonitorAlertWriteServiceImplTest {

    @Mock
    private MonitorAlertMapper monitorAlertMapper;

    private MonitorAlertWriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MonitorAlertWriteServiceImpl(monitorAlertMapper);
    }

    @Test
    void emitsOpenAlertWithCooldown_whenNoThrottleAndNoSemanticSuppress() {
        when(monitorAlertMapper.countByAnalysisIdAndAlertType(any(), any())).thenReturn(0);
        when(monitorAlertMapper.countOpenInThrottleWindow(any(), any(), anyInt())).thenReturn(0);
        when(monitorAlertMapper.countAnyInSemanticWindow(any(), any(), anyInt())).thenReturn(0);
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setRiskLevel("HIGH");

        service.emitAfterAnalysisPersist(run(), analysis(), decision);

        ArgumentCaptor<MonitorAlertDO> captor = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(captor.capture());
        MonitorAlertDO row = captor.getValue();
        assertThat(row.getStatus()).isEqualTo("OPEN");
        assertThat(row.getAlertType()).isEqualTo(MonitorAlertWriteServiceImpl.ALERT_TYPE_HIGH_RISK_DECISION);
        assertThat(row.getCooldownUntil()).isNotBlank();
        assertThat(row.getSuppressReason()).isNull();
    }

    @Test
    void emitsSuppressed_whenOpenExistsInThrottleWindow() {
        when(monitorAlertMapper.countByAnalysisIdAndAlertType(any(), any())).thenReturn(0);
        when(monitorAlertMapper.countOpenInThrottleWindow(any(), any(), anyInt())).thenReturn(1);
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setRiskLevel("HIGH");

        service.emitAfterAnalysisPersist(run(), analysis(), decision);

        ArgumentCaptor<MonitorAlertDO> captor = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(captor.capture());
        MonitorAlertDO row = captor.getValue();
        assertThat(row.getStatus()).isEqualTo("SUPPRESSED");
        assertThat(row.getSuppressReason()).contains("THROTTLE_DB");
        assertThat(row.getCooldownUntil()).isNull();
    }

    @Test
    void emitsConfluenceBreakdownAndSkipsOpenBlocked_familyDedup() {
        when(monitorAlertMapper.countByAnalysisIdAndAlertType(any(), any())).thenReturn(0);
        when(monitorAlertMapper.countOpenInThrottleWindow(any(), any(), anyInt())).thenReturn(0);
        when(monitorAlertMapper.countAnyInSemanticWindow(any(), any(), anyInt())).thenReturn(0);
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setAiConflictLevel("LEVEL_3");
        decision.setAiConflictScore(50);
        decision.setMultiTfConvergence("WEAK");
        decision.setIsWorthOpening(false);

        service.emitAfterAnalysisPersist(run(), analysis(), decision);

        ArgumentCaptor<MonitorAlertDO> captor = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(captor.capture());
        assertThat(captor.getValue().getAlertType())
                .isEqualTo(MonitorAlertWriteServiceImpl.ALERT_TYPE_CONFLUENCE_BREAKDOWN);
        verify(monitorAlertMapper, never()).countByAnalysisIdAndAlertType(
                eq("a-1"), eq(MonitorAlertWriteServiceImpl.ALERT_TYPE_OPEN_BLOCKED_BY_CONFLICT));
    }

    private static AnalysisRunDO run() {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setTraceId("tr-1");
        run.setRuleVersion("v1");
        return run;
    }

    private static AssetAnalysisVO analysis() {
        AssetAnalysisVO a = new AssetAnalysisVO();
        a.setAnalysisId("a-1");
        a.setSymbol("BTCUSDT");
        return a;
    }
}
