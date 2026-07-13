package org.example.trademodel.integration;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiReviewConflictLevel;
import org.example.trademodel.ai.AiReviewStance;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class AnalysisDecisionExecutionPlanIntegrationTest {

    private static final String ANALYSIS_ID = "ana-int-decision-plan-1";
    private static final String SYMBOL = "BTCUSDT";

    @Autowired
    private AnalysisRunMapper analysisRunMapper;
    @Autowired
    private AssetStateMapper assetStateMapper;
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
    @Autowired
    private AiRoleResultsCodec aiRoleResultsCodec;
    @Autowired
    private PersistedOhlcvIngestionService persistedOhlcvIngestionService;

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
                "tm_asset_state",
                "tm_analysis_run")) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        when(realMarketEnvironmentService.tryBuildFromRealQuote(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(aiDecisionOrchestratorService.review(any())).thenAnswer(invocation -> {
            AiProviderRequest request = invocation.getArgument(0);
            return List.of("SOLUSDT", "BNBUSDT", "DOGEUSDT").contains(request.getSymbol())
                    ? threeRoleResult(request)
                    : emptyRoleResult(request);
        });
        when(aiDecisionOrchestratorService.providerReadiness()).thenReturn(List.of());
    }

    @Test
    void structuredAiRolePayloadPersistsAndLoadsWithoutLoss() {
        AnalysisRunResult result = runAiContractAnalysis("SOLUSDT", "req-ai-persistence");

        DecisionResult persisted = decisionResultMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(persisted).isNotNull();
        AiRoleResultsCodec.ParseResult parsed = aiRoleResultsCodec.parse(persisted.getAiRoleResults());

        assertThat(parsed.current()).isTrue();
        assertThat(parsed.payload().schemaVersion()).isEqualTo("v1");
        assertThat(parsed.payload().roles()).containsOnlyKeys("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(parsed.payload().roles().get("GPT_FINAL").summary()).isEqualTo("GPT persisted summary");
        assertThat(parsed.payload().roles().get("GEMINI_REVIEW").reasonCodes())
                .containsExactly("GEMINI_CONTRADICTION_ONLY");
        assertThat(parsed.payload().roles().get("GROK_CHALLENGE").summary())
                .isEqualTo("Grok persisted challenge");
        assertThat(persisted.getAiRoleResults()).doesNotContain("providerRequestId", "Authorization", "apiKey");
    }

    @Test
    void producerToDashboardHomeIntegrationRendersAllThreeRoles() {
        String symbol = "BNBUSDT";
        AnalysisRunResult result = runAiContractAnalysis(symbol, "req-ai-home");
        DecisionResult persisted = decisionResultMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(persisted.getMarketBiasHierarchy()).isEqualTo("WAIT");

        clearInvocations(aiDecisionOrchestratorService);
        DashboardHomeVO home = dashboardHomeService.getHome(symbol, 6);
        verifyNoInteractions(aiDecisionOrchestratorService);

        assertThat(home.getAiDecision().getSchemaVersion()).isEqualTo("v1");
        DashboardHomeVO.AiTabVO gpt = aiTab(home, "GPT_FINAL");
        DashboardHomeVO.AiTabVO gemini = aiTab(home, "GEMINI_REVIEW");
        DashboardHomeVO.AiTabVO grok = aiTab(home, "GROK_CHALLENGE");
        assertThat(gpt.getFinalMarketBias()).isEqualTo("WAIT");
        assertThat(gpt.getDecisionSummary()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gpt.getCoreSupportingEvidence()).containsExactly("AI 证据已记录，需人工复核");
        assertThat(gemini.getReviewConclusion()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gemini.getDetectedContradictions()).containsExactly("AI 发现证据冲突");
        assertThat(grok.getChallengeThesis()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(grok.getCounterEvidence()).containsExactly("AI 提供反向证据");
        assertThat(gemini.getReviewConclusion()).doesNotContain("GPT", "Grok");
        assertThat(grok.getChallengeThesis()).doesNotContain("GPT", "Gemini");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_user_position", Integer.class)).isZero();
        assertThat(home.getSafety().getNotAutoTrading()).isTrue();
        assertThat(home.getSafety().getNotOrderExecution()).isTrue();
    }

    @Test
    void ruleLayerRemainsAuthoritative() {
        AnalysisRunResult result = runAiContractAnalysis("DOGEUSDT", "req-ai-authority");
        DecisionResult persisted = decisionResultMapper.selectLatestByAnalysisId(result.getAnalysisId());
        AiRoleResultsPayload payload = aiRoleResultsCodec.parse(persisted.getAiRoleResults()).payload();

        assertThat(persisted.getMarketBiasHierarchy()).isEqualTo("WAIT");
        assertThat(payload.synthesis().finalMarketBias()).isEqualTo("WAIT");
        assertThat(payload.safety().ruleDirectionPreserved()).isTrue();
        assertThat(payload.safety().notStateMachineOverride()).isTrue();
        assertThat(payload.safety().notUserPositionCreation()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_user_position", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT state FROM tm_asset_state", String.class))
                .doesNotContain("TRIGGERED");
    }

    @Test
    void assemblerAndDashboardBothFailClosedWhenDataQualityIsInsufficient() {
        long latestCloseMs = persistDecisionTimeframes(SYMBOL, true);
        String analysisTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(latestCloseMs + 1_000L), ZoneOffset.UTC).toString();

        AnalysisRunResult result = analysisRunOrchestrator.run(
                AnalysisRunCommand.manual(SYMBOL, "5m", "req-boundary-long", analysisTime));

        assertThat(result.isSuccessfulAnalysisAvailable()).isTrue();
        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(plan).isNotNull();
        assertThat(plan.getExecutionPlanStatus()).isEqualTo("INCOMPLETE");
        assertThat(plan.getSourceGateComplete()).isFalse();
        assertThat(plan.getEntryZone()).isEqualTo("暂无");
        assertThat(plan.getStopLoss()).isEqualTo("暂无");
        assertThat(plan.getTakeProfitRules()).isEqualTo("暂无");
        assertThat(plan.getInvalidCondition()).isNull();
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();

        DashboardHomeVO home = dashboardHomeService.getHome(SYMBOL, 6);
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("DATA_QUALITY_BLOCKED");
        assertThat(home.getExecutionSuggestion().getBlockedReason()).isEqualTo("数据质量不足，等待有效分析");
        assertThat(home.getExecutionSuggestion().getDirection()).isNull();
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getStopLoss()).isNull();
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isNull();
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
        assertThat(count("tm_user_position")).isZero();
        assertThat(count("tm_push_recheck_log")).isZero();
        assertThat(count("tm_ai_call_log")).isZero();
        assertReviewOnlySafety(home.getSafety());
    }

    @Test
    void bearishAssemblerAndDashboardBothFailClosedWhenDataQualityIsInsufficient() {
        String symbol = "ETHUSDT";
        long latestCloseMs = persistDecisionTimeframes(symbol, false);
        String analysisTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(latestCloseMs + 1_000L), ZoneOffset.UTC).toString();

        AnalysisRunResult result = analysisRunOrchestrator.run(
                AnalysisRunCommand.manual(symbol, "5m", "req-boundary-short", analysisTime));

        assertThat(result.isSuccessfulAnalysisAvailable()).isTrue();
        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(plan).isNotNull();
        assertThat(plan.getExecutionPlanStatus()).isEqualTo("INCOMPLETE");
        assertThat(plan.getSourceGateComplete()).isFalse();
        assertThat(plan.getEntryZone()).isEqualTo("暂无");
        assertThat(plan.getStopLoss()).isEqualTo("暂无");
        assertThat(plan.getTakeProfitRules()).isEqualTo("暂无");
        assertThat(plan.getInvalidCondition()).isNull();

        DashboardHomeVO home = dashboardHomeService.getHome(symbol, 6);
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("DATA_QUALITY_BLOCKED");
        assertThat(home.getExecutionSuggestion().getBlockedReason()).isEqualTo("数据质量不足，等待有效分析");
        assertThat(home.getExecutionSuggestion().getDirection()).isNull();
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getStopLoss()).isNull();
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isNull();
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
        assertThat(count("tm_user_position")).isZero();
    }

    @Test
    void assemblerFallsBackWithoutPersistedOhlcvAndDoesNotInventBoundaryPlan() {
        String analysisTime = LocalDateTime.now(ZoneOffset.UTC).toString();

        AnalysisRunResult result = analysisRunOrchestrator.run(
                AnalysisRunCommand.manual("XRPUSDT", "5m", "req-boundary-no-ohlcv", analysisTime));

        assertThat(result.isSuccessfulAnalysisAvailable()).isFalse();
        ExecutionPlanDO plan = executionPlanMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(plan).isNull();
        assertThat(count("tm_user_position")).isZero();
    }

    @Test
    void oneMinuteAnalysisDoesNotGenerateFormalExecutionPlanBoundary() {
        long latestCloseMs = persistDecisionTimeframes("ADAUSDT", true);
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
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("UNSUPPORTED_TIMEFRAME");
        assertThat(home.getExecutionSuggestion().getBlockedReason())
                .isEqualTo("周期不支持，需使用 5m / 15m / 1h / 4h");
        assertThat(home.getExecutionSuggestion().getValidPeriod()).isNull();
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getStopLoss()).isNull();
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isNull();
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
        assertThat(count("tm_user_position")).isZero();
    }

    @Test
    void persistedAnalysisDecisionAndExecutionPlanFlowIntoDashboardHomeExecutionSuggestion() {
        String validPeriod = persistControlledAnalysisDecisionAndPlan();

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
        assertThat(joined.getExecutionPlanSummary()).isEqualTo(validPeriod + " | plan invalidation wins");

        DashboardHomeVO home = dashboardHomeService.getHome(SYMBOL, 6);

        assertThat(home.getSelectedSymbol()).isEqualTo(SYMBOL);
        assertThat(home.getExecutionSuggestion().getDirection()).isEqualTo("BULLISH");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo("63000-64000 USDT");
        assertThat(home.getExecutionSuggestion().getStopLoss()).isEqualTo("60800 USDT");
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isEqualTo("66000 / 68500 / 71000 USDT");
        assertThat(home.getExecutionSuggestion().getLeverageSuggestion()).isEqualTo("3x");
        assertThat(home.getExecutionSuggestion().getPositionSuggestion()).isEqualTo("10% account risk cap");
        assertThat(home.getExecutionSuggestion().getValidPeriod()).isEqualTo(validPeriod);
        assertThat(home.getExecutionSuggestion().getValidFrom()).isNotNull();
        assertThat(home.getExecutionSuggestion().getExpiresAt()).isNotNull();
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

    private AnalysisRunResult runAiContractAnalysis(String symbol, String requestId) {
        long latestCloseMs = persistDecisionTimeframes(symbol, true);
        String analysisTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(latestCloseMs + 1_000L), ZoneOffset.UTC).toString();
        return analysisRunOrchestrator.run(
                AnalysisRunCommand.manual(symbol, "5m", requestId, analysisTime));
    }

    private AiOrchestratorResult threeRoleResult(AiProviderRequest request) {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId(request.getAnalysisId());
        result.setTraceId(request.getTraceId());
        result.setProviderResults(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "GPT_SUPPORT_ONLY", "GPT persisted summary"),
                role(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        AiReviewStance.CHALLENGE, "GEMINI_CONTRADICTION_ONLY", "Gemini persisted review"),
                role(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        AiReviewStance.CHALLENGE, "GROK_COUNTER_ONLY", "Grok persisted challenge")));
        result.setGptConsistentWithRule(true);
        result.setGeminiConsistentWithRule(false);
        result.setGrokConsistentWithRule(false);
        result.setAiObjectionCount(2);
        result.setAiSupportCount(1);
        result.setConflictContribution(20);
        return result;
    }

    private AiOrchestratorResult emptyRoleResult(AiProviderRequest request) {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId(request.getAnalysisId());
        result.setTraceId(request.getTraceId());
        return result;
    }

    private AiProviderReviewResult role(AiProviderName provider,
                                        AiProviderRole providerRole,
                                        AiReviewStance stance,
                                        String reasonCode,
                                        String summary) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(providerRole);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(stance);
        result.setConflictLevel(stance == AiReviewStance.CHALLENGE
                ? AiReviewConflictLevel.MAJOR
                : AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of(reasonCode));
        result.setSummary(summary);
        return result;
    }

    private DashboardHomeVO.AiTabVO aiTab(DashboardHomeVO home, String role) {
        return home.getAiDecision().getTabs().stream()
                .filter(tab -> role.equals(tab.getRole()))
                .findFirst()
                .orElseThrow();
    }

    private String persistControlledAnalysisDecisionAndPlan() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 2, 9, 30);
        DateTimeFormatter validPeriodFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime validityNow = LocalDateTime.now(ZoneOffset.UTC);
        String validPeriod = validityNow.minusHours(1).format(validPeriodFormatter)
                + " ~ " + validityNow.plusHours(1).format(validPeriodFormatter);
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

        AssetStateDO state = new AssetStateDO();
        state.setSymbol(SYMBOL);
        state.setState(org.example.trademodel.enums.AssetStateEnum.CANDIDATE);
        state.setConfusedScore(8);
        state.setConfusedLowStreak(0);
        state.setLastUpdateTime(now);
        state.setTraceId("trace-int-1");
        assetStateMapper.mergeUpsertCore(state);

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
        AiOrchestratorResult aiResult = new AiOrchestratorResult();
        aiResult.setAnalysisId(ANALYSIS_ID);
        aiResult.setTraceId("trace-int-1");
        decision.setAiRoleResults(aiRoleResultsCodec.serialize(aiResult, "v1.0",
                new AiRoleResultsPayload.SynthesisPayload(
                        "BULLISH", "HIGH", "LOW", true,
                        "LEVEL_1_CONSISTENT", 12, "HIGH", "UNCHANGED",
                        "CONFIRM", false, null)));
        decision.setIsAdopted(null);
        decision.setValidPeriod(validPeriod);
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
        return validPeriod;
    }

    private long persistDecisionTimeframes(String symbol, boolean bullish) {
        long latest5m = 0L;
        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            long latest = persistBoundaryBars(symbol, timeframe, bullish);
            if ("5m".equals(timeframe)) latest5m = latest;
        }
        return latest5m;
    }

    private long persistBoundaryBars(String symbol, String timeframe, boolean bullishStructure) {
        long intervalMs = timeframeMs(timeframe);
        long latestOpenMs = (System.currentTimeMillis() / intervalMs) * intervalMs - intervalMs;
        long latestCloseMs = latestOpenMs + intervalMs - 1L;
        long firstOpenMs = latestOpenMs - (49L * intervalMs);
        String batchId = "batch-" + symbol + "-" + latestOpenMs;
        List<OhlcvBarInput> bars = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            BigDecimal open = bullishStructure ? new BigDecimal("101.00") : new BigDecimal("102.00");
            BigDecimal high = new BigDecimal("103.00");
            BigDecimal low = new BigDecimal("99.00");
            BigDecimal close = bullishStructure ? new BigDecimal("102.00") : new BigDecimal("101.00");
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
            bars.add(new OhlcvBarInput(symbol, timeframe, openTimeMs, closeTimeMs,
                    open, high, low, close, new BigDecimal("1000.00"),
                    new BigDecimal("100000.00"), 100L, new BigDecimal("500.00"),
                    new BigDecimal("50000.00"), true));
        }
        OhlcvIngestionResult ingestion = persistedOhlcvIngestionService.ingest(new OhlcvIngestionBatch(
                "TEST_PUBLIC_PROVIDER", "SPOT", "/controlled-test/klines", OhlcvSourceState.READY,
                Instant.ofEpochMilli(latestCloseMs + 1_000L), "integration-fixture-v1", 1,
                "trace-" + symbol, batchId, bars));
        assertThat(ingestion.ready()).isTrue();
        assertThat(ingestion.insertedCount()).isEqualTo(50);
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
