package org.example.trademodel.stress;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.AiConflictResult;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.ConfusedResult;
import org.example.trademodel.service.ConfusedStateService;
import org.example.trademodel.service.DecisionContext;
import org.example.trademodel.service.DecisionEngineService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.RecheckResult;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.impl.PositionMonitorServiceImpl;
import org.example.trademodel.service.impl.PushRecheckServiceImpl;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.RuleConfigContractService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("historical-replay")
class V1HistoricalReplayValidationTest {

    private static final String FIXTURE_SOURCE = "LOCAL_REPLAY_FIXTURE_NOT_PROVIDER";
    private static final String SYMBOL = "BTCUSDT";

    @Test
    void replayStylePathsKeepOpportunityAndPlanDecisionsFailClosed() {
        List<ReplayResult> results = replayScenarios().stream().map(this::runScenario).toList();

        assertThat(results).hasSize(9);
        assertThat(results.stream().filter(ReplayResult::validOpportunityDetected).count()).isEqualTo(5);
        assertThat(results.stream().filter(ReplayResult::falsePositive).count()).isZero();
        assertThat(results.stream().filter(ReplayResult::missedValidOpportunity).count()).isZero();
        assertResult(results, "HISTORICAL_STYLE_UPTREND_BREAKOUT", "BULLISH", true, AssetStateEnum.CANDIDATE);
        assertResult(results, "HISTORICAL_STYLE_DOWNTREND_BREAKDOWN", "BEARISH", true, AssetStateEnum.CANDIDATE);
        assertResult(results, "CHOPPY_RANGE_NO_TRADE", "BULLISH", false, AssetStateEnum.OBSERVING);
        assertResult(results, "WICK_STOP_SWEEP", "BEARISH", false, AssetStateEnum.OBSERVING);
        assertResult(results, "FAST_CRASH_REBOUND", "BULLISH", false, AssetStateEnum.CONFUSED);
        assertResult(results, "SLOW_TREND_PULLBACK", "BULLISH", true, AssetStateEnum.WAITING_TRIGGER);
        assertResult(results, "HIGH_RISK_EVENT_WINDOW", "BULLISH", false, AssetStateEnum.HIGH_RISK);

        for (ReplayResult result : results) {
            assertThat(result.decision().getAiRoleResults()).contains("RULE_ONLY_FALLBACK");
            assertThat(result.plan().getAccountRiskJson()).contains(result.decision().getRiskLevel());
            assertPlanSafety(result.plan());
            if (result.scenario().completePlanExpected()) {
                assertCompletePlan(result.plan());
            } else {
                assertThat(isCompletePlan(result.plan())).as(result.scenario().name()).isFalse();
                assertThat(result.plan().getExecutionPlanStatus()).isEqualTo("INCOMPLETE");
            }
        }

        RecheckResult invalidated = freshnessRecheck("FAKE_BREAKOUT_REVERSAL", new BigDecimal("92"), true);
        assertThat(invalidated.getRecheckStatus()).isEqualTo(RecheckStatusEnum.INVALIDATED);
        assertSafeRecheck(invalidated);
        RecheckResult drifted = freshnessRecheck("PRICE_DRIFT_AFTER_SIGNAL", new BigDecimal("110"), false);
        assertThat(drifted.getRecheckStatus()).isEqualTo(RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE);
        assertSafeRecheck(drifted);

        System.out.println("HISTORICAL_REPLAY_OPPORTUNITY_SUMMARY scenarios=9 valid=5 false_positive=0 false_negative=0 source="
                + FIXTURE_SOURCE);
    }

    @Test
    void replayPaperPositionsCoverValidWeakenedReversalTpStopAndClosedExclusion() {
        MonitorHarness harness = new MonitorHarness();

        PositionMonitorResultDTO valid = harness.monitor(MonitorPoint.normal(201L, "OPEN_LOGIC_VALID", "101", "90", "120"));
        assertThat(valid.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(valid.getDirectionSupportStatus()).isEqualTo("SUPPORTED");
        assertThat(valid.getReversalStatus()).isEqualTo("NO_REVERSAL_SIGNAL");
        assertSafeMonitor(valid);

        PositionMonitorResultDTO weakened = harness.monitor(MonitorPoint.normal(202L, "FAKE_BREAKOUT_WEAKENED", "99", "98", "120"));
        assertThat(weakened.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(weakened.getDirectionSupportStatus()).isEqualTo("WEAKENED");
        assertThat(weakened.getSuggestedManualAction()).isEqualTo("MANUAL_REVIEW");
        assertThat(weakened.getReasonCodes()).contains("NEAR_STOP_LOSS");
        assertSafeMonitor(weakened);

        PositionMonitorResultDTO reversal = harness.monitor(MonitorPoint.normal(203L, "STRONG_REVERSAL", "89", "90", "120"));
        assertThat(reversal.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(reversal.getReversalStatus()).isEqualTo("MANUAL_REVIEW_REQUIRED");
        assertThat(reversal.getSuggestedManualAction()).isEqualTo("RECHECK_PLAN");
        assertSafeMonitor(reversal);

        PositionMonitorResultDTO takeProfit = harness.monitor(MonitorPoint.normal(204L, "PAPER_OPEN_TO_TAKE_PROFIT", "118", "90", "120"));
        assertThat(takeProfit.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(takeProfit.isNearTakeProfit()).isTrue();
        assertThat(takeProfit.getSuggestedManualAction()).isEqualTo("MANUAL_REVIEW");
        assertThat(takeProfit.getSuggestedManualActionText()).contains("人工");
        assertThat(takeProfit.getPnlPct()).isPositive();
        assertSafeMonitor(takeProfit);

        PositionMonitorResultDTO stop = harness.monitor(MonitorPoint.normal(205L, "PAPER_OPEN_TO_STOP_OR_INVALIDATION", "90", "90", "120"));
        assertThat(stop.getLogicStatus()).isEqualTo("PLAN_INVALIDATED");
        assertThat(stop.isStopLossBreached()).isTrue();
        assertThat(stop.getSuggestedManualActionText()).contains("复核");
        assertSafeMonitor(stop);

        PositionMonitorResultDTO highRisk = harness.monitor(MonitorPoint.highRisk(206L, "FAST_CRASH_HIGH_RISK", "94", "90", "120"));
        assertThat(highRisk.getLogicStatus()).isEqualTo("HIGH_RISK");
        assertThat(highRisk.getRiskLevel()).isEqualTo("HIGH");
        assertThat(highRisk.getSuggestedManualAction()).isEqualTo("RISK_REVIEW");
        assertSafeMonitor(highRisk);

        assertThatThrownBy(() -> harness.monitorClosed(207L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OPEN or PARTIALLY_CLOSED");
        PositionMonitorBatchResultDTO afterClose = harness.afterCloseBatch();
        assertThat(afterClose.getTotalCount()).isZero();
        assertThat(afterClose.getSuccessCount()).isZero();
        assertThat(afterClose.getFailureCount()).isZero();
        verify(harness.positionMapper, never()).manualClose(anyLong(), any(), any(), anyString(), any());

        System.out.println("HISTORICAL_REPLAY_MONITOR_SUMMARY valid=1 weakened=1 invalidated=2 take_profit_zone=1 high_risk=1 closed_active=0 source="
                + FIXTURE_SOURCE);
    }

    private ReplayResult runScenario(ReplayScenario scenario) {
        RealMarketDataFetcherService localAdapter = mock(RealMarketDataFetcherService.class);
        AiConflictResolverService conflictResolver = mock(AiConflictResolverService.class);
        ConfusedStateService confusedStateService = mock(ConfusedStateService.class);
        AssetStateService assetStateService = mock(AssetStateService.class);
        RuleConfigService ruleConfigService = mock(RuleConfigService.class);
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Map.of());
        when(localAdapter.fetchKlines(eq(scenario.symbol()), eq("1m"), anyInt())).thenReturn(klines(scenario.oneMinute()));
        when(localAdapter.fetchKlines(eq(scenario.symbol()), eq("5m"), anyInt())).thenReturn(klines(scenario.fiveMinute()));
        when(conflictResolver.resolve(any(DecisionContext.class))).thenReturn(scenario.conflict());
        when(confusedStateService.calculateConfused(eq(scenario.symbol()), any(DecisionContext.class)))
                .thenReturn(scenario.confused());
        when(assetStateService.buildSnapshotAtDecision(anyString(), anyString(), any(), any(), anyInt(), anyInt(), any(Boolean.class), any(Boolean.class)))
                .thenAnswer(invocation -> "{\"source\":\"" + FIXTURE_SOURCE + "\",\"nextState\":\""
                        + ((AssetStateEnum) invocation.getArgument(3)).name() + "\"}");

        DecisionEngineService engine = new DecisionEngineService(localAdapter, conflictResolver, confusedStateService,
                assetStateService, ruleConfigService, null);
        DecisionBundleVO decision = engine.makeDecision(scenario.symbol(), "5m", "replay-" + scenario.name(),
                scenario.dataQualityScore(), scenario.trendScore(), scenario.externalContext());
        ExecutionPlanDO plan = planFromReplay(scenario, decision);

        assertThat(decision.getMarketBiasHierarchy()).isEqualTo(scenario.direction());
        assertThat(decision.getIsWorthOpening()).isEqualTo(scenario.opportunityExpected());
        assertThat(decision.getAssetState()).isEqualTo(scenario.state());
        assertThat(decision.getAssetStateSnapshot()).contains(FIXTURE_SOURCE);
        verify(localAdapter).fetchKlines(scenario.symbol(), "1m", 3);
        verify(localAdapter).fetchKlines(scenario.symbol(), "5m", 3);
        return new ReplayResult(scenario, decision, plan);
    }

    private RecheckResult freshnessRecheck(String scenario, BigDecimal currentPrice, boolean invalidation) {
        PushSnapshotMapper snapshotMapper = mock(PushSnapshotMapper.class);
        PushRecheckLogMapper logMapper = mock(PushRecheckLogMapper.class);
        MarketQuoteClient quoteClient = mock(MarketQuoteClient.class);
        UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        RuleConfigContractService config = mock(RuleConfigContractService.class);
        TmPushSnapshotDO snapshot = pushSnapshot(scenario, invalidation);
        when(snapshotMapper.selectByPushId(snapshot.getPushId())).thenReturn(snapshot);
        when(riskAdapter.currentRisk()).thenReturn(UserPositionRiskResult.noOpenPosition(0));
        when(config.requirePushRecheckThresholds()).thenReturn(
                new RuleConfigContractService.PushRecheckThresholds(new BigDecimal("0.02"), 70, 85, 60));
        PushRecheckServiceImpl service = new PushRecheckServiceImpl(snapshotMapper,
                mock(AccountRiskSnapshotMapper.class), logMapper, mock(PushRecheckDispatchConfigService.class),
                riskAdapter, quoteClient, config);
        RecheckResult result = service.recheck(snapshot.getPushId(), currentPrice);
        verifyNoInteractions(quoteClient);
        verify(logMapper).insert(any());
        return result;
    }

    private static List<ReplayScenario> replayScenarios() {
        return List.of(
                ReplayScenario.valid("HISTORICAL_STYLE_UPTREND_BREAKOUT", "BTCUSDT", "BULLISH",
                        uptrend(), uptrend(), AssetStateEnum.CANDIDATE),
                ReplayScenario.valid("HISTORICAL_STYLE_DOWNTREND_BREAKDOWN", "ETHUSDT", "BEARISH",
                        downtrend(), downtrend(), AssetStateEnum.CANDIDATE),
                ReplayScenario.valid("FAKE_BREAKOUT_REVERSAL", "SOLUSDT", "BULLISH",
                        fakeBreakout(), fakeBreakout(), AssetStateEnum.CANDIDATE),
                ReplayScenario.noTrade("CHOPPY_RANGE_NO_TRADE", "BNBUSDT", "BULLISH",
                        choppy1m(), choppy5m(), AssetStateEnum.OBSERVING, "RANGE_NO_TRADE"),
                ReplayScenario.noTrade("WICK_STOP_SWEEP", "XRPUSDT", "BEARISH",
                        wick1m(), wick5m(), AssetStateEnum.OBSERVING, "WICK_RISK_REVIEW"),
                ReplayScenario.confused("FAST_CRASH_REBOUND", "DOGEUSDT", crashRebound(), crashRebound()),
                ReplayScenario.valid("SLOW_TREND_PULLBACK", "ADAUSDT", "BULLISH",
                        slowPullback(), slowPullback(), AssetStateEnum.WAITING_TRIGGER),
                ReplayScenario.highRisk("HIGH_RISK_EVENT_WINDOW", "AVAXUSDT", uptrend(), uptrend()),
                ReplayScenario.valid("PRICE_DRIFT_AFTER_SIGNAL", "LINKUSDT", "BULLISH",
                        uptrend(), uptrend(), AssetStateEnum.CANDIDATE));
    }

    private static void assertResult(List<ReplayResult> results, String name, String direction,
                                     boolean complete, AssetStateEnum state) {
        ReplayResult result = results.stream().filter(item -> name.equals(item.scenario().name())).findFirst().orElseThrow();
        assertThat(result.decision().getMarketBiasHierarchy()).isEqualTo(direction);
        assertThat(result.decision().getAssetState()).isEqualTo(state);
        assertThat(isCompletePlan(result.plan())).isEqualTo(complete);
    }

    private static ExecutionPlanDO planFromReplay(ReplayScenario scenario, DecisionBundleVO decision) {
        ExecutionPlanDO plan = basePlan(scenario, decision);
        if (!scenario.completePlanExpected() || !Boolean.TRUE.equals(decision.getIsWorthOpening())) {
            plan.setExecutionPlanStatus("INCOMPLETE");
            plan.setSourceGateStatus("INCOMPLETE");
            plan.setSourceGateComplete(false);
            plan.setRecommendedAction("MANUAL_REVIEW_REQUIRED");
            plan.setSourceCompletenessSummary("boundary incomplete in " + FIXTURE_SOURCE);
            return plan;
        }

        BigDecimal close = last(scenario.fiveMinute()).close();
        BigDecimal low = scenario.fiveMinute().stream().map(ReplayCandle::low).min(BigDecimal::compareTo).orElseThrow();
        BigDecimal high = scenario.fiveMinute().stream().map(ReplayCandle::high).max(BigDecimal::compareTo).orElseThrow();
        boolean bullish = "BULLISH".equals(decision.getMarketBiasHierarchy());
        BigDecimal stop = bullish ? low : high;
        BigDecimal distance = close.subtract(stop).abs();
        BigDecimal target = bullish ? close.add(distance.multiply(new BigDecimal("2")))
                : close.subtract(distance.multiply(new BigDecimal("2")));
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setRecommendedAction("MANUAL_REVIEW_ONLY");
        plan.setEntryZone(FIXTURE_SOURCE + " close=" + close);
        plan.setStopLoss(FIXTURE_SOURCE + " structural-stop=" + stop);
        plan.setTakeProfitRules(FIXTURE_SOURCE + " staged-target=" + target);
        plan.setLeverageSuggestion("manual risk review only");
        plan.setPositionSuggestion("paper position review only");
        plan.setInvalidCondition(FIXTURE_SOURCE + " invalid if structure crosses " + stop);
        plan.setSourceCompletenessSummary("direction, entry, stop, target, invalidation, risk and analysis trace present");
        return plan;
    }

    private static ExecutionPlanDO basePlan(ReplayScenario scenario, DecisionBundleVO decision) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-replay-" + scenario.name());
        plan.setAnalysisId("replay-" + scenario.name());
        plan.setPlanMode("REVIEW_ONLY_PLAN");
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        plan.setAccountRiskJson("{\"riskLevel\":\"" + decision.getRiskLevel() + "\",\"source\":\""
                + FIXTURE_SOURCE + "\"}");
        plan.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        return plan;
    }

    private static void assertCompletePlan(ExecutionPlanDO plan) {
        assertThat(isCompletePlan(plan)).isTrue();
        assertThat(plan.getRecommendedAction()).isEqualTo("MANUAL_REVIEW_ONLY");
        assertThat(plan.getPlanMode()).isEqualTo("REVIEW_ONLY_PLAN");
        assertThat(plan.getEntryZone()).contains(FIXTURE_SOURCE);
        assertThat(plan.getInvalidCondition()).contains(FIXTURE_SOURCE);
    }

    private static boolean isCompletePlan(ExecutionPlanDO plan) {
        return plan != null && "VALID".equals(plan.getExecutionPlanStatus())
                && Boolean.TRUE.equals(plan.getSourceGateComplete())
                && hasText(plan.getEntryZone()) && hasText(plan.getStopLoss())
                && hasText(plan.getTakeProfitRules()) && hasText(plan.getInvalidCondition())
                && hasText(plan.getSourceCompletenessSummary()) && Boolean.TRUE.equals(plan.getManualReviewRequired());
    }

    private static void assertPlanSafety(ExecutionPlanDO plan) {
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();
    }

    private static void assertSafeMonitor(PositionMonitorResultDTO result) {
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoReduce()).isTrue();
        assertThat(result.isNotAutoClose()).isTrue();
        assertThat(result.isNotAutoReverse()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotPositionMutation()).isTrue();
    }

    private static void assertSafeRecheck(RecheckResult result) {
        assertThat(result.isReviewPassed()).isFalse();
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !"暂无".equals(value);
    }

    private static List<String[]> klines(List<ReplayCandle> candles) {
        return candles.stream().map(ReplayCandle::toKline).toList();
    }

    private static ReplayCandle last(List<ReplayCandle> candles) {
        return candles.get(candles.size() - 1);
    }

    private static List<ReplayCandle> uptrend() {
        return candles("100,106,99,105,1200", "105,111,104,110,1600", "110,118,109,117,2400");
    }

    private static List<ReplayCandle> downtrend() {
        return candles("118,119,112,113,1400", "113,114,106,107,1800", "107,108,98,99,2600");
    }

    private static List<ReplayCandle> fakeBreakout() {
        return candles("100,106,99,105,1200", "105,112,104,111,2100", "111,116,110,114,2500");
    }

    private static List<ReplayCandle> choppy1m() {
        return candles("100,103,98,101,900", "101,103,98,99,1100", "99,102,98,101,950");
    }

    private static List<ReplayCandle> choppy5m() {
        return candles("101,103,98,100,3200", "100,103,98,101,3100", "101,102,98,99,3300");
    }

    private static List<ReplayCandle> wick1m() {
        return candles("103,105,101,104,1000", "104,118,96,102,4200", "102,104,97,99,3000");
    }

    private static List<ReplayCandle> wick5m() {
        return candles("99,104,98,103,3800", "103,118,96,104,5200", "104,106,100,105,4000");
    }

    private static List<ReplayCandle> crashRebound() {
        return candles("110,111,82,86,6000", "86,98,80,95,7200", "95,104,92,101,6500");
    }

    private static List<ReplayCandle> slowPullback() {
        return candles("100,106,99,105,1200", "105,108,102,104,1050", "104,111,103,109,1500");
    }

    private static List<ReplayCandle> candles(String... values) {
        AtomicLong minute = new AtomicLong();
        return java.util.Arrays.stream(values).map(value -> {
            String[] parts = value.split(",");
            return new ReplayCandle("2026-01-01T00:" + String.format("%02d", minute.getAndIncrement()) + ":00Z",
                    decimal(parts[0]), decimal(parts[1]), decimal(parts[2]), decimal(parts[3]), decimal(parts[4]));
        }).toList();
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static EventImpactInputVO blockedContext() {
        EventImpactInputVO input = new EventImpactInputVO();
        input.setExternalContextStatus("BLOCKED");
        input.setExternalContextBlocked(true);
        input.setExternalContextRiskLevel("HIGH");
        input.setExternalContextSourceHealth(ExternalContextPolicy.SOURCE_HEALTH_OK);
        input.setActiveExternalEventCount(1);
        input.setExternalContextReasonCodes(List.of(ExternalContextPolicy.REASON_WINDOW_BLOCKED));
        return input;
    }

    private static TmPushSnapshotDO pushSnapshot(String scenario, boolean invalidation) {
        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setPushId(invalidation ? 801L : 802L);
        snapshot.setAnalysisId("replay-" + scenario);
        snapshot.setSymbol(SYMBOL);
        snapshot.setTimeframe("5m");
        snapshot.setPushType("OPPORTUNITY_REVIEW");
        snapshot.setPushStatus("CAPTURED");
        snapshot.setPushCreateTime(LocalDateTime.now());
        snapshot.setRuleVersion("rules-replay-local");
        snapshot.setEntryZoneJson("{\"lower\":99,\"upper\":101}");
        snapshot.setStopZoneJson("{\"stop\":94}");
        snapshot.setInvalidationConditionJson(invalidation
                ? "{\"invalidPriceBelow\":94,\"source\":\"" + FIXTURE_SOURCE + "\"}"
                : "{\"text\":\"price drift requires manual review\",\"source\":\"" + FIXTURE_SOURCE + "\"}");
        snapshot.setPlanModeSnapshot("REVIEW_ONLY_PLAN");
        snapshot.setCauseEffectAlignmentSnapshot("ALIGNED");
        snapshot.setExecutionFeasibilitySnapshot(88);
        snapshot.setDataQualityScoreSnapshot(88);
        snapshot.setConfusedScoreSnapshot(10);
        snapshot.setTriggerPrice(decimal("100"));
        snapshot.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        snapshot.setTraceId("trace-replay-" + scenario);
        snapshot.setCreateTime(LocalDateTime.now());
        return snapshot;
    }

    private static UserPositionDO paperPosition(Long id, String status, String planId,
                                                String stopLoss, String takeProfit) {
        UserPositionDO position = new UserPositionDO();
        position.setId(id);
        position.setAssetSymbol(SYMBOL);
        position.setSide("LONG");
        position.setStatus(status);
        position.setEntryPrice(decimal("100"));
        position.setQuantity(decimal("0.20"));
        position.setLeverage(decimal("2"));
        position.setStopLoss(decimal(stopLoss));
        position.setTakeProfit(decimal(takeProfit));
        position.setSourceType("MANUAL");
        position.setSourceRefId(planId);
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

    private static MarketQuoteSnapshot quote(String price) {
        MarketQuoteSnapshot snapshot = new MarketQuoteSnapshot();
        snapshot.setProvider(FIXTURE_SOURCE);
        snapshot.setSymbolNormalized(SYMBOL);
        snapshot.setLastPrice(decimal(price));
        return snapshot;
    }

    private static UserPositionRiskResult risk(String level, boolean blocked) {
        UserPositionRiskResult result = new UserPositionRiskResult();
        result.setRiskStatus(blocked ? "RISK_BLOCKED" : "RISK_ALLOWED");
        result.setRiskLevel(level);
        result.setRiskBlocked(blocked);
        result.setReasonCodes(List.of(blocked ? "RISK_BLOCKED" : "RISK_ALLOWED"));
        return result;
    }

    private record ReplayCandle(String timestamp, BigDecimal open, BigDecimal high, BigDecimal low,
                                BigDecimal close, BigDecimal volume) {
        private String[] toKline() {
            return new String[]{timestamp, open.toPlainString(), high.toPlainString(), low.toPlainString(),
                    close.toPlainString(), volume.toPlainString()};
        }
    }

    private record ReplayScenario(String name,
                                  String symbol,
                                  String direction,
                                  boolean opportunityExpected,
                                  boolean completePlanExpected,
                                  AssetStateEnum state,
                                  Integer dataQualityScore,
                                  Integer trendScore,
                                  List<ReplayCandle> oneMinute,
                                  List<ReplayCandle> fiveMinute,
                                  AiConflictResult conflict,
                                  ConfusedResult confused,
                                  EventImpactInputVO externalContext) {

        private static ReplayScenario valid(String name, String symbol, String direction,
                                            List<ReplayCandle> oneMinute, List<ReplayCandle> fiveMinute,
                                            AssetStateEnum state) {
            return new ReplayScenario(name, symbol, direction, true, true, state, 88, 72,
                    oneMinute, fiveMinute,
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_1_CONSISTENT, direction, "HIGH", "CONFIRM", 20),
                    new ConfusedResult(20, "OBSERVING", state.name(), false, false, 0, false,
                            "none", "stable"), null);
        }

        private static ReplayScenario noTrade(String name, String symbol, String direction,
                                              List<ReplayCandle> oneMinute, List<ReplayCandle> fiveMinute,
                                              AssetStateEnum state, String reason) {
            return new ReplayScenario(name, symbol, direction, false, false, state, 88, 42,
                    oneMinute, fiveMinute,
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_2_LIGHT_DIVERGENCE, direction,
                            "MEDIUM", "REVIEW", 40),
                    new ConfusedResult(45, "OBSERVING", state.name(), false, false, 0, false,
                            reason, "no-trade"), null);
        }

        private static ReplayScenario confused(String name, String symbol,
                                               List<ReplayCandle> oneMinute, List<ReplayCandle> fiveMinute) {
            return new ReplayScenario(name, symbol, "BULLISH", false, false, AssetStateEnum.CONFUSED, 70, 65,
                    oneMinute, fiveMinute,
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_4_EXTREME_DIVERGENCE, "BULLISH", "LOW", "HIGH",
                            "CONFUSED", 92, 3, false, 92),
                    new ConfusedResult(88, "OBSERVING", "CONFUSED", true, false, 0, true,
                            "fast crash and rebound conflict", "manual review block"), null);
        }

        private static ReplayScenario highRisk(String name, String symbol,
                                               List<ReplayCandle> oneMinute, List<ReplayCandle> fiveMinute) {
            return new ReplayScenario(name, symbol, "BULLISH", false, false, AssetStateEnum.HIGH_RISK, 88, 72,
                    oneMinute, fiveMinute,
                    new AiConflictResult(AiConflictLevelEnum.LEVEL_1_CONSISTENT, "BULLISH", "MEDIUM", "CONFIRM", 20),
                    new ConfusedResult(20, "OBSERVING", "CANDIDATE", false, false, 0, false,
                            "external event window", "risk"), blockedContext());
        }
    }

    private record ReplayResult(ReplayScenario scenario, DecisionBundleVO decision, ExecutionPlanDO plan) {
        private boolean validOpportunityDetected() {
            return scenario.opportunityExpected() && Boolean.TRUE.equals(decision.getIsWorthOpening()) && isCompletePlan(plan);
        }

        private boolean falsePositive() {
            return !scenario.opportunityExpected()
                    && (Boolean.TRUE.equals(decision.getIsWorthOpening()) || isCompletePlan(plan));
        }

        private boolean missedValidOpportunity() {
            return scenario.opportunityExpected()
                    && (!Boolean.TRUE.equals(decision.getIsWorthOpening()) || !isCompletePlan(plan));
        }
    }

    private record MonitorPoint(Long id, String name, String currentPrice, String stopLoss,
                                String takeProfit, UserPositionRiskResult risk) {
        private static MonitorPoint normal(Long id, String name, String currentPrice, String stopLoss, String takeProfit) {
            return new MonitorPoint(id, name, currentPrice, stopLoss, takeProfit,
                    V1HistoricalReplayValidationTest.risk("LOW", false));
        }

        private static MonitorPoint highRisk(Long id, String name, String currentPrice, String stopLoss, String takeProfit) {
            return new MonitorPoint(id, name, currentPrice, stopLoss, takeProfit,
                    V1HistoricalReplayValidationTest.risk("HIGH", true));
        }
    }

    private static final class MonitorHarness {
        private final UserPositionMapper positionMapper = mock(UserPositionMapper.class);
        private final MarketQuoteClient quoteClient = mock(MarketQuoteClient.class);
        private final UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        private final ExecutionPlanMapper planMapper = mock(ExecutionPlanMapper.class);
        private final PositionMonitorLogService logService = mock(PositionMonitorLogService.class);
        private final PositionMonitorServiceImpl service;
        private final AtomicLong logIds = new AtomicLong(3000L);

        private MonitorHarness() {
            when(logService.listByPositionId(anyLong(), anyInt())).thenReturn(List.of());
            when(logService.recordMonitorRun(any())).thenAnswer(invocation -> {
                PositionMonitorLogDTO log = new PositionMonitorLogDTO();
                log.setLogId(logIds.incrementAndGet());
                return log;
            });
            service = new PositionMonitorServiceImpl(positionMapper, quoteClient, riskAdapter, planMapper, logService,
                    mock(EvidenceItemMapper.class), mock(ScoreItemMapper.class), mock(DecisionResultMapper.class),
                    new ObjectMapper(), null);
        }

        private PositionMonitorResultDTO monitor(MonitorPoint point) {
            String planId = "plan-replay-" + point.name();
            when(positionMapper.selectById(point.id()))
                    .thenReturn(paperPosition(point.id(), "OPEN", planId, point.stopLoss(), point.takeProfit()));
            when(quoteClient.fetch24hTicker(SYMBOL)).thenReturn(Optional.of(quote(point.currentPrice())));
            when(riskAdapter.currentRisk()).thenReturn(point.risk());
            lenient().when(planMapper.selectByPlanId(planId)).thenReturn(monitorPlan(planId));
            return service.monitorUserPosition(point.id());
        }

        private void monitorClosed(Long id) {
            when(positionMapper.selectById(id)).thenReturn(paperPosition(id, "CLOSED", "plan-closed", "90", "120"));
            service.monitorUserPosition(id);
        }

        private PositionMonitorBatchResultDTO afterCloseBatch() {
            when(positionMapper.listOpenPositions()).thenReturn(List.of());
            return service.monitorOpenUserPositions();
        }
    }
}
