package org.example.trademodel.market.client.impl;

import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FallbackMarketQuoteClientTest {

    @Test
    void fetch24hTicker_usesBinanceWhenPrimaryAvailable() {
        BinanceMarketQuoteClient binance = mock(BinanceMarketQuoteClient.class);
        OkxMarketQuoteClient okx = mock(OkxMarketQuoteClient.class);
        MarketQuoteSnapshot snap = quote("binance");
        when(binance.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(snap));

        Optional<MarketQuoteSnapshot> out = new FallbackMarketQuoteClient(binance, okx).fetch24hTicker("BTCUSDT");

        assertTrue(out.isPresent());
        assertEquals("binance", out.get().getProvider());
        verify(okx, never()).fetch24hTicker("BTCUSDT");
    }

    @Test
    void fetch24hTicker_usesOkxWhenBinanceEmpty() {
        BinanceMarketQuoteClient binance = mock(BinanceMarketQuoteClient.class);
        OkxMarketQuoteClient okx = mock(OkxMarketQuoteClient.class);
        when(binance.fetch24hTicker("BTCUSDT")).thenReturn(Optional.empty());
        when(okx.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(quote(OkxMarketQuoteClient.PROVIDER)));

        Optional<MarketQuoteSnapshot> out = new FallbackMarketQuoteClient(binance, okx).fetch24hTicker("BTCUSDT");

        assertTrue(out.isPresent());
        assertEquals(OkxMarketQuoteClient.PROVIDER, out.get().getProvider());
        assertEquals(0, new BigDecimal("1.2").compareTo(out.get().getPriceChangePercent24h()));
    }

    private static MarketQuoteSnapshot quote(String provider) {
        MarketQuoteSnapshot snap = new MarketQuoteSnapshot();
        snap.setProvider(provider);
        snap.setSymbolNormalized("BTCUSDT");
        snap.setLastPrice(new BigDecimal("100"));
        snap.setPriceChangePercent24h(new BigDecimal("1.2"));
        return snap;
    }
}
