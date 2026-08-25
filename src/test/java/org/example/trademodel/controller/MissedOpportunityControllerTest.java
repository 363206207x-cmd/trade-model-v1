package org.example.trademodel.controller;

import org.example.trademodel.entity.MissedOpportunityDO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.MissedOpportunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MissedOpportunityControllerTest {

    @Mock
    private MissedOpportunityService missedOpportunityService;
    @Mock
    private AuthenticatedUserIdResolver userIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(userIdResolver.requireCurrentUserId()).thenReturn(41L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MissedOpportunityController(missedOpportunityService, userIdResolver)).build();
    }

    @Test
    void reviewArchiveStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        LocalDate bizDate = LocalDate.of(2026, 6, 11);
        MissedOpportunityDO row = row("mo-ready", "ana-ready", "BTCUSDT", bizDate, validReasonJson(), "trace-ready");
        when(missedOpportunityService.countByBizDateForUser(41L, bizDate)).thenReturn(1);
        when(missedOpportunityService.queryForUser(41L, "ana-ready", "BTCUSDT", bizDate, 5))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/api/missed-opportunity/review-archive-status")
                        .param("analysisId", "ana-ready")
                .param("symbol", "btcusdt")
                .param("bizDate", "2026-06-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.aggregateStatus").value("REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.analysisId").value("ana-ready"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.missedCount").value(1))
                .andExpect(jsonPath("$.todayMissedCount").value(1))
                .andExpect(jsonPath("$.missedOpportunityCountStatus").value("MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY"))
                .andExpect(jsonPath("$.reviewArchiveCountStatus").value("REVIEW_ARCHIVE_COUNT_REVIEW_ONLY"))
                .andExpect(jsonPath("$.latestMissedId").value("mo-ready"))
                .andExpect(jsonPath("$.latestRuleVersion").value("missed-v1"))
                .andExpect(jsonPath("$.traceIdPresent").value(true))
                .andExpect(jsonPath("$.reasonViewAvailable").value(true))
                .andExpect(jsonPath("$.reasonParseStatus").value("OK"))
                .andExpect(jsonPath("$.archiveLinked").value(true))
                .andExpect(jsonPath("$.reviewAggregateMissedAvailable").value(false))
                .andExpect(jsonPath("$.queryAvailable").value(true))
                .andExpect(jsonPath("$.countAvailable").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("OK"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notFinalDirection").value(true))
                .andExpect(jsonPath("$.notEntryStopTpRr").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notMissedOpportunityGeneration").value(true))
                .andExpect(jsonPath("$.notMissedOpportunityWrite").value(true))
                .andExpect(jsonPath("$.notReviewResultGeneration").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notExternalChannel").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.missedOpportunityGenerationBoundaryStatus").value("MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.missedOpportunityWriteBoundaryStatus").value("MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reviewResultGenerationBoundaryStatus").value("REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.replayBoundaryStatus").value("REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.recheckBoundaryStatus").value("RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.pushBoundaryStatus").value("PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.candidateBoundaryStatus").value("CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.pointBoundaryStatus").value("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.tradingBoundaryStatus").value("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.failClosed").value(false));

        verify(missedOpportunityService, never()).save(any(MissedOpportunityDO.class));
        verify(missedOpportunityService, never())
                .recordFromAuthoritativeAnalysisIfEligible(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void reviewArchiveStatusEndpointFailsClosedWhenArchiveIsEmpty() throws Exception {
        LocalDate bizDate = LocalDate.of(2026, 6, 11);
        when(missedOpportunityService.countByBizDateForUser(41L, bizDate)).thenReturn(0);
        when(missedOpportunityService.queryForUser(41L, null, null, bizDate, 5))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/missed-opportunity/review-archive-status")
                .param("bizDate", "2026-06-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_ARCHIVE_AGGREGATE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.missedCount").value(0))
                .andExpect(jsonPath("$.todayMissedCount").value(0))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reason").value("REVIEW_ARCHIVE_AGGREGATE_MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notMissedOpportunityGeneration").value(true))
                .andExpect(jsonPath("$.notMissedOpportunityWrite").value(true))
                .andExpect(jsonPath("$.notReviewResultGeneration").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void reviewArchiveStatusEndpointReturnsCountOnlyPartialWhenCountExistsButRowsAreUnavailable() throws Exception {
        LocalDate bizDate = LocalDate.of(2026, 6, 11);
        when(missedOpportunityService.countByBizDateForUser(41L, bizDate)).thenReturn(3);
        when(missedOpportunityService.queryForUser(41L, null, null, bizDate, 5))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/missed-opportunity/review-archive-status")
                .param("bizDate", "2026-06-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY"))
                .andExpect(jsonPath("$.todayMissedCount").value(3))
                .andExpect(jsonPath("$.sourceHealth").value("PARTIAL"))
                .andExpect(jsonPath("$.missedOpportunityCountStatus").value("MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY"))
                .andExpect(jsonPath("$.reviewArchiveCountStatus").value("REVIEW_ARCHIVE_COUNT_REVIEW_ONLY"))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void reviewArchiveStatusEndpointFailsClosedWhenReadPathThrows() throws Exception {
        LocalDate bizDate = LocalDate.of(2026, 6, 11);
        when(missedOpportunityService.countByBizDateForUser(41L, bizDate))
                .thenThrow(new IllegalStateException("read unavailable"));

        mockMvc.perform(get("/api/missed-opportunity/review-archive-status")
                        .param("bizDate", "2026-06-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.queryAvailable").value(false))
                .andExpect(jsonPath("$.countAvailable").value(false))
                .andExpect(jsonPath("$.notMissedOpportunityGeneration").value(true))
                .andExpect(jsonPath("$.notMissedOpportunityWrite").value(true))
                .andExpect(jsonPath("$.notReviewResultGeneration").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notExternalChannel").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));

        verify(missedOpportunityService, never()).save(any(MissedOpportunityDO.class));
        verify(missedOpportunityService, never())
                .recordFromAuthoritativeAnalysisIfEligible(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void reviewArchiveStatusEndpointFailsClosedWhenReasonParsingFails() throws Exception {
        LocalDate bizDate = LocalDate.of(2026, 6, 11);
        MissedOpportunityDO row = row("mo-bad-json", "ana-bad-json", "BTCUSDT", bizDate, "{bad-json", "trace-bad-json");
        when(missedOpportunityService.countByBizDateForUser(41L, bizDate)).thenReturn(1);
        when(missedOpportunityService.queryForUser(41L, "ana-bad-json", "BTCUSDT", bizDate, 5))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/api/missed-opportunity/review-archive-status")
                        .param("analysisId", "ana-bad-json")
                .param("symbol", "BTCUSDT")
                .param("bizDate", "2026-06-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY"))
                .andExpect(jsonPath("$.reasonParseStatus").value("PARSE_FAILED"))
                .andExpect(jsonPath("$.reasonViewAvailable").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void reviewArchiveStatusEndpointDoesNotExposeExecutableCandidatePointOrTradingFields() throws Exception {
        LocalDate bizDate = LocalDate.of(2026, 6, 11);
        MissedOpportunityDO row = row("mo-safe", "ana-safe", "BTCUSDT", bizDate, validReasonJson(), "trace-safe");
        when(missedOpportunityService.countByBizDateForUser(41L, bizDate)).thenReturn(1);
        when(missedOpportunityService.queryForUser(41L, "ana-safe", "BTCUSDT", bizDate, 5))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/api/missed-opportunity/review-archive-status")
                        .param("analysisId", "ana-safe")
                        .param("symbol", "BTCUSDT")
                        .param("bizDate", "2026-06-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.generatedDecision").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.replayExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.recheckExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.missedOpportunityGenerationAction").doesNotExist())
                .andExpect(jsonPath("$.missedOpportunityWriteAction").doesNotExist())
                .andExpect(jsonPath("$.reviewResultGenerationAction").doesNotExist())
                .andExpect(jsonPath("$.reviewResultWriteAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.pushSendAction").doesNotExist())
                .andExpect(jsonPath("$.externalChannelAction").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    private static MissedOpportunityDO row(String missedId,
                                           String analysisId,
                                           String symbol,
                                           LocalDate bizDate,
                                           String reasonJson,
                                           String traceId) {
        MissedOpportunityDO row = new MissedOpportunityDO();
        row.setMissedId(missedId);
        row.setDecisionId("dec-" + missedId);
        row.setAnalysisId(analysisId);
        row.setSymbol(symbol);
        row.setBizDate(bizDate);
        row.setReasonJson(reasonJson);
        row.setRuleVersion("missed-v1");
        row.setTraceId(traceId);
        row.setCreateTime(LocalDateTime.of(2026, 6, 11, 9, 30));
        return row;
    }

    private static String validReasonJson() {
        return """
                {"version":"1","rule":"missed-v1","whyMissed":"review only archive row","facts":{},"refs":{"analysisId":"ana-ready"}}
                """;
    }
}
