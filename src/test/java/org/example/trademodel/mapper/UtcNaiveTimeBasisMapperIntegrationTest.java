package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.impl.MonitorAlertWriteServiceImpl;
import org.example.trademodel.service.impl.RunBaselineServiceImpl;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.RunBaselineVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class UtcNaiveTimeBasisMapperIntegrationTest {

    @Autowired
    private DecisionResultMapper decisionResultMapper;

    @Autowired
    private PushRecheckLogMapper pushRecheckLogMapper;

    @Autowired
    private MonitorAlertMapper monitorAlertMapper;

    @Autowired
    private AnalysisRunMapper analysisRunMapper;

    @Autowired
    private HotResetEventMapper hotResetEventMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void decisionAtUtcMidnightBelongsToCorrectUtcDay() {
        insertDecision("utc-day-before", "2026-07-13 23:59:59");
        insertDecision("utc-day-start", "2026-07-14 00:00:00");
        insertDecision("utc-day-end", "2026-07-14 23:59:59");
        insertDecision("utc-day-after", "2026-07-15 00:00:00");

        int count = decisionResultMapper.countDecisionsInRange(
                LocalDateTime.parse("2026-07-14T00:00:00"),
                LocalDateTime.parse("2026-07-15T00:00:00"));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void recheckWindowIncludesExactStartAndAsOf() {
        insertRecheck("UTC_BOUNDARY_INCLUSIVE", "2026-07-14 11:30:00");
        insertRecheck("UTC_BOUNDARY_INCLUSIVE", "2026-07-14 12:00:00");

        Integer count = pushRecheckLogMapper.countByStatusInWindow(
                "UTC_BOUNDARY_INCLUSIVE",
                LocalDateTime.parse("2026-07-14T11:30:00"),
                LocalDateTime.parse("2026-07-14T12:00:00"));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void recheckWindowExcludesBeforeStartAndAfterAsOf() {
        insertRecheck("UTC_BOUNDARY_EXCLUSIVE", "2026-07-14 11:29:59");
        insertRecheck("UTC_BOUNDARY_EXCLUSIVE", "2026-07-14 11:59:59");
        insertRecheck("UTC_BOUNDARY_EXCLUSIVE", "2026-07-14 12:00:01");

        Integer count = pushRecheckLogMapper.countByStatusInWindow(
                "UTC_BOUNDARY_EXCLUSIVE",
                LocalDateTime.parse("2026-07-14T11:30:00"),
                LocalDateTime.parse("2026-07-14T12:00:00"));

        assertThat(count).isEqualTo(1);
    }

    @Test
    void baselineWindowIncludesExactStartAndAsOf() {
        String marker = "UTC_BASELINE_BOUNDARY";
        insertBaselineRows(marker + "_BEFORE", marker, "2026-07-14 11:29:59");
        insertBaselineRows(marker + "_START", marker, "2026-07-14 11:30:00");
        insertBaselineRows(marker + "_INTERIOR", marker, "2026-07-14 11:59:59");
        insertBaselineRows(marker + "_ASOF", marker, "2026-07-14 12:00:00");
        insertBaselineRows(marker + "_FUTURE", marker, "2026-07-14 12:00:01");

        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");

        assertThat(monitorAlertMapper.countByStatusInWindow(marker, windowStartUtc, asOfUtc)).isEqualTo(3);
        assertThat(monitorAlertMapper.countByStatusAndTypeInWindow(
                marker, marker, windowStartUtc, asOfUtc)).isEqualTo(3);
        assertThat(analysisRunMapper.countInWindow(windowStartUtc, asOfUtc)).isEqualTo(3);
        assertThat(analysisRunMapper.countLowQualityInWindow(windowStartUtc, asOfUtc, 60)).isEqualTo(3);
        assertThat(pushRecheckLogMapper.countByStatusInWindow(marker, windowStartUtc, asOfUtc)).isEqualTo(3);
        assertThat(hotResetEventMapper.countInWindow(windowStartUtc, asOfUtc)).isEqualTo(3);
        assertThat(hotResetEventMapper.selectTriggerTypeCountsInWindow(windowStartUtc, asOfUtc))
                .filteredOn(row -> marker.equals(row.getKey()))
                .extracting(row -> row.getCount())
                .containsExactly(3);
    }

    @Test
    void baselineWindowExcludesFutureRows() {
        String marker = "UTC_BASELINE_FUTURE_ONLY";
        insertBaselineRows(marker, marker, "2026-07-14 12:00:01");

        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");

        assertThat(monitorAlertMapper.countByStatusInWindow(marker, windowStartUtc, asOfUtc)).isZero();
        assertThat(monitorAlertMapper.countByStatusAndTypeInWindow(
                marker, marker, windowStartUtc, asOfUtc)).isZero();
        assertThat(analysisRunMapper.countInWindow(windowStartUtc, asOfUtc)).isZero();
        assertThat(analysisRunMapper.countLowQualityInWindow(windowStartUtc, asOfUtc, 60)).isZero();
        assertThat(pushRecheckLogMapper.countByStatusInWindow(marker, windowStartUtc, asOfUtc)).isZero();
        assertThat(hotResetEventMapper.countInWindow(windowStartUtc, asOfUtc)).isZero();
        assertThat(hotResetEventMapper.selectTriggerTypeCountsInWindow(windowStartUtc, asOfUtc))
                .noneMatch(row -> marker.equals(row.getKey()));
    }

    @Test
    void monitorAlertWriteThenBaselineCountUsesSameWindow() {
        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");
        int before = monitorAlertMapper.countByStatusInWindow("OPEN", windowStartUtc, asOfUtc);

        writeHighRiskMonitorAlert("writer-baseline-current", Instant.parse("2026-07-14T12:00:00Z"));
        RunBaselineVO baseline = runBaselineAt(Instant.parse("2026-07-14T12:00:00Z"));

        assertThat(baseline.getAlertSummary().getOpenCountWindow()).isEqualTo(before + 1);
        MonitorAlertDO stored = monitorAlertMapper.listByAnalysisId("writer-baseline-current").get(0);
        assertThat(stored.getCreatedAt()).isEqualTo("2026-07-14 12:00:00");
        assertThat(stored.getUpdatedAt()).isEqualTo("2026-07-14 12:00:00");
        assertThat(stored.getCooldownUntil()).isEqualTo("2026-07-14 12:15:00");
    }

    @Test
    void monitorAlertBaselineExcludesFutureRowsAfterRealWriterInsert() {
        LocalDateTime windowStartUtc = LocalDateTime.parse("2026-07-14T11:30:00");
        LocalDateTime asOfUtc = LocalDateTime.parse("2026-07-14T12:00:00");
        int before = monitorAlertMapper.countByStatusInWindow("OPEN", windowStartUtc, asOfUtc);

        writeHighRiskMonitorAlert("writer-baseline-future", Instant.parse("2026-07-14T12:00:01Z"));
        RunBaselineVO baseline = runBaselineAt(Instant.parse("2026-07-14T12:00:00Z"));

        assertThat(monitorAlertMapper.listByAnalysisId("writer-baseline-future"))
                .singleElement()
                .extracting(MonitorAlertDO::getCreatedAt)
                .isEqualTo("2026-07-14 12:00:01");
        assertThat(baseline.getAlertSummary().getOpenCountWindow()).isEqualTo(before);
    }

    private void writeHighRiskMonitorAlert(String analysisId, Instant instant) {
        MonitorAlertWriteServiceImpl writer = new MonitorAlertWriteServiceImpl(monitorAlertMapper);
        writer.setClock(Clock.fixed(instant, ZoneOffset.UTC));
        AnalysisRunDO run = new AnalysisRunDO();
        run.setTraceId("trace-" + analysisId);
        run.setRuleVersion("v1");
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId(analysisId);
        analysis.setSymbol("BTCUSDT");
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setRiskLevel("HIGH");

        writer.emitAfterAnalysisPersist(run, analysis, decision);
    }

    private RunBaselineVO runBaselineAt(Instant instant) {
        SystemHealthService systemHealthService = mock(SystemHealthService.class);
        PositionSyncService positionSyncService = mock(PositionSyncService.class);
        DecisionService decisionService = mock(DecisionService.class);
        RuntimeMetricService runtimeMetricService = mock(RuntimeMetricService.class);
        when(systemHealthService.getSystemHealth()).thenReturn(Map.of());
        when(runtimeMetricService.snapshot()).thenReturn(Map.of());
        when(decisionService.getLightSystemStatus()).thenReturn(new LightSystemStatusVO());
        RunBaselineServiceImpl baseline = new RunBaselineServiceImpl(
                systemHealthService,
                positionSyncService,
                decisionService,
                runtimeMetricService,
                monitorAlertMapper,
                analysisRunMapper,
                pushRecheckLogMapper,
                hotResetEventMapper);
        baseline.setClock(Clock.fixed(instant, ZoneOffset.UTC));
        return baseline.getRunBaseline(30);
    }

    private void insertDecision(String id, String createTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_decision_result(decision_id, analysis_id, symbol, create_time) "
                        + "VALUES (?,?,?, CAST(? AS TIMESTAMP))",
                id, "analysis-" + id, "BTCUSDT", createTime);
    }

    private void insertRecheck(String status, String createTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_push_recheck_log(push_id, recheck_status, create_time) "
                        + "VALUES (?,?, CAST(? AS TIMESTAMP))",
                991L, status, createTime);
    }

    private void insertBaselineRows(String id, String marker, String eventTime) {
        jdbcTemplate.update(
                "INSERT INTO tm_monitor_alert(id, alert_type, status, created_at) "
                        + "VALUES (?,?,?, CAST(? AS TIMESTAMP))",
                "alert-" + id, marker, marker, eventTime);
        jdbcTemplate.update(
                "INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, data_quality_score) "
                        + "VALUES (?,?,?,?,?)",
                "analysis-" + id, "BTCUSDT", "1h", LocalDateTime.parse(eventTime.replace(' ', 'T')), 50);
        insertRecheck(marker, eventTime);
        jdbcTemplate.update(
                "INSERT INTO tm_hot_reset_event(event_id, event_key, analysis_id, symbol, trigger_type, event_time) "
                        + "VALUES (?,?,?,?,?, CAST(? AS TIMESTAMP))",
                "event-" + id, "event-key-" + id, "analysis-" + id, "BTCUSDT", marker, eventTime);
    }
}
