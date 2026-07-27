package org.example.trademodel.userpositionreview;

import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.vo.ReviewStateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.beans.Introspector;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class UserPositionReviewAdapterTest {
    private static final Long USER_ID = 17L;

    @Mock
    private UserPositionMapper userPositionMapper;
    @Mock
    private ExecutionPlanMapper executionPlanMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;
    @Mock
    private PositionMonitorLogService positionMonitorLogService;
    @Mock
    private ReviewService reviewService;

    private DefaultUserPositionReviewAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DefaultUserPositionReviewAdapter(
                userPositionMapper,
                executionPlanMapper,
                analysisRunMapper,
                positionMonitorLogService,
                reviewService);
        lenient().when(analysisRunMapper.selectById(anyString())).thenAnswer(invocation ->
                analysisRun(invocation.getArgument(0), "BTCUSDT"));
    }

    @Test
    void closedLongWinReadsPlanRealFieldsAllLogsAndProducesSafeAlignedSummary() throws Exception {
        UserPositionDO position = closedPosition(1L, "LONG", "plan-1", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(1L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-1")).thenReturn(plan("plan-1", "ana-1", "100", "95", "120"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 1L)).thenReturn(List.of(
                log(11L, "LOGIC_VALID", "HOLD", "LOW", LocalDateTime.of(2026, 6, 22, 8, 30)),
                log(12L, "LOGIC_WEAKENED", "MANUAL_REVIEW", "MEDIUM", LocalDateTime.of(2026, 6, 22, 9, 0))));

        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 1L);

        assertThat(summary.getPositionId()).isEqualTo(1L);
        assertThat(summary.getAnalysisId()).isEqualTo("ana-1");
        assertThat(summary.getExecutionPlanId()).isEqualTo("plan-1");
        assertThat(summary.getEntryPrice()).isEqualByComparingTo("100");
        assertThat(summary.getClosePrice()).isEqualByComparingTo("112");
        assertThat(summary.getStopLoss()).isEqualByComparingTo("95");
        assertThat(summary.getTakeProfit()).isEqualByComparingTo("120");
        assertThat(summary.getQuantity()).isEqualByComparingTo("2");
        assertThat(summary.getLeverage()).isEqualByComparingTo("3");
        assertThat(summary.getOutcome()).isEqualTo("WIN");
        assertThat(summary.getGrossPnl()).isEqualByComparingTo("24");
        assertThat(summary.getGrossReturnPct()).isEqualByComparingTo("12");
        assertThat(summary.getLeveragedReturnPctProxy()).isEqualByComparingTo("36");
        assertThat(summary.getExecutionDeviationStatus()).isEqualTo("ALIGNED");
        assertThat(summary.getMonitorLogs()).extracting(PositionMonitorLogDTO::getLogId).containsExactly(11L, 12L);
        assertThat(summary.isWarnedBeforeClose()).isTrue();
        assertThat(summary.getWarningTimelinessStatus()).isEqualTo("TIMELY_WARNING");
        assertThat(summary.isIgnoredWarning()).isFalse();
        assertSafetyFields(summary);
        assertForbiddenSummaryFieldsAbsent();

        verify(reviewService, never()).saveOrUpdate(any());
        verify(userPositionMapper, never())
                .manualCloseByIdAndUserId(anyLong(), anyLong(), any(), any(), any(), any());
        verify(userPositionMapper, never()).insert(any());
    }

    @Test
    void closedLongLossPlanInvalidationAndIgnoredWarningAreDetected() {
        UserPositionDO position = closedPosition(2L, "LONG", "plan-2", "100", "90", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(2L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-2")).thenReturn(plan("plan-2", "ana-2", "100", "95", "120"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 2L)).thenReturn(List.of(
                log(21L, "PLAN_INVALIDATED", "RECHECK_PLAN", "HIGH", LocalDateTime.of(2026, 6, 22, 9, 0)),
                log(22L, "HIGH_RISK", "RISK_REVIEW", "HIGH", LocalDateTime.of(2026, 6, 22, 9, 30))));

        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 2L);

        assertThat(summary.getOutcome()).isEqualTo("LOSS");
        assertThat(summary.isPlanInvalidatedBeforeClose()).isTrue();
        assertThat(summary.getFirstPlanInvalidatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 22, 9, 0));
        assertThat(summary.getPlanInvalidationWarningCount()).isEqualTo(1);
        assertThat(summary.isIgnoredWarning()).isTrue();
        assertThat(summary.getIgnoredWarningReasons())
                .contains("LOSS_AFTER_WARNING", "PLAN_INVALIDATED_BEFORE_CLOSE", "HIGH_RISK_BEFORE_CLOSE");
    }

    @Test
    void closedShortWinLossAndBreakevenOutcomesUseSideAwarePnl() {
        when(userPositionMapper.selectByIdAndUserId(3L, USER_ID)).thenReturn(closedPosition(3L, "SHORT", null, "100", "90", "110", "80"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 3L)).thenReturn(List.of());
        assertThat(adapter.buildSummaryForUser(USER_ID, 3L).getOutcome()).isEqualTo("WIN");

        when(userPositionMapper.selectByIdAndUserId(4L, USER_ID)).thenReturn(closedPosition(4L, "SHORT", null, "100", "110", "115", "80"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 4L)).thenReturn(List.of());
        assertThat(adapter.buildSummaryForUser(USER_ID, 4L).getOutcome()).isEqualTo("LOSS");

        when(userPositionMapper.selectByIdAndUserId(5L, USER_ID)).thenReturn(closedPosition(5L, "LONG", null, "100", "100", "95", "120"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 5L)).thenReturn(List.of());
        assertThat(adapter.buildSummaryForUser(USER_ID, 5L).getOutcome()).isEqualTo("BREAKEVEN");
    }

    @Test
    void rejectsOpenPartiallyClosedAndInvalidClosedPositionsFailClosed() {
        when(userPositionMapper.selectByIdAndUserId(6L, USER_ID)).thenReturn(positionWithStatus(6L, "OPEN"));
        assertThatThrownBy(() -> adapter.buildSummaryForUser(USER_ID, 6L)).hasMessageContaining("POSITION_NOT_CLOSED");

        when(userPositionMapper.selectByIdAndUserId(7L, USER_ID)).thenReturn(positionWithStatus(7L, "PARTIALLY_CLOSED"));
        assertThatThrownBy(() -> adapter.buildSummaryForUser(USER_ID, 7L)).hasMessageContaining("POSITION_NOT_FULLY_CLOSED");

        UserPositionDO missingEntry = closedPosition(8L, "LONG", null, "100", "110", "95", "120");
        missingEntry.setEntryPrice(null);
        when(userPositionMapper.selectByIdAndUserId(8L, USER_ID)).thenReturn(missingEntry);
        assertThatThrownBy(() -> adapter.buildSummaryForUser(USER_ID, 8L)).hasMessageContaining("entryPrice");

        UserPositionDO missingClose = closedPosition(9L, "LONG", null, "100", "110", "95", "120");
        missingClose.setClosePrice(null);
        when(userPositionMapper.selectByIdAndUserId(9L, USER_ID)).thenReturn(missingClose);
        assertThatThrownBy(() -> adapter.buildSummaryForUser(USER_ID, 9L)).hasMessageContaining("closePrice");

        UserPositionDO badQuantity = closedPosition(10L, "LONG", null, "100", "110", "95", "120");
        badQuantity.setQuantity(BigDecimal.ZERO);
        when(userPositionMapper.selectByIdAndUserId(10L, USER_ID)).thenReturn(badQuantity);
        assertThatThrownBy(() -> adapter.buildSummaryForUser(USER_ID, 10L)).hasMessageContaining("quantity");

        UserPositionDO badLeverage = closedPosition(11L, "LONG", null, "100", "110", "95", "120");
        badLeverage.setLeverage(BigDecimal.ZERO);
        when(userPositionMapper.selectByIdAndUserId(11L, USER_ID)).thenReturn(badLeverage);
        assertThatThrownBy(() -> adapter.buildSummaryForUser(USER_ID, 11L)).hasMessageContaining("leverage");

        UserPositionDO badTime = closedPosition(12L, "LONG", null, "100", "110", "95", "120");
        badTime.setClosedAt(LocalDateTime.of(2026, 6, 22, 7, 0));
        when(userPositionMapper.selectByIdAndUserId(12L, USER_ID)).thenReturn(badTime);
        assertThatThrownBy(() -> adapter.buildSummaryForUser(USER_ID, 12L)).hasMessageContaining("closedAt");

        verify(positionMonitorLogService, never())
                .listAllByPositionIdForUserReview(USER_ID, 6L);
    }

    @Test
    void typedUniqueAnalysisLinksPlanAndMissingPlanKeepsSafeNotComputableSummary() {
        UserPositionDO byAnalysis = closedPosition(13L, "LONG",
                PositionMonitorSourceContract.analysisReference("ana-13"),
                "100", "111", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(13L, USER_ID)).thenReturn(byAnalysis);
        when(executionPlanMapper.selectOnlyByAnalysisId("ana-13"))
                .thenReturn(plan("plan-from-analysis", "ana-13", "100", "95", "120"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 13L)).thenReturn(List.of());
        assertThat(adapter.buildSummaryForUser(USER_ID, 13L).getExecutionPlanId()).isEqualTo("plan-from-analysis");

        UserPositionDO missingPlan = closedPosition(14L, "LONG", null, "100", "111", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(14L, USER_ID)).thenReturn(missingPlan);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 14L)).thenReturn(List.of());
        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 14L);
        assertThat(summary.getPlanContextStatus()).isEqualTo("PLAN_CONTEXT_MISSING");
        assertThat(summary.getAnalysisId()).isEqualTo("USER_POSITION_14");
        assertThat(summary.getExecutionDeviationStatus()).isEqualTo("NOT_COMPUTABLE");
        assertThat(summary.isManualReviewOnly()).isTrue();
    }

    @Test
    void executionDeviationDeviatedAndNotComputableAreSeparated() {
        UserPositionDO deviated = closedPosition(15L, "LONG", "plan-15", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(15L, USER_ID)).thenReturn(deviated);
        when(executionPlanMapper.selectByPlanId("plan-15")).thenReturn(plan("plan-15", "ana-15", "103", "95", "120"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 15L)).thenReturn(List.of());
        UserPositionReviewSummaryDTO result = adapter.buildSummaryForUser(USER_ID, 15L);
        assertThat(result.getExecutionDeviationStatus()).isEqualTo("DEVIATED");
        assertThat(result.getExecutionDeviationReasons()).contains("ENTRY_DEVIATED");

        UserPositionDO notComputable = closedPosition(16L, "LONG", "plan-16", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(16L, USER_ID)).thenReturn(notComputable);
        ExecutionPlanDO plan = plan("plan-16", "ana-16", "zone", "stop", "take profit text");
        when(executionPlanMapper.selectByPlanId("plan-16")).thenReturn(plan);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 16L)).thenReturn(List.of());
        assertThat(adapter.buildSummaryForUser(USER_ID, 16L).getExecutionDeviationStatus()).isEqualTo("NOT_COMPUTABLE");
    }

    @Test
    void noWarningAndNonIgnoredWarningRemainDistinct() {
        UserPositionDO noWarning = closedPosition(17L, "LONG", null, "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(17L, USER_ID)).thenReturn(noWarning);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 17L)).thenReturn(List.of(
                log(171L, "LOGIC_VALID", "HOLD", "LOW", LocalDateTime.of(2026, 6, 22, 8, 30))));
        UserPositionReviewSummaryDTO noWarningSummary = adapter.buildSummaryForUser(USER_ID, 17L);
        assertThat(noWarningSummary.isWarnedBeforeClose()).isFalse();
        assertThat(noWarningSummary.getWarningTimelinessStatus()).isEqualTo("NO_WARNING_BEFORE_CLOSE");

        UserPositionDO winAfterWarning = closedPosition(18L, "LONG", null, "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(18L, USER_ID)).thenReturn(winAfterWarning);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 18L)).thenReturn(List.of(
                log(181L, "LOGIC_WEAKENED", "MANUAL_REVIEW", "MEDIUM", LocalDateTime.of(2026, 6, 22, 9, 0))));
        UserPositionReviewSummaryDTO warningSummary = adapter.buildSummaryForUser(USER_ID, 18L);
        assertThat(warningSummary.isWarnedBeforeClose()).isTrue();
        assertThat(warningSummary.isIgnoredWarning()).isFalse();
    }

    @Test
    void feedbackRecordsThroughExistingReviewServiceAndDerivesAnalysisIdServerSide() {
        UserPositionDO position = closedPosition(19L, "LONG", "plan-19", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(19L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-19"))
                .thenReturn(plan("plan-19", "ana-server-19", "100", "95", "120"));
        ReviewStateVO state = new ReviewStateVO();
        state.setReviewId("review-19");
        state.setAnalysisId("ana-server-19");
        state.setErrorType("PLAN_EXECUTION_MISMATCH");
        state.setActualOutcome("LOSS");
        state.setAdjustmentSuggestion("tighten feedback");
        state.setUpdateTime(LocalDateTime.of(2026, 6, 22, 11, 0));
        when(reviewService.saveOrUpdateForUserPosition(anyLong(), anyLong(), any())).thenReturn(state);

        UserPositionReviewFeedbackReq req = new UserPositionReviewFeedbackReq();
        req.setErrorType("PLAN_EXECUTION_MISMATCH");
        req.setActualOutcome("LOSS");
        req.setAdjustmentSuggestion("tighten feedback");
        UserPositionReviewFeedbackResultDTO result = adapter.recordFeedbackForUser(USER_ID, 19L, req);

        ArgumentCaptor<WriteReviewResultReq> captor = ArgumentCaptor.forClass(WriteReviewResultReq.class);
        verify(reviewService).saveOrUpdateForUserPosition(eq(USER_ID), eq(19L), captor.capture());
        assertThat(captor.getValue().getAnalysisId()).isEqualTo("ana-server-19");
        assertThat(result.getReviewId()).isEqualTo("review-19");
        assertThat(result.isRuleFeedbackRecorded()).isTrue();
        assertThat(result.isRuleChangeApplied()).isFalse();
        assertThat(result.isNotRuleAutoApply()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
    }

    @Test
    void feedbackUsesStableUserPositionAnalysisIdWhenPlanMissing() {
        UserPositionDO position = closedPosition(20L, "LONG", null, "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(20L, USER_ID)).thenReturn(position);
        ReviewStateVO state = new ReviewStateVO();
        state.setReviewId("review-20");
        state.setAnalysisId("USER_POSITION_20");
        when(reviewService.saveOrUpdateForUserPosition(anyLong(), anyLong(), any())).thenReturn(state);

        UserPositionReviewFeedbackResultDTO result = adapter.recordFeedbackForUser(USER_ID, 20L, new UserPositionReviewFeedbackReq());

        ArgumentCaptor<WriteReviewResultReq> captor = ArgumentCaptor.forClass(WriteReviewResultReq.class);
        verify(reviewService).saveOrUpdateForUserPosition(eq(USER_ID), eq(20L), captor.capture());
        assertThat(captor.getValue().getAnalysisId()).isEqualTo("USER_POSITION_20");
        assertThat(result.getAnalysisId()).isEqualTo("USER_POSITION_20");
    }

    @Test
    void closedPositionCrossSymbolTypedPlanFailsClosed() {
        UserPositionDO position = closedPosition(21L, "LONG", "plan-foreign", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(21L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-foreign"))
                .thenReturn(plan("plan-foreign", "ana-foreign", "100", "95", "120"));
        when(analysisRunMapper.selectById("ana-foreign"))
                .thenReturn(analysisRun("ana-foreign", "ETHUSDT"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 21L)).thenReturn(List.of());

        assertUnverifiedPlanSummary(adapter.buildSummaryForUser(USER_ID, 21L), 21L);
    }

    @Test
    void closedPositionMissingAnalysisRunFailsClosed() {
        UserPositionDO position = closedPosition(22L, "LONG", "plan-no-run", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(22L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-no-run"))
                .thenReturn(plan("plan-no-run", "ana-no-run", "100", "95", "120"));
        when(analysisRunMapper.selectById("ana-no-run")).thenReturn(null);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 22L)).thenReturn(List.of());

        assertUnverifiedPlanSummary(adapter.buildSummaryForUser(USER_ID, 22L), 22L);
    }

    @Test
    void closedPositionPlanRunAnalysisMismatchFailsClosed() {
        UserPositionDO position = closedPosition(23L, "LONG", "plan-run-mismatch", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(23L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-run-mismatch"))
                .thenReturn(plan("plan-run-mismatch", "ana-plan", "100", "95", "120"));
        when(analysisRunMapper.selectById("ana-plan"))
                .thenReturn(analysisRun("ana-other", "BTCUSDT"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 23L)).thenReturn(List.of());

        assertUnverifiedPlanSummary(adapter.buildSummaryForUser(USER_ID, 23L), 23L);
    }

    @Test
    void closedPositionAmbiguousAnalysisSourceFailsClosed() {
        UserPositionDO position = closedPosition(24L, "LONG",
                PositionMonitorSourceContract.analysisReference("ana-ambiguous"),
                "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(24L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectOnlyByAnalysisId("ana-ambiguous")).thenReturn(null);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 24L)).thenReturn(List.of());

        assertUnverifiedPlanSummary(adapter.buildSummaryForUser(USER_ID, 24L), 24L);
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
    }

    @Test
    void closedPositionExactVerifiedPlanStillBuildsReview() {
        UserPositionDO position = closedPosition(25L, "LONG", "plan-exact", "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(25L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-exact"))
                .thenReturn(plan("plan-exact", "ana-exact", "100", "95", "120"));
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 25L)).thenReturn(List.of());

        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 25L);

        assertThat(summary.getPlanContextStatus()).isEqualTo("PLAN_CONTEXT_FOUND");
        assertThat(summary.getAnalysisId()).isEqualTo("ana-exact");
        assertThat(summary.getExecutionPlanId()).isEqualTo("plan-exact");
        assertThat(summary.getEntryZone()).isEqualTo("100");
    }

    @Test
    void unverifiedClosedPositionFeedbackNeverTargetsForeignAnalysis() {
        UserPositionDO position = closedPosition(26L, "LONG", "plan-foreign-feedback",
                "100", "112", "95", "120");
        when(userPositionMapper.selectByIdAndUserId(26L, USER_ID)).thenReturn(position);
        when(executionPlanMapper.selectByPlanId("plan-foreign-feedback"))
                .thenReturn(plan("plan-foreign-feedback", "ana-foreign-feedback", "100", "95", "120"));
        when(analysisRunMapper.selectById("ana-foreign-feedback"))
                .thenReturn(analysisRun("ana-foreign-feedback", "ETHUSDT"));
        when(reviewService.saveOrUpdateForUserPosition(anyLong(), anyLong(), any())).thenAnswer(invocation -> {
            WriteReviewResultReq request = invocation.getArgument(2);
            ReviewStateVO state = new ReviewStateVO();
            state.setAnalysisId(request.getAnalysisId());
            return state;
        });

        UserPositionReviewFeedbackResultDTO result = adapter.recordFeedbackForUser(USER_ID,
                26L, new UserPositionReviewFeedbackReq());

        ArgumentCaptor<WriteReviewResultReq> captor = ArgumentCaptor.forClass(WriteReviewResultReq.class);
        verify(reviewService).saveOrUpdateForUserPosition(eq(USER_ID), eq(26L), captor.capture());
        assertThat(captor.getValue().getAnalysisId()).isEqualTo("USER_POSITION_26");
        assertThat(captor.getValue().getAnalysisId()).isNotEqualTo("ana-foreign-feedback");
        assertThat(result.getAnalysisId()).isEqualTo("USER_POSITION_26");
    }

    @Test
    void typedAnalysisMonitorsA_thenSiblingBAppears_closedReviewStillUsesA() {
        UserPositionDO position = closedPosition(27L, "LONG",
                PositionMonitorSourceContract.analysisReference("analysis-X"),
                "100", "112", "95", "120");
        PositionMonitorLogDTO monitorA = sourceLog(271L, 27L, "analysis-X", "plan-A",
                LocalDateTime.of(2026, 6, 22, 9, 0));
        when(userPositionMapper.selectByIdAndUserId(27L, USER_ID)).thenReturn(position);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 27L)).thenReturn(List.of(monitorA));
        when(executionPlanMapper.selectByPlanId("plan-A"))
                .thenReturn(plan("plan-A", "analysis-X", "A-entry", "A-stop", "A-tp"));
        lenient().when(executionPlanMapper.selectOnlyByAnalysisId("analysis-X")).thenReturn(null);
        lenient().when(executionPlanMapper.selectLatestByAnalysisId("analysis-X"))
                .thenReturn(plan("plan-B", "analysis-X", "B-entry", "B-stop", "B-tp"));

        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 27L);

        assertThat(summary.getPlanContextStatus()).isEqualTo("PLAN_CONTEXT_FOUND");
        assertThat(summary.getExecutionPlanId()).isEqualTo("plan-A");
        assertThat(summary.getEntryZone()).isEqualTo("A-entry");
        assertThat(summary.getEntryZone()).isNotEqualTo("B-entry");
        assertThat(summary.getMonitorLogs()).singleElement().satisfies(log -> {
            assertThat(log.isSourceVerified()).isTrue();
            assertThat(log.getAnalysisId()).isEqualTo("analysis-X");
            assertThat(log.getExecutionPlanId()).isEqualTo("plan-A");
        });
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(anyString());
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
        verify(executionPlanMapper, never()).selectByPlanId("plan-B");
    }

    @Test
    void closedReviewAndFeedbackUseSameMonitorResolvedPlanA() {
        UserPositionDO position = closedPosition(28L, "LONG",
                PositionMonitorSourceContract.analysisReference("analysis-X"),
                "100", "112", "95", "120");
        PositionMonitorLogDTO monitorA = sourceLog(281L, 28L, "analysis-X", "plan-A",
                LocalDateTime.of(2026, 6, 22, 9, 0));
        when(userPositionMapper.selectByIdAndUserId(28L, USER_ID)).thenReturn(position);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 28L)).thenReturn(List.of(monitorA));
        when(executionPlanMapper.selectByPlanId("plan-A"))
                .thenReturn(plan("plan-A", "analysis-X", "A-entry", "A-stop", "A-tp"));
        when(reviewService.saveOrUpdateForUserPosition(anyLong(), anyLong(), any())).thenAnswer(invocation -> {
            WriteReviewResultReq request = invocation.getArgument(2);
            ReviewStateVO state = new ReviewStateVO();
            state.setReviewId("review-28");
            state.setAnalysisId(request.getAnalysisId());
            return state;
        });

        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 28L);
        UserPositionReviewFeedbackResultDTO feedback = adapter.recordFeedbackForUser(USER_ID,
                28L, new UserPositionReviewFeedbackReq());

        ArgumentCaptor<WriteReviewResultReq> captor = ArgumentCaptor.forClass(WriteReviewResultReq.class);
        verify(reviewService).saveOrUpdateForUserPosition(eq(USER_ID), eq(28L), captor.capture());
        assertThat(summary.getExecutionPlanId()).isEqualTo("plan-A");
        assertThat(summary.getAnalysisId()).isEqualTo("analysis-X");
        assertThat(feedback.getAnalysisId()).isEqualTo("analysis-X");
        assertThat(captor.getValue().getAnalysisId()).isEqualTo(summary.getAnalysisId());
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(anyString());
        verify(executionPlanMapper, never()).selectByPlanId("plan-B");
    }

    @Test
    void legacyGuessedSiblingBDoesNotReachDashboardOrReviewCenter() {
        UserPositionDO position = closedPosition(29L, "LONG",
                PositionMonitorSourceContract.executionPlanReference("plan-A"),
                "100", "112", "95", "120");
        PositionMonitorLogDTO guessedB = sourceLog(291L, 29L, "analysis-X", "plan-B",
                LocalDateTime.of(2026, 6, 22, 9, 0));
        when(userPositionMapper.selectByIdAndUserId(29L, USER_ID)).thenReturn(position);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 29L)).thenReturn(List.of(guessedB));
        when(executionPlanMapper.selectByPlanId("plan-A"))
                .thenReturn(plan("plan-A", "analysis-X", "A-entry", "A-stop", "A-tp"));

        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 29L);

        assertThat(summary.getExecutionPlanId()).isEqualTo("plan-A");
        assertThat(summary.getEntryZone()).isEqualTo("A-entry");
        assertThat(summary.getMonitorLogs()).singleElement().satisfies(log -> {
            assertThat(log.isSourceVerified()).isFalse();
            assertThat(log.getAnalysisId()).isNull();
            assertThat(log.getExecutionPlanId()).isNull();
        });
        verify(executionPlanMapper, never()).selectByPlanId("plan-B");
    }

    @Test
    void untypedLegacyPositionCannotPromoteOldMonitorIds() {
        UserPositionDO position = closedPosition(30L, "LONG", null,
                "100", "112", "95", "120");
        position.setSourceRefId("legacy-untyped");
        PositionMonitorLogDTO legacy = sourceLog(301L, 30L, "analysis-X", "plan-A",
                LocalDateTime.of(2026, 6, 22, 9, 0));
        when(userPositionMapper.selectByIdAndUserId(30L, USER_ID)).thenReturn(position);
        when(positionMonitorLogService.listAllByPositionIdForUserReview(USER_ID, 30L)).thenReturn(List.of(legacy));

        UserPositionReviewSummaryDTO summary = adapter.buildSummaryForUser(USER_ID, 30L);

        assertUnverifiedPlanSummary(summary, 30L);
        assertThat(summary.getMonitorLogs()).singleElement().satisfies(log -> {
            assertThat(log.isSourceVerified()).isFalse();
            assertThat(log.getAnalysisId()).isNull();
            assertThat(log.getExecutionPlanId()).isNull();
        });
        verify(executionPlanMapper, never()).selectByPlanId(anyString());
    }

    private static UserPositionDO positionWithStatus(Long id, String status) {
        UserPositionDO row = closedPosition(id, "LONG", null, "100", "110", "95", "120");
        row.setStatus(status);
        return row;
    }

    private static UserPositionDO closedPosition(Long id,
                                                 String side,
                                                 String sourceRefId,
                                                 String entryPrice,
                                                 String closePrice,
                                                 String stopLoss,
                                                 String takeProfit) {
        UserPositionDO row = new UserPositionDO();
        row.setId(id);
        row.setUserId(USER_ID);
        row.setAssetSymbol("BTCUSDT");
        row.setSide(side);
        row.setStatus("CLOSED");
        row.setEntryPrice(new BigDecimal(entryPrice));
        row.setClosePrice(new BigDecimal(closePrice));
        row.setStopLoss(new BigDecimal(stopLoss));
        row.setTakeProfit(new BigDecimal(takeProfit));
        row.setQuantity(new BigDecimal("2"));
        row.setLeverage(new BigDecimal("3"));
        row.setOpenedAt(LocalDateTime.of(2026, 6, 22, 8, 0));
        row.setClosedAt(LocalDateTime.of(2026, 6, 22, 10, 0));
        row.setSourceType("MANUAL");
        row.setSourceRefId(sourceRefId == null || PositionMonitorSourceContract.parse(sourceRefId) != null
                ? sourceRefId
                : PositionMonitorSourceContract.executionPlanReference(sourceRefId));
        return row;
    }

    private static ExecutionPlanDO plan(String planId,
                                        String analysisId,
                                        String entryZone,
                                        String stopLoss,
                                        String takeProfitRules) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(planId);
        plan.setAnalysisId(analysisId);
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setEntryZone(entryZone);
        plan.setStopLoss(stopLoss);
        plan.setTakeProfitRules(takeProfitRules);
        plan.setInvalidCondition("invalid when stop breached");
        plan.setRecommendedAction("WATCH");
        return plan;
    }

    private static AnalysisRunDO analysisRun(String analysisId, String symbol) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        run.setTraceId("trace-" + analysisId);
        return run;
    }

    private static void assertUnverifiedPlanSummary(UserPositionReviewSummaryDTO summary, Long positionId) {
        assertThat(summary.getPlanContextStatus()).isEqualTo("PLAN_CONTEXT_MISSING");
        assertThat(summary.getExecutionDeviationStatus()).isEqualTo("NOT_COMPUTABLE");
        assertThat(summary.getAnalysisId()).isEqualTo("USER_POSITION_" + positionId);
        assertThat(summary.getSourceRefId()).isNull();
        assertThat(summary.getExecutionPlanId()).isNull();
        assertThat(summary.getExecutionPlanStatus()).isNull();
        assertThat(summary.getEntryZone()).isNull();
        assertThat(summary.getPlanStopLoss()).isNull();
        assertThat(summary.getTakeProfitRules()).isNull();
        assertThat(summary.getInvalidCondition()).isNull();
        assertThat(summary.getRecommendedAction()).isNull();
        assertThat(summary.getReviewReasons()).contains("PLAN_SOURCE_UNVERIFIED");
    }

    private static PositionMonitorLogDTO log(Long logId,
                                             String logicStatus,
                                             String suggestedAction,
                                             String riskLevel,
                                             LocalDateTime createdAt) {
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogId(logId);
        dto.setPositionId(1L);
        dto.setAnalysisId("ana-log");
        dto.setCurrentPrice(new BigDecimal("101"));
        dto.setLogicStatus(logicStatus);
        dto.setSuggestedAction(suggestedAction);
        dto.setRiskLevel(riskLevel);
        dto.setCreatedAt(createdAt);
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

    private static PositionMonitorLogDTO sourceLog(Long logId,
                                                   Long positionId,
                                                   String analysisId,
                                                   String executionPlanId,
                                                   LocalDateTime createdAt) {
        PositionMonitorLogDTO dto = log(logId, "LOGIC_VALID", "HOLD", "LOW", createdAt);
        dto.setPositionId(positionId);
        dto.setAnalysisId(analysisId);
        dto.setExecutionPlanId(executionPlanId);
        return dto;
    }

    private static void assertSafetyFields(UserPositionReviewSummaryDTO dto) {
        assertThat(dto.isReviewOnly()).isTrue();
        assertThat(dto.isManualReviewOnly()).isTrue();
        assertThat(dto.isNotTradeInstruction()).isTrue();
        assertThat(dto.isNotExecutable()).isTrue();
        assertThat(dto.isNotAutoTrading()).isTrue();
        assertThat(dto.isNotOrderExecution()).isTrue();
        assertThat(dto.isNotAutoOpen()).isTrue();
        assertThat(dto.isNotAutoClose()).isTrue();
        assertThat(dto.isNotAutoReverse()).isTrue();
        assertThat(dto.isNotUserPositionMutation()).isTrue();
        assertThat(dto.isNotRuleAutoApply()).isTrue();
    }

    private static void assertForbiddenSummaryFieldsAbsent() throws Exception {
        Set<String> propertyNames = Arrays.stream(Introspector.getBeanInfo(UserPositionReviewSummaryDTO.class).getPropertyDescriptors())
                .map(descriptor -> descriptor.getName())
                .collect(Collectors.toSet());
        assertThat(propertyNames).doesNotContain(
                "openAction", "closeAction", "reduceAction", "reverseAction", "orderAction", "executionAction",
                "autoTradingAction", "ruleApplyAction", "executablePayload", "providerPayload");
    }
}
