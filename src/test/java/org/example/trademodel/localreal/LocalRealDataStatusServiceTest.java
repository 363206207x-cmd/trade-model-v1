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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalRealDataStatusServiceTest {
    @Mock PersistedOhlcvBarMapper ohlcvMapper;
    @Mock AnalysisRunMapper analysisRunMapper;
    @Mock DecisionResultMapper decisionResultMapper;
    @Mock RoutedPublicOhlcvProvider routedProvider;
    @Mock PersistedRealMarketEnvironmentService realMarketEnvironmentService;

    @Test
    void localRealStatusDoesNotExposeSecretsAndShowsAiDisabled() throws Exception {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        readiness.transition(LocalRealReadinessState.DEGRADED, "MARKET_WINDOW_INCOMPLETE");
        when(ohlcvMapper.countAllClosedBars()).thenReturn(0L);
        when(analysisRunMapper.countLocalRealSuccessfulSymbols()).thenReturn(0);
        when(routedProvider.primaryProvider()).thenReturn("KRAKEN");
        when(routedProvider.health()).thenReturn(Map.of());
        when(routedProvider.krakenPairCacheState()).thenReturn(KrakenPairCacheState.READY);
        when(routedProvider.requestPair("BTCUSDT")).thenReturn("XBTUSD");
        LocalRealDataStatusService service = new LocalRealDataStatusService(
                readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService);

        Map<String, Object> status = service.status();
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(status).toLowerCase();

        assertThat(status.get("ai")).isEqualTo(Map.of("enabled", false, "status", "DISABLED"));
        assertThat(json).doesNotContain("api_key", "apikey", "password", "authorization", "prompt");
        assertThat(status).containsEntry("notAutoTrading", true).containsEntry("notOrderExecution", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> market = (Map<String, Object>) status.get("marketData");
        assertThat(market).containsEntry("provider", "KRAKEN").containsEntry("readyAssetCount", 0L);
        assertThat((java.util.List<?>) market.get("assets")).hasSize(6);
        @SuppressWarnings("unchecked")
        Map<String, Object> providers = (Map<String, Object>) status.get("providers");
        assertThat(providers).containsEntry("primary", "KRAKEN")
                .containsEntry("krakenPairCacheState", "READY");
        assertThat((java.util.List<?>) status.get("assets")).hasSize(6);
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

        Map<String, Object> status = service.status();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> assets = (List<Map<String, Object>>) status.get("assets");
        assertThat(assets.get(0)).containsEntry("marketDataStatus", "READY")
                .containsEntry("realMarketEnvironment", true)
                .containsEntry("analysisStatus", "FAILED")
                .containsEntry("latestAnalysisFailureCode", "REAL_MARKET_ENVIRONMENT_REQUIRED");
    }
}
