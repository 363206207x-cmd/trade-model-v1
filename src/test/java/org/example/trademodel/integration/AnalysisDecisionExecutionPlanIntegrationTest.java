package org.example.trademodel.integration;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.example.trademodel.service.impl.PlanServiceImpl;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class AnalysisDecisionExecutionPlanIntegrationTest {

    private static final String ANALYSIS_ID = "ana-int-decision-plan-1";
    private static final String SYMBOL = "BTCUSDT";

    @Autowired
    private AnalysisRunMapper analysisRunMapper;
    @Autowired
    private EvidenceItemMapper evidenceItemMapper;
    @Autowired
    private ScoreItemMapper scoreItemMapper;
    @Autowired
    private DecisionResultMapper decisionResultMapper;
    @Autowired
    private ExecutionPlanMapper executionPlanMapper;
    @Autowired
    private DashboardHomeService dashboardHomeService;
    @Autowired
    private AnalysisRunOrchestrator analysisRunOrchestrator;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RealMarketDataFetcherService realMarketDataFetcherService;
    @MockBean
    private RealMarketEnvironmentService realMarketEnvironmentService;
    @MockBean
    private AiDecisionOrchestratorService aiDecisionOrchestratorService;

    @BeforeEach
    void cleanDashboardRuntimeTables() {
        for (String table : List.of(
                "tm_push_recheck_log",
                "tm_push_snapshot",
                "tm_opportunity_log",
                "tm_monitor_alert",
                "tm_account_risk_snapshot",
                "tm_execution_plan",
                "tm_decision_result",
                "tm_score_item",
                "tm_evidence_item",
                "tm_market_environment_snapshot",
                "tm_persisted_ohlcv_bar",
                "tm_ai_call_log",
                "tm_user_position",
                "tm_analysis_run")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        when(realMarketEnvironmentService.tryBuildFromRealQuote(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(aiDecisionOrchestratorService.review(any())).thenReturn(new AiOrchestratorResult());
        when(aiDecisionOrchestratorService.providerReadiness()).thenReturn(List.of());
        stubDecisionKlines(true);
    }

    @Test
    void assemblerUsesFreshPersistedOhlcvToPersistBoundaryBackedPlanIntoDashboardHome() {
        long latestCloseMs = persistBoundaryBars(SYMBOL, "5m", true);
        String analysisTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(latestCloseMs + 1_000L), ZoneOffset.UTC).toString();

        AnalysisRunResult result = analysisRunOrchestrator.run(
                AnalysisRunCommand.manual(SYMBOL, "5m", "req-boundary-long", analysisTime));

        assertThat(result.isSuccessfulAnalysisAvailable()).isTrue();
        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(plan).isNotNull();
        assertThat(plan.getEntryZone()).contains("入场区间").doesNotContain("暂无");
        assertThat(plan.getStopLoss()).contains("止损参考").doesNotContain("暂无");
        assertThat(plan.getTakeProfitRules()).contains("分批止盈").doesNotContain("暂无");
        assertThat(plan.getInvalidCondition()).contains("失效条件").doesNotContain("decision invalidation fallback");
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();

        DashboardHomeVO home = dashboardHomeService.getHome(SYMBOL, 6);
        assertThat(home.getExecutionSuggestion().getDirection()).isEqualTo("BULLISH");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo(plan.getEntryZone());
        assertThat(home.getExecutionSuggestion().getStopLoss()).isEqualTo(plan.getStopLoss());
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isEqualTo(plan.getTakeProfitRules());
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isEqualTo(plan.getInvalidCondition());
        assertThat(count("tm_user_position")).isZero();
        assertThat(count("tm_push_recheck_log")).isZero();
        assertThat(count("tm_ai_call_log")).isZero();
        assertReviewOnlySafety(home.getSafety());
    }

    @Test
    void assemblerUsesFreshPersistedOhlcvForBearishBoundaryBackedPlan() {
        String symbol = "ETHUSDT";
        stubDecisionKlines(false);
        long latestCloseMs = persistBoundaryBars(symbol, "5m", false);
        String analysisTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(latestCloseMs + 1_000L), ZoneOffset.UTC).toString();

        AnalysisRunResult result = analysisRunOrchestrator.run(
                AnalysisRunCommand.manual(symbol, "5m", "req-boundary-short", analysisTime));

        assertThat(result.isSuccessfulAnalysisAvailable()).isTrue();
        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(plan).isNotNull();
        assertThat(plan.getEntryZone()).contains("入场区间").doesNotContain("暂无");
        assertThat(plan.getStopLoss()).contains("止损参考").doesNotContain("暂无");
        assertThat(plan.getTakeProfitRules()).contains("分批止盈").doesNotContain("暂无");
        assertThat(plan.getInvalidCondition()).contains("失效条件");

        DashboardHomeVO home = dashboardHomeService.getHome(symbol, 6);
        assertThat(home.getExecutionSuggestion().getDirection()).isEqualTo("BEARISH");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo(plan.getEntryZone());
        assertThat(home.getExecutionSuggestion().getStopLoss()).isEqualTo(plan.getStopLoss());
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isEqualTo(plan.getTakeProfitRules());
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isEqualTo(plan.getInvalidCondition());
        assertThat(count("tm_user_position")).isZero();
    }

    @Test
    void assemblerFallsBackWithoutPersistedOhlcvAndDoesNotInventBoundaryPlan() {
        String analysisTime = LocalDateTime.now(ZoneOffset.UTC).toString();

        AnalysisRunResult result = analysisRunOrchestrator.run(
                AnalysisRunCommand.manual("XRPUSDT", "5m", "req-boundary-no-ohlcv", analysisTime));

        assertThat(result.isSuccessfulAnalysisAvailable()).isTrue();
        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(plan).isNotNull();
        assertThat(plan.getEntryZone()).isEqualTo("暂无");
        assertThat(plan.getStopLoss()).isEqualTo("暂无");
        assertThat(plan.getTakeProfitRules()).isEqualTo("暂无");
        assertThat(plan.getInvalidCondition()).isNull();
        assertThat(count("tm_user_position")).isZero();
    }

    @Test
    void oneMinuteAnalysisDoesNotGenerateFormalExecutionPlanBoundaryEvenWithFreshBars() {
        long latestCloseMs = persistBoundaryBars("ADAUSDT", "1m", true);
        String analysisTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(latestCloseMs + 1_000L), ZoneOffset.UTC).toString();

        AnalysisRunResult result = analysisRunOrchestrator.run(
                AnalysisRunCommand.manual("ADAUSDT", "1m", "req-boundary-1m-unsupported", analysisTime));

        assertThat(result.isSuccessfulAnalysisAvailable()).isTrue();
        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(plan).isNotNull();
        assertThat(plan.getEntryZone()).isEqualTo("暂无");
        assertThat(plan.getStopLoss()).isEqualTo("暂无");
        assertThat(plan.getTakeProfitRules()).isEqualTo("暂无");
        assertThat(plan.getInvalidCondition()).isNull();

        DashboardHomeVO home = dashboardHomeService.getHome("ADAUSDT", 6);
        assertThat(home.getExecutionSuggestion().getValidPeriod())
                .isEqualTo("周期不支持，需使用 5m / 15m / 1h / 4h");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getStopLoss()).isNull();
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isNull();
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
        assertThat(count("tm_user_position")).isZero();
    }

    @Test
    void persistedAnalysisDecisionAndExecutionPlanFlowIntoDashboardHomeExecutionSuggestion() {
        persistControlledAnalysisDecisionAndPlan();

        assertThat(analysisRunMapper.selectById(ANALYSIS_ID)).isNotNull();
        assertThat(analysisRunMapper.selectEvidenceIdsByAnalysisId(ANALYSIS_ID)).containsExactly("ev-int-1");
        assertThat(analysisRunMapper.selectScoreIdsByAnalysisId(ANALYSIS_ID)).containsExactly("sc-int-1");
        assertThat(analysisRunMapper.selectDecisionIdsByAnalysisId(ANALYSIS_ID)).containsExactly("dec-int-1");
        assertThat(analysisRunMapper.selectExecutionPlanIdsByAnalysisId(ANALYSIS_ID)).containsExactly("plan-int-1");

        DecisionResultVO joined = decisionResultMapper.findLatestDecisionResultBySymbolJoined(SYMBOL);
        assertThat(joined).isNotNull();
        assertThat(joined.getAnalysisId()).isEqualTo(ANALYSIS_ID);
        assertThat(joined.getMarketBiasHierarchy()).isEqualTo("BULLISH");
        assertThat(joined.getEntryZone()).isEqualTo("63000-64000 USDT");
        assertThat(joined.getStopLoss()).isEqualTo("60800 USDT");
        assertThat(joined.getTakeProfitRules()).isEqualTo("66000 / 68500 / 71000 USDT");
        assertThat(joined.getLeverageSuggestion()).isEqualTo("3x");
        assertThat(joined.getPositionSuggestion()).isEqualTo("10% account risk cap");
        assertThat(joined.getInvalidCondition()).isEqualTo("plan invalidation wins");
        assertThat(joined.getExecutionPlanSummary()).isEqualTo("12h | plan invalidation wins");

        DashboardHomeVO home = dashboardHomeService.getHome(SYMBOL, 6);

        assertThat(home.getSelectedSymbol()).isEqualTo(SYMBOL);
        assertThat(home.getExecutionSuggestion().getDirection()).isEqualTo("BULLISH");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo("63000-64000 USDT");
        assertThat(home.getExecutionSuggestion().getStopLoss()).isEqualTo("60800 USDT");
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isEqualTo("66000 / 68500 / 71000 USDT");
        assertThat(home.getExecutionSuggestion().getLeverageSuggestion()).isEqualTo("3x");
        assertThat(home.getExecutionSuggestion().getPositionSuggestion()).isEqualTo("10% account risk cap");
        assertThat(home.getExecutionSuggestion().getValidPeriod()).isEqualTo("12h");
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isEqualTo("plan invalidation wins");

        assertThat(home.getPositions()).isEmpty();
        assertThat(count("tm_user_position")).isZero();
        assertThat(count("tm_push_recheck_log")).isZero();
        assertThat(count("tm_ai_call_log")).isZero();
        assertReviewOnlySafety(home.getSafety());
    }

    @Test
    void dashboardHomeEmptyStateDoesNotInventExecutionSuggestionOrPositions() {
        DashboardHomeVO home = dashboardHomeService.getHome("EMPTYUSDT", 6);

        assertThat(home.getExecutionSuggestion().getDirection()).isNull();
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getStopLoss()).isNull();
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isNull();
        assertThat(home.getExecutionSuggestion().getLeverageSuggestion()).isNull();
        assertThat(home.getExecutionSuggestion().getPositionSuggestion()).isNull();
        assertThat(home.getExecutionSuggestion().getValidPeriod()).isNull();
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
        assertThat(home.getPositions()).isEmpty();
        assertReviewOnlySafety(home.getSafety());
    }

    @Test
    void planServiceWithoutSourceTraceDocumentsCurrentAdvisoryPlaceholderBoundary() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        decision.setPushInvalidationSummary("decision invalidation placeholder");

        ExecutionPlanVO plan = new PlanServiceImpl().generateExecutionPlan(decision, null, null, null);

        assertThat(plan.getPlanMode()).isEqualTo(ExecutionPlanVO.PLAN_MODE_ADVISORY);
        assertThat(plan.getExecutionPlanStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getSourceGateStatus()).isEqualTo(ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE);
        assertThat(plan.getSourceTraceComplete()).isFalse();
        assertThat(plan.getEntryZone()).isEqualTo("暂无");
        assertThat(plan.getStopLoss()).isEqualTo("暂无");
        assertThat(plan.getTakeProfitRules()).isEqualTo("暂无");
        assertThat(plan.getLeverageSuggestion()).isEqualTo("1-5x");
        assertThat(plan.getPositionSuggestion()).isEqualTo("单笔风险不超过总资金 2%");
        assertThat(plan.getInvalidCondition()).isNull();
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();
    }

    @Test
    void analysisDecisionPlanDashboardPathDoesNotReferenceTradingTelegramOrRecheckSideEffects() throws Exception {
        String generationSource = String.join("\n", List.of(
                readSource("src/main/java/org/example/trademodel/analysisrun/AnalysisRunOrchestratorImpl.java"),
                readSource("src/main/java/org/example/trademodel/service/impl/AnalysisAssemblerServiceImpl.java"),
                readSource("src/main/java/org/example/trademodel/service/DecisionEngineService.java"),
                readSource("src/main/java/org/example/trademodel/service/impl/PlanServiceImpl.java")
        ));
        String dashboardConsumptionSource = readSource(
                "src/main/java/org/example/trademodel/service/impl/DashboardHomeServiceImpl.java"
        );

        assertThat(generationSource).doesNotContain(
                "UserPositionService",
                "UserPositionMapper",
                "manualOpen(",
                "manualClose(",
                "OrderService",
                "TradeService",
                "ExchangeOrder",
                "placeOrder",
                "createOrder",
                "autoOpen",
                "autoClose",
                "Telegram",
                "sendMessage",
                "PushRecheckService",
                "pushRecheckService.recheck",
                ".recheck("
        );
        assertThat(dashboardConsumptionSource).doesNotContain(
                "manualOpen(",
                "manualClose(",
                "placeOrder",
                "createOrder",
                "pushRecheckService.recheck",
                ".recheck("
        );
    }

    private void persistControlledAnalysisDecisionAndPlan() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 9, 30);
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(ANALYSIS_ID);
        run.setSymbol(SYMBOL);
        run.setTimeframe("1h");
        run.setAnalysisTime(now);
        run.setRuleVersion("rules-test");
        run.setDataQualityScore(91);
        run.setTraceId("trace-int-1");
        run.setStatus("SUCCESS");
        run.setIdempotencyKey("idem-int-1");
        run.setRequestId("req-int-1");
        run.setTriggerType("MANUAL_API");
        run.setTriggerReference("test-controlled-input");
        run.setInputSnapshotJson("{\"reviewOnly\":true}");
        run.setInputSnapshotHash("hash-int-1");
        run.setAttemptCount(1);
        run.setLeaseOwner(null);
        run.setLeaseExpiresAt(null);
        run.setStartedAt(now);
        run.setCompletedAt(now);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        run.setVersionNo(1);
        analysisRunMapper.insert(run);

        EvidenceItemDO evidence = new EvidenceItemDO();
        evidence.setEvidenceId("ev-int-1");
        evidence.setAnalysisId(ANALYSIS_ID);
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("controlled evidence for integration contract");
        evidence.setDirection("BULLISH");
        evidence.setStrength(0.82);
        evidence.setConfidence(0.88);
        evidence.setSource("TEST_CONTROLLED");
        evidence.setSourceProvider("test-double");
        evidence.setSourceReference("test://analysis-decision-plan");
        evidence.setCreateTime(now);
        evidenceItemMapper.insert(evidence);

        ScoreItemDO score = new ScoreItemDO();
        score.setScoreId("sc-int-1");
        score.setAnalysisId(ANALYSIS_ID);
        score.setScoreType("趋势结构分");
        score.setScoreValue(88.0);
        score.setWeight(1.0);
        score.setDirection("BULLISH");
        score.setDescription("controlled score for integration contract");
        scoreItemMapper.insert(score);

        DecisionResult decision = new DecisionResult();
        decision.setDecisionId("dec-int-1");
        decision.setAnalysisId(ANALYSIS_ID);
        decision.setSymbol(SYMBOL);
        decision.setMarketBiasHierarchy("BULLISH");
        decision.setTradeType("SPOT");
        decision.setConfidenceLevel("HIGH");
        decision.setRiskLevel("LOW");
        decision.setActionPriority("HIGH");
        decision.setConclusionSummary("controlled decision result");
        decision.setIsWorthOpening(true);
        decision.setMultiTfConvergence("STRONG");
        decision.setAiRoleResults("{\"GPT_FINAL\":{\"reviewConclusion\":\"controlled review\"}}");
        decision.setIsAdopted(null);
        decision.setValidPeriod("12h");
        decision.setInvalidCondition("decision invalidation fallback");
        decision.setEvidenceSummary("controlled evidence summary");
        decision.setExplanationJson("{\"summary\":\"controlled\"}");
        decision.setReviewReasons("[]");
        decision.setAiConflictLevel("LOW");
        decision.setAiConflictScore(12);
        decision.setAiPlanMode("ADVISORY");
        decision.setConfusedScore(8);
        decision.setAssetStateSnapshot("{\"state\":\"CANDIDATE\"}");
        decision.setCreateTime(now.plusSeconds(1));
        decisionResultMapper.insert(decision);

        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-int-1");
        plan.setAnalysisId(ANALYSIS_ID);
        plan.setPlanMode("ADVISORY");
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setSourceCompletenessSummary("controlled source gate valid");
        plan.setRecommendedAction("REVIEW_ONLY_PLAN");
        plan.setEntryZone("63000-64000 USDT");
        plan.setStopLoss("60800 USDT");
        plan.setTakeProfitRules("66000 / 68500 / 71000 USDT");
        plan.setLeverageSuggestion("3x");
        plan.setPositionSuggestion("10% account risk cap");
        plan.setAccountRiskJson("{\"riskAllowed\":true}");
        plan.setInvalidCondition("plan invalidation wins");
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        plan.setCreateTime(now.plusSeconds(2));
        executionPlanMapper.insert(plan);
    }

    private void stubDecisionKlines(boolean bullish) {
        List<String[]> klines = bullish
                ? List.of(
                kline("100", "105"),
                kline("101", "106"),
                kline("102", "107"))
                : List.of(
                kline("107", "102"),
                kline("106", "101"),
                kline("105", "100"));
        when(realMarketDataFetcherService.fetchKlines(anyString(), eq("1m"), anyInt())).thenReturn(klines);
        when(realMarketDataFetcherService.fetchKlines(anyString(), eq("5m"), anyInt())).thenReturn(klines);
    }

    private String[] kline(String open, String close) {
        BigDecimal openValue = new BigDecimal(open);
        BigDecimal closeValue = new BigDecimal(close);
        BigDecimal high = openValue.max(closeValue).add(new BigDecimal("1.00"));
        BigDecimal low = openValue.min(closeValue).subtract(new BigDecimal("1.00"));
        return new String[]{"0", open, high.toPlainString(), low.toPlainString(), close};
    }

    private long persistBoundaryBars(String symbol, String timeframe, boolean bullishStructure) {
        long intervalMs = timeframeMs(timeframe);
        long latestOpenMs = (System.currentTimeMillis() / intervalMs) * intervalMs - intervalMs;
        long latestCloseMs = latestOpenMs + intervalMs - 1L;
        long firstOpenMs = latestOpenMs - (49L * intervalMs);
        String batchId = "batch-" + symbol + "-" + latestOpenMs;
        for (int i = 0; i < 50; i++) {
            BigDecimal open = new BigDecimal("101.00");
            BigDecimal high = new BigDecimal("103.00");
            BigDecimal low = new BigDecimal("99.00");
            BigDecimal close = new BigDecimal("102.00");
            if (bullishStructure) {
                if (i == 44) {
                    open = new BigDecimal("100.00");
                    high = new BigDecimal("102.00");
                    low = new BigDecimal("95.00");
                    close = new BigDecimal("100.50");
                }
                if (i == 46) {
                    open = new BigDecimal("102.00");
                    high = new BigDecimal("110.00");
                    low = new BigDecimal("100.00");
                    close = new BigDecimal("104.00");
                }
            } else {
                if (i == 44) {
                    open = new BigDecimal("104.00");
                    high = new BigDecimal("110.00");
                    low = new BigDecimal("101.00");
                    close = new BigDecimal("103.00");
                }
                if (i == 46) {
                    open = new BigDecimal("100.00");
                    high = new BigDecimal("103.00");
                    low = new BigDecimal("95.00");
                    close = new BigDecimal("98.00");
                }
            }
            long openTimeMs = firstOpenMs + (i * intervalMs);
            long closeTimeMs = openTimeMs + intervalMs - 1L;
            jdbcTemplate.update("""
                            INSERT INTO tm_persisted_ohlcv_bar(
                                symbol, timeframe, open_time_ms, close_time_ms,
                                open_price, high_price, low_price, close_price, volume,
                                is_closed, provider, provider_market_type, source_endpoint,
                                source_batch_id, source_trace_id, source_version, ingested_at,
                                updated_at, quality_status, is_deleted)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
                                CURRENT_TIMESTAMP, 'OK', 0)
                            """,
                    symbol,
                    timeframe,
                    openTimeMs,
                    closeTimeMs,
                    open,
                    high,
                    low,
                    close,
                    new BigDecimal("1000.00"),
                    "TEST_FIXTURE",
                    "SPOT",
                    "/test/klines",
                    batchId,
                    "trace-" + symbol + "-" + i,
                    1);
        }
        return latestCloseMs;
    }

    private long timeframeMs(String timeframe) {
        if (timeframe == null || timeframe.isBlank() || timeframe.length() < 2) {
            return 60_000L;
        }
        long amount = Long.parseLong(timeframe.substring(0, timeframe.length() - 1));
        String unit = timeframe.substring(timeframe.length() - 1);
        if ("h".equals(unit)) {
            return amount * 60L * 60_000L;
        }
        return amount * 60_000L;
    }

    private int count(String table) {
        Integer value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    private static void assertReviewOnlySafety(DashboardHomeVO.SafetyVO safety) {
        assertThat(safety.getReviewOnly()).isTrue();
        assertThat(safety.getManualReviewOnly()).isTrue();
        assertThat(safety.getNotTradeInstruction()).isTrue();
        assertThat(safety.getNotExecutable()).isTrue();
        assertThat(safety.getNotAutoTrading()).isTrue();
        assertThat(safety.getNotOrderExecution()).isTrue();
        assertThat(safety.getNotPushSend()).isTrue();
        assertThat(safety.getNotExternalChannel()).isTrue();
        assertThat(safety.getNotUserPositionCreation()).isTrue();
        assertThat(safety.getNotUserPositionMutation()).isTrue();
    }

    private static String readSource(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
