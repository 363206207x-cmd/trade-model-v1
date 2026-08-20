package org.example.trademodel.service.impl;

import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.config.FundamentalAiV41Properties;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
                assetPoolService, decisionResultMapper, assetStateMapper,
                FundamentalAiV41Properties.contractFixture(),
                Clock.fixed(Instant.parse("2026-08-11T12:30:00Z"), ZoneOffset.UTC));
    }

    @Test
    void topSixChangesDynamicallyWhenOpportunityScoresChange() {
        List<String> symbols = List.of("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT",
                "FUSDT", "GUSDT", "HUSDT", "IUSDT", "JUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(states(symbols, List.of(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)))
                .thenReturn(states(symbols, List.of(100, 90, 80, 70, 60, 50, 79, 78, 77, 10)));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
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
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID))).thenReturn(states(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisions(symbols, List.of(100, 90, 80, 70, 60, 50, 40, 30, 20, 10)));

        assertThat(service.rankForHome(USER_ID, 12)).hasSize(6);
    }

    @Test
    void removedDefaultSymbolsAreNeverReintroducedByHomeRanking() {
        List<String> userPool = List.of("AAVEUSDT", "LINKUSDT", "TAOUSDT",
                "SUIUSDT", "ARBUSDT", "OPUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(userPool));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisions(userPool, List.of(95, 90, 85, 80, 75, 70)));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(states(userPool, List.of(95, 90, 85, 80, 75, 70)));

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).extracting(HomeTopAssetProjection::symbol).containsExactlyElementsOf(userPool);
        assertThat(ranked).extracting(HomeTopAssetProjection::symbol)
                .doesNotContain("BTCUSDT", "ETHUSDT", "SOLUSDT");
    }

    @Test
    void homeProjectionRequiresExactOpportunityAndAnalysisSource() {
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT", "SOLUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisions(symbols, List.of(90, 80, 70)));
        AssetStateDO exact = state("BTCUSDT");
        AssetStateDO staleAnalysis = state("ETHUSDT");
        staleAnalysis.setLastAnalysisId("old-analysis");
        AssetStateDO wrongTimeframe = state("SOLUSDT");
        wrongTimeframe.setTimeframe("1h");
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(List.of(exact, staleAnalysis, wrongTimeframe));

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).singleElement().satisfies(asset -> {
            assertThat(asset.symbol()).isEqualTo("BTCUSDT");
            assertThat(asset.opportunityId()).isEqualTo("opportunity-BTCUSDT");
            assertThat(asset.analysisId()).isEqualTo("analysis-BTCUSDT");
            assertThat(asset.rankingReason()).contains(
                    "OPPORTUNITY_SCORE=90", "CONFIDENCE=HIGH", "RISK_LEVEL=LOW",
                    "PLAN_MODE=CONFIRMATION", "AI_DECISION=LEVEL_1_CONSISTENT", "DATA_QUALITY=90");
        });
    }

    @Test
    void rankingUsesEveryFrozenPriorityInput() {
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 80, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "MEDIUM", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "HIGH", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "LOW", "PREPARATION",
                        "LEVEL_1_CONSISTENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_3_SIGNIFICANT_DISAGREEMENT", 90), "AUSDT");
        assertWinner(decision("AUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 90),
                decision("BUSDT", 90, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 70), "AUSDT");
    }

    @Test
    void blockedIneligibleAndStaleAssetsAreExcludedWithoutBackfill() {
        List<String> symbols = List.of("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT", "FUSDT");
        List<DecisionResultVO> decisions = decisions(symbols, List.of(95, 94, 93, 92, 91, 90));
        decisions.get(3).setPlanMode("BLOCKED");
        decisions.get(4).setAnalysisTime(LocalDateTime.of(2026, 8, 11, 10, 0));
        List<AssetStateDO> stateRows = states(symbols);
        stateRows.get(0).setState(AssetStateEnum.INVALIDATED);
        stateRows.get(1).setState(AssetStateEnum.COOLING);
        stateRows.get(2).setState(AssetStateEnum.CONFUSED);
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID))).thenReturn(decisions);
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID))).thenReturn(stateRows);

        assertThat(service.rankForHome(USER_ID, 6)).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("FUSDT");
    }

    @Test
    void fewerThanSixEligibleOpportunitiesAreNotFilledWithPoolDefaults() {
        List<String> symbols = List.of("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT", "FUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisions(symbols.subList(0, 2), List.of(90, 80)));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID))).thenReturn(states(symbols));

        assertThat(service.rankForHome(USER_ID, 6)).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("AUSDT", "BUSDT");
    }

    @Test
    void highRiskOpportunityIsReservedForAlertsAndExcludedFromPositiveTopSix() {
        List<String> symbols = List.of("STATEHIGHUSDT", "RISKHIGHUSDT", "EXTREMEUSDT", "READYUSDT");
        List<AssetStateDO> stateRows = states(symbols);
        stateRows.get(0).setState(AssetStateEnum.HIGH_RISK);
        List<DecisionResultVO> decisionRows = decisions(symbols, List.of(99, 98, 97, 70));
        stateRows.get(1).setRisk("HIGH");
        stateRows.get(2).setRisk("EXTREME");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisionRows);
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(stateRows);

        assertThat(service.rankForHome(USER_ID, 6)).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("READYUSDT");
    }

    @Test
    void exactTiesUseStableSymbolOrdering() {
        List<String> symbols = List.of("BUSDT", "AUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisions(symbols, List.of(90, 90)));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID))).thenReturn(states(symbols));

        assertThat(service.rankForHome(USER_ID, 6)).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("AUSDT", "BUSDT");
    }

    @Test
    void sameAssetTimeframesAggregateWithoutSilentlyAveragingOpposingDirections() {
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(List.of("BTCUSDT")));
        DecisionResultVO fiveMinute = decision("BTCUSDT", 94, "HIGH", "LOW", "CONFIRMATION",
                "LEVEL_1_CONSISTENT", 95);
        fiveMinute.setAnalysisId("analysis-BTC-5m");
        fiveMinute.setTimeframe("5m");
        fiveMinute.setFinalMarketBias("BULLISH");
        DecisionResultVO oneHour = decision("BTCUSDT", 82, "MEDIUM", "MEDIUM", "PREPARATION",
                "LEVEL_2_MINOR_DISAGREEMENT", 88);
        oneHour.setAnalysisId("analysis-BTC-1h");
        oneHour.setTimeframe("1h");
        oneHour.setFinalMarketBias("BEARISH");
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(
                anyList(), eq("USER"), eq(USER_ID))).thenReturn(List.of(fiveMinute, oneHour));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(List.of(
                        stateForTimeframe("BTCUSDT", "5m", "analysis-BTC-5m", "opportunity-BTC-5m", 94),
                        stateForTimeframe("BTCUSDT", "1h", "analysis-BTC-1h", "opportunity-BTC-1h", 82)));

        assertThat(service.rankForHome(USER_ID, 6)).singleElement().satisfies(asset -> {
            assertThat(asset.primaryOpportunityId()).isEqualTo("opportunity-BTC-5m");
            assertThat(asset.primaryTimeframe()).isEqualTo("5m");
            assertThat(asset.primaryPlanMode()).isEqualTo("CONFIRMATION");
            assertThat(asset.secondaryOpportunityCount()).isEqualTo(1);
            assertThat(asset.timeframeConflictState()).isEqualTo("OPPOSING");
            assertThat(asset.rankingReason()).contains(
                    "PRIMARY_TIMEFRAME=5m", "SECONDARY_OPPORTUNITY_COUNT=1",
                    "TIMEFRAME_CONFLICT_STATE=OPPOSING");
        });
    }

    private void assertWinner(DecisionResultVO first, DecisionResultVO second, String expected) {
        List<String> symbols = List.of(first.getSymbol(), second.getSymbol());
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(List.of(first, second));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID))).thenReturn(List.of(
                state(first.getSymbol(), first.getOpportunityScore().intValue(), first.getConfidenceLevel(), first.getRiskLevel()),
                state(second.getSymbol(), second.getOpportunityScore().intValue(), second.getConfidenceLevel(), second.getRiskLevel())));
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
                .mapToObj(index -> decision(symbols.get(index), scores.get(index), "HIGH", "LOW", "CONFIRMATION",
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
        decision.setFinalMarketBias("BULLISH");
        decision.setAiConflictLevel(aiDecision);
        decision.setDataQualityScore(dataQuality);
        decision.setAnalysisTime(LocalDateTime.of(2026, 8, 11, 12, 0));
        decision.setCreateTime(LocalDateTime.of(2026, 8, 11, 12, 0));
        return decision;
    }

    private static List<AssetStateDO> states(List<String> symbols) {
        return symbols.stream().map(OpportunityPriorityRankingServiceImplTest::state).toList();
    }

    private static List<AssetStateDO> states(List<String> symbols, List<Integer> scores) {
        return java.util.stream.IntStream.range(0, symbols.size())
                .mapToObj(index -> state(symbols.get(index), scores.get(index), "HIGH", "LOW"))
                .toList();
    }

    private static AssetStateDO state(String symbol) {
        return state(symbol, 90, "HIGH", "LOW");
    }

    private static AssetStateDO state(String symbol, int score, String confidence, String risk) {
        AssetStateDO state = new AssetStateDO();
        state.setSymbol(symbol);
        state.setTimeframe("5m");
        state.setState(AssetStateEnum.CANDIDATE);
        state.setOpportunityId("opportunity-" + symbol);
        state.setLastAnalysisId("analysis-" + symbol);
        state.setLastUpdateTime(LocalDateTime.of(2026, 8, 11, 12, 0));
        state.setStateEnteredAt(LocalDateTime.of(2026, 8, 11, 12, 0));
        state.setOpportunityScore(score);
        state.setConfidence(confidence);
        state.setRisk(risk);
        return state;
    }

    private static AssetStateDO stateForTimeframe(String symbol, String timeframe,
                                                   String analysisId, String opportunityId, int score) {
        AssetStateDO state = state(symbol, score, "HIGH", "LOW");
        state.setTimeframe(timeframe);
        state.setLastAnalysisId(analysisId);
        state.setOpportunityId(opportunityId);
        return state;
    }
}
