package org.example.trademodel.service;

import org.example.trademodel.analysistrace.AnalysisTraceService;
import org.example.trademodel.analysistrace.AnalysisTraceSnapshot;
import org.example.trademodel.entity.AnalysisRunDO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardAggregationFacadeTest {

    @Test
    void overviewAggregatesAnalysisSchedulerLatencyAndIdempotencyMetricsReadOnly() {
        JdbcTemplate jdbc = jdbcTemplate();
        insertRun(jdbc, "ana-success", "trace-success", "req-success", "SUCCESS", "MANUAL_API", 1,
                time("2026-06-24T10:00:00"), time("2026-06-24T10:00:01"));
        insertRun(jdbc, "ana-failed", "trace-failed", "req-failed", "FAILED", "SCHEDULED", 3,
                time("2026-06-24T10:01:00"), time("2026-06-24T10:01:02"));
        insertRun(jdbc, "ana-running", "trace-running", "req-running", "STARTED", "SCHEDULED", 1,
                time("2026-06-24T10:02:00"), null);

        DashboardAggregationFacade facade = facade(jdbc, new FakeTraceService(), scheduler(Map.of("enabled", true)));

        Map<String, Object> overview = facade.overview();
        Map<String, Object> analysisStatus = facade.analysisStatus();
        Map<String, Object> idempotency = castMap(analysisStatus.get("idempotency"));

        assertThat(overview).containsEntry("totalAnalysisRuns", 3L);
        assertThat(overview).containsEntry("successCount", 1L);
        assertThat(overview).containsEntry("failureCount", 1L);
        assertThat(overview).containsEntry("runningCount", 1L);
        assertThat(overview).containsEntry("retryCount", 2L);
        assertThat(overview).containsEntry("avgLatencyMs", 1500L);
        assertThat(overview).containsEntry("schedulerExecutionCount", 2L);
        assertThat(overview).containsEntry("successRate", new BigDecimal("0.3333"));
        assertThat(overview).containsEntry("failureRate", new BigDecimal("0.3333"));
        assertThat(idempotency).containsEntry("retryCount", 2L);
        assertThat(idempotency).containsEntry("idempotencyHitRate", new BigDecimal("0.4000"));
        assertThat(overview).containsEntry("readOnly", true);
        assertThat(overview).containsEntry("notAutoTrading", true);
        assertThat(overview).containsEntry("notOrderExecution", true);
    }

    @Test
    void schedulerStatusReadsStatusOnlyAndAddsExecutionCounts() {
        JdbcTemplate jdbc = jdbcTemplate();
        insertRun(jdbc, "ana-scheduled-ok", "trace-scheduled-ok", "req-scheduled-ok", "SUCCESS", "SCHEDULED", 1,
                time("2026-06-24T10:00:00"), time("2026-06-24T10:00:01"));
        insertRun(jdbc, "ana-scheduled-fail", "trace-scheduled-fail", "req-scheduled-fail", "FAILED", "SCHEDULED", 1,
                time("2026-06-24T10:01:00"), time("2026-06-24T10:01:01"));

        DashboardAggregationFacade facade = facade(jdbc, new FakeTraceService(),
                scheduler(Map.of("enabled", true, "configValid", true)));

        Map<String, Object> status = facade.schedulerStatus();
        Map<String, Object> schedulerStatus = castMap(status.get("schedulerStatus"));

        assertThat(schedulerStatus).containsEntry("enabled", true);
        assertThat(status).containsEntry("schedulerExecutionCount", 2L);
        assertThat(status).containsEntry("schedulerSuccessCount", 1L);
        assertThat(status).containsEntry("schedulerFailureCount", 1L);
        assertThat(status).containsEntry("statusAccessOnly", true);
        assertThat(status).containsEntry("notAutoTrading", true);
    }

    @Test
    void traceSummaryReturnsCompleteTraceWhenEveryDashboardSegmentExists() {
        FakeTraceService traceService = new FakeTraceService();
        traceService.byAnalysisId.put("ana-complete", snapshot("ana-complete", "trace-complete", "req-complete",
                "SUCCESS", "hash-complete", "{\"symbol\":\"BTCUSDT\"}",
                List.of("evidence-1"), List.of("score-1"), List.of("decision-1"), List.of("plan-1"),
                List.of("monitor-1"), List.of("review-1"), List.of("ai-1"), List.of("opp-1")));

        DashboardAggregationFacade facade = facade(jdbcTemplate(), traceService, scheduler(Map.of()));

        Map<String, Object> summary = facade.traceSummary(7L, "ana-complete", null, null);
        Map<String, Object> chain = castMap(summary.get("chain"));

        assertThat(summary).containsEntry("traceStatus", "COMPLETE");
        assertThat(summary.get("missingSegments")).isEqualTo(List.of());
        assertThat(castMap(chain.get("executionPlan"))).containsEntry("present", true);
        assertThat(castMap(chain.get("aiCallLog"))).containsEntry("count", 1);
        assertThat(summary).containsEntry("notExternalChannel", true);
    }

    @Test
    void traceSummaryReturnsPartialTraceAndDoesNotFabricateMissingSegments() {
        FakeTraceService traceService = new FakeTraceService();
        traceService.byTraceId.put("trace-partial", snapshot("ana-partial", "trace-partial", "req-partial",
                "SUCCESS", null, null,
                List.of("evidence-1"), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()));

        DashboardAggregationFacade facade = facade(jdbcTemplate(), traceService, scheduler(Map.of()));

        Map<String, Object> summary = facade.traceSummary(7L, null, "trace-partial", null);
        Map<String, Object> chain = castMap(summary.get("chain"));
        Map<String, Object> inputSnapshot = castMap(chain.get("inputSnapshot"));

        assertThat(summary).containsEntry("traceStatus", "PARTIAL_TRACE");
        assertThat(castList(summary.get("missingSegments")))
                .contains("inputSnapshot", "score", "decision", "executionPlan",
                        "positionMonitorLog", "reviewResult", "aiCallLog", "opportunityLog");
        assertThat(inputSnapshot.get("inputSnapshotHash")).isNull();
        assertThat(inputSnapshot.get("inputSnapshotJson")).isNull();
        assertThat(inputSnapshot).containsEntry("present", false);
    }

    private static DashboardAggregationFacade facade(JdbcTemplate jdbc,
                                                     AnalysisTraceService traceService,
                                                     AnalysisSchedulerService schedulerService) {
        return new DashboardAggregationFacade(jdbc, traceService, schedulerService);
    }

    private static AnalysisSchedulerService scheduler(Map<String, Object> status) {
        AnalysisSchedulerService scheduler = mock(AnalysisSchedulerService.class);
        when(scheduler.status()).thenReturn(status);
        return scheduler;
    }

    private static JdbcTemplate jdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:dashboard-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=true;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        populate(dataSource);
        return new JdbcTemplate(dataSource);
    }

    private static void populate(DataSource dataSource) {
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
    }

    private static void insertRun(JdbcTemplate jdbc, String analysisId, String traceId, String requestId,
                                  String status, String triggerType, int attemptCount,
                                  LocalDateTime startedAt, LocalDateTime completedAt) {
        jdbc.update("INSERT INTO tm_analysis_run(analysis_id, symbol, timeframe, analysis_time, rule_version, "
                        + "trace_id, status, idempotency_key, request_id, trigger_type, trigger_reference, "
                        + "input_snapshot_hash, attempt_count, started_at, completed_at, created_at, updated_at) "
                        + "VALUES(?, 'BTCUSDT', '5m', ?, 'rules-test', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                analysisId,
                Timestamp.valueOf(startedAt),
                traceId,
                status,
                "key-" + analysisId,
                requestId,
                triggerType,
                triggerType + ":" + analysisId,
                "hash-" + analysisId,
                attemptCount,
                Timestamp.valueOf(startedAt),
                completedAt != null ? Timestamp.valueOf(completedAt) : null,
                Timestamp.valueOf(startedAt),
                Timestamp.valueOf(startedAt));
    }

    private static AnalysisTraceSnapshot snapshot(String analysisId, String traceId, String requestId, String status,
                                                  String inputSnapshotHash, String inputSnapshotJson,
                                                  List<String> evidenceIds, List<String> scoreIds,
                                                  List<String> decisionIds, List<String> executionPlanIds,
                                                  List<String> positionMonitorLogIds, List<String> reviewResultIds,
                                                  List<String> aiCallIds, List<String> opportunityIds) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setTraceId(traceId);
        run.setRequestId(requestId);
        run.setStatus(status);
        run.setSymbol("BTCUSDT");
        run.setTimeframe("5m");
        run.setIdempotencyKey("key-" + analysisId);
        run.setInputSnapshotHash(inputSnapshotHash);
        run.setInputSnapshotJson(inputSnapshotJson);
        return new AnalysisTraceSnapshot(run, evidenceIds, scoreIds, decisionIds, executionPlanIds,
                positionMonitorLogIds, reviewResultIds, aiCallIds, opportunityIds, 0);
    }

    private static LocalDateTime time(String value) {
        return LocalDateTime.parse(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> castList(Object value) {
        return (List<String>) value;
    }

    private static final class FakeTraceService implements AnalysisTraceService {
        private final Map<String, AnalysisTraceSnapshot> byAnalysisId = new HashMap<>();
        private final Map<String, AnalysisTraceSnapshot> byTraceId = new HashMap<>();
        private final Map<String, AnalysisTraceSnapshot> byRequestId = new HashMap<>();

        @Override
        public AnalysisTraceSnapshot byAnalysisId(String analysisId) {
            return byAnalysisId.get(analysisId);
        }

        @Override
        public AnalysisTraceSnapshot byTraceId(String traceId) {
            return byTraceId.get(traceId);
        }

        @Override
        public AnalysisTraceSnapshot byRequestId(String requestId) {
            return byRequestId.get(requestId);
        }

        @Override
        public AnalysisTraceSnapshot byAnalysisIdForUser(String analysisId, Long userId) {
            return byAnalysisId.get(analysisId);
        }

        @Override
        public AnalysisTraceSnapshot byTraceIdForUser(String traceId, Long userId) {
            return byTraceId.get(traceId);
        }

        @Override
        public AnalysisTraceSnapshot byRequestIdForUser(String requestId, Long userId) {
            return byRequestId.get(requestId);
        }
    }
}
