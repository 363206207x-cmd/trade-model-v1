package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinanceMarketQuoteClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseBinance24hrJson_setsHighPriceLowPrice_whenPresent() throws Exception {
        String json = "{\"lastPrice\":\"100.0\",\"priceChangePercent\":\"1.5\",\"highPrice\":\"101.0\",\"lowPrice\":\"99.5\"}";
        Optional<MarketQuoteSnapshot> out = BinanceMarketQuoteClient.parseBinance24hrJson(
                objectMapper.readTree(json), "BTCUSDT");
        assertTrue(out.isPresent());
        MarketQuoteSnapshot s = out.get();
        assertEquals("BTCUSDT", s.getSymbolNormalized());
        assertEquals(0, new java.math.BigDecimal("100.0").compareTo(s.getLastPrice()));
        assertEquals(0, new java.math.BigDecimal("101.0").compareTo(s.getHighPrice()));
        assertEquals(0, new java.math.BigDecimal("99.5").compareTo(s.getLowPrice()));
        assertNotNull(s.getPriceChangePercent24h());
    }

    @Test
    void parseBinance24hrJson_succeedsWithoutHighLow() throws Exception {
        String json = "{\"lastPrice\":\"50\",\"priceChangePercent\":\"0\"}";
        Optional<MarketQuoteSnapshot> out = BinanceMarketQuoteClient.parseBinance24hrJson(
                objectMapper.readTree(json), "ETHUSDT");
        assertTrue(out.isPresent());
        MarketQuoteSnapshot s = out.get();
        assertNull(s.getHighPrice());
        assertNull(s.getLowPrice());
    }

    @Test
    void parseBinance24hrJson_empty_whenMissingLastPrice() throws Exception {
        String json = "{\"priceChangePercent\":\"0\",\"highPrice\":\"1\",\"lowPrice\":\"1\"}";
        Optional<MarketQuoteSnapshot> out = BinanceMarketQuoteClient.parseBinance24hrJson(
                objectMapper.readTree(json), "BTCUSDT");
        assertTrue(out.isEmpty());
    }
}
