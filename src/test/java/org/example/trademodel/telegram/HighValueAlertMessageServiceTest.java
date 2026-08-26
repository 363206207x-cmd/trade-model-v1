package org.example.trademodel.telegram;

import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.MessageDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MessageMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.MessageFactService;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.PushRecheckCoreTransactionService;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.service.WorkspacePushRecheckService;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighValueAlertMessageServiceTest {
    @Mock private MessageFactService messageFactService;
    @Mock private AssetPoolService assetPoolService;
    @Mock private PushSnapshotMapper pushSnapshotMapper;
    @Mock private ExecutionPlanMapper executionPlanMapper;
    @Mock private MessageMapper messageMapper;
    @Mock private PushRecheckLogMapper pushRecheckLogMapper;
    @Mock private PushRecheckCoreTransactionService coreTransactionService;
    @Mock private AnalysisRunOrchestrator analysisRunOrchestrator;

    private HighValueAlertMessageService service;
    private TelegramProperties telegramProperties;

    @BeforeEach
    void setUp() {
        telegramProperties = new TelegramProperties();
        telegramProperties.setCooldownMinutes(15);
        org.mockito.Mockito.lenient().when(messageFactService.recordIfAbsent(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new HighValueAlertMessageService(
                messageFactService, assetPoolService, pushSnapshotMapper, executionPlanMapper,
                FundamentalAiV41Properties.contractFixture(), telegramProperties, new HighValueAlertPolicy(),
                Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void validatedConfirmationFinalWithSnapshotCreatesShortCanonicalMessage() {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("analysis-9");
        run.setTraceId("trace-9");
        run.setOwnerType("USER");
        run.setOwnerId(41L);
        run.setAssetId(9L);
        run.setSymbol("SOLUSDT");
        run.setPreview(false);

        DecisionResult decision = new DecisionResult();
        decision.setEvidenceSummary("趋势、资金与事件证据通过规则校验");

        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-9");
        plan.setAnalysisId("analysis-9");
        plan.setOpportunityId("opportunity-9");
        plan.setTraceId("trace-9");
        plan.setFinalPlan(true);
        plan.setFinalPlanMode("CONFIRMATION");
        plan.setFinalMarketBias("LONG");
        plan.setRuleValidationStatus("PASS");
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setPlanLifecycleState("CURRENT");
        plan.setSourceGateComplete(true);
        plan.setSourceGateStatus("PASS");
        plan.setSourceStatus("VERIFIED");
        plan.setExecutionFeasibilityStatus("PASS");
        plan.setExecutionFeasibilityFreshUntil(LocalDateTime.of(2026, 8, 16, 12, 5));
        plan.setValidUntil(LocalDateTime.of(2026, 8, 16, 13, 0));
        plan.setDataQuality(90);
        plan.setEntryZone("142 - 145");
        plan.setTriggerCondition("15m 收盘确认");
        plan.setStopLoss("138");
        plan.setTakeProfitRules("150 / 156 分批止盈");
        plan.setNotTradeInstruction(true);
        plan.setNotOrderExecution(true);

        OpportunityTransitionResult opportunity = new OpportunityTransitionResult(
                "opportunity-9", "SOLUSDT", AssetStateEnum.CANDIDATE,
                AssetStateEnum.TRIGGERED, true, false, "TRIGGER_CONFIRMED",
                "ANALYSIS", "MANUAL_REVIEW", LocalDateTime.of(2026, 8, 16, 12, 0));
        OpportunityLogDTO persistedLog = new OpportunityLogDTO();
        persistedLog.setOpportunityId("opportunity-9");
        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setPushId(99L);
        snapshot.setExpiresAt(LocalDateTime.of(2026, 8, 16, 12, 30));

        when(assetPoolService.isOpportunitySource("USER", 41L, 9L, "SOLUSDT")).thenReturn(true);
        when(pushSnapshotMapper.listByAnalysisId("analysis-9")).thenReturn(List.of(snapshot));

        MessageDO message = service.recordOpportunity(run, decision, plan, opportunity, persistedLog);

        assertThat(message.getCategory()).isEqualTo("HIGH_PERMISSION_OPPORTUNITY");
        assertThat(message.getSourceType()).isEqualTo("PUSH_SNAPSHOT");
        assertThat(message.getSourceId()).isEqualTo("99");
        assertThat(message.getCurrentRecheckId()).isNull();
        assertThat(message.getPlanId()).isEqualTo("plan-9");
        assertThat(message.getTitle()).isEqualTo("【可复核执行计划】");
        assertThat(message.getDedupeKey()).startsWith("TG1|OPPORTUNITY_READY|CONFIRMATION|3|");
        assertThat(message.getBody()).contains(
                "SOLUSDT  ·  偏多  ·  确认型", "入场：142 - 145", "触发：15m 收盘确认",
                "止损：138", "目标：150 / 156 分批止盈", "操作：打开系统重新校验");
        assertThat(message.getBody()).doesNotContain("主要依据", "主要风险", "不构成交易指令");
        assertThat(message.getNotTradeInstruction()).isTrue();
        assertThat(message.getNotOrderExecution()).isTrue();
        verify(messageFactService).recordIfAbsent(message);
    }

    @Test
    void completeConfirmationFinalWithoutSnapshotUsesPlanIdentityAndNoFakeRecheck() {
        AnalysisRunDO run = qualifiedRun();
        ExecutionPlanDO plan = qualifiedPlan();
        OpportunityTransitionResult opportunity = qualifiedOpportunity();
        OpportunityLogDTO persistedLog = new OpportunityLogDTO();
        persistedLog.setOpportunityId("opportunity-9");
        when(assetPoolService.isOpportunitySource("USER", 41L, 9L, "SOLUSDT")).thenReturn(true);
        when(pushSnapshotMapper.listByAnalysisId("analysis-9")).thenReturn(List.of());

        MessageDO message = service.recordOpportunity(run, new DecisionResult(), plan, opportunity, persistedLog);

        assertThat(message.getSourceType()).isEqualTo("FINAL_PLAN");
        assertThat(message.getSourceId()).isEqualTo("plan-9");
        assertThat(message.getCurrentRecheckId()).isNull();
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(message)).isPresent();
    }

    @Test
    void realSnapshotExpiryCanOwnValidityButMissingBothExpiriesFailsClosed() {
        AnalysisRunDO run = qualifiedRun();
        ExecutionPlanDO plan = qualifiedPlan();
        plan.setValidUntil(null);
        OpportunityLogDTO persistedLog = new OpportunityLogDTO();
        persistedLog.setOpportunityId("opportunity-9");
        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setPushId(99L);
        snapshot.setExpiresAt(LocalDateTime.of(2026, 8, 16, 12, 30));
        when(assetPoolService.isOpportunitySource("USER", 41L, 9L, "SOLUSDT")).thenReturn(true);
        when(pushSnapshotMapper.listByAnalysisId("analysis-9")).thenReturn(List.of(snapshot));

        MessageDO message = service.recordOpportunity(
                run, new DecisionResult(), plan, qualifiedOpportunity(), persistedLog);
        assertThat(message.getExpiresAt()).isEqualTo(snapshot.getExpiresAt());
        assertThat(message.getBody()).contains("有效至：2026-08-16 12:30 UTC");

        snapshot.setExpiresAt(null);
        assertThat(service.recordOpportunity(
                run, new DecisionResult(), plan, qualifiedOpportunity(), persistedLog)).isNull();
    }

    @Test
    void reducedCannotUseLegacyOptInAndMissingPlanFactsFailClosed() {
        AnalysisRunDO run = qualifiedRun();
        OpportunityTransitionResult opportunity = qualifiedOpportunity();
        OpportunityLogDTO persistedLog = new OpportunityLogDTO();
        persistedLog.setOpportunityId("opportunity-9");
        when(assetPoolService.isOpportunitySource("USER", 41L, 9L, "SOLUSDT")).thenReturn(true);
        when(pushSnapshotMapper.listByAnalysisId("analysis-9")).thenReturn(List.of());
        telegramProperties.setAllowHighQualityReduced(true);

        ExecutionPlanDO reduced = qualifiedPlan();
        reduced.setFinalPlanMode("REDUCED");
        assertThat(service.recordOpportunity(run, new DecisionResult(), reduced, opportunity, persistedLog)).isNull();

        ExecutionPlanDO missingStop = qualifiedPlan();
        missingStop.setStopLoss(null);
        assertThat(service.recordOpportunity(run, new DecisionResult(), missingStop, opportunity, persistedLog)).isNull();

        ExecutionPlanDO missingTarget = qualifiedPlan();
        missingTarget.setTakeProfitRules(null);
        missingTarget.setTargetLogic(null);
        assertThat(service.recordOpportunity(run, new DecisionResult(), missingTarget, opportunity, persistedLog)).isNull();
    }

    @Test
    void safetyChangeCreatesCanonicalMessageBeforeAnyChannelDelivery() {
        MessageDO message = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.HOT_RESET,
                "HOT_RESET", "reset-9", "analysis-9", "plan-9", "opportunity-9", "99",
                "SOLUSDT", "trace-9", "CONFUSED", 4,
                "证据冲突", "等待规则与数据重新验证", LocalDateTime.of(2026, 8, 16, 12, 0), null));

        assertThat(message.getCategory()).isEqualTo("OPPORTUNITY_PLAN_SAFETY_CHANGE");
        assertThat(message.getCurrentRecheckId()).isNull();
        assertThat(message.getDedupeKey()).startsWith("TG1|HOT_RESET|CONFUSED|4|");
        assertThat(message.getTraceId()).isEqualTo("trace-9");
        assertThat(message.getNotTradeInstruction()).isTrue();
        assertThat(message.getNotOrderExecution()).isTrue();
        assertThat(message.getBody()).contains("当前状态：暂不视为有效机会", "恢复条件");
    }

    @Test
    void productionMessageIdentityResolvesOwnedSnapshotAndWritesRealRecheckOnlyAfterOpen() {
        when(messageFactService.recordIfAbsent(any())).thenAnswer(invocation -> {
            MessageDO message = invocation.getArgument(0);
            message.setMessageId("message-prod");
            return message;
        });
        MessageDO message = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.EXECUTION_DRIFT,
                "PUSH_SNAPSHOT", "99", "analysis-9", "plan-9", "opportunity-9", "99",
                "SOLUSDT", "trace-9", "DRIFTED", 3,
                "价格偏离", "重新校验", LocalDateTime.of(2026, 8, 16, 12, 0), null));
        assertThat(message.getCurrentRecheckId()).isNull();

        when(messageMapper.selectByIdForUser("message-prod", 41L)).thenReturn(message);
        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setPushId(99L);
        snapshot.setAnalysisId("analysis-9");
        snapshot.setTraceId("trace-9");
        snapshot.setSymbol("SOLUSDT");
        snapshot.setTimeframe("15m");
        when(pushSnapshotMapper.selectByPushId(99L)).thenReturn(snapshot);
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-9");
        plan.setAnalysisId("analysis-9");
        plan.setOpportunityId("opportunity-9");
        plan.setTraceId("trace-9");
        when(executionPlanMapper.selectByPlanId("plan-9")).thenReturn(plan);
        TmPushRecheckLogDO recheck = new TmPushRecheckLogDO();
        recheck.setLogId(701L);
        recheck.setPushId(99L);
        recheck.setTriggerSource("PUSH_OPEN");
        recheck.setExecutionStatus("COMPLETED");
        recheck.setRecheckStatus("REVIEW_PASSED");
        when(coreTransactionService.execute(41L, "message-prod", 99L, null, 1))
                .thenReturn(new PushRecheckCoreTransactionService.AttemptResult(
                        new RecheckResult(), recheck, true));

        WorkspacePushRecheckService workspace = new WorkspacePushRecheckService(
                messageMapper, pushSnapshotMapper, pushRecheckLogMapper, executionPlanMapper,
                coreTransactionService, analysisRunOrchestrator);
        WorkspacePushRecheckService.Projection result = workspace.open(
                41L, "message-prod", "push-snapshot-99");

        assertThat(result.messageId()).isEqualTo("message-prod");
        assertThat(result.pushSnapshotId()).isEqualTo("push-snapshot-99");
        assertThat(result.pushId()).isEqualTo(99L);
        assertThat(result.recheckId()).isEqualTo(701L);
        assertThat(result.analysisId()).isEqualTo("analysis-9");
        assertThat(result.planId()).isEqualTo("plan-9");
        verify(coreTransactionService).execute(41L, "message-prod", 99L, null, 1);
    }

    @Test
    void safetyMessagesKeepTheirTg1FactsButNeverBecomeTelegramDeliveries() {
        MessageDO first = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.EXECUTION_DRIFT,
                "PUSH_RECHECK", "recheck-1", "analysis-1", "plan-1", "opportunity-1", "snapshot-1",
                "BTCUSDT", "trace-1", "DRIFTED", 3, "drift", "revalidate",
                LocalDateTime.of(2026, 8, 16, 12, 0), null));
        MessageDO second = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.EXECUTION_DRIFT,
                "PUSH_RECHECK", "recheck-2", "analysis-2", "plan-1", "opportunity-1", "snapshot-2",
                "BTCUSDT", "trace-2", "DRIFTED", 3, "drift", "revalidate",
                LocalDateTime.of(2026, 8, 16, 12, 30), null));

        assertThat(first.getSourceId()).isNotEqualTo(second.getSourceId());
        assertThat(first.getDedupeKey()).startsWith("TG1|EXECUTION_DRIFT|DRIFTED|");
        assertThat(second.getDedupeKey()).startsWith("TG1|EXECUTION_DRIFT|DRIFTED|");
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(first)).isEmpty();
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(second)).isEmpty();
    }

    @Test
    void verifiedFreshManualPositionUsesHighestConcreteChangeAndShortTemplate() {
        UserPositionDO position = position();
        PositionMonitorLogDTO log = trustedLog();
        PositionMonitorResultDTO result = trustedResult();
        result.setStopLossBreached(true);
        result.setTakeProfitReached(true);

        MessageDO message = service.recordPosition(position, log, result);

        assertThat(message.getCategory()).isEqualTo("POSITION_LOGIC_RISK_CHANGE");
        assertThat(message.getPositionId()).isEqualTo(91L);
        assertThat(message.getSourceType()).isEqualTo("POSITION_MONITOR");
        assertThat(message.getTitle()).isEqualTo("【持仓需关注】");
        assertThat(message.getBody()).contains(
                "BTCUSDT  ·  做多", "变化：触及止损", "入场：100", "现价：99",
                "止损：95  目标：110", "操作：打开持仓详情");
        assertThat(message.getBody()).doesNotContain("建议动作", "不构成交易指令");
        assertThat(message.getDedupeKey()).startsWith("TG1|POSITION_RISK_CHANGE|STOP_LOSS_BREACHED|4|");
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(message)).isPresent();
    }

    @Test
    void weakenedAndPlanInvalidatedInAppMessagesRemainButAreNotTelegramEligible() {
        UserPositionDO position = position();
        PositionMonitorLogDTO log = trustedLog();
        log.setReversalStatus("NO_REVERSAL");
        log.setRiskLevel("MEDIUM");
        log.setRiskTrend("STABLE");
        log.setMonitorConclusion("PLAN_INVALIDATED");
        PositionMonitorResultDTO result = trustedResult();
        result.setReversalStatus("NO_REVERSAL");
        result.setRiskLevel("MEDIUM");
        result.setRiskTrend("STABLE");
        result.setMonitorConclusion("PLAN_INVALIDATED");

        MessageDO message = service.recordPosition(position, log, result);

        assertThat(message).isNotNull();
        assertThat(message.getTitle()).isEqualTo("【持仓逻辑发生重要变化】");
        assertThat(message.getDedupeKey()).startsWith("TG1|POSITION_RISK_CHANGE|PLAN_INVALIDATED|");
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(message)).isEmpty();
    }

    @Test
    void riskAlertMayShowUnsetBoundariesButStopEventCannotBorrowMissingStop() {
        UserPositionDO position = position();
        position.setStopLoss(null);
        position.setTakeProfit(null);
        PositionMonitorLogDTO log = trustedLog();
        log.setReversalStatus("NO_REVERSAL");
        log.setRiskLevel("HIGH");
        log.setRiskTrend("STABLE");
        PositionMonitorResultDTO risk = trustedResult();
        risk.setReversalStatus("NO_REVERSAL");
        risk.setRiskLevel("HIGH");
        risk.setRiskTrend("STABLE");
        risk.setStopLossBreached(true);

        MessageDO message = service.recordPosition(position, log, risk);

        assertThat(message.getDedupeKey()).contains("|RISK_HIGH|");
        assertThat(message.getBody()).contains("变化：风险高", "止损：未设置  目标：未设置");
        assertThat(message.getBody()).doesNotContain("触及止损");
    }

    @Test
    void partiallyClosedRemainsActiveWhileClosedPositionCannotCreateMessage() {
        UserPositionDO position = position();
        position.setStatus("PARTIALLY_CLOSED");
        PositionMonitorResultDTO result = trustedResult();
        result.setStopLossBreached(false);
        assertThat(service.recordPosition(position, trustedLog(), result)).isNotNull();

        position.setStatus("CLOSED");
        assertThat(service.recordPosition(position, trustedLog(), result)).isNull();
    }

    @Test
    void missingEntryOrUntrustedCurrentPriceKeepsInAppFactButBlocksTelegram() {
        UserPositionDO position = position();
        position.setEntryPrice(null);
        PositionMonitorResultDTO missingEntry = trustedResult();
        MessageDO inAppOnly = service.recordPosition(position, trustedLog(), missingEntry);
        assertThat(inAppOnly).isNotNull();
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(inAppOnly)).isEmpty();

        position.setEntryPrice(new BigDecimal("100"));
        PositionMonitorResultDTO stalePrice = trustedResult();
        stalePrice.setMarkPriceFresh(false);
        MessageDO staleInAppOnly = service.recordPosition(position, trustedLog(), stalePrice);
        assertThat(staleInAppOnly).isNotNull();
        assertThat(HighValueAlertPolicy.telegramDeliveryIdentity(staleInAppOnly)).isEmpty();
    }

    @Test
    void pendingStaleAndNonMaterialPositionResultsCreateNoMessage() {
        UserPositionDO position = position();
        PositionMonitorLogDTO pending = trustedLog();
        pending.setMonitorSourceStatus("PENDING_VERIFICATION");

        assertThat(service.recordPosition(position, pending, trustedResult())).isNull();
        verify(messageFactService, never()).recordIfAbsent(any());
    }

    private static AnalysisRunDO qualifiedRun() {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("analysis-9");
        run.setTraceId("trace-9");
        run.setOwnerType("USER");
        run.setOwnerId(41L);
        run.setAssetId(9L);
        run.setSymbol("SOLUSDT");
        run.setPreview(false);
        return run;
    }

    private static ExecutionPlanDO qualifiedPlan() {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-9");
        plan.setAnalysisId("analysis-9");
        plan.setOpportunityId("opportunity-9");
        plan.setTraceId("trace-9");
        plan.setFinalPlan(true);
        plan.setFinalPlanMode("CONFIRMATION");
        plan.setFinalMarketBias("LONG");
        plan.setPlanLifecycleState("CURRENT");
        plan.setRuleValidationStatus("PASS");
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setSourceGateComplete(true);
        plan.setSourceGateStatus("PASS");
        plan.setSourceStatus("VERIFIED");
        plan.setExecutionFeasibilityStatus("PASS");
        plan.setExecutionFeasibilityFreshUntil(LocalDateTime.of(2026, 8, 16, 12, 5));
        plan.setValidUntil(LocalDateTime.of(2026, 8, 16, 13, 0));
        plan.setDataQuality(90);
        plan.setEntryZone("142 - 145");
        plan.setTriggerCondition("15m 收盘确认");
        plan.setStopLoss("138");
        plan.setTakeProfitRules("150 / 156 分批止盈");
        plan.setNotTradeInstruction(true);
        plan.setNotOrderExecution(true);
        return plan;
    }

    private static OpportunityTransitionResult qualifiedOpportunity() {
        return new OpportunityTransitionResult(
                "opportunity-9", "SOLUSDT", AssetStateEnum.CANDIDATE,
                AssetStateEnum.TRIGGERED, true, false, "TRIGGER_CONFIRMED",
                "ANALYSIS", "MANUAL_REVIEW", LocalDateTime.of(2026, 8, 16, 12, 0));
    }

    private static UserPositionDO position() {
        UserPositionDO position = new UserPositionDO();
        position.setId(91L);
        position.setUserId(41L);
        position.setAssetSymbol("BTCUSDT");
        position.setSide("LONG");
        position.setStatus("OPEN");
        position.setSourceType("MANUAL_POSITION");
        position.setEntryPrice(new BigDecimal("100"));
        position.setStopLoss(new BigDecimal("95"));
        position.setTakeProfit(new BigDecimal("110"));
        return position;
    }

    private static PositionMonitorResultDTO trustedResult() {
        PositionMonitorResultDTO result = new PositionMonitorResultDTO();
        result.setPositionId(91L);
        result.setMonitorLogId(201L);
        result.setMarkPrice(new BigDecimal("99"));
        result.setCurrentPrice(new BigDecimal("99"));
        result.setMarkPriceSource("BINANCE");
        result.setMarkPriceObservedAt(LocalDateTime.of(2026, 8, 16, 11, 59));
        result.setMarkPriceFresh(true);
        result.setEntryLogicStatus("WEAKENED");
        result.setReversalStatus("STRONG_REVERSAL");
        result.setRiskLevel("HIGH");
        result.setRiskTrend("INCREASED");
        result.setMonitorConclusion("HIGH_RISK_OBSERVATION");
        return result;
    }

    private static PositionMonitorLogDTO trustedLog() {
        PositionMonitorLogDTO log = new PositionMonitorLogDTO();
        log.setLogId(201L);
        log.setPositionId(91L);
        log.setAnalysisId("analysis-9");
        log.setTraceId("trace-9");
        log.setMonitorSourceStatus("VERIFIED");
        log.setObservedAt(LocalDateTime.of(2026, 8, 16, 11, 59));
        log.setFreshUntil(LocalDateTime.of(2026, 8, 16, 12, 5));
        log.setCreatedAt(LocalDateTime.of(2026, 8, 16, 12, 0));
        log.setCurrentPrice(new BigDecimal("99"));
        log.setEntryLogicStatus("WEAKENED");
        log.setReversalStatus("STRONG_REVERSAL");
        log.setRiskLevel("HIGH");
        log.setRiskTrend("INCREASED");
        log.setMonitorConclusion("HIGH_RISK_OBSERVATION");
        log.setRiskChangeReason("OPPOSING_EVIDENCE_INCREASED");
        return log;
    }
}
