package org.example.trademodel.controller;

import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.RuleVersionLogQueryService;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.userpositionreview.UserPositionReviewAdapter;
import org.example.trademodel.userpositionreview.UserPositionReviewFeedbackReq;
import org.example.trademodel.userpositionreview.UserPositionReviewFeedbackResultDTO;
import org.example.trademodel.userpositionreview.UserPositionReviewSummaryDTO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class ReviewControllerUserPositionReviewTest {
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
    void summaryEndpointReturnsReviewOnlyUserPositionSummaryAndDoesNotWriteFeedback() throws Exception {
        when(userPositionReviewAdapter.buildSummary(7L)).thenReturn(summary());

        mockMvc.perform(get("/api/review/user-positions/7/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionId").value(7))
                .andExpect(jsonPath("$.data.analysisId").value("ana-review-7"))
                .andExpect(jsonPath("$.data.executionPlanId").value("plan-review-7"))
                .andExpect(jsonPath("$.data.outcome").value("LOSS"))
                .andExpect(jsonPath("$.data.executionDeviationStatus").value("DEVIATED"))
                .andExpect(jsonPath("$.data.planInvalidatedBeforeClose").value(true))
                .andExpect(jsonPath("$.data.warnedBeforeClose").value(true))
                .andExpect(jsonPath("$.data.ignoredWarning").value(true))
                .andExpect(jsonPath("$.data.monitorLogs.length()").value(1))
                .andExpect(jsonPath("$.data.reviewOnly").value(true))
                .andExpect(jsonPath("$.data.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notExecutable").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true))
                .andExpect(jsonPath("$.data.notOrderExecution").value(true))
                .andExpect(jsonPath("$.data.notAutoOpen").value(true))
                .andExpect(jsonPath("$.data.notAutoClose").value(true))
                .andExpect(jsonPath("$.data.notAutoReverse").value(true))
                .andExpect(jsonPath("$.data.notUserPositionMutation").value(true))
                .andExpect(jsonPath("$.data.notRuleAutoApply").value(true))
                .andExpect(jsonPath("$.data.openAction").doesNotExist())
                .andExpect(jsonPath("$.data.closeAction").doesNotExist())
                .andExpect(jsonPath("$.data.reduceAction").doesNotExist())
                .andExpect(jsonPath("$.data.reverseAction").doesNotExist())
                .andExpect(jsonPath("$.data.orderAction").doesNotExist())
                .andExpect(jsonPath("$.data.executionAction").doesNotExist())
                .andExpect(jsonPath("$.data.autoTradingAction").doesNotExist())
                .andExpect(jsonPath("$.data.ruleApplyAction").doesNotExist())
                .andExpect(jsonPath("$.data.executablePayload").doesNotExist())
                .andExpect(jsonPath("$.data.providerPayload").doesNotExist());

        verify(userPositionReviewAdapter).buildSummary(7L);
        verify(reviewService, never()).saveOrUpdate(any());
    }

    @Test
    void feedbackEndpointRecordsManualRuleFeedbackThroughAdapter() throws Exception {
        UserPositionReviewFeedbackResultDTO result = new UserPositionReviewFeedbackResultDTO();
        result.setPositionId(7L);
        result.setAnalysisId("ana-review-7");
        result.setReviewId("review-7");
        result.setErrorType("PLAN_EXECUTION_MISMATCH");
        result.setActualOutcome("LOSS");
        result.setAdjustmentSuggestion("tighten rule");
        result.setRuleFeedbackRecorded(true);
        result.setRuleChangeApplied(false);
        result.setRecordedAt(LocalDateTime.of(2026, 6, 22, 12, 0));
        when(userPositionReviewAdapter.recordFeedback(any(), any(UserPositionReviewFeedbackReq.class))).thenReturn(result);

        mockMvc.perform(post("/api/review/user-positions/7/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"errorType\":\"PLAN_EXECUTION_MISMATCH\",\"actualOutcome\":\"LOSS\",\"adjustmentSuggestion\":\"tighten rule\",\"analysisId\":\"client-forbidden\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionId").value(7))
                .andExpect(jsonPath("$.data.analysisId").value("ana-review-7"))
                .andExpect(jsonPath("$.data.ruleFeedbackRecorded").value(true))
                .andExpect(jsonPath("$.data.ruleChangeApplied").value(false))
                .andExpect(jsonPath("$.data.manualInputOnly").value(true))
                .andExpect(jsonPath("$.data.notRuleAutoApply").value(true))
                .andExpect(jsonPath("$.data.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.notExecutable").value(true))
                .andExpect(jsonPath("$.data.notAutoTrading").value(true));

        verify(userPositionReviewAdapter).recordFeedback(any(), any(UserPositionReviewFeedbackReq.class));
        verify(positionMonitorLogService, never()).recordMonitorRun(any());
    }

    @Test
    void summaryEndpointRejectsOpenPositionErrorsAsBadRequest() throws Exception {
        when(userPositionReviewAdapter.buildSummary(8L))
                .thenThrow(new IllegalArgumentException("POSITION_NOT_CLOSED"));

        mockMvc.perform(get("/api/review/user-positions/8/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("POSITION_NOT_CLOSED"));
    }

    private static UserPositionReviewSummaryDTO summary() {
        UserPositionReviewSummaryDTO dto = new UserPositionReviewSummaryDTO();
        dto.setPositionId(7L);
        dto.setAssetSymbol("BTCUSDT");
        dto.setSide("LONG");
        dto.setPositionStatus("CLOSED");
        dto.setAnalysisId("ana-review-7");
        dto.setExecutionPlanId("plan-review-7");
        dto.setPlanContextStatus("PLAN_CONTEXT_FOUND");
        dto.setEntryPrice(new BigDecimal("100"));
        dto.setClosePrice(new BigDecimal("90"));
        dto.setStopLoss(new BigDecimal("95"));
        dto.setTakeProfit(new BigDecimal("120"));
        dto.setQuantity(new BigDecimal("2"));
        dto.setLeverage(new BigDecimal("3"));
        dto.setOpenedAt(LocalDateTime.of(2026, 6, 22, 8, 0));
        dto.setClosedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
        dto.setGrossPnl(new BigDecimal("-20"));
        dto.setOutcome("LOSS");
        dto.setExecutionDeviationStatus("DEVIATED");
        dto.setMonitorLogCount(1);
        dto.setMonitorLogs(List.of(log()));
        dto.setPlanInvalidatedBeforeClose(true);
        dto.setWarnedBeforeClose(true);
        dto.setIgnoredWarning(true);
        dto.setWarningTimelinessStatus("TIMELY_WARNING");
        dto.setReviewStatus("REVIEW_SUMMARY_READY");
        return dto;
    }

    private static PositionMonitorLogDTO log() {
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogId(1L);
        dto.setPositionId(7L);
        dto.setAnalysisId("ana-review-7");
        dto.setLogicStatus("PLAN_INVALIDATED");
        dto.setSuggestedAction("RECHECK_PLAN");
        dto.setRiskLevel("HIGH");
        dto.setCurrentPrice(new BigDecimal("94"));
        dto.setCreatedAt(LocalDateTime.of(2026, 6, 22, 9, 0));
        return dto;
    }
}
