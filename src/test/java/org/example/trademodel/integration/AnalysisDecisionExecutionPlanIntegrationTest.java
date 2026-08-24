package org.example.trademodel.integration;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
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
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.ConflictResolverResultMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanCandidateMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.DecisionChainAiOrchestratorService;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.service.OpportunityStateIdentity;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.example.trademodel.service.impl.PlanServiceImpl;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
@TestPropertySource(properties = "trade-model.ohlcv.provider.primary=binance")
@Transactional
@Tag("core-regression")
class AnalysisDecisionExecutionPlanIntegrationTest {

    private static final String ANALYSIS_ID = "ana-int-decision-plan-1";
    private static final String SYMBOL = "BTCUSDT";
    private static final Instant PLAN_VALIDITY_NOW = Instant.parse("2026-07-20T11:49:00Z");
    private static final DateTimeFormatter PLAN_VALIDITY_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    private AnalysisRunMapper analysisRunMapper;
    @Autowired
    private AssetStateMapper assetStateMapper;
    @Autowired
    private AssetStateService assetStateService;
    @Autowired
    private ExecutionPlanCandidateMapper executionPlanCandidateMapper;
    @Autowired
    private ConflictResolverResultMapper conflictResolverResultMapper;
    @Autowired
    private EvidenceItemMapper evidenceItemMapper;
    @Autowired
    private ScoreItemMapper scoreItemMapper;
    @Autowired
    private DecisionResultMapper decisionResultMapper;
    @Autowired
    private ExecutionPlanMapper executionPlanMapper;
    @Autowired
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Autowired
    private DashboardHomeService dashboardHomeService;
    @Autowired
    private OpportunityLogMapper opportunityLogMapper;
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
    @MockBean
    private DecisionChainAiOrchestratorService decisionChainAiOrchestratorService;

    @BeforeEach
    void cleanDashboardRuntimeTables() {
        ReflectionTestUtils.invokeMethod(
                dashboardHomeService,
                "setPlanValidityClock",
                Clock.fixed(PLAN_VALIDITY_NOW, ZoneOffset.UTC));
        for (String table : List.of(
                "tm_push_recheck_log",
                "tm_push_snapshot",
                "tm_opportunity_log",
                "tm_monitor_alert",
                "tm_account_risk_snapshot",
                "tm_execution_plan",
                "tm_conflict_resolver_result",
                "tm_execution_plan_candidate",
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
        when(decisionChainAiOrchestratorService.invoke(any())).thenAnswer(invocation -> {
            AiDecisionChainRequest request = invocation.getArgument(0);
            return List.of("SOLUSDT", "BNBUSDT", "ADAUSDT").contains(request.getSymbol())
                    ? decisionChainRoleResult(request)
                    : AiDecisionChainResult.failed(provider(request.getRole()), request.getRole(),
                    AiProviderCallStatus.NOT_CONFIGURED, "TEST_PROVIDER_NOT_CONFIGURED");
        });
    }

    @AfterEach
    void restoreDashboardPlanValidityClock() {
        ReflectionTestUtils.invokeMethod(
                dashboardHomeService,
                "setPlanValidityClock",
                Clock.systemUTC());
    }

    @Test
    void structuredAiRolePayloadPersistsAndLoadsWithoutLoss() {
        AnalysisRunResult result = runAiContractAnalysis("SOLUSDT", "req-ai-persistence");

        DecisionResult persisted = decisionResultMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(persisted).isNotNull();
        AiRoleResultsCodec.ParseResult parsed = aiRoleResultsCodec.parse(persisted.getAiRoleResults());

        assertThat(parsed.current()).isTrue();
        assertThat(parsed.payload().schemaVersion()).isEqualTo("v2");
        assertThat(parsed.payload().roles()).containsOnlyKeys("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(parsed.payload().roles().get("GPT_FINAL").coreJudgment().marketBias()).isEqualTo("BULLISH");
        assertThat(parsed.payload().roles().get("GPT_FINAL").supportingEvidence()).isEmpty();
        assertThat(parsed.payload().roles().get("GPT_FINAL").supportingEvidenceState()).isEqualTo("NONE_FOUND");
        assertThat(parsed.payload().roles().get("GEMINI_REVIEW").reviewResult()).isEqualTo("APPROVE");
        assertThat(parsed.payload().roles().get("GROK_CHALLENGE").failurePaths()).isEmpty();
        assertThat(parsed.payload().roles().get("GROK_CHALLENGE").failurePathState())
                .isEqualTo("NO_VERIFIABLE_FAILURE_PATH");
        assertThat(persisted.getAiRoleResults()).doesNotContain("providerRequestId", "Authorization", "apiKey");
    }

    @Test
    void nonFinalAnalysisKeepsAiExplanationSeparateFromFinalPlan() {
        String symbol = "BNBUSDT";
        AnalysisRunResult result = runAiContractAnalysis(symbol, "req-ai-home");
        DecisionResult persisted = decisionResultMapper.selectLatestByAnalysisId(result.getAnalysisId());
        assertThat(persisted.getMarketBiasHierarchy()).isEqualTo("WAIT");

        clearInvocations(aiDecisionOrchestratorService, decisionChainAiOrchestratorService);
        DashboardHomeVO home = dashboardHomeService.getHome(symbol, 6);
        verifyNoInteractions(aiDecisionOrchestratorService);
        verifyNoInteractions(decisionChainAiOrchestratorService);

        assertThat(home.getAssets()).isEmpty();
        assertThat(home.getSelectedSymbol()).isEqualTo(symbol);
        assertThat(home.getSelectedContextState()).isEqualTo("EXITED_TOP6");
        assertThat(home.getSelectedAssetContext()).isNotNull();
        assertThat(home.getSelectedAssetContext().getRawSymbol()).isEqualTo(symbol);
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("DATA_QUALITY_BLOCKED");
        assertThat(home.getAiDecision().getSchemaVersion()).isEqualTo("v2");
        assertThat(home.getAiDecision().getTabs()).allSatisfy(tab ->
                assertThat(tab.getResultAvailable()).isTrue());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tm_user_position", Integer.class)).isZero();
        assertThat(home.getSafety().getNotAutoTrading()).isTrue();
        assertThat(home.getSafety().getNotOrderExecution()).isTrue();
    }

    @Test
    void ruleLayerRemainsAuthoritative() {
        AnalysisRunResult result = runAiContractAnalysis("ADAUSDT", "req-ai-authority");
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
        assertThat(plan.getEntryZone()).isNull();
        assertThat(plan.getStopLoss()).isNull();
        assertThat(plan.getTakeProfitRules()).isNull();
        assertThat(plan.getInvalidCondition()).isNull();
        assertThat(plan.getManualReviewRequired()).isTrue();
        assertThat(plan.getNotTradeInstruction()).isTrue();
        assertThat(plan.getNotExecutable()).isTrue();
        assertThat(plan.getNotAutoTrading()).isTrue();
        assertThat(plan.getNotOrderExecution()).isTrue();
        assertThat(plan.getNotUserPositionCreation()).isTrue();

        DashboardHomeVO home = dashboardHomeService.getHome(SYMBOL, 6);
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("DATA_QUALITY_BLOCKED");
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
        assertThat(plan.getEntryZone()).isNull();
        assertThat(plan.getStopLoss()).isNull();
        assertThat(plan.getTakeProfitRules()).isNull();
        assertThat(plan.getInvalidCondition()).isNull();

        DashboardHomeVO home = dashboardHomeService.getHome(symbol, 6);
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("DATA_QUALITY_BLOCKED");
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
        assertThat(plan.getFinalPlan()).isFalse();
        assertThat(plan.getRuleValidationStatus()).isEqualTo("BLOCKED");
        assertThat(plan.getRuleVetoReason()).contains("ANALYSIS_TIMEFRAME_UNSUPPORTED");

        DashboardHomeVO home = dashboardHomeService.getHome("ADAUSDT", 6);
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("UNSUPPORTED_TIMEFRAME");
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
        assertThat(joined.getExecutionPlanSummary()).isEqualTo("plan invalidation wins");

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

    private AiDecisionChainResult decisionChainRoleResult(AiDecisionChainRequest request) {
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(provider(request.getRole()));
        result.setRole(request.getRole());
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setPayloadJson(switch (request.getRole()) {
            case GPT_FINAL -> gptDecisionChainPayload();
            case GEMINI_REVIEW -> geminiDecisionChainPayload();
            case GROK_CHALLENGE -> grokDecisionChainPayload();
        });
        result.setSelectedModel("test-role-model");
        return result;
    }

    private static String gptDecisionChainPayload() {
        return """
                {
                  "coreJudgment":{"marketBias":"BULLISH","opportunityState":"CANDIDATE","text":"Rule direction remains supported"},
                  "supportingEvidenceState":"NONE_FOUND","supportingEvidence":[],
                  "opposingEvidenceState":"NONE_FOUND","opposingEvidence":[],
                  "multiTimeframeExplanation":{"4h":"bullish context","1h":"bullish structure","15m":"setup forming","5m":"manual trigger pending"},
                  "biasAdjustment":{"before":"BULLISH","after":"WEAK_BULLISH","reason":"same-family evidence downgrade"},
                  "candidateSummary":{
                    "planMode":"PREPARATION","confidence":"MEDIUM","riskLevel":"MEDIUM","worthOpening":false,
                    "opportunityType":"TREND_CONTINUATION","recommendedAction":"WAIT_FOR_MANUAL_CONFIRMATION",
                    "entryLogic":"verified continuation logic","entryZone":"100-101","entrySource":"source-entry-1",
                    "entryReason":"verified entry boundary","triggerCondition":"manual confirmation after refresh",
                    "stopLogic":"rule invalidation boundary","stopZone":"95","stopSource":"source-stop-1",
                    "stopReason":"structure fails below boundary","targetLogic":"risk reward target structure",
                    "targetZones":"110 then 120","targetSource":"source-target-1","targetReason":"validated target zones",
                    "addPositionCondition":"manual review only","reducePositionCondition":"risk increases",
                    "abandonCondition":"source becomes stale","leverageSuggestion":"1x","positionSuggestion":"small",
                    "riskExplanation":"bounded manual decision risk","invalidCondition":"close below 95",
                    "invalidationSource":"source-stop-1","invalidationReason":"verified structure invalidation boundary",
                    "expectedRiskReward":2.0,"expectedRiskRewardSource":"source-rr-1",
                    "expectedRiskRewardReason":"entry stop target relation","validity":"until source expiry",
                    "triggerTimeframe":"5m","holdingHorizon":"intraday",
                    "revalidationRule":"refresh all verified evidence","summary":"Candidate only, not final"
                  }
                }
                """;
    }

    private static String geminiDecisionChainPayload() {
        return """
                {
                  "evidenceGapsState":"NONE_FOUND","evidenceGaps":[],
                  "logicConflictsState":"NONE_FOUND","logicConflicts":[],
                  "underestimatedRisksState":"NONE_FOUND","underestimatedRisks":[],
                  "downgradeSuggestion":{"before":"PREPARATION","after":"PREPARATION","reason":"no further downgrade","recoveryCondition":"new verified analysis"},
                  "reviewResult":"APPROVE","conflictLevel":"LEVEL_1_CONSISTENT",
                  "finalDirectionImpact":"UNCHANGED","confidenceAdjustment":"UNCHANGED",
                  "riskAdjustment":"UNCHANGED","planModeAdjustment":"UNCHANGED",
                  "recoveryCondition":"new verified analysis"
                }
                """;
    }

    private static String grokDecisionChainPayload() {
        return """
                {
                  "failurePathState":"NO_VERIFIABLE_FAILURE_PATH","failurePaths":[],
                  "opposingScenariosState":"NONE_FOUND","opposingScenarios":[],
                  "externalEventRisksState":"NONE_FOUND","externalEventRisks":[],
                  "microstructureRisksState":"NONE_FOUND","microstructureRisks":[],
                  "watchIndicatorsState":"NONE_FOUND","watchIndicators":[],
                  "challengeSummary":"No verifiable major challenge","currentDirectionChallenge":"No cross-family challenge",
                  "majorCounterEvidence":false,"conflictLevel":"LEVEL_1_CONSISTENT",
                  "riskAdjustment":"UNCHANGED","planModeImpact":"UNCHANGED"
                }
                """;
    }

    private AiProviderName provider(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> AiProviderName.OPENAI;
            case GEMINI_REVIEW -> AiProviderName.GEMINI;
            case GROK_CHALLENGE -> AiProviderName.XAI;
        };
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
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC()).minusSeconds(5);
        OffsetDateTime validityNow = OffsetDateTime.ofInstant(PLAN_VALIDITY_NOW, ZoneOffset.UTC);
        OffsetDateTime validFrom = validityNow.minusHours(1);
        OffsetDateTime expiresAt = validityNow.plusHours(1);
        Long poolItemId = jdbcTemplate.queryForObject("""
                SELECT id FROM tm_asset_pool_item
                WHERE owner_type = 'SYSTEM' AND owner_id = 0 AND active = TRUE AND symbol = ?
                """, Long.class, SYMBOL);
        Long assetId = jdbcTemplate.queryForObject("""
                SELECT asset_id FROM tm_asset_pool_item
                WHERE id = ?
                """, Long.class, poolItemId);
        String opportunityId = "opp-btcusdt-1h";
        String candidateId = "candidate-int-1";
        String resolverId = "resolver-int-1";
        String validationId = "validation-int-1";
        String traceId = "trace-int-1";
        String validPeriod = PLAN_VALIDITY_FORMAT.format(validFrom)
                + " ~ "
                + PLAN_VALIDITY_FORMAT.format(expiresAt);
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(ANALYSIS_ID);
        run.setSymbol(SYMBOL);
        run.setTimeframe("1h");
        run.setAnalysisTime(now);
        run.setRuleVersion("rules-test");
        run.setDataQualityScore(91);
        run.setTraceId(traceId);
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
        run.setOwnerType("SYSTEM");
        run.setOwnerId(0L);
        run.setAssetId(assetId);
        run.setPreview(false);
        analysisRunMapper.insert(run);

        AssetStateDO state = new AssetStateDO();
        state.setSymbol(SYMBOL);
        state.setTimeframe("1h");
        state.setOwnerType("SYSTEM");
        state.setOwnerId(0L);
        state.setAssetId(assetId);
        state.setPoolItemId(poolItemId);
        state.setState(org.example.trademodel.enums.AssetStateEnum.CANDIDATE);
        state.setConfusedScore(8);
        state.setConfusedLowStreak(0);
        state.setOpportunityId(opportunityId);
        state.setStateEnteredAt(now);
        state.setLastTransitionReason("CONTROLLED_INTEGRATION_FIXTURE");
        state.setLastTriggerSource("MANUAL_API");
        state.setLastAnalysisId(ANALYSIS_ID);
        state.setLastUpdateTime(now);
        state.setTraceId(traceId);
        state.setRuleVersion("rules-test");
        assetStateMapper.mergeUpsertCore(state);
        assetStateService.recordOpportunityProjection(
                new OpportunityStateIdentity("SYSTEM", 0L, assetId, SYMBOL, "1h"),
                poolItemId,
                ANALYSIS_ID,
                traceId,
                "rules-test",
                88,
                "HIGH",
                "LOW",
                "{\"source\":\"CONTROLLED_INTEGRATION_FIXTURE\"}");

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
        decision.setAiRoleResults(aiRoleResultsCodec.serializeDecisionChain(
                ANALYSIS_ID,
                traceId,
                "BULLISH",
                Map.of(),
                new AiRoleResultsPayload.SynthesisPayload(
                        "BULLISH", "HIGH", "LOW", "CONFIRMATION", true,
                        "LEVEL_1_CONSISTENT", 12, "UNCHANGED", "UNCHANGED",
                        "UNCHANGED", false, null, "FINAL_VALIDATED", null)));
        decision.setIsAdopted(null);
        decision.setValidPeriod(validPeriod);
        decision.setValidFrom(validFrom);
        decision.setExpiresAt(expiresAt);
        decision.setInvalidCondition("decision invalidation fallback");
        decision.setEvidenceSummary("controlled evidence summary");
        decision.setExplanationJson("{\"summary\":\"controlled\"}");
        decision.setReviewReasons("[]");
        decision.setAiConflictLevel("LEVEL_1_CONSISTENT");
        decision.setAiConflictScore(12);
        decision.setAiPlanMode("CONFIRMATION");
        decision.setRuleMarketBias("BULLISH");
        decision.setFinalMarketBias("BULLISH");
        decision.setRuleConfidence("HIGH");
        decision.setRuleRisk("LOW");
        decision.setRulePlanMode("CONFIRMATION");
        decision.setRuleCanExecute(true);
        decision.setCandidatePlanMode("CONFIRMATION");
        decision.setFinalPlanMode("CONFIRMATION");
        decision.setBiasAdjustmentReason("RULE_DIRECTION_PRESERVED");
        decision.setPlanModeAdjustmentReason("RULE_DIRECTION_PRESERVED");
        decision.setConfusedScore(8);
        decision.setAssetStateSnapshot("{\"state\":\"CANDIDATE\"}");
        decision.setCreateTime(now.plusSeconds(1));
        decisionResultMapper.insert(decision);

        ExecutionPlanCandidateDO candidate = new ExecutionPlanCandidateDO();
        candidate.setCandidateId(candidateId);
        candidate.setOpportunityId(opportunityId);
        candidate.setAnalysisId(ANALYSIS_ID);
        candidate.setTraceId(traceId);
        candidate.setRuleDirection("BULLISH");
        candidate.setRuleConfidence("HIGH");
        candidate.setRuleRisk("LOW");
        candidate.setRulePlanMode("CONFIRMATION");
        candidate.setRuleCanExecute(true);
        candidate.setCandidateDirection("BULLISH");
        candidate.setBiasAdjustmentReason("RULE_DIRECTION_PRESERVED");
        candidate.setPlanMode("CONFIRMATION");
        candidate.setConfidenceLevel("HIGH");
        candidate.setRiskLevel("LOW");
        candidate.setWorthOpening(true);
        candidate.setRecommendedAction("MANUAL_REVIEW");
        candidate.setAssetId(assetId);
        candidate.setRuleVersion("rules-test");
        candidate.setOpportunityType("TREND_CONTINUATION");
        candidate.setEntryLogic("verified continuation logic");
        candidate.setEntryZone("63000-64000 USDT");
        candidate.setEntrySource("TEST_CONTROLLED");
        candidate.setEntryReason("verified entry boundary");
        candidate.setTriggerCondition("manual confirmation after fresh analysis");
        candidate.setStopLogic("rule invalidation boundary");
        candidate.setStopLoss("60800 USDT");
        candidate.setStopSource("TEST_CONTROLLED");
        candidate.setStopReason("verified invalidation boundary");
        candidate.setTargetLogic("risk reward target structure");
        candidate.setTakeProfitRules("66000 / 68500 / 71000 USDT");
        candidate.setTargetSource("TEST_CONTROLLED");
        candidate.setTargetReason("verified target boundaries");
        candidate.setAddPositionCondition("manual review only");
        candidate.setReducePositionCondition("risk increases");
        candidate.setAbandonCondition("source becomes stale");
        candidate.setLeverageSuggestion("3x");
        candidate.setPositionSuggestion("10% account risk cap");
        candidate.setRiskExplanation("bounded manual decision risk");
        candidate.setInvalidCondition("plan invalidation wins");
        candidate.setInvalidationSource("TEST_CONTROLLED");
        candidate.setInvalidationReason("verified structure invalidation boundary");
        candidate.setExpectedRiskReward(new BigDecimal("2.0"));
        candidate.setExpectedRiskRewardSource("RULE_CALCULATION");
        candidate.setExpectedRiskRewardReason("validated entry stop target relation");
        candidate.setValidity(validPeriod);
        candidate.setAnalysisTimeframesJson("[\"4h\",\"1h\",\"15m\",\"5m\"]");
        candidate.setTriggerTimeframe("5m");
        candidate.setValidFrom(validFrom.toLocalDateTime());
        candidate.setValidUntil(expiresAt.toLocalDateTime());
        candidate.setHoldingHorizon("INTRADAY");
        candidate.setRevalidationRule("refresh all verified evidence");
        candidate.setSourceRefsJson("[\"test://analysis-decision-plan\"]");
        candidate.setEvidenceRefsJson("[\"ev-int-1\"]");
        candidate.setScoreRefsJson("[\"sc-int-1\"]");
        candidate.setDataQuality(91);
        candidate.setConfusedScore(8);
        TmAccountRiskSnapshotDO accountRiskSnapshot = verifiedAccountRiskSnapshot(now, traceId);
        accountRiskSnapshotMapper.insert(accountRiskSnapshot);
        candidate.setAccountRiskSnapshotId(accountRiskSnapshot.getId());
        candidate.setSummary("GPT candidate, pending resolver and rule validation");
        candidate.setCandidateSource("GPT_FINAL");
        candidate.setCandidateStatus("VALIDATED");
        candidate.setPayloadJson("{\"source\":\"GPT_FINAL\",\"roleState\":\"READY\"}");
        candidate.setNotFinalPlan(true);
        candidate.setNotStateMachineMutation(true);
        candidate.setNotUserPositionCreation(true);
        candidate.setCreatedAt(now.plusSeconds(1));
        executionPlanCandidateMapper.insert(candidate);

        ConflictResolverResultDO resolver = new ConflictResolverResultDO();
        resolver.setResolverResultId(resolverId);
        resolver.setCandidateId(candidateId);
        resolver.setAnalysisId(ANALYSIS_ID);
        resolver.setTraceId(traceId);
        resolver.setRuleDirection("BULLISH");
        resolver.setRuleConfidence("HIGH");
        resolver.setRuleRisk("LOW");
        resolver.setRulePlanMode("CONFIRMATION");
        resolver.setRuleCanExecute(true);
        resolver.setDataQualityScore(91);
        resolver.setConfusedScore(8);
        resolver.setAccountRiskState("ALLOWED");
        resolver.setGeminiReviewJson("{\"roleState\":\"READY\",\"dataState\":\"READY\",\"reviewResult\":\"approve\"}");
        resolver.setGrokChallengeJson("{\"roleState\":\"READY\",\"dataState\":\"READY\",\"failurePathState\":\"NO_VERIFIABLE_FAILURE_PATH\",\"failurePaths\":[]}");
        resolver.setConflictLevel("LEVEL_1_CONSISTENT");
        resolver.setConflictScore(12);
        resolver.setPlanModeBefore("CONFIRMATION");
        resolver.setPlanModeAfter("CONFIRMATION");
        resolver.setConfidenceBefore("HIGH");
        resolver.setConfidenceAfter("HIGH");
        resolver.setRiskBefore("LOW");
        resolver.setRiskAfter("LOW");
        resolver.setBiasBefore("BULLISH");
        resolver.setBiasAfter("BULLISH");
        resolver.setAdjustmentReason("RULE_DIRECTION_PRESERVED");
        resolver.setRecoveryCondition("new verified analysis");
        resolver.setConfusedDecision(false);
        resolver.setRuleDirectionPreserved(true);
        resolver.setCreatedAt(now.plusSeconds(1));
        conflictResolverResultMapper.insert(resolver);

        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-int-1");
        plan.setAnalysisId(ANALYSIS_ID);
        plan.setPlanMode("CONFIRMATION");
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setSourceCompletenessSummary("controlled source gate valid");
        plan.setRecommendedAction("MANUAL_REVIEW");
        plan.setEntryZone("63000-64000 USDT");
        plan.setStopLoss("60800 USDT");
        plan.setTakeProfitRules("66000 / 68500 / 71000 USDT");
        plan.setLeverageSuggestion("3x");
        plan.setPositionSuggestion("10% account risk cap");
        plan.setAccountRiskJson("{\"riskAllowed\":true}");
        plan.setInvalidCondition("plan invalidation wins");
        plan.setInvalidationSource("TEST_CONTROLLED");
        plan.setInvalidationReason("verified structure invalidation boundary");
        plan.setCandidateId(candidateId);
        plan.setOpportunityId(opportunityId);
        plan.setResolverResultId(resolverId);
        plan.setTraceId(traceId);
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setRuleValidationStatus("PASS");
        plan.setFinalizedAt(now.plusSeconds(2));
        plan.setFinalPlan(true);
        plan.setAssetId(assetId);
        plan.setRuleVersion("rules-test");
        plan.setRuleMarketBias("BULLISH");
        plan.setFinalMarketBias("BULLISH");
        plan.setCandidatePlanMode("CONFIRMATION");
        plan.setFinalPlanMode("CONFIRMATION");
        plan.setBiasAdjustmentReason("RULE_DIRECTION_PRESERVED");
        plan.setPlanModeAdjustmentReason("RULE_DIRECTION_PRESERVED");
        plan.setAdjustmentReason("RULE_DIRECTION_PRESERVED");
        plan.setOpportunityType("TREND_CONTINUATION");
        plan.setEntryLogic("verified continuation logic");
        plan.setEntrySource("TEST_CONTROLLED");
        plan.setEntryReason("verified entry boundary");
        plan.setTriggerCondition("manual confirmation after fresh analysis");
        plan.setStopLogic("rule invalidation boundary");
        plan.setStopSource("TEST_CONTROLLED");
        plan.setStopReason("verified invalidation boundary");
        plan.setTargetLogic("risk reward target structure");
        plan.setTargetSource("TEST_CONTROLLED");
        plan.setTargetReason("verified target boundaries");
        plan.setAddPositionCondition("manual review only");
        plan.setReducePositionCondition("risk increases");
        plan.setAbandonCondition("source becomes stale");
        plan.setRiskExplanation("bounded manual decision risk");
        plan.setLeverageLimit("3x");
        plan.setPositionLimit("10% account risk cap");
        plan.setRiskLimit(new BigDecimal("0.10"));
        plan.setAccountRiskSnapshotId(accountRiskSnapshot.getId());
        plan.setExecutionFeasibilityStatus("VERIFIED");
        plan.setSlippageStatus("VERIFIED");
        plan.setDepthStatus("VERIFIED");
        plan.setEntryDriftStatus("VERIFIED");
        plan.setTriggerStatus("VERIFIED");
        plan.setExecutionFeasibilityReason("CONTROLLED_VERIFIED_EXECUTION_CONTEXT");
        plan.setExecutionFeasibilityObservedAt(validityNow.minusMinutes(1).toLocalDateTime());
        plan.setExecutionFeasibilityFreshUntil(validityNow.plusHours(1).toLocalDateTime());
        plan.setExecutionFeasibilitySourceRefsJson("[\"test://verified-execution-context\"]");
        plan.setExpectedRiskReward(new BigDecimal("2.0"));
        plan.setExpectedRiskRewardSource("RULE_CALCULATION");
        plan.setExpectedRiskRewardReason("validated entry stop target relation");
        plan.setAnalysisTimeframesJson("[\"4h\",\"1h\",\"15m\",\"5m\"]");
        plan.setTriggerTimeframe("5m");
        plan.setValidFrom(validFrom.toLocalDateTime());
        plan.setValidUntil(expiresAt.toLocalDateTime());
        plan.setHoldingHorizon("INTRADAY");
        plan.setRevalidationRule("refresh all verified evidence");
        plan.setDataQuality(91);
        plan.setSourceRefsJson("[\"test://analysis-decision-plan\"]");
        plan.setEvidenceRefsJson("[\"ev-int-1\"]");
        plan.setScoreRefsJson("[\"sc-int-1\"]");
        plan.setValidationResultId(validationId);
        plan.setValidationReasons("[\"SOURCE_GATE_PASS\",\"RULE_DIRECTION_PRESERVED\"]");
        plan.setSourceStatus("VALID");
        plan.setManualReviewRequired(true);
        plan.setNotTradeInstruction(true);
        plan.setNotExecutable(true);
        plan.setNotAutoTrading(true);
        plan.setNotOrderExecution(true);
        plan.setNotUserPositionCreation(true);
        plan.setCreateTime(now.plusSeconds(2));
        executionPlanMapper.insert(plan);

        OpportunityLogDO relation = new OpportunityLogDO();
        relation.setOpportunityId("opp-int-decision-plan-1");
        relation.setOpportunityKey(ANALYSIS_ID + ":dec-int-1");
        relation.setAnalysisId(ANALYSIS_ID);
        relation.setDecisionId("dec-int-1");
        relation.setExecutionPlanId("plan-int-1");
        relation.setSymbol(SYMBOL);
        relation.setTimeframe("1h");
        relation.setDirection("LONG");
        relation.setLifecycleStatus("PENDING_EVALUATION");
        relation.setAnchorTime(now.plusSeconds(2));
        relation.setTargetHit(false);
        relation.setInvalidationHit(false);
        relation.setPushPresent(false);
        relation.setRiskBlockedEvidence(false);
        relation.setUserPositionPresent(false);
        relation.setSourceType("AUTHORITATIVE_ANALYSIS");
        relation.setSourceReference("analysisId=" + ANALYSIS_ID + ";decisionId=dec-int-1");
        relation.setReasonCodes("AUTHORITATIVE_PLAN_RELATION");
        relation.setTraceId("trace-int-1");
        relation.setCreatedAt(now.plusSeconds(2));
        relation.setUpdatedAt(now.plusSeconds(2));
        opportunityLogMapper.insert(relation);
        return validPeriod;
    }

    private TmAccountRiskSnapshotDO verifiedAccountRiskSnapshot(LocalDateTime now, String traceId) {
        TmAccountRiskSnapshotDO snapshot = new TmAccountRiskSnapshotDO();
        snapshot.setAnalysisId(ANALYSIS_ID);
        snapshot.setSymbol(SYMBOL);
        snapshot.setOwnerType("SYSTEM");
        snapshot.setOwnerId(0L);
        snapshot.setAccountRiskStatus("ALLOWED");
        snapshot.setRiskLevelSnapshot("LOW");
        snapshot.setRiskAllowed(true);
        snapshot.setRiskReasonCode("CONTROLLED_VERIFIED_ACCOUNT_RISK");
        snapshot.setRiskReasonText("Controlled integration account-risk fixture");
        snapshot.setPositionExposure(new BigDecimal("0.10"));
        snapshot.setMaxAllowedExposure(new BigDecimal("0.20"));
        snapshot.setCandidateLeverage(new BigDecimal("3"));
        snapshot.setMaxAllowedLeverage(new BigDecimal("5"));
        snapshot.setSourceStatus("VERIFIED");
        snapshot.setObservedAt(now);
        snapshot.setFreshUntil(now.plusHours(1));
        snapshot.setSnapshotSource("CONTROLLED_INTEGRATION_FIXTURE");
        snapshot.setSnapshotVersion(1);
        snapshot.setSourceNote("TEST_ONLY_VERIFIED_SOURCE");
        snapshot.setTraceId(traceId);
        snapshot.setCreateTime(now);
        return snapshot;
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
                "BINANCE_PUBLIC", "SPOT", "/controlled-test/klines", OhlcvSourceState.READY,
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
