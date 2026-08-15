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
    private static final Instant FETCHED_AT = Instant.parse("2026-07-13T10:30:00Z");

    @Test void krakenMapsFiveMinuteInterval() { assertThat(KrakenPublicOhlcvProvider.intervalMinutes("5m")).isEqualTo(5); }
    @Test void krakenMapsFifteenMinuteInterval() { assertThat(KrakenPublicOhlcvProvider.intervalMinutes("15m")).isEqualTo(15); }
    @Test void krakenMapsOneHourInterval() { assertThat(KrakenPublicOhlcvProvider.intervalMinutes("1h")).isEqualTo(60); }
    @Test void krakenMapsFourHourInterval() { assertThat(KrakenPublicOhlcvProvider.intervalMinutes("4h")).isEqualTo(240); }

    @Test
    void ohlcRequestUsesAltnameNotWsnameAndDropsCurrentCandle() throws Exception {
        RealMarketDataFetcherService fetcher = mock(RealMarketDataFetcherService.class);
        stubAssetPairs(fetcher);
        when(fetcher.fetchPublicJson(eq("KRAKEN"), contains("pair=XBTUSDT")))
                .thenReturn(ready(ohlc("XXBTUSDT")));
        KrakenPublicOhlcvProvider provider = provider(fetcher);

        PublicOhlcvProviderResult result = provider.fetchClosedBars("BTCUSDT", "5m", 100, "run-1");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch().provider()).isEqualTo("KRAKEN");
        assertThat(result.batch().bars()).hasSize(2);
        assertThat(provider.cachedRequestPair("BTCUSDT")).isEqualTo("XBTUSDT");
    }

    @Test
    void ohlcRequestDoesNotContainEncodedSlashPair() {
        String url = KrakenPublicOhlcvProvider.buildOhlcUrl("https://api.kraken.com", "XBTUSD",
                "15m", 100, FETCHED_AT);
        assertThat(url).contains("pair=XBTUSD", "interval=15", "since=", "assetVersion=1")
                .doesNotContain("%2F", "BTC/USD", "XBT/USD");
    }

    @Test
    void responseParsesInternalResultKey() throws Exception {
        assertReadyFromDirectMap("XXBTUSDT", metadata("BTCUSDT", "XBTUSDT", "XBT/USDT", "XXBTUSDT"));
    }

    @Test
    void responseParsesDisplayResultKeyAndIgnoresLastField() throws Exception {
        assertReadyFromDirectMap("BTC/USDT", metadata("BTCUSDT", "XBTUSDT", "XBT/USDT", "XXBTUSDT"));
    }

    @Test
    void responseParsesDogeAlternateResultKey() throws Exception {
        assertReadyFromDirectMap("DOGE/USDT", metadata("DOGEUSDT", "XDGUSDT", "XDG/USDT", "XXDGUSDT"));
    }

    @Test
    void invalidGeometryFailsClosed() throws Exception {
        KrakenPublicOhlcvProvider provider = provider(mock(RealMarketDataFetcherService.class));
        String body = """
                {"error":[],"result":{"XETHUSDT":[
                  [1783936800,"100","90","95","105","100","1.0",1],
                  [1783937100,"100","110","90","105","100","1.0",1]
                ],"last":1783937100}}
                """;

        PublicOhlcvProviderResult result = provider.mapResponse("ETHUSDT", "5m", 100, "run-1",
                ready(body), metadata("ETHUSDT", "ETHUSDT", "ETH/USDT", "XETHUSDT"));

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.ERROR);
        assertThat(result.reasonCode()).isEqualTo("INVALID_RESPONSE");
    }

    @Test
    void krakenRejectsInvalidPayload() throws Exception {
        KrakenPublicOhlcvProvider provider = provider(mock(RealMarketDataFetcherService.class));
        PublicOhlcvProviderResult result = provider.mapResponse("XRPUSDT", "5m", 100, "run-1",
                ready("{\"error\":[],\"result\":{}}"),
                metadata("XRPUSDT", "XRPUSDT", "XRP/USDT", "XXRPUSDT"));
        assertThat(result.reasonCode()).isEqualTo("INVALID_RESPONSE");
    }

    @Test
    void unknownPairAfterSuccessfulResolutionIsMappingErrorNotPairAbsence() throws Exception {
        KrakenPublicOhlcvProvider provider = provider(mock(RealMarketDataFetcherService.class));
        PublicOhlcvProviderResult result = provider.mapResponse("BTCUSDT", "5m", 100, "run-1",
                ready("{\"error\":[\"EQuery:Unknown asset pair\"],\"result\":{}}"),
                metadata("BTCUSDT", "XBTUSDT", "XBT/USDT", "XXBTUSDT"));
        assertThat(result.reasonCode()).isEqualTo("KRAKEN_OHLC_UNKNOWN_PAIR");
    }

    private void assertReadyFromDirectMap(String resultKey, KrakenPairMetadata metadata) throws Exception {
        KrakenPublicOhlcvProvider provider = provider(mock(RealMarketDataFetcherService.class));
        PublicOhlcvProviderResult result = provider.mapResponse(metadata.internalSymbol(), "5m", 100, "run-1",
                ready(ohlc(resultKey)), metadata);
        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch().bars()).hasSize(2);
    }

    private KrakenPublicOhlcvProvider provider(RealMarketDataFetcherService fetcher) {
        return new KrakenPublicOhlcvProvider(fetcher, true, true, "https://api.kraken.com",
                new KrakenPairResolver(fetcher, "https://api.kraken.com"));
    }

    private void stubAssetPairs(RealMarketDataFetcherService fetcher) throws Exception {
        when(fetcher.fetchPublicJson(eq("KRAKEN"), contains("/0/public/AssetPairs")))
                .thenReturn(ready(KrakenPairResolverTest.assetPairsFixture(false)));
    }

    private PublicMarketHttpResult ready(String json) throws Exception {
        return new PublicMarketHttpResult(OhlcvSourceState.READY, null, 200, FETCHED_AT, mapper.readTree(json));
    }

    private static KrakenPairMetadata metadata(String symbol, String request, String display, String key) {
        return new KrakenPairMetadata(symbol, request, display, key, "online");
    }

    private static String ohlc(String resultKey) {
        return "{\"error\":[],\"result\":{\"" + resultKey + "\":["
                + "[1783936800,\"100\",\"110\",\"90\",\"105\",\"102\",\"12.5\",10],"
                + "[1783937100,\"105\",\"112\",\"101\",\"108\",\"106\",\"13.5\",11],"
                + "[1783937400,\"108\",\"115\",\"107\",\"114\",\"110\",\"4.0\",3]],"
                + "\"last\":1783937400}}";
    }
}
