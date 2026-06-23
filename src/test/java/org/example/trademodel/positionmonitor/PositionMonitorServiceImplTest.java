package org.example.trademodel.positionmonitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
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
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.impl.PositionMonitorServiceImpl;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.ExternalContextSnapshot;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PositionMonitorServiceImplTest {
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
    private ExternalContextEvidenceBuilder externalContextEvidenceBuilder;

    private PositionMonitorServiceImpl service;
    private final AtomicLong logIds = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        service = new PositionMonitorServiceImpl(
                userPositionMapper,
                marketQuoteClient,
                userPositionRiskAdapter,
                executionPlanMapper,
                positionMonitorLogService,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                new ObjectMapper());
        lenient().when(positionMonitorLogService.listByPositionId(anyLong(), eq(1))).thenReturn(List.of());
        lenient().when(positionMonitorLogService.recordMonitorRun(any())).thenAnswer(invocation -> {
            RecordPositionMonitorLogCommand command = invocation.getArgument(0);
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
        });
    }

    @Test
    void longLogicValidWritesExactlyOneLogWithSafetyFields() throws Exception {
        UserPositionDO position = position(1L, "LONG", "OPEN", "plan-valid", "90", "120");
        arrange(position, "100", risk("LOW", false), plan("plan-valid", "ana-1", "VALID", true));

        PositionMonitorResultDTO result = service.monitorUserPosition(1L);

        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(result.getSuggestedAction()).isEqualTo("HOLD");
        assertThat(result.isNearStopLoss()).isFalse();
        assertThat(result.isNearTakeProfit()).isFalse();
        assertThat(result.getMonitorLogId()).isNotNull();
        assertSafetyFields(result);
        assertForbiddenActionFieldsAbsent();

        ArgumentCaptor<RecordPositionMonitorLogCommand> captor = ArgumentCaptor.forClass(RecordPositionMonitorLogCommand.class);
        verify(positionMonitorLogService).recordMonitorRun(captor.capture());
        assertThat(captor.getValue().getCurrentPrice()).isEqualByComparingTo("100");
        assertThat(captor.getValue().getLogicStatus()).isEqualTo("LOGIC_VALID");
        verify(userPositionMapper, never()).manualClose(anyLong(), any(), any(), any(), any());
    }

    @Test
    void shortLogicValidUsesShortStopAndTakeProfitRules() {
        UserPositionDO position = position(2L, "SHORT", "OPEN", "plan-short", "110", "80");
        arrange(position, "100", risk("LOW", false), plan("plan-short", "ana-2", "VALID", true));

        PositionMonitorResultDTO result = service.monitorUserPosition(2L);

        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(result.getSide()).isEqualTo("SHORT");
        assertThat(result.isStopLossBreached()).isFalse();
        assertThat(result.isTakeProfitReached()).isFalse();
    }

    @Test
    void longAndShortNearStopLossAreWeakened() {
        UserPositionDO longPosition = position(3L, "LONG", "OPEN", "plan-long-near-stop", "99", "120");
        arrange(longPosition, "100", risk("LOW", false), plan("plan-long-near-stop", "ana-3", "VALID", true));
        assertThat(service.monitorUserPosition(3L).getLogicStatus()).isEqualTo("LOGIC_WEAKENED");

        UserPositionDO shortPosition = position(4L, "SHORT", "OPEN", "plan-short-near-stop", "101", "80");
        arrange(shortPosition, "100", risk("LOW", false), plan("plan-short-near-stop", "ana-4", "VALID", true));
        PositionMonitorResultDTO result = service.monitorUserPosition(4L);
        assertThat(result.isNearStopLoss()).isTrue();
        assertThat(result.getReasonCodes()).contains("NEAR_STOP_LOSS");
        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
    }

    @Test
    void longAndShortNearTakeProfitCanRemainValidWithManualReviewSuggestion() {
        UserPositionDO longPosition = position(5L, "LONG", "OPEN", "plan-long-near-tp", "90", "101");
        arrange(longPosition, "100", risk("LOW", false), plan("plan-long-near-tp", "ana-5", "VALID", true));
        PositionMonitorResultDTO longResult = service.monitorUserPosition(5L);
        assertThat(longResult.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(longResult.isNearTakeProfit()).isTrue();
        assertThat(longResult.getSuggestedAction()).isEqualTo("MANUAL_REVIEW");

        UserPositionDO shortPosition = position(6L, "SHORT", "PARTIALLY_CLOSED", "plan-short-near-tp", "110", "99");
        arrange(shortPosition, "100", risk("LOW", false), plan("plan-short-near-tp", "ana-6", "VALID", true));
        PositionMonitorResultDTO shortResult = service.monitorUserPosition(6L);
        assertThat(shortResult.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(shortResult.isNearTakeProfit()).isTrue();
        assertThat(shortResult.getPositionStatus()).isEqualTo("PARTIALLY_CLOSED");
    }

    @Test
    void stopBreachedPlanInvalidAndIncompleteSourceGateInvalidatePlan() {
        UserPositionDO longBreached = position(7L, "LONG", "OPEN", "plan-long-breached", "100", "130");
        arrange(longBreached, "99", risk("LOW", false), plan("plan-long-breached", "ana-7", "VALID", true));
        assertThat(service.monitorUserPosition(7L).getLogicStatus()).isEqualTo("PLAN_INVALIDATED");

        UserPositionDO shortBreached = position(8L, "SHORT", "OPEN", "plan-short-breached", "100", "80");
        arrange(shortBreached, "101", risk("LOW", false), plan("plan-short-breached", "ana-8", "VALID", true));
        assertThat(service.monitorUserPosition(8L).getLogicStatus()).isEqualTo("PLAN_INVALIDATED");

        UserPositionDO invalidPlan = position(9L, "LONG", "OPEN", "plan-invalid", "90", "120");
        arrange(invalidPlan, "100", risk("LOW", false), plan("plan-invalid", "ana-9", "INVALID", true));
        assertThat(service.monitorUserPosition(9L).getReasonCodes()).contains("PLAN_INVALID");

        UserPositionDO incompleteSource = position(10L, "LONG", "OPEN", "plan-source-missing", "90", "120");
        arrange(incompleteSource, "100", risk("LOW", false), plan("plan-source-missing", "ana-10", "VALID", false));
        PositionMonitorResultDTO result = service.monitorUserPosition(10L);
        assertThat(result.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(result.getReasonCodes()).contains("SOURCE_GATE_INCOMPLETE");
    }

    @Test
    void planContextAndMissingBoundariesWeakenLogic() {
        UserPositionDO missingContext = position(11L, "LONG", "OPEN", null, "90", "120");
        arrange(missingContext, "100", risk("LOW", false), null);
        PositionMonitorResultDTO missingContextResult = service.monitorUserPosition(11L);
        assertThat(missingContextResult.getAnalysisId()).isEqualTo("USER_POSITION_11");
        assertThat(missingContextResult.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(missingContextResult.getReasonCodes()).contains("PLAN_CONTEXT_MISSING");

        UserPositionDO missingStop = position(12L, "LONG", "OPEN", "plan-missing-stop", null, "120");
        arrange(missingStop, "100", risk("LOW", false), plan("plan-missing-stop", "ana-12", "VALID", true));
        assertThat(service.monitorUserPosition(12L).getReasonCodes()).contains("STOP_LOSS_MISSING");

        UserPositionDO missingTakeProfit = position(13L, "LONG", "OPEN", "plan-missing-tp", "90", null);
        arrange(missingTakeProfit, "100", risk("LOW", false), plan("plan-missing-tp", "ana-13", "VALID", true));
        PositionMonitorResultDTO result = service.monitorUserPosition(13L);
        assertThat(result.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(result.getReasonCodes()).contains("TAKE_PROFIT_MISSING");
    }

    @Test
    void riskBlockedAndRiskIncreasedAreFailClosed() {
        UserPositionDO highRisk = position(14L, "LONG", "OPEN", "plan-high-risk", "90", "120");
        arrange(highRisk, "100", risk("HIGH", true), plan("plan-high-risk", "ana-14", "VALID", true));
        PositionMonitorResultDTO blocked = service.monitorUserPosition(14L);
        assertThat(blocked.getLogicStatus()).isEqualTo("HIGH_RISK");
        assertThat(blocked.getSuggestedAction()).isEqualTo("RISK_REVIEW");
        assertThat(blocked.isRiskBlocked()).isTrue();

        UserPositionDO increased = position(15L, "LONG", "OPEN", "plan-risk-up", "90", "120");
        arrange(increased, "100", risk("MEDIUM", false), plan("plan-risk-up", "ana-15", "VALID", true));
        when(positionMonitorLogService.listByPositionId(15L, 1)).thenReturn(List.of(previousLog("LOW")));
        assertThat(service.monitorUserPosition(15L).isRiskIncreased()).isTrue();

        UserPositionDO unchanged = position(16L, "LONG", "OPEN", "plan-risk-same", "90", "120");
        arrange(unchanged, "100", risk("MEDIUM", false), plan("plan-risk-same", "ana-16", "VALID", true));
        when(positionMonitorLogService.listByPositionId(16L, 1)).thenReturn(List.of(previousLog("MEDIUM")));
        assertThat(service.monitorUserPosition(16L).isRiskIncreased()).isFalse();
    }

    @Test
    void closedMissingInvalidQuoteAndQuoteUnavailableDoNotWriteLogs() {
        when(userPositionMapper.selectById(17L)).thenReturn(position(17L, "LONG", "CLOSED", "plan-closed", "90", "120"));
        assertThatThrownBy(() -> service.monitorUserPosition(17L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OPEN or PARTIALLY_CLOSED");

        when(userPositionMapper.selectById(18L)).thenReturn(null);
        assertThatThrownBy(() -> service.monitorUserPosition(18L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UserPosition not found");

        UserPositionDO invalidQuote = position(19L, "LONG", "OPEN", "plan-invalid-price", "90", "120");
        when(userPositionMapper.selectById(19L)).thenReturn(invalidQuote);
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.of(quote("0")));
        assertThatThrownBy(() -> service.monitorUserPosition(19L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVALID_MARKET_PRICE");

        UserPositionDO unavailableQuote = position(20L, "LONG", "OPEN", "plan-no-quote", "90", "120");
        when(userPositionMapper.selectById(20L)).thenReturn(unavailableQuote);
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.monitorUserPosition(20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QUOTE_UNAVAILABLE");

        verify(positionMonitorLogService, never()).recordMonitorRun(any());
    }

    @Test
    void batchMonitorsOnlyActivePositionsAndReportsIndividualFailures() {
        UserPositionDO open = position(21L, "LONG", "OPEN", "plan-batch-open", "90", "120");
        UserPositionDO partial = position(22L, "SHORT", "PARTIALLY_CLOSED", "plan-batch-partial", "110", "80");
        when(userPositionMapper.listOpenPositions()).thenReturn(List.of(open, partial));
        when(marketQuoteClient.fetch24hTicker("BTC")).thenReturn(Optional.of(quote("100")));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(risk("LOW", false));
        when(executionPlanMapper.selectByPlanId("plan-batch-open"))
                .thenReturn(plan("plan-batch-open", "ana-21", "VALID", true));
        when(marketQuoteClient.fetch24hTicker("ETH")).thenReturn(Optional.empty());

        PositionMonitorBatchResultDTO batch = service.monitorOpenUserPositions();

        assertThat(batch.getTotalCount()).isEqualTo(2);
        assertThat(batch.getSuccessCount()).isEqualTo(1);
        assertThat(batch.getFailureCount()).isEqualTo(1);
        assertThat(batch.getResults()).extracting(PositionMonitorResultDTO::getPositionId).containsExactly(21L);
        assertThat(batch.getFailures()).hasSize(1);
        verify(positionMonitorLogService).recordMonitorRun(any());
    }

    @Test
    void activeBlockingExternalContextMakesHighRiskReviewWithoutPositionMutation() {
        service = new PositionMonitorServiceImpl(
                userPositionMapper,
                marketQuoteClient,
                userPositionRiskAdapter,
                executionPlanMapper,
                positionMonitorLogService,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                new ObjectMapper(),
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

        PositionMonitorResultDTO result = service.monitorUserPosition(30L);

        assertThat(result.getLogicStatus()).isEqualTo("HIGH_RISK");
        assertThat(result.getSuggestedAction()).isEqualTo("RISK_REVIEW");
        assertThat(result.getExternalContextBlocked()).isTrue();
        assertThat(result.getReasonCodes()).contains(ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        assertSafetyFields(result);
        verify(userPositionMapper, never()).manualClose(anyLong(), any(), any(), any(), any());
    }

    private void arrange(UserPositionDO position,
                         String currentPrice,
                         UserPositionRiskResult risk,
                         ExecutionPlanDO plan) {
        when(userPositionMapper.selectById(position.getId())).thenReturn(position);
        when(marketQuoteClient.fetch24hTicker(position.getAssetSymbol())).thenReturn(Optional.of(quote(currentPrice)));
        when(userPositionRiskAdapter.currentRisk()).thenReturn(risk);
        if (plan != null) {
            when(executionPlanMapper.selectByPlanId(plan.getPlanId())).thenReturn(plan);
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
        row.setAssetSymbol(id == 22L ? "ETH" : "BTC");
        row.setSide(side);
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100"));
        row.setQuantity(new BigDecimal("1"));
        row.setLeverage(new BigDecimal("2"));
        row.setStopLoss(stopLoss == null ? null : new BigDecimal(stopLoss));
        row.setTakeProfit(takeProfit == null ? null : new BigDecimal(takeProfit));
        row.setSourceType("MANUAL");
        row.setSourceRefId(sourceRefId);
        return row;
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
        return plan;
    }

    private static PositionMonitorLogDTO previousLog(String riskLevel) {
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogId(1L);
        dto.setRiskLevel(riskLevel);
        return dto;
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
