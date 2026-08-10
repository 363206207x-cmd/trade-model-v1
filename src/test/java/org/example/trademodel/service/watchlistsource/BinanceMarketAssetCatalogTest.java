package org.example.trademodel.service.watchlistsource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("core-regression")
class BinanceMarketAssetCatalogTest {

    @Test
    void exchangeCatalogKeepsOnlyTradableUsdtMarkets() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThat(BinanceMarketAssetCatalog.parseExchangeInfo(objectMapper.readTree("""
                {"symbols":[
                  {"symbol":"BTCUSDT","baseAsset":"BTC","quoteAsset":"USDT","status":"TRADING"},
                  {"symbol":"ETHBTC","baseAsset":"ETH","quoteAsset":"BTC","status":"TRADING"},
                  {"symbol":"OLDUSDT","baseAsset":"OLD","quoteAsset":"USDT","status":"BREAK"},
                  {"symbol":"BTCUSDT","baseAsset":"BTC","quoteAsset":"USDT","status":"TRADING"}
                ]}
                """)))
                .containsExactly(new MarketAssetDTO("BTCUSDT", "BTC", "USDT", "SPOT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void marketSearchIsCaseInsensitiveAndFuzzyAcrossSymbolAndBaseAsset() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {"symbols":[
                  {"symbol":"BTCUSDT","baseAsset":"BTC","quoteAsset":"USDT","status":"TRADING"},
                  {"symbol":"ETHUSDT","baseAsset":"ETH","quoteAsset":"USDT","status":"TRADING"}
                ]}
                """);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        BinanceMarketAssetCatalog catalog = new BinanceMarketAssetCatalog(
                new ObjectMapper(), mock(ProviderSymbolMappingRegistry.class), httpClient);

        assertThat(catalog.search("bt", 10)).extracting(MarketAssetDTO::symbol)
                .containsExactly("BTCUSDT");
        assertThat(catalog.requireTradable("eth/usdt").symbol()).isEqualTo("ETHUSDT");
    }
}
