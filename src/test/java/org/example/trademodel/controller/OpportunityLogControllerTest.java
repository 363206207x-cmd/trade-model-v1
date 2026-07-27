package org.example.trademodel.controller;

import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.OpportunityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class OpportunityLogControllerTest {
    private static final Long USER_ID = 17L;

    @Mock
    private OpportunityLogService opportunityLogService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(USER_ID);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new OpportunityLogController(opportunityLogService, authenticatedUserIdResolver)).build();
    }

    @Test
    void findEndpointReturnsReviewOnlyOpportunityLog() throws Exception {
        when(opportunityLogService.findByIdForUser("opp-1", USER_ID)).thenReturn(dto());

        mockMvc.perform(get("/api/opportunity-log/opp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunityId").value("opp-1"))
                .andExpect(jsonPath("$.data.opportunityStatus").value(OpportunityLogStatus.MISSED_VALID))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notExecutable").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true))
                .andExpect(jsonPath("$.data.notUserPositionCreation").value(true))
                .andExpect(jsonPath("$.data.notUserPositionMutation").value(true))
                .andExpect(jsonPath("$.data.notPushSend").value(true))
                .andExpect(jsonPath("$.data.notExternalChannel").value(true))
                .andExpect(jsonPath("$.data.orderAction").doesNotExist())
                .andExpect(jsonPath("$.data.executionAction").doesNotExist())
                .andExpect(jsonPath("$.data.autoTradingAction").doesNotExist())
                .andExpect(jsonPath("$.data.pushSendAction").doesNotExist());
    }

    @Test
    void queryEndpointDelegatesReadOnlyFilters() throws Exception {
        when(opportunityLogService.queryForUser(eq(USER_ID), eq("ana-1"), isNull(), isNull(), eq("BTCUSDT"),
                eq(OpportunityLogStatus.MISSED_VALID), isNull(), any(), any(), eq(25)))
                .thenReturn(List.of(dto()));

        mockMvc.perform(get("/api/opportunity-log/query")
                        .param("analysisId", "ana-1")
                        .param("symbol", "BTCUSDT")
                        .param("opportunityStatus", OpportunityLogStatus.MISSED_VALID)
                        .param("from", "2026-06-23T00:00:00")
                        .param("to", "2026-06-24T00:00:00")
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void evaluateEndpointOnlyAcceptsAsOfAndDoesNotCreateOpportunity() throws Exception {
        when(opportunityLogService.evaluateOpportunityForUser(eq("opp-1"), eq(USER_ID), any())).thenReturn(dto());

        mockMvc.perform(post("/api/opportunity-log/opp-1/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asOf\":\"2026-06-23T12:00:00\",\"orderAction\":\"forbidden\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunityId").value("opp-1"))
                .andExpect(jsonPath("$.data.notExecutable").value(true));

        verify(opportunityLogService).evaluateOpportunityForUser(eq("opp-1"), eq(USER_ID), any());
    }

    private static OpportunityLogDTO dto() {
        OpportunityLogDTO dto = new OpportunityLogDTO();
        dto.setOpportunityId("opp-1");
        dto.setOpportunityKey("ana-1:dec-1");
        dto.setAnalysisId("ana-1");
        dto.setDecisionId("dec-1");
        dto.setExecutionPlanId("plan-1");
        dto.setSymbol("BTCUSDT");
        dto.setTimeframe("1h");
        dto.setDirection("LONG");
        dto.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        dto.setOpportunityStatus(OpportunityLogStatus.MISSED_VALID);
        dto.setHitOrder(OpportunityLogStatus.TARGET_FIRST);
        dto.setMfeRatio(new BigDecimal("0.20"));
        dto.setMaeRatio(new BigDecimal("0.04"));
        dto.setAnchorTime(LocalDateTime.of(2026, 6, 23, 10, 0));
        return dto;
    }
}
