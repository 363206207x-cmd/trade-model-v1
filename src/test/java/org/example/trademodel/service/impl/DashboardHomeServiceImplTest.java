package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
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
        position.setUpdatedAt(LocalDateTime.of(2026, 6, 27, 2, 0));

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
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
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
        assertThat(home.getPositions().get(0).getLeverage()).isEqualByComparingTo("2");
        assertThat(home.getPositions().get(0).getCurrentPrice()).isNull();
        assertThat(home.getPositions().get(0).getMonitorConclusion()).isNull();
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

}
