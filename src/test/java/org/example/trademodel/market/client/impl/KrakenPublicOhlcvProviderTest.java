package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicMarketHttpResult;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KrakenPublicOhlcvProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void krakenMapsFiveMinuteInterval() {
        assertThat(KrakenPublicOhlcvProvider.intervalMinutes("5m")).isEqualTo(5);
    }

    @Test
    void krakenMapsFifteenMinuteInterval() {
        assertThat(KrakenPublicOhlcvProvider.intervalMinutes("15m")).isEqualTo(15);
    }

    @Test
    void krakenMapsOneHourInterval() {
        assertThat(KrakenPublicOhlcvProvider.intervalMinutes("1h")).isEqualTo(60);
    }

    @Test
    void krakenMapsFourHourInterval() {
        assertThat(KrakenPublicOhlcvProvider.intervalMinutes("4h")).isEqualTo(240);
    }

    @Test
    void productionShapeMapsClosedBarsAndAlwaysDropsLastCandle() throws Exception {
        RealMarketDataFetcherService fetcher = mock(RealMarketDataFetcherService.class);
        Instant fetchedAt = Instant.parse("2026-07-13T10:30:00Z");
        String body = """
                {"error":[],"result":{"XXBTZUSD":[
                  [1783936800,"100","110","90","105","102","12.5",10],
                  [1783937100,"105","112","101","108","106","13.5",11],
                  [1783937400,"108","115","107","114","110","4.0",3]
                ],"last":1783937400}}
                """;
        when(fetcher.fetchPublicJson(eq("KRAKEN"), contains("pair=BTC%2FUSD")))
                .thenReturn(new PublicMarketHttpResult(OhlcvSourceState.READY, null, 200, fetchedAt,
                        mapper.readTree(body)));
        KrakenPublicOhlcvProvider provider = new KrakenPublicOhlcvProvider(
                fetcher, true, true, "https://api.kraken.com");

        PublicOhlcvProviderResult result = provider.fetchClosedBars("BTCUSDT", "5m", 100, "run-1");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch().provider()).isEqualTo("KRAKEN");
        assertThat(result.batch().sourceEndpoint()).isEqualTo("/0/public/OHLC");
        assertThat(result.batch().bars()).hasSize(2);
        assertThat(result.batch().bars().get(0).volume()).isEqualByComparingTo("12.5");
        assertThat(result.batch().bars()).extracting(bar -> bar.openTimeMs())
                .containsExactly(1783936800000L, 1783937100000L);
    }

    @Test
    void invalidGeometryFailsClosed() throws Exception {
        RealMarketDataFetcherService fetcher = mock(RealMarketDataFetcherService.class);
        String body = """
                {"error":[],"result":{"XETHZUSD":[
                  [1783936800,"100","90","95","105","100","1.0",1],
                  [1783937100,"100","110","90","105","100","1.0",1]
                ],"last":1783937100}}
                """;
        when(fetcher.fetchPublicJson(eq("KRAKEN"), contains("pair=ETH%2FUSD")))
                .thenReturn(new PublicMarketHttpResult(OhlcvSourceState.READY, null, 200, Instant.now(),
                        mapper.readTree(body)));

        PublicOhlcvProviderResult result = new KrakenPublicOhlcvProvider(fetcher, true, true,
                "https://api.kraken.com").fetchClosedBars("ETHUSDT", "5m", 100, "run-1");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.ERROR);
        assertThat(result.reasonCode()).isEqualTo("INVALID_RESPONSE");
    }

    @Test
    void krakenRejectsInvalidPayload() throws Exception {
        RealMarketDataFetcherService fetcher = mock(RealMarketDataFetcherService.class);
        when(fetcher.fetchPublicJson(eq("KRAKEN"), contains("pair=XRP%2FUSD")))
                .thenReturn(new PublicMarketHttpResult(OhlcvSourceState.READY, null, 200, Instant.now(),
                        mapper.readTree("{\"error\":[],\"result\":{}}")));

        PublicOhlcvProviderResult result = new KrakenPublicOhlcvProvider(fetcher, true, true,
                "https://api.kraken.com").fetchClosedBars("XRPUSDT", "5m", 100, "run-1");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.ERROR);
        assertThat(result.reasonCode()).isEqualTo("INVALID_RESPONSE");
    }

    @Test
    void assetPairsResolutionUsesDisplayPairOnlyWhenPresent() throws Exception {
        assertThat(KrakenPublicOhlcvProvider.parseAssetVersionDisplayPair(mapper.readTree(
                "{\"error\":[],\"result\":{\"BNBUSD\":{\"wsname\":\"BNB/USD\"}}}"), "BNB/USD"))
                .isEqualTo("BNB/USD");
        assertThat(KrakenPublicOhlcvProvider.parseAssetVersionDisplayPair(mapper.readTree(
                "{\"error\":[\"EQuery:Unknown asset pair\"],\"result\":{}}"), "BNB/USD"))
                .isNull();
    }

    @Test
    void urlContainsIncrementalSinceAndAssetVersion() {
        String url = KrakenPublicOhlcvProvider.buildOhlcUrl("https://api.kraken.com", "BTC/USD",
                "15m", 100, Instant.parse("2026-07-13T10:30:00Z"));
        assertThat(url).contains("/0/public/OHLC", "pair=BTC%2FUSD", "interval=15", "since=", "assetVersion=1");
    }
}
