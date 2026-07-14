package org.example.trademodel.service.impl;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class DecisionServiceImplTest {

    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;
    @Mock
    private MarketQuoteClient marketQuoteClient;
    @Mock
    private UserPositionMapper userPositionMapper;
    @Mock
    private AssetStateService assetStateService;
    @Mock
    private AssetStateMapper assetStateMapper;
    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private MissedOpportunityMapper missedOpportunityMapper;

    private DecisionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DecisionServiceImpl(
                decisionResultMapper,
                analysisRunMapper,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                userPositionMapper,
                assetStateService,
                assetStateMapper,
                pushSnapshotMapper,
                missedOpportunityMapper,
                new RuntimeMetricService()
        );
    }

    @Test
    void getLatestDecisionResults_preservesCoreDashboardTruthFields() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        populateCoreDashboardTruthFields(row);
        when(decisionResultMapper.findLatestDecisionResultsJoined(10)).thenReturn(List.of(row));
        when(userPositionMapper.listOpenPositions()).thenReturn(Collections.emptyList());
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        List<DecisionResultVO> result = service.getLatestDecisionResults(10);

        assertThat(result).hasSize(1);
        assertCoreDashboardTruthFields(result.get(0));
    }

    @Test
    void getLatestDecisionResults_marksPartialAndDoesNotInferClosedPosition() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        row.setValidPeriod(null);
        row.setInvalidCondition(null);
        row.setExplanationJson(null);
        row.setReviewReasons(null);
        row.setAiConflictLevel(null);
        row.setAiConflictScore(null);
        row.setConfusedScore(null);
        row.setAssetStateSnapshot(null);
        when(decisionResultMapper.findLatestDecisionResultsJoined(10)).thenReturn(List.of(row));
        when(userPositionMapper.listOpenPositions()).thenReturn(Collections.emptyList());
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        List<DecisionResultVO> result = service.getLatestDecisionResults(10);

        assertThat(result).hasSize(1);
        DecisionResultVO item = result.get(0);
        assertThat(item.getReadModelTruthStatus()).isEqualTo("PARTIAL");
        assertThat(item.getReadModelFallbackReason()).startsWith("LEGACY_MISSING:");
        assertThat(item.getHasOpenPosition()).isFalse();
        assertThat(item.getPositionStatus()).isNull();
    }

    @Test
    void getLatestDecisionResults_marksFullWhenRequiredFieldsPresent() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("ETHUSDT");
        row.setValidPeriod("next 4h");
        row.setInvalidCondition("price < 3100");
        row.setExplanationJson("{\"summary\":\"ok\"}");
        row.setReviewReasons("[\"r1\"]");
        row.setAiConflictLevel("L1");
        row.setAiConflictScore(10);
        row.setConfusedScore(5);
        row.setAssetStateSnapshot("{\"state\":\"ACTIVE\"}");
        when(decisionResultMapper.findLatestDecisionResultsJoined(5)).thenReturn(List.of(row));
        when(userPositionMapper.listOpenPositions()).thenReturn(Collections.emptyList());
        when(marketQuoteClient.fetch24hTicker("ETHUSDT")).thenReturn(Optional.empty());

        List<DecisionResultVO> result = service.getLatestDecisionResults(5);

        assertThat(result).hasSize(1);
        DecisionResultVO item = result.get(0);
        assertThat(item.getReadModelTruthStatus()).isEqualTo("FULL");
        assertThat(item.getReadModelFallbackReason()).isNull();
    }

    @Test
    void getLatestDecisionResults_clampsDashboardLimitForGuardrail() {
        when(decisionResultMapper.findLatestDecisionResultsJoined(24)).thenReturn(Collections.emptyList());
        when(userPositionMapper.listOpenPositions()).thenReturn(Collections.emptyList());

        List<DecisionResultVO> result = service.getLatestDecisionResults(200);

        assertThat(result).isEmpty();
        verify(decisionResultMapper).findLatestDecisionResultsJoined(24);
    }

    @Test
    void getLatestDecisionResultBySymbol_preservesCoreDashboardTruthFields() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        populateCoreDashboardTruthFields(row);
        when(decisionResultMapper.findLatestDecisionResultBySymbolJoined("BTCUSDT")).thenReturn(row);
        when(userPositionMapper.listOpenPositions()).thenReturn(Collections.emptyList());
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        DecisionResultVO result = service.getLatestDecisionResultBySymbol("btcusdt");

        assertThat(result).isNotNull();
        assertCoreDashboardTruthFields(result);
    }

    @Test
    void getLatestDecisionResultBySymbol_returnsNullForBlankSymbol() {
        DecisionResultVO row = service.getLatestDecisionResultBySymbol(" ");
        assertThat(row).isNull();
    }

    @Test
    void getLatestDecisionResultBySymbol_appliesSameReadModelRulesAsSummary() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        row.setValidPeriod(null);
        row.setInvalidCondition(null);
        row.setExplanationJson(null);
        row.setReviewReasons(null);
        row.setAiConflictLevel(null);
        row.setAiConflictScore(null);
        row.setConfusedScore(null);
        row.setAssetStateSnapshot(null);
        when(decisionResultMapper.findLatestDecisionResultBySymbolJoined("BTCUSDT")).thenReturn(row);
        when(userPositionMapper.listOpenPositions()).thenReturn(Collections.emptyList());
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        DecisionResultVO result = service.getLatestDecisionResultBySymbol("btcusdt");

        assertThat(result).isNotNull();
        assertThat(result.getReadModelTruthStatus()).isEqualTo("PARTIAL");
        assertThat(result.getReadModelFallbackReason()).startsWith("LEGACY_MISSING:");
        assertThat(result.getHasOpenPosition()).isFalse();
        assertThat(result.getPositionStatus()).isNull();
    }

    @Test
    void getLatestDecisionResultsDoesNotInferOpenPositionFromTriggeredDecisionWithoutManualUserPosition() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        row.setTradeType("TRIGGERED");
        row.setIsWorthOpening(Boolean.TRUE);
        row.setRecommendedAction("OPEN_LONG");
        when(decisionResultMapper.findLatestDecisionResultsJoined(10)).thenReturn(List.of(row));
        when(userPositionMapper.listOpenPositions()).thenReturn(Collections.emptyList());
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        List<DecisionResultVO> result = service.getLatestDecisionResults(10);

        assertThat(result).hasSize(1);
        DecisionResultVO item = result.get(0);
        assertThat(item.getHasOpenPosition()).isFalse();
        assertThat(item.getPositionStatus()).isNull();
        assertThat(item.getPositionSide()).isNull();
        assertThat(item.getAvgOpenPrice()).isNull();
        assertThat(item.getPositionQuantity()).isNull();
    }

    @Test
    void getLatestDecisionResultsUsesOnlyManualOpenUserPositionRowsForDashboardPositionFields() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 6, 22, 8, 30);
        DecisionResultVO manualDecision = new DecisionResultVO();
        manualDecision.setSymbol("BTCUSDT");
        DecisionResultVO executionPlanOnlyDecision = new DecisionResultVO();
        executionPlanOnlyDecision.setSymbol("ETHUSDT");
        executionPlanOnlyDecision.setTradeType("TRIGGERED");
        executionPlanOnlyDecision.setIsWorthOpening(Boolean.TRUE);
        executionPlanOnlyDecision.setRecommendedAction("OPEN_LONG");
        executionPlanOnlyDecision.setEntryZone("3000-3050");
        executionPlanOnlyDecision.setStopLoss("2900");
        executionPlanOnlyDecision.setTakeProfitRules("3200");
        UserPositionDO manualOpen = manualUserPosition("BTCUSDT", "OPEN", openedAt);
        UserPositionDO manualClosed = manualUserPosition("ETHUSDT", "CLOSED", LocalDateTime.of(2026, 6, 22, 8, 10));
        UserPositionDO pushRecheckCreatedSurface = manualUserPosition("ETHUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 20));
        pushRecheckCreatedSurface.setSourceType("PUSH_RECHECK");

        when(decisionResultMapper.findLatestDecisionResultsJoined(10)).thenReturn(List.of(manualDecision, executionPlanOnlyDecision));
        when(userPositionMapper.listOpenPositions()).thenReturn(List.of(manualOpen, manualClosed, pushRecheckCreatedSurface));
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());
        when(marketQuoteClient.fetch24hTicker("ETHUSDT")).thenReturn(Optional.empty());

        List<DecisionResultVO> result = service.getLatestDecisionResults(10);

        assertThat(result).hasSize(2);
        DecisionResultVO manualItem = result.get(0);
        assertThat(manualItem.getHasOpenPosition()).isTrue();
        assertThat(manualItem.getPositionStatus()).isEqualTo("OPEN");
        assertThat(manualItem.getPositionSide()).isEqualTo("LONG");
        assertThat(manualItem.getAvgOpenPrice()).isEqualByComparingTo("100.50");
        assertThat(manualItem.getPositionQuantity()).isEqualByComparingTo("0.25");
        assertThat(manualItem.getPositionOpenTime()).isEqualTo(openedAt);

        DecisionResultVO executionPlanOnlyItem = result.get(1);
        assertThat(executionPlanOnlyItem.getTradeType()).isEqualTo("TRIGGERED");
        assertThat(executionPlanOnlyItem.getRecommendedAction()).isEqualTo("OPEN_LONG");
        assertThat(executionPlanOnlyItem.getEntryZone()).isEqualTo("3000-3050");
        assertThat(executionPlanOnlyItem.getHasOpenPosition()).isFalse();
        assertThat(executionPlanOnlyItem.getPositionStatus()).isNull();
        assertThat(executionPlanOnlyItem.getPositionSide()).isNull();
        assertThat(executionPlanOnlyItem.getAvgOpenPrice()).isNull();
        assertThat(executionPlanOnlyItem.getPositionQuantity()).isNull();
    }

    @Test
    void getLatestDecisionResultBySymbolUsesManualOpenUserPositionAsDashboardPositionSource() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 6, 22, 8, 30);
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        when(decisionResultMapper.findLatestDecisionResultBySymbolJoined("BTCUSDT")).thenReturn(row);
        when(userPositionMapper.listOpenPositions()).thenReturn(List.of(
                manualUserPosition("btcusdt", "OPEN", openedAt)
        ));
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        DecisionResultVO result = service.getLatestDecisionResultBySymbol("btcusdt");

        assertThat(result).isNotNull();
        assertThat(result.getHasOpenPosition()).isTrue();
        assertThat(result.getPositionStatus()).isEqualTo("OPEN");
        assertThat(result.getPositionSide()).isEqualTo("LONG");
        assertThat(result.getAvgOpenPrice()).isEqualByComparingTo("100.50");
        assertThat(result.getPositionQuantity()).isEqualByComparingTo("0.25");
        assertThat(result.getPositionOpenTime()).isEqualTo(openedAt);
        assertThat(result.getUnrealizedPnlPct()).isNull();
        assertThat(result.getMarkPrice()).isNull();
        assertThat(result.getBreakEvenPrice()).isNull();
        assertThat(result.getLiquidationPrice()).isNull();
    }

    @Test
    void getLatestDecisionResultBySymbolExcludesClosedAndNonManualUserPositionRows() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        UserPositionDO closed = manualUserPosition("BTCUSDT", "CLOSED", LocalDateTime.of(2026, 6, 22, 8, 30));
        UserPositionDO synced = manualUserPosition("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 35));
        synced.setSourceType("POSITION_SYNC");
        when(decisionResultMapper.findLatestDecisionResultBySymbolJoined("BTCUSDT")).thenReturn(row);
        when(userPositionMapper.listOpenPositions()).thenReturn(List.of(closed, synced));
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        DecisionResultVO result = service.getLatestDecisionResultBySymbol("BTCUSDT");

        assertThat(result).isNotNull();
        assertThat(result.getHasOpenPosition()).isFalse();
        assertThat(result.getPositionStatus()).isNull();
        assertThat(result.getPositionSide()).isNull();
        assertThat(result.getAvgOpenPrice()).isNull();
    }

    @Test
    void countOpenPositionsCountsOnlyManualOpenUserPositions() {
        UserPositionDO openManual = manualUserPosition("BTCUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 30));
        UserPositionDO partialManual = manualUserPosition("ETHUSDT", "PARTIALLY_CLOSED", LocalDateTime.of(2026, 6, 22, 8, 35));
        UserPositionDO closedManual = manualUserPosition("SOLUSDT", "CLOSED", LocalDateTime.of(2026, 6, 22, 8, 40));
        UserPositionDO synced = manualUserPosition("BNBUSDT", "OPEN", LocalDateTime.of(2026, 6, 22, 8, 45));
        synced.setSourceType("POSITION_SYNC");
        when(userPositionMapper.listOpenPositions()).thenReturn(List.of(openManual, partialManual, closedManual, synced));

        int count = service.countOpenPositions();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void getLightSystemStatus_mapsMissedCountAndHotResetDefaults() {
        when(analysisRunMapper.countDistinctSymbols()).thenReturn(7);
        when(decisionResultMapper.selectLastDecisionTime()).thenReturn(null);
        when(decisionResultMapper.countDecisionsInRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0);
        when(missedOpportunityMapper.countByBizDate(any(LocalDate.class))).thenReturn(5);
        when(assetStateMapper.countDirectionalPushBlocked(85)).thenReturn(3);
        when(pushSnapshotMapper.countPendingRecheckBacklog(any(LocalDateTime.class))).thenReturn(11);
        when(decisionResultMapper.countOpenSymbolsWithReverseSignal()).thenReturn(2);
        when(assetStateService.findLatestHotResetSnapshot()).thenReturn(null);

        LightSystemStatusVO vo = service.getLightSystemStatus();

        assertThat(vo.getMissedValidOpportunityCount()).isEqualTo(5);
        assertThat(vo.getConfusedCount()).isEqualTo(3);
        assertThat(vo.getPendingCount()).isEqualTo(11);
        assertThat(vo.getReverseSignalCount()).isEqualTo(2);
        assertThat(vo.getHotResetFired()).isFalse();
    }

    @Test
    void lightSystemStatusUsesOneUtcDateForAllTodayMetrics() {
        service.setClock(Clock.fixed(Instant.parse("2026-07-14T23:30:00Z"), ZoneOffset.UTC));

        service.getLightSystemStatus();

        verify(decisionResultMapper).countDecisionsInRange(
                LocalDateTime.parse("2026-07-14T00:00:00"),
                LocalDateTime.parse("2026-07-15T00:00:00"));
        verify(missedOpportunityMapper).countByBizDate(LocalDate.parse("2026-07-14"));
    }

    @Test
    void missedOpportunityBizDateIsTimezoneIndependent() {
        service.setClock(Clock.fixed(Instant.parse("2026-07-14T23:30:00Z"), ZoneOffset.UTC));
        TimeZone original = TimeZone.getDefault();

        try {
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                service.getLightSystemStatus();
            }
        } finally {
            TimeZone.setDefault(original);
        }

        ArgumentCaptor<LocalDate> bizDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(missedOpportunityMapper, times(3)).countByBizDate(bizDateCaptor.capture());
        assertThat(bizDateCaptor.getAllValues()).containsOnly(LocalDate.parse("2026-07-14"));
    }

    @Test
    void utcMidnightDecisionAndMissedMetricsStayOnSameDate() {
        service.setClock(Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC));
        TimeZone original = TimeZone.getDefault();

        try {
            for (String zone : List.of("UTC", "Asia/Shanghai", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                service.getLightSystemStatus();
            }
        } finally {
            TimeZone.setDefault(original);
        }

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDate> bizDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(decisionResultMapper, times(3)).countDecisionsInRange(startCaptor.capture(), endCaptor.capture());
        verify(missedOpportunityMapper, times(3)).countByBizDate(bizDateCaptor.capture());
        assertThat(startCaptor.getAllValues()).containsOnly(LocalDateTime.parse("2026-07-15T00:00:00"));
        assertThat(endCaptor.getAllValues()).containsOnly(LocalDateTime.parse("2026-07-16T00:00:00"));
        assertThat(bizDateCaptor.getAllValues()).containsOnly(LocalDate.parse("2026-07-15"));
    }

    private static UserPositionDO manualUserPosition(String symbol, String status, LocalDateTime openedAt) {
        UserPositionDO row = new UserPositionDO();
        row.setAssetSymbol(symbol);
        row.setSide("LONG");
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100.50"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setOpenedAt(openedAt);
        row.setSourceType("MANUAL");
        row.setSourceRefId("manual-test");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        return row;
    }

    private static void populateCoreDashboardTruthFields(DecisionResultVO row) {
        row.setMarketBiasHierarchy("H1>H4>D1");
        row.setIsWorthOpening(Boolean.TRUE);
        row.setRecommendedAction("OPEN_LONG");
        row.setEntryZone("62000–62500");
        row.setStopLoss("60500");
        row.setTakeProfitRules("TP1 65000 / TP2 68000");
        row.setLeverageSuggestion("3–5x");
        row.setPositionSuggestion("单笔≤2%");
        row.setAiConflictLevel("L2");
        row.setAiConflictScore(42);
        row.setAiPlanMode("AGGRESSIVE");
        row.setConfusedScore(3);
        row.setDataQualityScore(91);
    }

    private static void assertCoreDashboardTruthFields(DecisionResultVO item) {
        assertThat(item.getMarketBiasHierarchy()).isEqualTo("H1>H4>D1");
        assertThat(item.getIsWorthOpening()).isEqualTo(Boolean.TRUE);
        assertThat(item.getRecommendedAction()).isEqualTo("OPEN_LONG");
        assertThat(item.getEntryZone()).isEqualTo("62000–62500");
        assertThat(item.getStopLoss()).isEqualTo("60500");
        assertThat(item.getTakeProfitRules()).isEqualTo("TP1 65000 / TP2 68000");
        assertThat(item.getLeverageSuggestion()).isEqualTo("3–5x");
        assertThat(item.getPositionSuggestion()).isEqualTo("单笔≤2%");
        assertThat(item.getAiConflictLevel()).isEqualTo("L2");
        assertThat(item.getAiConflictScore()).isEqualTo(42);
        assertThat(item.getAiPlanMode()).isEqualTo("AGGRESSIVE");
        assertThat(item.getConfusedScore()).isEqualTo(3);
        assertThat(item.getDataQualityScore()).isEqualTo(91);
    }
}
