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

    @BeforeEach
    void setUp() {
        TelegramProperties telegram = new TelegramProperties();
        telegram.setCooldownMinutes(15);
        org.mockito.Mockito.lenient().when(messageFactService.recordIfAbsent(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new HighValueAlertMessageService(
                messageFactService, assetPoolService, pushSnapshotMapper, executionPlanMapper,
                FundamentalAiV41Properties.contractFixture(), telegram, new HighValueAlertPolicy(),
                Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void validatedFinalOpportunityWithSnapshotCreatesCanonicalMessage() {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("analysis-9");
        run.setTraceId("trace-9");
        run.setOwnerType("USER");
        run.setOwnerId(41L);
        run.setAssetId(9L);
        run.setSymbol("SOLUSDT");
        run.setTimeframe("15m");
        run.setPreview(false);

        DecisionResult decision = new DecisionResult();
        decision.setEvidenceSummary("趋势、资金与事件证据通过规则校验");

        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-9");
        plan.setFinalPlan(true);
        plan.setFinalPlanMode("CONFIRMATION");
        plan.setFinalMarketBias("LONG");
        plan.setRuleValidationStatus("PASS");
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setSourceGateComplete(true);
        plan.setSourceGateStatus("PASS");
        plan.setSourceStatus("VERIFIED");
        plan.setExecutionFeasibilityStatus("PASS");
        plan.setExecutionFeasibilityFreshUntil(LocalDateTime.of(2026, 8, 16, 12, 5));
        plan.setValidUntil(LocalDateTime.of(2026, 8, 16, 13, 0));
        plan.setDataQuality(90);
        plan.setEntryLogic("多源证据一致");
        plan.setEntryZone("100-101");
        plan.setTriggerCondition("15m 收盘确认");
        plan.setStopLoss("98");
        plan.setInvalidCondition("结构跌破 98");
        plan.setTakeProfitRules("分批止盈");
        plan.setPositionSuggestion("小仓位人工确认");
        plan.setRiskExplanation("仅供人工复核，需关注流动性变化");
        plan.setSourceRefsJson("BINANCE:BTCUSDT:15m");
        plan.setExecutionFeasibilityObservedAt(LocalDateTime.of(2026, 8, 16, 11, 59));
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

        when(assetPoolService.isOpportunitySource("USER", 41L, 9L, "SOLUSDT")).thenReturn(true);
        when(pushSnapshotMapper.listByAnalysisId("analysis-9")).thenReturn(List.of(snapshot));

        MessageDO message = service.recordOpportunity(run, decision, plan, opportunity, persistedLog);

        assertThat(message.getCategory()).isEqualTo("HIGH_PERMISSION_OPPORTUNITY");
        assertThat(message.getSourceType()).isEqualTo("PUSH_SNAPSHOT");
        assertThat(message.getSourceId()).isEqualTo("99");
        assertThat(message.getCurrentRecheckId()).isNull();
        assertThat(message.getPlanId()).isEqualTo("plan-9");
        assertThat(message.getDedupeKey()).startsWith("TG1|OPPORTUNITY_READY|TRIGGERED|3|");
        assertThat(message.getBody()).contains(
                "资产：SOLUSDT", "周期：15m", "方向：偏多", "计划模式：确认型",
                "机会状态：已触发", "入场区：100-101", "触发条件：15m 收盘确认",
                "止损区：98", "失效条件：结构跌破 98", "目标/仓位管理：分批止盈；小仓位人工确认",
                "数据来源：BINANCE:BTCUSDT:15m", "数据时间：2026-08-16 11:59 UTC",
                "来源有效至：2026-08-16 12:05 UTC", "操作：打开系统重新校验");
        assertThat(message.getNotTradeInstruction()).isTrue();
        assertThat(message.getNotOrderExecution()).isTrue();
        verify(messageFactService).recordIfAbsent(message);
    }

    @Test
    void safetyChangeCreatesCanonicalMessageBeforeAnyChannelDelivery() {
        MessageDO message = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.HOT_RESET,
                "HOT_RESET", "reset-9", "analysis-9", "plan-9", "opportunity-9", "99",
                "SOLUSDT", "15m", "TRIGGERED", "trace-9", "CONFUSED", 4,
                "证据冲突", "等待规则与数据重新验证", LocalDateTime.of(2026, 8, 16, 12, 0), null));

        assertThat(message.getCategory()).isEqualTo("OPPORTUNITY_PLAN_SAFETY_CHANGE");
        assertThat(message.getCurrentRecheckId()).isNull();
        assertThat(message.getDedupeKey()).startsWith("TG1|HOT_RESET|CONFUSED|4|");
        assertThat(message.getTraceId()).isEqualTo("trace-9");
        assertThat(message.getNotTradeInstruction()).isTrue();
        assertThat(message.getNotOrderExecution()).isTrue();
        assertThat(message.getBody()).contains(
                "周期：15m", "原状态：TRIGGERED", "当前状态：CONFUSED",
                "当前有效性：需重新校验，当前不作为可执行机会", "发生时间：2026-08-16 12:00 UTC");
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
                "SOLUSDT", "15m", "READY", "trace-9", "DRIFTED", 3,
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
        when(executionPlanMapper.selectByPlanIdForUser("plan-9", 41L)).thenReturn(plan);
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
    void samePlanUsesStableExactAndCooldownIdentityAcrossDifferentRecheckEvidence() {
        MessageDO first = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.EXECUTION_DRIFT,
                "PUSH_RECHECK", "recheck-1", "analysis-1", "plan-1", "opportunity-1", "snapshot-1",
                "BTCUSDT", "15m", "READY", "trace-1", "DRIFTED", 3, "drift", "revalidate",
                LocalDateTime.of(2026, 8, 16, 12, 0), null));
        MessageDO second = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.EXECUTION_DRIFT,
                "PUSH_RECHECK", "recheck-2", "analysis-2", "plan-1", "opportunity-1", "snapshot-2",
                "BTCUSDT", "15m", "READY", "trace-2", "DRIFTED", 3, "drift", "revalidate",
                LocalDateTime.of(2026, 8, 16, 12, 30), null));

        assertThat(first.getSourceId()).isNotEqualTo(second.getSourceId());
        assertThat(TelegramDedupeKey.cooldownKey(first.getCategory(), first.getDedupeKey()))
                .isEqualTo(TelegramDedupeKey.cooldownKey(second.getCategory(), second.getDedupeKey()));
    }

    @Test
    void verifiedFreshManualPositionMaterialChangeCreatesRiskMessage() {
        UserPositionDO position = new UserPositionDO();
        position.setId(91L);
        position.setUserId(41L);
        position.setAssetSymbol("BTCUSDT");
        position.setSide("LONG");
        position.setEntryPrice(new java.math.BigDecimal("100"));
        position.setStopLoss(new java.math.BigDecimal("95"));
        position.setTakeProfit(new java.math.BigDecimal("110"));
        position.setStatus("OPEN");
        position.setSourceType("MANUAL_POSITION");
        PositionMonitorLogDTO log = trustedLog();
        log.setCurrentPrice(new java.math.BigDecimal("102"));
        log.setMarkPriceSource("BINANCE");
        log.setReason("反向证据增加且接近止损区域");
        log.setSuggestedAction("TIGHTEN_STOP");
        PositionMonitorResultDTO result = new PositionMonitorResultDTO();
        result.setMarkPrice(new java.math.BigDecimal("102"));
        result.setMarkPriceSource("BINANCE");
        result.setNearStopLoss(true);
        result.setSuggestedManualActionText("收紧止损并人工复核");

        MessageDO message = service.recordPosition(position, log, result);

        assertThat(message.getCategory()).isEqualTo("POSITION_LOGIC_RISK_CHANGE");
        assertThat(message.getPositionId()).isEqualTo(91L);
        assertThat(message.getSourceType()).isEqualTo("POSITION_MONITOR");
        assertThat(message.getBody()).contains(
                "方向：做多", "实际入场价：100", "可信当前价：102",
                "当前变化：入场逻辑弱化", "反转状态：强反转", "风险：高，正在升级",
                "止损/目标距离：接近实际止损；止损 95；目标 110",
                "监控结论：高风险观察", "建议动作：收紧止损并人工复核",
                "来源时效：BINANCE；观测于 2026-08-16 11:59 UTC；有效至 2026-08-16 12:05 UTC",
                "操作：打开持仓详情");
        assertThat(message.getDedupeKey()).startsWith("TG1|POSITION_RISK_CHANGE|STRONG_REVERSAL|4|");
    }

    @Test
    void pendingStaleAndNonMaterialPositionResultsCreateNoMessage() {
        UserPositionDO position = new UserPositionDO();
        position.setId(91L);
        position.setUserId(41L);
        position.setAssetSymbol("BTCUSDT");
        position.setStatus("OPEN");
        position.setSourceType("MANUAL_POSITION");
        PositionMonitorLogDTO pending = trustedLog();
        pending.setMonitorSourceStatus("PENDING_VERIFICATION");

        assertThat(service.recordPosition(position, pending, new PositionMonitorResultDTO())).isNull();
        verify(messageFactService, never()).recordIfAbsent(any());
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
        log.setEntryLogicStatus("WEAKENED");
        log.setReversalStatus("STRONG_REVERSAL");
        log.setRiskLevel("HIGH");
        log.setRiskTrend("INCREASED");
        log.setMonitorConclusion("HIGH_RISK_OBSERVATION");
        log.setRiskChangeReason("OPPOSING_EVIDENCE_INCREASED");
        return log;
    }
}
