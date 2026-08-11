package org.example.trademodel.service.impl;

import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.HomeTopAssetProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class OpportunityPriorityRankingServiceImplTest {
    private static final Long USER_ID = 41L;

    @Mock
    private AssetPoolService assetPoolService;
    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private AssetStateMapper assetStateMapper;

    private OpportunityPriorityRankingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OpportunityPriorityRankingServiceImpl(
                assetPoolService, decisionResultMapper, assetStateMapper);
    }

    @Test
    void topSixChangesDynamicallyWhenOpportunityScoresChange() {
        List<String> symbols = List.of("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT",
                "FUSDT", "GUSDT", "HUSDT", "IUSDT", "JUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(assetStateMapper.listBySymbols(anyList())).thenReturn(states(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList()))
                .thenReturn(decisions(symbols, List.of(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)))
                .thenReturn(decisions(symbols, List.of(100, 90, 80, 70, 60, 50, 79, 78, 77, 10)));

        List<HomeTopAssetProjection> first = service.rankForHome(USER_ID, 6);
        List<HomeTopAssetProjection> second = service.rankForHome(USER_ID, 6);

        assertThat(first).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT", "FUSDT");
        assertThat(second).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("AUSDT", "BUSDT", "CUSDT", "GUSDT", "HUSDT", "IUSDT");
    }

    @Test
    void homeProjectionNeverExceedsSixAssets() {
        List<String> symbols = List.of("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT",
                "FUSDT", "GUSDT", "HUSDT", "IUSDT", "JUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(assetStateMapper.listBySymbols(anyList())).thenReturn(states(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList()))
                .thenReturn(decisions(symbols, List.of(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)));

        assertThat(service.rankForHome(USER_ID, 12)).hasSize(6);
    }

    @Test
    void removedDefaultSymbolsAreNeverReintroducedByHomeRanking() {
        List<String> userPool = List.of("AAVEUSDT", "LINKUSDT", "TAOUSDT",
                "SUIUSDT", "ARBUSDT", "OPUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(userPool));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList()))
                .thenReturn(decisions(userPool, List.of(95, 90, 85, 80, 75, 70)));
        when(assetStateMapper.listBySymbols(anyList())).thenReturn(states(userPool));

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).extracting(HomeTopAssetProjection::symbol).containsExactlyElementsOf(userPool);
        assertThat(ranked).extracting(HomeTopAssetProjection::symbol)
                .doesNotContain("BTCUSDT", "ETHUSDT", "SOLUSDT");
    }

    @Test
    void homeProjectionRequiresExactOpportunityAndAnalysisSource() {
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT", "SOLUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList()))
                .thenReturn(decisions(symbols, List.of(90, 80, 70)));
        AssetStateDO exact = state("BTCUSDT");
        AssetStateDO staleAnalysis = state("ETHUSDT");
        staleAnalysis.setLastAnalysisId("old-analysis");
        AssetStateDO wrongTimeframe = state("SOLUSDT");
        wrongTimeframe.setTimeframe("1h");
        when(assetStateMapper.listBySymbols(anyList()))
                .thenReturn(List.of(exact, staleAnalysis, wrongTimeframe));

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).singleElement().satisfies(asset -> {
            assertThat(asset.symbol()).isEqualTo("BTCUSDT");
            assertThat(asset.opportunityId()).isEqualTo("opportunity-BTCUSDT");
            assertThat(asset.analysisId()).isEqualTo("analysis-BTCUSDT");
            assertThat(asset.rankingReason()).contains(
                    "OPPORTUNITY_SCORE=90", "CONFIDENCE=HIGH", "RISK_LEVEL=LOW",
                    "PLAN_MODE=CONFIRM", "AI_DECISION=LEVEL_1_CONSISTENT", "DATA_QUALITY=90");
        });
    }

    @Test
    void rankingUsesEveryFrozenPriorityInput() {
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 80, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "MEDIUM", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "HIGH", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "LOW", "PREPARE",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_3_SIGNIFICANT_DISAGREEMENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 70), "AUSDT");
    }

    private void assertWinner(DecisionResultVO first, DecisionResultVO second, String expected) {
        List<String> symbols = List.of(first.getSymbol(), second.getSymbol());
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList()))
                .thenReturn(List.of(first, second));
        when(assetStateMapper.listBySymbols(anyList())).thenReturn(states(symbols));
        assertThat(service.rankForHome(USER_ID, 1)).first()
                .extracting(HomeTopAssetProjection::symbol).isEqualTo(expected);
    }

    private static List<AssetPoolAssetDTO> pool(List<String> symbols) {
        return java.util.stream.IntStream.range(0, symbols.size())
                .mapToObj(index -> new AssetPoolAssetDTO(
                        (long) index + 1,
                        symbols.get(index),
                        symbols.get(index).replace("USDT", ""),
                        "SPOT",
                        "USDT",
                        true,
                        (index + 1) * 10,
                        index < 6 ? "DEFAULT" : "USER_ADDED"))
                .toList();
    }

    private static List<DecisionResultVO> decisions(List<String> symbols, List<Integer> scores) {
        return java.util.stream.IntStream.range(0, symbols.size())
                .mapToObj(index -> decision(symbols.get(index), scores.get(index), "HIGH", "LOW", "CONFIRM",
                        "LEVEL_1_CONSISTENT", 90))
                .toList();
    }

    private static DecisionResultVO decision(String symbol,
                                             int score,
                                             String confidence,
                                             String risk,
                                             String planMode,
                                             String aiDecision,
                                             int dataQuality) {
        DecisionResultVO decision = new DecisionResultVO();
        decision.setDecisionId("decision-" + symbol);
        decision.setAnalysisId("analysis-" + symbol);
        decision.setSymbol(symbol);
        decision.setTimeframe("5m");
        decision.setOpportunityScore((double) score);
        decision.setConfidenceLevel(confidence);
        decision.setRiskLevel(risk);
        decision.setPlanMode(planMode);
        decision.setAiConflictLevel(aiDecision);
        decision.setDataQualityScore(dataQuality);
        decision.setCreateTime(LocalDateTime.of(2026, 8, 11, 12, 0));
        return decision;
    }

    private static List<AssetStateDO> states(List<String> symbols) {
        return symbols.stream().map(OpportunityPriorityRankingServiceImplTest::state).toList();
    }

    private static AssetStateDO state(String symbol) {
        AssetStateDO state = new AssetStateDO();
        state.setSymbol(symbol);
        state.setTimeframe("5m");
        state.setState(AssetStateEnum.CANDIDATE);
        state.setOpportunityId("opportunity-" + symbol);
        state.setLastAnalysisId("analysis-" + symbol);
        state.setLastUpdateTime(LocalDateTime.of(2026, 8, 11, 12, 0));
        return state;
    }
}
