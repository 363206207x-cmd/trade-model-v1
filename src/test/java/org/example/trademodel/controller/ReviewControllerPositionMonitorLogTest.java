package org.example.trademodel.controller;

import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.RuleVersionLogQueryService;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.userpositionreview.UserPositionReviewAdapter;
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
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class ReviewControllerPositionMonitorLogTest {
    @Mock
    private ReviewService reviewService;
    @Mock
    private ReviewAggregateService reviewAggregateService;
    @Mock
    private RuleVersionLogQueryService ruleVersionLogQueryService;
    @Mock
    private PositionMonitorLogService positionMonitorLogService;
    @Mock
    private UserPositionReviewAdapter userPositionReviewAdapter;
    @Mock
    private OpportunityLogService opportunityLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(
                reviewService,
                reviewAggregateService,
                ruleVersionLogQueryService,
                positionMonitorLogService,
                userPositionReviewAdapter,
                opportunityLogService)).build();
    }

    @Test
    void reviewEndpointQueriesMonitorLogsReadOnlyWithSafetyFields() throws Exception {
        when(positionMonitorLogService.listByPositionId(7L, 20)).thenReturn(List.of(dto(11L)));

        mockMvc.perform(get("/api/review/positions/7/monitor-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].positionId").value(7))
                .andExpect(jsonPath("$.data[0].analysisId").value("ana-p0-4"))
                .andExpect(jsonPath("$.data[0].logicStatus").value("HIGH_RISK"))
                .andExpect(jsonPath("$.data[0].suggestedAction").value("RISK_REVIEW"))
                .andExpect(jsonPath("$.data[0].reviewOnly").value(true))
                .andExpect(jsonPath("$.data[0].manualReviewOnly").value(true))
                .andExpect(jsonPath("$.data[0].notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data[0].notExecutable").value(true))
                .andExpect(jsonPath("$.data[0].notAutoClose").value(true))
                .andExpect(jsonPath("$.data[0].notAutoReverse").value(true))
                .andExpect(jsonPath("$.data[0].notOrderExecution").value(true))
                .andExpect(jsonPath("$.data[0].notAutoTrading").value(true))
                .andExpect(jsonPath("$.data[0].notPositionMutation").value(true))
                .andExpect(jsonPath("$.data[0].closeAction").doesNotExist())
                .andExpect(jsonPath("$.data[0].reduceAction").doesNotExist())
                .andExpect(jsonPath("$.data[0].reverseAction").doesNotExist())
                .andExpect(jsonPath("$.data[0].orderAction").doesNotExist())
                .andExpect(jsonPath("$.data[0].executionAction").doesNotExist())
                .andExpect(jsonPath("$.data[0].autoTradingAction").doesNotExist())
                .andExpect(jsonPath("$.data[0].executablePayload").doesNotExist())
                .andExpect(jsonPath("$.data[0].providerPayload").doesNotExist());

        verify(positionMonitorLogService).listByPositionId(7L, 20);
        verify(reviewService, never()).saveOrUpdate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reviewEndpointRejectsUnsafeLimitAndDoesNotWriteLog() throws Exception {
        when(positionMonitorLogService.listByPositionId(7L, 101))
                .thenThrow(new IllegalArgumentException("limit must be <= 100"));

        mockMvc.perform(get("/api/review/positions/7/monitor-logs").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("limit must be <= 100"));

        verify(positionMonitorLogService).listByPositionId(7L, 101);
        verify(positionMonitorLogService, never())
                .recordMonitorRun(org.mockito.ArgumentMatchers.any());
    }

    private static PositionMonitorLogDTO dto(Long logId) {
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogId(logId);
        dto.setPositionId(7L);
        dto.setAnalysisId("ana-p0-4");
        dto.setExecutionPlanId("plan-p0-4");
        dto.setCurrentPrice(new BigDecimal("111.25"));
        dto.setLogicStatus("HIGH_RISK");
        dto.setRiskLevel("HIGH");
        dto.setSuggestedAction("RISK_REVIEW");
        dto.setReason("manual review note");
        dto.setEvidenceSnapshot("{\"evidence\":\"stable\"}");
        dto.setScoreSnapshot("{\"score\":70}");
        dto.setDecisionSnapshot("{\"decision\":\"watch\"}");
        dto.setRiskSnapshot("{\"risk\":\"guarded\"}");
        dto.setTraceId("trace-p0-4");
        dto.setCreatedAt(LocalDateTime.of(2026, 6, 22, 9, 0));
        dto.setReviewOnly(true);
        dto.setManualReviewOnly(true);
        dto.setNotTradeInstruction(true);
        dto.setNotExecutable(true);
        dto.setNotAutoClose(true);
        dto.setNotAutoReverse(true);
        dto.setNotOrderExecution(true);
        dto.setNotAutoTrading(true);
        dto.setNotPositionMutation(true);
        return dto;
    }
}
