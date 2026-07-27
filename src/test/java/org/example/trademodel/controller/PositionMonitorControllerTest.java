package org.example.trademodel.controller;

import org.example.trademodel.common.GlobalExceptionHandler;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PositionMonitorControllerTest {
    @Mock
    private PositionMonitorService positionMonitorService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PositionMonitorController(positionMonitorService, authenticatedUserIdResolver))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void singleEndpointReturnsReviewOnlyMonitorResult() throws Exception {
        when(positionMonitorService.monitorUserPositionForUser(7L, 7L)).thenReturn(result(7L));

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
    void batchEndpointIsForbiddenForAuthenticatedUsers() throws Exception {
        mockMvc.perform(post("/api/position-monitor/user-positions/open/run"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(positionMonitorService);
    }

    @Test
    void singleEndpointSanitizesUnexpectedMonitorFailure() throws Exception {
        when(positionMonitorService.monitorUserPositionForUser(8L, 7L))
                .thenThrow(new IllegalStateException("QUOTE_UNAVAILABLE"));

        mockMvc.perform(post("/api/position-monitor/user-positions/8/run"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("internal server error"));
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
