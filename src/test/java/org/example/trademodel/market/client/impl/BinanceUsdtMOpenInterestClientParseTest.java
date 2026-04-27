package org.example.trademodel.market.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class BinanceUsdtMOpenInterestClientParseTest {

    @Test
    void parseOpenInterestNode_readsContractField() throws Exception {
        String json = "{\"openInterest\":\"75797.837\",\"symbol\":\"BTCUSDT\"}";
        var root = new ObjectMapper().readTree(json);
        Optional<BigDecimal> oi = BinanceUsdtMOpenInterestClient.parseOpenInterestNode(root, "BTCUSDT");
        assertTrue(oi.isPresent());
        assertEquals(new BigDecimal("75797.837"), oi.get());
    }

    @Test
    void parseOpenInterestNode_emptyWhenMissing() throws Exception {
        String json = "{\"symbol\":\"BTCUSDT\"}";
        var root = new ObjectMapper().readTree(json);
        assertTrue(BinanceUsdtMOpenInterestClient.parseOpenInterestNode(root, "BTCUSDT").isEmpty());
    }
}
