package org.example.trademodel.service.impl;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private RealPositionMapper realPositionMapper;
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
                marketQuoteClient,
                realPositionMapper,
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
        when(realPositionMapper.findOpenPositions()).thenReturn(Collections.emptyList());
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
        when(realPositionMapper.findOpenPositions()).thenReturn(Collections.emptyList());
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
        when(realPositionMapper.findOpenPositions()).thenReturn(Collections.emptyList());
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
        when(realPositionMapper.findOpenPositions()).thenReturn(Collections.emptyList());

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
        when(realPositionMapper.findOpenPositions()).thenReturn(Collections.emptyList());
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
        when(realPositionMapper.findOpenPositions()).thenReturn(Collections.emptyList());
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());

        DecisionResultVO result = service.getLatestDecisionResultBySymbol("btcusdt");

        assertThat(result).isNotNull();
        assertThat(result.getReadModelTruthStatus()).isEqualTo("PARTIAL");
        assertThat(result.getReadModelFallbackReason()).startsWith("LEGACY_MISSING:");
        assertThat(result.getHasOpenPosition()).isFalse();
        assertThat(result.getPositionStatus()).isNull();
    }

    @Test
    void getLightSystemStatus_mapsMissedCountAndHotResetDefaults() {
        when(analysisRunMapper.countDistinctSymbols()).thenReturn(7);
        when(decisionResultMapper.selectLastDecisionTime()).thenReturn(null);
        when(decisionResultMapper.countDecisionsToday()).thenReturn(0);
        when(missedOpportunityMapper.countByBizDate(any(LocalDate.class))).thenReturn(5);
        when(assetStateMapper.countSymbolsWhereConfusedScorePositive()).thenReturn(3);
        when(pushSnapshotMapper.countPendingRecheckBacklog()).thenReturn(11);
        when(decisionResultMapper.countOpenSymbolsWithReverseSignal()).thenReturn(2);
        when(assetStateService.findLatestHotResetSnapshot()).thenReturn(null);

        LightSystemStatusVO vo = service.getLightSystemStatus();

        assertThat(vo.getMissedValidOpportunityCount()).isEqualTo(5);
        assertThat(vo.getConfusedCount()).isEqualTo(3);
        assertThat(vo.getPendingCount()).isEqualTo(11);
        assertThat(vo.getReverseSignalCount()).isEqualTo(2);
        assertThat(vo.getHotResetFired()).isFalse();
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
