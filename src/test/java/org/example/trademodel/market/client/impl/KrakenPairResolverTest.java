package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicMarketHttpResult;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KrakenPairResolverTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void assetPairsMapsBtcUsdtToXbtUsdAltname() throws Exception { assertPair("BTCUSDT", "XBTUSD", "XBT/USD"); }
    @Test void assetPairsMapsEthUsdtToEthUsdAltname() throws Exception { assertPair("ETHUSDT", "ETHUSD", "ETH/USD"); }
    @Test void assetPairsMapsSolUsdt() throws Exception { assertPair("SOLUSDT", "SOLUSD", "SOL/USD"); }
    @Test void assetPairsMapsXrpUsdt() throws Exception { assertPair("XRPUSDT", "XRPUSD", "XRP/USD"); }
    @Test void assetPairsMapsDogeUsdtToXdgUsd() throws Exception { assertPair("DOGEUSDT", "XDGUSD", "XDG/USD"); }

    @Test
    void assetPairsHandlesBtcAndXbtDisplayAliases() throws Exception {
        assertPairFromFixture("BTCUSDT", "XBTUSD", fixtureWithAliases("BTC/USD", "XDG/USD"));
        assertPairFromFixture("BTCUSDT", "XBTUSD", fixtureWithAliases("XBT/USD", "XDG/USD"));
    }

    @Test
    void assetPairsHandlesDogeAndXdgDisplayAliases() throws Exception {
        assertPairFromFixture("DOGEUSDT", "XDGUSD", fixtureWithAliases("XBT/USD", "DOGE/USD"));
        assertPairFromFixture("DOGEUSDT", "XDGUSD", fixtureWithAliases("XBT/USD", "XDG/USD"));
    }

    @Test
    void assetPairsCacheLoadsOnlyOnce() throws Exception {
        RealMarketDataFetcherService fetcher = fetcher(assetPairsFixture(false));
        KrakenPairResolver resolver = resolver(fetcher);
        for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "DOGEUSDT", "BNBUSDT")) {
            resolver.resolve(symbol);
        }
        verify(fetcher, times(1)).fetchPublicJson(eq("KRAKEN"), contains("/0/public/AssetPairs"));
        assertThat(resolver.state()).isEqualTo(KrakenPairCacheState.READY);
    }

    @Test
    void twentyFourBootstrapRequestsReuseOnePairCache() throws Exception {
        RealMarketDataFetcherService fetcher = fetcher(assetPairsFixture(true));
        KrakenPairResolver resolver = resolver(fetcher);
        for (String symbol : List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT")) {
            for (String ignored : List.of("5m", "15m", "1h", "4h")) {
                assertThat(resolver.resolve(symbol).ready()).isTrue();
            }
        }
        verify(fetcher, times(1)).fetchPublicJson(eq("KRAKEN"), contains("/0/public/AssetPairs"));
    }

    @Test
    void missingAltnameFallsBackToResultObjectKey() throws Exception {
        String fixture = assetPairsFixture(false).replace("\"altname\":\"ETHUSD\",", "");
        KrakenPairResolution result = resolver(fetcher(fixture)).resolve("ETHUSDT");
        assertThat(result.ready()).isTrue();
        assertThat(result.metadata().requestPair()).isEqualTo("XETHZUSD");
    }

    @Test
    void missingWsnameForKnownCorePairFailsClosed() throws Exception {
        String fixture = assetPairsFixture(false).replace("\"wsname\":\"ETH/USD\",", "");
        KrakenPairResolver resolver = resolver(fetcher(fixture));
        KrakenPairResolution result = resolver.resolve("ETHUSDT");
        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("KRAKEN_PAIR_RESOLUTION_ERROR");
        assertThat(resolver.state()).isEqualTo(KrakenPairCacheState.FAILED);
    }

    @Test
    void bnbAbsentIsGenuinePairNotSupportedAfterReadyCache() throws Exception {
        KrakenPairResolver resolver = resolver(fetcher(assetPairsFixture(false)));
        KrakenPairResolution result = resolver.resolve("BNBUSDT");
        assertThat(result.reasonCode()).isEqualTo("PAIR_NOT_SUPPORTED");
        assertThat(resolver.state()).isEqualTo(KrakenPairCacheState.READY);
    }

    @Test
    void assetPairsLoadFailureRetriesOnceThenStaysFailed() {
        RealMarketDataFetcherService fetcher = mock(RealMarketDataFetcherService.class);
        PublicMarketHttpResult timeout = new PublicMarketHttpResult(
                OhlcvSourceState.ERROR, "TIMEOUT", 0, Instant.now(), null);
        when(fetcher.fetchPublicJson(eq("KRAKEN"), contains("/0/public/AssetPairs")))
                .thenReturn(timeout, timeout);
        KrakenPairResolver resolver = resolver(fetcher);

        assertThat(resolver.resolve("BTCUSDT").reasonCode()).isEqualTo("TIMEOUT");
        assertThat(resolver.resolve("ETHUSDT").reasonCode()).isEqualTo("TIMEOUT");
        assertThat(resolver.state()).isEqualTo(KrakenPairCacheState.FAILED);
        verify(fetcher, times(2)).fetchPublicJson(eq("KRAKEN"), contains("/0/public/AssetPairs"));
    }

    private void assertPair(String symbol, String requestPair, String displayPair) throws Exception {
        KrakenPairResolution result = resolver(fetcher(assetPairsFixture(false))).resolve(symbol);
        assertThat(result.ready()).isTrue();
        assertThat(result.metadata().requestPair()).isEqualTo(requestPair);
        assertThat(result.metadata().displayPair()).isEqualTo(displayPair);
    }

    private void assertPairFromFixture(String symbol, String requestPair, String fixture) throws Exception {
        KrakenPairResolution result = resolver(fetcher(fixture)).resolve(symbol);
        assertThat(result.ready()).isTrue();
        assertThat(result.metadata().requestPair()).isEqualTo(requestPair);
    }

    private RealMarketDataFetcherService fetcher(String fixture) throws Exception {
        RealMarketDataFetcherService fetcher = mock(RealMarketDataFetcherService.class);
        when(fetcher.fetchPublicJson(eq("KRAKEN"), contains("/0/public/AssetPairs")))
                .thenReturn(new PublicMarketHttpResult(OhlcvSourceState.READY, null, 200, Instant.now(),
                        mapper.readTree(fixture)));
        return fetcher;
    }

    private static KrakenPairResolver resolver(RealMarketDataFetcherService fetcher) {
        return new KrakenPairResolver(fetcher, "https://api.kraken.com");
    }

    static String assetPairsFixture(boolean includeBnb) {
        String bnb = includeBnb
                ? ",\"BNBUSD\":{\"altname\":\"BNBUSD\",\"wsname\":\"BNB/USD\",\"base\":\"BNB\",\"quote\":\"ZUSD\",\"status\":\"online\"}"
                : "";
        return "{\"error\":[],\"result\":{"
                + "\"XXBTZUSD\":{\"altname\":\"XBTUSD\",\"wsname\":\"XBT/USD\",\"base\":\"XXBT\",\"quote\":\"ZUSD\",\"status\":\"online\"},"
                + "\"XETHZUSD\":{\"altname\":\"ETHUSD\",\"wsname\":\"ETH/USD\",\"base\":\"XETH\",\"quote\":\"ZUSD\",\"status\":\"online\"},"
                + "\"SOLUSD\":{\"altname\":\"SOLUSD\",\"wsname\":\"SOL/USD\",\"base\":\"SOL\",\"quote\":\"ZUSD\",\"status\":\"online\"},"
                + "\"XXRPZUSD\":{\"altname\":\"XRPUSD\",\"wsname\":\"XRP/USD\",\"base\":\"XXRP\",\"quote\":\"ZUSD\",\"status\":\"online\"},"
                + "\"XXDGZUSD\":{\"altname\":\"XDGUSD\",\"wsname\":\"XDG/USD\",\"base\":\"XXDG\",\"quote\":\"ZUSD\",\"status\":\"online\"}"
                + bnb + "}}";
    }

    private static String fixtureWithAliases(String btcDisplay, String dogeDisplay) {
        return assetPairsFixture(false)
                .replace("\"wsname\":\"XBT/USD\"", "\"wsname\":\"" + btcDisplay + "\"")
                .replace("\"wsname\":\"XDG/USD\"", "\"wsname\":\"" + dogeDisplay + "\"");
    }
}
