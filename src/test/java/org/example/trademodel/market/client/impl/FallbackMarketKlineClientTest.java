package org.example.trademodel.market.client.impl;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FallbackMarketKlineClientTest {

    @Test
    void fetchKlines_usesBinanceWhenPrimaryAvailable() {
        BinanceMarketKlineClient binance = mock(BinanceMarketKlineClient.class);
        OkxMarketKlineClient okx = mock(OkxMarketKlineClient.class);
        List<String[]> primary = Collections.singletonList(row("100", "101"));
        when(binance.fetchKlines("BTCUSDT", "1m", 3)).thenReturn(primary);

        List<String[]> out = new FallbackMarketKlineClient(binance, okx).fetchKlines("BTCUSDT", "1m", 3);

        assertEquals(primary, out);
        verify(okx, never()).fetchKlines("BTCUSDT", "1m", 3);
    }

    @Test
    void fetchKlines_usesOkxWhenBinanceEmpty() {
        BinanceMarketKlineClient binance = mock(BinanceMarketKlineClient.class);
        OkxMarketKlineClient okx = mock(OkxMarketKlineClient.class);
        List<String[]> fallback = Collections.singletonList(row("100", "102"));
        when(binance.fetchKlines("BTCUSDT", "5m", 3)).thenReturn(List.of());
        when(okx.fetchKlines("BTCUSDT", "5m", 3)).thenReturn(fallback);

        List<String[]> out = new FallbackMarketKlineClient(binance, okx).fetchKlines("BTCUSDT", "5m", 3);

        assertEquals(fallback, out);
    }

    private static String[] row(String open, String close) {
        return new String[]{"0", open, close, open, close, "1", "59999"};
    }
}
