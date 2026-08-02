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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class MonitorAlertWriteServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-14T12:00:00Z");
    private static final LocalDateTime FIXED_UTC = LocalDateTime.parse("2026-07-14T12:00:00");

    @Mock
    private MonitorAlertMapper monitorAlertMapper;

    private MonitorAlertWriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MonitorAlertWriteServiceImpl(monitorAlertMapper);
        service.setClock(Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    }

    @Test
    void emitsOpenAlertWithCooldown_whenNoThrottleAndNoSemanticSuppress() {
        when(monitorAlertMapper.countByAnalysisIdAndAlertType(any(), any())).thenReturn(0);
        when(monitorAlertMapper.countOpenInThrottleWindow(any(), any(), any(), any())).thenReturn(0);
        when(monitorAlertMapper.countAnyInSemanticWindow(any(), any(), any(), any())).thenReturn(0);
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setRiskLevel("HIGH");

        service.emitAfterAnalysisPersist(run(), analysis(), decision);

        ArgumentCaptor<MonitorAlertDO> captor = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(captor.capture());
        MonitorAlertDO row = captor.getValue();
        assertThat(row.getStatus()).isEqualTo("OPEN");
        assertThat(row.getAlertType()).isEqualTo(MonitorAlertWriteServiceImpl.ALERT_TYPE_HIGH_RISK_DECISION);
        assertThat(row.getCooldownUntil()).isNotBlank();
        assertThat(row.getCreatedAtUtc()).isEqualTo(FIXED_UTC);
        assertThat(row.getUpdatedAtUtc()).isEqualTo(FIXED_UTC);
        assertThat(row.getCooldownUntilUtc()).isEqualTo(LocalDateTime.parse("2026-07-14T12:15:00"));
        assertThat(row.getSuppressReason()).isNull();
    }

    @Test
    void emitsSuppressed_whenOpenExistsInThrottleWindow() {
        when(monitorAlertMapper.countByAnalysisIdAndAlertType(any(), any())).thenReturn(0);
        when(monitorAlertMapper.countOpenInThrottleWindow(any(), any(), any(), any())).thenReturn(1);
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setRiskLevel("HIGH");

        service.emitAfterAnalysisPersist(run(), analysis(), decision);

        ArgumentCaptor<MonitorAlertDO> captor = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(captor.capture());
        MonitorAlertDO row = captor.getValue();
        assertThat(row.getStatus()).isEqualTo("SUPPRESSED");
        assertThat(row.getSuppressReason()).contains("THROTTLE_DB");
        assertThat(row.getCooldownUntil()).isNull();
        assertThat(row.getCooldownUntilUtc()).isNull();
        assertThat(row.getCreatedAtUtc()).isEqualTo(FIXED_UTC);
    }

    @Test
    void emitsConfluenceBreakdownAndSkipsOpenBlocked_familyDedup() {
        when(monitorAlertMapper.countByAnalysisIdAndAlertType(any(), any())).thenReturn(0);
        when(monitorAlertMapper.countOpenInThrottleWindow(any(), any(), any(), any())).thenReturn(0);
        when(monitorAlertMapper.countAnyInSemanticWindow(any(), any(), any(), any())).thenReturn(0);
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

    @Test
    void monitorAlertWriterPersistsUtcNaiveTimeAcrossJvmZones() {
        stubNoExistingAlert();
        DecisionBundleVO decision = highRiskDecision();
        TimeZone original = TimeZone.getDefault();

        try {
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                service.emitAfterAnalysisPersist(run(), analysis(), decision);
            }
        } finally {
            TimeZone.setDefault(original);
        }

        ArgumentCaptor<MonitorAlertDO> rows = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper, times(3)).insert(rows.capture());
        assertThat(rows.getAllValues()).allSatisfy(row -> {
            assertThat(row.getCreatedAtUtc()).isEqualTo(FIXED_UTC);
            assertThat(row.getUpdatedAtUtc()).isEqualTo(FIXED_UTC);
            assertThat(row.getCooldownUntilUtc()).isEqualTo(LocalDateTime.parse("2026-07-14T12:15:00"));
        });
    }

    @Test
    void monitorAlertThrottleUsesSameUtcClockAsWriter() {
        stubNoExistingAlert();

        service.emitAfterAnalysisPersist(run(), analysis(), highRiskDecision());

        verify(monitorAlertMapper).countOpenInThrottleWindow(
                "BTCUSDT", MonitorAlertWriteServiceImpl.ALERT_TYPE_HIGH_RISK_DECISION,
                LocalDateTime.parse("2026-07-14T11:45:00"), FIXED_UTC);
        ArgumentCaptor<MonitorAlertDO> row = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(row.capture());
        assertThat(row.getValue().getCreatedAtUtc()).isEqualTo(FIXED_UTC);
    }

    @Test
    void monitorAlertSemanticWindowUsesSameUtcClockAsWriter() {
        stubNoExistingAlert();

        service.emitAfterAnalysisPersist(run(), analysis(), highRiskDecision());

        verify(monitorAlertMapper).countAnyInSemanticWindow(
                "BTCUSDT", MonitorAlertWriteServiceImpl.ALERT_TYPE_HIGH_RISK_DECISION,
                LocalDateTime.parse("2026-07-14T11:15:00"), FIXED_UTC);
        ArgumentCaptor<MonitorAlertDO> row = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(row.capture());
        assertThat(row.getValue().getUpdatedAtUtc()).isEqualTo(FIXED_UTC);
    }

    @Test
    void dataQualityAlertUsesTheSharedSeventyPointCircuitBreaker() {
        stubNoExistingAlert();
        AssetAnalysisVO blocked = analysis();
        blocked.setDataQualityScore(69);

        service.emitAfterAnalysisPersist(run(), blocked, null);

        ArgumentCaptor<MonitorAlertDO> row = ArgumentCaptor.forClass(MonitorAlertDO.class);
        verify(monitorAlertMapper).insert(row.capture());
        assertThat(row.getValue().getAlertType())
                .isEqualTo(MonitorAlertWriteServiceImpl.ALERT_TYPE_DATA_QUALITY_INSUFFICIENT);
        assertThat(row.getValue().getAlertMessage()).contains("dataQualityScore=69", "低于阈值 70");

        AssetAnalysisVO passed = analysis();
        passed.setAnalysisId("a-70");
        passed.setDataQualityScore(70);
        service.emitAfterAnalysisPersist(run(), passed, null);

        verify(monitorAlertMapper, times(1)).insert(any(MonitorAlertDO.class));
    }

    private void stubNoExistingAlert() {
        when(monitorAlertMapper.countByAnalysisIdAndAlertType(any(), any())).thenReturn(0);
        when(monitorAlertMapper.countOpenInThrottleWindow(any(), any(), any(), any())).thenReturn(0);
        when(monitorAlertMapper.countAnyInSemanticWindow(any(), any(), any(), any())).thenReturn(0);
    }

    private static DecisionBundleVO highRiskDecision() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setRiskLevel("HIGH");
        return decision;
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
