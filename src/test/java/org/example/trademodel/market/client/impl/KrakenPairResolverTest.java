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

    @Test void assetPairsMapsBtcUsdtToExactXbtUsdtAltname() throws Exception { assertPair("BTCUSDT", "XBTUSDT", "XBT/USDT"); }
    @Test void assetPairsMapsEthUsdtToExactEthUsdtAltname() throws Exception { assertPair("ETHUSDT", "ETHUSDT", "ETH/USDT"); }
    @Test void assetPairsMapsSolUsdt() throws Exception { assertPair("SOLUSDT", "SOLUSDT", "SOL/USDT"); }
    @Test void assetPairsMapsXrpUsdt() throws Exception { assertPair("XRPUSDT", "XRPUSDT", "XRP/USDT"); }
    @Test void assetPairsMapsDogeUsdtToXdgUsdt() throws Exception { assertPair("DOGEUSDT", "XDGUSDT", "XDG/USDT"); }
    @Test void assetPairsMapsAdaUsdtFromAuthoritativeDirectory() throws Exception { assertPair("ADAUSDT", "ADAUSDT", "ADA/USDT"); }

    @Test
    void assetPairsHandlesBtcAndXbtDisplayAliases() throws Exception {
        assertPairFromFixture("BTCUSDT", "XBTUSDT", fixtureWithAliases("BTC/USDT", "XDG/USDT"));
        assertPairFromFixture("BTCUSDT", "XBTUSDT", fixtureWithAliases("XBT/USDT", "XDG/USDT"));
    }

    @Test
    void assetPairsHandlesDogeAndXdgDisplayAliases() throws Exception {
        assertPairFromFixture("DOGEUSDT", "XDGUSDT", fixtureWithAliases("XBT/USDT", "DOGE/USDT"));
        assertPairFromFixture("DOGEUSDT", "XDGUSDT", fixtureWithAliases("XBT/USDT", "XDG/USDT"));
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
        String fixture = assetPairsFixture(false).replace("\"altname\":\"ETHUSDT\",", "");
        KrakenPairResolution result = resolver(fetcher(fixture)).resolve("ETHUSDT");
        assertThat(result.ready()).isTrue();
        assertThat(result.metadata().requestPair()).isEqualTo("XETHUSDT");
    }

    @Test
    void missingWsnameFailsOnlyThatPairAndKeepsDirectoryUsable() throws Exception {
        String fixture = assetPairsFixture(false).replace("\"wsname\":\"ETH/USDT\",", "");
        KrakenPairResolver resolver = resolver(fetcher(fixture));
        KrakenPairResolution result = resolver.resolve("ETHUSDT");
        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("PAIR_NOT_SUPPORTED");
        assertThat(resolver.resolve("BTCUSDT").ready()).isTrue();
        assertThat(resolver.state()).isEqualTo(KrakenPairCacheState.READY);
    }

    @Test
    void usdPairCannotSilentlySatisfyUsdtInstrument() throws Exception {
        String usdOnly = assetPairsFixture(false)
                .replace("USDT", "USD")
                .replace("XXBTUSD", "XXBTZUSD")
                .replace("XETHUSD", "XETHZUSD");
        KrakenPairResolution result = resolver(fetcher(usdOnly)).resolve("ADAUSDT");
        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("PAIR_NOT_SUPPORTED");
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
                ? ",\"BNBUSDT\":{\"altname\":\"BNBUSDT\",\"wsname\":\"BNB/USDT\",\"base\":\"BNB\",\"quote\":\"USDT\",\"status\":\"online\"}"
                : "";
        return "{\"error\":[],\"result\":{"
                + "\"XXBTUSDT\":{\"altname\":\"XBTUSDT\",\"wsname\":\"XBT/USDT\",\"base\":\"XXBT\",\"quote\":\"USDT\",\"status\":\"online\"},"
                + "\"XETHUSDT\":{\"altname\":\"ETHUSDT\",\"wsname\":\"ETH/USDT\",\"base\":\"XETH\",\"quote\":\"USDT\",\"status\":\"online\"},"
                + "\"SOLUSDT\":{\"altname\":\"SOLUSDT\",\"wsname\":\"SOL/USDT\",\"base\":\"SOL\",\"quote\":\"USDT\",\"status\":\"online\"},"
                + "\"XXRPUSDT\":{\"altname\":\"XRPUSDT\",\"wsname\":\"XRP/USDT\",\"base\":\"XXRP\",\"quote\":\"USDT\",\"status\":\"online\"},"
                + "\"XXDGUSDT\":{\"altname\":\"XDGUSDT\",\"wsname\":\"XDG/USDT\",\"base\":\"XXDG\",\"quote\":\"USDT\",\"status\":\"online\"},"
                + "\"ADAUSDT\":{\"altname\":\"ADAUSDT\",\"wsname\":\"ADA/USDT\",\"base\":\"ADA\",\"quote\":\"USDT\",\"status\":\"online\"}"
                + bnb + "}}";
    }

    private static String fixtureWithAliases(String btcDisplay, String dogeDisplay) {
        return assetPairsFixture(false)
                .replace("\"wsname\":\"XBT/USDT\"", "\"wsname\":\"" + btcDisplay + "\"")
                .replace("\"wsname\":\"XDG/USDT\"", "\"wsname\":\"" + dogeDisplay + "\"");
    }
}
