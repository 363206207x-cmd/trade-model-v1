package org.example.trademodel.controller;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.dashboard.DefaultDashboardRuntimeKlineContextAdapter;
import org.example.trademodel.service.dashboard.DefaultDashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.DashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.ExecutionPlanDisplayAdapter;
import org.example.trademodel.service.dashboard.PaperObservationDisplayAdapter;
import org.example.trademodel.service.dashboard.PlanBoundaryDisplayAdapter;
import org.example.trademodel.service.dashboard.RiskActionGuardDisplayAdapter;
import org.example.trademodel.service.impl.RuntimeKlineContextAssemblyServiceImpl;
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

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private static final Path DASHBOARD_TEMPLATE =
            Path.of("src/main/resources/templates/dashboard.html");
    private static final String INTERNAL_PUSH_PREVIEW_START =
            "<section class=\"card module-status-card review-display-card\" id=\"internalPushPreviewDisplay\"";
    private static final String SECTION_END = "</section>";

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
        mockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                new DefaultDashboardSourceTraceDetailAdapter(
                        new DefaultDashboardRuntimeKlineContextAdapter((symbol, timeframe, requiredWindowSize, maxReadLagMs) ->
                                readiness(
                                        PersistedOhlcvReadinessStatus.MISSING,
                                        PersistedOhlcvStaleReasonCode.NO_BARS_FOR_SYMBOL_TIMEFRAME,
                                        "No closed persisted OHLCV bars exist for symbol/timeframe.",
                                        List.of("persistedOhlcvWindow", "klineItems")
                                )
                        )
                )
        )).build();
    }

    @Test
    void dashboardTemplateRendersInternalPushPreviewDisplayGateAsReviewOnlyAndNonSendable() throws Exception {
        String section = normalizedInternalPushPreviewDisplay();

        assertThat(section).contains("internal push preview");
        assertThat(section).contains("review-only preview");
        assertThat(section).contains("not a trade instruction");
        assertThat(section).contains("manual review required");
        assertThat(section).contains("recheck required");
        assertThat(section).contains("risk action guard required");
        assertThat(section).contains("external channel disabled");
        assertThat(section).contains("no telegram / email / webhook connected");
        assertThat(section).contains("no readiness / point / entry / stop / tp / rr generated");
        assertThat(section).contains("blocked / fail-closed explanation");
        assertThat(section).contains("watchlist pool");
        assertThat(section).contains("display slots");
        assertThat(section).contains("not display slots");

        assertThat(section).doesNotContain("telegram enabled");
        assertThat(section).doesNotContain("email enabled");
        assertThat(section).doesNotContain("webhook enabled");
        assertThat(section).doesNotContain("external channel enabled");
        assertThat(section).doesNotContain("readiness generated");
        assertThat(section).doesNotContain("point generated");
        assertThat(section).doesNotContain("entry generated");
        assertThat(section).doesNotContain("stop generated");
        assertThat(section).doesNotContain("take profit generated");
        assertThat(section).doesNotContain("place order");
        assertThat(section).doesNotContain("execute order");
        assertThat(section).doesNotContain("start auto-trading");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyPositionSyncProviderStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/system/position-sync-status");
        assertThat(html).contains("providerStatusValue");
        assertThat(html).contains("providerActiveValue");
        assertThat(html).contains("providerConfiguredValue");
        assertThat(html).contains("providerFallbackValue");
        assertThat(html).contains("providerFallbackReasonValue");
        assertThat(html).contains("providerLastSyncSuccessValue");
        assertThat(html).contains("providerFreshnessValue");
        assertThat(html).contains("providerOpenCountValue");
        assertThat(html).contains("providerLastSyncTimeValue");
        assertThat(html).contains("REVIEW_ONLY_POSITION_SYNC_READY");
        assertThat(html).contains("SIMULATED_FALLBACK");
        assertThat(html).contains("INCOMPLETE");
        assertThat(html).contains("BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("只读状态，不是交易建议");
        assertThat(html).contains("模拟来源不等于真实 Binance 持仓");
        assertThat(html).contains("currentOpenPositionCount");
        assertThat(html).contains("lastSyncEndTime");
        assertThat(html).contains("lastSyncStartTime");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyWatchlistRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/rule/push-watchlist");
        assertThat(html).contains("watchlistStatusPanel");
        assertThat(html).contains("watchlistRuntimeStatusValue");
        assertThat(html).contains("watchlistSymbolsValue");
        assertThat(html).contains("watchlistSourceValue");
        assertThat(html).contains("watchlistFailClosedValue");
        assertThat(html).contains("watchlistDisplaySlotsBoundaryValue");
        assertThat(html).contains("watchlistReviewOnlyValue");
        assertThat(html).contains("WATCHLIST_REVIEW_ONLY_READY");
        assertThat(html).contains("WATCHLIST_EMPTY_FAIL_CLOSED");
        assertThat(html).contains("WATCHLIST_CONFIG_MISSING");
        assertThat(html).contains("BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("Display Slots 只是首页展示位");
        assertThat(html).contains("Display Slots 不是候选池");
        assertThat(html).contains("默认六个币不是候选池");
        assertThat(html).contains("只读状态，不发送 Push");
        assertThat(html).contains("不在 Watchlist Pool 不进入候选/推送/扫描/点位");
    }

    private DashboardController controllerWith(DashboardSourceTraceDetailAdapter sourceTraceDetailAdapter) {
        PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter = (symbol, decision, fallbackDisplay) -> fallbackDisplay;
        ExecutionPlanDisplayAdapter executionPlanDisplayAdapter = (decision, planBoundaryDisplay, fallbackDisplay) -> fallbackDisplay;
        RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter = (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> fallbackDisplay;
        PaperObservationDisplayAdapter paperObservationDisplayAdapter = (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay;
        return new DashboardController(
                decisionService,
                systemHealthService,
                monitorService,
                runtimeMetricService,
                realMarketEnvironmentService,
                marketEnvironmentSnapshotMapper,
                evidenceService,
                scoreService,
                sourceTraceDetailAdapter,
                planBoundaryDisplayAdapter,
                executionPlanDisplayAdapter,
                riskActionGuardDisplayAdapter,
                paperObservationDisplayAdapter
        );
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
        decision.setDecisionId("dec-btc");
        decision.setAnalysisId("ana-btc");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        decision.setTimeframe("1h");
        decision.setMultiTfConvergence("STRONG");
        decision.setDataQualityScore(91);
        decision.setLatestPrice(BigDecimal.valueOf(68100));
        decision.setPriceUpdateTimeMs(1710000000000L);
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
                .andExpect(jsonPath("$.sourceTrace.decisionId").value("dec-btc"))
                .andExpect(jsonPath("$.sourceTrace.decisionIdSource").value("DecisionResultVO.decisionId"))
                .andExpect(jsonPath("$.sourceTrace.analysisId").value("ana-btc"))
                .andExpect(jsonPath("$.sourceTrace.analysisIdSource").value("DecisionResultVO.analysisId"))
                .andExpect(jsonPath("$.sourceTrace.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.sourceTrace.symbolSource").value("DecisionResultVO.symbol"))
                .andExpect(jsonPath("$.sourceTrace.decisionCreateTime").exists())
                .andExpect(jsonPath("$.sourceTrace.decisionCreateTimeSource").value("DecisionResultVO.createTime"))
                .andExpect(jsonPath("$.sourceTrace.timeframe").value("1h"))
                .andExpect(jsonPath("$.sourceTrace.timeframeSource").value("DecisionResultVO.timeframe"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineContextStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineContextSource").value("dashboardDetail.noRuntimeKlineContext"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineReadinessStatus").value("MISSING"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineStaleReasonCode").value("NO_BARS_FOR_SYMBOL_TIMEFRAME"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineStaleReasonText").value("No closed persisted OHLCV bars exist for symbol/timeframe."))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineReadinessMissingFields[?(@ == 'persistedOhlcvWindow')]").exists())
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineReadinessMissingFields[?(@ == 'klineItems')]").exists())
                .andExpect(jsonPath("$.runtimeKlineContext.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.runtimeKlineContext.timeframe").value("1h"))
                .andExpect(jsonPath("$.runtimeKlineContext.fallbackStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.runtimeKlineContext.latestPrice").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvReadinessStatus").value("MISSING"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvStaleReasonCode").value("NO_BARS_FOR_SYMBOL_TIMEFRAME"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvStaleReasonText").value("No closed persisted OHLCV bars exist for symbol/timeframe."))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvMissingFields[?(@ == 'persistedOhlcvWindow')]").exists())
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvMissingFields[?(@ == 'klineItems')]").exists())
                .andExpect(jsonPath("$.runtimeKlineContext.entryPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.stopPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.tpPriceSources").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.rrSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.liquiditySource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.eventSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.wickSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.runtimeKlineContext.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTrace.quoteLatestPrice").value(68100))
                .andExpect(jsonPath("$.sourceTrace.quoteLatestPriceSource").value("DecisionResultVO.latestPrice"))
                .andExpect(jsonPath("$.sourceTrace.quotePriceUpdateTimeMs").value(1710000000000L))
                .andExpect(jsonPath("$.sourceTrace.quotePriceUpdateTimeSource").value("DecisionResultVO.priceUpdateTimeMs"))
                .andExpect(jsonPath("$.sourceTrace.quoteFreshnessStatus").value("QUOTE_UPDATE_TIME_ONLY"))
                .andExpect(jsonPath("$.sourceTrace.dataQualityScore").value(91))
                .andExpect(jsonPath("$.sourceTrace.dataQualityScoreSource").value("DecisionResultVO.dataQualityScore"))
                .andExpect(jsonPath("$.sourceTrace.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.sourceTrace.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTrace.missingFields").isArray())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'runtimeKlineContext')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'timeframe')]").doesNotExist())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'latestPrice')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entryPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpPriceSources')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'rrSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'liquiditySource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.multiTimeframeSource").value("DecisionResultVO.multiTfConvergence"))
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'multiTimeframeSource')]").doesNotExist())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'eventSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'wickSource')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.fallbackStatus").value("SAFE_FAIL_CLOSED_ONLY"))
                .andExpect(jsonPath("$.derivativesRiskContext.timeframe").value("1h"))
                .andExpect(jsonPath("$.derivativesRiskContext.timeframeSource").value("DecisionResultVO.timeframe"))
                .andExpect(jsonPath("$.derivativesRiskContext.dataQualityScore").value(91))
                .andExpect(jsonPath("$.derivativesRiskContext.dataQualityScoreSource").value("DecisionResultVO.dataQualityScore"))
                .andExpect(jsonPath("$.derivativesRiskContext.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.derivativesRiskContext.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'openInterestHistory')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'fundingHistory')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'liquidationCluster')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'leverageDistribution')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'longShortRatio')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'liquidityStress')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'eventWindowBlockers')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'wickConfirmationSources')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'dataQualityScore')]").doesNotExist());
    }

    @Test
    void detail_json_exposesRuntimeKlineContextAsSeparateReadOnlyBoundaryWhenAssemblyIsSafe() throws Exception {
        MockMvc runtimeKlineMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                new DefaultDashboardSourceTraceDetailAdapter(
                        new DefaultDashboardRuntimeKlineContextAdapter(
                                (symbol, timeframe, requiredWindowSize, maxReadLagMs) -> freshReadiness(List.of(
                                        bar(60_000L, 119_999L, "101.10", "130.00", "100.50", "102.30"),
                                        bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10")
                                )),
                                new RuntimeKlineContextAssemblyServiceImpl()
                        )
                )
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setSymbol("BTCUSDT");
        decision.setAnalysisId("ana-runtime");
        decision.setTimeframe("1m");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-runtime")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-runtime")).thenReturn(Collections.emptyList());

        runtimeKlineMockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runtimeKlineContext.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.runtimeKlineContext.timeframe").value("1m"))
                .andExpect(jsonPath("$.runtimeKlineContext.fallbackStatus").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.latestPrice").value(102.3))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems.length()").value(2))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems[0].closePrice").value(102.3))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems[0].provider").value("LOCAL_FIXTURE"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvReadinessStatus").value("FRESH"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvStaleReasonCode").value("NONE"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvMissingFields").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.missingFields").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.entryPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.stopPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.tpPriceSources").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.rrSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.liquiditySource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.eventSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.wickSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.runtimeKlineContext.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTrace.fallbackStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'runtimeKlineContext')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'latestPrice')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entryPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceType')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceTimeframe')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceReason')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceType')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceTimeframe')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceReason')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpPriceSources')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceType')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceTimeframe')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceReason')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'rrSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'rrRuleRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'liquiditySource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'eventSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'wickSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.quoteLatestPrice").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.entryPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.stopPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.tpPriceSources").isEmpty())
                .andExpect(jsonPath("$.sourceTrace.rrSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.liquiditySource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.eventSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.wickSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.sourceTrace.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.planBoundaryDisplay.planBoundaryStatus").value("BACKEND_PENDING"))
                .andExpect(jsonPath("$.planBoundaryDisplay.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.planBoundaryDisplay.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.executionPlanDisplay.executionPlanStatus").value("BOUNDARY_PENDING"))
                .andExpect(jsonPath("$.executionPlanDisplay.executionPlanBoundaryAligned").value(false))
                .andExpect(jsonPath("$.executionPlanDisplay.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.executionPlanDisplay.notTradeInstruction").value(true));
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

    private String normalizedInternalPushPreviewDisplay() throws Exception {
        return internalPushPreviewDisplay()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String internalPushPreviewDisplay() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);
        int sectionStart = html.indexOf(INTERNAL_PUSH_PREVIEW_START);
        assertThat(sectionStart).isNotNegative();

        int sectionEnd = html.indexOf(SECTION_END, sectionStart);
        assertThat(sectionEnd).isNotNegative();

        return html.substring(sectionStart, sectionEnd + SECTION_END.length());
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

    private static PersistedOhlcvReadinessResult readiness(
            PersistedOhlcvReadinessStatus status,
            PersistedOhlcvStaleReasonCode reasonCode,
            String reasonText,
            List<String> missingFields
    ) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setStatus(status);
        result.setStaleReasonCode(reasonCode);
        result.setStaleReasonText(reasonText);
        result.setMissingFields(missingFields);
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }

    private static PersistedOhlcvReadinessResult freshReadiness(List<PersistedOhlcvBarDO> bars) {
        PersistedOhlcvReadinessResult result = readiness(
                PersistedOhlcvReadinessStatus.FRESH,
                PersistedOhlcvStaleReasonCode.NONE,
                "Persisted OHLCV window is fresh.",
                List.of()
        );
        result.setSymbol("BTCUSDT");
        result.setTimeframe("1m");
        result.setRequiredWindowSize(2);
        result.setBars(bars);
        result.setLatestCloseTimeMs(bars.stream()
                .filter(bar -> bar.getCloseTimeMs() != null)
                .map(PersistedOhlcvBarDO::getCloseTimeMs)
                .max(Long::compareTo)
                .orElse(null));
        result.setLatestIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        return result;
    }

    private static PersistedOhlcvBarDO bar(
            Long openTimeMs,
            Long closeTimeMs,
            String openPrice,
            String highPrice,
            String lowPrice,
            String closePrice
    ) {
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setSymbol("BTCUSDT");
        bar.setTimeframe("1m");
        bar.setOpenTimeMs(openTimeMs);
        bar.setCloseTimeMs(closeTimeMs);
        bar.setOpenPrice(new BigDecimal(openPrice));
        bar.setHighPrice(new BigDecimal(highPrice));
        bar.setLowPrice(new BigDecimal(lowPrice));
        bar.setClosePrice(new BigDecimal(closePrice));
        bar.setVolume(new BigDecimal("123.45"));
        bar.setClosed(true);
        bar.setProvider("LOCAL_FIXTURE");
        bar.setProviderMarketType("USDT_PERP");
        bar.setSourceEndpoint("persisted-ohlcv-fixture");
        bar.setSourceBatchId("batch-1");
        bar.setSourceTraceId("trace-1");
        bar.setSourceVersion(1);
        bar.setIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        bar.setQualityStatus("OK");
        bar.setIsDeleted(0);
        return bar;
    }
}
