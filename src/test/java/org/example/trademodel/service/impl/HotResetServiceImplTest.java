package org.example.trademodel.service.impl;

import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
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
import org.example.trademodel.service.HotResetPolicy;
import org.example.trademodel.service.HotResetResult;
import org.example.trademodel.service.support.RuleConfigContractService;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotResetServiceImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-14T12:00:00Z");
    private static final LocalDateTime FIXED_UTC = LocalDateTime.parse("2026-07-14T12:00:00");

    @Mock private AssetStateMapper assetStateMapper;
    @Mock private HotResetEventMapper hotResetEventMapper;
    @Mock private DecisionResultMapper decisionResultMapper;
    @Mock private ExecutionPlanMapper executionPlanMapper;
    @Mock private PushSnapshotMapper pushSnapshotMapper;
    @Mock private ConfusedStateService confusedStateService;
    @Mock private UserPositionRiskAdapter userPositionRiskAdapter;
    @Mock private ObjectProvider<AnalysisSchedulerService> schedulerProvider;
    @Mock private AnalysisSchedulerService scheduler;
    @Mock private RuleConfigContractService ruleConfigContractService;

    private HotResetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HotResetServiceImpl(assetStateMapper, hotResetEventMapper, decisionResultMapper,
                executionPlanMapper, pushSnapshotMapper, confusedStateService, userPositionRiskAdapter,
                schedulerProvider, ruleConfigContractService);
        service.setClock(Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        lenient().when(schedulerProvider.getIfAvailable()).thenReturn(scheduler);
        lenient().when(ruleConfigContractService.requireHotResetThresholds()).thenReturn(thresholds());
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
        verify(scheduler, never()).runHotResetRebuild(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void hotResetConfigUnavailableFailsClosedWithoutBusinessWrites() {
        when(ruleConfigContractService.requireHotResetThresholds()).thenThrow(new IllegalStateException("missing config"));
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        command.setPriceMoveRatio(new BigDecimal("0.20"));

        HotResetResult result = service.evaluateAndExecute(command);

        assertThat(result.isTriggered()).isFalse();
        assertThat(result.getReasonCodes()).contains("HOT_RESET_CONFIG_NOT_READY");
        verify(assetStateMapper, never()).mergeUpsertCore(any());
        verify(hotResetEventMapper, never()).insert(any());
        verify(scheduler, never()).runHotResetRebuild(anyString(), anyString(), anyString(), any(), any());
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
        whenRebuildReturns(executed("ana-rebuild", false, false));

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
        whenRebuildReturns(executed("ana-rebuild", false, false));

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
        verify(scheduler, never()).runHotResetRebuild(anyString(), anyString(), anyString(), any(), any());
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

        verify(scheduler, never()).runHotResetRebuild(anyString(), anyString(), anyString(), any(), any());
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        TransactionSynchronizationManager.clearSynchronization();
        verify(scheduler, never()).runHotResetRebuild(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void rebuildSuccessAndFailureUpdateEventOutcomeWithoutRestoringOldPlan() {
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CANDIDATE, 10, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(80, "CANDIDATE", "OBSERVING", false, false, 0, false, "price", "base"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        whenRebuildReturns(executed("ana-rebuild", false, false));

        HotResetResult success = service.evaluateAndExecute(command);

        assertThat(success.getRebuildAnalysisId()).isEqualTo("ana-rebuild");
        assertThat(success.getExecutionStatus()).isEqualTo("COMPLETED");
        verify(scheduler).runHotResetRebuild(eq("BTCUSDT"), eq("1m"), anyString(), eq("ana-test"), eq("trace-test"));
        verify(executionPlanMapper).markNeedsRevalidationForHotReset(any(), anyString(), anyString(), anyString(), any());
        verify(hotResetEventMapper).updateRebuildOutcome(any(HotResetEventDO.class));
    }

    @Test
    void recoveredRebuildExecutionIsCompleted() {
        mockTriggeredPath();
        whenRebuildReturns(executed("ana-recovered", true, false));

        HotResetResult result = service.evaluateAndExecute(command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE));

        assertThat(result.getExecutionStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRebuildAnalysisId()).isEqualTo("ana-recovered");
        assertRebuildOutcome("COMPLETED", "ana-recovered", null, null);
    }

    @Test
    void duplicateSuccessReusesExistingSuccessfulAnalysisWithoutCompatibilityExecution() {
        mockTriggeredPath();
        whenRebuildReturns(AnalysisRunResult.duplicateSuccess(run("ana-existing-success", "SUCCESS")));

        HotResetResult result = service.evaluateAndExecute(command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE));

        assertThat(result.getExecutionStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRebuildAnalysisId()).isEqualTo("ana-existing-success");
        verify(scheduler, never()).executeAnalysis(anyString(), anyString(), anyString());
        assertRebuildOutcome("COMPLETED", "ana-existing-success", null, null);
    }

    @Test
    void failedRebuildIsRecordedAsFailedWithoutRebuildAnalysisIdAndRedactsMessage() {
        mockTriggeredPath();
        whenRebuildReturns(AnalysisRunResult.failed(run("ana-failed", "STARTED"),
                "Authorization: Bearer SECRET https://api.example.test/path?api_key=SECRET&x=1 token=SECRET"));

        HotResetResult result = service.evaluateAndExecute(command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE));

        assertThat(result.getExecutionStatus()).isEqualTo("REBUILD_FAILED");
        assertThat(result.getRebuildAnalysisId()).isNull();
        HotResetEventDO update = assertRebuildOutcome("REBUILD_FAILED", null,
                "ANALYSIS_EXECUTION_FAILED", null);
        assertThat(update.getExecutionErrorMessage()).contains("<redacted>");
        assertThat(update.getExecutionErrorMessage()).doesNotContain("Bearer SECRET");
        assertThat(update.getExecutionErrorMessage()).doesNotContain("api_key=SECRET");
        assertThat(update.getExecutionErrorMessage()).doesNotContain("token=SECRET");
    }

    @Test
    void concurrentRebuildBlockedIsNotMarkedCompleted() {
        mockTriggeredPath();
        whenRebuildReturns(AnalysisRunResult.inProgress(run("ana-in-progress", "STARTED")));

        HotResetResult result = service.evaluateAndExecute(command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE));

        assertThat(result.getExecutionStatus()).isEqualTo("REBUILD_FAILED");
        assertThat(result.getRebuildAnalysisId()).isNull();
        assertRebuildOutcome("REBUILD_FAILED", null, "IDEMPOTENCY_IN_PROGRESS", "CONCURRENT_TRIGGER_BLOCKED");
    }

    @Test
    void recoveryBlockedRebuildIsNotMarkedCompleted() {
        mockTriggeredPath();
        whenRebuildReturns(AnalysisRunResult.recoveryBlocked(run("ana-recovery-blocked", "FAILED"),
                "PARTIAL_STATE_RECOVERY_BLOCKED", "downstream analysis rows exist; recovery is blocked"));

        HotResetResult result = service.evaluateAndExecute(command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE));

        assertThat(result.getExecutionStatus()).isEqualTo("REBUILD_FAILED");
        assertThat(result.getRebuildAnalysisId()).isNull();
        assertRebuildOutcome("REBUILD_FAILED", null, "PARTIAL_STATE_RECOVERY_BLOCKED", "RECOVERY_BLOCKED");
    }

    @Test
    void maxAttemptsExceededRebuildIsNotMarkedCompleted() {
        mockTriggeredPath();
        whenRebuildReturns(AnalysisRunResult.maxAttempts(run("ana-max-attempts", "FAILED")));

        HotResetResult result = service.evaluateAndExecute(command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE));

        assertThat(result.getExecutionStatus()).isEqualTo("REBUILD_FAILED");
        assertThat(result.getRebuildAnalysisId()).isNull();
        assertRebuildOutcome("REBUILD_FAILED", null, "MAX_RECOVERY_ATTEMPTS_EXCEEDED", "RECOVERY_BLOCKED_MAX_ATTEMPTS");
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

    @Test
    void hotResetFallbackOccurredAtIsUtcAcrossJvmZones() {
        mockTriggeredPath();
        whenRebuildReturns(executed("ana-rebuild", false, false));
        TimeZone original = TimeZone.getDefault();
        List<HotResetResult> results = new ArrayList<>();

        try {
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
                command.setEventKey("event-key-" + zone);
                command.setOccurredAt(null);
                results.add(service.evaluateAndExecute(command));
            }
        } finally {
            TimeZone.setDefault(original);
        }

        ArgumentCaptor<HotResetEventDO> events = ArgumentCaptor.forClass(HotResetEventDO.class);
        verify(hotResetEventMapper, times(3)).insert(events.capture());
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.getEventTime()).isEqualTo(FIXED_UTC);
            assertThat(event.getCreateTime()).isEqualTo(FIXED_UTC);
            assertThat(event.getCompletedAt()).isEqualTo(FIXED_UTC);
        });
        assertThat(results).allSatisfy(result -> {
            assertThat(result.getOccurredAt()).isEqualTo(FIXED_UTC);
            assertThat(result.getCompletedAt()).isEqualTo(FIXED_UTC);
        });
    }

    @Test
    void legacyHotResetPathDoesNotUseJvmLocalTime() {
        DecisionResult current = new DecisionResult();
        current.setDecisionId("decision-legacy");
        current.setAnalysisId("ana-test");
        current.setSymbol("BTCUSDT");
        HotResetServiceImpl legacyService = spy(service);
        ArgumentCaptor<HotResetCommand> command = ArgumentCaptor.forClass(HotResetCommand.class);
        doReturn(new HotResetResult()).when(legacyService).evaluateAndExecute(command.capture());
        TimeZone original = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            legacyService.executeHotReset(new DecisionContext(), current);
        } finally {
            TimeZone.setDefault(original);
        }

        assertThat(command.getValue().getOccurredAt()).isNull();
        assertThat(command.getValue().getEventKey()).isEqualTo("legacy-hot-reset-decision-legacy");
    }

    @Test
    void hotResetWriteAndBaselineUseSameUtcWindow() {
        mockTriggeredPath();
        whenRebuildReturns(executed("ana-rebuild", false, false));
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        command.setOccurredAt(null);

        service.evaluateAndExecute(command);

        ArgumentCaptor<HotResetEventDO> event = ArgumentCaptor.forClass(HotResetEventDO.class);
        verify(hotResetEventMapper).insert(event.capture());
        assertThat(event.getValue().getEventTime())
                .isBetween(LocalDateTime.parse("2026-07-14T11:30:00"), FIXED_UTC)
                .isEqualTo(FIXED_UTC);
        ArgumentCaptor<AssetStateDO> core = ArgumentCaptor.forClass(AssetStateDO.class);
        ArgumentCaptor<AssetStateDO> hot = ArgumentCaptor.forClass(AssetStateDO.class);
        verify(assetStateMapper).mergeUpsertCore(core.capture());
        verify(assetStateMapper).updateHotResetColumns(hot.capture());
        assertThat(core.getValue().getLastUpdateTime()).isEqualTo(FIXED_UTC);
        assertThat(hot.getValue().getLastUpdateTime()).isEqualTo(FIXED_UTC);
        assertThat(hot.getValue().getHotResetTime()).isEqualTo(FIXED_UTC);
        verify(decisionResultMapper).markHotResetInvalidatedBySymbol(
                eq("BTCUSDT"), anyString(), anyString(), eq(FIXED_UTC));
        verify(executionPlanMapper).markNeedsRevalidationForHotReset(
                eq("ana-test"), eq("BTCUSDT"), anyString(), anyString(), eq(FIXED_UTC));
        ArgumentCaptor<HotResetEventDO> rebuild = ArgumentCaptor.forClass(HotResetEventDO.class);
        verify(hotResetEventMapper).updateRebuildOutcome(rebuild.capture());
        assertThat(rebuild.getValue().getCompletedAt()).isEqualTo(FIXED_UTC);
    }

    private void assertUnsafePreStateInvalidates(AssetStateEnum preState) {
        HotResetCommand command = command(HotResetEventTypeEnum.EXTREME_PRICE_MOVE);
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(preState, 10, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(80, preState.name(), "OBSERVING", false, false, 0, false, "price", "base"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        whenRebuildReturns(executed("ana-rebuild", false, false));

        HotResetResult result = service.evaluateAndExecute(command);

        assertThat(result.getPreState()).isEqualTo(preState.name());
        assertThat(result.getPostState()).isEqualTo(AssetStateEnum.INVALIDATED.name());
        assertThat(result.getPostState()).isNotIn("CANDIDATE", "WAITING_TRIGGER", "TRIGGERED");
    }

    private void assertPostStateFor(HotResetCommand command, AssetStateEnum expected, ConfusedResult confusedResult) {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 54, 1));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class))).thenReturn(confusedResult);
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        whenRebuildReturns(executed("ana-rebuild", false, false));

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

    private static HotResetPolicy.Thresholds thresholds() {
        return new HotResetPolicy.Thresholds(
                new BigDecimal("0.08"), new BigDecimal("-0.30"), new BigDecimal("-0.40"), 85);
    }

    private static AssetStateDO row(AssetStateEnum state, int confusedScore, int lowStreak) {
        AssetStateDO row = new AssetStateDO();
        row.setState(state);
        row.setConfusedScore(confusedScore);
        row.setConfusedLowStreak(lowStreak);
        return row;
    }

    private void mockTriggeredPath() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CANDIDATE, 10, 0));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(80, "CANDIDATE", "OBSERVING", false, false, 0, false, "price", "base"));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
    }

    private void whenRebuildReturns(AnalysisRunResult result) {
        when(scheduler.runHotResetRebuild(anyString(), anyString(), anyString(), any(), any())).thenReturn(result);
    }

    private HotResetEventDO assertRebuildOutcome(String status, String rebuildAnalysisId,
                                                 String errorCode, String messageFragment) {
        ArgumentCaptor<HotResetEventDO> captor = ArgumentCaptor.forClass(HotResetEventDO.class);
        verify(hotResetEventMapper).updateRebuildOutcome(captor.capture());
        HotResetEventDO update = captor.getValue();
        assertThat(update.getExecutionStatus()).isEqualTo(status);
        assertThat(update.getRebuildAnalysisId()).isEqualTo(rebuildAnalysisId);
        assertThat(update.getExecutionErrorCode()).isEqualTo(errorCode);
        if (messageFragment != null) {
            assertThat(update.getExecutionErrorMessage()).contains(messageFragment);
        } else if (errorCode == null) {
            assertThat(update.getExecutionErrorMessage()).isNull();
        }
        return update;
    }

    private static AnalysisRunResult executed(String analysisId, boolean failedRecovery, boolean expiredLeaseRecovery) {
        return AnalysisRunResult.executed(run(analysisId, "STARTED"), rebuilt(analysisId),
                failedRecovery, expiredLeaseRecovery);
    }

    private static AnalysisRunDO run(String analysisId, String status) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setTraceId("trace-" + analysisId);
        run.setRequestId("req-" + analysisId);
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1m");
        run.setTriggerType("HOT_RESET_REBUILD");
        run.setTriggerReference("hre-test");
        run.setStatus(status);
        return run;
    }

    private static AssetAnalysisVO rebuilt(String analysisId) {
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId(analysisId);
        analysis.setSymbol("BTCUSDT");
        analysis.setTimeframe("1m");
        return analysis;
    }
}
