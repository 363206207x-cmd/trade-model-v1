package org.example.trademodel.localreal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.market.client.impl.RoutedPublicOhlcvProvider;
import org.example.trademodel.market.client.impl.KrakenPairCacheState;
import org.example.trademodel.market.PersistedRealMarketEnvironmentService;
import org.example.trademodel.market.PersistedRealMarketEnvironmentAssessment;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.example.trademodel.dto.ohlcv.PublicProviderHealthSnapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalRealDataStatusServiceTest {
    @Mock PersistedOhlcvBarMapper ohlcvMapper;
    @Mock AnalysisRunMapper analysisRunMapper;
    @Mock DecisionResultMapper decisionResultMapper;
    @Mock RoutedPublicOhlcvProvider routedProvider;
    @Mock PersistedRealMarketEnvironmentService realMarketEnvironmentService;
    @Mock AssetPoolService assetPoolService;

    private static final List<String> POOL_SYMBOLS = List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");

    @Test
    void localRealStatusDoesNotExposeSecretsAndShowsAiDisabled() throws Exception {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        readiness.transition(LocalRealReadinessState.DEGRADED, "MARKET_WINDOW_INCOMPLETE");
        when(ohlcvMapper.countAllClosedBars()).thenReturn(0L);
        when(analysisRunMapper.countLocalRealSuccessfulSymbols()).thenReturn(0);
        when(routedProvider.primaryProvider()).thenReturn("KRAKEN");
        when(routedProvider.health()).thenReturn(Map.of());
        when(routedProvider.krakenPairCacheState()).thenReturn(KrakenPairCacheState.READY);
        when(routedProvider.requestPair(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation ->
                "BTCUSDT".equals(invocation.getArgument(0)) ? "XBTUSD" : invocation.getArgument(0));
        LocalRealDataStatusService service = new LocalRealDataStatusService(
                readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService);
        service.setAssetPoolService(assetPoolService);
        when(assetPoolService.listScanSymbols()).thenReturn(List.of("BTCUSDT"));

        Map<String, Object> status = service.status();
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(status).toLowerCase();

        assertThat(status.get("ai")).isEqualTo(Map.of("enabled", false, "status", "DISABLED"));
        assertThat(json).doesNotContain("api_key", "apikey", "password", "authorization", "prompt");
        assertThat(status).containsEntry("notAutoTrading", true).containsEntry("notOrderExecution", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> market = (Map<String, Object>) status.get("marketData");
        assertThat(market).containsEntry("provider", "KRAKEN").containsEntry("readyAssetCount", 0L);
        assertThat((java.util.List<?>) market.get("assets")).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> providers = (Map<String, Object>) status.get("providers");
        assertThat(providers).containsEntry("primary", "KRAKEN")
                .containsEntry("krakenPairCacheState", "READY");
        assertThat((java.util.List<?>) status.get("assets")).hasSize(1);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> assets = (java.util.List<Map<String, Object>>) status.get("assets");
        assertThat(assets.get(0)).containsEntry("symbol", "BTCUSDT")
                .containsEntry("provider", "KRAKEN")
                .containsEntry("requestPair", "XBTUSD")
                .containsEntry("marketDataStatus", "MARKET_DATA_NOT_READY")
                .containsEntry("realMarketEnvironment", false)
                .containsEntry("analysisStatus", "WAITING");
    }

    @Test
    void statusApiReportsMarketReadyAndLatestAnalysisFailureSeparately() {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        readiness.updateAsset("BTCUSDT", LocalRealAssetReadinessState.DEGRADED, "KRAKEN",
                "INITIAL_ANALYSIS_INCOMPLETE");
        when(ohlcvMapper.countAllClosedBars()).thenReturn(2424L);
        when(analysisRunMapper.countLocalRealSuccessfulSymbols()).thenReturn(0);
        when(routedProvider.primaryProvider()).thenReturn("KRAKEN");
        when(routedProvider.health()).thenReturn(Map.of());
        when(routedProvider.krakenPairCacheState()).thenReturn(KrakenPairCacheState.READY);
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setProvider("KRAKEN");
        bar.setFreshnessStatus("FRESH");
        bar.setCloseTimeMs(123L);
        when(ohlcvMapper.selectLatestClosedBarBySymbol("BTCUSDT")).thenReturn(bar);
        MarketEnvironmentVO environment = new MarketEnvironmentVO();
        when(realMarketEnvironmentService.assess("BTCUSDT", "5m")).thenReturn(
                new PersistedRealMarketEnvironmentAssessment(true, null, "KRAKEN",
                        "KRAKEN_PERSISTED_OHLCV", environment, Map.of(), 400, 123L, List.of("trace-1")));
        AnalysisRunDO failed = new AnalysisRunDO();
        failed.setStatus("FAILED");
        failed.setErrorCode("IllegalStateException");
        failed.setErrorMessage("REAL_MARKET_ENVIRONMENT_REQUIRED");
        when(analysisRunMapper.selectLatestBySymbol("BTCUSDT")).thenReturn(failed);
        LocalRealDataStatusService service = new LocalRealDataStatusService(
                readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService);
        service.setAssetPoolService(assetPoolService);
        when(assetPoolService.listScanSymbols()).thenReturn(List.of("BTCUSDT"));

        Map<String, Object> status = service.status();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assets = (List<Map<String, Object>>) status.get("assets");
        assertThat(assets.get(0)).containsEntry("marketDataStatus", "READY")
                .containsEntry("realMarketEnvironment", true)
                .containsEntry("analysisStatus", "FAILED")
                .containsEntry("latestAnalysisFailureCode", "REAL_MARKET_ENVIRONMENT_REQUIRED");
    }

    @Test
    void latestClosedBarAtUsesPersistedCloseTimeAndFailsClosedWithoutABar() {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        PersistedOhlcvBarDO persisted = new PersistedOhlcvBarDO();
        persisted.setCloseTimeMs(Instant.parse("2026-08-20T09:56:00Z").toEpochMilli());
        when(ohlcvMapper.selectLatestClosedBar()).thenReturn(persisted, null);
        LocalRealDataStatusService service = new LocalRealDataStatusService(
                readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService);

        assertThat(service.latestClosedBarAt()).isEqualTo(Instant.parse("2026-08-20T09:56:00Z"));
        assertThat(service.latestClosedBarAt()).isNull();
    }

    @Test
    void providerReadinessUsesCurrentBarAgeAndFailsClosedAfterFreshnessWindow() {
        Instant now = Instant.parse("2026-08-10T14:30:00Z");
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        for (String symbol : POOL_SYMBOLS) {
            readiness.updateAsset(symbol, LocalRealAssetReadinessState.READY, "KRAKEN",
                    "REAL_DATA_AVAILABLE");
        }
        readiness.transition(LocalRealReadinessState.DASHBOARD_READY, "REAL_DATA_AVAILABLE");
        when(analysisRunMapper.countLocalRealSuccessfulSymbols()).thenReturn(6);
        when(routedProvider.primaryProvider()).thenReturn("KRAKEN");
        when(routedProvider.krakenPairCacheState()).thenReturn(KrakenPairCacheState.READY);
        when(routedProvider.health()).thenReturn(Map.of(
                "kraken", new PublicProviderHealthSnapshot(
                        "KRAKEN", "UP", now, null, false, null)));

        PersistedOhlcvBarDO fresh = readyBar(now.minusSeconds(5 * 60));
        PersistedOhlcvBarDO stale = readyBar(now.minusSeconds(12 * 60));
        PersistedOhlcvBarDO invalid = readyBar(now.minusSeconds(5 * 60));
        invalid.setSourceStatus("INVALID");
        when(ohlcvMapper.selectLatestClosedBar()).thenReturn(fresh, stale, invalid);
        LocalRealDataStatusService service = new LocalRealDataStatusService(
                readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService, Clock.fixed(now, ZoneOffset.UTC));
        service.setAssetPoolService(assetPoolService);
        when(assetPoolService.listScanSymbols()).thenReturn(POOL_SYMBOLS);

        LocalRealDataStatusService.ProviderReadinessSnapshot freshSnapshot =
                service.providerReadinessSnapshot();
        LocalRealDataStatusService.ProviderReadinessSnapshot staleSnapshot =
                service.providerReadinessSnapshot();
        LocalRealDataStatusService.ProviderReadinessSnapshot invalidSnapshot =
                service.providerReadinessSnapshot();

        assertThat(freshSnapshot.dashboardReady()).isTrue();
        assertThat(freshSnapshot.freshnessStatus()).isEqualTo("FRESH");
        assertThat(staleSnapshot.dashboardReady()).isFalse();
        assertThat(staleSnapshot.freshnessStatus()).isEqualTo("STALE");
        assertThat(staleSnapshot.reasonCode()).isEqualTo("LOCAL_REAL_MARKET_DATA_STALE");
        assertThat(invalidSnapshot.dashboardReady()).isFalse();
        assertThat(invalidSnapshot.freshnessStatus()).isEqualTo("INVALID");
        assertThat(invalidSnapshot.reasonCode()).isEqualTo("LOCAL_REAL_MARKET_DATA_INVALID");
    }

    @Test
    void persistedFreshProviderEvidenceRestoresReadinessAfterRuntimeHealthReset() {
        Instant now = Instant.parse("2026-08-10T14:30:00Z");
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        for (String symbol : POOL_SYMBOLS) {
            readiness.updateAsset(symbol, LocalRealAssetReadinessState.READY, "KRAKEN",
                    "REAL_DATA_AVAILABLE");
        }
        readiness.transition(LocalRealReadinessState.DASHBOARD_READY, "REAL_DATA_AVAILABLE");
        when(analysisRunMapper.countLocalRealSuccessfulSymbols()).thenReturn(6);
        when(routedProvider.primaryProvider()).thenReturn("KRAKEN");
        when(routedProvider.krakenPairCacheState()).thenReturn(KrakenPairCacheState.NOT_LOADED);
        when(routedProvider.health()).thenReturn(Map.of(
                "kraken", new PublicProviderHealthSnapshot(
                        "KRAKEN", "NOT_USED", null, null, false, null)));
        PersistedOhlcvBarDO fresh = readyBar(now.minusSeconds(5 * 60));
        fresh.setProvider("KRAKEN");
        when(ohlcvMapper.selectLatestClosedBar()).thenReturn(fresh);
        LocalRealDataStatusService service = new LocalRealDataStatusService(
                readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService, Clock.fixed(now, ZoneOffset.UTC));
        service.setAssetPoolService(assetPoolService);
        when(assetPoolService.listScanSymbols()).thenReturn(POOL_SYMBOLS);

        LocalRealDataStatusService.ProviderReadinessSnapshot snapshot =
                service.providerReadinessSnapshot();

        assertThat(snapshot.dashboardReady()).isTrue();
        assertThat(snapshot.freshnessStatus()).isEqualTo("FRESH");
        assertThat(snapshot.reasonCode()).isEqualTo("REAL_DATA_AVAILABLE");
    }

    private PersistedOhlcvBarDO readyBar(Instant closedAt) {
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setTimeframe("5m");
        bar.setCloseTimeMs(closedAt.toEpochMilli());
        bar.setSourceStatus("READY");
        bar.setFreshnessStatus("FRESH");
        return bar;
    }
}
