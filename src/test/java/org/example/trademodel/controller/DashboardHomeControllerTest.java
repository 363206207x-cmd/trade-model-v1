package org.example.trademodel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.assetcard.AssetCardMarketDataService;
import org.example.trademodel.assetcard.AssetCardProperties;
import org.example.trademodel.assetcard.AssetCardService;
import org.example.trademodel.assetcard.AssetCardSnapshot;
import org.example.trademodel.common.GlobalExceptionHandler;
import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.mapper.AssetCardMapper;
import org.example.trademodel.security.AuthenticatedUserResolutionException;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.v41.DashboardLiveEventService;
import org.example.trademodel.vo.DashboardHomeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Tag("core-regression")
class DashboardHomeControllerTest {
    @Mock
    private DashboardHomeService dashboardHomeService;
    @Mock
    private AuthenticatedUserIdResolver authenticatedUserIdResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DashboardHomeController(dashboardHomeService, authenticatedUserIdResolver)).build();
    }

    @Test
    void homeReturnsApiResponseSuccess() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 6, null)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.selectedSymbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.data.safety.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.data.safety.notAutoTrading").value(true));

        verify(dashboardHomeService).getHomeForUser(7L, "BTCUSDT", 6, null);
    }

    @Test
    void runtimeSnapshotUsesAuthenticatedIdentityAndPreservesAllThirtySixPoolMembers() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSnapshotId("web-123");
        home.setProjectionVersion(123);
        home.setSnapshotComplete(true);
        home.setAssetPool(java.util.stream.IntStream.range(0, 36).mapToObj(index -> {
            var asset = new DashboardHomeVO.AssetVO();
            asset.setRawSymbol("ASSET" + index);
            asset.setSnapshotId("web-123");
            return asset;
        }).toList());
        when(dashboardHomeService.getHomeForUser(7L, null, null, null)).thenReturn(home);
        mockMvc.perform(get("/api/dashboard/runtime-snapshot").param("ownerId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshotId").value("web-123"))
                .andExpect(jsonPath("$.data.snapshotComplete").value(true))
                .andExpect(jsonPath("$.data.assetPool.length()").value(36))
                .andExpect(jsonPath("$.data.assetPool[35].snapshotId").value("web-123"));
        verify(dashboardHomeService).getHomeForUser(7L, null, null, null);
    }

    @Test
    void cardOnlySnapshotUsesSessionIdentityAndReadOnlyServiceForSixDisplayedMembers() throws Exception {
        try (CardReadFixture fixture = new CardReadFixture()) {
            var members = java.util.stream.IntStream.rangeClosed(1, 7).mapToObj(index ->
                    new AssetPoolAssetDTO((long) index, "ASSET" + index + "USDT", "Asset " + index,
                            "SPOT", "USDT", true, index, "USER")).toList();
            when(fixture.pool.listForUser(7L)).thenReturn(members);
            String[] requested = {"ASSET6USDT", "ASSET5USDT", "ASSET4USDT", "ASSET3USDT", "ASSET2USDT", "asset1usdt"};

            cardMvc(fixture.service).perform(get("/api/dashboard/runtime-snapshot")
                            .param("view", "ASSET_CARDS").param("symbols", requested)
                            .param("ownerId", "999").param("userId", "999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(6))
                    .andExpect(jsonPath("$.data[0].symbol").value("ASSET6USDT"))
                    .andExpect(jsonPath("$.data[5].symbol").value("ASSET1USDT"))
                    .andExpect(jsonPath("$.data[5].assetName").value("Asset 1"))
                    .andExpect(jsonPath("$.data[0].signal.status").value("INSUFFICIENT_DATA"))
                    .andExpect(jsonPath("$.data[0].signal.calibratedConfidence").isEmpty())
                    .andExpect(jsonPath("$.data.assets").doesNotExist())
                    .andExpect(jsonPath("$.data.assetPool").doesNotExist());

            verify(authenticatedUserIdResolver).requireCurrentUserId();
            verify(fixture.pool).listForUser(7L);
            for (int index = 1; index <= 6; index++) verify(fixture.mapper).selectSnapshotJson("ASSET" + index + "USDT");
            verifyNoMoreInteractions(fixture.pool, fixture.mapper);
            verifyNoInteractions(dashboardHomeService, fixture.market, fixture.events);
        }
    }

    @Test
    void cardOnlySnapshotRejectsSevenSymbolsBeforeAnyPoolOrSnapshotRead() throws Exception {
        try (CardReadFixture fixture = new CardReadFixture()) {
            cardMvc(fixture.service).perform(get("/api/dashboard/runtime-snapshot")
                            .param("view", "ASSET_CARDS")
                            .param("symbols", "BTCUSDT", "ETHUSDT", "SOLUSDT", "LINKUSDT", "AAVEUSDT", "XRPUSDT", "TRXUSDT"))
                    .andExpect(status().isBadRequest());
            verify(authenticatedUserIdResolver).requireCurrentUserId();
            verifyNoInteractions(dashboardHomeService, fixture.pool, fixture.mapper, fixture.market, fixture.events);
        }
    }

    @Test
    void cardOnlySnapshotRejectsForeignDuplicateAndMalformedSymbolsWithoutPartialReads() throws Exception {
        try (CardReadFixture fixture = new CardReadFixture()) {
            when(fixture.pool.listForUser(7L)).thenReturn(List.of(
                    new AssetPoolAssetDTO(1L, "BTCUSDT", "Bitcoin", "SPOT", "USDT", true, 1, "USER")));
            MockMvc cards = cardMvc(fixture.service);
            for (String[] requested : List.of(new String[]{"BTCUSDT", "ETHUSDT"},
                    new String[]{"BTCUSDT", "btcusdt"}, new String[]{"BTC/USDT"})) {
                cards.perform(get("/api/dashboard/runtime-snapshot")
                                .param("view", "ASSET_CARDS").param("symbols", requested)
                                .param("ownerId", "999").param("userId", "999"))
                        .andExpect(status().isBadRequest());
            }
            verify(fixture.pool, times(3)).listForUser(7L);
            verifyNoMoreInteractions(fixture.pool);
            verifyNoInteractions(dashboardHomeService, fixture.mapper, fixture.market, fixture.events);
        }
    }

    @Test
    void cardOnlySnapshotCannotUseForgedUserIdWithoutAuthenticatedSession() throws Exception {
        when(authenticatedUserIdResolver.requireCurrentUserId()).thenThrow(new AuthenticatedUserResolutionException());
        try (CardReadFixture fixture = new CardReadFixture()) {
            cardMvc(fixture.service).perform(get("/api/dashboard/runtime-snapshot")
                            .param("view", "ASSET_CARDS").param("symbols", "BTCUSDT")
                            .param("ownerId", "7").param("userId", "7"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(dashboardHomeService, fixture.pool, fixture.mapper, fixture.market, fixture.events);
        }
    }

    @Test
    void cardOnlySnapshotFiltersShadowAndExactCanaryCohortsWithoutLegacyFallback() throws Exception {
        try (CardReadFixture fixture = new CardReadFixture()) {
            when(fixture.pool.listForUser(7L)).thenReturn(List.of(
                    new AssetPoolAssetDTO(1L, "BTCUSDT", "Bitcoin", "SPOT", "USDT", true, 1, "USER"),
                    new AssetPoolAssetDTO(2L, "ETHUSDT", "Ethereum", "SPOT", "USDT", true, 2, "USER")));
            MockMvc cards = cardMvc(fixture.service);
            for (var mode : List.of(AssetCardProperties.ModelMode.LEGACY, AssetCardProperties.ModelMode.SHADOW)) {
                fixture.properties.setModelMode(mode);
                cards.perform(get("/api/dashboard/runtime-snapshot").param("view", "ASSET_CARDS")
                                .param("symbols", "ETHUSDT", "BTCUSDT"))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
            }
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            fixture.properties.setEnabled(false);
            cards.perform(get("/api/dashboard/runtime-snapshot").param("view", "ASSET_CARDS")
                            .param("symbols", "ETHUSDT", "BTCUSDT"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
            verifyNoInteractions(fixture.mapper);

            fixture.properties.setEnabled(true);
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.CANARY);
            fixture.properties.setCanarySymbols(java.util.Set.of("BTCUSDT"));
            cards.perform(get("/api/dashboard/runtime-snapshot").param("view", "ASSET_CARDS")
                            .param("symbols", "ETHUSDT", "BTCUSDT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].symbol").value("BTCUSDT"))
                    .andExpect(jsonPath("$.data[0].signal.status").value("INSUFFICIENT_DATA"))
                    .andExpect(jsonPath("$.data[0].signal.calibratedConfidence").isEmpty());
            verify(fixture.mapper).selectSnapshotJson("BTCUSDT");
            verifyNoMoreInteractions(fixture.mapper);
            verifyNoInteractions(dashboardHomeService, fixture.market, fixture.events);
        }
    }

    @Test
    void installedCardServiceDoesNotReplaceDefaultHomeOrRuntimeSnapshotRouting() throws Exception {
        AssetCardService cards = mock(AssetCardService.class);
        DashboardHomeVO home = new DashboardHomeVO();
        DashboardHomeVO.AssetVO first = new DashboardHomeVO.AssetVO();
        first.setRawSymbol("BTCUSDT");
        first.setMarketBias("WEAK_BEARISH");
        first.setFinalConfidence(51);
        first.setRiskLevel("MEDIUM");
        first.setOpportunityScore(94);
        first.setCardSignalDisplayEnabled(true);
        first.setCardSignal(AssetCardSnapshot.unavailable("BTCUSDT", "Bitcoin", "TEST_FIXTURE"));
        DashboardHomeVO.AssetVO second = new DashboardHomeVO.AssetVO();
        second.setRawSymbol("ETHUSDT");
        second.setOpportunityScore(83);
        home.setAssets(List.of(first, second));
        home.setAssetPool(List.of(second, first));
        when(dashboardHomeService.getHomeForUser(7L, null, null, null)).thenReturn(home);

        MockMvc installed = cardMvc(cards);
        for (String path : List.of("/api/dashboard/home", "/api/dashboard/runtime-snapshot")) {
            installed.perform(get(path).param("ownerId", "999").param("userId", "999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assets[0].rawSymbol").value("BTCUSDT"))
                    .andExpect(jsonPath("$.data.assets[0].marketBias").value("WEAK_BEARISH"))
                    .andExpect(jsonPath("$.data.assets[0].finalConfidence").value(51))
                    .andExpect(jsonPath("$.data.assets[0].riskLevel").value("MEDIUM"))
                    .andExpect(jsonPath("$.data.assets[0].opportunityScore").value(94))
                    .andExpect(jsonPath("$.data.assets[0].cardSignal.signal.status").value("INSUFFICIENT_DATA"))
                    .andExpect(jsonPath("$.data.assets[0].cardSignalDisplayEnabled").value(true))
                    .andExpect(jsonPath("$.data.assets[1].rawSymbol").value("ETHUSDT"))
                    .andExpect(jsonPath("$.data.assets[1].cardSignalDisplayEnabled").value(false))
                    .andExpect(jsonPath("$.data.assetPool[0].rawSymbol").value("ETHUSDT"))
                    .andExpect(jsonPath("$.data.assetPool[1].rawSymbol").value("BTCUSDT"));
        }
        verify(dashboardHomeService, times(2)).getHomeForUser(7L, null, null, null);
        verifyNoInteractions(cards);
    }

    private MockMvc cardMvc(AssetCardService cards) {
        var controller = new DashboardHomeController(dashboardHomeService, authenticatedUserIdResolver);
        controller.setAssetCardService(cards);
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /** Real request-facing service with only local test doubles; never starts background workers. */
    private static final class CardReadFixture implements AutoCloseable {
        final AssetPoolService pool = mock(AssetPoolService.class);
        final AssetCardMapper mapper = mock(AssetCardMapper.class);
        final AssetCardMarketDataService market = mock(AssetCardMarketDataService.class);
        final DashboardLiveEventService events = mock(DashboardLiveEventService.class);
        final AssetCardProperties properties = new AssetCardProperties();
        final AssetCardService service;
        CardReadFixture() {
            properties.setEnabled(true);
            properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            service = new AssetCardService(properties, market, mapper, pool, events, new ObjectMapper().findAndRegisterModules());
        }
        @Override public void close() { service.close(); }
    }

    @Test
    void homeSerializesBusinessLabelsAndFailClosedPlanSemantics() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");

        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setSymbol("BTC/USDT");
        asset.setRawSymbol("BTCUSDT");
        asset.setAnalysisId("analysis-BTCUSDT-exact");
        asset.setAssetState("CONFUSED");
        asset.setAssetStateLabel("冲突状态");
        asset.setMarketBias("WAIT");
        asset.setMarketBiasLabel("观望");
        asset.setWorthOpening(false);
        home.setAssets(List.of(asset));

        DashboardHomeVO.ExecutionSuggestionVO suggestion = new DashboardHomeVO.ExecutionSuggestionVO();
        suggestion.setStatus("DATA_QUALITY_BLOCKED");
        suggestion.setStatusLabel("当前暂无完整执行计划");
        suggestion.setBlockedReason("数据质量不足，暂不交易 / 事件观望");
        home.setExecutionSuggestion(suggestion);

        DashboardHomeVO.AiDecisionVO ai = new DashboardHomeVO.AiDecisionVO();
        ai.setRunStatus("NOT_CALLED");
        ai.setRunStatusLabel("未调用");
        ai.setDecisionModeLabel("仅规则判断");
        DashboardHomeVO.ConsistencyVO consistency = new DashboardHomeVO.ConsistencyVO();
        consistency.setDataState("SOURCE_UNAVAILABLE");
        ai.setConsistency(consistency);
        home.setAiDecision(ai);

        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 6, null)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assets[0].assetState").value("CONFUSED"))
                .andExpect(jsonPath("$.data.assets[0].assetStateLabel").value("冲突状态"))
                .andExpect(jsonPath("$.data.assets[0].analysisId").value("analysis-BTCUSDT-exact"))
                .andExpect(jsonPath("$.data.assets[0].marketBias").value("WAIT"))
                .andExpect(jsonPath("$.data.assets[0].marketBiasLabel").value("观望"))
                .andExpect(jsonPath("$.data.aiDecision.runStatusLabel").value("未调用"))
                .andExpect(jsonPath("$.data.aiDecision.decisionModeLabel").value("仅规则判断"))
                .andExpect(jsonPath("$.data.aiDecision.consistency.dataState").value("SOURCE_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.aiDecision.consistency.consistencyLevel").doesNotExist())
                .andExpect(jsonPath("$.data.executionSuggestion.status").value("DATA_QUALITY_BLOCKED"))
                .andExpect(jsonPath("$.data.executionSuggestion.entryZone").doesNotExist())
                .andExpect(jsonPath("$.data.executionSuggestion.stopLoss").doesNotExist())
                .andExpect(jsonPath("$.data.executionSuggestion.takeProfitRules").doesNotExist());
    }

    @Test
    void homeSerializesOpportunityRankingProjectionContract() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("LINKUSDT");

        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setAssetId(9_007_199_254_740_993L);
        asset.setSymbol("LINK/USDT");
        asset.setRawSymbol("LINKUSDT");
        asset.setName("Chainlink");
        asset.setAnalysisId("analysis-link-ranked");
        asset.setOpportunityId("opportunity-link-ranked");
        asset.setOpportunityState("CANDIDATE");
        asset.setOpportunityScore(94);
        asset.setPlanMode("CONFIRM");
        asset.setAiDecisionResult("LEVEL_1_CONSISTENT");
        asset.setDataQualityScore(91);
        asset.setRankingReason("OPPORTUNITY_SCORE=94|CONFIDENCE=HIGH|RISK_LEVEL=LOW"
                + "|PLAN_MODE=CONFIRM|AI_DECISION=LEVEL_1_CONSISTENT|DATA_QUALITY=91");
        home.setAssets(List.of(asset));

        when(dashboardHomeService.getHomeForUser(7L, null, 6, null)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home").param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assets[0].assetId").value("9007199254740993"))
                .andExpect(jsonPath("$.data.assets[0].symbol").value("LINK/USDT"))
                .andExpect(jsonPath("$.data.assets[0].name").value("Chainlink"))
                .andExpect(jsonPath("$.data.assets[0].analysisId").value("analysis-link-ranked"))
                .andExpect(jsonPath("$.data.assets[0].opportunityId").value("opportunity-link-ranked"))
                .andExpect(jsonPath("$.data.assets[0].opportunityState").value("CANDIDATE"))
                .andExpect(jsonPath("$.data.assets[0].opportunityScore").value(94))
                .andExpect(jsonPath("$.data.assets[0].planMode").value("CONFIRM"))
                .andExpect(jsonPath("$.data.assets[0].aiDecisionResult").value("LEVEL_1_CONSISTENT"))
                .andExpect(jsonPath("$.data.assets[0].dataQualityScore").value(91))
                .andExpect(jsonPath("$.data.assets[0].rankingReason")
                        .value("OPPORTUNITY_SCORE=94|CONFIDENCE=HIGH|RISK_LEVEL=LOW"
                                + "|PLAN_MODE=CONFIRM|AI_DECISION=LEVEL_1_CONSISTENT|DATA_QUALITY=91"));
    }

    @Test
    void homeControllerPassesSelectedPositionId() throws Exception {
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedSymbol("BTCUSDT");
        home.setSelectedPositionId(42L);
        home.setPositionSelectionStatus("EXACT_POSITION_SELECTED");
        when(dashboardHomeService.getHomeForUser(7L, "BTCUSDT", 6, 42L)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("selectedSymbol", "BTCUSDT")
                        .param("positionId", "42")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedPositionId").value("42"))
                .andExpect(jsonPath("$.data.positionSelectionStatus").value("EXACT_POSITION_SELECTED"));

        verify(dashboardHomeService).getHomeForUser(7L, "BTCUSDT", 6, 42L);
    }

    @Test
    void homeSerializesPositionIdentityBeyondJavascriptSafeIntegerAsStrings() throws Exception {
        long positionId = 9_007_199_254_740_993L;
        DashboardHomeVO home = new DashboardHomeVO();
        home.setSelectedPositionId(positionId);
        DashboardHomeVO.PositionVO position = new DashboardHomeVO.PositionVO();
        position.setPositionId(positionId);
        position.setPositionStatus("OPEN");
        home.setPositions(List.of(position));
        when(dashboardHomeService.getHomeForUser(7L, null, null, positionId)).thenReturn(home);

        mockMvc.perform(get("/api/dashboard/home")
                        .param("positionId", Long.toString(positionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedPositionId")
                        .value("9007199254740993"))
                .andExpect(jsonPath("$.data.positions[0].positionId")
                        .value("9007199254740993"));
    }
}
