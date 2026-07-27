package org.example.trademodel.positionmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.MacroEventService;
import org.example.trademodel.service.NewsEventService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.impl.PositionMonitorServiceImpl;
import org.example.trademodel.service.support.ExecutionPlanReviewPolicy;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextImportRequest;
import org.example.trademodel.service.support.ExternalContextImportResult;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.ExternalContextSnapshot;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PositionMonitorServiceImplTest {
    private static final Long USER_ID = 17L;

    @Mock
    private UserPositionMapper userPositionMapper;
    @Mock
    private MarketQuoteClient marketQuoteClient;
    @Mock
    private UserPositionRiskAdapter userPositionRiskAdapter;
    @Mock
    private ExecutionPlanMapper executionPlanMapper;
    @Mock
    private PositionMonitorLogService positionMonitorLogService;
    @Mock
    private EvidenceItemMapper evidenceItemMapper;
    @Mock
    private ScoreItemMapper scoreItemMapper;
    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;
    @Mock
    private ExternalContextEvidenceBuilder externalContextEvidenceBuilder;

    private PositionMonitorServiceImpl service;
    private final AtomicLong logIds = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        service = new PositionMonitorServiceImpl(
                userPositionMapper,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                userPositionRiskAdapter,
                executionPlanMapper,
                positionMonitorLogService,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                new ObjectMapper(),
                analysisRunMapper,
                null);
        lenient().when(positionMonitorLogService.listByPositionIdForUser(eq(USER_ID), anyLong(), eq(1))).thenReturn(List.of());
        lenient().when(positionMonitorLogService.recordMonitorRunForUser(eq(USER_ID), any())).thenAnswer(invocation -> {
            return monitorLog(invocation.getArgument(1));
        });
        lenient().when(positionMonitorLogService.listByPositionIdForSystem(anyLong(), eq(1))).thenReturn(List.of());
        lenient().when(positionMonitorLogService.recordMonitorRunForSystem(any())).thenAnswer(invocation ->
                monitorLog(invocation.getArgument(0)));
    }

    @Test
    void longLogicValidWritesExactlyOneLogWithSafetyFields() throws Exception {
        UserPositionDO position = position(1L, "LONG", "OPEN", "plan-valid", "90", "120");
        arrange(position, "100", risk("LOW", false), plan("plan-valid", "ana-1", "VALID", true));

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(1L, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(result.getEntryLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(result.getDirectionSupportStatus()).isEqualTo("SUPPORTED");
        assertThat(result.getReversalStatus()).isEqualTo("NO_REVERSAL_SIGNAL");
        assertThat(result.getSuggestedAction()).isEqualTo("HOLD");
        assertThat(result.getSuggestedManualAction()).isEqualTo("HOLD");
        assertThat(result.getSuggestedManualActionText()).contains("人工");
        assertThat(result.isNearStopLoss()).isFalse();
        assertThat(result.isNearTakeProfit()).isFalse();
        assertThat(result.getMonitorLogId()).isNotNull();
        assertSafetyFields(result);
        assertForbiddenActionFieldsAbsent();

        ArgumentCaptor<RecordPositionMonitorLogCommand> captor = ArgumentCaptor.forClass(RecordPositionMonitorLogCommand.class);
        verify(positionMonitorLogService).recordMonitorRunForUser(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getCurrentPrice()).isEqualByComparingTo("100");
        assertThat(captor.getValue().getLogicStatus()).isEqualTo("LOGIC_VALID");
        verify(userPositionMapper, never()).manualCloseByIdAndUserId(
                anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void shortLogicValidUsesShortStopAndTakeProfitRules() {
        UserPositionDO position = position(2L, "SHORT", "OPEN", "plan-short", "110", "80");
        arrange(position, "100", risk("LOW", false), plan("plan-short", "ana-2", "VALID", true));

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(2L, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(result.getSide()).isEqualTo("SHORT");
        assertThat(result.isStopLossBreached()).isFalse();
        assertThat(result.isTakeProfitReached()).isFalse();
    }

    @Test
    void longAndShortNearStopLossAreWeakened() {
        UserPositionDO longPosition = position(3L, "LONG", "OPEN", "plan-long-near-stop", "99", "120");
        arrange(longPosition, "100", risk("LOW", false), plan("plan-long-near-stop", "ana-3", "VALID", true));
        assertThat(service.monitorUserPositionForUser(3L, USER_ID).getLogicStatus()).isEqualTo("LOGIC_WEAKENED");

        UserPositionDO shortPosition = position(4L, "SHORT", "OPEN", "plan-short-near-stop", "101", "80");
        arrange(shortPosition, "100", risk("LOW", false), plan("plan-short-near-stop", "ana-4", "VALID", true));
        PositionMonitorResultDTO result = service.monitorUserPositionForUser(4L, USER_ID);
        assertThat(result.isNearStopLoss()).isTrue();
        assertThat(result.getReasonCodes()).contains("NEAR_STOP_LOSS");
        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
    }

    @Test
    void longAndShortNearTakeProfitCanRemainValidWithManualReviewSuggestion() {
        UserPositionDO longPosition = position(5L, "LONG", "OPEN", "plan-long-near-tp", "90", "101");
        arrange(longPosition, "100", risk("LOW", false), plan("plan-long-near-tp", "ana-5", "VALID", true));
        PositionMonitorResultDTO longResult = service.monitorUserPositionForUser(5L, USER_ID);
        assertThat(longResult.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(longResult.isNearTakeProfit()).isTrue();
        assertThat(longResult.getSuggestedAction()).isEqualTo("MANUAL_REVIEW");

        UserPositionDO shortPosition = position(6L, "SHORT", "PARTIALLY_CLOSED", "plan-short-near-tp", "110", "99");
        arrange(shortPosition, "100", risk("LOW", false), plan("plan-short-near-tp", "ana-6", "VALID", true));
        PositionMonitorResultDTO shortResult = service.monitorUserPositionForUser(6L, USER_ID);
        assertThat(shortResult.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(shortResult.isNearTakeProfit()).isTrue();
        assertThat(shortResult.getPositionStatus()).isEqualTo("PARTIALLY_CLOSED");
    }

    @Test
    void stopBreachedAndPersistedInvalidPlansInvalidateWhileIncompleteGateWeakens() {
        UserPositionDO longBreached = position(7L, "LONG", "OPEN", "plan-long-breached", "100", "130");
        arrange(longBreached, "99", risk("LOW", false), plan("plan-long-breached", "ana-7", "VALID", true));
        assertThat(service.monitorUserPositionForUser(7L, USER_ID).getLogicStatus()).isEqualTo("PLAN_INVALIDATED");

        UserPositionDO shortBreached = position(8L, "SHORT", "OPEN", "plan-short-breached", "100", "80");
        arrange(shortBreached, "101", risk("LOW", false), plan("plan-short-breached", "ana-8", "VALID", true));
        assertThat(service.monitorUserPositionForUser(8L, USER_ID).getLogicStatus()).isEqualTo("PLAN_INVALIDATED");

        UserPositionDO invalidPlan = position(9L, "LONG", "OPEN", "plan-invalid", "90", "120");
        arrange(invalidPlan, "100", risk("LOW", false), plan("plan-invalid", "ana-9", "INVALID", true));
        assertThat(service.monitorUserPositionForUser(9L, USER_ID).getReasonCodes()).contains("PLAN_INVALID");

        UserPositionDO incompleteSource = position(10L, "LONG", "OPEN", "plan-source-missing", "90", "120");
        arrange(incompleteSource, "100", risk("LOW", false), plan("plan-source-missing", "ana-10", "VALID", false));
        PositionMonitorResultDTO result = service.monitorUserPositionForUser(10L, USER_ID);
        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(result.getSuggestedAction()).isEqualTo("RECHECK_PLAN");
        assertThat(result.getReasonCodes()).contains("SOURCE_GATE_INCOMPLETE");
    }

    @Test
    void needsRevalidationPlanNeverLogsLogicValid() {
        UserPositionDO position = position(101L, "LONG", "OPEN", "plan-revalidate", "90", "120");
        ExecutionPlanDO plan = plan("plan-revalidate", "ana-101", "VALID", true);
        plan.setNeedsRevalidation(true);
        plan.setRevalidationReason("HOT_RESET_REVIEW_REQUIRED");
        arrange(position, "100", risk("LOW", false), plan);

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(101L, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(result.getSuggestedAction()).isEqualTo("RECHECK_PLAN");
        assertThat(result.getReasonCodes()).contains("PLAN_REVALIDATION_REQUIRED");
    }

    @Test
    void validPlanWithMissingExactBoundaryNeverLogsLogicValid() {
        assertIncompleteBoundary(102L, null, "90", "120");
    }

    @Test
    void validPlanWithPlaceholderEntryNeverLogsLogicValid() {
        assertIncompleteBoundary(103L, "待生成", "90", "120");
    }

    @Test
    void validPlanWithPlaceholderStopNeverLogsLogicValid() {
        assertIncompleteBoundary(104L, "100-101", "—", "120");
    }

    @Test
    void validPlanWithPlaceholderTakeProfitNeverLogsLogicValid() {
        assertIncompleteBoundary(105L, "100-101", "90", "暂无");
    }

    @Test
    void placeholderBoundaryVariantsShareOnePolicy() {
        assertThat(Arrays.asList(null, "", "  ", "暂无", "—", "待生成"))
                .allSatisfy(value -> assertThat(ExecutionPlanReviewPolicy.isConcreteBoundary(value)).isFalse());
        assertThat(ExecutionPlanReviewPolicy.isConcreteBoundary("100-101")).isTrue();
    }

    @Test
    void invalidPlanStillProducesPlanInvalidated() {
        UserPositionDO position = position(106L, "LONG", "OPEN", "plan-invalid-contract", "90", "120");
        arrange(position, "100", risk("LOW", false),
                plan("plan-invalid-contract", "ana-106", "INVALID", true));

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(106L, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(result.getSuggestedAction()).isEqualTo("RECHECK_PLAN");
        assertThat(result.getReasonCodes()).contains("PLAN_INVALID");
    }

    @Test
    void blockedPlanStillProducesPlanInvalidated() {
        UserPositionDO position = position(107L, "LONG", "OPEN", "plan-blocked-contract", "90", "120");
        ExecutionPlanDO plan = plan("plan-blocked-contract", "ana-107", "BLOCKED", true);
        plan.setSourceGateStatus("BLOCKED");
        arrange(position, "100", risk("LOW", false), plan);

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(107L, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(result.getSuggestedAction()).isEqualTo("RECHECK_PLAN");
        assertThat(result.getReasonCodes()).contains("PLAN_INVALID");
    }

    @Test
    void planContextAndMissingBoundariesWeakenLogic() {
        UserPositionDO missingContext = position(11L, "LONG", "OPEN", null, "90", "120");
        arrange(missingContext, "100", risk("LOW", false), null);
        PositionMonitorResultDTO missingContextResult = service.monitorUserPositionForUser(11L, USER_ID);
        assertThat(missingContextResult.getAnalysisId()).isNull();
        assertThat(missingContextResult.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(missingContextResult.getReasonCodes())
                .contains("PLAN_SOURCE_UNVERIFIED", "PLAN_CONTEXT_MISSING");

        UserPositionDO missingStop = position(12L, "LONG", "OPEN", "plan-missing-stop", null, "120");
        arrange(missingStop, "100", risk("LOW", false), plan("plan-missing-stop", "ana-12", "VALID", true));
        assertThat(service.monitorUserPositionForUser(12L, USER_ID).getReasonCodes()).contains("STOP_LOSS_MISSING");

        UserPositionDO missingTakeProfit = position(13L, "LONG", "OPEN", "plan-missing-tp", "90", null);
        arrange(missingTakeProfit, "100", risk("LOW", false), plan("plan-missing-tp", "ana-13", "VALID", true));
        PositionMonitorResultDTO result = service.monitorUserPositionForUser(13L, USER_ID);
        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(result.getReasonCodes()).contains("TAKE_PROFIT_MISSING");
    }

    @Test
    void positionSourceRefAnalysisWithPlansAAndB_monitorNeverLogsBAsOriginalSource() {
        UserPositionDO position = position(131L, "LONG", "OPEN", null, "90", "120");
        position.setSourceRefId("analysis-with-plan-a-and-b");
        arrange(position, "100", risk("LOW", false), null);
        ExecutionPlanDO latestB = plan("plan-B", "analysis-with-plan-a-and-b", "VALID", true);
        lenient().when(executionPlanMapper.selectLatestByAnalysisId("analysis-with-plan-a-and-b"))
                .thenReturn(latestB);

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(131L, USER_ID);

        ArgumentCaptor<RecordPositionMonitorLogCommand> captor =
                ArgumentCaptor.forClass(RecordPositionMonitorLogCommand.class);
        verify(positionMonitorLogService).recordMonitorRunForUser(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getAnalysisId())
                .isEqualTo(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        assertThat(captor.getValue().getExecutionPlanId()).isNull();
        assertThat(result.getAnalysisId()).isNull();
        assertThat(result.getExecutionPlanId()).isNull();
        assertThat(result.getReasonCodes()).contains("PLAN_SOURCE_UNVERIFIED");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(any());
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(any());
        verify(executionPlanMapper, never()).selectByPlanId(any());
    }

    @Test
    void ambiguousSourceRefProducesUnverifiedMonitorSource() {
        UserPositionDO position = position(132L, "LONG", "OPEN", null, "90", "120");
        position.setSourceRefId("plan-A-without-type");
        arrange(position, "100", risk("LOW", false), null);
        lenient().when(executionPlanMapper.selectByPlanId("plan-A-without-type"))
                .thenReturn(plan("plan-A-without-type", "analysis-A", "VALID", true));

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(132L, USER_ID);

        ArgumentCaptor<RecordPositionMonitorLogCommand> captor =
                ArgumentCaptor.forClass(RecordPositionMonitorLogCommand.class);
        verify(positionMonitorLogService).recordMonitorRunForUser(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getAnalysisId())
                .isEqualTo(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        assertThat(captor.getValue().getExecutionPlanId()).isNull();
        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        verify(executionPlanMapper, never()).selectByPlanId(any());
    }

    @Test
    void typedAnalysisSourceWithSinglePlanLogsVerifiedSource() {
        UserPositionDO position = position(133L, "LONG", "OPEN", null, "90", "120");
        position.setSourceRefId(PositionMonitorSourceContract.analysisReference("analysis-A"));
        arrange(position, "100", risk("LOW", false), null);
        ExecutionPlanDO planA = plan("plan-A", "analysis-A", "VALID", true);
        when(executionPlanMapper.selectOnlyByAnalysisId("analysis-A")).thenReturn(planA);
        when(analysisRunMapper.selectById("analysis-A")).thenReturn(analysisRun("analysis-A", "BTC"));

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(133L, USER_ID);

        ArgumentCaptor<RecordPositionMonitorLogCommand> captor =
                ArgumentCaptor.forClass(RecordPositionMonitorLogCommand.class);
        verify(positionMonitorLogService).recordMonitorRunForUser(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getAnalysisId()).isEqualTo("analysis-A");
        assertThat(captor.getValue().getExecutionPlanId()).isEqualTo("plan-A");
        assertThat(result.getAnalysisId()).isEqualTo("analysis-A");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(any());
    }

    @Test
    void monitorSourceSymbolMismatchFailsClosedBeforeLogWrite() {
        UserPositionDO position = position(134L, "LONG", "OPEN", "plan-A", "90", "120");
        arrange(position, "100", risk("LOW", false), plan("plan-A", "analysis-A", "VALID", true));
        when(analysisRunMapper.selectById("analysis-A")).thenReturn(analysisRun("analysis-A", "ETH"));

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(134L, USER_ID);

        ArgumentCaptor<RecordPositionMonitorLogCommand> captor =
                ArgumentCaptor.forClass(RecordPositionMonitorLogCommand.class);
        verify(positionMonitorLogService).recordMonitorRunForUser(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getAnalysisId())
                .isEqualTo(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
        assertThat(captor.getValue().getExecutionPlanId()).isNull();
        assertThat(result.getAnalysisId()).isNull();
        assertThat(result.getReasonCodes()).contains("PLAN_SOURCE_UNVERIFIED");
    }

    @Test
    void riskBlockedAndRiskIncreasedAreFailClosed() {
        UserPositionDO highRisk = position(14L, "LONG", "OPEN", "plan-high-risk", "90", "120");
        arrange(highRisk, "100", risk("HIGH", true), plan("plan-high-risk", "ana-14", "VALID", true));
        PositionMonitorResultDTO blocked = service.monitorUserPositionForUser(14L, USER_ID);
        assertThat(blocked.getLogicStatus()).isEqualTo("HIGH_RISK");
        assertThat(blocked.getSuggestedAction()).isEqualTo("RISK_REVIEW");
        assertThat(blocked.isRiskBlocked()).isTrue();

        UserPositionDO increased = position(15L, "LONG", "OPEN", "plan-risk-up", "90", "120");
        arrange(increased, "100", risk("MEDIUM", false), plan("plan-risk-up", "ana-15", "VALID", true));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 15L, 1)).thenReturn(List.of(previousLog("LOW")));
        assertThat(service.monitorUserPositionForUser(15L, USER_ID).isRiskIncreased()).isTrue();

        UserPositionDO unchanged = position(16L, "LONG", "OPEN", "plan-risk-same", "90", "120");
        arrange(unchanged, "100", risk("MEDIUM", false), plan("plan-risk-same", "ana-16", "VALID", true));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 16L, 1)).thenReturn(List.of(previousLog("MEDIUM")));
        assertThat(service.monitorUserPositionForUser(16L, USER_ID).isRiskIncreased()).isFalse();
    }

    @Test
    void closedMissingInvalidQuoteAndQuoteUnavailableDoNotWriteLogs() {
        when(userPositionMapper.selectByIdAndUserId(17L, USER_ID)).thenReturn(position(17L, "LONG", "CLOSED", "plan-closed", "90", "120"));
        assertThatThrownBy(() -> service.monitorUserPositionForUser(17L, USER_ID))
                .isInstanceOf(UserPositionConflictException.class)
                .hasMessageContaining("cannot be monitored");

        when(userPositionMapper.selectByIdAndUserId(18L, USER_ID)).thenReturn(null);
        assertThatThrownBy(() -> service.monitorUserPositionForUser(18L, USER_ID))
                .isInstanceOf(UserPositionNotFoundException.class)
                .hasMessageContaining("UserPosition not found");

        UserPositionDO invalidQuote = position(19L, "LONG", "OPEN", "plan-invalid-price", "90", "120");
        when(userPositionMapper.selectByIdAndUserId(19L, USER_ID)).thenReturn(invalidQuote);
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.of(quote("0")));
        assertThatThrownBy(() -> service.monitorUserPositionForUser(19L, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVALID_MARKET_PRICE");

        UserPositionDO unavailableQuote = position(20L, "LONG", "OPEN", "plan-no-quote", "90", "120");
        when(userPositionMapper.selectByIdAndUserId(20L, USER_ID)).thenReturn(unavailableQuote);
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.monitorUserPositionForUser(20L, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QUOTE_UNAVAILABLE");

        verify(positionMonitorLogService, never()).recordMonitorRunForUser(anyLong(), any());
    }

    @Test
    void batchMonitorsOnlyActivePositionsAndReportsIndividualFailures() {
        UserPositionDO open = position(21L, "LONG", "OPEN", "plan-batch-open", "90", "120");
        UserPositionDO partial = position(22L, "SHORT", "PARTIALLY_CLOSED", "plan-batch-partial", "110", "80");
        when(userPositionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(open, partial));
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.of(quote("100")));
        when(userPositionRiskAdapter.currentRiskForUser(USER_ID)).thenReturn(risk("LOW", false));
        when(executionPlanMapper.selectByPlanId("plan-batch-open"))
                .thenReturn(plan("plan-batch-open", "ana-21", "VALID", true));
        when(marketQuoteClient.fetch24hTicker("ETH")).thenReturn(Optional.empty());

        PositionMonitorBatchResultDTO batch = service.monitorClaimedOpenPositionsForSystem();

        assertThat(batch.getTotalCount()).isEqualTo(2);
        assertThat(batch.getSuccessCount()).isEqualTo(1);
        assertThat(batch.getFailureCount()).isEqualTo(1);
        assertThat(batch.getResults()).extracting(PositionMonitorResultDTO::getPositionId).containsExactly(21L);
        assertThat(batch.getFailures()).hasSize(1);
        verify(positionMonitorLogService).recordMonitorRunForSystem(any());
    }

    @Test
    void systemBatchCalculatesAndPersistsRiskWithinEachPositionOwnerScope() {
        UserPositionDO ownerA = position(23L, "LONG", "OPEN", "plan-owner-a", "90", "120");
        ownerA.setUserId(101L);
        UserPositionDO ownerB = position(24L, "LONG", "OPEN", "plan-owner-b", "90", "120");
        ownerB.setUserId(202L);
        ownerB.setAssetSymbol("ETH");
        UserPositionRiskResult riskA = risk("LOW", false);
        riskA.setAggregateRiskScore(new BigDecimal("11"));
        UserPositionRiskResult riskB = risk("HIGH", true);
        riskB.setAggregateRiskScore(new BigDecimal("88"));

        when(userPositionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(ownerA, ownerB));
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.of(quote("100")));
        when(marketQuoteClient.fetch24hTicker("ETH")).thenReturn(Optional.of(quote("100")));
        when(userPositionRiskAdapter.currentRiskForUser(101L)).thenReturn(riskA);
        when(userPositionRiskAdapter.currentRiskForUser(202L)).thenReturn(riskB);

        PositionMonitorBatchResultDTO batch = service.monitorClaimedOpenPositionsForSystem();

        assertThat(batch.getSuccessCount()).isEqualTo(2);
        ArgumentCaptor<RecordPositionMonitorLogCommand> captor =
                ArgumentCaptor.forClass(RecordPositionMonitorLogCommand.class);
        verify(positionMonitorLogService, times(2)).recordMonitorRunForSystem(captor.capture());
        assertThat(captor.getAllValues().get(0).getPositionId()).isEqualTo(23L);
        assertThat(captor.getAllValues().get(0).getRiskSnapshot())
                .contains("\"aggregateRiskScore\":11");
        assertThat(captor.getAllValues().get(1).getPositionId()).isEqualTo(24L);
        assertThat(captor.getAllValues().get(1).getRiskSnapshot())
                .contains("\"aggregateRiskScore\":88");
        verify(userPositionRiskAdapter, never()).currentRiskForSystem();
    }

    @Test
    void closedConflictIsRecordedPerItemAndDoesNotStopSystemBatch() {
        UserPositionDO staleClosed = position(25L, "LONG", "OPEN", "plan-stale", "90", "120");
        staleClosed.setUserId(101L);
        UserPositionDO next = position(26L, "LONG", "OPEN", "plan-next", "90", "120");
        next.setUserId(202L);
        next.setAssetSymbol("ETH");
        when(userPositionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(staleClosed, next));
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.of(quote("100")));
        when(marketQuoteClient.fetch24hTicker("ETH")).thenReturn(Optional.of(quote("100")));
        when(userPositionRiskAdapter.currentRiskForUser(101L)).thenReturn(risk("LOW", false));
        when(userPositionRiskAdapter.currentRiskForUser(202L)).thenReturn(risk("LOW", false));
        doThrow(new UserPositionConflictException("CLOSED UserPosition cannot record new monitor run logs"))
                .doAnswer(invocation -> monitorLog(invocation.getArgument(0)))
                .when(positionMonitorLogService).recordMonitorRunForSystem(any());

        PositionMonitorBatchResultDTO batch = service.monitorClaimedOpenPositionsForSystem();

        assertThat(batch.getTotalCount()).isEqualTo(2);
        assertThat(batch.getSuccessCount()).isEqualTo(1);
        assertThat(batch.getFailureCount()).isEqualTo(1);
        assertThat(batch.getFailures()).singleElement().satisfies(failure -> {
            assertThat(failure.getPositionId()).isEqualTo(25L);
            assertThat(failure.getReason()).contains("CLOSED UserPosition");
        });
        assertThat(batch.getResults()).extracting(PositionMonitorResultDTO::getPositionId)
                .containsExactly(26L);
        verify(positionMonitorLogService, times(2)).recordMonitorRunForSystem(any());
    }

    @Test
    void unexpectedMonitorFailureStopsSystemBatch() {
        UserPositionDO position = position(27L, "LONG", "OPEN", "plan-runtime-failure", "90", "120");
        position.setUserId(101L);
        when(userPositionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(position));
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.of(quote("100")));
        when(userPositionRiskAdapter.currentRiskForUser(101L)).thenReturn(risk("LOW", false));
        doThrow(new IllegalStateException("PositionMonitorLog insert failed"))
                .when(positionMonitorLogService).recordMonitorRunForSystem(any());

        assertThatThrownBy(service::monitorClaimedOpenPositionsForSystem)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PositionMonitorLog insert failed");
    }

    @Test
    void activeBlockingExternalContextMakesHighRiskReviewWithoutPositionMutation() {
        service = new PositionMonitorServiceImpl(
                userPositionMapper,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                userPositionRiskAdapter,
                executionPlanMapper,
                positionMonitorLogService,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                new ObjectMapper(),
                analysisRunMapper,
                externalContextEvidenceBuilder);
        UserPositionDO position = position(30L, "LONG", "OPEN", "plan-external-block", "90", "120");
        arrange(position, "100", risk("LOW", false), plan("plan-external-block", "ana-30", "VALID", true));
        ExternalContextSnapshot snapshot = new ExternalContextSnapshot();
        snapshot.setStatus("BLOCKED");
        snapshot.setRiskLevel("HIGH");
        snapshot.setExternalContextBlocked(true);
        snapshot.setSourceHealth(ExternalContextPolicy.SOURCE_HEALTH_OK);
        snapshot.setActiveExternalEventCount(1);
        snapshot.setActiveNewsEventCount(1);
        snapshot.addEventId("NEWS:major-event");
        snapshot.addReason(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        when(externalContextEvidenceBuilder.buildSnapshot(eq("ana-30"), eq("BTC"), eq(null), any(), eq(null)))
                .thenReturn(snapshot);

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(30L, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("HIGH_RISK");
        assertThat(result.getSuggestedAction()).isEqualTo("RISK_REVIEW");
        assertThat(result.getExternalContextBlocked()).isTrue();
        assertThat(result.getReasonCodes()).contains(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        assertSafetyFields(result);
        verify(userPositionMapper, never()).manualCloseByIdAndUserId(
                anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void crossMarketExternalContextDoesNotEscalatePositionMonitorRiskWhenMarketScopeMissing() {
        LocalDateTime now = LocalDateTime.now();
        MacroEventDO equitiesEvent = macroEvent("macro-position-equities", now);
        equitiesEvent.setAffectedSymbols(null);
        equitiesEvent.setMarketScope("EQUITIES");
        ExternalContextEvidenceBuilder scopedBuilder = new ExternalContextEvidenceBuilder(
                macroService(List.of(equitiesEvent)), newsService(List.of()));
        service = new PositionMonitorServiceImpl(
                userPositionMapper,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                userPositionRiskAdapter,
                executionPlanMapper,
                positionMonitorLogService,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                new ObjectMapper(),
                analysisRunMapper,
                scopedBuilder);
        UserPositionDO position = position(31L, "LONG", "OPEN", "plan-cross-market", "90", "120");
        arrange(position, "100", risk("LOW", false), plan("plan-cross-market", "ana-31", "VALID", true));

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(31L, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(result.getExternalContextBlocked()).isFalse();
        assertThat(result.getExternalEventIds()).isEmpty();
        assertThat(result.getReasonCodes()).doesNotContain(
                ExternalContextPolicy.REASON_WINDOW_BLOCKED,
                "EXTERNAL_CONTEXT_REVIEW_REQUIRED");
        verify(userPositionMapper, never()).manualCloseByIdAndUserId(
                anyLong(), anyLong(), any(), any(), any(), any());
    }

    private void arrange(UserPositionDO position,
                         String currentPrice,
                         UserPositionRiskResult risk,
                         ExecutionPlanDO plan) {
        when(userPositionMapper.selectByIdAndUserId(position.getId(), USER_ID)).thenReturn(position);
        when(marketQuoteClient.fetch24hTicker(position.getAssetSymbol())).thenReturn(Optional.of(quote(currentPrice)));
        when(userPositionRiskAdapter.currentRiskForUser(USER_ID)).thenReturn(risk);
        if (plan != null) {
            when(executionPlanMapper.selectByPlanId(plan.getPlanId())).thenReturn(plan);
            when(analysisRunMapper.selectById(plan.getAnalysisId()))
                    .thenReturn(analysisRun(plan.getAnalysisId(), position.getAssetSymbol()));
        }
    }

    private static UserPositionDO position(Long id,
                                           String side,
                                           String status,
                                           String sourceRefId,
                                           String stopLoss,
                                           String takeProfit) {
        UserPositionDO row = new UserPositionDO();
        row.setId(id);
        row.setUserId(USER_ID);
        row.setAssetSymbol(id == 22L ? "ETH" : "BTC");
        row.setSide(side);
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100"));
        row.setQuantity(new BigDecimal("1"));
        row.setLeverage(new BigDecimal("2"));
        row.setStopLoss(stopLoss == null ? null : new BigDecimal(stopLoss));
        row.setTakeProfit(takeProfit == null ? null : new BigDecimal(takeProfit));
        row.setSourceType("MANUAL");
        row.setSourceRefId(sourceRefId == null
                ? null
                : PositionMonitorSourceContract.executionPlanReference(sourceRefId));
        return row;
    }

    private PositionMonitorLogDTO monitorLog(RecordPositionMonitorLogCommand command) {
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogId(logIds.incrementAndGet());
        dto.setPositionId(command.getPositionId());
        dto.setAnalysisId(command.getAnalysisId());
        dto.setExecutionPlanId(command.getExecutionPlanId());
        dto.setCurrentPrice(command.getCurrentPrice());
        dto.setLogicStatus(command.getLogicStatus());
        dto.setRiskLevel(command.getRiskLevel());
        dto.setSuggestedAction(command.getSuggestedAction());
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    private static AnalysisRunDO analysisRun(String analysisId, String symbol) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        run.setTraceId("trace-" + analysisId);
        return run;
    }

    private static MarketQuoteSnapshot quote(String price) {
        MarketQuoteSnapshot snapshot = new MarketQuoteSnapshot();
        snapshot.setProvider("mock");
        snapshot.setSymbolNormalized("BTCUSDT");
        snapshot.setLastPrice(new BigDecimal(price));
        return snapshot;
    }

    private static UserPositionRiskResult risk(String riskLevel, boolean blocked) {
        UserPositionRiskResult result = new UserPositionRiskResult();
        result.setRiskStatus(blocked ? "RISK_BLOCKED" : "RISK_ALLOWED");
        result.setRiskLevel(riskLevel);
        result.setRiskBlocked(blocked);
        result.setReasonCodes(List.of(blocked ? "RISK_BLOCKED" : "RISK_ALLOWED"));
        return result;
    }

    private static ExecutionPlanDO plan(String planId, String analysisId, String status, boolean sourceGateComplete) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(planId);
        plan.setAnalysisId(analysisId);
        plan.setExecutionPlanStatus(status);
        plan.setSourceGateStatus(sourceGateComplete ? "VALID" : "INCOMPLETE");
        plan.setSourceGateComplete(sourceGateComplete);
        plan.setEntryZone("100-101");
        plan.setStopLoss("90");
        plan.setTakeProfitRules("120");
        return plan;
    }

    private void assertIncompleteBoundary(Long positionId,
                                          String entryZone,
                                          String stopLoss,
                                          String takeProfitRules) {
        String planId = "plan-boundary-" + positionId;
        String analysisId = "ana-boundary-" + positionId;
        UserPositionDO position = position(positionId, "LONG", "OPEN", planId, "90", "120");
        ExecutionPlanDO plan = plan(planId, analysisId, "VALID", true);
        plan.setEntryZone(entryZone);
        plan.setStopLoss(stopLoss);
        plan.setTakeProfitRules(takeProfitRules);
        arrange(position, "100", risk("LOW", false), plan);

        PositionMonitorResultDTO result = service.monitorUserPositionForUser(positionId, USER_ID);

        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(result.getSuggestedAction()).isEqualTo("RECHECK_PLAN");
        assertThat(result.getReasonCodes()).contains("PLAN_BOUNDARY_INCOMPLETE");
    }

    private static PositionMonitorLogDTO previousLog(String riskLevel) {
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogId(1L);
        dto.setRiskLevel(riskLevel);
        return dto;
    }

    private static MacroEventDO macroEvent(String id, LocalDateTime now) {
        MacroEventDO event = new MacroEventDO();
        event.setEventId(id);
        event.setAffectedSymbols("BTCUSDT");
        event.setMarketScope("CRYPTO");
        event.setEventTime(now.minusMinutes(1));
        event.setWindowStart(now.minusMinutes(5));
        event.setWindowEnd(now.plusMinutes(30));
        event.setImpactScore(95);
        event.setSeverity("CRITICAL");
        event.setDirection("NEUTRAL");
        event.setProvider("UNIT_TEST_PROVIDER");
        event.setSourceType("CALENDAR");
        event.setSourceReference("unit://" + id);
        event.setSourceTraceId("trace-" + id);
        event.setSourceEventId("source-" + id);
        event.setSourcePublishedAt(now.minusMinutes(10));
        event.setExecutionBlocking(true);
        event.setEventType("RATE_DECISION");
        event.setTitle("Macro " + id);
        event.setStatus("ACTIVE");
        return event;
    }

    private static MacroEventService macroService(List<MacroEventDO> events) {
        return new MacroEventService() {
            public ExternalContextImportResult<MacroEventDO> importEvent(ExternalContextImportRequest request) { return null; }
            public MacroEventDO findByEventId(String eventId) { return null; }
            public List<MacroEventDO> listRecent(int limit) { return events; }
            public List<MacroEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime) { return events; }
        };
    }

    private static NewsEventService newsService(List<NewsEventDO> events) {
        return new NewsEventService() {
            public ExternalContextImportResult<NewsEventDO> importEvent(ExternalContextImportRequest request) { return null; }
            public NewsEventDO findByEventId(String eventId) { return null; }
            public List<NewsEventDO> listRecent(int limit) { return events; }
            public List<NewsEventDO> findWindowCandidates(String symbol, String marketScope, LocalDateTime contextTime) { return events; }
        };
    }

    private static void assertSafetyFields(PositionMonitorResultDTO dto) {
        assertThat(dto.isReviewOnly()).isTrue();
        assertThat(dto.isManualReviewOnly()).isTrue();
        assertThat(dto.isNotTradeInstruction()).isTrue();
        assertThat(dto.isNotExecutable()).isTrue();
        assertThat(dto.isNotAutoReduce()).isTrue();
        assertThat(dto.isNotAutoClose()).isTrue();
        assertThat(dto.isNotAutoReverse()).isTrue();
        assertThat(dto.isNotOrderExecution()).isTrue();
        assertThat(dto.isNotAutoTrading()).isTrue();
        assertThat(dto.isNotPositionMutation()).isTrue();
    }

    private static void assertForbiddenActionFieldsAbsent() throws Exception {
        Set<String> propertyNames = Arrays.stream(Introspector.getBeanInfo(PositionMonitorResultDTO.class).getPropertyDescriptors())
                .map(descriptor -> descriptor.getName())
                .collect(Collectors.toSet());
        assertThat(propertyNames).doesNotContain(
                "reduceAction", "closeAction", "reverseAction", "orderAction", "executionAction",
                "autoTradingAction", "executablePayload", "providerPayload");
    }
}
