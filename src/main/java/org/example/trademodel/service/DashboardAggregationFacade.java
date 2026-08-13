package org.example.trademodel.service;

import org.example.trademodel.analysistrace.AnalysisTraceService;
import org.example.trademodel.analysistrace.AnalysisTraceSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardAggregationFacade implements DashboardReadService {
    private final JdbcTemplate jdbcTemplate;
    private final AnalysisTraceService analysisTraceService;
    private final AnalysisSchedulerService analysisSchedulerService;

    public DashboardAggregationFacade(JdbcTemplate jdbcTemplate,
                                      AnalysisTraceService analysisTraceService,
                                      AnalysisSchedulerService analysisSchedulerService) {
        this.jdbcTemplate = jdbcTemplate;
        this.analysisTraceService = analysisTraceService;
        this.analysisSchedulerService = analysisSchedulerService;
    }

    @Override
    public Map<String, Object> overview() {
        long totalRuns = count("SELECT COUNT(*) FROM tm_analysis_run");
        long successCount = countStatus("SUCCESS");
        long failureCount = countStatus("FAILED");
        long retryCount = count("SELECT COALESCE(SUM(CASE WHEN attempt_count > 1 THEN attempt_count - 1 ELSE 0 END), 0) FROM tm_analysis_run");
        long schedulerExecutionCount = countTrigger("SCHEDULED");

        Map<String, Object> overview = baseSafety("DASHBOARD_READ_ONLY_OVERVIEW");
        overview.put("totalAnalysisRuns", totalRuns);
        overview.put("successCount", successCount);
        overview.put("failureCount", failureCount);
        overview.put("runningCount", countStatus("STARTED"));
        overview.put("successRate", ratio(successCount, totalRuns));
        overview.put("failureRate", ratio(failureCount, totalRuns));
        overview.put("retryCount", retryCount);
        overview.put("avgLatencyMs", averageLatencyMs());
        overview.put("idempotencyHitRate", ratio(retryCount, totalRuns + retryCount));
        overview.put("schedulerExecutionCount", schedulerExecutionCount);
        overview.put("aiCallLogCount", count("SELECT COUNT(*) FROM tm_ai_call_log"));
        overview.put("opportunityLogCount", count("SELECT COUNT(*) FROM tm_opportunity_log"));
        overview.put("executionPlanCount", count("SELECT COUNT(*) FROM tm_execution_plan"));
        overview.put("positionMonitorLogCount", count("SELECT COUNT(*) FROM tm_position_monitor_log"));
        overview.put("hotResetEventCount", count("SELECT COUNT(*) FROM tm_hot_reset_event"));
        overview.put("latestAnalysisRun", latestAnalysisRun());
        return overview;
    }

    @Override
    public Map<String, Object> analysisStatus() {
        long totalRuns = count("SELECT COUNT(*) FROM tm_analysis_run");
        long retryCount = count("SELECT COALESCE(SUM(CASE WHEN attempt_count > 1 THEN attempt_count - 1 ELSE 0 END), 0) FROM tm_analysis_run");

        Map<String, Object> status = baseSafety("DASHBOARD_READ_ONLY_ANALYSIS_STATUS");
        status.put("totalAnalysisRuns", totalRuns);
        status.put("statusCounts", statusCounts());
        status.put("retryCount", retryCount);
        status.put("avgLatencyMs", averageLatencyMs());
        status.put("idempotency", idempotencyStats(totalRuns, retryCount));
        status.put("schedulerExecutionCount", countTrigger("SCHEDULED"));
        return status;
    }

    @Override
    public Map<String, Object> schedulerStatus() {
        Map<String, Object> status = baseSafety("DASHBOARD_READ_ONLY_SCHEDULER_STATUS");
        Map<String, Object> scheduler = analysisSchedulerService.status();
        status.put("schedulerStatus", scheduler != null ? new LinkedHashMap<>(scheduler) : Map.of());
        status.put("schedulerExecutionCount", countTrigger("SCHEDULED"));
        status.put("schedulerSuccessCount", countTriggerAndStatus("SCHEDULED", "SUCCESS"));
        status.put("schedulerFailureCount", countTriggerAndStatus("SCHEDULED", "FAILED"));
        status.put("statusAccessOnly", true);
        return status;
    }

    @Override
    public Map<String, Object> traceSummary(Long userId, String analysisId, String traceId, String requestId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        AnalysisTraceSnapshot snapshot = lookupTrace(userId, analysisId, traceId, requestId);
        if (snapshot == null) {
            Map<String, Object> missing = baseSafety("DASHBOARD_READ_ONLY_TRACE_NOT_FOUND");
            missing.put("traceStatus", "NOT_FOUND");
            missing.put("missingSegments", List.of("analysisRun"));
            missing.put("chain", Map.of());
            return missing;
        }

        List<String> missingSegments = dashboardMissingSegments(snapshot);
        Map<String, Object> summary = baseSafety("DASHBOARD_READ_ONLY_TRACE_SUMMARY");
        summary.put("analysisId", snapshot.getAnalysisId());
        summary.put("traceId", snapshot.getTraceId());
        summary.put("requestId", snapshot.getRequestId());
        summary.put("idempotencyKey", snapshot.getIdempotencyKey());
        summary.put("traceStatus", dashboardTraceStatus(snapshot, missingSegments));
        summary.put("missingSegments", missingSegments);
        summary.put("chain", traceChain(snapshot));
        return summary;
    }

    private AnalysisTraceSnapshot lookupTrace(Long userId, String analysisId, String traceId, String requestId) {
        if (hasText(analysisId)) {
            return analysisTraceService.byAnalysisIdForUser(analysisId.trim(), userId);
        }
        if (hasText(traceId)) {
            return analysisTraceService.byTraceIdForUser(traceId.trim(), userId);
        }
        if (hasText(requestId)) {
            return analysisTraceService.byRequestIdForUser(requestId.trim(), userId);
        }
        return null;
    }

    private Map<String, Object> traceChain(AnalysisTraceSnapshot snapshot) {
        Map<String, Object> chain = new LinkedHashMap<>();
        chain.put("inputSnapshot", inputSnapshotSegment(snapshot));
        chain.put("evidence", segment(snapshot.getEvidenceIds()));
        chain.put("score", segment(snapshot.getScoreIds()));
        chain.put("decision", segment(snapshot.getDecisionIds()));
        chain.put("executionPlan", segment(snapshot.getExecutionPlanIds()));
        chain.put("positionMonitorLog", segment(snapshot.getPositionMonitorLogIds()));
        chain.put("reviewResult", segment(snapshot.getReviewResultIds()));
        chain.put("aiCallLog", segment(snapshot.getAiCallIds()));
        chain.put("opportunityLog", segment(snapshot.getOpportunityIds()));
        return chain;
    }

    private static Map<String, Object> segment(List<String> ids) {
        List<String> safeIds = ids != null ? ids : List.of();
        return Map.of("ids", safeIds, "count", safeIds.size(), "present", !safeIds.isEmpty());
    }

    private static Map<String, Object> inputSnapshotSegment(AnalysisTraceSnapshot snapshot) {
        Map<String, Object> segment = new LinkedHashMap<>();
        segment.put("inputSnapshotHash", snapshot.getInputSnapshotHash());
        segment.put("inputSnapshotJson", snapshot.getInputSnapshotJson());
        segment.put("present", hasText(snapshot.getInputSnapshotHash()) || hasText(snapshot.getInputSnapshotJson()));
        return segment;
    }

    private static List<String> dashboardMissingSegments(AnalysisTraceSnapshot snapshot) {
        List<String> missing = new ArrayList<>();
        if (!hasText(snapshot.getInputSnapshotHash()) && !hasText(snapshot.getInputSnapshotJson())) {
            missing.add("inputSnapshot");
        }
        missingIfEmpty(missing, "evidence", snapshot.getEvidenceIds());
        missingIfEmpty(missing, "score", snapshot.getScoreIds());
        missingIfEmpty(missing, "decision", snapshot.getDecisionIds());
        missingIfEmpty(missing, "executionPlan", snapshot.getExecutionPlanIds());
        missingIfEmpty(missing, "positionMonitorLog", snapshot.getPositionMonitorLogIds());
        missingIfEmpty(missing, "reviewResult", snapshot.getReviewResultIds());
        missingIfEmpty(missing, "aiCallLog", snapshot.getAiCallIds());
        missingIfEmpty(missing, "opportunityLog", snapshot.getOpportunityIds());
        return List.copyOf(missing);
    }

    private static void missingIfEmpty(List<String> missing, String segment, List<String> values) {
        if (values == null || values.isEmpty()) {
            missing.add(segment);
        }
    }

    private static String dashboardTraceStatus(AnalysisTraceSnapshot snapshot, List<String> missingSegments) {
        if (!missingSegments.isEmpty()) {
            return "PARTIAL_TRACE";
        }
        if ("STARTED".equals(snapshot.getStatus())) {
            return "RUNNING";
        }
        if ("FAILED".equals(snapshot.getStatus())) {
            return "FAILED";
        }
        return "COMPLETE";
    }

    private Map<String, Object> latestAnalysisRun() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT analysis_id, trace_id, request_id, status, symbol, timeframe, created_at "
                        + "FROM tm_analysis_run ORDER BY created_at DESC, analysis_id DESC LIMIT 1");
        return rows.isEmpty() ? null : new LinkedHashMap<>(rows.get(0));
    }

    private Map<String, Object> statusCounts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList(
                "SELECT COALESCE(status, 'UNKNOWN') AS status, COUNT(*) AS count FROM tm_analysis_run GROUP BY status ORDER BY status")) {
            counts.put(String.valueOf(row.get("STATUS")), number(row.get("COUNT")));
        }
        return counts;
    }

    private Map<String, Object> idempotencyStats(long totalRuns, long retryCount) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("uniqueIdempotencyKeys", count("SELECT COUNT(DISTINCT idempotency_key) FROM tm_analysis_run WHERE idempotency_key IS NOT NULL"));
        stats.put("retryCount", retryCount);
        stats.put("idempotencyHitRate", ratio(retryCount, totalRuns + retryCount));
        stats.put("readOnly", true);
        return stats;
    }

    private Long averageLatencyMs() {
        Number value = jdbcTemplate.queryForObject(
                "SELECT AVG(CASE WHEN started_at IS NOT NULL AND completed_at IS NOT NULL "
                        + "THEN DATEDIFF('MILLISECOND', started_at, completed_at) ELSE NULL END) FROM tm_analysis_run",
                Number.class);
        return value == null ? null : value.longValue();
    }

    private long countStatus(String status) {
        return count("SELECT COUNT(*) FROM tm_analysis_run WHERE status = ?", status);
    }

    private long countTrigger(String triggerType) {
        return count("SELECT COUNT(*) FROM tm_analysis_run WHERE trigger_type = ?", triggerType);
    }

    private long countTriggerAndStatus(String triggerType, String status) {
        return count("SELECT COUNT(*) FROM tm_analysis_run WHERE trigger_type = ? AND status = ?", triggerType, status);
    }

    private long count(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    private static BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private static Map<String, Object> baseSafety(String status) {
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("dashboardStatus", status);
        base.put("readOnly", true);
        base.put("manualReviewOnly", true);
        base.put("notTradeInstruction", true);
        base.put("notExecutable", true);
        base.put("notAutoTrading", true);
        base.put("notOrderExecution", true);
        base.put("notUserPositionCreation", true);
        base.put("notUserPositionMutation", true);
        base.put("notPushSend", true);
        base.put("notExternalChannel", true);
        return base;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
