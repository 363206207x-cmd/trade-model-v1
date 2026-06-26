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

        DecisionResultVO decision = new DecisionResultVO();
        decision.setSymbol("BTCUSDT");
        decision.setTimeframe("1h");
        decision.setMarketBiasHierarchy("BULLISH");
        decision.setRiskLevel("HIGH");
        decision.setConfidenceLevel("HIGH");
        decision.setDataQualityScore(88);
        decision.setAiConflictLevel("LEVEL_2_REVIEW");
        decision.setAiConflictScore(25);
        decision.setIsWorthOpening(true);
        decision.setEntryZone("63000-64000");
        decision.setStopLoss("61000");
        decision.setTakeProfitRules("66000 / 69000");
        decision.setLeverageSuggestion("20x");
        decision.setPositionSuggestion("10%");
        decision.setValidPeriod("12h");
        decision.setInvalidCondition("跌破 61000");
        decision.setAiRoleResults("""
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
        when(decisionService.getLatestDecisionResults(anyInt())).thenReturn(List.of(decision));
        when(monitorService.getRecentAlerts(2)).thenReturn(List.of(alert));
        when(userPositionService.listOpenPositions()).thenReturn(List.of(position));
        when(positionSyncService.getPositionSyncStatus()).thenReturn(sync);
        when(pushSnapshotMapper.countPendingRecheckBacklog()).thenReturn(7);
        when(pushSnapshotMapper.listPendingRecheck(anyString(), anyInt())).thenReturn(List.of());

        DashboardHomeVO home = service.getHome("BTCUSDT", 6);

        assertThat(home.getSystemState().getPendingReview().getValue()).isEqualTo(4);
        assertThat(home.getSystemState().getPendingReview().getValue()).isNotEqualTo(99);
        assertThat(home.getSystemState().getRiskLevel().getValue()).isEqualTo("HIGH");
        assertThat(home.getSystemState().getRiskLevel().getHelper()).isEqualTo("决策风险");
        assertThat(home.getSystemState().getAiConflict().getScore()).isEqualTo(25);
        assertThat(home.getAssets()).hasSize(6);
        assertThat(home.getAssets().get(0).getCompositeScore()).isNull();
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
}
