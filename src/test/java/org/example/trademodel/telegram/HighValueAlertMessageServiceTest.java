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
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.MessageFactService;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
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
        plan.setRiskExplanation("仅供人工复核，需关注流动性变化");
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
        assertThat(message.getCurrentRecheckId()).isEqualTo("99");
        assertThat(message.getPlanId()).isEqualTo("plan-9");
        assertThat(message.getDedupeKey()).startsWith("TG1|OPPORTUNITY_READY|TRIGGERED|3|");
        assertThat(message.getBody()).contains(
                "资产：SOLUSDT", "方向：偏多", "计划模式：确认型",
                "机会状态：已触发", "操作：打开系统重新校验");
        assertThat(message.getNotTradeInstruction()).isTrue();
        assertThat(message.getNotOrderExecution()).isTrue();
        verify(messageFactService).recordIfAbsent(message);
    }

    @Test
    void safetyChangeCreatesCanonicalMessageBeforeAnyChannelDelivery() {
        MessageDO message = service.recordSafetyChange(new HighValueAlertMessageService.SafetyChangeInput(
                41L, HighValueAlertPolicy.SafetyChangeType.HOT_RESET,
                "HOT_RESET", "reset-9", "analysis-9", "plan-9", "99",
                "SOLUSDT", "trace-9", "CONFUSED", 4,
                "证据冲突", "等待规则与数据重新验证", LocalDateTime.of(2026, 8, 16, 12, 0), null));

        assertThat(message.getCategory()).isEqualTo("OPPORTUNITY_PLAN_SAFETY_CHANGE");
        assertThat(message.getCurrentRecheckId()).isEqualTo("99");
        assertThat(message.getDedupeKey()).startsWith("TG1|HOT_RESET|CONFUSED|4|");
        assertThat(message.getTraceId()).isEqualTo("trace-9");
        assertThat(message.getNotTradeInstruction()).isTrue();
        assertThat(message.getNotOrderExecution()).isTrue();
        assertThat(message.getBody()).contains("当前状态：暂不视为有效机会", "恢复条件");
    }

    @Test
    void verifiedFreshManualPositionMaterialChangeCreatesRiskMessage() {
        UserPositionDO position = new UserPositionDO();
        position.setId(91L);
        position.setUserId(41L);
        position.setAssetSymbol("BTCUSDT");
        position.setStatus("OPEN");
        position.setSourceType("MANUAL_POSITION");
        PositionMonitorLogDTO log = trustedLog();

        MessageDO message = service.recordPosition(position, log, new PositionMonitorResultDTO());

        assertThat(message.getCategory()).isEqualTo("POSITION_LOGIC_RISK_CHANGE");
        assertThat(message.getPositionId()).isEqualTo(91L);
        assertThat(message.getSourceType()).isEqualTo("POSITION_MONITOR");
        assertThat(message.getBody()).contains(
                "当前变化：入场逻辑弱化", "反转状态：强反转", "风险：高，正在升级",
                "建议：打开持仓详情人工处理");
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
