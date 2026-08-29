package org.example.trademodel.service.impl;

import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AnalysisRunMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock
    private AnalysisRunMapper analysisRunMapper;

    private OpportunityPriorityRankingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OpportunityPriorityRankingServiceImpl(
                assetPoolService, decisionResultMapper, assetStateMapper,
                FundamentalAiV41Properties.contractFixture(),
                Clock.fixed(Instant.parse("2026-08-11T12:30:00Z"), ZoneOffset.UTC));
        service.setAnalysisRunMapper(analysisRunMapper);
        lenient().when(analysisRunMapper.selectById(anyString())).thenAnswer(invocation -> {
            String analysisId = invocation.getArgument(0);
            String symbol = analysisId != null && analysisId.startsWith("analysis-")
                    ? analysisId.substring("analysis-".length()) : null;
            return symbol != null && symbol.endsWith("USDT")
                    ? formalRun(analysisId, symbol, assetIdFor(symbol),
                    LocalDateTime.of(2026, 8, 11, 12, 0)) : null;
        });
    }

    @Test
    void opportunityScoresDoNotReorderTheFixedHomeStateHierarchy() {
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
                .containsExactly("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT", "FUSDT");
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

        assertThat(ranked).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("AAVEUSDT", "ARBUSDT", "LINKUSDT", "OPUSDT", "SUIUSDT", "TAOUSDT");
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

        assertThat(ranked).hasSize(3);
        assertThat(ranked.get(0)).satisfies(asset -> {
            assertThat(asset.symbol()).isEqualTo("BTCUSDT");
            assertThat(asset.opportunityId()).isEqualTo("opportunity-BTCUSDT");
            assertThat(asset.analysisId()).isEqualTo("analysis-BTCUSDT");
            assertThat(asset.rankingReason()).contains(
                    "HOME_STATE=CANDIDATE", "DATA_STATUS=FRESH", "SOURCE=ASSET_POOL_SCAN");
        });
        assertThat(ranked.subList(1, 3)).allSatisfy(asset -> {
            assertThat(asset.sourceDecision()).isNull();
            assertThat(asset.opportunityId()).isNull();
            assertThat(asset.analysisId()).isNull();
            assertThat(asset.opportunityState()).isEqualTo("NEVER_SCANNED");
        });
    }

    @Test
    void rankingUsesTheFrozenStateHierarchyOnly() {
        List<String> symbols = List.of("OBSUSDT", "HIGHUSDT", "CANDUSDT", "WAITUSDT", "TRIGUSDT");
        List<AssetStateDO> rows = states(symbols);
        rows.get(0).setState(AssetStateEnum.OBSERVING);
        rows.get(1).setState(AssetStateEnum.HIGH_RISK);
        rows.get(2).setState(AssetStateEnum.CANDIDATE);
        rows.get(3).setState(AssetStateEnum.WAITING_TRIGGER);
        rows.get(4).setState(AssetStateEnum.TRIGGERED);
        rows.get(0).setExtJson(scanAudit(LocalDateTime.of(2026, 8, 11, 12, 5),
                "WAIT", "FRESH", true));
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisions(symbols.subList(1, 5), List.of(100, 10, 1, 0)));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID))).thenReturn(rows);

        assertThat(service.rankForHome(USER_ID, 6)).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("TRIGUSDT", "WAITUSDT", "CANDUSDT", "HIGHUSDT", "OBSUSDT");
    }

    @Test
    void ineligibleStatesBecomeObservationsWhileOpportunityPermissionDoesNotChangeStateOrder() {
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
                .containsExactly("DUSDT", "EUSDT", "FUSDT", "AUSDT", "BUSDT", "CUSDT");
    }

    @Test
    void fewerThanSixEligibleOpportunitiesAreFilledFromTheOwnedObservationPool() {
        List<String> symbols = List.of("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT", "FUSDT");
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(decisions(symbols.subList(0, 2), List.of(90, 80)));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID))).thenReturn(states(symbols));

        assertThat(service.rankForHome(USER_ID, 6)).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT", "FUSDT");
        assertThat(service.rankForHome(USER_ID, 6).subList(2, 6)).allSatisfy(asset -> {
            assertThat(asset.sourceDecision()).isNull();
            assertThat(asset.opportunityId()).isNull();
            assertThat(asset.opportunityScore()).isNull();
        });
    }

    @Test
    void highRiskOpportunityIsRetainedAfterDirectionalOpportunityStates() {
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
                .containsExactly("EXTREMEUSDT", "READYUSDT", "RISKHIGHUSDT", "STATEHIGHUSDT");
        HomeTopAssetProjection highRisk = service.rankForHome(USER_ID, 6).get(3);
        assertThat(highRisk.opportunityState()).isEqualTo("HIGH_RISK");
        assertThat(highRisk.sourceDecision()).isNotNull();
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
        when(analysisRunMapper.selectById("analysis-BTC-5m")).thenReturn(
                formalRun("analysis-BTC-5m", "BTCUSDT", assetIdFor("BTCUSDT"),
                        LocalDateTime.of(2026, 8, 11, 12, 5)));
        AnalysisRunDO oneHourRun = formalRun("analysis-BTC-1h", "BTCUSDT", assetIdFor("BTCUSDT"),
                LocalDateTime.of(2026, 8, 11, 12, 0));
        oneHourRun.setTimeframe("1h");
        when(analysisRunMapper.selectById("analysis-BTC-1h")).thenReturn(oneHourRun);

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

    @Test
    void unauthenticatedRequestsNeverReceiveSystemDefaultBackfill() {
        assertThat(service.rankForHome(null, 6)).isEmpty();
        assertThat(service.rankForHome(0L, 6)).isEmpty();

        verify(assetPoolService, never()).listForUser(org.mockito.ArgumentMatchers.anyLong());
        verify(assetPoolService, never()).listSystemDefaults();
    }

    @Test
    void observationBackfillUsesOnlyTheCurrentUsersActiveExplicitPoolRows() {
        List<AssetPoolAssetDTO> mixed = List.of(
                poolAsset("AUSDT", USER_ID, "OBSERVING"),
                poolAsset("SYSTEMUSDT", null, "OBSERVING"),
                poolAsset("OTHERUSDT", USER_ID + 1, "OBSERVING"),
                poolAsset("REMOVEDUSDT", USER_ID, "REMOVED"),
                poolAsset("DISABLEDUSDT", USER_ID, "DISABLED"),
                poolAsset("INACTIVEUSDT", USER_ID, "INACTIVE"),
                poolAsset("GUSDT", USER_ID, "OBSERVING"));
        when(assetPoolService.listForUser(USER_ID)).thenReturn(mixed);
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(
                anyList(), eq("USER"), eq(USER_ID))).thenReturn(List.of(
                decision("SEARCHONLYUSDT", 99, "HIGH", "LOW", "CONFIRMATION",
                        "LEVEL_1_CONSISTENT", 99)));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(List.of());

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("AUSDT", "GUSDT")
                .doesNotContain("SYSTEMUSDT", "OTHERUSDT", "REMOVEDUSDT", "DISABLEDUSDT",
                        "INACTIVEUSDT", "SEARCHONLYUSDT");
        assertThat(ranked).allSatisfy(asset -> {
            assertThat(asset.sourceDecision()).isNull();
            assertThat(asset.opportunityId()).isNull();
            assertThat(asset.opportunityScore()).isNull();
        });
        verify(assetPoolService, never()).listSystemDefaults();
    }

    @Test
    void allWaitResultsRemainVisibleAsTruthfulObservationCards() {
        List<String> symbols = List.of("AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT", "FUSDT");
        List<AssetStateDO> stateRows = states(symbols);
        stateRows.forEach(state -> {
            state.setState(AssetStateEnum.OBSERVING);
            state.setOpportunityId(null);
            state.setExtJson(scanAudit(LocalDateTime.of(2026, 8, 11, 12, 10),
                    "WAIT", "FRESH", true));
        });
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(
                anyList(), eq("USER"), eq(USER_ID))).thenReturn(List.of());
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(stateRows);

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).hasSize(6).allSatisfy(asset -> {
            assertThat(asset.opportunityState()).isEqualTo("NO_QUALIFIED_OPPORTUNITY");
            assertThat(asset.freshness()).isEqualTo("FRESH");
            assertThat(asset.analysisId()).isNotBlank();
            assertThat(asset.opportunityId()).isNull();
            assertThat(asset.opportunityScore()).isNull();
            assertThat(asset.finalMarketBias()).isNull();
            assertThat(asset.confidence()).isNull();
            assertThat(asset.riskLevel()).isNull();
            assertThat(asset.finalPlanMode()).isNull();
            assertThat(asset.sourceDecision()).isNull();
        });
    }

    @Test
    void observationsOrderByDataStateThenLatestFormalScanThenSymbol() {
        List<String> symbols = List.of(
                "FRESHOLDUSDT", "NEVERUSDT", "STALEUSDT", "CONFLICTUSDT", "FRESHNEWUSDT");
        AssetStateDO freshOld = observationState("FRESHOLDUSDT", "FRESH",
                LocalDateTime.of(2026, 8, 11, 12, 5));
        AssetStateDO never = state("NEVERUSDT");
        never.setState(AssetStateEnum.OBSERVING);
        never.setOpportunityId(null);
        never.setLastAnalysisId(null);
        never.setExtJson(null);
        AssetStateDO stale = observationState("STALEUSDT", "FRESH",
                LocalDateTime.of(2026, 8, 11, 10, 0));
        AssetStateDO conflict = observationState("CONFLICTUSDT", "TIMEFRAME_CONFLICT",
                LocalDateTime.of(2026, 8, 11, 12, 20));
        AssetStateDO freshNew = observationState("FRESHNEWUSDT", "FRESH",
                LocalDateTime.of(2026, 8, 11, 12, 20));
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(
                anyList(), eq("USER"), eq(USER_ID))).thenReturn(List.of());
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(List.of(freshOld, never, stale, conflict, freshNew));

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("FRESHNEWUSDT", "FRESHOLDUSDT", "CONFLICTUSDT", "STALEUSDT", "NEVERUSDT");
        assertThat(ranked).extracting(HomeTopAssetProjection::freshness)
                .containsExactly("FRESH", "FRESH", "TIMEFRAME_CONFLICT", "STALE", "NEVER_SCANNED");
    }

    @Test
    void opportunitiesUseCurrentSchedulerFreshnessBeforeLatestScanTime() {
        List<String> symbols = List.of(
                "STALEUSDT", "CONFLICTUSDT", "FRESHOLDUSDT", "FRESHNEWUSDT");
        List<AssetStateDO> stateRows = states(symbols);
        stateRows.forEach(state -> state.setState(AssetStateEnum.TRIGGERED));
        stateRows.get(0).setExtJson(scanAudit(LocalDateTime.of(2026, 8, 11, 10, 0),
                "DATA_NOT_READY", "STALE", false));
        stateRows.get(1).setExtJson(scanAudit(LocalDateTime.of(2026, 8, 11, 12, 25),
                "NO_MATERIAL_CHANGE", "TIMEFRAME_CONFLICT", false));
        stateRows.get(2).setExtJson(scanAudit(LocalDateTime.of(2026, 8, 11, 12, 5),
                "NO_MATERIAL_CHANGE", "FRESH", false));
        stateRows.get(3).setExtJson(scanAudit(LocalDateTime.of(2026, 8, 11, 12, 20),
                "NO_MATERIAL_CHANGE", "FRESH", false));
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(symbols));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(
                anyList(), eq("USER"), eq(USER_ID))).thenReturn(decisions(symbols, List.of(99, 1, 80, 20)));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(stateRows);

        List<HomeTopAssetProjection> ranked = service.rankForHome(USER_ID, 6);

        assertThat(ranked).extracting(HomeTopAssetProjection::symbol)
                .containsExactly("FRESHNEWUSDT", "FRESHOLDUSDT", "CONFLICTUSDT", "STALEUSDT");
        assertThat(ranked).extracting(HomeTopAssetProjection::freshness)
                .containsExactly("FRESH", "FRESH", "TIMEFRAME_CONFLICT", "STALE");
        assertThat(ranked.get(2).timeframeConflictState()).isEqualTo("TIMEFRAME_CONFLICT");
    }

    @Test
    void previewRunCannotBecomeAnOpportunityOrFormalObservationSource() {
        String previewId = "ana-preview-54";
        AssetStateDO state = state("BTCUSDT");
        state.setState(AssetStateEnum.CANDIDATE);
        state.setLastAnalysisId(previewId);
        state.setExtJson(scanAudit(LocalDateTime.of(2026, 8, 11, 12, 20),
                "WAIT", "FRESH", true));
        DecisionResultVO previewDecision = decision("BTCUSDT", 99, "HIGH", "LOW",
                "CONFIRMATION", "LEVEL_1_CONSISTENT", 99);
        previewDecision.setAnalysisId(previewId);
        AnalysisRunDO preview = formalRun(previewId, "BTCUSDT", assetIdFor("BTCUSDT"),
                LocalDateTime.of(2026, 8, 11, 12, 20));
        preview.setTriggerType("ANALYSIS_PREVIEW");
        preview.setAnalysisMode("ANALYSIS_PREVIEW");
        preview.setPreview(true);
        when(analysisRunMapper.selectById(previewId)).thenReturn(preview);
        when(assetPoolService.listForUser(USER_ID)).thenReturn(pool(List.of("BTCUSDT")));
        when(decisionResultMapper.findLatestDecisionResultsForSymbolsJoined(
                anyList(), eq("USER"), eq(USER_ID))).thenReturn(List.of(previewDecision));
        when(assetStateMapper.listByOwnerAndSymbols(anyList(), eq("USER"), eq(USER_ID)))
                .thenReturn(List.of(state));

        assertThat(service.rankForHome(USER_ID, 6)).singleElement().satisfies(asset -> {
            assertThat(asset.sourceDecision()).isNull();
            assertThat(asset.analysisId()).isNull();
            assertThat(asset.opportunityId()).isNull();
            assertThat(asset.opportunityScore()).isNull();
            assertThat(asset.opportunityState()).isEqualTo("NEVER_SCANNED");
            assertThat(asset.analysisTime()).isNull();
            assertThat(asset.freshness()).isEqualTo("NEVER_SCANNED");
        });
    }

    private static List<AssetPoolAssetDTO> pool(List<String> symbols) {
        return java.util.stream.IntStream.range(0, symbols.size())
                .mapToObj(index -> poolAsset(symbols.get(index), USER_ID, "OBSERVING"))
                .toList();
    }

    private static AssetPoolAssetDTO poolAsset(String symbol, Long userId, String watchStatus) {
        Long assetId = assetIdFor(symbol);
        return new AssetPoolAssetDTO(assetId, symbol, symbol.replace("USDT", ""),
                "SPOT", "USDT", true, assetId.intValue(), "USER_ADDED",
                assetId * 10, userId, symbol.replace("USDT", ""), "USER_ADDED",
                watchStatus, LocalDateTime.of(2026, 8, 10, 12, 0),
                LocalDateTime.of(2026, 8, 11, 12, 0), 1, null);
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
        state.setOwnerType("USER");
        state.setOwnerId(USER_ID);
        state.setAssetId(assetIdFor(symbol));
        state.setPoolItemId(assetIdFor(symbol) * 10);
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

    private static Long assetIdFor(String symbol) {
        return (long) Math.abs(symbol.hashCode()) + 1L;
    }

    private static AnalysisRunDO formalRun(String analysisId, String symbol,
                                           Long assetId, LocalDateTime completedAt) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        run.setTimeframe("5m");
        run.setStatus("SUCCESS");
        run.setTriggerType("ASSET_POOL_SCAN");
        run.setOwnerType("USER");
        run.setOwnerId(USER_ID);
        run.setAssetId(assetId);
        run.setPreview(false);
        run.setAnalysisMode("OPPORTUNITY_DECISION");
        run.setAnalysisTime(completedAt);
        run.setCompletedAt(completedAt);
        return run;
    }

    private static String scanAudit(LocalDateTime finishedAt, String result,
                                    String freshness, boolean fullAnalysisSucceeded) {
        return "{\"schedulerScan\":{\"finishedAt\":\"" + finishedAt
                + "\",\"result\":\"" + result
                + "\",\"dataFreshness\":\"" + freshness
                + "\",\"fullAnalysisSucceeded\":" + fullAnalysisSucceeded + "}}";
    }

    private static AssetStateDO stateForTimeframe(String symbol, String timeframe,
                                                   String analysisId, String opportunityId, int score) {
        AssetStateDO state = state(symbol, score, "HIGH", "LOW");
        state.setTimeframe(timeframe);
        state.setLastAnalysisId(analysisId);
        state.setOpportunityId(opportunityId);
        return state;
    }

    private static AssetStateDO observationState(String symbol,
                                                 String freshness,
                                                 LocalDateTime finishedAt) {
        AssetStateDO state = state(symbol);
        state.setState(AssetStateEnum.OBSERVING);
        state.setOpportunityId(null);
        state.setExtJson(scanAudit(finishedAt, "WAIT", freshness, true));
        return state;
    }
}
