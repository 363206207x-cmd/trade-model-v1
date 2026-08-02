package org.example.trademodel.service.impl;

import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.RunBaselineVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunBaselineServiceImplTest {

    @Mock
    private SystemHealthService systemHealthService;
    @Mock
    private PositionSyncService positionSyncService;
    @Mock
    private DecisionService decisionService;
    @Mock
    private RuntimeMetricService runtimeMetricService;
    @Mock
    private MonitorAlertMapper monitorAlertMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;
    @Mock
    private PushRecheckLogMapper pushRecheckLogMapper;
    @Mock
    private HotResetEventMapper hotResetEventMapper;

    private RunBaselineServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RunBaselineServiceImpl(
                systemHealthService,
                positionSyncService,
                decisionService,
                runtimeMetricService,
                monitorAlertMapper,
                analysisRunMapper,
                pushRecheckLogMapper,
                hotResetEventMapper);
        service.setClock(Clock.fixed(Instant.parse("2026-07-14T12:00:00Z"), ZoneOffset.UTC));
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(decisionService.getLightSystemStatus()).thenReturn(new LightSystemStatusVO());
    }

    @Test
    void recheckSummaryRemainsUnavailableAcrossJvmTimezones() {
        TimeZone original = TimeZone.getDefault();

        try {
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                RunBaselineVO result = service.getRunBaseline(30);
                assertThat(result.getGeneratedAt()).isEqualTo(LocalDateTime.parse("2026-07-14T12:00:00"));
                assertThat(result.getRecheckSummary().getAvailabilityStatus())
                        .isEqualTo("PRIVATE_SOURCE_UNAVAILABLE");
                assertThat(result.getRecheckSummary().getTotalCountWindow()).isNull();
                assertThat(result.getRecheckSummary().getStatusCountsWindow()).isNull();
            }
        } finally {
            TimeZone.setDefault(original);
        }

        verifyNoInteractions(pushRecheckLogMapper);
    }

    @Test
    void runBaselineDoesNotExposeGlobalRecheckCounts() {
        TimeZone original = TimeZone.getDefault();

        try {
            RunBaselineVO.RecheckSummary expected = null;
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                RunBaselineVO.RecheckSummary actual = service.getRunBaseline(30).getRecheckSummary();
                assertThat(actual.getAvailabilityStatus()).isEqualTo("PRIVATE_SOURCE_UNAVAILABLE");
                assertThat(actual.getAvailabilityDetail()).contains("authoritative source-owner relation");
                assertThat(actual.getTotalCountWindow()).isNull();
                assertThat(actual.getStatusCountsWindow()).isNull();
                if (expected == null) {
                    expected = actual;
                } else {
                    assertThat(actual.getAvailabilityStatus()).isEqualTo(expected.getAvailabilityStatus());
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }

        verifyNoInteractions(pushRecheckLogMapper);
    }

    @Test
    void runBaselinePassesOneUtcWindowToEveryMapper() {
        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");

        service.getRunBaseline(30);

        verify(monitorAlertMapper).countByStatusInWindow("OPEN", windowStartUtc, asOfUtc);
        verify(monitorAlertMapper).countByStatusInWindow("SUPPRESSED", windowStartUtc, asOfUtc);
        verify(monitorAlertMapper).countByStatusAndTypeInWindow(
                "OPEN", MonitorAlertWriteServiceImpl.ALERT_TYPE_DATA_QUALITY_INSUFFICIENT,
                windowStartUtc, asOfUtc);
        verify(monitorAlertMapper).countByStatusAndTypeInWindow(
                "SUPPRESSED", MonitorAlertWriteServiceImpl.ALERT_TYPE_DATA_QUALITY_INSUFFICIENT,
                windowStartUtc, asOfUtc);
        verify(analysisRunMapper).countInWindow(windowStartUtc, asOfUtc);
        verify(analysisRunMapper).countLowQualityInWindow(windowStartUtc, asOfUtc, 70);
        verifyNoInteractions(pushRecheckLogMapper);
        verify(hotResetEventMapper).countInWindow(windowStartUtc, asOfUtc);
        verify(hotResetEventMapper).selectTriggerTypeCountsInWindow(windowStartUtc, asOfUtc);
    }

    @Test
    void alertBaselineWindowIsTimezoneIndependent() {
        runAcrossJvmTimezones();

        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");
        verify(monitorAlertMapper, times(3)).countByStatusInWindow("OPEN", windowStartUtc, asOfUtc);
        verify(monitorAlertMapper, times(3)).countByStatusInWindow("SUPPRESSED", windowStartUtc, asOfUtc);
        verify(monitorAlertMapper, times(3)).countByStatusAndTypeInWindow(
                "OPEN", MonitorAlertWriteServiceImpl.ALERT_TYPE_DATA_QUALITY_INSUFFICIENT,
                windowStartUtc, asOfUtc);
        verify(monitorAlertMapper, times(3)).countByStatusAndTypeInWindow(
                "SUPPRESSED", MonitorAlertWriteServiceImpl.ALERT_TYPE_DATA_QUALITY_INSUFFICIENT,
                windowStartUtc, asOfUtc);
    }

    @Test
    void dataQualityBaselineWindowIsTimezoneIndependent() {
        runAcrossJvmTimezones();

        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");
        verify(analysisRunMapper, times(3)).countInWindow(windowStartUtc, asOfUtc);
        verify(analysisRunMapper, times(3)).countLowQualityInWindow(windowStartUtc, asOfUtc, 70);
    }

    @Test
    void hotResetBaselineWindowIsTimezoneIndependent() {
        runAcrossJvmTimezones();

        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");
        verify(hotResetEventMapper, times(3)).countInWindow(windowStartUtc, asOfUtc);
        verify(hotResetEventMapper, times(3)).selectTriggerTypeCountsInWindow(windowStartUtc, asOfUtc);
    }

    private void runAcrossJvmTimezones() {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                RunBaselineVO result = service.getRunBaseline(30);
                assertThat(result.getGeneratedAt()).isEqualTo(LocalDateTime.parse("2026-07-14T12:00:00"));
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
