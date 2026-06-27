package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.support.ExternalContextEvidenceBuilder;
import org.example.trademodel.vo.DashboardHomeVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.PositionSyncStatusVO;
import org.example.trademodel.vo.UserPositionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardHomeServiceImplTest {
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
    private ExternalContextEvidenceBuilder externalContextEvidenceBuilder;

    private DashboardHomeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardHomeServiceImpl(
                decisionService,
                monitorService,
                userPositionService,
                positionMonitorLogService,
                positionSyncService,
                pushSnapshotMapper,
                externalContextEvidenceBuilder,
                new ObjectMapper()
        );
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
        btc.setValidPeriod("12h");
        btc.setInvalidCondition("跌破 61000");
        btc.setAiRoleResults("""
                {
                  "GPT_FINAL": {
                    "supportEvidence": ["规则方向一致"],
                    "againstEvidence": ["事件窗口待复核"],
                    "riskPoints": ["高波动"],
                    "reviewConclusion": "保持人工复核"
                  },
                  "GEMINI_REVIEW": {},
                  "GROK_CHALLENGE": {}
                }
                """);

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
        assertThat(home.getSystemState().getDataQuality().getValue()).isEqualTo(87);
        assertThat(home.getSystemState().getDataQuality().getHelper()).isEqualTo("摘要均值");
        assertThat(home.getSystemState().getRiskLevel().getValue()).isEqualTo("EXTREME");
        assertThat(home.getSystemState().getRiskLevel().getHelper()).isEqualTo("决策风险");
        assertThat(home.getSystemState().getMarketTrend().getValue()).isEqualTo("BULLISH");
        assertThat(home.getSystemState().getAiConflict().getValue()).isEqualTo("LEVEL_4_EXTREME_DIVERGENCE");
        assertThat(home.getSystemState().getAiConflict().getScore()).isEqualTo(80);

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
        assertThat(home.getExecutionSuggestion().getEntryZone()).isEqualTo("63000-64000");
        assertThat(home.getExecutionSuggestion().getStopLoss()).isEqualTo("61000");
        assertThat(home.getExecutionSuggestion().getTakeProfitRules()).isEqualTo("66000 / 69000");
        assertThat(home.getExecutionSuggestion().getLeverageSuggestion()).isEqualTo("20x");
        assertThat(home.getAiDecision().getActiveTab()).isEqualTo("GPT_FINAL");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs().get(0).getSupportEvidence()).containsExactly("规则方向一致");
        assertThat(home.getPushInbox().getCounts().getWaiting()).isEqualTo(7);
        assertThat(home.getSafety().getNotTradeInstruction()).isTrue();
        assertThat(home.getSafety().getNotAutoTrading()).isTrue();
        assertThat(home.getSafety().getNotOrderExecution()).isTrue();
    }

    @Test
    void homePositionUsesLatestPersistedMonitorLogWithoutCalculatingPnl() {
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
        monitorLog.setLogicStatus("FOLLOW_PLAN");

        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(positionMonitorLogService.listByPositionId(9L, 1)).thenReturn(List.of(monitorLog));

        DashboardHomeVO home = service.getHome(null, 6);

        assertThat(home.getPositions()).hasSize(1);
        DashboardHomeVO.PositionVO homePosition = home.getPositions().get(0);
        assertThat(homePosition.getCurrentPrice()).isEqualByComparingTo("63500");
        assertThat(homePosition.getMonitorConclusion()).isEqualTo("FOLLOW_PLAN");
        assertThat(homePosition.getFloatingPnl()).isNull();
    }

    @Test
    void selectedSymbolDrivesExecutionSuggestionWithoutCrossFieldFallbacks() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setEntryZone("BTC entry");
        btc.setStopLoss("BTC stop");
        btc.setTakeProfitRules("BTC take profit");
        btc.setLeverageSuggestion("20x");
        btc.setPositionSuggestion("10%");
        btc.setValidPeriod("12h");
        btc.setInvalidCondition("BTC invalid");

        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "MEDIUM", 70, 10,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        eth.setEntryZone("ETH entry");
        eth.setStopLoss("ETH stop");
        eth.setTakeProfitRules("ETH take profit");
        eth.setLeverageSuggestion("3x");
        eth.setPositionSuggestion("5%");
        eth.setValidPeriod("6h");
        eth.setInvalidCondition("ETH invalid");

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(btc, eth));
        when(userPositionService.listOpenPositions()).thenReturn(List.of());

        DashboardHomeVO ethHome = service.getHome("ETHUSDT", 6);

        assertThat(ethHome.getSelectedSymbol()).isEqualTo("ETHUSDT");
        assertThat(ethHome.getExecutionSuggestion().getDirection()).isEqualTo("BEARISH");
        assertThat(ethHome.getExecutionSuggestion().getEntryZone()).isEqualTo("ETH entry");
        assertThat(ethHome.getExecutionSuggestion().getStopLoss()).isEqualTo("ETH stop");
        assertThat(ethHome.getExecutionSuggestion().getTakeProfitRules()).isEqualTo("ETH take profit");
        assertThat(ethHome.getExecutionSuggestion().getLeverageSuggestion()).isEqualTo("3x");
        assertThat(ethHome.getExecutionSuggestion().getPositionSuggestion()).isEqualTo("5%");
        assertThat(ethHome.getExecutionSuggestion().getValidPeriod()).isEqualTo("6h");
        assertThat(ethHome.getExecutionSuggestion().getInvalidCondition()).isEqualTo("ETH invalid");

        DashboardHomeVO defaultHome = service.getHome(null, 6);

        assertThat(defaultHome.getSelectedSymbol()).isEqualTo("BTCUSDT");
        assertThat(defaultHome.getExecutionSuggestion().getEntryZone()).isEqualTo("BTC entry");
        assertThat(defaultHome.getExecutionSuggestion().getValidPeriod()).isEqualTo("12h");
    }

    @Test
    void aiDecisionMapsStructuredRoleEvidenceOnly() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults("""
                {
                  "roles": [
                    {
                      "role": "GPT_FINAL",
                      "direction": "BULLISH",
                      "confidence": "HIGH",
                      "supportEvidence": ["规则方向一致", "量能确认"],
                      "againstEvidence": "事件窗口待复核",
                      "riskWarnings": ["高波动"],
                      "downgradeOrBlockReason": "等待事件落地",
                      "finalOpinion": "保持人工复核"
                    },
                    {
                      "providerRole": "GEMINI_CONSISTENCY_REVIEW",
                      "bias": "RANGE",
                      "confidence_level": "MEDIUM",
                      "positiveEvidence": "一致性支持",
                      "negativeEvidence": ["结构分歧"],
                      "risks": "假突破",
                      "rejectReason": "冲突降级",
                      "decisionSummary": "保持观察"
                    },
                    {
                      "aiRole": "GROK_ADVERSARIAL_CHALLENGE",
                      "finalDirection": "BEARISH",
                      "confidenceLevel": "LOW",
                      "coreSupportEvidence": ["反方压力"],
                      "counterEvidence": "趋势未确认",
                      "risk_points": ["流动性不足"],
                      "blockReason": "反方阻断",
                      "reviewConclusion": "挑战成立"
                    }
                  ]
                }
                """);

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getAiDecision().getActiveTab()).isEqualTo("GPT_FINAL");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRoleLabel)
                .containsExactly("最终裁决官", "冲突复核官", "反方挑战官");

        DashboardHomeVO.AiTabVO gpt = aiTab(home, "GPT_FINAL");
        assertThat(gpt.getDirection()).isEqualTo("BULLISH");
        assertThat(gpt.getConfidenceLevel()).isEqualTo("HIGH");
        assertThat(gpt.getSupportEvidence()).containsExactly("规则方向一致", "量能确认");
        assertThat(gpt.getAgainstEvidence()).containsExactly("事件窗口待复核");
        assertThat(gpt.getRiskPoints()).containsExactly("高波动");
        assertThat(gpt.getDowngradeReason()).isEqualTo("等待事件落地");
        assertThat(gpt.getReviewConclusion()).isEqualTo("保持人工复核");

        DashboardHomeVO.AiTabVO gemini = aiTab(home, "GEMINI_REVIEW");
        assertThat(gemini.getDirection()).isEqualTo("RANGE");
        assertThat(gemini.getConfidenceLevel()).isEqualTo("MEDIUM");
        assertThat(gemini.getSupportEvidence()).containsExactly("一致性支持");
        assertThat(gemini.getAgainstEvidence()).containsExactly("结构分歧");
        assertThat(gemini.getRiskPoints()).containsExactly("假突破");
        assertThat(gemini.getDowngradeReason()).isEqualTo("冲突降级");
        assertThat(gemini.getReviewConclusion()).isEqualTo("保持观察");

        DashboardHomeVO.AiTabVO grok = aiTab(home, "GROK_CHALLENGE");
        assertThat(grok.getDirection()).isEqualTo("BEARISH");
        assertThat(grok.getConfidenceLevel()).isEqualTo("LOW");
        assertThat(grok.getSupportEvidence()).containsExactly("反方压力");
        assertThat(grok.getAgainstEvidence()).containsExactly("趋势未确认");
        assertThat(grok.getRiskPoints()).containsExactly("流动性不足");
        assertThat(grok.getDowngradeReason()).isEqualTo("反方阻断");
        assertThat(grok.getReviewConclusion()).isEqualTo("挑战成立");
    }

    @Test
    void aiDecisionReturnsThreeEmptyTabsWhenRoleDataMissing() {
        DecisionResultVO decision = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        decision.setAiRoleResults("{\"role\":\"UNKNOWN\",\"supportEvidence\":[\"不应映射\"],\"summary\":\"不应映射\"}");

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs()).allSatisfy(tab -> {
            assertThat(tab.getDirection()).isNull();
            assertThat(tab.getConfidenceLevel()).isNull();
            assertThat(tab.getSupportEvidence()).isEmpty();
            assertThat(tab.getAgainstEvidence()).isEmpty();
            assertThat(tab.getRiskPoints()).isEmpty();
            assertThat(tab.getDowngradeReason()).isNull();
            assertThat(tab.getReviewConclusion()).isNull();
        });
    }

    @Test
    void aiDecisionMalformedAndRawTextDoNotFabricateEvidence() {
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
        decision.setAiRoleResults("{\"GPT_FINAL\":{\"summary\":\"只映射显式结论\"}}");

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        DashboardHomeVO.AiTabVO gpt = aiTab(home, "GPT_FINAL");
        assertThat(gpt.getReviewConclusion()).isEqualTo("只映射显式结论");
        assertThat(gpt.getAgainstEvidence()).isEmpty();
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .doesNotContain("裁决", "FINAL", "AI_SUMMARY");
    }

    @Test
    void selectedSymbolDrivesAiDecisionEvidence() {
        DecisionResultVO btc = decision("BTCUSDT", "BULLISH", "HIGH", "HIGH", 88, 25,
                "LEVEL_2_REVIEW", true, "{\"state\":\"CANDIDATE\"}");
        btc.setAiRoleResults("{\"GPT_FINAL\":{\"supportEvidence\":[\"BTC evidence\"]}}");

        DecisionResultVO eth = decision("ETHUSDT", "BEARISH", "MEDIUM", "MEDIUM", 70, 10,
                "LEVEL_1", false, "{\"state\":\"OBSERVING\"}");
        eth.setAiRoleResults("{\"GPT_FINAL\":{\"supportEvidence\":[\"ETH evidence\"]}}");

        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(btc, eth));

        DashboardHomeVO ethHome = service.getHome("ETHUSDT", 6);
        DashboardHomeVO defaultHome = service.getHome(null, 6);

        assertThat(aiTab(ethHome, "GPT_FINAL").getSupportEvidence()).containsExactly("ETH evidence");
        assertThat(aiTab(defaultHome, "GPT_FINAL").getSupportEvidence()).containsExactly("BTC evidence");
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

    private void assertNoAiEvidence(DashboardHomeVO home) {
        assertThat(home.getAiDecision().getTabs()).extracting(DashboardHomeVO.AiTabVO::getRole)
                .containsExactly("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(home.getAiDecision().getTabs()).allSatisfy(tab -> {
            assertThat(tab.getDirection()).isNull();
            assertThat(tab.getConfidenceLevel()).isNull();
            assertThat(tab.getSupportEvidence()).isEmpty();
            assertThat(tab.getAgainstEvidence()).isEmpty();
            assertThat(tab.getRiskPoints()).isEmpty();
            assertThat(tab.getDowngradeReason()).isNull();
            assertThat(tab.getReviewConclusion()).isNull();
        });
    }

}
