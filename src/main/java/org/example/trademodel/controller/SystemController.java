package org.example.trademodel.controller;

import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.RunBaselineService;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.RunBaselineVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final int DEFAULT_RUN_BASELINE_WINDOW_MINUTES = 60;
    private static final String RUNTIME_READINESS_READY = "RUNTIME_READINESS_REVIEW_ONLY_READY";
    private static final String RUNTIME_READINESS_BACKEND_PENDING = "RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED";
    private static final String RUNTIME_READINESS_MISSING = "RUNTIME_READINESS_MISSING_FAIL_CLOSED";
    private static final String RUNTIME_READINESS_PARTIAL = "RUNTIME_READINESS_PARTIAL_REVIEW_ONLY";
    private static final String SYSTEM_GUARDRAIL_READY = "SYSTEM_GUARDRAIL_REVIEW_ONLY_READY";
    private static final String SYSTEM_GUARDRAIL_DEGRADED = "SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY";
    private static final String SYSTEM_GUARDRAIL_BLOCKED = "SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED";
    private static final String RUN_BASELINE_READY = "RUN_BASELINE_REVIEW_ONLY_READY";
    private static final String RUN_BASELINE_MISSING = "RUN_BASELINE_MISSING_FAIL_CLOSED";
    private static final String RUNTIME_METRIC_READY = "RUNTIME_METRIC_REVIEW_ONLY_READY";
    private static final String RUNTIME_METRIC_MISSING = "RUNTIME_METRIC_MISSING_FAIL_CLOSED";
    private static final String EXECUTABLE_READINESS_BLOCKED = "EXECUTABLE_READINESS_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String TRADING_AUTHORIZATION_BLOCKED = "TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String RECOVERY_REPAIR_BLOCKED = "RECOVERY_REPAIR_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String SCHEDULER_TRIGGER_BLOCKED = "SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String COLLECTOR_TRIGGER_BLOCKED = "COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String API_CLIENT_REFRESH_BLOCKED = "API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String EXTERNAL_REFRESH_BLOCKED = "EXTERNAL_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String CANDIDATE_BOUNDARY_BLOCKED = "CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String POINT_BOUNDARY_BLOCKED = "POINT_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String TRADING_BOUNDARY_BLOCKED = "TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED";

    private final SystemHealthService systemHealthService;
    private final PositionSyncService positionSyncService;
    private final RunBaselineService runBaselineService;

    @Autowired
    public SystemController(SystemHealthService systemHealthService,
                            PositionSyncService positionSyncService,
                            RunBaselineService runBaselineService) {
        this.systemHealthService = systemHealthService;
        this.positionSyncService = positionSyncService;
        this.runBaselineService = runBaselineService;
    }

    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("trade-model-v1 is running on port 8081");
    }

    @GetMapping("/position-sync-status")
    public ApiResponse<PositionSyncStatusVO> positionSyncStatus() {
        return ApiResponse.success(positionSyncService.getPositionSyncStatus());
    }

    @GetMapping("/run-baseline")
    public ApiResponse<RunBaselineVO> runBaseline(@RequestParam(required = false) Integer windowMinutes) {
        int effectiveWindowMinutes = windowMinutes != null ? windowMinutes : 60;
        return ApiResponse.success(runBaselineService.getRunBaseline(effectiveWindowMinutes));
    }

    @GetMapping("/runtime-readiness-guardrail-status")
    public Map<String, Object> runtimeReadinessGuardrailStatus(
            @RequestParam(required = false) Integer windowMinutes) {
        int effectiveWindowMinutes = resolveWindowMinutes(windowMinutes);
        Map<String, Object> status = baseRuntimeReadinessGuardrailStatus(effectiveWindowMinutes);
        try {
            RunBaselineVO baseline = runBaselineService.getRunBaseline(effectiveWindowMinutes);
            applyRuntimeReadinessGuardrailStatus(status, baseline);
        } catch (Exception e) {
            applyRuntimeReadinessStatus(
                    status,
                    RUNTIME_READINESS_BACKEND_PENDING,
                    SYSTEM_GUARDRAIL_BLOCKED,
                    RUN_BASELINE_MISSING,
                    RUNTIME_METRIC_MISSING,
                    "RUNTIME_READINESS_BACKEND_PENDING",
                    "Runtime readiness / system guardrail 只读状态暂不可读；fail-closed，不触发恢复、刷新、调度或交易授权。",
                    true
            );
            status.put("errorMessage", safeMessage(e.getMessage()));
        }
        return status;
    }

    private int resolveWindowMinutes(Integer windowMinutes) {
        return windowMinutes != null && windowMinutes > 0 ? windowMinutes : DEFAULT_RUN_BASELINE_WINDOW_MINUTES;
    }

    private Map<String, Object> baseRuntimeReadinessGuardrailStatus(int windowMinutes) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", RUNTIME_READINESS_BACKEND_PENDING);
        status.put("runtimeReadinessStatus", RUNTIME_READINESS_BACKEND_PENDING);
        status.put("systemGuardrailStatus", SYSTEM_GUARDRAIL_BLOCKED);
        status.put("runBaselineStatus", RUN_BASELINE_MISSING);
        status.put("runtimeMetricStatus", RUNTIME_METRIC_MISSING);
        status.put("windowMinutes", windowMinutes);
        status.put("ownerPath", "SystemController -> /api/system/run-baseline -> RunBaselineService -> SystemHealthService + RuntimeMetricService.snapshot() -> RunBaselineVO");
        status.put("runBaselineOwnerPath", "/api/system/run-baseline");
        status.put("healthEndpointUse", "/api/system/health is static liveness only, not executable readiness");
        status.put("systemHealthStaticLivenessOnly", true);
        status.put("sourceHealth", "MISSING");
        status.put("databaseStatus", "UNKNOWN");
        status.put("databaseStatusDetail", "missing");
        status.put("schedulerObservationStatus", "UNKNOWN");
        status.put("schedulerObservationDetail", "missing");
        status.put("runtimeMetricHasSamples", false);
        status.put("runtimeMetricSampleCount", 0L);
        status.put("runtimeMetricBoundaryDetail", "runtime metric snapshot is missing");
        status.put("reviewOnly", true);
        status.put("manualReviewOnly", true);
        status.put("notExecutableReadiness", true);
        status.put("notTradingAuthorization", true);
        status.put("notRecoveryRepair", true);
        status.put("notRestartAction", true);
        status.put("notAutoFix", true);
        status.put("notSchedulerTrigger", true);
        status.put("notCollectorTrigger", true);
        status.put("notApiClientRefresh", true);
        status.put("notExternalRefresh", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notFinalDirection", true);
        status.put("notEntryStopTpRr", true);
        status.put("notTradingSignal", true);
        status.put("notExecutable", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("executableReadinessBoundaryStatus", EXECUTABLE_READINESS_BLOCKED);
        status.put("tradingAuthorizationBoundaryStatus", TRADING_AUTHORIZATION_BLOCKED);
        status.put("recoveryRepairBoundaryStatus", RECOVERY_REPAIR_BLOCKED);
        status.put("schedulerTriggerBoundaryStatus", SCHEDULER_TRIGGER_BLOCKED);
        status.put("collectorTriggerBoundaryStatus", COLLECTOR_TRIGGER_BLOCKED);
        status.put("apiClientRefreshBoundaryStatus", API_CLIENT_REFRESH_BLOCKED);
        status.put("externalRefreshBoundaryStatus", EXTERNAL_REFRESH_BLOCKED);
        status.put("candidateBoundaryStatus", CANDIDATE_BOUNDARY_BLOCKED);
        status.put("pointBoundaryStatus", POINT_BOUNDARY_BLOCKED);
        status.put("tradingBoundaryStatus", TRADING_BOUNDARY_BLOCKED);
        status.put("statusMapping", runtimeReadinessStatusMapping());
        status.put("reason", "RUNTIME_READINESS_STATUS_PENDING");
        status.put("message", "Runtime readiness / system guardrail 只读状态待确认；不是可执行就绪或交易授权。");
        status.put("failClosed", true);
        return status;
    }

    private void applyRuntimeReadinessGuardrailStatus(Map<String, Object> status, RunBaselineVO baseline) {
        if (baseline == null) {
            applyRuntimeReadinessStatus(
                    status,
                    RUN_BASELINE_MISSING,
                    SYSTEM_GUARDRAIL_BLOCKED,
                    RUN_BASELINE_MISSING,
                    RUNTIME_METRIC_MISSING,
                    "RUN_BASELINE_MISSING",
                    "RunBaseline read model 缺失；runtime readiness / system guardrail 只读状态 fail-closed。",
                    true
            );
            status.put("sourceHealth", "MISSING");
            return;
        }

        status.put("generatedAt", baseline.getGeneratedAt());
        status.put("windowMinutes", baseline.getWindowMinutes());

        RunBaselineVO.SystemHealthSnapshot health = baseline.getSystemHealth();
        RunBaselineVO.PerformanceSummary performance = baseline.getPerformance();
        boolean healthMissing = health == null;
        boolean metricMissing = performance == null || performance.getMetrics() == null;
        boolean guardrailDegraded = isSystemGuardrailDegraded(health);

        if (health != null) {
            status.put("databaseStatus", safeValue(health.getDatabaseStatus(), "UNKNOWN"));
            status.put("databaseStatusDetail", safeValue(health.getDatabaseStatusDetail(), "missing"));
            status.put("schedulerObservationStatus", safeValue(health.getSchedulerStatus(), "UNKNOWN"));
            status.put("schedulerObservationDetail", safeValue(health.getSchedulerStatusDetail(), "missing"));
        }
        if (performance != null) {
            status.put("runtimeMetricHasSamples", Boolean.TRUE.equals(performance.getHasSamples()));
            status.put("runtimeMetricSampleCount", performance.getTotalSampleCount() != null ? performance.getTotalSampleCount() : 0L);
            status.put("runtimeMetricBoundaryDetail", safeValue(performance.getSampleBoundaryDetail(), "runtime metric snapshot is readable but sample boundary is unspecified"));
        }

        if (healthMissing) {
            applyRuntimeReadinessStatus(
                    status,
                    RUNTIME_READINESS_MISSING,
                    SYSTEM_GUARDRAIL_BLOCKED,
                    RUN_BASELINE_READY,
                    metricMissing ? RUNTIME_METRIC_MISSING : RUNTIME_METRIC_READY,
                    "SYSTEM_HEALTH_MISSING",
                    "SystemHealth snapshot 缺失；运行时防护栏只读状态 fail-closed。",
                    true
            );
            status.put("sourceHealth", "MISSING");
            return;
        }

        if (metricMissing) {
            applyRuntimeReadinessStatus(
                    status,
                    RUNTIME_METRIC_MISSING,
                    guardrailDegraded ? SYSTEM_GUARDRAIL_DEGRADED : SYSTEM_GUARDRAIL_READY,
                    RUN_BASELINE_READY,
                    RUNTIME_METRIC_MISSING,
                    "RUNTIME_METRIC_MISSING",
                    "RuntimeMetric snapshot 缺失；运行时指标只读状态 fail-closed。",
                    true
            );
            status.put("sourceHealth", guardrailDegraded ? "DEGRADED" : "PARTIAL");
            return;
        }

        if (guardrailDegraded) {
            applyRuntimeReadinessStatus(
                    status,
                    SYSTEM_GUARDRAIL_DEGRADED,
                    SYSTEM_GUARDRAIL_DEGRADED,
                    RUN_BASELINE_READY,
                    RUNTIME_METRIC_READY,
                    "SYSTEM_GUARDRAIL_DEGRADED",
                    "System guardrail 可读但降级；仅人工复核，所有下游动作 fail-closed。",
                    true
            );
            status.put("runtimeReadinessStatus", RUNTIME_READINESS_PARTIAL);
            status.put("sourceHealth", "DEGRADED");
            return;
        }

        applyRuntimeReadinessStatus(
                status,
                RUNTIME_READINESS_READY,
                SYSTEM_GUARDRAIL_READY,
                RUN_BASELINE_READY,
                RUNTIME_METRIC_READY,
                "RUNTIME_READINESS_OWNER_PATH_READ",
                "Runtime readiness / system guardrail 只读状态可读；READY 仅表示 operational status 可人工复核，不是可执行就绪或交易授权。",
                false
        );
        status.put("sourceHealth", "OK");
    }

    private void applyRuntimeReadinessStatus(Map<String, Object> status,
                                             String runtimeReadinessStatus,
                                             String systemGuardrailStatus,
                                             String runBaselineStatus,
                                             String runtimeMetricStatus,
                                             String reason,
                                             String message,
                                             boolean failClosed) {
        status.put("status", runtimeReadinessStatus);
        status.put("runtimeReadinessStatus", runtimeReadinessStatus);
        status.put("systemGuardrailStatus", systemGuardrailStatus);
        status.put("runBaselineStatus", runBaselineStatus);
        status.put("runtimeMetricStatus", runtimeMetricStatus);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
    }

    private boolean isSystemGuardrailDegraded(RunBaselineVO.SystemHealthSnapshot health) {
        if (health == null) {
            return true;
        }
        String databaseStatus = safeValue(health.getDatabaseStatus(), "UNKNOWN").toUpperCase();
        String schedulerStatus = safeValue(health.getSchedulerStatus(), "UNKNOWN").toUpperCase();
        return !"UP".equals(databaseStatus) || !"RUNNING".equals(schedulerStatus);
    }

    private List<String> runtimeReadinessStatusMapping() {
        return List.of(
                RUNTIME_READINESS_READY,
                RUNTIME_READINESS_BACKEND_PENDING,
                RUNTIME_READINESS_MISSING,
                RUNTIME_READINESS_PARTIAL,
                SYSTEM_GUARDRAIL_READY,
                SYSTEM_GUARDRAIL_DEGRADED,
                SYSTEM_GUARDRAIL_BLOCKED,
                RUN_BASELINE_READY,
                RUN_BASELINE_MISSING,
                RUNTIME_METRIC_READY,
                RUNTIME_METRIC_MISSING,
                EXECUTABLE_READINESS_BLOCKED,
                TRADING_AUTHORIZATION_BLOCKED,
                RECOVERY_REPAIR_BLOCKED,
                SCHEDULER_TRIGGER_BLOCKED,
                COLLECTOR_TRIGGER_BLOCKED,
                API_CLIENT_REFRESH_BLOCKED,
                EXTERNAL_REFRESH_BLOCKED,
                CANDIDATE_BOUNDARY_BLOCKED,
                POINT_BOUNDARY_BLOCKED,
                TRADING_BOUNDARY_BLOCKED
        );
    }

    private static String safeValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String safeMessage(String message) {
        return message == null || message.trim().isEmpty() ? "unknown error" : message.trim();
    }
}
