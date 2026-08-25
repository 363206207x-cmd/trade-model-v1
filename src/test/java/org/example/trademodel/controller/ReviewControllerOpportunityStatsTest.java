package org.example.trademodel.controller;

import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.RuleVersionLogQueryService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.mapper.AnalysisRunMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class ReviewControllerOpportunityStatsTest {
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
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;
    @Mock
    private AnalysisRunMapper analysisRunMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(
                reviewService,
                reviewAggregateService,
                ruleVersionLogQueryService,
                positionMonitorLogService,
                userPositionReviewAdapter,
                opportunityLogService,
                authenticatedUserIdResolver,
                analysisRunMapper)).build();
    }

    @Test
    void statsEndpointReturnsReviewOnlyOpportunityStats() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        OpportunityLogStatsDTO stats = new OpportunityLogStatsDTO();
        stats.setTotalCount(3);
        stats.setResolvedCount(2);
        stats.setPendingCount(1);
        stats.setExecutedValidCount(1);
        stats.setMissedInvalidCount(1);
        stats.setValidRate(new BigDecimal("0.50000000"));
        stats.setGeneratedAt(LocalDateTime.of(2026, 6, 23, 12, 0));
        when(opportunityLogService.getStatsForUser(eq(7L), eq("BTCUSDT"), any(), any())).thenReturn(stats);

        mockMvc.perform(get("/api/review/opportunities/stats")
                        .param("symbol", "BTCUSDT")
                        .param("from", "2026-06-23T00:00:00")
                        .param("to", "2026-06-24T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.executedValidCount").value(1))
                .andExpect(jsonPath("$.data.missedInvalidCount").value(1))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notExecutable").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true))
                .andExpect(jsonPath("$.data.notUserPositionCreation").value(true))
                .andExpect(jsonPath("$.data.notPushSend").value(true))
                .andExpect(jsonPath("$.data.notExternalChannel").value(true))
                .andExpect(jsonPath("$.data.orderAction").doesNotExist())
                .andExpect(jsonPath("$.data.executionAction").doesNotExist());

        verify(opportunityLogService).getStatsForUser(eq(7L), eq("BTCUSDT"), any(), any());
    }
}
