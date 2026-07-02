package org.example.trademodel.integration;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.service.DashboardHomeService;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
    private JdbcTemplate jdbcTemplate;

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
                "tm_ai_call_log",
                "tm_user_position",
                "tm_analysis_run")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
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
        assertThat(plan.getInvalidCondition()).isEqualTo("decision invalidation placeholder");
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
