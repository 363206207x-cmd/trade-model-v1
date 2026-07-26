package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiReviewConflictLevel;
import org.example.trademodel.ai.AiReviewStance;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.controller.DashboardHomeController;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
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
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.readiness.ProviderReadinessService;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.example.trademodel.vo.UserPositionVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardHomeServiceImplTest {
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
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private PushRecheckLogMapper pushRecheckLogMapper;
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
    private final AiRoleResultsCodec aiRoleResultsCodec = new AiRoleResultsCodec(new ObjectMapper());

    @BeforeEach
    void setUp() {
        service = new DashboardHomeServiceImpl(
                decisionService,
                monitorService,
                userPositionService,
                positionMonitorLogService,
                positionSyncService,
                pushSnapshotMapper,
                pushRecheckLogMapper,
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
    void homeAggregatesStableReadOnlySemanticsWithoutCrossFallbacks() {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setStatus("OK");
        system.setPendingCount(4);
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
        position.setSourceType("MANUAL");
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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(btc, eth, sol, bnb));
        when(monitorService.getRecentAlerts(2)).thenReturn(List.of(alert));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position, nonManualPosition));
        when(positionSyncService.getPositionSyncStatus()).thenReturn(sync);
        when(pushSnapshotMapper.countPendingRecheckBacklog(any(LocalDateTime.class))).thenReturn(7);
        when(pushSnapshotMapper.listPendingRecheck(anyString(), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of());

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getSystemState().getPendingReview().getValue()).isEqualTo(4);
        assertThat(home.getSystemState().getPendingReview().getValue()).isNotEqualTo(99);
        assertThat(home.getSystemState().getDataQuality().getValue()).isEqualTo(88);
        assertThat(home.getSystemState().getDataQuality().getHelper()).isEqualTo("选中资产分析快照");
        assertThat(home.getSystemState().getRiskLevel().getValue()).isEqualTo("HIGH");
        assertThat(home.getSystemState().getRiskLevel().getHelper()).isEqualTo("选中资产决策风险");
        assertThat(home.getSystemState().getMarketTrend().getValue()).isEqualTo("BULLISH");
        assertThat(home.getSystemState().getAiConflict().getValue()).isEqualTo("LEVEL_2_REVIEW");
        assertThat(home.getSystemState().getAiConflict().getValueLabel()).isEqualTo("轻微分歧");
        assertThat(home.getSystemState().getAiConflict().getScore()).isEqualTo(25);

        assertThat(home.getAssets()).hasSize(6);
        DashboardHomeVO.AssetVO btcAsset = asset(home, "BTC/USDT");
        assertThat(btcAsset.getMarketBias()).isEqualTo("BULLISH");
        assertThat(btcAsset.getConfidenceLevel()).isEqualTo("HIGH");
        assertThat(btcAsset.getRiskLevel()).isEqualTo("HIGH");
        assertThat(btcAsset.getWorthOpening()).isTrue();
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
        assertThat(homePosition.getCurrentPrice()).isNull();
        assertThat(homePosition.getFloatingPnl()).isNull();
        assertThat(homePosition.getMonitorConclusion()).isEqualTo("入场逻辑仍成立");
        assertThat(home.getExecutionSuggestion().getPositionMode()).isTrue();
        assertThat(home.getExecutionSuggestion().getStatus()).isEqualTo("POSITION_MONITORING");
        assertThat(home.getExecutionSuggestion().getPositionMonitor()).isSameAs(homePosition);
        assertThat(home.getExecutionSuggestion().getOriginalPlanLabel())
                .isEqualTo("原执行计划，仅用于持仓复核和复盘对照");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo("63000-64000");
        assertThat(home.getExecutionSuggestion().getLeverageSuggestion()).isEqualTo("20x");
        assertThat(home.getAiDecision().getActiveTab()).isEqualTo("GPT_FINAL");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getSchemaVersion()).isEqualTo("v1");
        assertThat(home.getAiDecision().getTabs().get(0).getSupportEvidence())
                .containsExactly("规则方向与 AI 复核一致");
        assertThat(home.getPushInbox().getCounts().getWaiting()).isEqualTo(7);
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

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getDerivatives().getStatus()).isEqualTo("正常");
        assertThat(home.getDerivatives().getOpenInterestStructure()).isEqualTo("增加");
        assertThat(home.getDerivatives().getFundingRisk()).isEqualTo("正常");
        assertThat(home.getDerivatives().getLiquidationRisk()).isEqualTo("正常");
        assertThat(home.getDerivatives().getSource()).isEqualTo("CoinGlass v4");
        assertThat(home.getDerivatives().getDecisionImpact()).isIn("确认", "降级", "风险阻断");
        assertThat(home.getDerivatives().getDecisionImpact()).doesNotContain("做多", "做空");
    }

    @Test
    void pushInboxCountsUseReadonlyPushBacklogAndNotMissedOpportunityOrPositions() {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setMissedValidOpportunityCount(99);

        UserPositionVO nonManualPosition = new UserPositionVO();
        nonManualPosition.setId(12L);
        nonManualPosition.setAssetSymbol("ETHUSDT");
        nonManualPosition.setStatus("OPEN");
        nonManualPosition.setSourceType("SYSTEM");

        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(userPositionService.listOpenPositions()).thenReturn(List.of(nonManualPosition));
        when(pushSnapshotMapper.countPendingRecheckBacklog(any(LocalDateTime.class))).thenReturn(3);
        when(pushSnapshotMapper.countByPushStatuses(anyList())).thenAnswer(invocation -> {
            List<String> statuses = invocation.getArgument(0);
            if (statuses.contains("RECHECK_REVIEW_PASSED")) {
                return 2;
            }
            if (statuses.contains("RECHECK_INVALIDATED")) {
                return 5;
            }
            return 0;
        });

        DashboardHomeVO home = service.getHome(null, 6);

        assertThat(home.getPushInbox().getTelegramStatus()).isEqualTo("WAITING_SYNC");
        assertThat(home.getDiagnostics().getTelegram()).isEqualTo("WAITING_SYNC");
        assertThat(home.getPushInbox().getTelegramStatus()).isEqualTo(home.getDiagnostics().getTelegram());
        assertThat(home.getPushInbox().getHasOpenPosition()).isFalse();
        assertThat(home.getPushInbox().getMode()).isEqualTo("OPPORTUNITY_ONLY");
        assertThat(home.getPushInbox().getCounts().getWaiting()).isEqualTo(3);
        assertThat(home.getPushInbox().getCounts().getWaiting()).isNotEqualTo(99);
        assertThat(home.getPushInbox().getCounts().getExecutable()).isEqualTo(2);
        assertThat(home.getPushInbox().getCounts().getInvalidated()).isEqualTo(5);
        assertThat(home.getPushInbox().getCounts().getPositionRisk()).isZero();
    }

    @Test
    void telegramReadonlyStatusStaysWaitingSyncAndDoesNotUseConfigOnlyNotifyChannels() {
        UserConfigDO configOnly = new UserConfigDO();
        configOnly.setUserId("dashboard-home");
        configOnly.setNotifyChannels("telegram");

        DashboardHomeVO home = service.getHome(null, 6);

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

        DashboardHomeVO home = service.getHome(null, 6);

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
        manualPosition.setSourceType("MANUAL");

        when(userPositionService.listOpenPositions()).thenReturn(List.of(manualPosition));
        when(positionMonitorLogService.listByPositionId(13L, 1)).thenReturn(List.of());

        DashboardHomeVO home = service.getHome(null, 6);

        assertThat(home.getPushInbox().getHasOpenPosition()).isTrue();
        assertThat(home.getPushInbox().getMode()).isEqualTo("OPPORTUNITY_AND_POSITION_RISK");
        assertThat(home.getPushInbox().getCounts().getPositionRisk()).isZero();
    }

    @Test
    void pushInboxItemsMapOnlyRealSnapshotFieldsAndLatestRecheckStatus() {
        LocalDateTime createTime = LocalDateTime.of(2026, 6, 27, 9, 30);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 27, 11, 30);
        TmPushSnapshotDO snapshot = new TmPushSnapshotDO();
        snapshot.setPushId(101L);
        snapshot.setSymbol("BTCUSDT");
        snapshot.setPushType("OPPORTUNITY_RECHECK");
        snapshot.setPushStatus("CAPTURED");
        snapshot.setCreateTime(createTime);
        snapshot.setExpiresAt(expiresAt);

        TmPushRecheckLogDO latestLog = new TmPushRecheckLogDO();
        latestLog.setPushId(101L);
        latestLog.setRecheckStatus("DRIFTED");

        when(pushSnapshotMapper.countPendingRecheckBacklog(any(LocalDateTime.class))).thenReturn(1);
        when(pushSnapshotMapper.listPendingRecheck(eq("CAPTURED"), any(LocalDateTime.class), eq(6)))
                .thenReturn(List.of(snapshot));
        when(pushSnapshotMapper.listPendingRecheck(eq("RECHECK_REVIEW_WAITING"), any(LocalDateTime.class), eq(5)))
                .thenReturn(List.of());
        when(pushSnapshotMapper.listPendingRecheck(eq("RECHECK_VALID_WAITING"), any(LocalDateTime.class), eq(5)))
                .thenReturn(List.of());
        when(pushRecheckLogMapper.selectLatestByPushId(101L)).thenReturn(latestLog);

        DashboardHomeVO home = service.getHome(null, 6);

        assertThat(home.getPushInbox().getItems()).hasSize(1);
        DashboardHomeVO.PushItemVO item = home.getPushInbox().getItems().get(0);
        assertThat(item.getPushId()).isEqualTo(101L);
        assertThat(item.getSymbol()).isEqualTo("BTC/USDT");
        assertThat(item.getStatus()).isEqualTo("CAPTURED");
        assertThat(item.getType()).isEqualTo("OPPORTUNITY_RECHECK");
        assertThat(item.getCreatedAt()).isEqualTo(createTime);
        assertThat(item.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(item.getRecheckStatus()).isEqualTo("DRIFTED_FROM_ENTRY_ZONE");
        assertThat(Arrays.stream(DashboardHomeVO.PushItemVO.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("title", "summary");
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
        position.setSourceType("MANUAL");

        PositionMonitorLogDTO monitorLog = new PositionMonitorLogDTO();
        monitorLog.setPositionId(9L);
        monitorLog.setCurrentPrice(new BigDecimal("63500"));
        monitorLog.setLogicStatus("LOGIC_VALID");

        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionId(9L, 1)).thenReturn(List.of(monitorLog));

        DashboardHomeVO home = service.getHome(null, 6);

        assertThat(home.getPositions()).hasSize(1);
        DashboardHomeVO.PositionVO homePosition = home.getPositions().get(0);
        assertThat(homePosition.getCurrentPrice()).isEqualByComparingTo("63500");
        assertThat(homePosition.getMonitorConclusion()).isEqualTo("入场逻辑仍成立");
        assertThat(homePosition.getFloatingPnl()).isEqualByComparingTo("300.0");
        assertThat(homePosition.getPnlPct()).isEqualByComparingTo("2.41935500");
        assertThat(homePosition.getAccountImpactPct()).isEqualByComparingTo("4.83871000");
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
        position.setSourceType("MANUAL");

        PositionMonitorLogDTO monitorLog = new PositionMonitorLogDTO();
        monitorLog.setPositionId(19L);
        monitorLog.setCurrentPrice(new BigDecimal("105"));
        monitorLog.setLogicStatus("LOGIC_VALID");
        monitorLog.setRiskLevel("LOW");
        monitorLog.setSuggestedAction("TIGHTEN_STOP_REVIEW");
        monitorLog.setCreatedAt(LocalDateTime.of(2026, 7, 13, 10, 5));

        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionId(19L, 1)).thenReturn(List.of(monitorLog));

        DashboardHomeVO home = service.getHome(null, 6);

        assertThat(home.getPositions()).hasSize(1);
        DashboardHomeVO.PositionVO row = home.getPositions().get(0);
        assertThat(row.getEntryLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(row.getDirectionSupportStatus()).isEqualTo("SUPPORTED");
        assertThat(row.getReversalStatus()).isEqualTo("NO_REVERSAL_SIGNAL");
        assertThat(row.getRiskLevel()).isEqualTo("LOW");
        assertThat(row.getSuggestedManualAction()).isEqualTo("TIGHTEN_STOP_REVIEW");
        assertThat(row.getSuggestedManualActionText()).isEqualTo("复核是否收紧止损");
        assertThat(row.getEntryLogicStatusLabel()).isEqualTo("入场逻辑仍成立");
        assertThat(row.getDirectionSupportStatusLabel()).isEqualTo("当前方向仍获支持");
        assertThat(row.getReversalStatusLabel()).isEqualTo("暂无反转信号");
        assertThat(row.getUserStopLoss()).isEqualByComparingTo("95");
        assertThat(row.getUserTakeProfit()).isEqualByComparingTo("115");
        assertThat(row.getSystemSuggestedStopLoss()).isNull();
        assertThat(row.getSystemSuggestedTakeProfit()).isNull();
        assertThat(row.getOpenedAt()).isEqualTo(LocalDateTime.of(2026, 7, 13, 10, 0));
        assertThat(row.getLastMonitorAt()).isEqualTo(LocalDateTime.of(2026, 7, 13, 10, 5));
        assertThat(row.getNextMonitorAt()).isNull();
    }

    @Test
    void closedPositionNotDisplayedAsActiveMonitoring() {
        UserPositionVO closedPosition = new UserPositionVO();
        closedPosition.setId(21L);
        closedPosition.setAssetSymbol("BTCUSDT");
        closedPosition.setStatus("CLOSED");
        closedPosition.setSourceType("MANUAL");

        when(userPositionService.listOpenPositions()).thenReturn(List.of(closedPosition));

        DashboardHomeVO home = service.getHome(null, 6);

        assertThat(home.getPositions()).isEmpty();
        assertThat(home.getPushInbox().getHasOpenPosition()).isFalse();
        assertThat(home.getPushInbox().getMode()).isEqualTo("OPPORTUNITY_ONLY");
    }

    @Test
    void executionSuggestionDoesNotBecomePosition() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 85, 10,
                "LEVEL_1", true, "{\"state\":\"CANDIDATE\"}");
        decision.setEntryZone("63000-64000");
        decision.setStopLoss("61000");
        decision.setTakeProfitRules("66000 / 69000");
        decision.setValidPeriod(ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));
        when(userPositionService.listOpenPositions()).thenReturn(List.of());

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

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
        position.setSourceType("MANUAL");

        PositionMonitorLogDTO monitorLog = new PositionMonitorLogDTO();
        monitorLog.setPositionId(10L);
        monitorLog.setCurrentPrice(new BigDecimal("90"));
        monitorLog.setLogicStatus("LOGIC_VALID");

        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionId(10L, 1)).thenReturn(List.of(monitorLog));

        DashboardHomeVO home = service.getHome(null, 6);

        DashboardHomeVO.PositionVO homePosition = home.getPositions().get(0);
        assertThat(homePosition.getFloatingPnl()).isEqualByComparingTo("20");
        assertThat(homePosition.getPnlPct()).isEqualByComparingTo("10.00000000");
        assertThat(homePosition.getAccountImpactPct()).isEqualByComparingTo("30.00000000");
        assertThat(homePosition.getSuggestedManualActionText()).isEqualTo("人工复核");
    }

    @Test
    void singlePositionForSymbolStillAutoSelects() {
        OriginalPlanFixture fixture = originalPlanFixture(301L, "SINGLE-A");

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, null);

        assertThat(home.getPositionSelectionStatus()).isEqualTo("UNIQUE_POSITION_SELECTED");
        assertThat(home.getMatchingPositionCount()).isEqualTo(1);
        assertThat(home.getSelectedPositionId()).isEqualTo(301L);
        assertThat(home.getExecutionSuggestion().getPositionMonitor().getPositionId()).isEqualTo(301L);
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo(fixture.decision().getEntryZone());
    }

    @Test
    void multiplePositionsSameSymbolWithoutPositionIdFailClosed() {
        MultiPositionFixture fixture = twoSameSymbolPositions();

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, null);

        assertThat(home.getPositionSelectionStatus()).isEqualTo("POSITION_SELECTION_REQUIRED");
        assertThat(home.getMatchingPositionCount()).isEqualTo(2);
        assertThat(home.getSelectedPositionId()).isNull();
        assertPositionSelectionBlocked(home.getExecutionSuggestion(), "POSITION_SELECTION_REQUIRED", "请选择具体持仓");
        assertThat(home.getExecutionSuggestion().getEntryZone())
                .isNotEqualTo(fixture.planA().getEntryZone())
                .isNotEqualTo(fixture.planB().getEntryZone());
    }

    @Test
    void selectedPositionIdBShowsOnlyPlanB() {
        MultiPositionFixture fixture = twoSameSymbolPositions();

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, fixture.positionB().getId());
        DashboardHomeVO.ExecutionSuggestionVO suggestion = home.getExecutionSuggestion();

        assertThat(home.getPositionSelectionStatus()).isEqualTo("EXACT_POSITION_SELECTED");
        assertThat(home.getSelectedPositionId()).isEqualTo(fixture.positionB().getId());
        assertThat(suggestion.getPositionMonitor().getPositionId()).isEqualTo(fixture.positionB().getId());
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo(fixture.planB().getAnalysisId());
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo("plan-position-B");
        assertThat(suggestion.getEntryZone()).isEqualTo("POSITION-B-entry");
        assertThat(suggestion.getStopLoss()).isEqualTo("POSITION-B-stop");
        assertThat(suggestion.getTakeProfitRules()).isEqualTo("POSITION-B-tp");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("POSITION-A-entry");
    }

    @Test
    void selectedPositionIdANeverShowsPlanB() {
        MultiPositionFixture fixture = twoSameSymbolPositions();

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, fixture.positionA().getId());
        DashboardHomeVO.ExecutionSuggestionVO suggestion = home.getExecutionSuggestion();

        assertThat(home.getSelectedPositionId()).isEqualTo(fixture.positionA().getId());
        assertThat(suggestion.getPositionMonitor().getPositionId()).isEqualTo(fixture.positionA().getId());
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo(fixture.planA().getAnalysisId());
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo("plan-position-A");
        assertThat(suggestion.getEntryZone()).isEqualTo("POSITION-A-entry");
        assertThat(suggestion.getStopLoss()).isEqualTo("POSITION-A-stop");
        assertThat(suggestion.getTakeProfitRules()).isEqualTo("POSITION-A-tp");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("POSITION-B-entry");
    }

    @Test
    void selectedPositionIdFromAnotherSymbolFailsClosed() {
        UserPositionVO btc = activeManualPosition(311L, "BTCUSDT", null);
        UserPositionVO eth = activeManualPosition(312L, "ETHUSDT", null);
        DecisionResultVO btcPlan = sourcePlanDecision("analysis-btc-A", "BTCUSDT", "BTC-A");
        DecisionResultVO ethPlan = sourcePlanDecision("analysis-eth-B", "ETHUSDT", "ETH-B");
        stubMonitorExecutionPlanSource(btc, btcPlan, "plan-btc-A", "trace-shared", "trace-shared");
        stubMonitorExecutionPlanSource(eth, ethPlan, "plan-eth-B", "trace-eth", "trace-eth");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(btc, eth));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, 312L);

        assertThat(home.getPositionSelectionStatus()).isEqualTo("POSITION_SYMBOL_MISMATCH");
        assertPositionSelectionBlocked(home.getExecutionSuggestion(), "POSITION_SYMBOL_MISMATCH",
                "所选持仓与当前标的不匹配");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNotEqualTo("BTC-A-entry");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNotEqualTo("ETH-B-entry");
    }

    @Test
    void unknownSelectedPositionIdDoesNotFallbackToFirstPosition() {
        twoSameSymbolPositions();

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, 999999L);

        assertThat(home.getPositionSelectionStatus()).isEqualTo("POSITION_NOT_FOUND");
        assertThat(home.getSelectedPositionId()).isNull();
        assertPositionSelectionBlocked(home.getExecutionSuggestion(), "POSITION_NOT_FOUND", "所选持仓不存在");
    }

    @Test
    void nonPositiveSelectedPositionIdFailsClosed() {
        twoSameSymbolPositions();

        DashboardHomeVO zero = service.getHome("BTCUSDT", 6, 0L);
        DashboardHomeVO negative = service.getHome("BTCUSDT", 6, -1L);

        assertPositionSelectionBlocked(zero.getExecutionSuggestion(), "POSITION_NOT_FOUND", "所选持仓不存在");
        assertPositionSelectionBlocked(negative.getExecutionSuggestion(), "POSITION_NOT_FOUND", "所选持仓不存在");
        assertThat(zero.getSelectedPositionId()).isNull();
        assertThat(negative.getSelectedPositionId()).isNull();
    }

    @Test
    void closedPositionIdCannotBeSelected() {
        OriginalPlanFixture active = originalPlanFixture(321L, "ACTIVE-A");
        UserPositionVO closed = activeManualPosition(322L, "BTCUSDT", null);
        closed.setStatus("CLOSED");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(active.position(), closed));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, 322L);

        assertThat(home.getPositions()).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactly(321L);
        assertPositionSelectionBlocked(home.getExecutionSuggestion(), "POSITION_NOT_FOUND", "所选持仓不存在");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNotEqualTo("ACTIVE-A-entry");
    }

    @Test
    void nonManualPositionIdCannotBeSelected() {
        OriginalPlanFixture active = originalPlanFixture(331L, "MANUAL-A");
        UserPositionVO systemPosition = activeManualPosition(332L, "BTCUSDT", null);
        systemPosition.setSourceType("SYSTEM");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(active.position(), systemPosition));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6, 332L);

        assertThat(home.getPositions()).extracting(DashboardHomeVO.PositionVO::getPositionId)
                .containsExactly(331L);
        assertPositionSelectionBlocked(home.getExecutionSuggestion(), "POSITION_NOT_FOUND", "所选持仓不存在");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNotEqualTo("MANUAL-A-entry");
    }

    @Test
    void twoSameSymbolPositionsNeverCrossOriginalPlans() {
        MultiPositionFixture fixture = twoSameSymbolPositions();

        DashboardHomeVO.ExecutionSuggestionVO selectedA = service
                .getHome("BTCUSDT", 6, fixture.positionA().getId()).getExecutionSuggestion();
        DashboardHomeVO.ExecutionSuggestionVO selectedB = service
                .getHome("BTCUSDT", 6, fixture.positionB().getId()).getExecutionSuggestion();

        assertThat(selectedA.getPositionMonitor().getPositionId()).isEqualTo(fixture.positionA().getId());
        assertThat(selectedA.getEntryZone()).isEqualTo("POSITION-A-entry");
        assertThat(selectedA.getSourceExecutionPlanId()).isEqualTo("plan-position-A");
        assertThat(selectedA.getEntryZone()).isNotEqualTo(selectedB.getEntryZone());
        assertThat(selectedB.getPositionMonitor().getPositionId()).isEqualTo(fixture.positionB().getId());
        assertThat(selectedB.getEntryZone()).isEqualTo("POSITION-B-entry");
        assertThat(selectedB.getSourceExecutionPlanId()).isEqualTo("plan-position-B");
    }

    @Test
    void positionFromPlanA_latestDecisionB_neverShowsBAsOriginalPlan() {
        UserPositionVO position = activeManualPosition(31L, "BTCUSDT", null);
        DecisionResultVO planA = sourcePlanDecision("analysis-plan-A", "BTCUSDT", "A");
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        stubMonitorExecutionPlanSource(position, planA, "plan-A", "trace-A", "trace-A");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("ACTIVE");
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo("analysis-plan-A");
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo("plan-A");
        assertThat(suggestion.getEntryZone()).isEqualTo("A-entry");
        assertThat(suggestion.getStopLoss()).isEqualTo("A-stop");
        assertThat(suggestion.getTakeProfitRules()).isEqualTo("A-tp");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("B-entry");
        assertThat(suggestion.getStopLoss()).isNotEqualTo("B-stop");
        assertThat(suggestion.getTakeProfitRules()).isNotEqualTo("B-tp");
        assertThat(suggestion.getSourceAnalysisId()).isNotEqualTo("analysis-latest-B");
    }

    @Test
    void positionWithNoSourceReferenceHidesOriginalPlan() {
        UserPositionVO position = activeManualPosition(32L, "BTCUSDT", null);
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionId(32L, 1)).thenReturn(List.of());
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        verify(executionPlanMapper, never()).selectByPlanId(anyString());
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
    }

    @Test
    void legacyGuessedSiblingBDoesNotReachDashboard() {
        UserPositionVO position = activeManualPosition(321L, "BTCUSDT",
                PositionMonitorSourceContract.executionPlanReference("plan-A"));
        PositionMonitorLogDTO guessedSibling = analysisOnlyMonitor(position, "analysis-X");
        guessedSibling.setExecutionPlanId("plan-B");
        DecisionResultVO latestB = sourcePlanDecision("analysis-X", "BTCUSDT", "B");
        when(positionMonitorLogService.listByPositionId(321L, 1)).thenReturn(List.of(guessedSibling));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertUnverifiedOriginalPlan(home.getExecutionSuggestion());
        assertThat(home.getPositions()).singleElement().satisfies(row -> {
            assertThat(row.getSourceAnalysisId()).isNull();
            assertThat(row.getSourceExecutionPlanId()).isNull();
            assertThat(row.getSourceTraceId()).isNull();
        });
        assertThat(home.getExecutionSuggestion().getEntryZone()).isNotEqualTo("B-entry");
        verify(executionPlanMapper, never()).selectByPlanId("plan-B");
    }

    @Test
    void legacyUntypedPositionWithOldPlanIdsFailsClosed() {
        UserPositionVO position = activeManualPosition(322L, "BTCUSDT", "legacy-untyped-source");
        PositionMonitorLogDTO legacy = analysisOnlyMonitor(position, "analysis-A");
        legacy.setExecutionPlanId("plan-A");
        when(positionMonitorLogService.listByPositionId(322L, 1)).thenReturn(List.of(legacy));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertUnverifiedOriginalPlan(home.getExecutionSuggestion());
        assertThat(home.getPositions().get(0).getSourceAnalysisId()).isNull();
        assertThat(home.getPositions().get(0).getSourceExecutionPlanId()).isNull();
        verify(executionPlanMapper, never()).selectByPlanId(anyString());
    }

    @Test
    void monitorExecutionPlanIdResolvesExactOriginalPlan() {
        UserPositionVO position = activeManualPosition(33L, "BTCUSDT", "ambiguous-source-ref");
        DecisionResultVO planA = sourcePlanDecision("analysis-plan-A", "BTCUSDT", "A");
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        stubMonitorExecutionPlanSource(position, planA, "plan-A", "trace-A", "trace-A");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo("plan-A");
        assertThat(suggestion.getEntryZone()).isEqualTo("A-entry");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("B-entry");
        verify(executionPlanMapper).selectByPlanId("plan-A");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
    }

    @Test
    void typedAnalysisMonitorExactPlanResolvesOriginalPlan() {
        UserPositionVO position = activeManualPosition(34L, "BTCUSDT", null);
        DecisionResultVO planA = sourcePlanDecision("analysis-plan-A", "BTCUSDT", "A");
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        stubMonitorAnalysisSource(position, planA, "plan-A", "trace-A", "trace-A");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo("analysis-plan-A");
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo("plan-A");
        assertThat(suggestion.getEntryZone()).isEqualTo("A-entry");
        verify(executionPlanMapper).selectByPlanId("plan-A");
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(anyString());
    }

    @Test
    void analysisOnlyWithMultiplePlansFailsClosed() {
        UserPositionVO position = activeManualPosition(341L, "BTCUSDT",
                PositionMonitorSourceContract.analysisReference("analysis-shared"));
        PositionMonitorLogDTO monitor = analysisOnlyMonitor(position, "analysis-shared");
        when(positionMonitorLogService.listByPositionId(341L, 1)).thenReturn(List.of(monitor));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(anyString());
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
    }

    @Test
    void analysisOnlyWithNoPlanFailsClosed() {
        UserPositionVO position = activeManualPosition(342L, "BTCUSDT",
                PositionMonitorSourceContract.analysisReference("analysis-no-plan"));
        PositionMonitorLogDTO monitor = analysisOnlyMonitor(position, "analysis-no-plan");
        when(positionMonitorLogService.listByPositionId(342L, 1)).thenReturn(List.of(monitor));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        verify(decisionResultMapper, never()).findByAnalysisIdAndPlanIdJoined(anyString(), anyString());
    }

    @Test
    void analysisOnlyMultiplePlansNeverSelectsLatestSibling() {
        UserPositionVO position = activeManualPosition(343L, "BTCUSDT",
                PositionMonitorSourceContract.analysisReference("analysis-shared"));
        PositionMonitorLogDTO monitor = analysisOnlyMonitor(position, "analysis-shared");
        ExecutionPlanDO latestSiblingB = validExecutionPlan("plan-B", "analysis-shared");
        latestSiblingB.setEntryZone("B-entry");
        when(positionMonitorLogService.listByPositionId(343L, 1)).thenReturn(List.of(monitor));
        lenient().when(executionPlanMapper.selectLatestByAnalysisId("analysis-shared")).thenReturn(latestSiblingB);
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        assertThat(suggestion.getEntryZone()).isNotEqualTo("B-entry");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
    }

    @Test
    void monitorExecutionPlanIdStillResolvesExactPlanAmongSiblings() {
        UserPositionVO position = activeManualPosition(344L, "BTCUSDT", null);
        DecisionResultVO planA = sourcePlanDecision("analysis-shared", "BTCUSDT", "A");
        DecisionResultVO latestSiblingB = sourcePlanDecision("analysis-shared", "BTCUSDT", "B");
        stubMonitorExecutionPlanSource(position, planA, "plan-A", "trace-A", "trace-A");
        lenient().when(executionPlanMapper.selectOnlyByAnalysisId("analysis-shared")).thenReturn(null);
        lenient().when(executionPlanMapper.selectLatestByAnalysisId("analysis-shared"))
                .thenReturn(validExecutionPlan("plan-B", "analysis-shared"));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestSiblingB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo("plan-A");
        assertThat(suggestion.getEntryZone()).isEqualTo("A-entry");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("B-entry");
        verify(executionPlanMapper).selectByPlanId("plan-A");
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(anyString());
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
    }

    @Test
    void monitorExecutionPlanIdAndAnalysisIdMismatchFailsClosed() {
        UserPositionVO position = activeManualPosition(345L, "BTCUSDT",
                PositionMonitorSourceContract.executionPlanReference("plan-A"));
        PositionMonitorLogDTO monitor = analysisOnlyMonitor(position, "analysis-monitor-A");
        monitor.setExecutionPlanId("plan-A");
        when(positionMonitorLogService.listByPositionId(345L, 1)).thenReturn(List.of(monitor));
        when(executionPlanMapper.selectByPlanId("plan-A"))
                .thenReturn(validExecutionPlan("plan-A", "analysis-plan-B"));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        verify(decisionResultMapper, never()).findByAnalysisIdAndPlanIdJoined(anyString(), anyString());
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(anyString());
    }

    @Test
    void originalPlanSymbolMismatchFailsClosed() {
        UserPositionVO position = activeManualPosition(35L, "BTCUSDT", null);
        DecisionResultVO otherAssetPlan = sourcePlanDecision("analysis-eth-A", "ETHUSDT", "ETH-A");
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        stubMonitorExecutionPlanSource(position, otherAssetPlan, "plan-eth-A", "trace-eth", "trace-eth");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        assertThat(suggestion.getEntryZone()).isNotEqualTo("B-entry");
    }

    @Test
    void monitorFromAnotherPositionCannotSupplyOriginalPlanIdentity() {
        UserPositionVO position = activeManualPosition(351L, "BTCUSDT", null);
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        PositionMonitorLogDTO wrongPositionMonitor = new PositionMonitorLogDTO();
        wrongPositionMonitor.setPositionId(999L);
        wrongPositionMonitor.setAnalysisId("analysis-other-position-A");
        wrongPositionMonitor.setExecutionPlanId("plan-other-position-A");
        when(positionMonitorLogService.listByPositionId(351L, 1)).thenReturn(List.of(wrongPositionMonitor));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        verify(executionPlanMapper, never()).selectByPlanId("plan-other-position-A");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId("analysis-other-position-A");
    }

    @Test
    void sourceTraceMismatchDoesNotPromoteNewDecision() {
        UserPositionVO position = activeManualPosition(36L, "BTCUSDT", null);
        DecisionResultVO planA = sourcePlanDecision("analysis-plan-A", "BTCUSDT", "A");
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        stubMonitorExecutionPlanSource(position, planA, "plan-A", "trace-A", "trace-current-B");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("STATE_MISMATCH");
        assertThat(suggestion.getOriginalPlanLabel())
                .isEqualTo("状态已更新，原计划不再作为当前执行依据");
        assertThat(suggestion.getEntryZone()).isEqualTo("A-entry");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("B-entry");
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo("analysis-plan-A");
    }

    @Test
    void activePositionNeverFallsBackToLatestSymbolDecision() {
        UserPositionVO position = activeManualPosition(37L, "BTCUSDT", "plan-or-analysis-type-unknown");
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "B");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionId(37L, 1)).thenReturn(List.of());
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertUnverifiedOriginalPlan(home.getExecutionSuggestion());
        assertThat(home.getPositions().get(0).getSourceRefId()).isEqualTo("plan-or-analysis-type-unknown");
        verify(executionPlanMapper, never()).selectByPlanId("plan-or-analysis-type-unknown");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId("plan-or-analysis-type-unknown");
    }

    @Test
    void exactPlanA_monitorLogsAAndDashboardShowsOnlyA() {
        long positionId = 371L;
        String analysisA = "analysis-monitor-A";
        String planAId = "plan-monitor-A";
        List<PositionMonitorLogDTO> capturedLogs = new ArrayList<>();
        wireCapturedMonitorLogs(positionId, capturedLogs);

        UserPositionDO positionDO = monitorPosition(positionId,
                PositionMonitorSourceContract.executionPlanReference(planAId));
        UserPositionVO positionVO = activeManualPosition(positionId, "BTCUSDT", positionDO.getSourceRefId());
        UserPositionMapper monitorPositionMapper = mock(UserPositionMapper.class);
        MarketQuoteClient quoteClient = mock(MarketQuoteClient.class);
        UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        when(monitorPositionMapper.selectById(positionId)).thenReturn(positionDO);
        when(quoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(monitorQuote("100")));
        when(riskAdapter.currentRisk()).thenReturn(allowedMonitorRisk());

        ExecutionPlanDO exactPlanA = validExecutionPlan(planAId, analysisA);
        exactPlanA.setEntryZone("PLAN-A-entry");
        exactPlanA.setStopLoss("PLAN-A-stop");
        exactPlanA.setTakeProfitRules("PLAN-A-tp");
        exactPlanA.setLeverageSuggestion("2x");
        exactPlanA.setPositionSuggestion("PLAN-A-position");
        exactPlanA.setInvalidCondition("PLAN-A-invalid");
        when(executionPlanMapper.selectByPlanId(planAId)).thenReturn(exactPlanA);
        AnalysisRunDO runA = sourceRun(analysisA, "BTCUSDT", "trace-monitor-A");
        when(analysisRunMapper.selectById(analysisA)).thenReturn(runA);

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

        PositionMonitorResultDTO monitorResult = monitorService.monitorUserPosition(positionId);

        assertThat(monitorResult.getAnalysisId()).isEqualTo(analysisA);
        assertThat(monitorResult.getExecutionPlanId()).isEqualTo(planAId);
        assertThat(capturedLogs).singleElement().satisfies(log -> {
            assertThat(log.getAnalysisId()).isEqualTo(analysisA);
            assertThat(log.getExecutionPlanId()).isEqualTo(planAId);
        });

        DecisionResultVO joinedDecisionA = sourcePlanDecision(analysisA, "BTCUSDT", "SIBLING-B");
        DecisionResultVO latestDecisionB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "LATEST-B");
        when(decisionResultMapper.findByAnalysisIdAndPlanIdJoined(analysisA, planAId))
                .thenReturn(joinedDecisionA);
        when(userPositionService.listOpenPositions()).thenReturn(List.of(positionVO));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestDecisionB));
        when(assetStateMapper.selectBySymbol("BTCUSDT"))
                .thenReturn(sourceState("BTCUSDT", "trace-monitor-A"));

        DashboardHomeVO.ExecutionSuggestionVO suggestion =
                service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanIdentity()).isEqualTo("VERIFIED");
        assertThat(suggestion.getSourceAnalysisId()).isEqualTo(analysisA);
        assertThat(suggestion.getSourceExecutionPlanId()).isEqualTo(planAId);
        assertThat(suggestion.getEntryZone()).isEqualTo("PLAN-A-entry");
        assertThat(suggestion.getStopLoss()).isEqualTo("PLAN-A-stop");
        assertThat(suggestion.getTakeProfitRules()).isEqualTo("PLAN-A-tp");
        assertThat(List.of(suggestion.getEntryZone(), suggestion.getStopLoss(), suggestion.getTakeProfitRules()))
                .doesNotContain("SIBLING-B-entry", "SIBLING-B-stop", "SIBLING-B-tp",
                        "LATEST-B-entry", "LATEST-B-stop", "LATEST-B-tp");
    }

    @Test
    void positionMonitorToDashboardDoesNotReintroduceLatestSiblingFallback() {
        long positionId = 372L;
        String ambiguousAnalysis = "analysis-shared-A-B";
        List<PositionMonitorLogDTO> capturedLogs = new ArrayList<>();
        wireCapturedMonitorLogs(positionId, capturedLogs);

        UserPositionDO positionDO = monitorPosition(positionId, ambiguousAnalysis);
        UserPositionVO positionVO = activeManualPosition(positionId, "BTCUSDT", ambiguousAnalysis);
        UserPositionMapper monitorPositionMapper = mock(UserPositionMapper.class);
        MarketQuoteClient quoteClient = mock(MarketQuoteClient.class);
        UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        when(monitorPositionMapper.selectById(positionId)).thenReturn(positionDO);
        when(quoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(monitorQuote("100")));
        when(riskAdapter.currentRisk()).thenReturn(allowedMonitorRisk());
        ExecutionPlanDO latestSiblingB = validExecutionPlan("plan-latest-B", ambiguousAnalysis);
        latestSiblingB.setEntryZone("LATEST-B-entry");
        lenient().when(executionPlanMapper.selectLatestByAnalysisId(ambiguousAnalysis))
                .thenReturn(latestSiblingB);

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

        PositionMonitorResultDTO monitorResult = monitorService.monitorUserPosition(positionId);

        assertThat(monitorResult.getAnalysisId()).isNull();
        assertThat(capturedLogs).singleElement().satisfies(log -> {
            assertThat(log.getAnalysisId()).isEqualTo(PositionMonitorSourceContract.UNVERIFIED_ANALYSIS_ID);
            assertThat(log.getExecutionPlanId()).isNull();
        });

        DecisionResultVO latestDecisionB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "LATEST-B");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(positionVO));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestDecisionB));

        DashboardHomeVO.ExecutionSuggestionVO suggestion =
                service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertUnverifiedOriginalPlan(suggestion);
        assertThat(suggestion.getEntryZone()).isNotEqualTo("LATEST-B-entry");
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(ambiguousAnalysis);
        verify(executionPlanMapper, never()).selectOnlyByAnalysisId(ambiguousAnalysis);
    }

    @Test
    void positionMonitorAndDashboardAgreeOnRevalidationRequired() {
        assertMonitorDashboardPlanStateAgreement(
                373L,
                plan -> {
                    plan.setNeedsRevalidation(true);
                    plan.setRevalidationReason("EXTREME_PRICE_MOVE");
                },
                "PLAN_REVALIDATION_REQUIRED",
                "REVALIDATION_REQUIRED");
    }

    @Test
    void positionMonitorAndDashboardAgreeOnPlanIncomplete() {
        assertMonitorDashboardPlanStateAgreement(
                374L,
                plan -> plan.setEntryZone("待生成"),
                "PLAN_BOUNDARY_INCOMPLETE",
                "PLAN_INCOMPLETE");
    }

    @Test
    void invalidOriginalPlanIsNotMarkedActive() {
        OriginalPlanFixture fixture = originalPlanFixture(381L, "INVALID-A");
        fixture.executionPlan().setExecutionPlanStatus("INVALID");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_INVALID", "原计划已失效，仅用于历史复核", "INVALID-A");
    }

    @Test
    void blockedOriginalPlanIsNotMarkedActive() {
        OriginalPlanFixture fixture = originalPlanFixture(382L, "BLOCKED-A");
        fixture.executionPlan().setExecutionPlanStatus("BLOCKED");
        fixture.executionPlan().setSourceGateStatus("BLOCKED");
        fixture.executionPlan().setSourceGateComplete(false);

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_BLOCKED",
                "原计划已被门控阻断，仅用于历史复核", "BLOCKED-A");
    }

    @Test
    void incompleteOriginalPlanIsNotMarkedActive() {
        OriginalPlanFixture fixture = originalPlanFixture(383L, "INCOMPLETE-A");
        fixture.executionPlan().setExecutionPlanStatus("INCOMPLETE");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_INCOMPLETE",
                "原计划边界不完整，仅用于历史复核", "INCOMPLETE-A");
    }

    @Test
    void sourceGateIncompleteOriginalPlanIsNotMarkedActive() {
        OriginalPlanFixture fixture = originalPlanFixture(384L, "SOURCE-INCOMPLETE-A");
        fixture.executionPlan().setSourceGateStatus("INCOMPLETE");
        fixture.executionPlan().setSourceGateComplete(false);

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_INCOMPLETE",
                "原计划边界不完整，仅用于历史复核", "SOURCE-INCOMPLETE-A");
    }

    @Test
    void sourceGateCompleteFalseOriginalPlanIsNotMarkedActive() {
        OriginalPlanFixture fixture = originalPlanFixture(3841L, "SOURCE-FLAG-INCOMPLETE-A");
        fixture.executionPlan().setSourceGateStatus("VALID");
        fixture.executionPlan().setSourceGateComplete(false);

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_INCOMPLETE",
                "原计划边界不完整，仅用于历史复核", "SOURCE-FLAG-INCOMPLETE-A");
    }

    @Test
    void needsRevalidationOriginalPlanIsNotMarkedActive() {
        OriginalPlanFixture fixture = originalPlanFixture(385L, "REVALIDATE-A");
        fixture.executionPlan().setNeedsRevalidation(true);
        fixture.executionPlan().setRevalidationReason("证据结构发生变化，等待重新验证");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "REVALIDATION_REQUIRED",
                "原计划需要重新验证，仅用于历史复核：重验证原因已记录，等待人工复核", "REVALIDATE-A");
    }

    @Test
    void hotResetRevalidationReasonIsPreservedAsReviewCopy() {
        OriginalPlanFixture fixture = originalPlanFixture(386L, "HOT-RESET-A");
        fixture.executionPlan().setNeedsRevalidation(true);
        fixture.executionPlan().setRevalidationReason("EXTREME_PRICE_MOVE:PRICE_MOVE_THRESHOLD_BREACHED");
        fixture.executionPlan().setHotResetEventId("hot-reset-event-1");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "REVALIDATION_REQUIRED",
                "原计划需要重新验证，仅用于历史复核：极端价格波动触发重新验证", "HOT-RESET-A");
        assertThat(suggestion.getOriginalPlanLabel()).doesNotContain("EXTREME_PRICE_MOVE");
    }

    @Test
    void hotResetFallbackUsesChineseOnly() {
        OriginalPlanFixture fixture = originalPlanFixture(3861L, "HOT-RESET-FALLBACK-A");
        fixture.executionPlan().setNeedsRevalidation(true);
        fixture.executionPlan().setRevalidationReason(null);
        fixture.executionPlan().setHotResetEventId("hot-reset-event-2");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanLabel())
                .isEqualTo("原计划需要重新验证，仅用于历史复核：热重置已触发重新验证")
                .doesNotContain("Hot Reset");
    }

    @Test
    void mixedChineseAndInternalCodeDoesNotLeak() {
        OriginalPlanFixture fixture = originalPlanFixture(3862L, "MIXED-REASON-A");
        fixture.executionPlan().setNeedsRevalidation(true);
        fixture.executionPlan().setRevalidationReason("热重置 UNKNOWN_INTERNAL_CODE");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanLabel())
                .isEqualTo("原计划需要重新验证，仅用于历史复核：重验证原因已记录，等待人工复核")
                .doesNotContain("UNKNOWN_INTERNAL_CODE");
    }

    @Test
    void unknownRevalidationReasonUsesGenericChineseCopy() {
        OriginalPlanFixture fixture = originalPlanFixture(3863L, "UNKNOWN-REASON-A");
        fixture.executionPlan().setNeedsRevalidation(true);
        fixture.executionPlan().setRevalidationReason("{\"errorCode\":\"UNMAPPED_REASON\"}");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanLabel())
                .isEqualTo("原计划需要重新验证，仅用于历史复核：重验证原因已记录，等待人工复核")
                .doesNotContain("errorCode", "UNMAPPED_REASON", "{");
    }

    @Test
    void knownRevalidationReasonsUseWhitelistLabels() {
        List<List<String>> cases = List.of(
                List.of("EXTREME_PRICE_MOVE", "极端价格波动触发重新验证"),
                List.of("OI_COLLAPSE", "持仓量快速收缩触发重新验证"),
                List.of("LIQUIDITY_DRAIN", "流动性快速下降触发重新验证"),
                List.of("SYSTEMIC_SHOCK", "系统性冲击触发重新验证")
        );
        long positionId = 3864L;
        for (List<String> item : cases) {
            OriginalPlanFixture fixture = originalPlanFixture(positionId++, "KNOWN-REASON-A");
            fixture.executionPlan().setNeedsRevalidation(true);
            fixture.executionPlan().setRevalidationReason(item.get(0));

            DashboardHomeVO.ExecutionSuggestionVO suggestion =
                    service.getHome("BTCUSDT", 6).getExecutionSuggestion();

            assertThat(suggestion.getOriginalPlanLabel()).endsWith(item.get(1));
            assertThat(suggestion.getOriginalPlanLabel()).doesNotContain(item.get(0));
        }
    }

    @Test
    void validStatusWithMissingEntryIsPlanIncomplete() {
        OriginalPlanFixture fixture = originalPlanFixture(390L, "MISSING-ENTRY-A");
        fixture.executionPlan().setEntryZone(null);

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("PLAN_INCOMPLETE");
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getStopLoss()).isEqualTo("MISSING-ENTRY-A-stop");
    }

    @Test
    void validStatusWithMissingStopIsPlanIncomplete() {
        OriginalPlanFixture fixture = originalPlanFixture(391L, "MISSING-STOP-A");
        fixture.executionPlan().setStopLoss(null);

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("PLAN_INCOMPLETE");
        assertThat(suggestion.getStopLoss()).isNull();
    }

    @Test
    void validStatusWithMissingTakeProfitIsPlanIncomplete() {
        OriginalPlanFixture fixture = originalPlanFixture(392L, "MISSING-TP-A");
        fixture.executionPlan().setTakeProfitRules(null);

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("PLAN_INCOMPLETE");
        assertThat(suggestion.getTakeProfitRules()).isNull();
    }

    @Test
    void placeholderBoundariesArePlanIncomplete() {
        List<String> placeholders = List.of("暂无", "—", "待生成");
        long positionId = 393L;
        for (String placeholder : placeholders) {
            OriginalPlanFixture fixture = originalPlanFixture(positionId++, "PLACEHOLDER-A");
            fixture.executionPlan().setEntryZone(placeholder);

            DashboardHomeVO.ExecutionSuggestionVO suggestion =
                    service.getHome("BTCUSDT", 6).getExecutionSuggestion();

            assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("PLAN_INCOMPLETE");
            assertThat(suggestion.getEntryZone()).isNull();
        }
    }

    @Test
    void incompletePlanANeverBorrowsBoundariesFromPlanB() {
        OriginalPlanFixture fixture = originalPlanFixture(396L, "PLAN-A");
        fixture.executionPlan().setEntryZone("暂无");
        fixture.decision().setEntryZone("PLAN-B-entry");
        fixture.decision().setStopLoss("PLAN-B-stop");
        fixture.decision().setTakeProfitRules("PLAN-B-tp");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(suggestion.getOriginalPlanCurrentValidity()).isEqualTo("PLAN_INCOMPLETE");
        assertThat(suggestion.getEntryZone()).isNull();
        assertThat(suggestion.getStopLoss()).isEqualTo("PLAN-A-stop");
        assertThat(suggestion.getTakeProfitRules()).isEqualTo("PLAN-A-tp");
        assertThat(List.of(suggestion.getStopLoss(), suggestion.getTakeProfitRules()))
                .doesNotContain("PLAN-B-stop", "PLAN-B-tp");
    }

    @Test
    void verifiedInactivePlanStillShowsOnlyHistoricalPlanA() {
        OriginalPlanFixture fixture = originalPlanFixtureWithLatestB(387L, "HISTORICAL-A");
        fixture.executionPlan().setExecutionPlanStatus("BLOCKED");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_BLOCKED",
                "原计划已被门控阻断，仅用于历史复核", "HISTORICAL-A");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("LATEST-B-entry");
        assertThat(suggestion.getStatus()).isNotEqualTo("USABLE_REVIEW_PLAN");
    }

    @Test
    void invalidPlanNeverFallsBackToLatestPlanB() {
        OriginalPlanFixture fixture = originalPlanFixtureWithLatestB(388L, "INVALID-SOURCE-A");
        fixture.executionPlan().setExecutionPlanStatus("INVALID");

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_INVALID",
                "原计划已失效，仅用于历史复核", "INVALID-SOURCE-A");
        assertThat(suggestion.getEntryZone()).isNotEqualTo("LATEST-B-entry");
        assertThat(suggestion.getSourceAnalysisId()).isNotEqualTo("analysis-latest-B");
    }

    @Test
    void executionPlanStatusAndDecisionExpiryUseDeterministicPriority() {
        OriginalPlanFixture fixture = originalPlanFixture(389L, "PRIORITY-A");
        fixture.executionPlan().setExecutionPlanStatus("INVALID");
        fixture.decision().setValidFrom(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
        fixture.decision().setExpiresAt(OffsetDateTime.parse("2026-06-02T00:00:00Z"));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertVerifiedHistoricalPlan(suggestion, "PLAN_INVALID",
                "原计划已失效，仅用于历史复核", "PRIORITY-A");
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isNotEqualTo("STATE_MISMATCH");
        assertThat(suggestion.getOriginalPlanCurrentValidity()).isNotEqualTo("EXPIRED");
        verify(assetStateMapper, never()).selectBySymbol("BTCUSDT");
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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(btc, eth));
        when(userPositionService.listOpenPositions()).thenReturn(List.of());

        DashboardHomeVO ethHome = service.getHome("ETHUSDT", 6);

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

        DashboardHomeVO defaultHome = service.getHome(null, 6);

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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(btc, eth, bnb, sol));
        when(userPositionService.listOpenPositions()).thenReturn(List.of());

        DashboardHomeVO home = service.getHome("SOLUSDT", 3);

        assertThat(home.getSelectedSymbol()).isEqualTo("SOLUSDT");
        assertThat(home.getAssets())
                .extracting(DashboardHomeVO.AssetVO::getRawSymbol)
                .containsExactly("BTCUSDT", "ETHUSDT", "SOLUSDT");
        assertThat(asset(home, "SOL/USDT").getSlotType()).isEqualTo("DECISION");
    }

    @Test
    void decisionAssetsExposeOnlyTheirExactAnalysisIdentity() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAnalysisId("analysis-btc-exact");
        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "HIGH", 74, 35,
                "LEVEL_2_REVIEW", false, "{\"state\":\"OBSERVING\"}");
        eth.setAnalysisId("analysis-eth-exact");
        DecisionResultVO solWithoutIdentity = decision("SOLUSDT", "RANGE", "LOW", "LOW", 60, 10,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        solWithoutIdentity.setAnalysisId(null);

        when(decisionService.getLatestDecisionResults(anyInt()))
                .thenReturn(List.of(btc, eth, solWithoutIdentity));

        DashboardHomeVO home = service.getHome("BTCUSDT", 3);

        assertThat(asset(home, "BTC/USDT").getAnalysisId()).isEqualTo("analysis-btc-exact");
        assertThat(asset(home, "ETH/USDT").getAnalysisId()).isEqualTo("analysis-eth-exact");
        assertThat(asset(home, "SOL/USDT").getAnalysisId()).isNull();
    }

    @Test
    void realFallbackAssetsAreCollectedBeforeDefaultSlotsAreUsedToFillTheLimit() {
        PersistedOhlcvBarDO bnb = new PersistedOhlcvBarDO();
        bnb.setSymbol("BNBUSDT");
        bnb.setTimeframe("5m");
        bnb.setClosePrice(new BigDecimal("620.00"));
        bnb.setProvider("BINANCE_PUBLIC");
        bnb.setFreshnessStatus("FRESH");

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of());
        when(userPositionService.listOpenPositions()).thenReturn(List.of());
        lenient().when(persistedOhlcvBarMapper.selectLatestClosedWindow("BNBUSDT", "5m", 1))
                .thenReturn(List.of(bnb));

        DashboardHomeVO home = service.getHome(null, 3);

        assertThat(home.getSelectedSymbol()).isEqualTo("BNBUSDT");
        assertThat(home.getAssets())
                .extracting(DashboardHomeVO.AssetVO::getRawSymbol)
                .containsExactly("BNBUSDT", "BTCUSDT", "ETHUSDT");
        assertThat(asset(home, "BNB/USDT").getSlotType()).isEqualTo("MARKET_DATA");
        assertThat(asset(home, "BNB/USDT").getAnalysisId()).isNull();
        assertThat(home.getAssets().subList(1, 3))
                .extracting(DashboardHomeVO.AssetVO::getSlotType)
                .containsOnly("DEFAULT_SLOT");
        assertThat(home.getAssets().subList(1, 3))
                .extracting(DashboardHomeVO.AssetVO::getAnalysisId)
                .containsOnlyNulls();
        verify(externalContextEvidenceBuilder).buildSnapshot(
                eq("dashboard-home"),
                eq("BNBUSDT"),
                eq("1h"),
                any(LocalDateTime.class),
                eq("CRYPTO"));
    }

    @Test
    void implicitRealFallbackRebuildsDecisionContextForResolvedSymbol() {
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
        allowMatchingSnapshot(bnbDecision);

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of());
        lenient().when(decisionService.getLatestDecisionResultBySymbol("BNBUSDT"))
                .thenReturn(bnbDecision);
        when(userPositionService.listOpenPositions()).thenReturn(List.of());
        lenient().when(persistedOhlcvBarMapper.selectLatestClosedWindow("BNBUSDT", "5m", 1))
                .thenReturn(List.of(bnbBar));

        DashboardHomeVO home = service.getHome(null, 3);

        assertThat(home.getSelectedSymbol()).isEqualTo("BNBUSDT");
        assertThat(home.getAssets().get(0).getSlotType()).isEqualTo("DECISION");
        assertThat(home.getExecutionSuggestion().getSourceAnalysisId()).isEqualTo("analysis-BNBUSDT");
        assertThat(home.getExecutionSuggestion().getDirection()).isEqualTo("BULLISH");
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo("610-615");
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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getAiDecision().getActiveTab()).isEqualTo("GPT_FINAL");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRoleLabel)
                .containsExactly("最终裁决官", "冲突复核官", "反方挑战官");

        DashboardHomeVO.AiTabVO gpt = aiTab(home, "GPT_FINAL");
        assertThat(gpt.getFinalMarketBias()).isEqualTo("BULLISH");
        assertThat(gpt.getFinalConfidence()).isEqualTo("HIGH");
        assertThat(gpt.getFinalRiskLevel()).isEqualTo("HIGH");
        assertThat(gpt.getFinalPlanMode()).isEqualTo("PREPARE_ONLY");
        assertThat(gpt.getWorthOpening()).isEqualTo("是");
        assertThat(gpt.getFinalConclusion()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gpt.getCoreSupportingEvidence()).containsExactly("AI 证据已记录，需人工复核");
        assertThat(gpt.getCoreCounterEvidence()).isEmpty();
        assertThat(gpt.getDecisionSummary()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(gpt.getDowngradeReason()).isEqualTo("AI 发现证据冲突，需人工复核");
        assertThat(gpt.getDirection()).isEqualTo("BULLISH");
        assertThat(gpt.getSupportEvidence()).containsExactly("AI 证据已记录，需人工复核");
        assertThat(gpt.getReviewVerdict()).isNull();
        assertThat(gpt.getChallengeThesis()).isNull();
    }

    @Test
    void geminiReviewTabShowsConflictReviewFieldsOnly() {
        DecisionResultVO decision = decisionWithStructuredAiRoles();
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        DashboardHomeVO.AiTabVO gemini = aiTab(home, "GEMINI_REVIEW");
        assertThat(gemini.getReviewVerdict()).isEqualTo("提出反对意见");
        assertThat(gemini.getDetectedContradictions()).containsExactly("AI 发现证据冲突");
        assertThat(gemini.getWeakEvidence()).isEmpty();
        assertThat(gemini.getLogicGaps()).isEmpty();
        assertThat(gemini.getDowngradeRecommendation()).isNull();
        assertThat(gemini.getRiskAdjustmentSuggestion()).isNull();
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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        DashboardHomeVO.AiTabVO grok = aiTab(home, "GROK_CHALLENGE");
        assertThat(grok.getChallengeThesis()).isEqualTo("AI 复核结果已返回，等待人工复核");
        assertThat(grok.getEventRisks()).isEmpty();
        assertThat(grok.getSentimentReversalRisks()).isEmpty();
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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(malformed, raw));

        DashboardHomeVO malformedHome = service.getHome("BTCUSDT", 6);
        DashboardHomeVO rawHome = service.getHome("ETHUSDT", 6);

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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.AiTabVO tab = aiTab(service.getHome("BTCUSDT", 6), "GPT_FINAL");

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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getAiDecision().getRunStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertConsistencyNotApplicable(home);
    }

    @Test
    void allThreeSuccessfulAbstainMakeConsistencyNotApplicable() {
        DecisionResultVO decision = allAbstainDecision();
        decision.setConfusedScore(100);
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getAiDecision().getRunStatus()).isEqualTo("SUCCESS");
        assertThat(home.getAiDecision().getRunStatusLabel()).isEqualTo("复核成功");
        assertThat(home.getAiDecision().getDecisionModeLabel()).isEqualTo("AI 复核无可裁决结论");
        assertThat(home.getAiDecision().getTabs()).allSatisfy(tab -> {
            assertThat(tab.getRunStatusLabel()).isEqualTo("复核成功");
            assertThat(tab.getReviewConclusion()).isEqualTo("证据不足，暂不判断");
        });
        assertThat(home.getAiDecision().getConsistency().getDirectionalPushBlocked()).isTrue();
        assertConsistencyNotApplicable(home);
    }

    @Test
    void supportPlusAbstainUsesOnlySupportVote() {
        DecisionResultVO decision = decisionWithRoles(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "支持"),
                role(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        AiReviewStance.ABSTAIN, "INSUFFICIENT_DATA", "弃权")));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getAiDecision().getConsistency().getAiApplicable()).isTrue();
        assertThat(home.getSystemState().getAiConflict().getValue()).isEqualTo("LEVEL_2_REVIEW");
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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getAiDecision().getConsistency().getAiApplicable()).isTrue();
        assertThat(home.getSystemState().getAiConflict().getValueLabel()).isEqualTo("轻微分歧");
        assertThat(aiTab(home, "GEMINI_REVIEW").getStance()).isEqualTo("CHALLENGE");
        assertThat(aiTab(home, "GROK_CHALLENGE").getStance()).isEqualTo("ABSTAIN");
    }

    @Test
    void allAbstainKpiShowsNotApplicable() {
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(allAbstainDecision()));

        DashboardHomeVO.StatusCardVO card = service.getHome("BTCUSDT", 6).getSystemState().getAiConflict();

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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.StatusCardVO card = service.getHome("BTCUSDT", 6).getSystemState().getAiConflict();

        assertThat(card.getValue()).isEqualTo("LEVEL_2_REVIEW");
        assertThat(card.getValueLabel()).isEqualTo("轻微分歧");
    }

    @Test
    void controllerSerializesLocalizedAiConflictKpiFromServiceOutput() throws Exception {
        DecisionResultVO decision = decisionWithRoles(List.of(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "RULE_DIRECTION_ALIGNED", "支持")));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DashboardHomeController(service)).build();

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemState.aiConflict.value").value("LEVEL_2_REVIEW"))
                .andExpect(jsonPath("$.data.systemState.aiConflict.valueLabel").value("轻微分歧"));
    }

    @Test
    void headerDisabledAiShowsChineseDisabledLabel() {
        when(providerReadinessService.getReadiness())
                .thenReturn(providerReadiness("CONFIGURED", "DISABLED", "WAITING_SYNC", "真实行情"));

        DashboardHomeVO.HeaderVO header = service.getHome("BTCUSDT", 6).getHeader();

        assertThat(header.getAiStatus()).isEqualTo("DISABLED");
        assertThat(header.getAiStatusLabel()).isEqualTo("已禁用");
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

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(btc, eth));

        DashboardHomeVO ethHome = service.getHome("ETHUSDT", 6);
        DashboardHomeVO defaultHome = service.getHome(null, 6);

        assertThat(aiTab(ethHome, "GPT_FINAL").getSupportEvidence()).containsExactly("证据不足");
        assertThat(aiTab(defaultHome, "GPT_FINAL").getSupportEvidence()).containsExactly("规则方向与 AI 复核一致");
    }

    @Test
    void adjudicationConsistencyHasExplicitBackendContract() {
        DecisionResultVO decision = decisionWithStructuredAiRoles();
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        DashboardHomeVO.ConsistencyVO consistency = home.getAiDecision().getConsistency();
        assertThat(consistency.getLevel()).isEqualTo("LEVEL_2_REVIEW");
        assertThat(consistency.getScore()).isEqualTo(25);
        assertThat(consistency.getConsistencyScore()).isNull();
        assertThat(consistency.getConsistencyLevel()).isEqualTo("轻微分歧");
        assertThat(consistency.getConsistencySummary()).isEqualTo("基于本轮成功返回的 AI 角色形成一致性摘要");
        assertThat(consistency.getDowngradeReason()).isEqualTo("AI 发现证据冲突，需人工复核");
    }

    @Test
    void adjudicationConsistencyDoesNotFakeScore() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 80,
                "LEVEL_4_EXTREME_DIVERGENCE", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults(structuredAiRoleResults(
                List.of(role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.CHALLENGE, "CONFLICT_TOO_HIGH", "GPT challenge")),
                synthesis("BULLISH", "LOW", "HIGH", "CONFUSED", false, "CONFLICT_TOO_HIGH")));

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        DashboardHomeVO.ConsistencyVO consistency = home.getAiDecision().getConsistency();
        assertThat(consistency.getScore()).isEqualTo(80);
        assertThat(consistency.getConsistencyScore()).isNull();
        assertThat(consistency.getScore()).isNotEqualTo(20);
        assertThat(consistency.getDowngradeReason()).isEqualTo("冲突程度较高，需人工复核");
    }

    @Test
    void authoritativeAssetStateRowOverridesDecisionSnapshotForAllEightStates() {
        DecisionResultVO decision = decision("BTCUSDT", "RANGE", "LOW", "MEDIUM", 80, 0,
                null, false, "{\"state\":\"OBSERVING\"}");
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));
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

            DashboardHomeVO.AssetVO asset = asset(service.getHome("BTCUSDT", 6), "BTC/USDT");

            assertThat(asset.getAssetState()).isEqualTo(states[i].name());
            assertThat(asset.getAssetStateLabel()).isEqualTo(labels[i]);
        }
    }

    @Test
    void lowDataQualityAndMissingAnalysisSnapshotHideEveryPlanBoundary() {
        DecisionResultVO lowQuality = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 59, 0,
                null, true, "{\"state\":\"CANDIDATE\"}");
        lowQuality.setEntryZone("100-101");
        lowQuality.setStopLoss("95");
        lowQuality.setTakeProfitRules("110 / 115");
        lowQuality.setValidPeriod("12h");
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(lowQuality));

        DashboardHomeVO.ExecutionSuggestionVO blocked = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

        assertThat(blocked.getStatus()).isEqualTo("DATA_QUALITY_BLOCKED");
        assertThat(blocked.getEntryZone()).isNull();
        assertThat(blocked.getStopLoss()).isNull();
        assertThat(blocked.getTakeProfitRules()).isNull();

        lowQuality.setDataQualityScore(80);
        lowQuality.setAnalysisId(null);
        DashboardHomeVO.ExecutionSuggestionVO missingSnapshot = service.getHome("BTCUSDT", 6).getExecutionSuggestion();
        assertThat(missingSnapshot.getStatus()).isEqualTo("ANALYSIS_SNAPSHOT_MISSING");
        assertThat(missingSnapshot.getDirection()).isNull();
        assertThat(missingSnapshot.getEntryZone()).isNull();
    }

    @Test
    void zeroSuccessfulAiRolesRemainNotApplicableAndRuleOnly() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "MEDIUM", 80, 72,
                "LEVEL_3_DIVERGENCE", true, "{\"state\":\"CANDIDATE\"}");
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.AiDecisionVO ai = service.getHome("BTCUSDT", 6).getAiDecision();

        assertThat(ai.getRunStatus()).isEqualTo("NOT_CALLED");
        assertThat(ai.getDecisionModeLabel()).isEqualTo("仅规则判断");
        assertThat(ai.getConsistency().getLevel()).isNull();
        assertThat(ai.getConsistency().getScore()).isNull();
        assertThat(ai.getConsistency().getConsistencyLevel()).isEqualTo("不适用");
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
            when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

            DashboardHomeVO.ConsistencyVO consistency = service.getHome("BTCUSDT", 6)
                    .getAiDecision().getConsistency();

            assertThat(consistency.getAiApplicable()).isFalse();
            assertThat(consistency.getConsistencyLevel()).isEqualTo("不适用");
            assertThat(consistency.getLevel()).isNull();
            assertThat(consistency.getScore()).isNull();
            assertThat(consistency.getConfused()).isFalse();
            assertThat(consistency.getDirectionalPushBlocked()).isTrue();
        }
    }

    @Test
    void expiredAbsoluteValidPeriodBlocksSuggestion() {
        DecisionResultVO decision = completePlanDecision("BTCUSDT", ACTIVE_VALID_PERIOD);
        setActivePlanValidity(decision);
        allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO beforeExpiry = service.getHome("BTCUSDT", 6)
                .getExecutionSuggestion();
        assertThat(beforeExpiry.getStatus()).isEqualTo("USABLE_REVIEW_PLAN");
        assertThat(beforeExpiry.getValidFrom()).hasToString("2026-07-01T00:00Z");
        assertThat(beforeExpiry.getExpiresAt()).hasToString("2026-07-02T00:00Z");

        service.setPlanValidityClock(Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC));
        DashboardHomeVO.ExecutionSuggestionVO atExpiry = service.getHome("BTCUSDT", 6)
                .getExecutionSuggestion();
        assertThat(atExpiry.getStatus()).isEqualTo("PLAN_EXPIRED");

        service.setPlanValidityClock(Clock.fixed(Instant.parse("2026-07-02T00:00:01Z"), ZoneOffset.UTC));
        DashboardHomeVO.ExecutionSuggestionVO afterExpiry = service.getHome("BTCUSDT", 6)
                .getExecutionSuggestion();
        assertThat(afterExpiry.getStatus()).isEqualTo("PLAN_EXPIRED");
        assertThat(afterExpiry.getEntryZone()).isNull();
    }

    @Test
    void malformedValidPeriodFailsClosed() {
        DecisionResultVO decision = completePlanDecision(
                "BTCUSDT", "2026/07/01 00:00:00 - 2026/07/02 00:00:00");
        allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(suggestion.getStatus()).isEqualTo("VALID_PERIOD_INVALID");
        assertThat(suggestion.getBlockedReason()).isEqualTo("有效期格式异常，等待重新分析");
        assertThat(suggestion.getValidFrom()).isNull();
        assertThat(suggestion.getExpiresAt()).isNull();
    }

    @Test
    void legacyNoOffsetPlanFailsClosed() {
        DecisionResultVO decision = completePlanDecision(
                "BTCUSDT", "2026-07-01 00:00:00 ~ 2026-07-02 00:00:00");
        allowMatchingSnapshot(decision);
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6)
                .getExecutionSuggestion();

        assertThat(suggestion.getStatus()).isEqualTo("LEGACY_TIMEZONE_UNVERIFIED");
        assertThat(suggestion.getBlockedReason()).isEqualTo("历史计划时区不可验证，需重新分析");
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
                when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

                DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6)
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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(decisions);

        DashboardHomeVO.StatusCardVO thresholdFallback = service.getHome("BTCUSDT", 6)
                .getSystemState().getConfused();
        assertThat(thresholdFallback.getValue()).isEqualTo(1);

        when(decisionService.getLightSystemStatus()).thenThrow(new IllegalStateException("status unavailable"));
        DashboardHomeVO.StatusCardVO unavailable = service.getHome("BTCUSDT", 6)
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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

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
        assertThat(consistency.getAiApplicable()).isFalse();
        assertThat(consistency.getLevel()).isNull();
        assertThat(consistency.getScore()).isNull();
        assertThat(consistency.getConsistencyLevel()).isEqualTo("不适用");
        assertThat(consistency.getConsistencySummary())
                .isEqualTo("AI 成功返回，但所有角色均因证据不足而弃权");
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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO.AiTabVO tab = aiTab(service.getHome("BTCUSDT", 6), "GPT_FINAL");

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
        position.setSourceType("MANUAL");
        position.setSourceRefId(sourceRefId);
        return position;
    }

    private void wireCapturedMonitorLogs(long positionId, List<PositionMonitorLogDTO> capturedLogs) {
        when(positionMonitorLogService.listByPositionId(positionId, 1))
                .thenAnswer(invocation -> capturedLogs.isEmpty()
                        ? List.of()
                        : List.of(capturedLogs.get(capturedLogs.size() - 1)));
        when(positionMonitorLogService.recordMonitorRun(any())).thenAnswer(invocation -> {
            RecordPositionMonitorLogCommand command = invocation.getArgument(0);
            PositionMonitorLogDTO log = new PositionMonitorLogDTO();
            log.setLogId((long) capturedLogs.size() + 1);
            log.setPositionId(command.getPositionId());
            log.setAnalysisId(command.getAnalysisId());
            log.setExecutionPlanId(command.getExecutionPlanId());
            log.setCurrentPrice(command.getCurrentPrice());
            log.setLogicStatus(command.getLogicStatus());
            log.setRiskLevel(command.getRiskLevel());
            log.setSuggestedAction(command.getSuggestedAction());
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
        position.setSourceType("MANUAL");
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
        monitor.setLogicStatus("LOGIC_VALID");
        when(positionMonitorLogService.listByPositionId(position.getId(), 1)).thenReturn(List.of(monitor));
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
        when(positionMonitorLogService.listByPositionId(position.getId(), 1)).thenReturn(List.of(monitor));
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
        monitor.setLogicStatus("LOGIC_VALID");
        return monitor;
    }

    private ExecutionPlanDO validExecutionPlan(String planId, String analysisId) {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(planId);
        plan.setAnalysisId(analysisId);
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(true);
        plan.setNeedsRevalidation(false);
        return plan;
    }

    private void copyExactPlanFields(ExecutionPlanDO plan, DecisionResultVO decision) {
        plan.setEntryZone(decision.getEntryZone());
        plan.setStopLoss(decision.getStopLoss());
        plan.setTakeProfitRules(decision.getTakeProfitRules());
        plan.setLeverageSuggestion(decision.getLeverageSuggestion());
        plan.setPositionSuggestion(decision.getPositionSuggestion());
        plan.setInvalidCondition(decision.getInvalidCondition());
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
        when(monitorPositionMapper.selectById(positionId)).thenReturn(positionDO);
        when(quoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(monitorQuote("100")));
        when(riskAdapter.currentRisk()).thenReturn(allowedMonitorRisk());

        DecisionResultVO sourceDecision = sourcePlanDecision(analysisId, "BTCUSDT", "AGREEMENT-A");
        ExecutionPlanDO plan = validExecutionPlan(planId, analysisId);
        copyExactPlanFields(plan, sourceDecision);
        planMutation.accept(plan);
        when(executionPlanMapper.selectByPlanId(planId)).thenReturn(plan);
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

        PositionMonitorResultDTO monitorResult = monitorService.monitorUserPosition(positionId);

        assertThat(monitorResult.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
        assertThat(monitorResult.getSuggestedAction()).isEqualTo("RECHECK_PLAN");
        assertThat(monitorResult.getReasonCodes()).contains(expectedReasonCode);
        assertThat(capturedLogs).singleElement().satisfies(log -> {
            assertThat(log.getLogicStatus()).isEqualTo("LOGIC_WEAKENED");
            assertThat(log.getSuggestedAction()).isEqualTo("RECHECK_PLAN");
            assertThat(log.getAnalysisId()).isEqualTo(analysisId);
            assertThat(log.getExecutionPlanId()).isEqualTo(planId);
        });

        when(decisionResultMapper.findByAnalysisIdAndPlanIdJoined(analysisId, planId))
                .thenReturn(sourceDecision);
        when(userPositionService.listOpenPositions()).thenReturn(List.of(positionVO));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(sourceDecision));
        when(assetStateMapper.selectBySymbol("BTCUSDT"))
                .thenReturn(sourceState("BTCUSDT", traceId));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = service.getHome("BTCUSDT", 6).getExecutionSuggestion();

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
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        return new OriginalPlanFixture(position, plan, sourceDecision);
    }

    private OriginalPlanFixture originalPlanFixtureWithLatestB(Long positionId, String marker) {
        OriginalPlanFixture fixture = originalPlanFixture(positionId, marker);
        DecisionResultVO latestB = sourcePlanDecision("analysis-latest-B", "BTCUSDT", "LATEST-B");
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(latestB));
        return fixture;
    }

    private MultiPositionFixture twoSameSymbolPositions() {
        UserPositionVO positionA = activeManualPosition(3411L, "BTCUSDT", null);
        UserPositionVO positionB = activeManualPosition(3412L, "BTCUSDT", null);
        DecisionResultVO planA = sourcePlanDecision("analysis-position-A", "BTCUSDT", "POSITION-A");
        DecisionResultVO planB = sourcePlanDecision("analysis-position-B", "BTCUSDT", "POSITION-B");
        stubMonitorExecutionPlanSource(positionA, planA, "plan-position-A", "trace-shared", "trace-shared");
        stubMonitorExecutionPlanSource(positionB, planB, "plan-position-B", "trace-shared", "trace-shared");
        when(userPositionService.listOpenPositions()).thenReturn(List.of(positionA, positionB));
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(planB));
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

    private void allowMatchingSnapshot(DecisionResultVO decision) {
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
        monitor.setLogicStatus("LOGIC_VALID");
        when(positionMonitorLogService.listByPositionId(position.getId(), 1)).thenReturn(List.of(monitor));

        ExecutionPlanDO plan = validExecutionPlan(planId, sourceDecision.getAnalysisId());
        copyExactPlanFields(plan, sourceDecision);
        when(executionPlanMapper.selectByPlanId(planId)).thenReturn(plan);
        when(decisionResultMapper.findByAnalysisIdAndPlanIdJoined(sourceDecision.getAnalysisId(), planId))
                .thenReturn(sourceDecision);

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
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId("analysis-dashboard-ai");
        result.setTraceId("trace-dashboard-ai");
        result.setProviderResults(roleResults);
        return aiRoleResultsCodec.serialize(result, "v1.0", synthesis);
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
        return new AiRoleResultsPayload.SynthesisPayload(
                direction, confidence, risk, worthOpening,
                "LEVEL_2_LIGHT_DIVERGENCE", 25,
                confidence, "SLIGHTLY_RAISED", planMode, false,
                downgradeReason);
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
