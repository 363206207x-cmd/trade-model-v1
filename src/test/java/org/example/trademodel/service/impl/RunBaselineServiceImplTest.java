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
    void recheckWindowUsesExplicitUtcBoundsAcrossJvmTimezones() {
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

        for (RecheckStatusEnum status : RecheckStatusEnum.values()) {
            verify(pushRecheckLogMapper, times(3)).countByStatusInWindow(
                    status.name(),
                    LocalDateTime.parse("2026-07-14T11:30:00"),
                    LocalDateTime.parse("2026-07-14T12:00:00"));
        }
    }

    @Test
    void runBaselineRecheckCountIsTimezoneIndependent() {
        when(pushRecheckLogMapper.countByStatusInWindow(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenAnswer(invocation -> RecheckStatusEnum.REVIEW_WAITING.name().equals(invocation.getArgument(0)) ? 2 : 0);
        TimeZone original = TimeZone.getDefault();

        try {
            RunBaselineVO.RecheckSummary expected = null;
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                RunBaselineVO.RecheckSummary actual = service.getRunBaseline(30).getRecheckSummary();
                assertThat(actual.getTotalCountWindow()).isEqualTo(2);
                assertThat(actual.getStatusCountsWindow()).containsEntry(RecheckStatusEnum.REVIEW_WAITING.name(), 2);
                if (expected == null) {
                    expected = actual;
                } else {
                    assertThat(actual.getStatusCountsWindow()).isEqualTo(expected.getStatusCountsWindow());
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }

        verify(pushRecheckLogMapper, times(RecheckStatusEnum.values().length * 3))
                .countByStatusInWindow(anyString(), eq(LocalDateTime.parse("2026-07-14T11:30:00")),
                        eq(LocalDateTime.parse("2026-07-14T12:00:00")));
    }
}
