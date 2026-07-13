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
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.derivatives.DerivativesSnapshotReadPort;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserConfigDO;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
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

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

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
        allowMatchingSnapshot(btc);

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
        when(positionMonitorLogService.listByPositionId(9L, 1)).thenReturn(List.of());
        when(positionSyncService.getPositionSyncStatus()).thenReturn(sync);
        when(pushSnapshotMapper.countPendingRecheckBacklog()).thenReturn(7);
        when(pushSnapshotMapper.listPendingRecheck(anyString(), anyInt())).thenReturn(List.of());

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getSystemState().getPendingReview().getValue()).isEqualTo(4);
        assertThat(home.getSystemState().getPendingReview().getValue()).isNotEqualTo(99);
        assertThat(home.getSystemState().getDataQuality().getValue()).isEqualTo(88);
        assertThat(home.getSystemState().getDataQuality().getHelper()).isEqualTo("选中资产分析快照");
        assertThat(home.getSystemState().getRiskLevel().getValue()).isEqualTo("HIGH");
        assertThat(home.getSystemState().getRiskLevel().getHelper()).isEqualTo("选中资产决策风险");
        assertThat(home.getSystemState().getMarketTrend().getValue()).isEqualTo("BULLISH");
        assertThat(home.getSystemState().getAiConflict().getValue()).isEqualTo("LEVEL_2_REVIEW");
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
        assertThat(homePosition.getMonitorConclusion()).isNull();
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
        when(pushSnapshotMapper.countPendingRecheckBacklog()).thenReturn(3);
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

        when(pushSnapshotMapper.countPendingRecheckBacklog()).thenReturn(1);
        when(pushSnapshotMapper.listPendingRecheck("CAPTURED", 6)).thenReturn(List.of(snapshot));
        when(pushSnapshotMapper.listPendingRecheck("RECHECK_REVIEW_WAITING", 5)).thenReturn(List.of());
        when(pushSnapshotMapper.listPendingRecheck("RECHECK_VALID_WAITING", 5)).thenReturn(List.of());
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

    private void allowMatchingSnapshot(DecisionResultVO decision) {
        String traceId = "trace-" + decision.getAnalysisId();
        AssetStateDO state = new AssetStateDO();
        state.setSymbol(decision.getSymbol());
        state.setState(AssetStateEnum.CANDIDATE);
        state.setTraceId(traceId);
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(decision.getAnalysisId());
        run.setTraceId(traceId);
        when(assetStateMapper.selectBySymbol(decision.getSymbol())).thenReturn(state);
        when(analysisRunMapper.selectById(decision.getAnalysisId())).thenReturn(run);
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
