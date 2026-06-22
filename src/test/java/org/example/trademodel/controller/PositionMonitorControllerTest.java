package org.example.trademodel.controller;

import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.service.PositionMonitorService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PositionMonitorControllerTest {
    @Mock
    private PositionMonitorService positionMonitorService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PositionMonitorController(positionMonitorService)).build();
    }

    @Test
    void singleEndpointReturnsReviewOnlyMonitorResult() throws Exception {
        when(positionMonitorService.monitorUserPosition(7L)).thenReturn(result(7L));

        mockMvc.perform(post("/api/position-monitor/user-positions/7/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionId").value(7))
                .andExpect(jsonPath("$.data.logicStatus").value("LOGIC_VALID"))
                .andExpect(jsonPath("$.data.monitorLogId").value(101))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notExecutable").value(true))
                .andExpect(jsonPath("$.data.notAutoReduce").value(true))
                .andExpect(jsonPath("$.data.notAutoClose").value(true))
                .andExpect(jsonPath("$.data.notAutoReverse").value(true))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andExpect(jsonPath("$.data.notPositionMutation").value(true))
                .andExpect(jsonPath("$.data.orderAction").doesNotExist())
                .andExpect(jsonPath("$.data.executionAction").doesNotExist())
                .andExpect(jsonPath("$.data.autoTradingAction").doesNotExist());
    }

    @Test
    void batchEndpointReturnsCountsAndItemResults() throws Exception {
        PositionMonitorBatchResultDTO batch = new PositionMonitorBatchResultDTO();
        batch.setTotalCount(1);
        batch.setSuccessCount(1);
        batch.setFailureCount(0);
        batch.setBlockedCount(0);
        batch.setResults(List.of(result(7L)));
        when(positionMonitorService.monitorOpenUserPositions()).thenReturn(batch);

        mockMvc.perform(post("/api/position-monitor/user-positions/open/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.results[0].positionId").value(7))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.notExecutable").value(true));
    }

    @Test
    void singleEndpointReturnsBadRequestWhenMonitorFailsClosed() throws Exception {
        when(positionMonitorService.monitorUserPosition(8L))
                .thenThrow(new IllegalStateException("QUOTE_UNAVAILABLE"));

        mockMvc.perform(post("/api/position-monitor/user-positions/8/run"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("QUOTE_UNAVAILABLE"));
    }

    private static PositionMonitorResultDTO result(Long positionId) {
        PositionMonitorResultDTO dto = new PositionMonitorResultDTO();
        dto.setPositionId(positionId);
        dto.setAssetSymbol("BTC");
        dto.setSide("LONG");
        dto.setPositionStatus("OPEN");
        dto.setAnalysisId("ana-p0-5");
        dto.setExecutionPlanId("plan-p0-5");
        dto.setCurrentPrice(new BigDecimal("100"));
        dto.setEntryPrice(new BigDecimal("95"));
        dto.setStopLoss(new BigDecimal("90"));
        dto.setTakeProfit(new BigDecimal("120"));
        dto.setLogicStatus("LOGIC_VALID");
        dto.setRiskLevel("LOW");
        dto.setSuggestedAction("HOLD");
        dto.setMonitorLogId(101L);
        dto.setMonitoredAt(LocalDateTime.of(2026, 6, 22, 12, 0));
        return dto;
    }
}
