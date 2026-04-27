package org.example.trademodel.service;

import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.vo.DecisionBundleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionEngineServiceTest {

    @Mock
    private RealMarketDataFetcherService marketDataFetcher;
    @Mock
    private AiConflictResolverService aiConflictResolverService;
    @Mock
    private ConfusedStateService confusedStateService;
    @Mock
    private AssetStateService assetStateService;
    @Mock
    private RuleConfigService ruleConfigService;

    private DecisionEngineService service;

    @BeforeEach
    void setUp() {
        service = new DecisionEngineService(
                marketDataFetcher,
                aiConflictResolverService,
                confusedStateService,
                assetStateService,
                ruleConfigService
        );
        when(ruleConfigService.getRuleConfigMap()).thenReturn(Collections.emptyMap());
        when(assetStateService.buildSnapshotAtDecision(anyString(), anyString(), any(AssetStateEnum.class), any(Integer.class), any(Boolean.class)))
                .thenReturn("{\"state\":\"TEST\"}");
        when(aiConflictResolverService.resolve(any(DecisionContext.class)))
                .thenReturn(new AiConflictResult(
                        AiConflictLevelEnum.LEVEL_1_CONSISTENT,
                        "BULLISH",
                        "HIGH",
                        "NORMAL",
                        20
                ));
        when(confusedStateService.calculateConfused(anyString(), any(DecisionContext.class)))
                .thenReturn(new ConfusedResult(20, false, false, "none"));
        when(marketDataFetcher.fetchKlines(anyString(), anyString(), any(Integer.class)))
                .thenAnswer(invocation -> "1m".equals(invocation.getArgument(1))
                        ? bullishKlines()
                        : bullishKlines());
    }

    @Test
    void makeDecision_forcesWorthOpeningFalseWhenDataQualityBelow60_andKeepsReasonPresent() {
        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-1", 59, 65);

        assertThat(decision.getIsWorthOpening()).isFalse();
        assertThat(decision.getReviewReasons()).contains("DATA_QUALITY_INSUFFICIENT");
    }

    @Test
    void makeDecision_dataQualityAtOrAbove60_doesNotChangeExistingWorthOpeningOutcome() {
        DecisionBundleVO baseline = service.makeDecision("BTCUSDT", "1m", "analysis-2", null, 65);
        DecisionBundleVO withGateBoundaryScore = service.makeDecision("BTCUSDT", "1m", "analysis-3", 60, 65);

        assertThat(withGateBoundaryScore.getIsWorthOpening()).isEqualTo(baseline.getIsWorthOpening());
    }

    @Test
    void makeDecision_dataQualityGate_doesNotAffectConfidenceRiskOrMarketBias() {
        DecisionBundleVO normal = service.makeDecision("BTCUSDT", "1m", "analysis-4", 60, 65);
        DecisionBundleVO gated = service.makeDecision("BTCUSDT", "1m", "analysis-5", 59, 65);

        assertThat(gated.getIsWorthOpening()).isFalse();
        assertThat(gated.getConfidenceLevel()).isEqualTo(normal.getConfidenceLevel());
        assertThat(gated.getRiskLevel()).isEqualTo(normal.getRiskLevel());
        assertThat(gated.getMarketBiasHierarchy()).isEqualTo(normal.getMarketBiasHierarchy());
    }

    @Test
    void makeDecision_forcesWorthOpeningFalseWhenTrendStructureScoreBelow50_andKeepsReasonPresent() {
        DecisionBundleVO decision = service.makeDecision("BTCUSDT", "1m", "analysis-6", 60, 49);

        assertThat(decision.getIsWorthOpening()).isFalse();
        assertThat(decision.getReviewReasons()).contains("TREND_STRUCTURE_SCORE_INSUFFICIENT");
    }

    @Test
    void makeDecision_trendStructureGate_runsBeforeDataQualityGateButDqRemainsFinalFallback() {
        DecisionBundleVO trendOnlyGate = service.makeDecision("BTCUSDT", "1m", "analysis-7", 60, 49);
        DecisionBundleVO dqOnlyGate = service.makeDecision("BTCUSDT", "1m", "analysis-8", 59, 65);

        assertThat(trendOnlyGate.getIsWorthOpening()).isFalse();
        assertThat(trendOnlyGate.getReviewReasons()).contains("TREND_STRUCTURE_SCORE_INSUFFICIENT");
        assertThat(dqOnlyGate.getIsWorthOpening()).isFalse();
        assertThat(dqOnlyGate.getReviewReasons()).contains("DATA_QUALITY_INSUFFICIENT");
    }

    @Test
    void makeDecision_gateRuleUnchanged_whenDqTierMovesFrom55To85() {
        DecisionBundleVO dq55 = service.makeDecision("BTCUSDT", "1m", "analysis-9", 55, 65);
        DecisionBundleVO dq85 = service.makeDecision("BTCUSDT", "1m", "analysis-10", 85, 65);

        assertThat(dq55.getIsWorthOpening()).isFalse();
        assertThat(dq55.getReviewReasons()).contains("DATA_QUALITY_INSUFFICIENT");
        assertThat(dq85.getIsWorthOpening()).isTrue();
        assertThat(dq85.getReviewReasons()).doesNotContain("DATA_QUALITY_INSUFFICIENT");
    }

    private static List<String[]> bullishKlines() {
        return List.of(
                new String[]{"0", "100", "110", "95", "108"},
                new String[]{"0", "108", "112", "107", "110"},
                new String[]{"0", "110", "118", "109", "117"}
        );
    }
}
