package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiReviewConflictLevel;
import org.example.trademodel.ai.AiReviewStance;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.controller.DashboardHomeController;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.market.PersistedRealMarketEnvironmentAssessment;
import org.example.trademodel.market.PersistedRealMarketEnvironmentService;
import org.example.trademodel.localreal.LocalRealReadinessService;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.junit.jupiter.api.Tag;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.OpportunityPriorityRankingService;
import org.example.trademodel.testsupport.FrozenFinalExecutionPlanTestFixture;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.readiness.ProviderReadinessService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.example.trademodel.vo.UserPositionVO;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardHomeServiceImplTest {
    private static final Long USER_ID = 17L;
    private static final String ACTIVE_VALID_PERIOD =
            "2026-07-01T00:00:00Z ~ 2026-07-02T00:00:00Z";
    private static final List<String> FORBIDDEN_TELEGRAM_STATUS_DEPENDENCIES = List.of(
            "Telegram",
            "Notification",
            "PushRecheckService",
            "PushRecheckScheduler",
            "PushRecheckController",
            "PushRecheckDispatch",
            "UserConfigService",
            "UserConfigMapper",
            "Webhook"
    );

    @Mock
    private DecisionService decisionService;
    @Mock
    private MonitorService monitorService;
    @Mock
    private UserPositionService userPositionService;
    @Mock
    private PositionMonitorLogService positionMonitorLogService;
    @Mock
    private PositionSyncService positionSyncService;
    @Mock
    private OpportunityLogService opportunityLogService;
    @Mock
    private ExternalContextEvidenceBuilder externalContextEvidenceBuilder;
    @Mock
    private ProviderReadinessService providerReadinessService;
    @Mock
    private DerivativesSnapshotReadPort derivativesSnapshotReadPort;
    @Mock
    private AssetStateMapper assetStateMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;
    @Mock
    private PersistedOhlcvBarMapper persistedOhlcvBarMapper;
    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private ExecutionPlanMapper executionPlanMapper;

    private DashboardHomeServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new DashboardHomeServiceImpl(
                decisionService,
                monitorService,
                userPositionService,
                positionMonitorLogService,
                positionSyncService,
                opportunityLogService,
                externalContextEvidenceBuilder,
                providerReadinessService,
                new ObjectMapper()
        );
        service.setAssetStateMapper(assetStateMapper);
        service.setLocalRealDashboardSources(persistedOhlcvBarMapper, analysisRunMapper);
        service.setOriginalPlanSources(decisionResultMapper, executionPlanMapper, analysisRunMapper);
        service.setPlanValidityClock(Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC));
        lenient().when(analysisRunMapper.selectAverageScoreByAnalysisId(anyString())).thenReturn(null);
        lenient().when(analysisRunMapper.countEvidenceByAnalysisId(anyString())).thenReturn(null);
    }

    @Test
    void homeAssetsConsumeOpportunityRankingProjectionWithoutPoolOrderFallback() {
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        OpportunityPriorityRankingService rankingService = mock(OpportunityPriorityRankingService.class);
        service.setAssetPoolService(assetPoolService);
        service.setOpportunityPriorityRankingService(rankingService);

        DecisionResultVO link = decision("LINKUSDT", "BULLISH", "HIGH", "LOW", 91, 8,
                "LEVEL_1_CONSISTENT", true, "{\"state\":\"CANDIDATE\"}");
        link.setPlanMode("CONFIRM");
        DecisionResultVO aave = decision("AAVEUSDT", "WEAK_BULLISH", "MEDIUM", "MEDIUM", 84, 21,
                "LEVEL_2_MINOR_DISAGREEMENT", true, "{\"state\":\"WAITING_TRIGGER\"}");
        aave.setPlanMode("PREPARE");
        when(rankingService.rankForHome(USER_ID, 6)).thenReturn(List.of(
                projection(101L, link, 94, "opportunity-link", "CANDIDATE"),
                projection(102L, aave, 88, "opportunity-aave", "WAITING_TRIGGER")));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6, null);

        assertThat(home.getAssets()).extracting(DashboardHomeVO.AssetVO::getRawSymbol)
                .containsExactly("LINKUSDT", "AAVEUSDT");
        assertThat(home.getAssets().get(0).getAssetId()).isEqualTo(101L);
        assertThat(home.getAssets().get(0).getName()).isEqualTo("LINKUSDT");
        assertThat(home.getAssets().get(0).getOpportunityId()).isEqualTo("opportunity-link");
        assertThat(home.getAssets().get(0).getAnalysisId()).isEqualTo("analysis-LINKUSDT");
        assertThat(home.getAssets().get(0).getOpportunityScore()).isEqualTo(94);
        assertThat(home.getAssets().get(0).getRankingReason()).contains("OPPORTUNITY_SCORE=94");
        verify(assetPoolService, never()).listFocusSymbols(any(), anyInt());
        verify(decisionService, never()).getLatestDecisionResultBySymbolForUser(any(), anyString());
    }

    @Test
    void homeAggregatesStableReadOnlySemanticsWithoutCrossFallbacks() {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setStatus("OK");
        system.setMissedValidOpportunityCount(99);
        system.setConfusedCount(2);
        system.setHotResetFired(false);

        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setEntryZone("63000-64000");
        btc.setStopLoss("61000");
        btc.setTakeProfitRules("66000 / 69000");
        btc.setLeverageSuggestion("20x");
        btc.setPositionSuggestion("10%");
        btc.setValidPeriod(ACTIVE_VALID_PERIOD);
        setActivePlanValidity(btc);
        btc.setInvalidCondition("跌破 61000");
        btc.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "保持人工复核")),
                synthesis("BULLISH", "HIGH", "HIGH", "REDUCED", true, "EVENT_WINDOW_REVIEW")));

        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "EXTREME", 72, 80,
                "LEVEL_4_EXTREME_DIVERGENCE", false, "{\"nextState\":\"HIGH_RISK\"}");
        DecisionResultVO sol = decision("SOLUSDT", "RANGE", "LOW", "LOW", null, null,
                null, null, "CONFUSED");
        DecisionResultVO bnb = decision("BNBUSDT", "WEAK_BULLISH", "LOW", "MEDIUM", 100, 40,
                "LEVEL_3_DIVERGENCE", true, "{\"state\":\"UNKNOWN\"}");

        UserPositionVO position = new UserPositionVO();
        position.setId(9L);
        position.setAssetSymbol("BTCUSDT");
        position.setSide("LONG");
        position.setStatus("OPEN");
        position.setEntryPrice(new BigDecimal("62000"));
        position.setQuantity(new BigDecimal("0.2"));
        position.setLeverage(new BigDecimal("2"));
        position.setSourceType("MANUAL_INDEPENDENT");
        position.setUpdatedAt(LocalDateTime.of(2026, 6, 27, 2, 0));
        allowResolvedOriginalPlan(position, btc, "plan-btc-source", "trace-" + btc.getAnalysisId());

        UserPositionVO nonManualPosition = new UserPositionVO();
        nonManualPosition.setId(10L);
        nonManualPosition.setAssetSymbol("ETHUSDT");
        nonManualPosition.setSide("SHORT");
        nonManualPosition.setStatus("OPEN");
        nonManualPosition.setEntryPrice(new BigDecimal("3000"));
        nonManualPosition.setQuantity(new BigDecimal("1.5"));
        nonManualPosition.setLeverage(new BigDecimal("99"));
        nonManualPosition.setSourceType("SYSTEM");

        MonitorAlertDO alert = new MonitorAlertDO();
        alert.setAlertLevel("WARN");
        alert.setAlertMessage("测试告警");
        alert.setAssetSymbol("BTCUSDT");
        alert.setCreatedAt("2026-06-27 02:00:00");

        PositionSyncStatusVO sync = new PositionSyncStatusVO();
        sync.setFreshnessStatus("FRESH");
        sync.setActiveProviderType("BINANCE");

        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc, eth, sol, bnb));
        when(monitorService.getRecentAlerts(2)).thenReturn(List.of(alert));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position, nonManualPosition));
        when(positionSyncService.getPositionSyncStatus()).thenReturn(sync);
        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getSystemState().getPendingReview().getValue()).isNull();
        assertThat(home.getSystemState().getPendingReview().getValueLabel())
                .isEqualTo("当前不可查看");
        assertThat(home.getSystemState().getPendingReview().getStatus())
                .isEqualTo("PRIVATE_SOURCE_UNAVAILABLE");
        assertThat(home.getSystemState().getDataQuality().getValue()).isNull();
        assertThat(home.getSystemState().getDataQuality().getHelper()).isEqualTo("全局行情 Provider 尚未就绪");
        assertThat(home.getSystemState().getRiskLevel().getValue()).isNull();
        assertThat(home.getSystemState().getRiskLevel().getValueLabel()).isEqualTo("— / 尚未形成系统级评估");
        assertThat(home.getSystemState().getMarketTrend().getValue()).isNull();
        assertThat(home.getSystemState().getAccountStatus().getValueLabel())
                .isEqualTo("暂无评估·1笔·覆盖未知");
        assertThat(home.getSystemState().getAiConflict().getValue())
                .isEqualTo("LEVEL_2_MINOR_DISAGREEMENT");
        assertThat(home.getSystemState().getAiConflict().getValueLabel()).isEqualTo("轻微分歧");
        assertThat(home.getSystemState().getAiConflict().getScore()).isNull();

        assertThat(home.getAssets()).hasSize(4);
        DashboardHomeVO.AssetVO btcAsset = asset(home, "BTC/USDT");
        assertThat(btcAsset.getMarketBias()).isEqualTo("BULLISH");
        assertThat(btcAsset.getConfidenceLevel()).isEqualTo("HIGH");
        assertThat(btcAsset.getRiskLevel()).isEqualTo("HIGH");
        assertThat(btcAsset.getWorthOpening()).isNull();
        assertThat(btcAsset.getCompositeScore()).isNull();
        assertThat(btcAsset.getAssetState()).isEqualTo("CANDIDATE");
        assertThat(btcAsset.getAssetStateLabel()).isEqualTo("候选");

        DashboardHomeVO.AssetVO ethAsset = asset(home, "ETH/USDT");
        assertThat(ethAsset.getAssetState()).isEqualTo("HIGH_RISK");
        assertThat(ethAsset.getAssetStateLabel()).isEqualTo("高风险观察");
        assertThat(ethAsset.getCompositeScore()).isNull();

        DashboardHomeVO.AssetVO solAsset = asset(home, "SOL/USDT");
        assertThat(solAsset.getAssetState()).isNull();
        assertThat(solAsset.getAssetStateLabel()).isNull();

        DashboardHomeVO.AssetVO bnbAsset = asset(home, "BNB/USDT");
        assertThat(bnbAsset.getAssetState()).isNull();
        assertThat(bnbAsset.getAssetStateLabel()).isNull();

        assertThat(home.getPositions()).hasSize(1);
        DashboardHomeVO.PositionVO homePosition = home.getPositions().get(0);
        assertThat(homePosition.getPositionId()).isEqualTo(9L);
        assertThat(homePosition.getSymbol()).isEqualTo("BTC/USDT");
        assertThat(homePosition.getDirection()).isEqualTo("LONG");
        assertThat(homePosition.getEntryPrice()).isEqualByComparingTo("62000");
        assertThat(homePosition.getPositionSize()).isEqualByComparingTo("0.2");
        assertThat(homePosition.getPositionStatus()).isEqualTo("OPEN");
        assertThat(homePosition.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 27, 2, 0));
        assertThat(homePosition.getLeverage()).isEqualByComparingTo("2");
        assertThat(homePosition.getLeverage()).isNotEqualByComparingTo("20");
        assertThat(homePosition.getSourceType()).isEqualTo("MANUAL_INDEPENDENT");
        assertThat(homePosition.getFinalPlanId()).isNull();
        assertThat(homePosition.getCurrentPrice()).isNull();
        assertThat(homePosition.getFloatingPnl()).isNull();
        assertThat(homePosition.getMonitorConclusion()).isNull();
        assertThat(homePosition.getDataState()).isEqualTo("WAITING_MONITOR_DATA");
        assertThat(home.getExecutionSuggestion().getPositionMode()).isFalse();
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("PLAN_IDENTITY_MISSING");
        assertThat(home.getExecutionSuggestion().getPositionMonitor()).isNull();
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getAiDecision().getActiveTab()).isEqualTo("GPT_FINAL");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getSchemaVersion()).isEqualTo("v2");
        assertThat(home.getAiDecision().getTabs().get(0).getSupportEvidence())
                .containsExactly("规则方向与 AI 复核一致");
        assertThat(home.getPushInbox().getCounts().getWaiting()).isZero();
        assertThat(home.getPushInbox().getCounts().getPositionRisk()).isZero();
        assertThat(home.getPushInbox().getHasOpenPosition()).isTrue();
        assertThat(home.getPushInbox().getMode()).isEqualTo("OPPORTUNITY_AND_POSITION_RISK");
        assertThat(home.getPushInbox().getTelegramStatus()).isEqualTo("WAITING_SYNC");
        assertThat(home.getDiagnostics().getTelegram()).isEqualTo("WAITING_SYNC");
        assertThat(home.getSafety().getNotTradeInstruction()).isTrue();
        assertThat(home.getSafety().getNotAutoTrading()).isTrue();
        assertThat(home.getSafety().getNotOrderExecution()).isTrue();
    }

    @Test
    void homeUsesSharedCoinGlassSnapshotForBusinessSummaryWithoutRawDirectionSignal() {
        service.setDerivativesBusinessIntegration(derivativesSnapshotReadPort,
                new DerivativesBusinessIntegrationService(null));
        when(derivativesSnapshotReadPort.readCached(any(), any(), any(), any()))
                .thenReturn(new ProviderCallResult<>(dashboardDerivativesSnapshot(), null, null));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getDerivatives().getStatus()).isEqualTo("正常");
        assertThat(home.getDerivatives().getOpenInterestStructure()).isEqualTo("增加");
        assertThat(home.getDerivatives().getFundingRisk()).isEqualTo("正常");
        assertThat(home.getDerivatives().getLiquidationRisk()).isEqualTo("正常");
        assertThat(home.getDerivatives().getSource()).isEqualTo("CoinGlass v4");
        assertThat(home.getDerivatives().getDecisionImpact())
                .isIn("未发现衍生品阻断", "数据不足，需降级", "风险阻断");
        assertThat(home.getDerivatives().getDecisionImpact()).doesNotContain("做多", "做空");
    }

    @Test
    void pushInboxCountsUseOnlyPublicOpportunityProjection() {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setMissedValidOpportunityCount(99);

        UserPositionVO nonManualPosition = new UserPositionVO();
        nonManualPosition.setId(12L);
        nonManualPosition.setAssetSymbol("ETHUSDT");
        nonManualPosition.setStatus("OPEN");
        nonManualPosition.setSourceType("SYSTEM");

        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(nonManualPosition));
        when(opportunityLogService.queryPublic(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(6)))
                .thenReturn(List.of(
                        publicOpportunity("opp-pending", OpportunityLogStatus.PENDING_EVALUATION, null),
                        publicOpportunity("opp-valid", OpportunityLogStatus.RESOLVED,
                                OpportunityLogStatus.MISSED_VALID),
                        publicOpportunity("opp-invalid", OpportunityLogStatus.RESOLVED,
                                OpportunityLogStatus.MISSED_INVALID)));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPushInbox().getTelegramStatus()).isEqualTo("WAITING_SYNC");
        assertThat(home.getDiagnostics().getTelegram()).isEqualTo("WAITING_SYNC");
        assertThat(home.getPushInbox().getTelegramStatus()).isEqualTo(home.getDiagnostics().getTelegram());
        assertThat(home.getPushInbox().getHasOpenPosition()).isFalse();
        assertThat(home.getPushInbox().getMode()).isEqualTo("OPPORTUNITY_ONLY");
        assertThat(home.getPushInbox().getCounts().getWaiting()).isEqualTo(1);
        assertThat(home.getPushInbox().getCounts().getWaiting()).isNotEqualTo(99);
        assertThat(home.getPushInbox().getCounts().getExecutable()).isZero();
        assertThat(home.getPushInbox().getCounts().getInvalidated()).isEqualTo(1);
        assertThat(home.getPushInbox().getCounts().getPositionRisk()).isZero();
    }

    @Test
    void telegramReadonlyStatusStaysWaitingSyncAndDoesNotUseConfigOnlyNotifyChannels() {
        UserConfigDO configOnly = new UserConfigDO();
        configOnly.setUserId("dashboard-home");
        configOnly.setNotifyChannels("telegram");

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(configOnly.getNotifyChannels()).containsIgnoringCase("telegram");
        assertThat(home.getPushInbox().getTelegramStatus()).isEqualTo("WAITING_SYNC");
        assertThat(home.getDiagnostics().getTelegram()).isEqualTo("WAITING_SYNC");
        assertThat(home.getPushInbox().getTelegramStatus()).isEqualTo(home.getDiagnostics().getTelegram());
        assertThat(home.getPushInbox().getTelegramStatus()).isNotEqualTo("CONNECTED");
        assertThat(home.getDiagnostics().getTelegram()).isNotEqualTo("CONNECTED");
    }

    @Test
    void providerReadinessFeedsHeaderAndDiagnosticsWithoutFakeConnectedStatus() {
        ProviderReadinessVO readiness = providerReadiness(
                "CONFIGURED",
                "CONFIGURED",
                "WAITING_SYNC",
                "Binance public data / CONFIGURED"
        );
        when(providerReadinessService.getReadiness()).thenReturn(readiness);

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getHeader().getAiStatus()).isEqualTo("NOT_CALLED");
        assertThat(home.getHeader().getAiStatusLabel()).isEqualTo("未调用");
        assertThat(home.getHeader().getDataSourceText()).isEqualTo("Binance public data / CONFIGURED");
        assertThat(home.getDiagnostics().getMarketDataProvider()).isEqualTo("CONFIGURED");
        assertThat(home.getDiagnostics().getAiProvider()).isEqualTo("CONFIGURED");
        assertThat(home.getDiagnostics().getExternalContextProvider()).isEqualTo("WAITING_SYNC");
        assertThat(home.getDiagnostics().getProviderReadiness().getProviders()).allSatisfy(provider -> {
            assertThat(provider.getStatus()).isNotEqualTo("CONNECTED");
            assertThat(provider.getConnected()).isFalse();
        });
    }

    @Test
    void localRealHeaderAndDiagnosticsUseTheSameProviderReadinessSnapshot() {
        ProviderReadinessVO readiness = providerReadiness(
                "CONNECTED",
                "WAITING_SYNC",
                "WAITING_SYNC",
                "Kraken public data / CONNECTED"
        );
        ProviderReadinessVO.ProviderStatusVO market = readiness.getProviders().get(0);
        market.setName("KRAKEN_PUBLIC_MARKET_DATA");
        market.setConnected(true);
        market.setReason("LOCAL_REAL_PROVIDER_VERIFIED_FRESH");
        when(providerReadinessService.getReadiness()).thenReturn(readiness);
        LocalRealReadinessService localRealReadiness = mock(LocalRealReadinessService.class);
        when(localRealReadiness.updatedAt()).thenReturn(Instant.parse("2026-08-20T09:56:00Z"));
        service.setLocalRealReadinessService(localRealReadiness);

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getHeader().getDataSourceText()).isEqualTo("Kraken public data / CONNECTED");
        assertThat(home.getHeader().getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 20, 9, 56));
        assertThat(home.getDiagnostics().getMarketDataProvider()).isEqualTo("CONNECTED");
        assertThat(home.getDiagnostics().getProviderReadiness()).isSameAs(readiness);
    }

    @Test
    void dashboardHomeAggregationHasNoTelegramSendDispatchOrRecheckExecutionDependency() {
        assertThat(Arrays.stream(DashboardHomeServiceImpl.class.getDeclaredFields())
                .map(field -> field.getType().getSimpleName()))
                .doesNotContain(FORBIDDEN_TELEGRAM_STATUS_DEPENDENCIES.toArray(String[]::new));
        assertThat(Arrays.stream(DashboardHomeServiceImpl.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .map(Class::getSimpleName))
                .doesNotContain(FORBIDDEN_TELEGRAM_STATUS_DEPENDENCIES.toArray(String[]::new));
        assertThat(Arrays.stream(DashboardHomeServiceImpl.class.getDeclaredMethods())
                .map(method -> method.getName().toLowerCase()))
                .allSatisfy(methodName -> {
                    assertThat(methodName).doesNotContain("telegram");
                    assertThat(methodName).doesNotContain("notify");
                    assertThat(methodName).doesNotContain("notification");
                    assertThat(methodName).doesNotContain("webhook");
                    assertThat(methodName).doesNotContain("dispatch");
                    assertThat(methodName).doesNotContain("replay");
                    assertThat(methodName).doesNotContain("send");
                });
    }

    @Test
    void pushInboxModeUsesRealOpenManualPositionOnly() {
        UserPositionVO manualPosition = new UserPositionVO();
        manualPosition.setId(13L);
        manualPosition.setAssetSymbol("BTCUSDT");
        manualPosition.setStatus("OPEN");
        manualPosition.setSourceType("MANUAL_INDEPENDENT");

        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(manualPosition));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 13L, 1)).thenReturn(List.of());

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPushInbox().getHasOpenPosition()).isTrue();
        assertThat(home.getPushInbox().getMode()).isEqualTo("OPPORTUNITY_AND_POSITION_RISK");
        assertThat(home.getPushInbox().getCounts().getPositionRisk()).isZero();
    }

    @Test
    void pushInboxItemsUsePublicSchemaWithoutPrivatePushOrRecheckFields() throws Exception {
        LocalDateTime publicTime = LocalDateTime.of(2026, 6, 27, 9, 30);
        OpportunityLogPublicDTO opportunity = publicOpportunity(
                "opp-dashboard",
                "resolved",
                "missed_valid",
                publicTime);
        when(opportunityLogService.queryPublic(
                any(), any(), any(), any(), any(), any(), any(), any(), eq(6)))
                .thenReturn(List.of(opportunity));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPushInbox().getItems()).hasSize(1);
        DashboardHomeVO.PushItemVO item = home.getPushInbox().getItems().get(0);
        assertThat(item.getMessageId()).isEqualTo("opp-dashboard");
        assertThat(item.getSourceIdentity()).isEqualTo("OPPORTUNITY");
        assertThat(item.getSymbol()).isEqualTo("BTC/USDT");
        assertThat(item.getPublicLifecycle()).isEqualTo(OpportunityLogStatus.RESOLVED);
        assertThat(item.getPublicStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(item.getPublicTimestamp()).isEqualTo(publicTime);
        assertThat(item.getPublicDescription()).isEqualTo("BTCUSDT LONG 1H");
        assertThat(Arrays.stream(DashboardHomeVO.PushItemVO.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain(
                        "pushId",
                        "recheckStatus",
                        "failReasonJson",
                        "currentAccountRiskAllowed",
                        "title",
                        "summary");
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(item);
        assertThat(json)
                .contains("\"messageId\":\"opp-dashboard\"")
                .doesNotContain(
                        "\"pushId\"",
                        "\"recheckStatus\"",
                        "\"failReasonJson\"",
                        "\"currentAccountRiskAllowed\"");
    }

    @Test
    void homePositionUsesLatestPersistedMonitorLogAndCalculatesLongPnl() {
        UserPositionVO position = new UserPositionVO();
        position.setId(9L);
        position.setAssetSymbol("BTCUSDT");
        position.setSide("LONG");
        position.setStatus("OPEN");
        position.setEntryPrice(new BigDecimal("62000"));
        position.setQuantity(new BigDecimal("0.2"));
        position.setLeverage(new BigDecimal("2"));
        position.setSourceType("MANUAL_INDEPENDENT");

        PositionMonitorLogDTO monitorLog = new PositionMonitorLogDTO();
        monitorLog.setPositionId(9L);
        monitorLog.setCurrentPrice(new BigDecimal("63500"));
        completeTrustedMonitor(monitorLog, "LOGIC_VALID", "LOW", "CONTINUE_HOLD");

        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 9L, 1)).thenReturn(List.of(monitorLog));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPositions()).hasSize(1);
        DashboardHomeVO.PositionVO homePosition = home.getPositions().get(0);
        assertThat(homePosition.getCurrentPrice()).isEqualByComparingTo("63500");
        assertThat(homePosition.getMarkPrice()).isEqualByComparingTo("63500");
        assertThat(homePosition.getMonitorConclusion()).isEqualTo("LOGIC_VALID");
        assertThat(homePosition.getMonitorConclusionLabel()).isEqualTo("逻辑仍成立");
        assertThat(homePosition.getFloatingPnl()).isEqualByComparingTo("300.0");
        assertThat(homePosition.getPnlPct()).isEqualByComparingTo("2.41935500");
        assertThat(homePosition.getPnlAmount()).isEqualByComparingTo("300.0");
        assertThat(homePosition.getPnlPercent()).isEqualByComparingTo("2.41935500");
        assertThat(homePosition.getAccountImpactPct()).isNull();
        assertThat(home.getSystemState().getAccountStatus().getValueLabel())
                .isEqualTo("低·1笔·覆盖完整");
        assertThat(home.getDiagnostics().getAccountRiskCoverageState()).isEqualTo("COMPLETE");
    }

    @Test
    void pendingMonitorSourceFailsClosedWithoutLeakingMonitorResults() {
        UserPositionVO position = activeManualPosition(901L, "BTCUSDT", null);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(new BigDecimal("2"));
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setCurrentPrice(new BigDecimal("110"));
        completeTrustedMonitor(monitor, "LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        monitor.setMonitorSourceStatus("PENDING_VERIFICATION");
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1))
                .thenReturn(List.of(monitor));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);
        DashboardHomeVO.PositionVO row = home.getPositions().get(0);

        assertWaitingMonitorData(row);
        assertThat(home.getSystemState().getAccountStatus().getValueLabel())
                .isEqualTo("暂无评估·1笔·覆盖未知");
        assertThat(home.getDiagnostics().getAccountRiskCoverageState()).isEqualTo("UNKNOWN");
    }

    @Test
    void invalidMonitorSourceFailsClosedWithoutLeakingMonitorResults() {
        UserPositionVO position = activeManualPosition(905L, "BTCUSDT", null);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setCurrentPrice(new BigDecimal("110"));
        completeTrustedMonitor(monitor, "LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        monitor.setMonitorSourceStatus("INVALID");
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1))
                .thenReturn(List.of(monitor));

        DashboardHomeVO.PositionVO row = service.getHomeForUser(USER_ID, null, 6).getPositions().get(0);

        assertWaitingMonitorData(row);
    }

    @Test
    void missingMarkPriceSourceFailsClosedWithoutUsingTheNumericPrice() {
        UserPositionVO position = activeManualPosition(906L, "BTCUSDT", null);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setCurrentPrice(new BigDecimal("110"));
        completeTrustedMonitor(monitor, "LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        monitor.setMarkPriceSource(null);
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1))
                .thenReturn(List.of(monitor));

        DashboardHomeVO.PositionVO row = service.getHomeForUser(USER_ID, null, 6).getPositions().get(0);

        assertWaitingMonitorData(row);
    }

    @Test
    void expiredMonitorSourceFailsClosedWithoutUsingStaleMarkPrice() {
        UserPositionVO position = activeManualPosition(902L, "BTCUSDT", null);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setCurrentPrice(new BigDecimal("111"));
        completeTrustedMonitor(monitor, "HIGH_RISK_OBSERVATION", "HIGH", "REDUCE_POSITION");
        monitor.setObservedAt(LocalDateTime.of(2026, 7, 1, 11, 0));
        monitor.setFreshUntil(LocalDateTime.of(2026, 7, 1, 11, 59));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1))
                .thenReturn(List.of(monitor));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPositionMonitoringState()).isEqualTo("WAITING_MONITOR_DATA");
        assertWaitingMonitorData(home.getPositions().get(0));
    }

    @Test
    void illegalMonitorConclusionActionPairFailsClosedOnHome() {
        UserPositionVO position = activeManualPosition(904L, "BTCUSDT", null);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setCurrentPrice(new BigDecimal("105"));
        completeTrustedMonitor(monitor, "LOGIC_VALID", "LOW", "REDUCE_POSITION");
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1))
                .thenReturn(List.of(monitor));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPositionMonitoringState()).isEqualTo("WAITING_MONITOR_DATA");
        assertWaitingMonitorData(home.getPositions().get(0));
    }

    @Test
    void dashboardHomeJsonKeepsFrozenPositionFieldsIndependent() throws Exception {
        UserPositionVO position = activeManualPosition(903L, "BTCUSDT", null);
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(new BigDecimal("2"));
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setCurrentPrice(new BigDecimal("105"));
        completeTrustedMonitor(monitor, "LOGIC_WEAKENED", "MEDIUM", "NO_ADD_POSITION");
        monitor.setEntryLogicStatus("WEAKENED");
        monitor.setRiskChangeReason("OPPOSING_EVIDENCE_INCREASED");
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1))
                .thenReturn(List.of(monitor));
        AuthenticatedUserIdResolver resolver = mock(AuthenticatedUserIdResolver.class);
        when(resolver.requireCurrentUserId()).thenReturn(USER_ID);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DashboardHomeController(service, resolver)).build();

        mockMvc.perform(get("/api/dashboard/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positions[0].symbol").value("BTC/USDT"))
                .andExpect(jsonPath("$.data.positions[0].direction").value("LONG"))
                .andExpect(jsonPath("$.data.positions[0].entryPrice").value(100))
                .andExpect(jsonPath("$.data.positions[0].markPrice").value(105))
                .andExpect(jsonPath("$.data.positions[0].markPriceSource").value("TEST"))
                .andExpect(jsonPath("$.data.positions[0].markPriceFresh").value(true))
                .andExpect(jsonPath("$.data.positions[0].pnlAmount").value(10))
                .andExpect(jsonPath("$.data.positions[0].pnlPercent").value(5.0))
                .andExpect(jsonPath("$.data.positions[0].riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.positions[0].riskTrend").value("STABLE"))
                .andExpect(jsonPath("$.data.positions[0].monitorConclusion").value("LOGIC_WEAKENED"))
                .andExpect(jsonPath("$.data.positions[0].entryLogicStatus").value("WEAKENED"))
                .andExpect(jsonPath("$.data.positions[0].reversalStatus").value("NO_REVERSAL"))
                .andExpect(jsonPath("$.data.positions[0].riskReason")
                        .value("OPPOSING_EVIDENCE_INCREASED"))
                .andExpect(jsonPath("$.data.positions[0].suggestedAction").value("NO_ADD_POSITION"))
                .andExpect(jsonPath("$.data.positions[0].lastMonitorTime").exists())
                .andExpect(jsonPath("$.data.positions[0].dataState").value("OPEN_MONITORING"));
    }

    @Test
    void positionMonitorUsesRealMonitorFields() {
        UserPositionVO position = new UserPositionVO();
        position.setId(19L);
        position.setAssetSymbol("BTCUSDT");
        position.setSide("LONG");
        position.setStatus("OPEN");
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(new BigDecimal("1"));
        position.setStopLoss(new BigDecimal("95"));
        position.setTakeProfit(new BigDecimal("115"));
        position.setOpenedAt(LocalDateTime.of(2026, 7, 13, 10, 0));
        position.setSourceType("MANUAL_INDEPENDENT");

        PositionMonitorLogDTO monitorLog = new PositionMonitorLogDTO();
        monitorLog.setPositionId(19L);
        monitorLog.setCurrentPrice(new BigDecimal("105"));
        completeTrustedMonitor(monitorLog, "NEAR_STOP_LOSS", "LOW", "TIGHTEN_STOP");
        monitorLog.setCreatedAt(LocalDateTime.of(2026, 7, 13, 10, 5));

        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 19L, 1)).thenReturn(List.of(monitorLog));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPositions()).hasSize(1);
        DashboardHomeVO.PositionVO row = home.getPositions().get(0);
        assertThat(row.getEntryLogicStatus()).isEqualTo("STILL_VALID");
        assertThat(row.getDirectionSupportStatus()).isEqualTo("SUPPORTED");
        assertThat(row.getReversalStatus()).isEqualTo("NO_REVERSAL");
        assertThat(row.getRiskLevel()).isEqualTo("LOW");
        assertThat(row.getSuggestedManualAction()).isEqualTo("TIGHTEN_STOP");
        assertThat(row.getSuggestedManualActionText()).isEqualTo("收紧止损");
        assertThat(row.getEntryLogicStatusLabel()).isEqualTo("仍成立");
        assertThat(row.getDirectionSupportStatusLabel()).isEqualTo("当前方向仍获支持");
        assertThat(row.getReversalStatusLabel()).isEqualTo("无明显反转");
        assertThat(row.getUserStopLoss()).isEqualByComparingTo("95");
        assertThat(row.getUserTakeProfit()).isEqualByComparingTo("115");
        assertThat(row.getSystemSuggestedStopLoss()).isNull();
        assertThat(row.getSystemSuggestedTakeProfit()).isNull();
        assertThat(row.getOpenedAt()).isEqualTo(LocalDateTime.of(2026, 7, 13, 10, 0));
        assertThat(row.getLastMonitorAt()).isEqualTo(LocalDateTime.of(2026, 7, 13, 10, 5));
        assertThat(row.getNextMonitorAt()).isNull();
    }

    @Test
    @Tag("core-regression")
    void homeRiskEscalationDependsOnTrendNotAbsoluteRiskLevel() {
        UserPositionVO position = activeManualPosition(907L, "BTCUSDT", null);
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setCurrentPrice(new BigDecimal("105"));
        completeTrustedMonitor(monitor, "HIGH_RISK_OBSERVATION", "HIGH", "REDUCE_POSITION");
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1))
                .thenReturn(List.of(monitor));

        DashboardHomeVO.PositionVO stable = service.getHomeForUser(USER_ID, null, 6).getPositions().get(0);

        assertThat(stable.getRiskLevel()).isEqualTo("HIGH");
        assertThat(stable.getRiskTrend()).isEqualTo("STABLE");
        assertThat(stable.getDataState()).isEqualTo("OPEN_MONITORING");

        monitor.setRiskTrend("INCREASED");
        DashboardHomeVO.PositionVO increased = service.getHomeForUser(USER_ID, null, 6).getPositions().get(0);

        assertThat(increased.getDataState()).isEqualTo("RISK_ESCALATED");
    }

    @Test
    void closedPositionNotDisplayedAsActiveMonitoring() {
        UserPositionVO closedPosition = new UserPositionVO();
        closedPosition.setId(21L);
        closedPosition.setAssetSymbol("BTCUSDT");
        closedPosition.setStatus("CLOSED");
        closedPosition.setSourceType("MANUAL_INDEPENDENT");

        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(closedPosition));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPositions()).isEmpty();
        assertThat(home.getPositionMonitoringState()).isEqualTo("NO_POSITION");
        assertThat(home.getPushInbox().getHasOpenPosition()).isFalse();
        assertThat(home.getPushInbox().getMode()).isEqualTo("OPPORTUNITY_ONLY");
    }

    @Test
    void executionSuggestionDoesNotBecomePosition() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        decision.setEntryZone("63000-64000");
        decision.setStopLoss("61000");
        decision.setTakeProfitRules("66000 / 69000");
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of());

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getPositions()).isEmpty();
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
        assertThat(home.getExecutionSuggestion().getPositionMode()).isFalse();
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo("63000-64000");
        assertThat(home.getExecutionSuggestion().getStopLoss()).isEqualTo("61000");
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isEqualTo("66000 / 69000");
    }

    @Test
    void homePositionCalculatesShortPnlWithoutExecutionPlanFallback() {
        UserPositionVO position = new UserPositionVO();
        position.setId(10L);
        position.setAssetSymbol("ETHUSDT");
        position.setSide("SHORT");
        position.setStatus("OPEN");
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(new BigDecimal("2"));
        position.setLeverage(new BigDecimal("3"));
        position.setSourceType("MANUAL_INDEPENDENT");

        PositionMonitorLogDTO monitorLog = new PositionMonitorLogDTO();
        monitorLog.setPositionId(10L);
        monitorLog.setCurrentPrice(new BigDecimal("90"));
        completeTrustedMonitor(monitorLog, "LOGIC_VALID", "LOW", "CONTINUE_HOLD");

        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 10L, 1)).thenReturn(List.of(monitorLog));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        DashboardHomeVO.PositionVO homePosition = home.getPositions().get(0);
        assertThat(homePosition.getFloatingPnl()).isEqualByComparingTo("20");
        assertThat(homePosition.getPnlPct()).isEqualByComparingTo("10.00000000");
        assertThat(homePosition.getPnlAmount()).isEqualByComparingTo("20");
        assertThat(homePosition.getPnlPercent()).isEqualByComparingTo("10.00000000");
        assertThat(homePosition.getAccountImpactPct()).isNull();
        assertThat(homePosition.getSuggestedManualActionText()).isEqualTo("继续持有");
    }

    @Test
    void openPositionAndAssetExecutionPlanRemainIndependent() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);
        UserPositionVO position = activeManualPosition(301L, "BTCUSDT", null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6, null);

        assertThat(home.getPositions()).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactly(301L);
        assertThat(home.getPositionSelectionStatus()).isEqualTo("POSITION_SELECTION_REQUIRED");
        assertThat(home.getSelectedPositionId()).isNull();
        assertThat(home.getMatchingPositionCount()).isEqualTo(1);
        assertAssetExecutionPlan(home.getExecutionSuggestion(), decision,
                "plan-" + decision.getAnalysisId());
    }

    @Test
    void activePersistedPlanWithCompleteBoundariesRemainsUsable() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertAssetExecutionPlan(suggestion, decision, "plan-" + decision.getAnalysisId());
    }

    @Test
    void validatedFinalPlanExposesEveryFrozenHomeFieldWithoutCandidateSubstitution() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setFinalMarketBias("BULLISH");
        plan.setFinalPlanMode("CONFIRMATION");
        plan.setOpportunityType("BREAKOUT_CONFIRMATION");
        plan.setRecommendedAction("WAIT_MANUAL_CONFIRMATION");
        plan.setEntryLogic("verified entry logic");
        plan.setTriggerCondition("verified trigger");
        plan.setStopLogic("verified stop logic");
        plan.setTargetLogic("verified target logic");
        plan.setAddPositionCondition("verified add condition");
        plan.setReducePositionCondition("verified reduce condition");
        plan.setAbandonCondition("verified abandon condition");
        plan.setLeverageLimit("3x");
        plan.setPositionLimit("8%");
        plan.setExpectedRiskReward(new BigDecimal("2.5"));
        plan.setAnalysisTimeframesJson("[\"4h\",\"1h\",\"15m\",\"5m\"]");
        plan.setTriggerTimeframe("15m");
        plan.setHoldingHorizon("1-3 days");
        plan.setValidationReasons("ALL_RULES_PASS");
        plan.setDowngradeReason("NO_DOWNGRADE");
        plan.setRuleVetoReason(null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(suggestion.getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
        assertThat(suggestion.getFinalPlan()).isTrue();
        assertThat(suggestion.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(suggestion.getFinalPlanMode()).isEqualTo("CONFIRMATION");
        assertThat(suggestion.getDirection()).isEqualTo("BULLISH");
        assertThat(suggestion.getWorthOpening()).isTrue();
        assertThat(suggestion.getOpportunityType()).isEqualTo("BREAKOUT_CONFIRMATION");
        assertThat(suggestion.getRecommendedAction()).isEqualTo("WAIT_MANUAL_CONFIRMATION");
        assertThat(suggestion.getEntryLogic()).isEqualTo("verified entry logic");
        assertThat(suggestion.getEntryZone()).isEqualTo(decision.getEntryZone());
        assertThat(suggestion.getTriggerCondition()).isEqualTo("verified trigger");
        assertThat(suggestion.getStopLogic()).isEqualTo("verified stop logic");
        assertThat(suggestion.getStopZone()).isEqualTo(decision.getStopLoss());
        assertThat(suggestion.getTargetLogic()).isEqualTo("verified target logic");
        assertThat(suggestion.getTargetZones()).isEqualTo(decision.getTakeProfitRules());
        assertThat(suggestion.getAddCondition()).isEqualTo("verified add condition");
        assertThat(suggestion.getReduceCondition()).isEqualTo("verified reduce condition");
        assertThat(suggestion.getAbandonCondition()).isEqualTo("verified abandon condition");
        assertThat(suggestion.getInvalidCondition()).isEqualTo(decision.getInvalidCondition());
        assertThat(suggestion.getLeverageSuggestion()).isEqualTo("3x");
        assertThat(suggestion.getPositionSuggestion()).isEqualTo("8%");
        assertThat(suggestion.getExpectedRiskReward()).isEqualByComparingTo("2.5");
        assertThat(suggestion.getAnalysisTimeframes()).isEqualTo("[\"4h\",\"1h\",\"15m\",\"5m\"]");
        assertThat(suggestion.getTriggerTimeframe()).isEqualTo("15m");
        assertThat(suggestion.getHoldingHorizon()).isEqualTo("1-3 days");
        assertThat(suggestion.getValidationStatus()).isEqualTo("PASS");
        assertThat(suggestion.getValidationReasons()).isEqualTo("ALL_RULES_PASS");
        assertThat(suggestion.getDowngradeReason()).isEqualTo("NO_DOWNGRADE");
        assertThat(suggestion.getSourceStatus()).isEqualTo("VALID");
        assertThat(suggestion.getChainStatus()).isEqualTo("FINAL_VALIDATED");
        assertThat(suggestion.getCandidateId()).isEqualTo(plan.getCandidateId());
        assertThat(suggestion.getResolverResultId()).isEqualTo(plan.getResolverResultId());
        assertThat(suggestion.getValidationResultId()).isEqualTo(plan.getValidationResultId());
        assertThat(suggestion.getNotTradeInstruction()).isTrue();
    }

    @Test
    void systemPlanPositionKeepsExplicitFinalPlanSourceInHomeProjection() {
        UserPositionVO position = activeManualPosition(302L, "BTCUSDT", null);
        position.setSourceType("SYSTEM_PLAN_POSITION");
        position.setFinalPlanId("final-plan-302");
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 6);

        assertThat(home.getPositions()).hasSize(1);
        assertThat(home.getPositions().get(0).getSourceType()).isEqualTo("SYSTEM_PLAN_POSITION");
        assertThat(home.getPositions().get(0).getFinalPlanId()).isEqualTo("final-plan-302");
        assertThat(home.getPositions().get(0).getDataState()).isEqualTo("WAITING_MONITOR_DATA");
    }

    @Test
    void finalPlanModeExclusivelyDeterminesWhetherThePlanIsWorthManualParticipation() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        decision.setIsWorthOpening(false);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO reduced = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(reduced.getWorthOpening()).isTrue();

        plan.setFinalPlanMode("OBSERVATION");
        DashboardHomeVO.ExecutionSuggestionVO observation = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(observation.getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
        assertThat(observation.getWorthOpening()).isFalse();
    }

    @Test
    void candidatePlanNeverLeaksIntoHomeExecutionSuggestion() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO candidateLookingPlan = allowMatchingSnapshot(decision);
        candidateLookingPlan.setFinalPlan(false);
        candidateLookingPlan.setChainStatus("CANDIDATE_GENERATED");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "PLAN_INCOMPLETE");
        assertThat(suggestion.getSourceExecutionPlanId()).isNull();
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getStopLoss()).isNull();
        assertThat(suggestion.getTakeProfitRules()).isNull();
    }

    @Test
    void exactExecutionPlanRelationWinsWhenLatestPlanDiffers() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO exact = allowMatchingSnapshot(decision);
        exact.setEntryZone("EXACT-entry");

        ExecutionPlanDO latest = validExecutionPlan("plan-latest-not-selected", decision.getAnalysisId());
        copyExactPlanFields(latest, decision);
        latest.setEntryZone("LATEST-entry");
        lenient().when(executionPlanMapper.selectLatestByAnalysisId(decision.getAnalysisId()))
                .thenReturn(latest);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo(exact.getPlanId());
        assertThat(suggestion.getEntryZone()).isEqualTo("EXACT-entry");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
    }

    @Test
    void blockedPersistedPlanWithCompleteBoundariesFailsClosedAndKeepsPosition() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setExecutionPlanStatus("BLOCKED");
        UserPositionVO position = activeManualPosition(302L, "BTCUSDT", null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6, 302L);

        assertThat(home.getPositions()).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactly(302L);
        assertUnavailableAssetExecutionPlan(home.getExecutionSuggestion(), "PLAN_BLOCKED");
        assertThat(home.getExecutionSuggestion().getStatus()).isNotEqualTo("POSITION_MONITORING");
    }

    @Test
    void invalidPersistedPlanWithCompleteBoundariesFailsClosed() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setSourceGateStatus("INVALID");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "PLAN_INVALID");
    }

    @Test
    void incompletePersistedPlanWithCompleteLookingFieldsFailsClosed() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setExecutionPlanStatus("INCOMPLETE");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "PLAN_INCOMPLETE");
    }

    @Test
    void reviewOnlyPersistedPlanDoesNotBecomeCurrentAssetPlan() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setExecutionPlanStatus("REVIEW_ONLY");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "PLAN_REVIEW_ONLY");
    }

    @Test
    void persistedPlanNeedingRevalidationFailsClosedDespiteCompleteBoundaries() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setNeedsRevalidation(true);
        plan.setRevalidationReason("EXTREME_PRICE_MOVE");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "REVALIDATION_REQUIRED");
        assertThat(suggestion.getBlockedReason()).isEqualTo("极端价格波动触发重新验证");
    }

    @Test
    void persistedPlanWithIncompleteSourceGateFailsClosed() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setSourceGateComplete(false);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "PLAN_INCOMPLETE");
    }

    @Test
    void activeStatusWithMissingPersistedBoundaryFailsClosed() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setStopLoss(null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "BOUNDARY_INCOMPLETE");
    }

    @Test
    void unknownAndNullPersistedPlanStatesFailClosed() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        plan.setExecutionPlanStatus("UNRECOGNIZED");
        assertUnavailableAssetExecutionPlan(service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion(), "PLAN_INCOMPLETE");

        plan.setExecutionPlanStatus(null);
        assertUnavailableAssetExecutionPlan(service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion(), "PLAN_INCOMPLETE");
    }

    @Test
    void mismatchedPersistedPlanIdentityFailsClosed() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setAnalysisId("analysis-other-asset");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertUnavailableAssetExecutionPlan(suggestion, "PLAN_IDENTITY_ERROR");
    }

    @Test
    void multiplePositionsForSameAssetDoNotBlockAssetExecutionPlan() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        decision.setEntryZone("BTC-ASSET-entry");
        decision.setStopLoss("BTC-ASSET-stop");
        decision.setTakeProfitRules("BTC-ASSET-tp");
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);
        UserPositionVO first = activeManualPosition(311L, "BTCUSDT", null);
        UserPositionVO second = activeManualPosition(312L, "BTCUSDT", null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(first, second));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6, null);

        assertThat(home.getPositions()).hasSize(2);
        assertThat(home.getPositionSelectionStatus()).isEqualTo("POSITION_SELECTION_REQUIRED");
        assertThat(home.getMatchingPositionCount()).isEqualTo(2);
        assertAssetExecutionPlan(home.getExecutionSuggestion(), decision,
                "plan-" + decision.getAnalysisId());
    }

    @Test
    void assetSwitchKeepsExplicitPositionIdentityAndChangesOnlyAssetContext() {
        DecisionResultVO btc = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        btc.setEntryZone("BTC-ASSET-entry");
        btc.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "BTC summary")),
                synthesis("BULLISH", "HIGH", "MEDIUM", "PREPARE_ONLY", true, null)));
        setActivePlanValidity(btc);
        allowMatchingSnapshot(btc);

        DecisionResultVO eth = completePlanDecision("ETHUSDT", ACTIVE_VALID_PERIOD);
        eth.setMarketBiasHierarchy("BEARISH");
        eth.setEntryZone("ETH-ASSET-entry");
        eth.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "ETH summary")),
                synthesis("BEARISH", "MEDIUM", "MEDIUM", "PREPARE_ONLY", true, null)));
        setActivePlanValidity(eth);
        allowMatchingSnapshot(eth);

        UserPositionVO position = activeManualPosition(321L, "BTCUSDT", null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(btc, eth));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));

        DashboardHomeVO btcHome = service.getHomeForUser(USER_ID, "BTCUSDT", 6, 321L);
        DashboardHomeVO ethHome = service.getHomeForUser(USER_ID, "ETHUSDT", 6, 321L);

        assertThat(btcHome.getSelectedPositionId()).isEqualTo(321L);
        assertThat(ethHome.getSelectedPositionId()).isEqualTo(321L);
        assertThat(btcHome.getPositions()).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactlyElementsOf(ethHome.getPositions().stream()
                        .map(DashboardHomeVO.PositionVO::getPositionId).toList());
        assertThat(btcHome.getExecutionSuggestion().getEntryZone()).isEqualTo("BTC-ASSET-entry");
        assertThat(ethHome.getExecutionSuggestion().getEntryZone()).isEqualTo("ETH-ASSET-entry");
        assertThat(aiTab(btcHome, "GPT_FINAL").getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(aiTab(ethHome, "GPT_FINAL").getFinalMarketBias()).isEqualTo("BEARISH");
    }

    @Test
    void topThreeOwnerPositionsAreStableAcrossAssetSwitchAndKeepExactSelectionOutsideTopThree() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 20,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "MEDIUM", 82, 25,
                "LEVEL_2_REVIEW", false, "{\"state\":\"OBSERVING\"}");
        List<UserPositionVO> positions = List.of(
                activeManualPosition(401L, "BTCUSDT", null),
                activeManualPosition(402L, "ETHUSDT", null),
                activeManualPosition(403L, "BTCUSDT", null),
                activeManualPosition(404L, "SOLUSDT", null),
                activeManualPosition(405L, "ETHUSDT", null));
        positions.forEach(position -> {
            position.setStopLoss(new BigDecimal("90"));
            position.setTakeProfit(new BigDecimal("120"));
            position.setUpdatedAt(LocalDateTime.of(2026, 7, 20, 10, 0));
        });
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 401L, 1))
                .thenReturn(List.of(positionMonitor(401L, "HIGH_RISK", "HIGH", 5)));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 402L, 1))
                .thenReturn(List.of(positionMonitor(402L, "PLAN_INVALIDATED", "MEDIUM", 4)));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 403L, 1))
                .thenReturn(List.of(positionMonitor(403L, "LOGIC_WEAKENED", "LOW", 3)));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 404L, 1))
                .thenReturn(List.of(positionMonitor(404L, "LOGIC_VALID", "LOW", 2)));
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, 405L, 1))
                .thenReturn(List.of());
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(btc, eth));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(positions);

        DashboardHomeVO btcHome = service.getHomeForUser(USER_ID, "BTCUSDT", 6, 404L);
        DashboardHomeVO ethHome = service.getHomeForUser(USER_ID, "ETHUSDT", 6, 404L);

        assertThat(btcHome.getPositions()).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactly(401L, 402L, 403L);
        assertThat(ethHome.getPositions()).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactly(401L, 402L, 403L);
        assertThat(btcHome.getSelectedPositionId()).isEqualTo(404L);
        assertThat(ethHome.getSelectedPositionId()).isEqualTo(404L);
        assertThat(btcHome.getPositionSelectionStatus()).isEqualTo("EXACT_POSITION_SELECTED");
        assertThat(ethHome.getPositionSelectionStatus()).isEqualTo("EXACT_POSITION_SELECTED");
        verify(userPositionService, times(2)).listOpenPositionsForUser(USER_ID);
    }

    @Test
    void missingAssetPlanIdentityFailsClosedWithoutPositionFallback() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        String traceId = "trace-" + decision.getAnalysisId();
        when(assetStateMapper.selectBySymbol("BTCUSDT"))
                .thenReturn(sourceState("BTCUSDT", traceId));
        when(analysisRunMapper.selectById(decision.getAnalysisId()))
                .thenReturn(sourceRun(decision.getAnalysisId(), "BTCUSDT", traceId));
        UserPositionVO position = activeManualPosition(331L, "BTCUSDT", null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6, 331L)
                .getExecutionSuggestion();

        assertThat(suggestion.getStatus()).isEqualTo("PLAN_IDENTITY_MISSING");
        assertThat(suggestion.getPositionMode()).isFalse();
        assertThat(suggestion.getPositionMonitor()).isNull();
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getSourceExecutionPlanId()).isNull();
    }

    @Test
    void homeProjectionPerformsNoPositionMutation() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));
        when(userPositionService.listOpenPositionsForUser(USER_ID))
                .thenReturn(List.of(activeManualPosition(341L, "BTCUSDT", null)));

        service.getHomeForUser(USER_ID, "BTCUSDT", 6, 341L);

        verify(userPositionService, never()).manualOpenForUser(any(), any());
        verify(userPositionService, never()).manualCloseForUser(any(), any(), any());
        verify(executionPlanMapper, never()).insert(any());
        verify(executionPlanMapper, never()).markNeedsRevalidationForHotReset(
                any(), any(), any(), any(), any());
    }

    @Test
    void selectedSymbolDrivesExecutionSuggestionWithoutCrossFieldFallbacks() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setEntryZone("BTC entry");
        btc.setStopLoss("BTC stop");
        btc.setTakeProfitRules("BTC take profit");
        btc.setLeverageSuggestion("20x");
        btc.setPositionSuggestion("10%");
        btc.setValidPeriod(ACTIVE_VALID_PERIOD);
        setActivePlanValidity(btc);
        btc.setInvalidCondition("BTC invalid");

        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "MEDIUM", 70, 10,
                "LEVEL_1", true, "{\"state\":\"CANDIDATE\"}");
        eth.setEntryZone("ETH entry");
        eth.setStopLoss("ETH stop");
        eth.setTakeProfitRules("ETH take profit");
        eth.setLeverageSuggestion("3x");
        eth.setPositionSuggestion("5%");
        eth.setValidPeriod(ACTIVE_VALID_PERIOD);
        setActivePlanValidity(eth);
        eth.setInvalidCondition("ETH invalid");
        allowMatchingSnapshot(btc);
        allowMatchingSnapshot(eth);

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc, eth));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of());

        DashboardHomeVO ethHome = service.getHomeForUser(USER_ID, "ETHUSDT", 6);

        assertThat(ethHome.getSelectedSymbol()).isEqualTo("ETHUSDT");
        assertThat(ethHome.getExecutionSuggestion().getSourceAnalysisId()).isEqualTo("analysis-ETHUSDT");
        assertThat(ethHome.getExecutionSuggestion().getDirection()).isEqualTo("BEARISH");
        assertThat(ethHome.getExecutionSuggestion().getEntryZone()).isEqualTo("ETH entry");
        assertThat(ethHome.getExecutionSuggestion().getStopLoss()).isEqualTo("ETH stop");
        assertThat(ethHome.getExecutionSuggestion().getTakeProfitRules()).isEqualTo("ETH take profit");
        assertThat(ethHome.getExecutionSuggestion().getLeverageSuggestion()).isEqualTo("3x");
        assertThat(ethHome.getExecutionSuggestion().getPositionSuggestion()).isEqualTo("5%");
        assertThat(ethHome.getExecutionSuggestion().getValidPeriod()).isEqualTo(ACTIVE_VALID_PERIOD);
        assertThat(ethHome.getExecutionSuggestion().getInvalidCondition()).isEqualTo("ETH invalid");

        DashboardHomeVO defaultHome = service.getHomeForUser(USER_ID, null, 6);

        assertThat(defaultHome.getSelectedSymbol()).isEqualTo("BTCUSDT");
        assertThat(defaultHome.getExecutionSuggestion().getSourceAnalysisId()).isEqualTo("analysis-BTCUSDT");
        assertThat(defaultHome.getExecutionSuggestion().getEntryZone()).isEqualTo("BTC entry");
        assertThat(defaultHome.getExecutionSuggestion().getValidPeriod()).isEqualTo(ACTIVE_VALID_PERIOD);
    }

    @Test
    void selectedDecisionOutsideVisibleLimitReplacesLastRankedAsset() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "MEDIUM", 75, 20,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        DecisionResultVO bnb = decision("BNBUSDT", "RANGE", "LOW", "LOW", 70, 10,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        DecisionResultVO sol = decision("SOLUSDT", "BULLISH", "MEDIUM", "HIGH", 80, 40,
                "LEVEL_3_DIVERGENCE", false, "{\"state\":\"HIGH_RISK\"}");

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc, eth, bnb, sol));
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of());

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "SOLUSDT", 3);

        assertThat(home.getSelectedSymbol()).isEqualTo("SOLUSDT");
        assertThat(home.getAssets())
                .extracting(DashboardHomeVO.AssetVO::getRawSymbol)
                .containsExactly("BTCUSDT", "ETHUSDT", "SOLUSDT");
        assertThat(asset(home, "SOL/USDT").getSlotType()).isEqualTo("DECISION");
    }

    @Test
    void decisionAssetsExposeOnlyTheirAuthoritativeMatchingAnalysisIdentity() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAnalysisId("analysis-btc-exact");
        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "HIGH", 74, 35,
                "LEVEL_2_REVIEW", false, "{\"state\":\"OBSERVING\"}");
        eth.setAnalysisId("analysis-eth-exact");
        DecisionResultVO solWithoutIdentity = decision("SOLUSDT", "RANGE", "LOW", "LOW", 60, 10,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        solWithoutIdentity.setAnalysisId(null);

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(btc, eth, solWithoutIdentity));
        when(analysisRunMapper.selectById("analysis-btc-exact"))
                .thenReturn(analysisRun("analysis-btc-exact", "BTC/USDT"));
        when(analysisRunMapper.selectById("analysis-eth-exact"))
                .thenReturn(analysisRun("analysis-eth-exact", "ETHUSDT"));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 3);

        assertThat(asset(home, "BTC/USDT").getAnalysisId()).isEqualTo("analysis-btc-exact");
        assertThat(asset(home, "ETH/USDT").getAnalysisId()).isEqualTo("analysis-eth-exact");
        assertThat(asset(home, "SOL/USDT").getAnalysisId()).isNull();
        verify(analysisRunMapper, never()).selectLatestBySymbol(anyString());
    }

    @Test
    void orphanAnalysisIdentityFailsClosedWithoutBreakingDashboard() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAnalysisId("analysis-orphan");

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc));
        when(analysisRunMapper.selectById("analysis-orphan")).thenReturn(null);

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);

        assertThat(asset(home, "BTC/USDT").getAnalysisId()).isNull();
        assertThat(asset(home, "BTC/USDT").getMarketBias()).isEqualTo("BULLISH");
        verify(analysisRunMapper, never()).selectLatestBySymbol(anyString());
    }

    @Test
    void mismatchedAnalysisRunSymbolFailsClosed() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAnalysisId("analysis-symbol-mismatch");

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc));
        when(analysisRunMapper.selectById("analysis-symbol-mismatch"))
                .thenReturn(analysisRun("analysis-symbol-mismatch", "ETHUSDT"));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);

        assertThat(asset(home, "BTC/USDT").getAnalysisId()).isNull();
        verify(analysisRunMapper, never()).selectLatestBySymbol(anyString());
    }

    @Test
    void nullDecisionAnalysisIdentityRemainsNullWithoutLookupOrFallback() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAnalysisId(null);

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);

        assertThat(asset(home, "BTC/USDT").getAnalysisId()).isNull();
        verify(analysisRunMapper, never()).selectById(anyString());
        verify(analysisRunMapper, never()).selectLatestBySymbol(anyString());
    }

    @Test
    void analysisIdentityLookupFailureIsIsolatedPerAsset() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAnalysisId("analysis-btc-failing");
        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "HIGH", 74, 35,
                "LEVEL_2_REVIEW", false, "{\"state\":\"OBSERVING\"}");
        eth.setAnalysisId("analysis-eth-valid");

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc, eth));
        when(analysisRunMapper.selectById("analysis-btc-failing"))
                .thenThrow(new IllegalStateException("isolated lookup failure"));
        when(analysisRunMapper.selectById("analysis-eth-valid"))
                .thenReturn(analysisRun("analysis-eth-valid", "ETHUSDT"));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 2);

        assertThat(asset(home, "BTC/USDT").getAnalysisId()).isNull();
        assertThat(asset(home, "BTC/USDT").getMarketBias()).isEqualTo("BULLISH");
        assertThat(asset(home, "ETH/USDT").getAnalysisId()).isEqualTo("analysis-eth-valid");
    }

    @Test
    void marketDataAloneCannotCreateHomeOpportunityAssetsOrDefaultSlots() {
        PersistedOhlcvBarDO bnb = new PersistedOhlcvBarDO();
        bnb.setSymbol("BNBUSDT");
        bnb.setTimeframe("5m");
        bnb.setClosePrice(new BigDecimal("620.00"));
        bnb.setProvider("BINANCE_PUBLIC");
        bnb.setFreshnessStatus("FRESH");

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of());
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of());
        lenient().when(persistedOhlcvBarMapper.selectLatestClosedWindow("BNBUSDT", "5m", 1))
                .thenReturn(List.of(bnb));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 3);

        assertThat(home.getSelectedSymbol()).isNull();
        assertThat(home.getAssets()).isEmpty();
        verify(decisionService, never()).getLatestDecisionResultBySymbolForUser(any(), anyString());
    }

    @Test
    void unrankedDecisionAndMarketDataCannotBecomeImplicitHomeOpportunity() {
        PersistedOhlcvBarDO bnbBar = new PersistedOhlcvBarDO();
        bnbBar.setSymbol("BNBUSDT");
        bnbBar.setTimeframe("5m");
        bnbBar.setClosePrice(new BigDecimal("620.00"));
        bnbBar.setProvider("BINANCE_PUBLIC");
        bnbBar.setFreshnessStatus("FRESH");

        DecisionResultVO bnbDecision = decision("BNBUSDT", "BULLISH", "HIGH", "MEDIUM", 86, 20,
                "LEVEL_1_CONSISTENT", true, "{\"state\":\"CANDIDATE\"}");
        bnbDecision.setEntryZone("610-615");
        bnbDecision.setStopLoss("600");
        bnbDecision.setTakeProfitRules("630 / 640");
        bnbDecision.setLeverageSuggestion("2x");
        bnbDecision.setPositionSuggestion("人工复核仓位");
        bnbDecision.setValidPeriod(ACTIVE_VALID_PERIOD);
        bnbDecision.setInvalidCondition("跌破 600");
        setActivePlanValidity(bnbDecision);

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of());
        lenient().when(decisionService.getLatestDecisionResultBySymbolForUser(USER_ID, "BNBUSDT"))
                .thenReturn(bnbDecision);
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of());
        lenient().when(persistedOhlcvBarMapper.selectLatestClosedWindow("BNBUSDT", "5m", 1))
                .thenReturn(List.of(bnbBar));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, null, 3);

        assertThat(home.getSelectedSymbol()).isNull();
        assertThat(home.getAssets()).isEmpty();
        assertThat(home.getExecutionSuggestion().getSourceAnalysisId()).isNull();
        verify(decisionService, never()).getLatestDecisionResultBySymbolForUser(any(), anyString());
    }

    @Test
    void assetProjectionUsesPersistedMarketDataDerivedDecisionFieldsAndBusinessTime() {
        LocalDateTime marketUpdatedAt = LocalDateTime.of(2026, 7, 21, 9, 30);
        LocalDateTime decisionUpdatedAt = LocalDateTime.of(2026, 7, 21, 10, 0);
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 92, 20,
                "LEVEL_1", false, "{\"state\":\"CANDIDATE\"}");
        decision.setMultiTfConvergence("ALIGNED");
        decision.setConfusedScore(25);
        decision.setCreateTime(decisionUpdatedAt);
        AnalysisRunDO run = analysisRun(decision.getAnalysisId(), "BTCUSDT");
        when(analysisRunMapper.selectById(decision.getAnalysisId())).thenReturn(run);
        when(analysisRunMapper.selectAverageScoreByAnalysisId(decision.getAnalysisId())).thenReturn(86.4);
        when(analysisRunMapper.countEvidenceByAnalysisId(decision.getAnalysisId())).thenReturn(7);
        when(assetStateMapper.selectBySymbol("BTCUSDT"))
                .thenReturn(sourceState("BTCUSDT", null));
        PersistedOhlcvBarDO marketBar = persistedBar("BTCUSDT", "64123.45", "FRESH", marketUpdatedAt);
        lenient().when(persistedOhlcvBarMapper.selectLatestClosedWindow(eq("BTCUSDT"), anyString(), eq(1)))
                .thenReturn(List.of(marketBar));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);
        DashboardHomeVO.AssetVO asset = asset(home, "BTC/USDT");

        assertThat(asset.getLatestPrice()).isEqualByComparingTo("64123.45");
        assertThat(asset.getMarketBias()).isEqualTo("BULLISH");
        assertThat(asset.getCompositeScore()).isEqualTo(86);
        assertThat(asset.getConfidenceLevel()).isEqualTo("HIGH");
        assertThat(asset.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(asset.getAssetState()).isEqualTo("CANDIDATE");
        assertThat(asset.getDataQuality()).isEqualTo("GOOD");
        assertThat(asset.getMultiTimeframeState()).isEqualTo("ALIGNED");
        assertThat(asset.getConfused()).isFalse();
        assertThat(asset.getUpdatedAt()).isEqualTo(decisionUpdatedAt);
        assertThat(home.getHeader().getUpdatedAt()).isEqualTo(decisionUpdatedAt);
        assertThat(asset.getModuleState()).isEqualTo("READY");
        assertThat(home.getStates().getAssets()).isEqualTo("READY");
        assertThat(asset.getFieldSourceStatus()).containsEntry("symbol", "REAL")
                .containsEntry("latestPrice", "REAL")
                .containsEntry("direction", "DERIVED")
                .containsEntry("score", "DERIVED")
                .containsEntry("confidence", "DERIVED")
                .containsEntry("riskLevel", "DERIVED")
                .containsEntry("assetState", "REAL")
                .containsEntry("dataQuality", "DERIVED")
                .containsEntry("multiTimeframeState", "DERIVED")
                .containsEntry("confused", "DERIVED")
                .containsEntry("updatedAt", "REAL");
    }

    @Test
    void assetCardDataQualityBoundaryBlocksSixtyAndSixtyNine() {
        for (int score : List.of(60, 69)) {
            DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", score, 20,
                    "LEVEL_1", true, "{\"state\":\"CANDIDATE\"}");
            decision.setMultiTfConvergence("ALIGNED");
            stubCompleteAssetProjection(decision);
            when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                    .thenReturn(List.of(decision));

            DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);
            DashboardHomeVO.AssetVO asset = asset(home, "BTC/USDT");

            assertThat(asset.getDataQuality()).isEqualTo("PARTIAL");
            assertThat(asset.getModuleState()).isEqualTo("PARTIAL");
            assertThat(asset.getCurrentConclusion()).contains("暂不交易 / 事件观望");
            assertThat(home.getStates().getAssets()).isEqualTo("PARTIAL");
            assertThat(home.getStates().getOverall()).isNotEqualTo("READY");
        }
    }

    @Test
    void assetCardAtSeventyRequiresEveryOtherFieldBeforeReady() {
        DecisionResultVO complete = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 70, 20,
                "LEVEL_1", true, "{\"state\":\"CANDIDATE\"}");
        complete.setMultiTfConvergence("ALIGNED");
        stubCompleteAssetProjection(complete);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(complete));

        DashboardHomeVO.AssetVO ready = asset(
                service.getHomeForUser(USER_ID, "BTCUSDT", 1), "BTC/USDT");

        assertThat(ready.getDataQuality()).isEqualTo("GOOD");
        assertThat(ready.getModuleState()).isEqualTo("READY");

        complete.setMultiTfConvergence(null);
        DashboardHomeVO.AssetVO missingMultiTimeframe = asset(
                service.getHomeForUser(USER_ID, "BTCUSDT", 1), "BTC/USDT");

        assertThat(missingMultiTimeframe.getDataQuality()).isEqualTo("GOOD");
        assertThat(missingMultiTimeframe.getModuleState()).isEqualTo("PARTIAL");
    }

    @Test
    void marketDataSourceCannotCreateHomeAssetWithoutOpportunity() {
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of());

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);

        assertThat(home.getAssets()).isEmpty();
        assertThat(home.getStates().getAssets()).isNotEqualTo("READY");
        verify(persistedOhlcvBarMapper, never()).selectLatestClosedWindow(anyString(), anyString(), anyInt());
    }

    @Test
    void marketReadFailureIsErrorAndCannotBeMaskedByOtherAssetFields() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 92, 20,
                "LEVEL_1", false, "{\"state\":\"CANDIDATE\"}");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));
        when(persistedOhlcvBarMapper.selectLatestClosedWindow(eq("BTCUSDT"), anyString(), eq(1)))
                .thenThrow(new IllegalStateException("market read failed"));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);
        DashboardHomeVO.AssetVO asset = asset(home, "BTC/USDT");

        assertThat(asset.getLatestPrice()).isNull();
        assertThat(asset.getDataQuality()).isEqualTo("ERROR");
        assertThat(asset.getModuleState()).isEqualTo("ERROR");
        assertThat(asset.getFieldSourceStatus()).containsEntry("latestPrice", "ERROR")
                .containsEntry("dataQuality", "ERROR")
                .containsEntry("updatedAt", "ERROR");
        assertThat(home.getStates().getAssets()).isEqualTo("ERROR");
        assertThat(home.getStates().getOverall()).isEqualTo("ERROR");
    }

    @Test
    void positionReadFailureRemainsVisibleInModuleAndOverallStates() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 92, 20,
                "LEVEL_1", false, "{\"state\":\"CANDIDATE\"}");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(decision));
        when(userPositionService.listOpenPositionsForUser(USER_ID))
                .thenThrow(new IllegalStateException("owner position read failed"));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 1);

        assertThat(home.getPositions()).isEmpty();
        assertThat(home.getStates().getPositions()).isEqualTo("ERROR");
        assertThat(home.getStates().getOverall()).isNotEqualTo("READY");
    }

    @Test
    void oneMinuteDecisionDoesNotExposeFormalExecutionSuggestionBoundary() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 85, 10,
                "LEVEL_1", true, "{\"state\":\"CANDIDATE\"}");
        decision.setTimeframe("1m");
        decision.setEntryZone("63000-64000");
        decision.setStopLoss("61000");
        decision.setTakeProfitRules("66000 / 69000");
        decision.setLeverageSuggestion("20x");
        decision.setPositionSuggestion("10%");
        decision.setValidPeriod("2026-07-03 00:34:21 ~ 2026-07-04 00:34:21");
        decision.setInvalidCondition("结构失效：当前价高于近端 1m 摆动高点");

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("UNSUPPORTED_TIMEFRAME");
        assertThat(home.getExecutionSuggestion().getDirection()).isNull();
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getStopLoss()).isNull();
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isNull();
        assertThat(home.getExecutionSuggestion().getLeverageSuggestion()).isNull();
        assertThat(home.getExecutionSuggestion().getPositionSuggestion()).isNull();
        assertThat(home.getExecutionSuggestion().getValidPeriod()).isNull();
        assertThat(home.getExecutionSuggestion().getBlockedReason())
                .isEqualTo("周期不支持，需使用 5m / 15m / 1h / 4h");
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
    }

    @Test
    void incompleteExecutionBoundaryShowsStructurePendingValidPeriod() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 85, 10,
                "LEVEL_1", true, "{\"state\":\"CANDIDATE\"}");
        decision.setTimeframe("5m");
        decision.setEntryZone("暂无");
        decision.setStopLoss("61000");
        decision.setTakeProfitRules("66000 / 69000");
        decision.setLeverageSuggestion("3x");
        decision.setPositionSuggestion("10%");
        decision.setValidPeriod("2026-07-03 00:34:21 ~ 2026-07-04 00:34:21");
        decision.setInvalidCondition("结构失效：当前价高于近端 5m 摆动高点");
        allowMatchingSnapshot(decision);

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("BOUNDARY_INCOMPLETE");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getStopLoss()).isNull();
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isNull();
        assertThat(home.getExecutionSuggestion().getValidPeriod()).isNull();
        assertThat(home.getExecutionSuggestion().getBlockedReason()).isEqualTo("边界不足，等待结构确认");
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
    }

    @Test
    void gptFinalTabShowsFinalDecisionFieldsOnly() {
        DecisionResultVO decision = decisionWithStructuredAiRoles();
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getAiDecision().getActiveTab()).isEqualTo("GPT_FINAL");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRoleLabel)
                .containsExactly("最终裁决官", "冲突复核官", "反方挑战官");

        DashboardHomeVO.AiTabVO gpt = aiTab(home, "GPT_FINAL");
        assertThat(gpt.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(gpt.getFinalConfidence()).isEqualTo("HIGH");
        assertThat(gpt.getFinalRiskLevel()).isEqualTo("HIGH");
        assertThat(gpt.getFinalPlanMode()).isEqualTo("PREPARATION");
        assertThat(gpt.getWorthOpening()).isEqualTo("是");
        assertThat(gpt.getFinalConclusion()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gpt.getCoreSupportingEvidence()).containsExactly("AI 证据已记录，需人工复核");
        assertThat(gpt.getCoreCounterEvidence()).isEmpty();
        assertThat(gpt.getDecisionSummary()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gpt.getDowngradeReason()).isEqualTo("GEMINI_CONTRADICTION_ONLY");
        assertThat(gpt.getDirection()).isEqualTo("BULLISH");
        assertThat(gpt.getSupportEvidence()).containsExactly("AI 证据已记录，需人工复核");
        assertThat(gpt.getReviewVerdict()).isNull();
        assertThat(gpt.getChallengeThesis()).isNull();
    }

    @Test
    void geminiReviewTabShowsConflictReviewFieldsOnly() {
        DecisionResultVO decision = decisionWithStructuredAiRoles();
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        DashboardHomeVO.AiTabVO gemini = aiTab(home, "GEMINI_REVIEW");
        assertThat(gemini.getReviewVerdict()).isEqualTo("DOWNGRADE");
        assertThat(gemini.getDetectedContradictions()).containsExactly("AI 发现证据冲突");
        assertThat(gemini.getWeakEvidence()).isEmpty();
        assertThat(gemini.getLogicGaps()).containsExactly("AI 发现证据冲突");
        assertThat(gemini.getDowngradeRecommendation())
                .isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gemini.getRiskAdjustmentSuggestion()).isEqualTo("RAISED");
        assertThat(gemini.getManualReviewRequired()).isEqualTo("是");
        assertThat(gemini.getReviewConclusion()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gemini.getDirection()).isNull();
        assertThat(gemini.getFinalMarketBias()).isNull();
        assertThat(gemini.getChallengeThesis()).isNull();
        assertThat(gemini.getSupportEvidence()).isEmpty();
    }

    @Test
    void grokChallengeTabShowsCounterEvidenceFieldsOnly() {
        DecisionResultVO decision = decisionWithStructuredAiRoles();
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        DashboardHomeVO.AiTabVO grok = aiTab(home, "GROK_CHALLENGE");
        assertThat(grok.getChallengeThesis()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(grok.getEventRisks()).isEmpty();
        assertThat(grok.getSentimentReversalRisks()).containsExactly("AI 提供反向证据");
        assertThat(grok.getMicrostructureTraps()).isEmpty();
        assertThat(grok.getLiquidityRisks()).isEmpty();
        assertThat(grok.getCounterEvidence()).containsExactly("AI 提供反向证据");
        assertThat(grok.getChallengeConclusion()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(grok.getDirection()).isNull();
        assertThat(grok.getFinalMarketBias()).isNull();
        assertThat(grok.getReviewVerdict()).isNull();
        assertThat(grok.getSupportEvidence()).isEmpty();
    }

    @Test
    void missingRoleDataDoesNotCloneOtherRoleContent() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "GPT_ONLY_EVIDENCE", "GPT only summary")),
                synthesis("BULLISH", "HIGH", "HIGH", "CONFIRM", true, null)));

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        DashboardHomeVO.AiTabVO gpt = aiTab(home, "GPT_FINAL");
        assertThat(gpt.getCoreSupportingEvidence()).containsExactly("AI 证据已记录，需人工复核");
        assertThat(gpt.getDecisionSummary()).isEqualTo("AI 复核结果已返回，等待人工复核");

        DashboardHomeVO.AiTabVO gemini = aiTab(home, "GEMINI_REVIEW");
        assertThat(gemini.getReviewVerdict()).isNull();
        assertThat(gemini.getDetectedContradictions()).isEmpty();
        assertThat(gemini.getWeakEvidence()).isEmpty();
        assertThat(gemini.getReviewConclusion()).isNull();
        assertThat(gemini.getDirection()).isNull();
        assertThat(gemini.getSupportEvidence()).isEmpty();

        DashboardHomeVO.AiTabVO grok = aiTab(home, "GROK_CHALLENGE");
        assertThat(grok.getChallengeThesis()).isNull();
        assertThat(grok.getCounterEvidence()).isEmpty();
        assertThat(grok.getChallengeConclusion()).isNull();
        assertThat(grok.getDirection()).isNull();
        assertThat(grok.getSupportEvidence()).isEmpty();
    }

    @Test
    void malformedPayloadFailsClosedAndLegacyPlainTextDoesNotBecomeThreeRoleEvidence() {
        DecisionResultVO malformed = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        malformed.setInvalidCondition("跌破 61000");
        malformed.setAiRoleResults("{not-json");

        DecisionResultVO raw = decision("ETHUSDT", "BEARISH", "MEDIUM", "MEDIUM", 70, 10,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        raw.setInvalidCondition("站回 3100");
        raw.setAiRoleResults("orchestrationMode=RULE_ONLY_FALLBACK; providers=OPENAI:SUCCESS:SUPPORT");

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(malformed, raw));

        DashboardHomeVO malformedHome = service.getHomeForUser(USER_ID, "BTCUSDT", 6);
        DashboardHomeVO rawHome = service.getHomeForUser(USER_ID, "ETHUSDT", 6);

        assertNoAiEvidence(malformedHome);
        assertNoAiEvidence(rawHome);
    }

    @Test
    void aiDecisionDoesNotUseInvalidConditionAsAgainstEvidence() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        decision.setInvalidCondition("跌破 61000");
        decision.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.ABSTAIN, "GPT_REVIEW", "只映射显式结论")),
                synthesis("BULLISH", "HIGH", "HIGH", "REDUCED", true, null)));

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        DashboardHomeVO.AiTabVO gpt = aiTab(home, "GPT_FINAL");
        assertThat(gpt.getResultAvailable()).isTrue();
        assertThat(gpt.getStance()).isEqualTo("ABSTAIN");
        assertThat(gpt.getReviewConclusion()).isEqualTo("证据不足，暂不判断");
        assertThat(gpt.getFinalMarketBias()).isNull();
        assertThat(gpt.getFinalPlanMode()).isNull();
        assertThat(gpt.getAgainstEvidence()).isEmpty();
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .doesNotContain("裁决", "FINAL", "AI_SUMMARY");
    }

    @Test
    void disabledAiRoleHasNoBusinessResult() {
        assertNonSuccessfulAiRole(AiProviderCallStatus.DISABLED, "AI 复核未启用");
    }

    @Test
    void timeoutAiRoleHasNoBusinessResult() {
        assertNonSuccessfulAiRole(AiProviderCallStatus.TIMEOUT, "AI 复核超时，本轮未采纳该角色");
    }

    @Test
    void failedAiRoleHasNoBusinessResult() {
        assertNonSuccessfulAiRole(AiProviderCallStatus.FAILED, "AI 复核失败，本轮未采纳该角色");
    }

    @Test
    void notConfiguredAiRoleHasNoBusinessResult() {
        assertNonSuccessfulAiRole(AiProviderCallStatus.NOT_CONFIGURED, "AI 模型未配置");
    }

    @Test
    void budgetBlockedAiRoleHasNoBusinessResult() {
        assertNonSuccessfulAiRole(AiProviderCallStatus.BUDGET_BLOCKED, "AI 预算门控阻断");
    }

    @Test
    void successfulAbstainDoesNotExposeFinalDirection() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "证据不足")),
                synthesis("BULLISH", "HIGH", "HIGH", "PREPARE_ONLY", true, null)));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.AiTabVO tab = aiTab(service.getHomeForUser(USER_ID, "BTCUSDT", 6), "GPT_FINAL");

        assertThat(tab.getResultAvailable()).isTrue();
        assertThat(tab.getStance()).isEqualTo("ABSTAIN");
        assertThat(tab.getReviewConclusion()).isEqualTo("证据不足，暂不判断");
        assertThat(tab.getDirection()).isNull();
        assertThat(tab.getFinalMarketBias()).isNull();
        assertThat(tab.getFinalPlanMode()).isNull();
        assertThat(tab.getWorthOpening()).isNull();
        assertThat(tab.getSupportEvidence()).isEmpty();
        assertThat(tab.getAgainstEvidence()).isEmpty();
    }

    @Test
    void singleSuccessfulAbstainMakesConsistencyNotApplicable() {
        DecisionResultVO decision = decisionWithRoles(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "证据不足")));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getAiDecision().getRunStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertConsistencyNotApplicable(home);
    }

    @Test
    void allThreeSuccessfulAbstainMakeConsistencyNotApplicable() {
        DecisionResultVO decision = allAbstainDecision();
        decision.setConfusedScore(100);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getAiDecision().getRunStatus()).isEqualTo("SUCCESS");
        assertThat(home.getAiDecision().getRunStatusLabel()).isEqualTo("复核成功");
        assertThat(home.getAiDecision().getDecisionModeLabel()).isEqualTo("AI 复核无可裁决结论");
        assertThat(home.getAiDecision().getTabs()).allSatisfy(tab -> {
            assertThat(tab.getRunStatusLabel()).isEqualTo("复核成功");
            assertThat(tab.getDataState()).isEqualTo("INSUFFICIENT_DATA");
        });
        assertConsistencyNotApplicable(home);
    }

    @Test
    void supportPlusAbstainUsesOnlySupportVote() {
        DecisionResultVO decision = decisionWithRoles(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "支持"),
                role(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "弃权")));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getAiDecision().getConsistency().getDataState()).isEqualTo("READY");
        assertThat(home.getAiDecision().getConsistency().getConflictLevel())
                .isEqualTo("LEVEL_2_MINOR_DISAGREEMENT");
        assertThat(home.getSystemState().getAiConflict().getValue())
                .isEqualTo("LEVEL_2_MINOR_DISAGREEMENT");
        assertThat(aiTab(home, "GPT_FINAL").getStance()).isEqualTo("SUPPORT");
        assertThat(aiTab(home, "GEMINI_REVIEW").getStance()).isEqualTo("ABSTAIN");
    }

    @Test
    void challengePlusAbstainUsesOnlyChallengeVote() {
        DecisionResultVO decision = decisionWithRoles(List.of(
                role(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        AiReviewStance.CHALLENGE, "GEMINI_CONTRADICTION_ONLY", "反对"),
                role(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "弃权")));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(home.getAiDecision().getConsistency().getDataState()).isEqualTo("READY");
        assertThat(home.getAiDecision().getConsistency().getConflictLevel())
                .isEqualTo("LEVEL_2_MINOR_DISAGREEMENT");
        assertThat(home.getSystemState().getAiConflict().getValueLabel()).isEqualTo("轻微分歧");
        assertThat(aiTab(home, "GEMINI_REVIEW").getStance()).isEqualTo("CHALLENGE");
        assertThat(aiTab(home, "GROK_CHALLENGE").getStance()).isEqualTo("ABSTAIN");
    }

    @Test
    void allAbstainKpiShowsNotApplicable() {
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(allAbstainDecision()));

        DashboardHomeVO.StatusCardVO card = service.getHomeForUser(USER_ID, "BTCUSDT", 6).getSystemState().getAiConflict();

        assertThat(card.getValue()).isNull();
        assertThat(card.getValueLabel()).isEqualTo("不适用");
        assertThat(card.getHelper()).isEqualTo("本轮未形成可裁决 AI 意见");
        assertThat(card.getStatus()).isEqualTo("NOT_APPLICABLE");
    }

    @Test
    void realDashboardHomeServiceLocalizesAiConflictKpi() {
        DecisionResultVO decision = decisionWithRoles(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "支持")));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.StatusCardVO card = service.getHomeForUser(USER_ID, "BTCUSDT", 6).getSystemState().getAiConflict();

        assertThat(card.getValue()).isEqualTo("LEVEL_2_MINOR_DISAGREEMENT");
        assertThat(card.getValueLabel()).isEqualTo("轻微分歧");
    }

    @Test
    void controllerSerializesLocalizedAiConflictKpiFromServiceOutput() throws Exception {
        DecisionResultVO decision = decisionWithRoles(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "支持")));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));
        AuthenticatedUserIdResolver resolver = mock(AuthenticatedUserIdResolver.class);
        when(resolver.requireCurrentUserId()).thenReturn(USER_ID);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DashboardHomeController(service, resolver)).build();

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemState.aiConflict.value")
                        .value("LEVEL_2_MINOR_DISAGREEMENT"))
                .andExpect(jsonPath("$.data.systemState.aiConflict.valueLabel").value("轻微分歧"));
    }

    @Test
    void headerDisabledAiShowsChineseDisabledLabel() {
        when(providerReadinessService.getReadiness())
                .thenReturn(providerReadiness("CONFIGURED", "DISABLED", "WAITING_SYNC", "真实行情"));

        DashboardHomeVO.HeaderVO header = service.getHomeForUser(USER_ID, "BTCUSDT", 6).getHeader();

        assertThat(header.getAiStatus()).isEqualTo("DISABLED");
        assertThat(header.getAiStatusLabel()).isEqualTo("已禁用");
    }

    @Test
    void systemStripUsesMacroAndAggregateOwnersInsteadOfSelectedAsset() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "LOW", "HIGH", 80, 0,
                "LEVEL_1_CONSISTENT", true, "{\"state\":\"WAITING_TRIGGER\"}");
        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "HIGH", "MEDIUM", 60, 0,
                "LEVEL_1_CONSISTENT", false, "{\"state\":\"OBSERVING\"}");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(btc, eth));
        when(providerReadinessService.getReadiness())
                .thenReturn(providerReadiness("CONNECTED", "SUCCESS", "WAITING_SYNC", "真实行情"));

        DashboardHomeVO.SystemStateVO ethState = service
                .getHomeForUser(USER_ID, "ETHUSDT", 6).getSystemState();
        DashboardHomeVO.SystemStateVO btcState = service
                .getHomeForUser(USER_ID, "BTCUSDT", 6).getSystemState();

        assertThat(ethState.getMarketTrend().getValue()).isNull();
        assertThat(ethState.getMarketTrend().getValueLabel()).isEqualTo("— / 当前不可查看");
        assertThat(ethState.getRiskLevel().getValue()).isNull();
        assertThat(ethState.getRiskLevel().getValueLabel()).isEqualTo("— / 尚未形成系统级评估");
        assertThat(ethState.getDataQuality().getValue()).isEqualTo("FRESH");
        assertThat(ethState.getDataQuality().getValueLabel()).isEqualTo("新鲜");
        assertThat(ethState.getServiceAvailability().getValueLabel()).isEqualTo("正常");
        assertThat(btcState.getMarketTrend().getValueLabel()).isEqualTo(ethState.getMarketTrend().getValueLabel());
        assertThat(btcState.getRiskLevel().getValueLabel()).isEqualTo(ethState.getRiskLevel().getValueLabel());
        assertThat(btcState.getDataQuality().getValueLabel()).isEqualTo(ethState.getDataQuality().getValueLabel());
    }

    @Test
    void systemStripUsesFormalFreshMacroProducerWithoutReadingSelectedOpportunity() {
        PersistedRealMarketEnvironmentService environmentService = mock(PersistedRealMarketEnvironmentService.class);
        MarketEnvironmentVO environment = new MarketEnvironmentVO();
        environment.setEnvironmentType("trend_market");
        environment.setFreshness("FRESH");
        when(environmentService.assess("BTCUSDT", "1h")).thenReturn(
                new PersistedRealMarketEnvironmentAssessment(true, null, "KRAKEN",
                        "KRAKEN_PERSISTED_OHLCV", environment, Map.of(), 400,
                        1_786_000_000_000L, List.of("trace-macro")));
        service.setPersistedRealMarketEnvironmentService(environmentService);

        DecisionResultVO btc = decision("BTCUSDT", "BEARISH", "LOW", "LOW", 90, 0,
                "LEVEL_1_CONSISTENT", true, "{\"state\":\"WAITING_TRIGGER\"}");
        DecisionResultVO eth = decision("ETHUSDT", "BULLISH", "HIGH", "HIGH", 70, 0,
                "LEVEL_2_MINOR_DISAGREEMENT", true, "{\"state\":\"CANDIDATE\"}");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt()))
                .thenReturn(List.of(btc, eth));

        DashboardHomeVO.SystemStateVO btcState = service.getHomeForUser(USER_ID, "BTCUSDT", 6).getSystemState();
        DashboardHomeVO.SystemStateVO ethState = service.getHomeForUser(USER_ID, "ETHUSDT", 6).getSystemState();

        assertThat(btcState.getMarketTrend().getValue()).isEqualTo("TREND_MARKET");
        assertThat(btcState.getMarketTrend().getValueLabel()).isEqualTo("趋势环境");
        assertThat(btcState.getMarketTrend().getHelper()).isEqualTo("KRAKEN_PERSISTED_OHLCV");
        assertThat(ethState.getMarketTrend().getValueLabel()).isEqualTo("趋势环境");
        verify(environmentService, times(2)).assess("BTCUSDT", "1h");
    }

    @Test
    void selectedSymbolDrivesAiDecisionEvidence() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "BTC summary")),
                synthesis("BULLISH", "HIGH", "HIGH", "CONFIRM", true, null)));

        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "MEDIUM", 70, 10,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        eth.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "INSUFFICIENT_DATA", "ETH summary")),
                synthesis("BEARISH", "MEDIUM", "MEDIUM", "REDUCED", false, null)));

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(btc, eth));

        DashboardHomeVO ethHome = service.getHomeForUser(USER_ID, "ETHUSDT", 6);
        DashboardHomeVO defaultHome = service.getHomeForUser(USER_ID, null, 6);

        assertThat(aiTab(ethHome, "GPT_FINAL").getSupportEvidence()).isEmpty();
        assertThat(aiTab(ethHome, "GPT_FINAL").getSupportingEvidenceState())
                .isEqualTo("INSUFFICIENT_DATA");
        assertThat(aiTab(defaultHome, "GPT_FINAL").getSupportEvidence()).containsExactly("规则方向与 AI 复核一致");
    }

    @Test
    void adjudicationConsistencyHasExplicitBackendContract() {
        DecisionResultVO decision = decisionWithStructuredAiRoles();
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        DashboardHomeVO.ConsistencyVO consistency = home.getAiDecision().getConsistency();
        assertThat(consistency.getDataState()).isEqualTo("READY");
        assertThat(consistency.getConflictLevel()).isEqualTo("LEVEL_2_MINOR_DISAGREEMENT");
        assertThat(consistency.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(consistency.getFinalPlanMode()).isEqualTo("PREPARATION");
        assertThat(consistency.getMainReason()).isEqualTo("GEMINI_CONTRADICTION_ONLY");
        assertThat(consistency.getRecoveryCondition()).isEqualTo("NEW_VERIFIED_ANALYSIS");
        assertThat(consistency.getLevel()).isNull();
        assertThat(consistency.getScore()).isNull();
        assertThat(consistency.getConsistencyScore()).isNull();
        assertThat(consistency.getConsistencyLevel()).isNull();
        assertThat(consistency.getConsistencySummary()).isNull();
        assertThat(consistency.getDowngradeReason()).isNull();
    }

    @Test
    void adjudicationConsistencyDoesNotFakeScore() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 80,
                "LEVEL_4_EXTREME_DIVERGENCE", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.CHALLENGE, "CONFLICT_TOO_HIGH", "GPT challenge")),
                synthesis("BULLISH", "LOW", "HIGH", "CONFUSED", false, "CONFLICT_TOO_HIGH")));

        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        DashboardHomeVO.ConsistencyVO consistency = home.getAiDecision().getConsistency();
        assertThat(consistency.getDataState()).isEqualTo("READY");
        assertThat(consistency.getConflictLevel()).isEqualTo("LEVEL_4_EXTREME_CONFLICT");
        assertThat(consistency.getFinalPlanMode()).isEqualTo("BLOCKED");
        assertThat(consistency.getMainReason()).isEqualTo("CONFLICT_TOO_HIGH");
        assertThat(consistency.getRecoveryCondition()).isEqualTo("NEW_VERIFIED_ANALYSIS");
        assertThat(consistency.getScore()).isNull();
        assertThat(consistency.getConsistencyScore()).isNull();
        assertThat(consistency.getDowngradeReason()).isNull();
    }

    @Test
    void authoritativeAssetStateRowOverridesDecisionSnapshotForAllEightStates() {
        DecisionResultVO decision = decision("BTCUSDT", "RANGE", "LOW", "MEDIUM", 80, 0,
                null, false, "{\"state\":\"OBSERVING\"}");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));
        AssetStateEnum[] states = {
                AssetStateEnum.OBSERVING,
                AssetStateEnum.CANDIDATE,
                AssetStateEnum.WAITING_TRIGGER,
                AssetStateEnum.TRIGGERED,
                AssetStateEnum.HIGH_RISK,
                AssetStateEnum.INVALIDATED,
                AssetStateEnum.COOLING,
                AssetStateEnum.CONFUSED
        };
        String[] labels = {"观察", "候选", "等待触发", "已触发", "高风险观察", "已失效", "冷却", "冲突状态"};

        for (int i = 0; i < states.length; i++) {
            AssetStateDO row = new AssetStateDO();
            row.setSymbol("BTCUSDT");
            row.setState(states[i]);
            when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row);

            DashboardHomeVO.AssetVO asset = asset(service.getHomeForUser(USER_ID, "BTCUSDT", 6), "BTC/USDT");

            assertThat(asset.getAssetState()).isEqualTo(states[i].name());
            assertThat(asset.getAssetStateLabel()).isEqualTo(labels[i]);
        }
    }

    @Test
    void dataQualityCircuitBreakerHidesExactPlanBelowSeventyAndMissingSnapshotHidesBoundaries() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        for (int score : List.of(59, 60, 69)) {
            decision.setDataQualityScore(score);
            DashboardHomeVO.ExecutionSuggestionVO blocked = service
                    .getHomeForUser(USER_ID, "BTCUSDT", 6)
                    .getExecutionSuggestion();

            assertUnavailableAssetExecutionPlan(blocked, "DATA_QUALITY_BLOCKED");
            assertThat(blocked.getModuleState()).isEqualTo("PARTIAL");
            assertThat(blocked.getBlockedReason()).contains("暂不交易 / 事件观望");
        }

        decision.setDataQualityScore(70);
        decision.setAnalysisId(null);
        DashboardHomeVO.ExecutionSuggestionVO missingSnapshot = service.getHomeForUser(USER_ID, "BTCUSDT", 6).getExecutionSuggestion();
        assertThat(missingSnapshot.getStatus()).isEqualTo("ANALYSIS_SNAPSHOT_MISSING");
        assertThat(missingSnapshot.getDirection()).isNull();
        assertThat(missingSnapshot.getEntryZone()).isNull();
    }

    @Test
    void dataQualityAtSeventyAllowsPlanOnlyAfterEveryExistingGatePasses() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        decision.setDataQualityScore(70);
        setActivePlanValidity(decision);
        String traceId = "trace-" + decision.getAnalysisId();
        when(assetStateMapper.selectBySymbol(decision.getSymbol()))
                .thenReturn(sourceState(decision.getSymbol(), traceId));
        when(analysisRunMapper.selectById(decision.getAnalysisId()))
                .thenReturn(sourceRun(decision.getAnalysisId(), decision.getSymbol(), traceId));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        assertUnavailableAssetExecutionPlan(
                service.getHomeForUser(USER_ID, "BTCUSDT", 6).getExecutionSuggestion(),
                "PLAN_IDENTITY_MISSING");

        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setNeedsRevalidation(true);
        assertUnavailableAssetExecutionPlan(
                service.getHomeForUser(USER_ID, "BTCUSDT", 6).getExecutionSuggestion(),
                "REVALIDATION_REQUIRED");

        plan.setNeedsRevalidation(false);
        plan.setSourceGateStatus("BLOCKED");
        assertUnavailableAssetExecutionPlan(
                service.getHomeForUser(USER_ID, "BTCUSDT", 6).getExecutionSuggestion(),
                "PLAN_BLOCKED");

        plan.setSourceGateStatus("VALID");
        assertAssetExecutionPlan(
                service.getHomeForUser(USER_ID, "BTCUSDT", 6).getExecutionSuggestion(),
                decision,
                plan.getPlanId());
    }

    @Test
    void zeroSuccessfulAiRolesRemainNotApplicableAndRuleOnly() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 80, 72,
                "LEVEL_3_DIVERGENCE", true, "{\"state\":\"CANDIDATE\"}");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.AiDecisionVO ai = service.getHomeForUser(USER_ID, "BTCUSDT", 6).getAiDecision();

        assertThat(ai.getRunStatus()).isEqualTo("NOT_CALLED");
        assertThat(ai.getDecisionModeLabel()).isEqualTo("仅规则判断");
        assertThat(ai.getConsistency().getLevel()).isNull();
        assertThat(ai.getConsistency().getScore()).isNull();
        assertThat(ai.getConsistency().getDataState()).isEqualTo("SOURCE_UNAVAILABLE");
        assertThat(ai.getConsistency().getConflictLevel()).isNull();
        assertThat(ai.getTabs()).allSatisfy(tab -> {
            assertThat(tab.getRunStatus()).isEqualTo("NOT_CALLED");
            assertThat(tab.getRunStatusLabel()).isEqualTo("未调用");
        });
    }

    @Test
    void aiNotApplicableOverridesDirectionalBlockInConsistencyCard() {
        DecisionResultVO snapshotBlocked = decision("BTCUSDT", "WAIT", "LOW", "HIGH", 80, null,
                null, false, "{\"state\":\"CANDIDATE\",\"directionalPushBlocked\":true}");
        DecisionResultVO scoreBlocked = decision("BTCUSDT", "WAIT", "LOW", "HIGH", 80, 100,
                "LEVEL_4_EXTREME_DIVERGENCE", false, "{\"state\":\"CANDIDATE\"}");
        scoreBlocked.setConfusedScore(100);
        ProviderReadinessVO aiDisabled = providerReadiness("CONFIGURED", "DISABLED", "WAITING_SYNC", "真实行情");
        when(providerReadinessService.getReadiness()).thenReturn(aiDisabled);

        for (DecisionResultVO decision : List.of(snapshotBlocked, scoreBlocked)) {
            when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

            DashboardHomeVO.ConsistencyVO consistency = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                    .getAiDecision().getConsistency();

            assertThat(consistency.getDataState()).isEqualTo("SOURCE_UNAVAILABLE");
            assertThat(consistency.getConflictLevel()).isNull();
            assertThat(consistency.getLevel()).isNull();
            assertThat(consistency.getScore()).isNull();
            assertThat(consistency.getConfused()).isNull();
            assertThat(consistency.getDirectionalPushBlocked()).isNull();
        }
    }

    @Test
    void expiredAbsoluteValidPeriodBlocksSuggestion() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO beforeExpiry = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();
        assertThat(beforeExpiry.getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
        assertThat(beforeExpiry.getValidFrom()).hasToString("2026-07-01T00:00Z");
        assertThat(beforeExpiry.getExpiresAt()).hasToString("2026-07-02T00:00Z");

        service.setPlanValidityClock(Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC));
        DashboardHomeVO.ExecutionSuggestionVO atExpiry = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();
        assertThat(atExpiry.getStatus()).isEqualTo("PLAN_EXPIRED");

        service.setPlanValidityClock(Clock.fixed(Instant.parse("2026-07-02T00:00:01Z"), ZoneOffset.UTC));
        DashboardHomeVO.ExecutionSuggestionVO afterExpiry = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();
        assertThat(afterExpiry.getStatus()).isEqualTo("PLAN_EXPIRED");
        assertThat(afterExpiry.getEntryZone()).isNull();
    }

    @Test
    void finalPlanWithoutStructuredValidityFailsClosedEvenWhenDecisionHasMalformedLegacyPeriod() {
        DecisionResultVO decision = completePlanDecision(
                "BTCUSDT", "2026/07/01 00:00:00 - 2026/07/02 00:00:00");
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setValidFrom(null);
        plan.setValidUntil(null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(suggestion.getStatus()).isEqualTo("PLAN_INCOMPLETE");
        assertThat(suggestion.getBlockedReason()).isEqualTo("执行计划状态、来源或边界信息不完整");
        assertThat(suggestion.getValidFrom()).isNull();
        assertThat(suggestion.getExpiresAt()).isNull();
    }

    @Test
    void decisionOnlyLegacyValidityCannotSubstituteForFinalPlanValidity() {
        DecisionResultVO decision = completePlanDecision(
                "BTCUSDT", "2026-07-01 00:00:00 ~ 2026-07-02 00:00:00");
        ExecutionPlanDO plan = allowMatchingSnapshot(decision);
        plan.setValidFrom(null);
        plan.setValidUntil(null);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(suggestion.getStatus()).isEqualTo("PLAN_INCOMPLETE");
        assertThat(suggestion.getBlockedReason()).isEqualTo("执行计划状态、来源或边界信息不完整");
        assertThat(suggestion.getDirection()).isNull();
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getValidFrom()).isNull();
        assertThat(suggestion.getExpiresAt()).isNull();
    }

    @Test
    void offsetAwarePlanExpiryIsTimezoneIndependent() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            for (String timeZone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
                DecisionResultVO decision = completePlanDecision("BTCUSDT", null);
                decision.setValidFrom(OffsetDateTime.parse("2026-07-01T08:00:00+08:00"));
                decision.setExpiresAt(OffsetDateTime.parse("2026-07-02T08:00:00+08:00"));
                allowMatchingSnapshot(decision);
                when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

                DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                        .getExecutionSuggestion();

                assertThat(suggestion.getStatus()).as(timeZone).isEqualTo("USABLE_REVIEW_PLAN");
                assertThat(suggestion.getValidFrom().toInstant()).as(timeZone)
                        .isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
                assertThat(suggestion.getExpiresAt().toInstant()).as(timeZone)
                        .isEqualTo(Instant.parse("2026-07-02T00:00:00Z"));
            }
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    void conflictFallbackUsesDirectionalBlockThreshold() {
        LightSystemStatusVO statusWithoutAggregate = new LightSystemStatusVO();
        when(decisionService.getLightSystemStatus()).thenReturn(statusWithoutAggregate);
        List<DecisionResultVO> decisions = List.of(
                decision("BTCUSDT", "WAIT", "LOW", "LOW", 80, 1, null, false, null),
                decision("ETHUSDT", "WAIT", "LOW", "LOW", 80, 69, null, false, null),
                decision("SOLUSDT", "WAIT", "LOW", "LOW", 80, 70, null, false, null),
                decision("BNBUSDT", "WAIT", "LOW", "LOW", 80, 84, null, false, null),
                decision("XRPUSDT", "WAIT", "LOW", "LOW", 80, 85, null, false, null));
        decisions.get(0).setConfusedScore(1);
        decisions.get(1).setConfusedScore(69);
        decisions.get(2).setConfusedScore(70);
        decisions.get(3).setConfusedScore(84);
        decisions.get(4).setConfusedScore(85);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(decisions);

        DashboardHomeVO.StatusCardVO thresholdFallback = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getSystemState().getConfused();
        assertThat(thresholdFallback.getValue()).isEqualTo(1);

        when(decisionService.getLightSystemStatus()).thenThrow(new IllegalStateException("status unavailable"));
        DashboardHomeVO.StatusCardVO unavailable = service.getHomeForUser(USER_ID, "BTCUSDT", 6)
                .getSystemState().getConfused();
        assertThat(unavailable.getValue()).isNull();
        assertThat(unavailable.getStatus()).isEqualTo("WAITING_SYNC");
    }

    @Test
    void mismatchedAssetStateTraceBlocksPlan() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        AssetStateDO state = new AssetStateDO();
        state.setSymbol("BTCUSDT");
        state.setState(AssetStateEnum.CANDIDATE);
        state.setTraceId("trace-current-state");
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(decision.getAnalysisId());
        run.setTraceId("trace-original-plan");
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(state);
        when(analysisRunMapper.selectById(decision.getAnalysisId())).thenReturn(run);
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHomeForUser(USER_ID, "BTCUSDT", 6);

        assertThat(asset(home, "BTC/USDT").getAssetStateLabel()).isEqualTo("候选");
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("STATE_SNAPSHOT_MISMATCH");
        assertThat(home.getExecutionSuggestion().getBlockedReason())
                .isEqualTo("状态已更新，原计划需重新分析");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNull();
        assertThat(home.getExecutionSuggestion().getInvalidCondition()).isNull();
    }

    @Test
    void noFakeAiEvidenceOrProviderCalls() {
        assertThat(Arrays.stream(DashboardHomeServiceImpl.class.getDeclaredFields())
                .map(field -> field.getType().getSimpleName()))
                .doesNotContain("OpenAiClient", "OpenAIClient", "GeminiClient", "GrokClient", "XaiClient");
        assertThat(Arrays.stream(DashboardHomeServiceImpl.class.getDeclaredConstructors())
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .map(Class::getSimpleName))
                .doesNotContain("OpenAiClient", "OpenAIClient", "GeminiClient", "GrokClient", "XaiClient");
    }

    private DecisionResultVO decisionWithStructuredAiRoles() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults(structuredAiRoleResults(List.of(
                        role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                                AiReviewStance.SUPPORT, "GPT_SUPPORT_ONLY", "GPT summary"),
                        role(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                                AiReviewStance.CHALLENGE, "GEMINI_CONTRADICTION_ONLY", "Gemini review summary"),
                        role(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                                AiReviewStance.CHALLENGE, "GROK_COUNTER_ONLY", "Grok challenge summary")),
                synthesis("BULLISH", "HIGH", "HIGH", "PREPARE_ONLY", true,
                        "GEMINI_CONTRADICTION_ONLY")));
        return decision;
    }

    private DecisionResultVO decisionWithRoles(List<AiProviderReviewResult> roles) {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults(structuredAiRoleResults(roles,
                synthesis("BULLISH", "HIGH", "HIGH", "PREPARE_ONLY", true, null)));
        return decision;
    }

    private DecisionResultVO allAbstainDecision() {
        return decisionWithRoles(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "证据不足"),
                role(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "证据不足"),
                role(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "证据不足")));
    }

    private void assertConsistencyNotApplicable(DashboardHomeVO home) {
        DashboardHomeVO.ConsistencyVO consistency = home.getAiDecision().getConsistency();
        assertThat(consistency.getDataState()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(consistency.getConflictLevel()).isNull();
        assertThat(consistency.getFinalMarketBias()).isNull();
        assertThat(consistency.getFinalPlanMode()).isNull();
        assertThat(consistency.getLevel()).isNull();
        assertThat(consistency.getScore()).isNull();
        assertThat(consistency.getConsistencyLevel()).isNull();
        assertThat(consistency.getConsistencySummary()).isNull();
        assertThat(home.getSystemState().getAiConflict().getValue()).isNull();
        assertThat(home.getSystemState().getAiConflict().getValueLabel()).isEqualTo("不适用");
    }

    private DecisionResultVO completePlanDecision(String symbol, String validPeriod) {
        DecisionResultVO decision = decision(symbol, "BULLISH", "HIGH", "MEDIUM", 85, 10,
                "LEVEL_1", true, "{\"state\":\"CANDIDATE\"}");
        decision.setEntryZone("100-105");
        decision.setStopLoss("95");
        decision.setTakeProfitRules("110 / 115");
        decision.setLeverageSuggestion("2x");
        decision.setPositionSuggestion("人工复核仓位");
        decision.setValidPeriod(validPeriod);
        decision.setInvalidCondition("结构失效");
        return decision;
    }

    private void assertAssetExecutionPlan(DashboardHomeVO.ExecutionSuggestionVO suggestion,
                                          DecisionResultVO decision,
                                          String planId) {
        assertThat(suggestion.getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
        assertThat(suggestion.getPositionMode()).isFalse();
        assertThat(suggestion.getPositionMonitor()).isNull();
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo(decision.getAnalysisId());
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo(planId);
        assertThat(suggestion.getFinalPlanMode()).isEqualTo("CONFIRMATION");
        assertThat(suggestion.getWorthOpening()).isTrue();
        assertThat(suggestion.getEntryZone()).isEqualTo(decision.getEntryZone());
        assertThat(suggestion.getStopLoss()).isEqualTo(decision.getStopLoss());
        assertThat(suggestion.getTakeProfitRules()).isEqualTo(decision.getTakeProfitRules());
    }

    private void assertUnavailableAssetExecutionPlan(DashboardHomeVO.ExecutionSuggestionVO suggestion,
                                                      String expectedStatus) {
        assertThat(suggestion.getStatus()).isEqualTo(expectedStatus);
        assertThat(suggestion.getStatus()).isNotEqualTo("USABLE_REVIEW_PLAN");
        assertThat(suggestion.getStatusLabel()).isNotBlank();
        assertThat(suggestion.getBlockedReason()).isNotBlank();
        assertThat(suggestion.getPositionMode()).isFalse();
        assertThat(suggestion.getPositionMonitor()).isNull();
        assertThat(suggestion.getSourceExecutionPlanId()).isNull();
        assertThat(suggestion.getSourceTraceId()).isNull();
        assertThat(suggestion.getDirection()).isNull();
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getStopLoss()).isNull();
        assertThat(suggestion.getTakeProfitRules()).isNull();
        assertThat(suggestion.getLeverageSuggestion()).isNull();
        assertThat(suggestion.getPositionSuggestion()).isNull();
        assertThat(suggestion.getValidPeriod()).isNull();
        assertThat(suggestion.getValidFrom()).isNull();
        assertThat(suggestion.getExpiresAt()).isNull();
        assertThat(suggestion.getInvalidCondition()).isNull();
    }

    private void setActivePlanValidity(DecisionResultVO decision) {
        decision.setValidFrom(OffsetDateTime.parse("2026-07-01T00:00:00Z"));
        decision.setExpiresAt(OffsetDateTime.parse("2026-07-02T00:00:00Z"));
    }

    private void assertNonSuccessfulAiRole(AiProviderCallStatus status, String expectedMessage) {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        AiProviderReviewResult role = role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "不应展示的业务结论");
        role.setCallStatus(status);
        decision.setAiRoleResults(structuredAiRoleResults(List.of(role),
                synthesis("BULLISH", "HIGH", "HIGH", "PREPARE_ONLY", true, "EVENT_WINDOW_REVIEW")));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.AiTabVO tab = aiTab(service.getHomeForUser(USER_ID, "BTCUSDT", 6), "GPT_FINAL");

        assertThat(tab.getRunStatus()).isEqualTo(status.name());
        assertThat(tab.getResultAvailable()).isFalse();
        assertThat(tab.getStatusMessage()).isEqualTo(expectedMessage);
        assertThat(tab.getStance()).isNull();
        assertThat(tab.getDirection()).isNull();
        assertThat(tab.getFinalMarketBias()).isNull();
        assertThat(tab.getFinalConfidence()).isNull();
        assertThat(tab.getFinalRiskLevel()).isNull();
        assertThat(tab.getFinalPlanMode()).isNull();
        assertThat(tab.getWorthOpening()).isNull();
        assertThat(tab.getReviewConclusion()).isNull();
        assertThat(tab.getSupportEvidence()).isEmpty();
        assertThat(tab.getAgainstEvidence()).isEmpty();
    }

    private UserPositionVO activeManualPosition(Long id, String symbol, String sourceRefId) {
        UserPositionVO position = new UserPositionVO();
        position.setId(id);
        position.setAssetSymbol(symbol);
        position.setSide("LONG");
        position.setStatus("OPEN");
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        position.setLeverage(BigDecimal.ONE);
        position.setSourceType("MANUAL_INDEPENDENT");
        position.setSourceRefId(sourceRefId);
        return position;
    }

    private PersistedOhlcvBarDO persistedBar(String symbol,
                                             String closePrice,
                                             String freshness,
                                             LocalDateTime updatedAt) {
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setSymbol(symbol);
        bar.setTimeframe("5m");
        bar.setClosePrice(new BigDecimal(closePrice));
        bar.setProvider("BINANCE_PUBLIC");
        bar.setFreshnessStatus(freshness);
        bar.setUpdatedAt(updatedAt);
        return bar;
    }

    private void stubCompleteAssetProjection(DecisionResultVO decision) {
        AnalysisRunDO run = analysisRun(decision.getAnalysisId(), decision.getSymbol());
        when(analysisRunMapper.selectById(decision.getAnalysisId())).thenReturn(run);
        when(analysisRunMapper.selectAverageScoreByAnalysisId(decision.getAnalysisId())).thenReturn(86.4);
        when(analysisRunMapper.countEvidenceByAnalysisId(decision.getAnalysisId())).thenReturn(7);
        when(assetStateMapper.selectBySymbol(decision.getSymbol()))
                .thenReturn(sourceState(decision.getSymbol(), null));
        PersistedOhlcvBarDO marketBar = persistedBar(
                decision.getSymbol(), "64123.45", "FRESH", LocalDateTime.of(2026, 7, 21, 9, 30));
        when(persistedOhlcvBarMapper.selectLatestClosedWindow(
                eq(decision.getSymbol()), anyString(), eq(1)))
                .thenReturn(List.of(marketBar));
    }

    private PositionMonitorLogDTO positionMonitor(Long positionId,
                                                   String logicStatus,
                                                   String riskLevel,
                                                   int minute) {
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(positionId);
        monitor.setCurrentPrice(new BigDecimal("105"));
        String conclusion = "HIGH_RISK".equals(logicStatus) ? "HIGH_RISK_OBSERVATION" : logicStatus;
        String action = switch (conclusion) {
            case "PLAN_INVALIDATED" -> "WAIT_CONFIRMATION";
            case "HIGH_RISK_OBSERVATION" -> "REDUCE_POSITION";
            case "LOGIC_WEAKENED" -> "NO_ADD_POSITION";
            default -> "CONTINUE_HOLD";
        };
        completeTrustedMonitor(monitor, conclusion, riskLevel, action);
        monitor.setCreatedAt(LocalDateTime.of(2026, 7, 20, 10, minute));
        return monitor;
    }

    private static void completeTrustedMonitor(PositionMonitorLogDTO monitor,
                                               String conclusion,
                                               String riskLevel,
                                               String suggestedAction) {
        monitor.setMarkPriceSource("TEST");
        monitor.setEntryLogicStatus("PLAN_INVALIDATED".equals(conclusion) ? "INVALIDATED"
                : "LOGIC_WEAKENED".equals(conclusion) ? "WEAKENED" : "STILL_VALID");
        monitor.setMonitorConclusion(conclusion);
        monitor.setReversalStatus("NO_REVERSAL");
        monitor.setRiskChangeReason("NO_CLEAR_RISK_FACTOR");
        monitor.setRiskLevel(riskLevel);
        monitor.setRiskTrend("STABLE");
        monitor.setSuggestedAction(suggestedAction);
        monitor.setMonitorSourceStatus("VERIFIED");
        monitor.setObservedAt(LocalDateTime.of(2026, 7, 1, 11, 59));
        monitor.setFreshUntil(LocalDateTime.of(2026, 7, 1, 12, 30));
        if (monitor.getCreatedAt() == null) {
            monitor.setCreatedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        }
    }

    private static void assertWaitingMonitorData(DashboardHomeVO.PositionVO row) {
        assertThat(row.getDataState()).isEqualTo("WAITING_MONITOR_DATA");
        assertThat(row.getMarkPrice()).isNull();
        assertThat(row.getCurrentPrice()).isNull();
        assertThat(row.getMarkPriceSource()).isNull();
        assertThat(row.getMarkPriceObservedAt()).isNull();
        assertThat(row.getMarkPriceFresh()).isFalse();
        assertThat(row.getPnlAmount()).isNull();
        assertThat(row.getPnlPercent()).isNull();
        assertThat(row.getRiskLevel()).isNull();
        assertThat(row.getMonitorConclusion()).isNull();
        assertThat(row.getSuggestedAction()).isNull();
        assertThat(row.getEntryLogicStatus()).isNull();
        assertThat(row.getReversalStatus()).isNull();
        assertThat(row.getRiskReason()).isNull();
    }

    private void wireCapturedMonitorLogs(long positionId, List<PositionMonitorLogDTO> capturedLogs) {
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, positionId, 1))
                .thenAnswer(invocation -> capturedLogs.isEmpty()
                        ? List.of()
                        : List.of(capturedLogs.get(capturedLogs.size() - 1)));
        when(positionMonitorLogService.recordMonitorRunForUser(eq(USER_ID), any())).thenAnswer(invocation -> {
            RecordPositionMonitorLogCommand command = invocation.getArgument(1);
            PositionMonitorLogDTO log = new PositionMonitorLogDTO();
            log.setLogId((long) capturedLogs.size() + 1);
            log.setPositionId(command.getPositionId());
            log.setAnalysisId(command.getAnalysisId());
            log.setExecutionPlanId(command.getExecutionPlanId());
            log.setCurrentPrice(command.getCurrentPrice());
            log.setMarkPriceSource(command.getMarkPriceSource());
            log.setEntryLogicStatus(command.getEntryLogicStatus());
            log.setMonitorConclusion(command.getMonitorConclusion());
            log.setReversalStatus(command.getReversalStatus());
            log.setRiskChangeReason(command.getRiskChangeReason());
            log.setRiskLevel(command.getRiskLevel());
            log.setSuggestedAction(command.getSuggestedAction());
            log.setMonitorSourceStatus(command.getMonitorSourceStatus());
            log.setObservedAt(command.getObservedAt());
            log.setFreshUntil(command.getFreshUntil());
            log.setTraceId(command.getTraceId());
            log.setCreatedAt(LocalDateTime.of(2026, 7, 1, 11, 0));
            capturedLogs.add(log);
            return log;
        });
    }

    private UserPositionDO monitorPosition(long positionId, String sourceRefId) {
        UserPositionDO position = new UserPositionDO();
        position.setId(positionId);
        position.setAssetSymbol("BTCUSDT");
        position.setSide("LONG");
        position.setStatus("OPEN");
        position.setEntryPrice(new BigDecimal("100"));
        position.setQuantity(BigDecimal.ONE);
        position.setLeverage(BigDecimal.ONE);
        position.setStopLoss(new BigDecimal("90"));
        position.setTakeProfit(new BigDecimal("120"));
        position.setSourceType("MANUAL_INDEPENDENT");
        position.setSourceRefId(sourceRefId);
        return position;
    }

    private MarketQuoteSnapshot monitorQuote(String price) {
        MarketQuoteSnapshot quote = new MarketQuoteSnapshot();
        quote.setProvider("fixture");
        quote.setSymbolNormalized("BTCUSDT");
        quote.setLastPrice(new BigDecimal(price));
        quote.setFetchedAtEpochMillis(System.currentTimeMillis());
        return quote;
    }

    private UserPositionRiskResult allowedMonitorRisk() {
        UserPositionRiskResult risk = new UserPositionRiskResult();
        risk.setRiskStatus("RISK_ALLOWED");
        risk.setRiskLevel("LOW");
        risk.setRiskBlocked(false);
        risk.setReasonCodes(List.of("RISK_ALLOWED"));
        return risk;
    }

    private AnalysisRunDO sourceRun(String analysisId, String symbol, String traceId) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        run.setTraceId(traceId);
        return run;
    }

    private AssetStateDO sourceState(String symbol, String traceId) {
        AssetStateDO state = new AssetStateDO();
        state.setSymbol(symbol);
        state.setState(AssetStateEnum.CANDIDATE);
        state.setTraceId(traceId);
        return state;
    }

    private DecisionResultVO sourcePlanDecision(String analysisId, String symbol, String marker) {
        DecisionResultVO decision = completePlanDecision(symbol, ACTIVE_VALID_PERIOD);
        decision.setAnalysisId(analysisId);
        decision.setEntryZone(marker + "-entry");
        decision.setStopLoss(marker + "-stop");
        decision.setTakeProfitRules(marker + "-tp");
        decision.setLeverageSuggestion(marker + "-leverage");
        decision.setPositionSuggestion(marker + "-position");
        decision.setInvalidCondition(marker + "-invalid");
        setActivePlanValidity(decision);
        return decision;
    }

    private ExecutionPlanDO stubMonitorExecutionPlanSource(UserPositionVO position,
                                                           DecisionResultVO sourceDecision,
                                                           String planId,
                                                           String sourceTraceId,
                                                           String currentStateTraceId) {
        position.setSourceRefId(PositionMonitorSourceContract.executionPlanReference(planId));
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setAnalysisId(sourceDecision.getAnalysisId());
        monitor.setExecutionPlanId(planId);
        monitor.setCurrentPrice(new BigDecimal("100"));
        completeTrustedMonitor(monitor, "LOGIC_VALID", "LOW", "CONTINUE_HOLD");
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1)).thenReturn(List.of(monitor));
        return stubResolvedOriginalPlan(sourceDecision, planId, sourceTraceId, currentStateTraceId, false);
    }

    private ExecutionPlanDO stubMonitorAnalysisSource(UserPositionVO position,
                                                      DecisionResultVO sourceDecision,
                                                      String planId,
                                                      String sourceTraceId,
                                                      String currentStateTraceId) {
        position.setSourceRefId(PositionMonitorSourceContract.analysisReference(sourceDecision.getAnalysisId()));
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setAnalysisId(sourceDecision.getAnalysisId());
        monitor.setExecutionPlanId(planId);
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1)).thenReturn(List.of(monitor));
        return stubResolvedOriginalPlan(sourceDecision, planId, sourceTraceId, currentStateTraceId, false);
    }

    private ExecutionPlanDO stubResolvedOriginalPlan(DecisionResultVO sourceDecision,
                                                     String planId,
                                                     String sourceTraceId,
                                                     String currentStateTraceId,
                                                     boolean resolveByAnalysisId) {
        ExecutionPlanDO plan = validExecutionPlan(planId, sourceDecision.getAnalysisId());
        copyExactPlanFields(plan, sourceDecision);
        if (resolveByAnalysisId) {
            when(executionPlanMapper.selectOnlyByAnalysisId(sourceDecision.getAnalysisId())).thenReturn(plan);
        } else {
            when(executionPlanMapper.selectByPlanId(planId)).thenReturn(plan);
        }
        lenient().when(executionPlanMapper.selectLatestByAnalysisId(sourceDecision.getAnalysisId()))
                .thenReturn(plan);
        allowExactAssetPlanRelation(sourceDecision, plan);
        lenient().when(decisionResultMapper.findByAnalysisIdAndPlanIdJoined(sourceDecision.getAnalysisId(), planId))
                .thenReturn(sourceDecision);

        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(sourceDecision.getAnalysisId());
        run.setSymbol(sourceDecision.getSymbol());
        run.setTraceId(sourceTraceId);
        when(analysisRunMapper.selectById(sourceDecision.getAnalysisId())).thenReturn(run);

        AssetStateDO state = new AssetStateDO();
        state.setSymbol(sourceDecision.getSymbol());
        state.setState(AssetStateEnum.CANDIDATE);
        state.setTraceId(currentStateTraceId);
        lenient().when(assetStateMapper.selectBySymbol(sourceDecision.getSymbol())).thenReturn(state);
        return plan;
    }

    private PositionMonitorLogDTO analysisOnlyMonitor(UserPositionVO position, String analysisId) {
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setAnalysisId(analysisId);
        return monitor;
    }

    private ExecutionPlanDO validExecutionPlan(String planId, String analysisId) {
        ExecutionPlanDO plan = FrozenFinalExecutionPlanTestFixture.complete(
                planId, analysisId, LocalDateTime.of(2026, 7, 1, 12, 0));
        plan.setExecutionFeasibilityFreshUntil(LocalDateTime.of(2026, 7, 3, 11, 59));
        return plan;
    }

    private void copyExactPlanFields(ExecutionPlanDO plan, DecisionResultVO decision) {
        plan.setRuleMarketBias(decision.getMarketBiasHierarchy());
        plan.setFinalMarketBias(decision.getMarketBiasHierarchy());
        plan.setFinalPlanMode("CONFIRMATION");
        plan.setEntryZone(decision.getEntryZone());
        plan.setStopLoss(decision.getStopLoss());
        plan.setTakeProfitRules(decision.getTakeProfitRules());
        plan.setLeverageSuggestion(decision.getLeverageSuggestion());
        plan.setLeverageLimit(decision.getLeverageSuggestion());
        plan.setPositionSuggestion(decision.getPositionSuggestion());
        plan.setPositionLimit(decision.getPositionSuggestion());
        plan.setInvalidCondition(decision.getInvalidCondition());
        if (decision.getValidFrom() != null) {
            plan.setValidFrom(decision.getValidFrom().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }
        if (decision.getExpiresAt() != null) {
            plan.setValidUntil(decision.getExpiresAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }
    }

    private void assertMonitorDashboardPlanStateAgreement(
            long positionId,
            java.util.function.Consumer<ExecutionPlanDO> planMutation,
            String expectedReasonCode,
            String expectedDashboardValidity) {
        String analysisId = "analysis-agreement-" + positionId;
        String planId = "plan-agreement-" + positionId;
        String traceId = "trace-agreement-" + positionId;
        List<PositionMonitorLogDTO> capturedLogs = new ArrayList<>();
        wireCapturedMonitorLogs(positionId, capturedLogs);

        UserPositionDO positionDO = monitorPosition(positionId,
                PositionMonitorSourceContract.executionPlanReference(planId));
        UserPositionVO positionVO = activeManualPosition(positionId, "BTCUSDT", positionDO.getSourceRefId());
        UserPositionMapper monitorPositionMapper = mock(UserPositionMapper.class);
        MarketQuoteClient quoteClient = mock(MarketQuoteClient.class);
        UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        when(monitorPositionMapper.selectByIdAndUserId(positionId, USER_ID)).thenReturn(positionDO);
        when(quoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(monitorQuote("100")));
        when(riskAdapter.currentRiskForUser(USER_ID)).thenReturn(allowedMonitorRisk());

        DecisionResultVO sourceDecision = sourcePlanDecision(analysisId, "BTCUSDT", "AGREEMENT-A");
        ExecutionPlanDO plan = validExecutionPlan(planId, analysisId);
        copyExactPlanFields(plan, sourceDecision);
        planMutation.accept(plan);
        lenient().when(executionPlanMapper.selectByPlanId(planId)).thenReturn(plan);
        when(analysisRunMapper.selectById(analysisId))
                .thenReturn(sourceRun(analysisId, "BTCUSDT", traceId));

        PositionMonitorServiceImpl monitorService = new PositionMonitorServiceImpl(
                monitorPositionMapper,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(quoteClient),
                riskAdapter,
                executionPlanMapper,
                positionMonitorLogService,
                mock(EvidenceItemMapper.class),
                mock(ScoreItemMapper.class),
                decisionResultMapper,
                new ObjectMapper(),
                analysisRunMapper,
                null);

        PositionMonitorResultDTO monitorResult = monitorService.monitorUserPositionForUser(positionId, USER_ID);

        assertThat(monitorResult.getMonitorConclusion()).isEqualTo("LOGIC_WEAKENED");
        assertThat(monitorResult.getSuggestedAction()).isEqualTo("NO_ADD_POSITION");
        assertThat(monitorResult.getReasonCodes()).contains(expectedReasonCode);
        assertThat(capturedLogs).singleElement().satisfies(log -> {
            assertThat(log.getMonitorConclusion()).isEqualTo("LOGIC_WEAKENED");
            assertThat(log.getSuggestedAction()).isEqualTo("NO_ADD_POSITION");
            assertThat(log.getAnalysisId()).isEqualTo(analysisId);
            assertThat(log.getExecutionPlanId()).isEqualTo(planId);
        });

        when(decisionResultMapper.findByAnalysisIdAndPlanIdJoined(analysisId, planId))
                .thenReturn(sourceDecision);
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(positionVO));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(sourceDecision));
        when(assetStateMapper.selectBySymbol("BTCUSDT"))
                .thenReturn(sourceState("BTCUSDT", traceId));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHomeForUser(USER_ID, "BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getStatus()).isEqualTo("POSITION_MONITORING");
        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo(expectedDashboardValidity);
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo(analysisId);
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo(planId);
    }

    private OriginalPlanFixture originalPlanFixture(Long positionId, String marker) {
        UserPositionVO position = activeManualPosition(positionId, "BTCUSDT", null);
        DecisionResultVO sourceDecision = sourcePlanDecision(
                "analysis-source-" + positionId, "BTCUSDT", marker);
        ExecutionPlanDO plan = stubMonitorExecutionPlanSource(
                position, sourceDecision, "plan-source-" + positionId,
                "trace-source-" + positionId, "trace-source-" + positionId);
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(position));
        return new OriginalPlanFixture(position, plan, sourceDecision);
    }

    private OriginalPlanFixture originalPlanFixtureWithLatestB(Long positionId, String marker) {
        OriginalPlanFixture fixture = originalPlanFixture(positionId, marker);
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "LATEST-B");
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(latestB));
        return fixture;
    }

    private MultiPositionFixture twoSameSymbolPositions() {
        UserPositionVO positionA = activeManualPosition(3411L, "BTCUSDT", null);
        UserPositionVO positionB = activeManualPosition(3412L, "BTCUSDT", null);
        DecisionResultVO planA = sourcePlanDecision("analysis-position-A", "BTCUSDT", "POSITION-A");
        DecisionResultVO planB = sourcePlanDecision("analysis-position-B", "BTCUSDT", "POSITION-B");
        stubMonitorExecutionPlanSource(positionA, planA, "plan-position-A", "trace-shared", "trace-shared");
        stubMonitorExecutionPlanSource(positionB, planB, "plan-position-B", "trace-shared", "trace-shared");
        when(userPositionService.listOpenPositionsForUser(USER_ID)).thenReturn(List.of(positionA, positionB));
        when(decisionService.getLatestDecisionResultsForUser(eq(USER_ID), anyInt())).thenReturn(List.of(planB));
        return new MultiPositionFixture(positionA, positionB, planA, planB);
    }

    private void assertPositionSelectionBlocked(DashboardHomeVO.ExecutionSuggestionVO suggestion,
                                                String status,
                                                String statusLabel) {
        assertThat(suggestion.getStatus()).isEqualTo(status);
        assertThat(suggestion.getStatusLabel()).isEqualTo(statusLabel);
        assertThat(suggestion.getPositionMode()).isFalse();
        assertThat(suggestion.getPositionMonitor()).isNull();
        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("UNVERIFIED");
        assertThat(suggestion.getDirection()).isNull();
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getStopLoss()).isNull();
        assertThat(suggestion.getTakeProfitRules()).isNull();
        assertThat(suggestion.getSourceAnalysisId()).isNull();
        assertThat(suggestion.getSourceExecutionPlanId()).isNull();
        assertThat(suggestion.getSourceTraceId()).isNull();
    }

    private void assertVerifiedHistoricalPlan(DashboardHomeVO.ExecutionSuggestionVO suggestion,
                                              String expectedValidity,
                                              String expectedLabel,
                                              String marker) {
        assertThat(suggestion.getStatus()).isEqualTo("POSITION_MONITORING");
        assertThat(suggestion.getPositionMode()).isTrue();
        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo(expectedValidity);
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isNotEqualTo("ACTIVE");
        assertThat(suggestion.getOriginalPlanLabel()).isEqualTo(expectedLabel);
        assertThat(suggestion.getEntryZone()).isEqualTo(marker + "-entry");
        assertThat(suggestion.getStopLoss()).isEqualTo(marker + "-stop");
        assertThat(suggestion.getTakeProfitRules()).isEqualTo(marker + "-tp");
        assertThat(suggestion.getSourceAnalysisId()).isNotBlank();
        assertThat(suggestion.getSourceExecutionPlanId()).isNotBlank();
        assertThat(suggestion.getStatus()).isNotEqualTo("USABLE_REVIEW_PLAN");
    }

    private void assertUnverifiedOriginalPlan(DashboardHomeVO.ExecutionSuggestionVO suggestion) {
        assertThat(suggestion.getStatus()).isEqualTo("POSITION_MONITORING");
        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("UNVERIFIED");
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("UNVERIFIED");
        assertThat(suggestion.getOriginalPlanLabel()).isEqualTo("暂无可关联的原执行计划");
        assertThat(suggestion.getDirection()).isNull();
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getStopLoss()).isNull();
        assertThat(suggestion.getTakeProfitRules()).isNull();
        assertThat(suggestion.getLeverageSuggestion()).isNull();
        assertThat(suggestion.getPositionSuggestion()).isNull();
        assertThat(suggestion.getValidPeriod()).isNull();
        assertThat(suggestion.getValidFrom()).isNull();
        assertThat(suggestion.getExpiresAt()).isNull();
        assertThat(suggestion.getInvalidCondition()).isNull();
        assertThat(suggestion.getSourceAnalysisId()).isNull();
        assertThat(suggestion.getSourceExecutionPlanId()).isNull();
        assertThat(suggestion.getSourceTraceId()).isNull();
    }

    private ExecutionPlanDO allowMatchingSnapshot(DecisionResultVO decision) {
        String traceId = "trace-" + decision.getAnalysisId();
        AssetStateDO state = new AssetStateDO();
        state.setSymbol(decision.getSymbol());
        state.setState(AssetStateEnum.CANDIDATE);
        state.setTraceId(traceId);
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(decision.getAnalysisId());
        run.setSymbol(decision.getSymbol());
        run.setTraceId(traceId);
        when(assetStateMapper.selectBySymbol(decision.getSymbol())).thenReturn(state);
        when(analysisRunMapper.selectById(decision.getAnalysisId())).thenReturn(run);
        return allowAssetExecutionPlan(decision, "plan-" + decision.getAnalysisId());
    }

    private ExecutionPlanDO allowAssetExecutionPlan(DecisionResultVO decision, String planId) {
        ExecutionPlanDO plan = validExecutionPlan(planId, decision.getAnalysisId());
        copyExactPlanFields(plan, decision);
        allowExactAssetPlanRelation(decision, plan);
        return plan;
    }

    private void allowExactAssetPlanRelation(DecisionResultVO decision, ExecutionPlanDO plan) {
        OpportunityLogDTO relation = new OpportunityLogDTO();
        relation.setAnalysisId(decision.getAnalysisId());
        relation.setDecisionId(decision.getDecisionId());
        relation.setExecutionPlanId(plan.getPlanId());
        relation.setSymbol(decision.getSymbol());
        relation.setSourceType("AUTHORITATIVE_ANALYSIS");
        relation.setTraceId("trace-" + decision.getAnalysisId());
        lenient().when(opportunityLogService.queryForSystem(
                        decision.getAnalysisId(), decision.getDecisionId(), null, decision.getSymbol(),
                        null, null, null, null, 2))
                .thenReturn(List.of(relation));
        lenient().when(executionPlanMapper.selectByPlanId(plan.getPlanId())).thenReturn(plan);
    }

    private void allowResolvedOriginalPlan(UserPositionVO position,
                                           DecisionResultVO sourceDecision,
                                           String planId,
                                           String traceId) {
        position.setSourceRefId(PositionMonitorSourceContract.executionPlanReference(planId));
        PositionMonitorLogDTO monitor = new PositionMonitorLogDTO();
        monitor.setPositionId(position.getId());
        monitor.setAnalysisId(sourceDecision.getAnalysisId());
        monitor.setExecutionPlanId(planId);
        when(positionMonitorLogService.listByPositionIdForUser(USER_ID, position.getId(), 1)).thenReturn(List.of(monitor));

        ExecutionPlanDO plan = validExecutionPlan(planId, sourceDecision.getAnalysisId());
        copyExactPlanFields(plan, sourceDecision);
        lenient().when(executionPlanMapper.selectByPlanId(planId)).thenReturn(plan);

        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(sourceDecision.getAnalysisId());
        run.setSymbol(sourceDecision.getSymbol());
        run.setTraceId(traceId);
        when(analysisRunMapper.selectById(sourceDecision.getAnalysisId())).thenReturn(run);

        AssetStateDO state = new AssetStateDO();
        state.setSymbol(sourceDecision.getSymbol());
        state.setState(AssetStateEnum.CANDIDATE);
        state.setTraceId(traceId);
        when(assetStateMapper.selectBySymbol(sourceDecision.getSymbol())).thenReturn(state);
    }

    private record OriginalPlanFixture(UserPositionVO position,
                                       ExecutionPlanDO executionPlan,
                                       DecisionResultVO decision) {
    }

    private record MultiPositionFixture(UserPositionVO positionA,
                                        UserPositionVO positionB,
                                        DecisionResultVO planA,
                                        DecisionResultVO planB) {
    }

    private String structuredAiRoleResults(List<AiProviderReviewResult> roleResults,
                                           AiRoleResultsPayload.SynthesisPayload synthesis) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", AiRoleResultsPayload.CURRENT_SCHEMA_VERSION);
        root.put("analysisId", "analysis-dashboard-ai");
        root.put("traceId", "trace-dashboard-ai");
        root.put("ruleVersion", "v4.1-test-fixture");
        root.put("orchestrationMode", "DECISION_CHAIN_V4_1");
        root.set("orchestrationReasonCodes", objectMapper.createArrayNode());

        ObjectNode roles = objectMapper.createObjectNode();
        for (String roleName : List.of("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE")) {
            AiProviderReviewResult source = roleResults == null ? null : roleResults.stream()
                    .filter(value -> roleName.equals(decisionRoleName(value)))
                    .findFirst()
                    .orElse(null);
            roles.set(roleName, structuredRoleFixture(roleName, source, synthesis));
        }
        root.set("roles", roles);
        root.set("synthesis", objectMapper.valueToTree(synthesis));
        root.set("safety", objectMapper.valueToTree(AiRoleResultsPayload.SafetyBoundary.decisionChainV41()));
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create structured AI fixture", exception);
        }
    }

    private ObjectNode structuredRoleFixture(String roleName,
                                             AiProviderReviewResult source,
                                             AiRoleResultsPayload.SynthesisPayload synthesis) {
        ObjectNode role = objectMapper.createObjectNode();
        role.put("role", roleName);
        role.put("analysisId", "analysis-dashboard-ai");
        role.put("traceId", "trace-dashboard-ai");
        role.put("generatedAt", "2026-07-01T12:00:00Z");
        if (source == null) {
            role.put("callStatus", "NOT_CALLED");
            role.put("roleState", "UNAVAILABLE");
            role.put("dataState", "SOURCE_UNAVAILABLE");
            role.put("fallback", true);
            role.put("fallbackReason", "ROLE_RESULT_UNAVAILABLE");
            initializeRoleCollections(roleName, role, "SOURCE_UNAVAILABLE");
            return role;
        }

        String callStatus = source.getCallStatus() == null ? "FAILED" : source.getCallStatus().name();
        boolean successful = "SUCCESS".equals(callStatus);
        boolean insufficient = source.getStance() == AiReviewStance.ABSTAIN
                || source.getReasonCodes().stream().anyMatch("INSUFFICIENT_DATA"::equals);
        role.put("provider", source.getProvider() == null ? "TEST_FIXTURE" : source.getProvider().name());
        role.put("sourceRole", roleName);
        role.put("callStatus", callStatus);
        role.put("roleState", successful ? "READY" : source.isFallback() ? "FALLBACK" : "ERROR");
        role.put("dataState", successful
                ? insufficient ? "INSUFFICIENT_DATA" : "READY"
                : "TIMEOUT".equals(callStatus) ? "AI_TIMEOUT" : "AI_FAILED");
        if (successful && source.getStance() != null) role.put("stance", source.getStance().name());
        role.set("reasonCodes", objectMapper.valueToTree(source.getReasonCodes()));
        if (source.getSummary() != null) role.put("summary", source.getSummary());
        if (source.isFallback()) role.put("fallback", true);
        if (source.getFallbackReason() != null) role.put("fallbackReason", source.getFallbackReason());
        role.put("manualReviewRequired", true);

        if (!successful) {
            initializeRoleCollections(roleName, role, "SOURCE_UNAVAILABLE");
            return role;
        }
        switch (roleName) {
            case "GPT_FINAL" -> populateGptFixture(role, source, synthesis, insufficient);
            case "GEMINI_REVIEW" -> populateGeminiFixture(role, source, synthesis, insufficient);
            case "GROK_CHALLENGE" -> populateGrokFixture(role, source, synthesis, insufficient);
            default -> throw new IllegalArgumentException("Unknown AI role fixture: " + roleName);
        }
        return role;
    }

    private void populateGptFixture(ObjectNode role,
                                    AiProviderReviewResult source,
                                    AiRoleResultsPayload.SynthesisPayload synthesis,
                                    boolean insufficient) {
        ObjectNode judgment = objectMapper.createObjectNode();
        if (!insufficient) judgment.put("marketBias", synthesis.finalMarketBias());
        judgment.put("opportunityState", "CANDIDATE");
        judgment.put("text", insufficient ? "证据不足，暂不判断" : "AI 复核结果已返回，等待人工复核");
        role.set("coreJudgment", judgment);

        ArrayNode support = objectMapper.createArrayNode();
        if (!insufficient) support.add(evidenceFixture(reasonDisplay(source), "SUPPORTING"));
        role.set("supportingEvidence", support);
        role.put("supportingEvidenceState", insufficient ? "INSUFFICIENT_DATA" : "FOUND");
        role.set("opposingEvidence", objectMapper.createArrayNode());
        role.put("opposingEvidenceState", insufficient ? "INSUFFICIENT_DATA" : "NONE_FOUND");

        ObjectNode timeframes = objectMapper.createObjectNode();
        timeframes.put("4h", "test fixture 4h");
        timeframes.put("1h", "test fixture 1h");
        timeframes.put("15m", "test fixture 15m");
        timeframes.put("5m", "test fixture 5m");
        role.set("multiTimeframeExplanation", timeframes);

        if (!insufficient) {
            ObjectNode adjustment = objectMapper.createObjectNode();
            adjustment.put("before", synthesis.finalMarketBias());
            adjustment.put("after", synthesis.finalMarketBias());
            adjustment.put("reason", synthesis.mainReason() == null ? "UNCHANGED" : synthesis.mainReason());
            role.set("biasAdjustment", adjustment);

            ObjectNode candidate = objectMapper.createObjectNode();
            candidate.put("planMode", synthesis.finalPlanMode());
            candidate.put("confidence", synthesis.finalConfidence());
            candidate.put("riskLevel", synthesis.finalRiskLevel());
            candidate.put("worthOpening", Boolean.TRUE.equals(synthesis.worthOpening()));
            candidate.put("summary", "AI 复核结果已返回，等待人工复核");
            role.set("candidateSummary", candidate);
        }
    }

    private void populateGeminiFixture(ObjectNode role,
                                       AiProviderReviewResult source,
                                       AiRoleResultsPayload.SynthesisPayload synthesis,
                                       boolean insufficient) {
        boolean challenge = source.getStance() == AiReviewStance.CHALLENGE;
        role.set("evidenceGaps", objectMapper.createArrayNode());
        role.put("evidenceGapsState", insufficient ? "INSUFFICIENT_DATA" : "NONE_FOUND");
        ArrayNode conflicts = objectMapper.createArrayNode();
        if (challenge) conflicts.add(findingFixture("AI 发现证据冲突", "LOGIC_CONFLICT"));
        role.set("logicConflicts", conflicts);
        role.put("logicConflictsState", insufficient ? "INSUFFICIENT_DATA" : challenge ? "FOUND" : "NONE_FOUND");
        role.set("underestimatedRisks", objectMapper.createArrayNode());
        role.put("underestimatedRisksState", insufficient ? "INSUFFICIENT_DATA" : "NONE_FOUND");
        if (!insufficient) {
            ObjectNode downgrade = objectMapper.createObjectNode();
            downgrade.put("before", synthesis.finalPlanMode());
            downgrade.put("after", synthesis.finalPlanMode());
            downgrade.put("reason", challenge ? "AI 复核结果已返回，等待人工复核" : "UNCHANGED");
            downgrade.put("recoveryCondition", "NEW_VERIFIED_ANALYSIS");
            role.set("downgradeSuggestion", downgrade);
            role.put("reviewResult", challenge ? "DOWNGRADE" : "APPROVE");
            role.put("finalDirectionImpact", "UNCHANGED");
            role.put("confidenceAdjustment", challenge ? "DOWNGRADE_ONE" : "UNCHANGED");
            role.put("riskAdjustment", challenge ? "RAISED" : "UNCHANGED");
            role.put("planModeAdjustment", challenge ? "DOWNGRADE_ONE" : "UNCHANGED");
            role.put("recoveryCondition", "NEW_VERIFIED_ANALYSIS");
        }
    }

    private void populateGrokFixture(ObjectNode role,
                                     AiProviderReviewResult source,
                                     AiRoleResultsPayload.SynthesisPayload synthesis,
                                     boolean insufficient) {
        boolean challenge = source.getStance() == AiReviewStance.CHALLENGE;
        ArrayNode failurePaths = objectMapper.createArrayNode();
        if (challenge) {
            ObjectNode path = objectMapper.createObjectNode();
            path.put("failurePathId", "test-failure-path");
            path.put("hypothesis", "AI 提供反向证据");
            path.put("triggerCondition", "test fixture trigger");
            path.put("causalPath", "test fixture causal path");
            path.put("observationWindow", "test fixture window");
            path.set("validationIndicators", objectMapper.valueToTree(List.of("test fixture indicator")));
            path.set("sourceRefs", objectMapper.valueToTree(List.of("TEST_FIXTURE_ONLY")));
            path.put("invalidatingEvidence", "test fixture invalidation");
            failurePaths.add(path);
        }
        role.set("failurePaths", failurePaths);
        role.put("failurePathState", insufficient ? "INSUFFICIENT_DATA"
                : challenge ? "FOUND" : "NO_VERIFIABLE_FAILURE_PATH");

        ArrayNode scenarios = objectMapper.createArrayNode();
        if (challenge) scenarios.add(findingFixture("AI 提供反向证据", "OPPOSING_SCENARIO"));
        role.set("opposingScenarios", scenarios);
        role.put("opposingScenariosState", insufficient ? "INSUFFICIENT_DATA" : challenge ? "FOUND" : "NONE_FOUND");
        for (String field : List.of("externalEventRisks", "microstructureRisks", "watchIndicators")) {
            role.set(field, objectMapper.createArrayNode());
            role.put(field + "State", insufficient ? "INSUFFICIENT_DATA" : "NONE_FOUND");
        }
        if (!insufficient) {
            role.put("challengeSummary", "AI 复核结果已返回，等待人工复核");
            role.put("currentDirectionChallenge", "AI 复核结果已返回，等待人工复核");
            role.put("majorCounterEvidence", challenge);
            role.put("conflictLevel", challenge
                    ? "LEVEL_2_MINOR_DISAGREEMENT" : "LEVEL_1_CONSISTENT");
            role.put("riskAdjustment", challenge ? "RAISED" : "UNCHANGED");
            role.put("planModeImpact", challenge ? "DOWNGRADE_ONE" : "UNCHANGED");
        }
    }

    private void initializeRoleCollections(String roleName, ObjectNode role, String state) {
        switch (roleName) {
            case "GPT_FINAL" -> {
                role.set("supportingEvidence", objectMapper.createArrayNode());
                role.put("supportingEvidenceState", state);
                role.set("opposingEvidence", objectMapper.createArrayNode());
                role.put("opposingEvidenceState", state);
            }
            case "GEMINI_REVIEW" -> {
                role.set("evidenceGaps", objectMapper.createArrayNode());
                role.put("evidenceGapsState", state);
                role.set("logicConflicts", objectMapper.createArrayNode());
                role.put("logicConflictsState", state);
                role.set("underestimatedRisks", objectMapper.createArrayNode());
                role.put("underestimatedRisksState", state);
            }
            case "GROK_CHALLENGE" -> {
                role.set("failurePaths", objectMapper.createArrayNode());
                role.put("failurePathState", state);
                role.set("opposingScenarios", objectMapper.createArrayNode());
                role.put("opposingScenariosState", state);
                role.set("externalEventRisks", objectMapper.createArrayNode());
                role.put("externalEventRisksState", state);
                role.set("microstructureRisks", objectMapper.createArrayNode());
                role.put("microstructureRisksState", state);
                role.set("watchIndicators", objectMapper.createArrayNode());
                role.put("watchIndicatorsState", state);
            }
            default -> throw new IllegalArgumentException("Unknown AI role fixture: " + roleName);
        }
    }

    private ObjectNode evidenceFixture(String value, String type) {
        ObjectNode evidence = objectMapper.createObjectNode();
        evidence.put("evidenceId", "test-evidence-1");
        evidence.put("type", type);
        evidence.put("source", "TEST_FIXTURE_ONLY");
        evidence.put("currentValue", value);
        evidence.put("change", "TEST_FIXTURE_ONLY");
        evidence.put("direction", "UNCHANGED");
        evidence.put("strength", 60.0);
        evidence.put("confidence", 60.0);
        evidence.put("observedAt", "2026-07-01T12:00:00Z");
        evidence.put("freshness", "FRESH");
        evidence.put("analysisId", "analysis-dashboard-ai");
        return evidence;
    }

    private ObjectNode findingFixture(String text, String category) {
        ObjectNode finding = objectMapper.createObjectNode();
        finding.put("findingId", "test-finding-1");
        finding.put("category", category);
        finding.put("text", text);
        finding.put("impact", "TEST_FIXTURE_ONLY");
        finding.set("evidenceRefs", objectMapper.valueToTree(List.of("test-evidence-1")));
        return finding;
    }

    private String reasonDisplay(AiProviderReviewResult source) {
        String reason = source.getReasonCodes().isEmpty() ? null : source.getReasonCodes().get(0);
        return switch (reason == null ? "" : reason) {
            case "INSUFFICIENT_DATA" -> "证据不足";
            case "RULE_DIRECTION_ALIGNED" -> "规则方向与 AI 复核一致";
            default -> "AI 证据已记录，需人工复核";
        };
    }

    private static String decisionRoleName(AiProviderReviewResult source) {
        if (source == null || source.getRole() == null) return null;
        return switch (source.getRole()) {
            case GPT_RULE_REVIEW -> "GPT_FINAL";
            case GEMINI_CONSISTENCY_REVIEW -> "GEMINI_REVIEW";
            case GROK_ADVERSARIAL_CHALLENGE -> "GROK_CHALLENGE";
        };
    }

    private AiProviderReviewResult role(AiProviderName provider,
                                        AiProviderRole providerRole,
                                        AiReviewStance stance,
                                        String reasonCode,
                                        String summary) {
        AiProviderReviewResult role = new AiProviderReviewResult();
        role.setProvider(provider);
        role.setRole(providerRole);
        role.setCallStatus(AiProviderCallStatus.SUCCESS);
        role.setStance(stance);
        role.setConflictLevel(stance == AiReviewStance.CHALLENGE
                ? AiReviewConflictLevel.MAJOR
                : AiReviewConflictLevel.NONE);
        role.setReasonCodes(List.of(reasonCode));
        role.setSummary(summary);
        return role;
    }

    private AiRoleResultsPayload.SynthesisPayload synthesis(String direction,
                                                            String confidence,
                                                            String risk,
                                                            String planMode,
                                                            Boolean worthOpening,
                                                            String downgradeReason) {
        String normalizedPlanMode = switch (planMode == null ? "" : planMode) {
            case "CONFIRM", "CONFIRMATION" -> "CONFIRMATION";
            case "PREPARE", "PREPARE_ONLY", "PREPARATION" -> "PREPARATION";
            case "REDUCE", "REDUCED" -> "REDUCED";
            case "WATCH", "OBSERVATION" -> "OBSERVATION";
            case "CONFUSED", "BLOCKED" -> "BLOCKED";
            default -> "BLOCKED";
        };
        boolean extreme = "CONFLICT_TOO_HIGH".equals(downgradeReason) || "BLOCKED".equals(normalizedPlanMode);
        return new AiRoleResultsPayload.SynthesisPayload(
                direction, confidence, risk, normalizedPlanMode, worthOpening,
                extreme ? "LEVEL_4_EXTREME_CONFLICT" : "LEVEL_2_MINOR_DISAGREEMENT",
                extreme ? 80 : 25,
                "UNCHANGED", extreme ? "RAISED" : "UNCHANGED",
                extreme ? "BLOCKED" : "UNCHANGED", extreme,
                downgradeReason,
                downgradeReason == null ? "ROLE_RELATIONSHIP_VERIFIED" : downgradeReason,
                "NEW_VERIFIED_ANALYSIS");
    }

    private static OpportunityLogPublicDTO publicOpportunity(
            String opportunityId,
            String lifecycle,
            String status) {
        return publicOpportunity(
                opportunityId,
                lifecycle,
                status,
                LocalDateTime.of(2026, 6, 27, 9, 30));
    }

    private static OpportunityLogPublicDTO publicOpportunity(
            String opportunityId,
            String lifecycle,
            String status,
            LocalDateTime timestamp) {
        return new OpportunityLogPublicDTO(
                opportunityId,
                "analysis-" + opportunityId,
                "BTCUSDT",
                "1h",
                "LONG",
                lifecycle,
                status,
                timestamp,
                OpportunityLogStatus.RESOLVED.equalsIgnoreCase(lifecycle) ? timestamp : null,
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("95"),
                OpportunityLogStatus.MISSED_VALID.equalsIgnoreCase(status),
                OpportunityLogStatus.MISSED_INVALID.equalsIgnoreCase(status),
                OpportunityLogStatus.MISSED_VALID.equalsIgnoreCase(status) ? timestamp : null,
                OpportunityLogStatus.MISSED_INVALID.equalsIgnoreCase(status) ? timestamp : null,
                OpportunityLogStatus.MISSED_VALID.equalsIgnoreCase(status)
                        ? OpportunityLogStatus.TARGET_FIRST
                        : OpportunityLogStatus.MISSED_INVALID.equalsIgnoreCase(status)
                        ? OpportunityLogStatus.INVALIDATION_FIRST
                        : null,
                null,
                null,
                null,
                null,
                "MARKET_DATA",
                timestamp,
                timestamp,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    private DecisionResultVO decision(String symbol,
                                      String marketBias,
                                      String confidence,
                                      String risk,
                                      Integer dataQuality,
                                      Integer aiConflictScore,
                                      String aiConflictLevel,
                                      Boolean worthOpening,
                                      String assetStateSnapshot) {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol(symbol);
        decision.setAnalysisId("analysis-" + symbol);
        decision.setDecisionId("decision-" + symbol);
        decision.setTimeframe("1h");
        decision.setMarketBiasHierarchy(marketBias);
        decision.setConfidenceLevel(confidence);
        decision.setRiskLevel(risk);
        decision.setDataQualityScore(dataQuality);
        decision.setAiConflictScore(aiConflictScore);
        decision.setAiConflictLevel(aiConflictLevel);
        decision.setIsWorthOpening(worthOpening);
        decision.setAssetStateSnapshot(assetStateSnapshot);
        return decision;
    }

    private HomeTopAssetProjection projection(Long assetId,
                                               DecisionResultVO decision,
                                               Integer opportunityScore,
                                               String opportunityId,
                                               String opportunityState) {
        return new HomeTopAssetProjection(
                assetId,
                decision.getSymbol(),
                decision.getSymbol(),
                opportunityScore,
                decision.getMarketBiasHierarchy(),
                decision.getConfidenceLevel(),
                decision.getRiskLevel(),
                decision.getPlanMode(),
                decision.getAiConflictLevel(),
                decision.getDataQualityScore(),
                "FRESH",
                0L,
                0L,
                opportunityScore,
                "OPPORTUNITY_SCORE=" + opportunityScore
                        + "|CONFIDENCE=" + decision.getConfidenceLevel()
                        + "|RISK_LEVEL=" + decision.getRiskLevel()
                        + "|PLAN_MODE=" + decision.getPlanMode()
                        + "|AI_DECISION=" + decision.getAiConflictLevel()
                        + "|DATA_QUALITY=" + decision.getDataQualityScore(),
                decision.getAnalysisId(),
                opportunityId,
                opportunityState,
                opportunityId,
                decision.getTimeframe(),
                decision.getPlanMode(),
                0,
                "ALIGNED",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                decision);
    }

    private AnalysisRunDO analysisRun(String analysisId, String symbol) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        return run;
    }

    private DashboardHomeVO.AssetVO asset(DashboardHomeVO home, String symbol) {
        return home.getAssets().stream()
                .filter(asset -> symbol.equals(asset.getSymbol()))
                .findFirst()
                .orElseThrow();
    }

    private DashboardHomeVO.AiTabVO aiTab(DashboardHomeVO home, String role) {
        return home.getAiDecision().getTabs().stream()
                .filter(tab -> role.equals(tab.getRole()))
                .findFirst()
                .orElseThrow();
    }

    private ProviderReadinessVO providerReadiness(String marketStatus,
                                                  String aiStatus,
                                                  String externalContextStatus,
                                                  String dataSourceText) {
        ProviderReadinessVO readiness = new ProviderReadinessVO();
        readiness.setMarketDataProviderStatus(marketStatus);
        readiness.setAiProviderStatus(aiStatus);
        readiness.setExternalContextProviderStatus(externalContextStatus);
        readiness.setDataSourceText(dataSourceText);
        readiness.setProviders(List.of(
                provider("MARKET_DATA", "BINANCE_PUBLIC_MARKET_DATA", marketStatus),
                provider("AI", "OPENAI", aiStatus),
                provider("EXTERNAL_CONTEXT", "MACRO_NEWS_CONTEXT", externalContextStatus)
        ));
        return readiness;
    }

    private ProviderReadinessVO.ProviderStatusVO provider(String category, String name, String status) {
        ProviderReadinessVO.ProviderStatusVO provider = new ProviderReadinessVO.ProviderStatusVO();
        provider.setCategory(category);
        provider.setName(name);
        provider.setStatus(status);
        provider.setEnabled("CONFIGURED".equals(status));
        provider.setConfigured("CONFIGURED".equals(status));
        provider.setConnected(false);
        provider.setReason("CONFIG_ONLY_NOT_CONNECTED");
        return provider;
    }

    private void assertNoAiEvidence(DashboardHomeVO home) {
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs()).allSatisfy(tab -> {
            assertThat(tab.getResultAvailable()).isFalse();
            assertThat(tab.getStatusMessage()).isNotBlank();
            assertThat(tab.getDirection()).isNull();
            assertThat(tab.getConfidenceLevel()).isNull();
            assertThat(tab.getSupportEvidence()).isEmpty();
            assertThat(tab.getAgainstEvidence()).isEmpty();
            assertThat(tab.getRiskPoints()).isEmpty();
            assertThat(tab.getDowngradeReason()).isNull();
            assertThat(tab.getReviewConclusion()).isNull();
            assertThat(tab.getFinalMarketBias()).isNull();
            assertThat(tab.getFinalConfidence()).isNull();
            assertThat(tab.getFinalRiskLevel()).isNull();
            assertThat(tab.getFinalPlanMode()).isNull();
            assertThat(tab.getWorthOpening()).isNull();
            assertThat(tab.getFinalConclusion()).isNull();
            assertThat(tab.getCoreSupportingEvidence()).isEmpty();
            assertThat(tab.getCoreCounterEvidence()).isEmpty();
            assertThat(tab.getDecisionSummary()).isNull();
            assertThat(tab.getReviewVerdict()).isNull();
            assertThat(tab.getDetectedContradictions()).isEmpty();
            assertThat(tab.getWeakEvidence()).isEmpty();
            assertThat(tab.getLogicGaps()).isEmpty();
            assertThat(tab.getDowngradeRecommendation()).isNull();
            assertThat(tab.getRiskAdjustmentSuggestion()).isNull();
            assertThat(tab.getManualReviewRequired()).isNull();
            assertThat(tab.getChallengeThesis()).isNull();
            assertThat(tab.getEventRisks()).isEmpty();
            assertThat(tab.getSentimentReversalRisks()).isEmpty();
            assertThat(tab.getMicrostructureTraps()).isEmpty();
            assertThat(tab.getLiquidityRisks()).isEmpty();
            assertThat(tab.getCounterEvidence()).isEmpty();
            assertThat(tab.getChallengeConclusion()).isNull();
        });
    }

    private DerivativesRiskSnapshot dashboardDerivativesSnapshot() {
        Instant now = Instant.now();
        List<String> datasets = List.of(
                ProviderDatasetType.COINGLASS_OPEN_INTEREST.name(),
                ProviderDatasetType.COINGLASS_FUNDING.name(),
                ProviderDatasetType.COINGLASS_LIQUIDATION.name(),
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO.name());
        return new DerivativesRiskSnapshot("BTCUSDT", "COINGLASS_V4", now, now, now.plusSeconds(60),
                new BigDecimal("100000000"), null, new BigDecimal("0.05"), new BigDecimal("0.05"), null,
                new BigDecimal("0.0001"), null, BigDecimal.ONE, "GLOBAL_ACCOUNT",
                null, new BigDecimal("1000"), null, null,
                null, new BigDecimal("1000"), null, null,
                null, new BigDecimal("0.20"), datasets, List.of(), List.of(), UnifiedSourceStatus.READY,
                SnapshotFreshnessStatus.FRESH, "COMPLETE", List.of(), "trace-dashboard-derivatives",
                Map.of(), null);
    }

}
