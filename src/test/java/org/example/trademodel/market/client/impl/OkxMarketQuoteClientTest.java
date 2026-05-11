package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OkxMarketQuoteClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseOkxTickerJson_mapsTickerToMarketQuote() throws Exception {
        String json = """
                {"code":"0","msg":"","data":[{"instId":"BTC-USDT","last":"102","open24h":"100","high24h":"105","low24h":"95"}]}
                """;

        Optional<MarketQuoteSnapshot> out =
                OkxMarketQuoteClient.parseOkxTickerJson(objectMapper.readTree(json), "BTCUSDT");

        assertTrue(out.isPresent());
        MarketQuoteSnapshot snap = out.get();
        assertEquals(OkxMarketQuoteClient.PROVIDER, snap.getProvider());
        assertEquals("BTCUSDT", snap.getSymbolNormalized());
        assertEquals(0, new BigDecimal("102").compareTo(snap.getLastPrice()));
        assertEquals(0, new BigDecimal("2.00000000").compareTo(snap.getPriceChangePercent24h()));
        assertEquals(0, new BigDecimal("105").compareTo(snap.getHighPrice()));
        assertEquals(0, new BigDecimal("95").compareTo(snap.getLowPrice()));
    }

    @Test
    void parseOkxTickerJson_emptyWhenCodeFails() throws Exception {
        String json = "{\"code\":\"50011\",\"data\":[]}";

        Optional<MarketQuoteSnapshot> out =
                OkxMarketQuoteClient.parseOkxTickerJson(objectMapper.readTree(json), "BTCUSDT");

        assertTrue(out.isEmpty());
    }

    @Test
    void toOkxInstId_convertsUsdtPair() {
        assertEquals("ETH-USDT", OkxMarketQuoteClient.toOkxInstId("ethusdt"));
    }
}
