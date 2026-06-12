package org.example.trademodel.controller;

import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.RunBaselineService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.vo.RunBaselineVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class SystemControllerTest {

    @Mock
    private SystemHealthService systemHealthService;
    @Mock
    private PositionSyncService positionSyncService;
    @Mock
    private RunBaselineService runBaselineService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SystemController(
                systemHealthService,
                positionSyncService,
                runBaselineService
        )).build();
    }

    @Test
    void runtimeReadinessGuardrailStatusReturnsReviewOnlyReadyProjection() throws Exception {
        when(runBaselineService.getRunBaseline(30)).thenReturn(runBaseline("UP", "RUNNING", true));

        mockMvc.perform(get("/api/system/runtime-readiness-guardrail-status").param("windowMinutes", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNTIME_READINESS_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.runtimeReadinessStatus").value("RUNTIME_READINESS_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.systemGuardrailStatus").value("SYSTEM_GUARDRAIL_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.runBaselineStatus").value("RUN_BASELINE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.runtimeMetricStatus").value("RUNTIME_METRIC_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.ownerPath").value(containsString("RunBaselineService")))
                .andExpect(jsonPath("$.runBaselineOwnerPath").value("/api/system/run-baseline"))
                .andExpect(jsonPath("$.healthEndpointUse").value(containsString("static liveness only")))
                .andExpect(jsonPath("$.systemHealthStaticLivenessOnly").value(true))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notExecutableReadiness").value(true))
                .andExpect(jsonPath("$.notTradingAuthorization").value(true))
                .andExpect(jsonPath("$.notRecoveryRepair").value(true))
                .andExpect(jsonPath("$.notRestartAction").value(true))
                .andExpect(jsonPath("$.notAutoFix").value(true))
                .andExpect(jsonPath("$.notSchedulerTrigger").value(true))
                .andExpect(jsonPath("$.notCollectorTrigger").value(true))
                .andExpect(jsonPath("$.notApiClientRefresh").value(true))
                .andExpect(jsonPath("$.notExternalRefresh").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notFinalDirection").value(true))
                .andExpect(jsonPath("$.notEntryStopTpRr").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false))
                .andExpect(jsonPath("$.statusMapping[?(@ == 'EXECUTABLE_READINESS_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'RECOVERY_REPAIR_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'EXTERNAL_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'POINT_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists());

        verify(runBaselineService).getRunBaseline(30);
        verifyNoInteractions(systemHealthService, positionSyncService);
    }

    @Test
    void runtimeReadinessGuardrailStatusFailsClosedWhenRunBaselineMissing() throws Exception {
        when(runBaselineService.getRunBaseline(60)).thenReturn(null);

        mockMvc.perform(get("/api/system/runtime-readiness-guardrail-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUN_BASELINE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.runtimeReadinessStatus").value("RUN_BASELINE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.systemGuardrailStatus").value("SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.runBaselineStatus").value("RUN_BASELINE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.runtimeMetricStatus").value("RUNTIME_METRIC_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notExecutableReadiness").value(true))
                .andExpect(jsonPath("$.notTradingAuthorization").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void runtimeReadinessGuardrailStatusFailsClosedWhenBackendThrows() throws Exception {
        when(runBaselineService.getRunBaseline(60)).thenThrow(new IllegalStateException("baseline unavailable"));

        mockMvc.perform(get("/api/system/runtime-readiness-guardrail-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.systemGuardrailStatus").value("SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reason").value("RUNTIME_READINESS_BACKEND_PENDING"))
                .andExpect(jsonPath("$.errorMessage").value("baseline unavailable"))
                .andExpect(jsonPath("$.notRecoveryRepair").value(true))
                .andExpect(jsonPath("$.notSchedulerTrigger").value(true))
                .andExpect(jsonPath("$.notApiClientRefresh").value(true))
                .andExpect(jsonPath("$.notExternalRefresh").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void runtimeReadinessGuardrailStatusShowsDegradedGuardrailAsReviewOnlyFailClosed() throws Exception {
        when(runBaselineService.getRunBaseline(60)).thenReturn(runBaseline("ERROR", "STALE", false));

        mockMvc.perform(get("/api/system/runtime-readiness-guardrail-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY"))
                .andExpect(jsonPath("$.runtimeReadinessStatus").value("RUNTIME_READINESS_PARTIAL_REVIEW_ONLY"))
                .andExpect(jsonPath("$.systemGuardrailStatus").value("SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY"))
                .andExpect(jsonPath("$.databaseStatus").value("ERROR"))
                .andExpect(jsonPath("$.schedulerObservationStatus").value("STALE"))
                .andExpect(jsonPath("$.runtimeMetricStatus").value("RUNTIME_METRIC_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.sourceHealth").value("DEGRADED"))
                .andExpect(jsonPath("$.message").value(containsString("仅人工复核")))
                .andExpect(jsonPath("$.notTradingAuthorization").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void runtimeReadinessGuardrailStatusFailsClosedWhenRuntimeMetricsMissing() throws Exception {
        RunBaselineVO baseline = runBaseline("UP", "RUNNING", true);
        baseline.setPerformance(null);
        when(runBaselineService.getRunBaseline(60)).thenReturn(baseline);

        mockMvc.perform(get("/api/system/runtime-readiness-guardrail-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNTIME_METRIC_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.runtimeMetricStatus").value("RUNTIME_METRIC_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.runBaselineStatus").value("RUN_BASELINE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.notExecutableReadiness").value(true))
                .andExpect(jsonPath("$.notTradingAuthorization").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void runtimeReadinessGuardrailStatusDoesNotExposeExecutableRefreshCandidatePointOrTradingFields() throws Exception {
        when(runBaselineService.getRunBaseline(60)).thenReturn(runBaseline("UP", "RUNNING", true));

        mockMvc.perform(get("/api/system/runtime-readiness-guardrail-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executableReadiness").doesNotExist())
                .andExpect(jsonPath("$.tradingAuthorization").doesNotExist())
                .andExpect(jsonPath("$.recoveryAction").doesNotExist())
                .andExpect(jsonPath("$.repairAction").doesNotExist())
                .andExpect(jsonPath("$.restartAction").doesNotExist())
                .andExpect(jsonPath("$.autoFixAction").doesNotExist())
                .andExpect(jsonPath("$.schedulerAction").doesNotExist())
                .andExpect(jsonPath("$.collectorAction").doesNotExist())
                .andExpect(jsonPath("$.apiClientRefreshAction").doesNotExist())
                .andExpect(jsonPath("$.externalRefreshAction").doesNotExist())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.externalChannelAction").doesNotExist());
    }

    @Test
    void healthEndpointRemainsStaticLivenessAndNotExecutableReadiness() throws Exception {
        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("trade-model-v1 is running on port 8081"))
                .andExpect(jsonPath("$.executableReadiness").doesNotExist())
                .andExpect(jsonPath("$.tradingAuthorization").doesNotExist());
    }

    private static RunBaselineVO runBaseline(String databaseStatus, String schedulerStatus, boolean hasSamples) {
        RunBaselineVO baseline = new RunBaselineVO();
        baseline.setGeneratedAt(LocalDateTime.of(2026, 6, 12, 12, 0));
        baseline.setWindowMinutes(60);
        RunBaselineVO.SystemHealthSnapshot health = new RunBaselineVO.SystemHealthSnapshot();
        health.setDatabaseStatus(databaseStatus);
        health.setDatabaseStatusDetail("database " + databaseStatus);
        health.setSchedulerStatus(schedulerStatus);
        health.setSchedulerStatusDetail("scheduler " + schedulerStatus);
        baseline.setSystemHealth(health);

        RunBaselineVO.PerformanceSummary performance = new RunBaselineVO.PerformanceSummary();
        Map<String, RunBaselineVO.RuntimeMetricSnapshot> metrics = new LinkedHashMap<>();
        RunBaselineVO.RuntimeMetricSnapshot metric = new RunBaselineVO.RuntimeMetricSnapshot();
        metric.setLastDurationMs(12L);
        metric.setAvgDurationMs(new BigDecimal("11.50"));
        metric.setSampleCount(hasSamples ? 3L : 0L);
        metrics.put("dashboard.summary", metric);
        performance.setMetrics(metrics);
        performance.setHasSamples(hasSamples);
        performance.setTotalSampleCount(hasSamples ? 3L : 0L);
        performance.setSampleBoundaryDetail(hasSamples
                ? "runtime metrics are sampled in-process snapshots, not executable readiness"
                : "no runtime samples recorded yet; metrics remain read-only status");
        performance.setBaselineAssembleDurationMs(5L);
        baseline.setPerformance(performance);
        return baseline;
    }
}
