package org.example.trademodel.controller;

import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.dashboard.DefaultDashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.ExecutionPlanDisplayAdapter;
import org.example.trademodel.service.dashboard.PaperObservationDisplayAdapter;
import org.example.trademodel.service.dashboard.PlanBoundaryDisplayAdapter;
import org.example.trademodel.service.dashboard.RiskActionGuardDisplayAdapter;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class DashboardControllerTest {

    @Mock
    private DecisionService decisionService;
    @Mock
    private SystemHealthService systemHealthService;
    @Mock
    private MonitorService monitorService;
    @Mock
    private RuntimeMetricService runtimeMetricService;
    @Mock
    private RealMarketEnvironmentService realMarketEnvironmentService;
    @Mock
    private MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    @Mock
    private EvidenceService evidenceService;
    @Mock
    private ScoreService scoreService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter = (symbol, decision, fallbackDisplay) -> fallbackDisplay;
        ExecutionPlanDisplayAdapter executionPlanDisplayAdapter = (decision, planBoundaryDisplay, fallbackDisplay) -> fallbackDisplay;
        RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter = (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> fallbackDisplay;
        PaperObservationDisplayAdapter paperObservationDisplayAdapter = (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay;
        DashboardController controller = new DashboardController(
                decisionService,
                systemHealthService,
                monitorService,
                runtimeMetricService,
                realMarketEnvironmentService,
                marketEnvironmentSnapshotMapper,
                evidenceService,
                scoreService,
                new DefaultDashboardSourceTraceDetailAdapter(),
                planBoundaryDisplayAdapter,
                executionPlanDisplayAdapter,
                riskActionGuardDisplayAdapter,
                paperObservationDisplayAdapter
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void summary_json_exposesPendingCountOnSystemStatus() throws Exception {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setPendingCount(7);
        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus.pendingCount").value(7));
    }

    @Test
    void summary_json_exposesConfusedCountOnSystemStatus() throws Exception {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setConfusedCount(4);
        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus.confusedCount").value(4));
    }

    @Test
    void summary_json_exposesReverseSignalCountOnSystemStatus() throws Exception {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setReverseSignalCount(3);
        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus.reverseSignalCount").value(3));
    }

    @Test
    void summary_json_exposesCoreFieldsOnFirstDecision() throws Exception {
        stubSummaryData();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        when(decisionService.getLatestDecisionResults(12)).thenReturn(List.of(decision));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisions[0].marketBiasHierarchy").value("H1>H4>D1"))
                .andExpect(jsonPath("$.decisions[0].isWorthOpening").value(true))
                .andExpect(jsonPath("$.decisions[0].recommendedAction").value("OPEN_LONG"))
                .andExpect(jsonPath("$.decisions[0].aiConflictLevel").value("L2"))
                .andExpect(jsonPath("$.decisions[0].aiConflictScore").value(42))
                .andExpect(jsonPath("$.decisions[0].aiPlanMode").value("AGGRESSIVE"))
                .andExpect(jsonPath("$.decisions[0].confusedScore").value(3));
    }

    @Test
    void detail_json_exposesCoreFieldsOnDecision() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-btc");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc")).thenReturn(Collections.emptyList());
        ScoreBriefVO score = new ScoreBriefVO();
        score.setScoreType("综合评分");
        score.setScoreValue(81.5);
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc")).thenReturn(List.of(score));

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.marketBiasHierarchy").value("H1>H4>D1"))
                .andExpect(jsonPath("$.decision.isWorthOpening").value(true))
                .andExpect(jsonPath("$.decision.recommendedAction").value("OPEN_LONG"))
                .andExpect(jsonPath("$.decision.aiConflictLevel").value("L2"))
                .andExpect(jsonPath("$.decision.aiConflictScore").value(42))
                .andExpect(jsonPath("$.decision.aiPlanMode").value("AGGRESSIVE"))
                .andExpect(jsonPath("$.decision.confusedScore").value(3))
                .andExpect(jsonPath("$.evidenceTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems[0].scoreType").value("综合评分"))
                .andExpect(jsonPath("$.scoreTopItems[0].scoreValue").value(81.5))
                .andExpect(jsonPath("$.sourceTrace.fallbackStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.sourceTrace.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.sourceTrace.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTrace.missingFields").isArray())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'runtimeKlineContext')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entryPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpPriceSources')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'rrSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'liquiditySource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'multiTimeframeSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'eventSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'wickSource')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.fallbackStatus").value("SAFE_FAIL_CLOSED_ONLY"))
                .andExpect(jsonPath("$.derivativesRiskContext.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.derivativesRiskContext.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'openInterestHistory')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'fundingHistory')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'liquidationCluster')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'leverageDistribution')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'longShortRatio')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'liquidityStress')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'eventWindowBlockers')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'wickConfirmationSources')]").exists());
    }

    @Test
    void detail_json_exposesMarketEnvironmentMini_fromSnapshot_whenAvailable() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        MarketEnvironmentSnapshotDO snapshot = new MarketEnvironmentSnapshotDO();
        snapshot.setSummary("snapshot summary: BTCUSDT env");
        snapshot.setEnvironmentType("trend_market");
        snapshot.setRiskMode("normal");
        snapshot.setSourceType("BINANCE_24H_HEURISTIC");
        EvidenceBriefVO evidence = new EvidenceBriefVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("突破后回踩确认");
        evidence.setDirection("BULLISH");
        evidence.setSource("SYSTEM_GENERATED");
        decision.setAnalysisId("ana-btc-env");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-btc-env")).thenReturn(snapshot);
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc-env")).thenReturn(List.of(evidence));
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc-env")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketEnvironmentMini.summary").value("snapshot summary: BTCUSDT env"))
                .andExpect(jsonPath("$.marketEnvironmentMini.environmentType").value("trend_market"))
                .andExpect(jsonPath("$.marketEnvironmentMini.riskMode").value("normal"))
                .andExpect(jsonPath("$.marketEnvironmentMini.sourceType").value("BINANCE_24H_HEURISTIC"))
                .andExpect(jsonPath("$.evidenceTopItems[0].evidenceType").value("价格结构"))
                .andExpect(jsonPath("$.evidenceTopItems[0].description").value("突破后回踩确认"))
                .andExpect(jsonPath("$.evidenceTopItems[0].direction").value("BULLISH"))
                .andExpect(jsonPath("$.evidenceTopItems[0].source").value("SYSTEM_GENERATED"))
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isEmpty());
    }

    @Test
    void detail_json_exposesMarketEnvironmentMini_fromHeuristic_whenSnapshotMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-btc-heuristic");
        MarketEnvironmentVO marketEnvironment = new MarketEnvironmentVO();
        marketEnvironment.setSummary("fallback summary from realtime heuristic");
        marketEnvironment.setEnvironmentType("range_market");
        marketEnvironment.setRiskMode("elevated");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-btc-heuristic")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.of(marketEnvironment));
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc-heuristic")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc-heuristic")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketEnvironmentMini.summary").value("fallback summary from realtime heuristic"))
                .andExpect(jsonPath("$.marketEnvironmentMini.environmentType").value("range_market"))
                .andExpect(jsonPath("$.marketEnvironmentMini.riskMode").value("elevated"))
                .andExpect(jsonPath("$.marketEnvironmentMini.sourceType").value("BINANCE_24H_HEURISTIC"))
                .andExpect(jsonPath("$.evidenceTopItems").isArray())
                .andExpect(jsonPath("$.evidenceTopItems").isEmpty())
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isEmpty());
    }

    @Test
    void detail_json_exposesMarketEnvironmentMini_fallback_whenSnapshotAndHeuristicMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-btc-missing");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-btc-missing")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc-missing")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc-missing")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketEnvironmentMini").exists())
                .andExpect(jsonPath("$.marketEnvironmentMini.summary").value(nullValue()))
                .andExpect(jsonPath("$.marketEnvironmentMini.environmentType").value(nullValue()))
                .andExpect(jsonPath("$.marketEnvironmentMini.riskMode").value(nullValue()))
                .andExpect(jsonPath("$.marketEnvironmentMini.sourceType").value("PLACEHOLDER_FALLBACK"));
    }

    @Test
    void summary_usesDefaultLimitWhenAbsent() throws Exception {
        stubSummaryData();
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(jsonPath("$.decisions").isArray())
                .andExpect(jsonPath("$.openPositionCount").value(0));

        verify(decisionService).getLatestDecisionResults(12);
        verify(runtimeMetricService).recordDuration(eq("dashboard.summary"), anyLong());
    }

    @Test
    void summary_clampsLimitToGuardrailRange() throws Exception {
        stubSummaryData();
        when(decisionService.getLatestDecisionResults(24)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary").param("limit", "200"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/summary").param("limit", "0"))
                .andExpect(status().isOk());

        verify(decisionService).getLatestDecisionResults(24);
        verify(decisionService).getLatestDecisionResults(1);
    }

    @Test
    void detail_rejectsBlankSymbolAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detail_rejectsMissingSymbolParameter() throws Exception {
        mockMvc.perform(get("/api/dashboard/detail"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detail_doesNotExposeDeprecationHeader() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("AAPL")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("AAPL", null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(jsonPath("$.evidenceTopItems").isArray())
                .andExpect(jsonPath("$.evidenceTopItems").isEmpty())
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isEmpty());
    }

    @Test
    void refresh_keepsLegacyContractAndMetrics() throws Exception {
        stubSummaryData();
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/refresh"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</api/dashboard/summary>; rel=\"alternate\"; title=\"replacement\""))
                .andExpect(jsonPath("$.decisions").isArray())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.systemHealth").isMap());

        verify(decisionService).getLatestDecisionResults(12);
        verify(runtimeMetricService).recordDuration(eq("dashboard.refresh"), anyLong());
    }

    private void stubSummaryData() {
        when(decisionService.getLightSystemStatus()).thenReturn(null);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
    }

    private static DecisionResultVO newDecisionWithCoreDashboardTruthFields() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        row.setMarketBiasHierarchy("H1>H4>D1");
        row.setIsWorthOpening(Boolean.TRUE);
        row.setRecommendedAction("OPEN_LONG");
        row.setAiConflictLevel("L2");
        row.setAiConflictScore(42);
        row.setAiPlanMode("AGGRESSIVE");
        row.setConfusedScore(3);
        return row;
    }
}
