package org.example.trademodel.stress;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.RecheckStatusEnum;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.AiConflictResult;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.ConfusedResult;
import org.example.trademodel.service.ConfusedStateService;
import org.example.trademodel.service.DecisionContext;
import org.example.trademodel.service.DecisionEngineService;
import org.example.trademodel.service.DecisionOhlcvSnapshotSource;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.impl.PositionMonitorServiceImpl;
import org.example.trademodel.service.impl.PushRecheckServiceImpl;
import org.example.trademodel.service.impl.UserPositionServiceImpl;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.RuleConfigContractService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.example.trademodel.vo.UserPositionVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("business-stress")
class V1BusinessStressTest {

    @Test
    void opportunityDiscoveryExecutionPlanAndRecheckScenariosStayDeterministicAndReviewOnly() {
        List<OpportunityScenario> scenarios = List.of(
                OpportunityScenario.valid("BULLISH_BREAKOUT_VALID", "BTCUSDT", "BULLISH", AssetStateEnum.CANDIDATE),
                OpportunityScenario.valid("BULLISH_PULLBACK_VALID", "ETHUSDT", "BULLISH", AssetStateEnum.WAITING_TRIGGER),
                OpportunityScenario.valid("BEARISH_BREAKDOWN_VALID", "SOLUSDT", "BEARISH", AssetStateEnum.CANDIDATE),
                OpportunityScenario.noTrade("NOISY_RANGE_NO_TRADE", "BNBUSDT", "BEARISH", "TREND_STRUCTURE_SCORE_INSUFFICIENT"),
                OpportunityScenario.highRisk("HIGH_RISK_BLOCKED", "XRPUSDT"),
                OpportunityScenario.conflicted("CONFLICTED_AI_OR_RULES", "DOGEUSDT")
        );

        List<OpportunityResult> results = scenarios.stream().map(this::runOpportunityScenario).toList();

        assertThat(results).hasSize(6);
        assertThat(results.stream().filter(OpportunityResult::validOpportunityDetected).count()).isEqualTo(3);
        assertThat(results.stream().filter(OpportunityResult::falsePositive).count()).isZero();
        assertThat(results.stream().filter(OpportunityResult::missedValidOpportunity).count()).isZero();
        assertThat(results.stream().filter(OpportunityResult::conflictDowngrade).count()).isEqualTo(1);
        assertThat(results.stream().filter(OpportunityResult::highRiskBlock).count()).isEqualTo(1);
        assertThat(results.stream().filter(OpportunityResult::confusedBlock).count()).isEqualTo(1);

        for (OpportunityResult result : results) {
            if (result.scenario().expectManualPlanComplete()) {
                assertCompleteManualPlan(result.plan());
            } else {
                assertThat(manualPlanComplete(result.plan())).as(result.scenario().name()).isFalse();
            }
            assertPlanSafety(result.plan());
        }

        RecheckResult drifted = runPriceDriftRecheck();
        assertThat(drifted.getRecheckStatus()).isEqualTo(RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE);
        assertThat(drifted.isValid()).isFalse();
        assertThat(drifted.isReviewPassed()).isFalse();
        assertSafeRecheck(drifted);
    }

    @Test
    void manualPaperPositionMonitoringScenariosProduceUsefulManualReviewSignalsWithoutMutatingPositions() {
        MonitorHarness harness = new MonitorHarness();

        PositionMonitorResultDTO valid = harness.monitor(MonitorScenario.valid(101L, "ENTRY_LOGIC_STILL_VALID", "100", "90", "120"));
        assertThat(valid.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(valid.getDirectionSupportStatus()).isEqualTo("SUPPORTED");
        assertThat(valid.getReversalStatus()).isEqualTo("NO_REVERSAL_SIGNAL");
        assertThat(valid.getSuggestedManualActionText()).contains("人工");
        assertThat(valid.getPnlAmount()).isEqualByComparingTo("0");
        assertSafeMonitor(valid);

        PositionMonitorResultDTO weakened = harness.monitor(MonitorScenario.valid(102L, "ENTRY_LOGIC_WEAKENS_AFTER_OPEN", "100", "99", "120"));
        assertThat(weakened.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(weakened.getEntryLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(weakened.getDirectionSupportStatus()).isEqualTo("WEAKENED");
        assertThat(weakened.getSuggestedManualAction()).isEqualTo("MANUAL_REVIEW");
        assertThat(weakened.getReasonCodes()).contains("NEAR_STOP_LOSS");

        PositionMonitorResultDTO reversal = harness.monitor(MonitorScenario.valid(103L, "STRONG_REVERSAL_AFTER_OPEN", "89", "90", "120"));
        assertThat(reversal.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(reversal.getReversalStatus()).isEqualTo("MANUAL_REVIEW_REQUIRED");
        assertThat(reversal.getSuggestedManualAction()).isEqualTo("RECHECK_PLAN");
        assertThat(reversal.isStopLossBreached()).isTrue();

        PositionMonitorResultDTO takeProfit = harness.monitor(MonitorScenario.valid(104L, "HIT_TAKE_PROFIT_ZONE", "118", "90", "120"));
        assertThat(takeProfit.isTakeProfitReached()).isFalse();
        assertThat(takeProfit.isNearTakeProfit()).isTrue();
        assertThat(takeProfit.getSuggestedManualAction()).isEqualTo("MANUAL_REVIEW");
        assertThat(takeProfit.getReasonCodes()).contains("NEAR_TAKE_PROFIT");

        PositionMonitorResultDTO stopZone = harness.monitor(MonitorScenario.valid(105L, "HIT_STOP_ZONE", "90", "90", "120"));
        assertThat(stopZone.isStopLossBreached()).isTrue();
        assertThat(stopZone.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(stopZone.getSuggestedManualActionText()).contains("复核");

        PositionMonitorResultDTO highRisk = harness.monitor(MonitorScenario.highRisk(106L, "HIGH_RISK_OR_CONFLICT_BLOCKED", "100", "90", "120"));
        assertThat(highRisk.getLogicStatus()).isEqualTo("HIGH_RISK");
        assertThat(highRisk.getRiskLevel()).isEqualTo("HIGH");
        assertThat(highRisk.getSuggestedManualAction()).isEqualTo("RISK_REVIEW");
        assertThat(highRisk.getDirectionSupportStatus()).isEqualTo("RISK_BLOCKED");

        assertThatThrownBy(() -> harness.monitorClosedPosition(107L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OPEN or PARTIALLY_CLOSED");
        PositionMonitorBatchResultDTO afterClose = harness.monitorOpenPositionsAfterClose();
        assertThat(afterClose.getTotalCount()).isZero();
        assertThat(afterClose.getSuccessCount()).isZero();
        assertThat(afterClose.getFailureCount()).isZero();

        assertThat(harness.recordedLogs()).hasSize(6);
        assertThat(harness.recordedLogs()).extracting(PositionMonitorLogDTO::getLogicStatus)
                .contains("LOGIC_VALID", "LOGIC_WEAKENED", "PLAN_INVALIDATED", "HIGH_RISK");
        verify(harness.userPositionMapper(), never()).manualClose(anyLong(), any(), any(), anyString(), any());
    }

    @Test
    void manualPaperOpenCloseLifecycleKeepsExecutionPlanSeparateFromUserPositionAndReviewOnly() throws Exception {
        UserPositionMapper mapper = mock(UserPositionMapper.class);
        Map<Long, UserPositionDO> positions = new LinkedHashMap<>();
        wireUserPositionMapper(mapper, positions);
        UserPositionServiceImpl userPositionService = new UserPositionServiceImpl(mapper);

        UserPositionVO opened = userPositionService.manualOpen(openPaperPositionRequest("plan-paper-loop"));

        assertThat(opened.getStatus()).isEqualTo("OPEN");
        assertThat(opened.getSourceType()).isEqualTo("MANUAL");
        assertThat(opened.getSourceRefId()).isEqualTo("plan-paper-loop");
        assertThat(opened.isNotTradeInstruction()).isTrue();
        assertThat(opened.isNotOrderExecution()).isTrue();
        assertThat(opened.isNotAutoTrading()).isTrue();
        assertThat(userPositionService.listOpenPositions()).extracting(UserPositionVO::getId).containsExactly(opened.getId());

        UserPositionVO closed = userPositionService.manualClose(opened.getId(), closePaperPositionRequest());

        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getClosePrice()).isEqualByComparingTo("112");
        assertThat(userPositionService.listOpenPositions()).isEmpty();
        assertThat(closed.isNotTradeInstruction()).isTrue();
        assertThat(closed.isNotOrderExecution()).isTrue();
        assertThat(closed.isNotAutoTrading()).isTrue();

        verify(mapper).insert(any(UserPositionDO.class));
        verify(mapper).manualClose(eq(opened.getId()), any(), eq(new BigDecimal("112")), eq("paper stress close"), any());
        assertNoForbiddenExecutableFields(UserPositionVO.class, PositionMonitorResultDTO.class, RecheckResult.class);
    }

    private OpportunityResult runOpportunityScenario(OpportunityScenario scenario) {
        DecisionOhlcvSnapshotSource fetcher = mock(DecisionOhlcvSnapshotSource.class);
        AiConflictResolverService conflictResolver = mock(AiConflictResolverService.class);
        ConfusedStateService confusedStateService = mock(ConfusedStateService.class);
        AssetStateService assetStateService = mock(AssetStateService.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of());
        when(fetcher.readClosedBars(eq(scenario.symbol()), eq("5m"), anyInt(), anyString())).thenReturn(scenario.klines1m());
        when(fetcher.readClosedBars(eq(scenario.symbol()), matches("15m|1h|4h"), anyInt(), anyString()))
                .thenReturn(scenario.klines5m());
        when(conflictResolver.resolve(any(DecisionContext.class))).thenReturn(scenario.conflictResult());
        when(confusedStateService.calculateConfused(eq(scenario.symbol()), any(DecisionContext.class)))
                .thenReturn(scenario.confusedResult());
        when(assetStateService.buildSnapshotAtDecision(anyString(), anyString(), any(), any(), anyInt(), anyInt(), any(Boolean.class), any(Boolean.class)))
                .thenAnswer(invocation -> "{\"previousState\":\"" + ((AssetStateEnum) invocation.getArgument(2)).name()
                        + "\",\"nextState\":\"" + ((AssetStateEnum) invocation.getArgument(3)).name() + "\"}");

        DecisionEngineService service = new DecisionEngineService(
                fetcher, conflictResolver, confusedStateService, assetStateService, ruleConfigService, null);
        DecisionBundleVO decision = service.makeDecision(
                scenario.symbol(), "5m", "analysis-" + scenario.name(), scenario.dataQualityScore(),
                scenario.trendStructureScore(), scenario.externalContext());
        ExecutionPlanDO plan = scenario.toPlan(decision);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo(scenario.expectedDirection());
        assertThat(decision.getIsWorthOpening()).isEqualTo(scenario.expectWorthOpening());
        assertThat(decision.getAssetState()).isEqualTo(scenario.expectedState());
        assertThat(decision.getAiRoleResults()).contains("RULE_ONLY_FALLBACK");
        verify(fetcher).readClosedBars(eq(scenario.symbol()), eq("5m"), eq(3), anyString());
        verify(fetcher).readClosedBars(eq(scenario.symbol()), eq("15m"), eq(3), anyString());
        verify(fetcher).readClosedBars(eq(scenario.symbol()), eq("1h"), eq(3), anyString());
        verify(fetcher).readClosedBars(eq(scenario.symbol()), eq("4h"), eq(3), anyString());
        return new OpportunityResult(scenario, decision, plan);
    }

    private RecheckResult runPriceDriftRecheck() {
        PushSnapshotMapper pushSnapshotMapper = mock(PushSnapshotMapper.class);
        AccountRiskSnapshotMapper accountRiskSnapshotMapper = mock(AccountRiskSnapshotMapper.class);
        PushRecheckLogMapper pushRecheckLogMapper = mock(PushRecheckLogMapper.class);
        PushRecheckDispatchConfigService dispatchConfigService = mock(PushRecheckDispatchConfigService.class);
        UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        MarketQuoteClient marketQuoteClient = mock(MarketQuoteClient.class);
        RuleConfigContractService ruleConfigContractService = mock(RuleConfigContractService.class);
        when(riskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        when(ruleConfigContractService.requirePushRecheckThresholds())
                .thenReturn(new RuleConfigContractService.PushRecheckThresholds(new BigDecimal("0.02"), 70, 85, 60));
        TmPushSnapshotDO snapshot = basePushSnapshot();
        snapshot.setTriggerPrice(new BigDecimal("100"));
        snapshot.setConfusedScoreSnapshot(10);
        snapshot.setDataQualityScoreSnapshot(88);
        when(pushSnapshotMapper.selectByPushId(701L)).thenReturn(snapshot);
        PushRecheckServiceImpl service = new PushRecheckServiceImpl(pushSnapshotMapper, accountRiskSnapshotMapper,
                pushRecheckLogMapper, dispatchConfigService, riskAdapter,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                ruleConfigContractService);

        RecheckResult result = service.recheck(701L, new BigDecimal("110"));

        verifyNoInteractions(marketQuoteClient);
        verify(pushSnapshotMapper).updatePushStatus(701L, "RECHECK_DRIFTED_FROM_ENTRY_ZONE");
        return result;
    }

    private static TmPushSnapshotDO basePushSnapshot() {
        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setPushId(701L);
        snapshot.setAnalysisId("analysis-price-drift-after-signal");
        snapshot.setSymbol("BTCUSDT");
        snapshot.setTimeframe("5m");
        snapshot.setPushType("OPPORTUNITY_REVIEW");
        snapshot.setPushStatus("CAPTURED");
        snapshot.setPushCreateTime(LocalDateTime.now());
        snapshot.setRuleVersion("rules-stress");
        snapshot.setEntryZoneJson("{\"lower\":99,\"upper\":101}");
        snapshot.setStopZoneJson("{\"stop\":94}");
        snapshot.setInvalidationConditionJson("{\"text\":\"drift invalidates original review\"}");
        snapshot.setPlanModeSnapshot("REVIEW_ONLY_PLAN");
        snapshot.setCauseEffectAlignmentSnapshot("ALIGNED");
        snapshot.setExecutionFeasibilitySnapshot(88);
        snapshot.setDataQualityScoreSnapshot(88);
        snapshot.setConfusedScoreSnapshot(10);
        snapshot.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        snapshot.setTraceId("trace-price-drift-after-signal");
        snapshot.setCreateTime(LocalDateTime.now());
        return snapshot;
    }

    private static void assertCompleteManualPlan(ExecutionPlanDO plan) {
        assertThat(manualPlanComplete(plan)).isTrue();
        assertThat(plan.getRecommendedAction()).contains("REVIEW");
        assertThat(plan.getPlanMode()).isEqualTo("REVIEW_ONLY_PLAN");
    }

    private static boolean manualPlanComplete(ExecutionPlanDO plan) {
        return plan != null
                && "VALID".equals(plan.getExecutionPlanStatus())
                && Boolean.TRUE.equals(plan.getSourceGateComplete())
                && hasText(plan.getEntryZone())
                && hasText(plan.getStopLoss())
                && hasText(plan.getTakeProfitRules())
                && hasText(plan.getInvalidCondition())
                && hasText(plan.getSourceCompletenessSummary())
                && Boolean.TRUE.equals(plan.getManualReviewRequired());
    }

    private static void assertPlanSafety(ExecutionPlanDO plan) {
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !"暂无".equals(value);
    }

    private static void assertSafeMonitor(PositionMonitorResultDTO dto) {
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

    private static void assertSafeRecheck(RecheckResult result) {
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
        assertThat(result.isNotPositionMutation()).isTrue();
        assertThat(result.isNotTradingAuthorization()).isTrue();
    }

    private static void assertNoForbiddenExecutableFields(Class<?>... types) throws Exception {
        Set<String> forbiddenFields = Set.of(
                "orderAction", "executionAction", "autoTradingAction", "buyAction", "sellAction",
                "executeAction", "tradeAllowed", "orderAllowed", "openAllowed", "closeAllowed",
                "autoOpenAllowed", "autoCloseAllowed", "pushRecheckCreatedUserPosition");
        for (Class<?> type : types) {
            Set<String> fieldNames = java.util.Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                    .map(PropertyDescriptor::getName)
                    .collect(Collectors.toSet());
            assertThat(fieldNames).doesNotContainAnyElementsOf(forbiddenFields);
        }
    }

    private static List<String[]> bullishKlines() {
        return List.of(kline("100", "105"), kline("104", "108"), kline("107", "112"));
    }

    private static List<String[]> bearishKlines() {
        return List.of(kline("112", "107"), kline("108", "104"), kline("105", "100"));
    }

    private static String[] kline(String open, String close) {
        BigDecimal openValue = new BigDecimal(open);
        BigDecimal closeValue = new BigDecimal(close);
        return new String[]{"0", open, openValue.max(closeValue).add(BigDecimal.ONE).toPlainString(),
                openValue.min(closeValue).subtract(BigDecimal.ONE).toPlainString(), close};
    }

    private static EventImpactInputVO blockedExternalContext() {
        EventImpactInputVO input = new EventImpactInputVO();
        input.setExternalContextStatus("BLOCKED");
        input.setExternalContextBlocked(true);
        input.setExternalContextRiskLevel("HIGH");
        input.setExternalContextSourceHealth(ExternalContextPolicy.SOURCE_HEALTH_OK);
        input.setActiveExternalEventCount(1);
        input.setExternalContextReasonCodes(List.of(ExternalContextPolicy.REASON_WINDOW_BLOCKED));
        return input;
    }

    private static ExecutionPlanDO completePlan(String scenarioName, DecisionBundleVO decision) {
        ExecutionPlanDO plan = basePlan(scenarioName, decision);
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setRecommendedAction("REVIEW_ONLY_PLAN");
        plan.setEntryZone(decision.getMarketBiasHierarchy() + " entry zone from SYNTHETIC_SCENARIO_DATA");
        plan.setStopLoss(decision.getMarketBiasHierarchy() + " stop zone from SYNTHETIC_SCENARIO_DATA");
        plan.setTakeProfitRules("staged manual take-profit review from SYNTHETIC_SCENARIO_DATA");
        plan.setLeverageSuggestion("manual review only");
        plan.setPositionSuggestion("paper position sizing only");
        plan.setInvalidCondition("synthetic boundary invalidation");
        plan.setSourceCompletenessSummary("source trace complete for deterministic stress fixture");
        return plan;
    }

    private static ExecutionPlanDO incompletePlan(String scenarioName, DecisionBundleVO decision) {
        ExecutionPlanDO plan = basePlan(scenarioName, decision);
        plan.setExecutionPlanStatus("INCOMPLETE");
        plan.setSourceGateStatus("INCOMPLETE");
        plan.setSourceGateComplete(false);
        plan.setRecommendedAction("MANUAL_REVIEW_REQUIRED");
        plan.setEntryZone(null);
        plan.setStopLoss(null);
        plan.setTakeProfitRules(null);
        plan.setInvalidCondition(null);
        plan.setSourceCompletenessSummary("boundary incomplete; no manual plan complete");
        return plan;
    }

    private static ExecutionPlanDO basePlan(String scenarioName, DecisionBundleVO decision) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-" + scenarioName);
        plan.setAnalysisId("analysis-" + scenarioName);
        plan.setPlanMode("REVIEW_ONLY_PLAN");
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        plan.setCreateTime(LocalDateTime.now());
        plan.setAccountRiskJson("{\"riskLevel\":\"" + decision.getRiskLevel() + "\"}");
        return plan;
    }

    private static UserPositionDO position(Long id, String status, String sourceRefId, String currentSymbol,
                                           String stopLoss, String takeProfit) {
        UserPositionDO position = new UserPositionDO();
        position.setId(id);
        position.setAssetSymbol(currentSymbol);
        position.setSide("LONG");
        position.setStatus(status);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        position.setLeverage(new BigDecimal("2"));
        position.setStopLoss(new BigDecimal(stopLoss));
        position.setTakeProfit(new BigDecimal(takeProfit));
        position.setSourceType("MANUAL");
        position.setSourceRefId(sourceRefId);
        position.setNotTradeInstruction(true);
        position.setNotAutoTrading(true);
        position.setNotOrderExecution(true);
        return position;
    }

    private static ExecutionPlanDO monitorPlan(String planId) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(planId);
        plan.setAnalysisId("analysis-" + planId);
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        return plan;
    }

    private static MarketQuoteSnapshot quote(String symbol, String price) {
        MarketQuoteSnapshot snapshot = new MarketQuoteSnapshot();
        snapshot.setProvider("SYNTHETIC_SCENARIO_DATA");
        snapshot.setSymbolNormalized(symbol);
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

    private static CreateUserPositionReq openPaperPositionRequest(String planId) {
        CreateUserPositionReq req = new CreateUserPositionReq();
        req.setAssetSymbol("BTCUSDT");
        req.setSide("LONG");
        req.setEntryPrice(new BigDecimal("100"));
        req.setQuantity(new BigDecimal("0.20"));
        req.setLeverage(new BigDecimal("2"));
        req.setStopLoss(new BigDecimal("90"));
        req.setTakeProfit(new BigDecimal("120"));
        req.setSourceType("MANUAL");
        req.setSourceRefId(planId);
        return req;
    }

    private static CloseUserPositionReq closePaperPositionRequest() {
        CloseUserPositionReq req = new CloseUserPositionReq();
        req.setClosePrice(new BigDecimal("112"));
        req.setCloseReason("paper stress close");
        return req;
    }

    private static void wireUserPositionMapper(UserPositionMapper mapper, Map<Long, UserPositionDO> positions) {
        AtomicLong ids = new AtomicLong(9000L);
        doAnswer(invocation -> {
            UserPositionDO row = invocation.getArgument(0);
            row.setId(ids.incrementAndGet());
            positions.put(row.getId(), row);
            return 1;
        }).when(mapper).insert(any(UserPositionDO.class));
        when(mapper.selectById(anyLong())).thenAnswer(invocation -> positions.get(invocation.getArgument(0)));
        when(mapper.listOpenPositions()).thenAnswer(invocation -> positions.values().stream()
                .filter(row -> Set.of("OPEN", "PARTIALLY_CLOSED").contains(row.getStatus()))
                .sorted(Comparator.comparing(UserPositionDO::getId))
                .collect(Collectors.toList()));
        when(mapper.manualClose(anyLong(), any(), any(), anyString(), any()))
                .thenAnswer(invocation -> {
                    UserPositionDO row = positions.get(invocation.getArgument(0));
                    if (row == null || !"OPEN".equals(row.getStatus())) {
                        return 0;
                    }
                    row.setClosedAt(invocation.getArgument(1));
                    row.setClosePrice(invocation.getArgument(2));
                    row.setCloseReason(invocation.getArgument(3));
                    row.setUpdatedAt(invocation.getArgument(4));
                    row.setStatus("CLOSED");
                    return 1;
                });
    }

    private record OpportunityScenario(String name,
                                       String symbol,
                                       String expectedDirection,
                                       boolean expectWorthOpening,
                                       boolean expectManualPlanComplete,
                                       AssetStateEnum expectedState,
                                       Integer dataQualityScore,
                                       Integer trendStructureScore,
                                       List<String[]> klines1m,
                                       List<String[]> klines5m,
                                       AiConflictResult conflictResult,
                                       ConfusedResult confusedResult,
                                       EventImpactInputVO externalContext,
                                       String expectedBlockReason) {

        private static OpportunityScenario valid(String name, String symbol, String direction, AssetStateEnum state) {
            boolean bullish = "BULLISH".equals(direction);
            return new OpportunityScenario(name, symbol, direction, true, true, state, 88, 70,
                    bullish ? bullishKlines() : bearishKlines(), bullish ? bullishKlines() : bearishKlines(),
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_1_CONSISTENT, direction, "HIGH", "CONFIRM", 20),
                    new ConfusedResult(20, "OBSERVING", state.name(), false, false, 0, false, "none", "stable"),
                    null, null);
        }

        private static OpportunityScenario noTrade(String name, String symbol, String direction, String reason) {
            return new OpportunityScenario(name, symbol, direction, false, false, AssetStateEnum.OBSERVING, 88, 45,
                    bullishKlines(), bearishKlines(),
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_2_LIGHT_DIVERGENCE, direction, "MEDIUM", "REVIEW", 40),
                    new ConfusedResult(45, "OBSERVING", "OBSERVING", false, false, 0, false, reason, "no-trade"),
                    null, reason);
        }

        private static OpportunityScenario highRisk(String name, String symbol) {
            return new OpportunityScenario(name, symbol, "BULLISH", false, false, AssetStateEnum.HIGH_RISK, 88, 70,
                    bullishKlines(), bullishKlines(),
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_1_CONSISTENT, "BULLISH", "MEDIUM", "CONFIRM", 20),
                    new ConfusedResult(20, "OBSERVING", "CANDIDATE", false, false, 0, false, "external", "risk"),
                    blockedExternalContext(), ExternalContextPolicy.REASON_WINDOW_BLOCKED);
        }

        private static OpportunityScenario conflicted(String name, String symbol) {
            return new OpportunityScenario(name, symbol, "BULLISH", false, false, AssetStateEnum.CONFUSED, 88, 70,
                    bullishKlines(), bullishKlines(),
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE, "BULLISH", "LOW", "HIGH",
                            "CONFUSED", 92, 3, false, 92),
                    new ConfusedResult(85, "OBSERVING", "CONFUSED", true, false, 0, true,
                            "extreme conflict", "block"),
                    null, "CONFUSED_SCORE_BLOCK_THRESHOLD");
        }

        private ExecutionPlanDO toPlan(DecisionBundleVO decision) {
            return expectManualPlanComplete ? completePlan(name, decision) : incompletePlan(name, decision);
        }
    }

    private record OpportunityResult(OpportunityScenario scenario, DecisionBundleVO decision, ExecutionPlanDO plan) {
        private boolean validOpportunityDetected() {
            return scenario.expectWorthOpening() && Boolean.TRUE.equals(decision.getIsWorthOpening()) && manualPlanComplete(plan);
        }

        private boolean falsePositive() {
            return !scenario.expectWorthOpening() && (Boolean.TRUE.equals(decision.getIsWorthOpening()) || manualPlanComplete(plan));
        }

        private boolean missedValidOpportunity() {
            return scenario.expectWorthOpening() && (!Boolean.TRUE.equals(decision.getIsWorthOpening()) || !manualPlanComplete(plan));
        }

        private boolean conflictDowngrade() {
            return scenario.name().equals("CONFLICTED_AI_OR_RULES")
                    && "CONFUSED".equals(decision.getAiPlanMode())
                    && !Boolean.TRUE.equals(decision.getIsWorthOpening());
        }

        private boolean highRiskBlock() {
            return scenario.name().equals("HIGH_RISK_BLOCKED") && "HIGH".equals(decision.getRiskLevel());
        }

        private boolean confusedBlock() {
            return scenario.name().equals("CONFLICTED_AI_OR_RULES") && decision.isDirectionalPushBlocked();
        }
    }

    private record MonitorScenario(Long id,
                                   String name,
                                   String currentPrice,
                                   String stopLoss,
                                   String takeProfit,
                                   UserPositionRiskResult risk) {
        private static MonitorScenario valid(Long id, String name, String currentPrice, String stopLoss, String takeProfit) {
            return new MonitorScenario(id, name, currentPrice, stopLoss, takeProfit, V1BusinessStressTest.risk("LOW", false));
        }

        private static MonitorScenario highRisk(Long id, String name, String currentPrice, String stopLoss, String takeProfit) {
            return new MonitorScenario(id, name, currentPrice, stopLoss, takeProfit, V1BusinessStressTest.risk("HIGH", true));
        }
    }

    private static final class MonitorHarness {
        private final UserPositionMapper userPositionMapper = mock(UserPositionMapper.class);
        private final MarketQuoteClient marketQuoteClient = mock(MarketQuoteClient.class);
        private final UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        private final ExecutionPlanMapper executionPlanMapper = mock(ExecutionPlanMapper.class);
        private final InMemoryMonitorLogService monitorLogService = new InMemoryMonitorLogService();
        private final PositionMonitorServiceImpl service;

        private MonitorHarness() {
            service = new PositionMonitorServiceImpl(userPositionMapper,
                    org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                    riskAdapter, executionPlanMapper,
                    monitorLogService, mock(EvidenceItemMapper.class), mock(ScoreItemMapper.class),
                    mock(DecisionResultMapper.class), new ObjectMapper(), null);
        }

        private PositionMonitorResultDTO monitor(MonitorScenario scenario) {
            String planId = "plan-" + scenario.name();
            UserPositionDO position = position(scenario.id(), "OPEN", planId, "BTCUSDT", scenario.stopLoss(), scenario.takeProfit());
            when(userPositionMapper.selectById(scenario.id())).thenReturn(position);
            when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(quote("BTCUSDT", scenario.currentPrice())));
            when(riskAdapter.currentRisk()).thenReturn(scenario.risk());
            lenient().when(executionPlanMapper.selectByPlanId(planId)).thenReturn(monitorPlan(planId));
            return service.monitorUserPosition(scenario.id());
        }

        private void monitorClosedPosition(Long id) {
            when(userPositionMapper.selectById(id)).thenReturn(position(id, "CLOSED", "plan-closed", "BTCUSDT", "90", "120"));
            service.monitorUserPosition(id);
        }

        private PositionMonitorBatchResultDTO monitorOpenPositionsAfterClose() {
            when(userPositionMapper.listOpenPositions()).thenReturn(List.of());
            return service.monitorOpenUserPositions();
        }

        private List<PositionMonitorLogDTO> recordedLogs() {
            return monitorLogService.logs();
        }

        private UserPositionMapper userPositionMapper() {
            return userPositionMapper;
        }
    }

    private static final class InMemoryMonitorLogService implements org.example.trademodel.service.PositionMonitorLogService {
        private final AtomicLong ids = new AtomicLong(1000L);
        private final List<PositionMonitorLogDTO> logs = new ArrayList<>();

        @Override
        public PositionMonitorLogDTO recordMonitorRun(RecordPositionMonitorLogCommand command) {
            PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
            dto.setLogId(ids.incrementAndGet());
            dto.setPositionId(command.getPositionId());
            dto.setAnalysisId(command.getAnalysisId());
            dto.setExecutionPlanId(command.getExecutionPlanId());
            dto.setCurrentPrice(command.getCurrentPrice());
            dto.setLogicStatus(command.getLogicStatus());
            dto.setRiskLevel(command.getRiskLevel());
            dto.setSuggestedAction(command.getSuggestedAction());
            dto.setReason(command.getReason());
            dto.setTraceId(command.getTraceId());
            dto.setNotTradeInstruction(true);
            dto.setNotExecutable(true);
            dto.setNotAutoTrading(true);
            dto.setNotOrderExecution(true);
            dto.setNotAutoClose(true);
            dto.setNotAutoReverse(true);
            dto.setNotPositionMutation(true);
            dto.setCreatedAt(LocalDateTime.now());
            logs.add(dto);
            return dto;
        }

        @Override
        public PositionMonitorLogDTO findById(Long logId) {
            return logs.stream().filter(log -> log.getLogId().equals(logId)).findFirst().orElse(null);
        }

        @Override
        public List<PositionMonitorLogDTO> listByPositionId(Long positionId, Integer limit) {
            return logs.stream()
                    .filter(log -> log.getPositionId().equals(positionId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt).reversed())
                    .limit(limit == null ? logs.size() : limit)
                    .collect(Collectors.toList());
        }

        @Override
        public List<PositionMonitorLogDTO> listAllByPositionIdForReview(Long positionId) {
            return logs.stream()
                    .filter(log -> log.getPositionId().equals(positionId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt))
                    .collect(Collectors.toList());
        }

        @Override
        public List<PositionMonitorLogDTO> listByAnalysisId(String analysisId, Integer limit) {
            return logs.stream()
                    .filter(log -> analysisId.equals(log.getAnalysisId()))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt).reversed())
                    .limit(limit == null ? logs.size() : limit)
                    .collect(Collectors.toList());
        }

        private List<PositionMonitorLogDTO> logs() {
            return List.copyOf(logs);
        }
    }
}
