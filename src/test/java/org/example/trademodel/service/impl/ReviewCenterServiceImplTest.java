package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.userpositionreview.UserPositionReviewAdapter;
import org.example.trademodel.userpositionreview.UserPositionReviewSummaryDTO;
import org.example.trademodel.vo.ReviewCenterDashboardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class ReviewCenterServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");
    @Mock
    private UserPositionMapper userPositionMapper;
    @Mock
    private UserPositionReviewAdapter userPositionReviewAdapter;
    @Mock
    private OpportunityLogService opportunityLogService;
    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private PushRecheckLogMapper pushRecheckLogMapper;
    @Mock
    private ReviewResultMapper reviewResultMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;

    private ReviewCenterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewCenterServiceImpl(
                userPositionMapper,
                userPositionReviewAdapter,
                opportunityLogService,
                pushSnapshotMapper,
                pushRecheckLogMapper,
                reviewResultMapper,
                analysisRunMapper);
        service.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void emptySourcesReturnEmptyArraysWithoutSyntheticRows() {
        when(userPositionMapper.listClosedManualPositions(anyInt())).thenReturn(List.of());
        when(opportunityLogService.query(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(pushSnapshotMapper.listRecent(anyInt())).thenReturn(List.of());
        when(reviewResultMapper.listRecent(anyInt())).thenReturn(List.of());

        ReviewCenterDashboardVO vo = service.getDashboard();

        assertThat(vo.getPositionReviews()).isEmpty();
        assertThat(vo.getOpportunityReviews()).isEmpty();
        assertThat(vo.getPushReviews()).isEmpty();
        assertThat(vo.getRuleFeedback()).isEmpty();
        assertThat(vo.getSummary().getPositionReviewCount()).isZero();
        assertThat(vo.getSummary().getOpportunityReviewCount()).isZero();
        assertThat(vo.getSummary().getPushReviewCount()).isZero();
        assertThat(vo.getSummary().getRuleFeedbackCount()).isZero();
        assertThat(vo.getDiagnostics().getOpportunityLogStatus()).isEqualTo("EMPTY");
        assertThat(vo.getDiagnostics().getReviewCenterStatus()).isEqualTo("READY_READONLY");
        verify(userPositionReviewAdapter, never()).buildSummary(any());
        verify(pushRecheckLogMapper, never()).selectLatestByPushId(any());
    }

    @Test
    void mapsOnlyReadonlyFieldsFromExistingSources() {
        when(userPositionMapper.listClosedManualPositions(anyInt())).thenReturn(List.of(position()));
        when(userPositionReviewAdapter.buildSummary(7L)).thenReturn(positionSummary());
        when(opportunityLogService.query(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(opportunity(OpportunityLogStatus.EXECUTED_VALID), opportunity(OpportunityLogStatus.PENDING_EVALUATION)));
        when(pushSnapshotMapper.listRecent(anyInt())).thenReturn(List.of(pushSnapshot()));
        when(pushRecheckLogMapper.selectLatestByPushId(3L)).thenReturn(pushLog());
        when(reviewResultMapper.listRecent(anyInt())).thenReturn(List.of(reviewResult()));
        when(analysisRunMapper.selectById("ana-rule-1")).thenReturn(analysisRun());

        ReviewCenterDashboardVO vo = service.getDashboard();

        assertThat(vo.getPositionReviews()).hasSize(1);
        ReviewCenterDashboardVO.PositionReviewItem position = vo.getPositionReviews().get(0);
        assertThat(position.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(position.getPnl()).isEqualByComparingTo("20");
        assertThat(position.getExecutionDeviation()).isEqualTo("ALIGNED");
        assertThat(position.getMonitorConclusion()).isEqualTo("PLAN_VALID");
        assertThat(position.getOriginalExecutionPlan().getPlanId()).isEqualTo("plan-7");

        assertThat(vo.getOpportunityReviews()).hasSize(1);
        ReviewCenterDashboardVO.OpportunityReviewItem opportunity = vo.getOpportunityReviews().get(0);
        assertThat(opportunity.getOpportunityType()).isEqualTo(OpportunityLogStatus.EXECUTED_VALID);
        assertThat(opportunity.getWasPushed()).isTrue();
        assertThat(opportunity.getWasClicked()).isNull();
        assertThat(opportunity.getWasExecuted()).isTrue();

        assertThat(vo.getPushReviews()).hasSize(1);
        ReviewCenterDashboardVO.PushReviewItem push = vo.getPushReviews().get(0);
        assertThat(push.getTelegramStatus()).isEqualTo(ReviewCenterServiceImpl.TELEGRAM_WAITING_SYNC);
        assertThat(push.getClicked()).isNull();
        assertThat(push.getRecheckStatus()).isEqualTo("REVIEW_PASSED");
        assertThat(push.getExpired()).isFalse();
        assertThat(push.getFailReason()).isEqualTo("{\"code\":\"NONE\"}");

        assertThat(vo.getRuleFeedback()).hasSize(1);
        ReviewCenterDashboardVO.RuleFeedbackItem rule = vo.getRuleFeedback().get(0);
        assertThat(rule.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(rule.getErrorType()).isEqualTo("RULE_TOO_LOOSE");
        assertThat(rule.getRuleIssue()).isTrue();
        assertThat(rule.getExecutionDeviation()).isNull();
        assertThat(rule.getStatus()).isNull();
        assertThat(vo.getDiagnostics().getOpportunityLogStatus()).isEqualTo("READY");
        assertThat(vo.getDiagnostics().getPushRecheckStatus()).isEqualTo("READY");

        verify(pushRecheckLogMapper).selectLatestByPushId(3L);
    }

    @Test
    void pushExpiryUsesUtcNaiveExactBoundary() {
        when(userPositionMapper.listClosedManualPositions(anyInt())).thenReturn(List.of());
        when(opportunityLogService.query(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(reviewResultMapper.listRecent(anyInt())).thenReturn(List.of());

        TmPushSnapshotDO before = pushSnapshotAt(LocalDateTime.of(2026, 7, 13, 12, 0, 1));
        TmPushSnapshotDO equal = pushSnapshotAt(LocalDateTime.of(2026, 7, 13, 12, 0));
        TmPushSnapshotDO after = pushSnapshotAt(LocalDateTime.of(2026, 7, 13, 11, 59, 59));
        when(pushSnapshotMapper.listRecent(anyInt())).thenReturn(List.of(before, equal, after));

        ReviewCenterDashboardVO vo = service.getDashboard();

        assertThat(vo.getPushReviews()).extracting(ReviewCenterDashboardVO.PushReviewItem::getExpired)
                .containsExactly(false, true, true);
    }

    @Test
    void reviewCenterTimelineHidesInternalSentinel() {
        UserPositionReviewSummaryDTO summary = positionSummary();
        PositionMonitorLogDTO internal = monitorLog();
        internal.setAnalysisId(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        internal.setExecutionPlanId("must-not-survive");
        summary.setMonitorLogs(List.of(internal));
        when(userPositionMapper.listClosedManualPositions(anyInt())).thenReturn(List.of(position()));
        when(userPositionReviewAdapter.buildSummary(7L)).thenReturn(summary);
        when(opportunityLogService.query(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(pushSnapshotMapper.listRecent(anyInt())).thenReturn(List.of());
        when(reviewResultMapper.listRecent(anyInt())).thenReturn(List.of());

        ReviewCenterDashboardVO vo = service.getDashboard();

        assertThat(vo.getPositionReviews()).singleElement().satisfies(item ->
                assertThat(item.getMonitorTimeline()).singleElement().satisfies(log -> {
                    assertThat(log.getAnalysisId()).isNull();
                    assertThat(log.getExecutionPlanId()).isNull();
                    assertThat(log.isSourceVerified()).isFalse();
                    assertThat(log.getSourceStatus()).isEqualTo("UNVERIFIED");
                    assertThat(log.getSourceStatusLabel()).isEqualTo("来源不可验证");
                }));
    }

    @Test
    void legacyGuessedSiblingBDoesNotReachReviewCenterTimeline() {
        UserPositionReviewSummaryDTO summary = positionSummary();
        PositionMonitorLogDTO guessedSibling = monitorLog();
        guessedSibling.setAnalysisId("analysis-X");
        guessedSibling.setExecutionPlanId("plan-B");
        guessedSibling.setSourceVerified(false);
        guessedSibling.setSourceStatus("PENDING_VERIFICATION");
        guessedSibling.setSourceStatusLabel("来源待验证");
        summary.setMonitorLogs(List.of(guessedSibling));
        when(userPositionMapper.listClosedManualPositions(anyInt())).thenReturn(List.of(position()));
        when(userPositionReviewAdapter.buildSummary(7L)).thenReturn(summary);
        when(opportunityLogService.query(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(pushSnapshotMapper.listRecent(anyInt())).thenReturn(List.of());
        when(reviewResultMapper.listRecent(anyInt())).thenReturn(List.of());

        ReviewCenterDashboardVO vo = service.getDashboard();

        assertThat(vo.getPositionReviews()).singleElement().satisfies(item ->
                assertThat(item.getMonitorTimeline()).singleElement().satisfies(log -> {
                    assertThat(log.getAnalysisId()).isNull();
                    assertThat(log.getExecutionPlanId()).isNull();
                    assertThat(log.isSourceVerified()).isFalse();
                    assertThat(log.getSourceStatus()).isEqualTo("UNVERIFIED");
                    assertThat(log.getSourceStatusLabel()).isEqualTo("来源不可验证");
                }));
    }

    private static UserPositionDO position() {
        UserPositionDO row = new UserPositionDO();
        row.setId(7L);
        row.setAssetSymbol("BTCUSDT");
        row.setSide("LONG");
        row.setStatus("CLOSED");
        row.setEntryPrice(new BigDecimal("100"));
        row.setClosePrice(new BigDecimal("110"));
        row.setQuantity(new BigDecimal("2"));
        row.setClosedAt(LocalDateTime.of(2026, 6, 24, 10, 0));
        return row;
    }

    private static UserPositionReviewSummaryDTO positionSummary() {
        UserPositionReviewSummaryDTO dto = new UserPositionReviewSummaryDTO();
        dto.setPositionId(7L);
        dto.setExecutionPlanId("plan-7");
        dto.setEntryZone("100-101");
        dto.setPlanStopLoss("95");
        dto.setTakeProfitRules("110/120");
        dto.setRecommendedAction("WATCH");
        dto.setGrossPnl(new BigDecimal("20"));
        dto.setExecutionDeviationStatus("ALIGNED");
        dto.setExecutionDeviationReasons(List.of("COMPARABLE_BOUNDARIES_WITHIN_TOLERANCE"));
        dto.setMonitorLogs(List.of(monitorLog()));
        dto.setReviewStatus("REVIEW_SUMMARY_READY");
        return dto;
    }

    private static PositionMonitorLogDTO monitorLog() {
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogicStatus("PLAN_VALID");
        dto.setSuggestedAction("MANUAL_REVIEW");
        return dto;
    }

    private static OpportunityLogDTO opportunity(String status) {
        OpportunityLogDTO dto = new OpportunityLogDTO();
        dto.setAnchorTime(LocalDateTime.of(2026, 6, 24, 11, 0));
        dto.setSymbol("BTCUSDT");
        dto.setOpportunityStatus(status);
        dto.setPushPresent(true);
        dto.setUserPositionPresent(true);
        dto.setMfeRatio(new BigDecimal("0.12"));
        dto.setMaeRatio(new BigDecimal("0.03"));
        return dto;
    }

    private static TmPushSnapshotDO pushSnapshot() {
        return pushSnapshotAt(LocalDateTime.of(2026, 7, 13, 12, 10));
    }

    private static TmPushSnapshotDO pushSnapshotAt(LocalDateTime expiresAt) {
        TmPushSnapshotDO row = new TmPushSnapshotDO();
        row.setPushId(3L);
        row.setSymbol("SOLUSDT");
        row.setPushType("WATCHLIST");
        row.setPushStatus("CAPTURED");
        row.setPushCreateTime(LocalDateTime.of(2026, 6, 24, 12, 0));
        row.setExpiresAt(expiresAt);
        return row;
    }

    private static TmPushRecheckLogDO pushLog() {
        TmPushRecheckLogDO row = new TmPushRecheckLogDO();
        row.setPushId(3L);
        row.setRecheckStatus("REVIEW_PASSED");
        row.setExecutionStatus("COMPLETED");
        row.setFailReasonJson("{\"code\":\"NONE\"}");
        return row;
    }

    private static ReviewResultDO reviewResult() {
        ReviewResultDO row = new ReviewResultDO();
        row.setAnalysisId("ana-rule-1");
        row.setErrorType("RULE_TOO_LOOSE");
        row.setAdjustmentSuggestion("tighten threshold");
        row.setUpdateTime(LocalDateTime.of(2026, 6, 24, 13, 0));
        return row;
    }

    private static AnalysisRunDO analysisRun() {
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId("ana-rule-1");
        row.setSymbol("ETHUSDT");
        row.setRuleVersion("rv-1");
        return row;
    }
}
