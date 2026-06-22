package org.example.trademodel.service.impl;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.HotResetEventTypeEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.ConfusedResult;
import org.example.trademodel.service.ConfusedStateService;
import org.example.trademodel.service.DecisionContext;
import org.example.trademodel.service.HotResetCommand;
import org.example.trademodel.service.HotResetResult;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotResetServiceImplTest {

    @Mock private AssetStateMapper assetStateMapper;
    @Mock private HotResetEventMapper hotResetEventMapper;
    @Mock private DecisionResultMapper decisionResultMapper;
    @Mock private ExecutionPlanMapper executionPlanMapper;
    @Mock private PushSnapshotMapper pushSnapshotMapper;
    @Mock private ConfusedStateService confusedStateService;
    @Mock private UserPositionRiskAdapter userPositionRiskAdapter;
    @Mock private ObjectProvider<AnalysisSchedulerService> schedulerProvider;
    @Mock private AnalysisSchedulerService scheduler;

    private HotResetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HotResetServiceImpl(assetStateMapper, hotResetEventMapper, decisionResultMapper,
                executionPlanMapper, pushSnapshotMapper, confusedStateService, userPositionRiskAdapter, schedulerProvider);
        lenient().when(schedulerProvider.getIfAvailable()).thenReturn(scheduler);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void nonExtremeEventDoesNotModifyBusinessDataOrTriggerRebuild() {
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        command.setPriceMoveRatio(new BigDecimal("0.01"));

        HotResetResult result = service.evaluateAndExecute(command);

        assertThat(result.isTriggered()).isFalse();
        verify(assetStateMapper, never()).mergeUpsertCore(any());
        verify(decisionResultMapper, never()).markHotResetInvalidatedBySymbol(anyString(), anyString(), anyString(), any());
        verify(scheduler, never()).executeAnalysis(anyString(), anyString(), anyString());
    }

    @Test
    void candidateWaitingAndTriggeredStatesAreReplacedBySafePostState() {
        assertUnsafePreStateInvalidates(AssetStateEnum.CANDIDATE);
        assertUnsafePreStateInvalidates(AssetStateEnum.WAITING_TRIGGER);
        assertUnsafePreStateInvalidates(AssetStateEnum.TRIGGERED);
    }

    @Test
    void riskBlockedForcesHighRiskPathAndRecordsRiskSnapshot() {
        HotResetCommand command = command(HotResetEventTypeEnum.SYSTEMIC_SHOCK);
        command.setSystemicShock(true);
        command.setSeverityScore(90);
        command.setSourceType("SYSTEMIC_SOURCE");
        command.setSourceReference("shock-feed");
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CANDIDATE, 20, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(90, "CANDIDATE", "CONFUSED", true, false, 0, true, "shock", "enter"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.failClosed("AGGREGATE_HIGH_RISK"));
        when(decisionResultMapper.markHotResetInvalidatedBySymbol(anyString(), anyString(), anyString(), any())).thenReturn(1);
        when(executionPlanMapper.markNeedsRevalidationForHotReset(any(), anyString(), anyString(), anyString(), any())).thenReturn(1);
        when(pushSnapshotMapper.invalidatePendingBySymbolForHotReset(anyString())).thenReturn(1);
        when(scheduler.executeAnalysis(anyString(), anyString(), anyString())).thenReturn(ApiResponse.success(rebuilt("ana-rebuild")));

        HotResetResult result = service.evaluateAndExecute(command);

        assertThat(result.getPostState()).isEqualTo(AssetStateEnum.HIGH_RISK.name());
        assertThat(result.isAccountRiskBlocked()).isTrue();
        ArgumentCaptor<HotResetEventDO> eventCaptor = ArgumentCaptor.forClass(HotResetEventDO.class);
        verify(hotResetEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAccountRiskSnapshot()).contains("AGGREGATE_HIGH_RISK");
    }

    @Test
    void oiCollapseCanMoveToConfusedAndLiquidityDrainCanMoveToCooling() {
        HotResetCommand oi = command(HotResetEventTypeEnum.OI_COLLAPSE);
        oi.setOpenInterestChangeRatio(new BigDecimal("-0.31"));
        oi.setSourceType("OI_SOURCE");
        oi.setSourceReference("openInterestDelta");
        assertPostStateFor(oi, AssetStateEnum.CONFUSED, new ConfusedResult(75, "OBSERVING", "OBSERVING",
                false, false, 0, false, "oi", "base"));

        HotResetCommand liquidity = command(HotResetEventTypeEnum.LIQUIDITY_DRAIN);
        liquidity.setLiquidityChangeRatio(new BigDecimal("-0.41"));
        liquidity.setSourceType("LIQUIDITY_SOURCE");
        liquidity.setSourceReference("liquidityChangeRatio");
        assertPostStateFor(liquidity, AssetStateEnum.COOLING, new ConfusedResult(54, "CONFUSED", "COOLING",
                false, true, 0, false, "liquidity", "exit"));
    }

    @Test
    void executionPlanDecisionAndPendingPushAreInvalidatedWithHotResetEventId() {
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        command.setPriceMoveRatio(new BigDecimal("0.10"));
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CANDIDATE, 10, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(80, "CANDIDATE", "OBSERVING", false, false, 0, false, "price", "base"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        when(decisionResultMapper.markHotResetInvalidatedBySymbol(anyString(), anyString(), anyString(), any())).thenReturn(2);
        when(executionPlanMapper.markNeedsRevalidationForHotReset(any(), anyString(), anyString(), anyString(), any())).thenReturn(3);
        when(pushSnapshotMapper.invalidatePendingBySymbolForHotReset(anyString())).thenReturn(4);
        when(scheduler.executeAnalysis(anyString(), anyString(), anyString())).thenReturn(ApiResponse.success(rebuilt("ana-rebuild")));

        HotResetResult result = service.evaluateAndExecute(command);

        assertThat(result.getDecisionInvalidatedCount()).isEqualTo(2);
        assertThat(result.getPlanRevalidationCount()).isEqualTo(3);
        assertThat(result.getPushInvalidatedCount()).isEqualTo(4);
        verify(decisionResultMapper).markHotResetInvalidatedBySymbol(anyString(), anyString(), anyString(), any());
        verify(executionPlanMapper).markNeedsRevalidationForHotReset(any(), anyString(), anyString(), anyString(), any());
        verify(pushSnapshotMapper).invalidatePendingBySymbolForHotReset("BTCUSDT");
    }

    @Test
    void duplicateEventKeyReturnsDeduplicatedAndDoesNotRepeatActionsOrRebuild() {
        HotResetEventDO existing = new HotResetEventDO();
        existing.setEventId("hre-existing");
        existing.setEventKey("event-key-EXTREME_PRICE_MOVE");
        existing.setTriggerType("EXTREME_PRICE_MOVE");
        existing.setSymbol("BTCUSDT");
        existing.setExecutionStatus("COMPLETED");
        when(hotResetEventMapper.selectByEventKey("event-key-EXTREME_PRICE_MOVE")).thenReturn(existing);

        HotResetResult result = service.evaluateAndExecute(command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE));

        assertThat(result.isDeduplicated()).isTrue();
        verify(assetStateMapper, never()).mergeUpsertCore(any());
        verify(scheduler, never()).executeAnalysis(anyString(), anyString(), anyString());
    }

    @Test
    void rebuildIsTriggeredAfterCommitAndRollbackDoesNotTrigger() {
        TransactionSynchronizationManager.initSynchronization();
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CANDIDATE, 10, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(80, "CANDIDATE", "OBSERVING", false, false, 0, false, "price", "base"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));

        service.evaluateAndExecute(command);

        verify(scheduler, never()).executeAnalysis(anyString(), anyString(), anyString());
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        TransactionSynchronizationManager.clearSynchronization();
        verify(scheduler, never()).executeAnalysis(anyString(), anyString(), anyString());
    }

    @Test
    void rebuildSuccessAndFailureUpdateEventOutcomeWithoutRestoringOldPlan() {
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CANDIDATE, 10, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(80, "CANDIDATE", "OBSERVING", false, false, 0, false, "price", "base"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        when(scheduler.executeAnalysis(anyString(), anyString(), anyString())).thenReturn(ApiResponse.success(rebuilt("ana-rebuild")));

        HotResetResult success = service.evaluateAndExecute(command);

        assertThat(success.getRebuildAnalysisId()).isEqualTo("ana-rebuild");
        verify(executionPlanMapper).markNeedsRevalidationForHotReset(any(), anyString(), anyString(), anyString(), any());
        verify(hotResetEventMapper).updateRebuildOutcome(any(HotResetEventDO.class));
    }

    @Test
    void resultSafetyFieldsStayReviewOnlyAndExposeNoExecutableActionFields() {
        HotResetResult result = new HotResetResult();

        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
        assertThat(result.isNotUserPositionMutation()).isTrue();
        assertThat(result.isNotAutoClose()).isTrue();
        assertThat(result.isNotAutoReverse()).isTrue();
        assertThat(HotResetResult.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("openAction", "closeAction", "reduceAction", "reverseAction",
                        "orderAction", "executionAction", "autoTradingAction", "executablePayload", "providerPayload");
    }

    private void assertUnsafePreStateInvalidates(AssetStateEnum preState) {
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(preState, 10, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(80, preState.name(), "OBSERVING", false, false, 0, false, "price", "base"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        when(scheduler.executeAnalysis(anyString(), anyString(), anyString())).thenReturn(ApiResponse.success(rebuilt("ana-rebuild")));

        HotResetResult result = service.evaluateAndExecute(command);

        assertThat(result.getPreState()).isEqualTo(preState.name());
        assertThat(result.getPostState()).isEqualTo(AssetStateEnum.INVALIDATED.name());
        assertThat(result.getPostState()).isNotIn("CANDIDATE", "WAITING_TRIGGER", "TRIGGERED");
    }

    private void assertPostStateFor(HotResetCommand command, AssetStateEnum expected, ConfusedResult confusedResult) {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 54, 1));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class))).thenReturn(confusedResult);
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        when(scheduler.executeAnalysis(anyString(), anyString(), anyString())).thenReturn(ApiResponse.success(rebuilt("ana-rebuild")));

        HotResetResult result = service.evaluateAndExecute(command);

        assertThat(result.getPostState()).isEqualTo(expected.name());
    }

    private static HotResetCommand command(HotResetEventTypeEnum eventType) {
        HotResetCommand command = new HotResetCommand();
        command.setEventKey("event-key-" + eventType.name());
        command.setAnalysisId("ana-test");
        command.setTraceId("trace-test");
        command.setSymbol("BTCUSDT");
        command.setTimeframe("1m");
        command.setEventType(eventType);
        command.setOccurredAt(LocalDateTime.now());
        command.setPriceMoveRatio(new BigDecimal("0.10"));
        command.setSourceType("TEST_SOURCE");
        command.setSourceReference("test-reference");
        command.setSeverityScore(80);
        DecisionContext context = new DecisionContext();
        context.setDriverConflictScore(80);
        context.setExecutionInstabilityScore(80);
        context.setMicrostructureTrapScore(80);
        context.setCauseEffectDivergenceScore(80);
        context.setAiConflictScore(80);
        command.setDecisionContext(context);
        return command;
    }

    private static AssetStateDO row(AssetStateEnum state, int confusedScore, int lowStreak) {
        AssetStateDO row = new AssetStateDO();
        row.setState(state);
        row.setConfusedScore(confusedScore);
        row.setConfusedLowStreak(lowStreak);
        return row;
    }

    private static AssetAnalysisVO rebuilt(String analysisId) {
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId(analysisId);
        return analysis;
    }
}
