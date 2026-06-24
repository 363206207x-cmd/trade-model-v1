package org.example.trademodel.controller;

import org.example.trademodel.service.DashboardReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardOverviewControllerTest {
    @Mock
    private DashboardReadService dashboardReadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardOverviewController(dashboardReadService)).build();
    }

    @Test
    void overviewReturnsReadOnlyAggregation() throws Exception {
        when(dashboardReadService.overview()).thenReturn(Map.of(
                "totalAnalysisRuns", 3L,
                "readOnly", true,
                "notAutoTrading", true));

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAnalysisRuns").value(3))
                .andExpect(jsonPath("$.data.readOnly").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true));

        verify(dashboardReadService).overview();
    }

    @Test
    void analysisStatusReturnsReadOnlyMetrics() throws Exception {
        when(dashboardReadService.analysisStatus()).thenReturn(Map.of(
                "statusCounts", Map.of("SUCCESS", 1L),
                "retryCount", 2L,
                "notOrderExecution", true));

        mockMvc.perform(get("/api/dashboard/analysis-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusCounts.SUCCESS").value(1))
                .andExpect(jsonPath("$.data.retryCount").value(2))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true));

        verify(dashboardReadService).analysisStatus();
    }

    @Test
    void schedulerStatusUsesReadServiceOnly() throws Exception {
        when(dashboardReadService.schedulerStatus()).thenReturn(Map.of(
                "schedulerStatus", Map.of("enabled", true),
                "schedulerExecutionCount", 4L,
                "statusAccessOnly", true));

        mockMvc.perform(get("/api/dashboard/scheduler-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedulerStatus.enabled").value(true))
                .andExpect(jsonPath("$.data.schedulerExecutionCount").value(4))
                .andExpect(jsonPath("$.data.statusAccessOnly").value(true));

        verify(dashboardReadService).schedulerStatus();
    }

    @Test
    void traceSummaryRequiresIdentifier() throws Exception {
        mockMvc.perform(get("/api/dashboard/trace-summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void traceSummaryRendersPartialTrace() throws Exception {
        when(dashboardReadService.traceSummary("ana-1", null, null)).thenReturn(Map.of(
                "analysisId", "ana-1",
                "traceId", "trace-1",
                "traceStatus", "PARTIAL_TRACE",
                "missingSegments", List.of("executionPlan"),
                "notTradeInstruction", true));

        mockMvc.perform(get("/api/dashboard/trace-summary").param("analysisId", "ana-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.traceStatus").value("PARTIAL_TRACE"))
                .andExpect(jsonPath("$.data.missingSegments[0]").value("executionPlan"))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true));

        verify(dashboardReadService).traceSummary("ana-1", null, null);
    }

    @Test
    void traceSummaryReturnsNotFoundWhenTraceDoesNotExist() throws Exception {
        when(dashboardReadService.traceSummary(null, "missing-trace", null)).thenReturn(Map.of(
                "traceStatus", "NOT_FOUND",
                "missingSegments", List.of("analysisRun")));

        mockMvc.perform(get("/api/dashboard/trace-summary").param("traceId", "missing-trace"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        verify(dashboardReadService).traceSummary(null, "missing-trace", null);
    }
}
