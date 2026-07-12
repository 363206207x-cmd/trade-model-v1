package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class RoutedPublicOhlcvProviderTest {
    @Test
    void genuinePairNotSupportedMayFallbackAndStaysAssetScoped() {
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        when(kraken.fetchClosedBars("BNBUSDT", "5m", 100, "run-1"))
                .thenReturn(failed("PAIR_NOT_SUPPORTED"));
        when(binance.fetchClosedBars("BNBUSDT", "5m", 100, "run-1"))
                .thenReturn(failed("GEO_RESTRICTED"));
        RoutedPublicOhlcvProvider routed = new RoutedPublicOhlcvProvider(
                kraken, binance, "kraken", "binance", true);

        PublicOhlcvProviderResult result = routed.fetchClosedBars("BNBUSDT", "5m", 100, "run-1");

        assertThat(result.reasonCode()).isEqualTo("PAIR_NOT_SUPPORTED_OR_GEO_RESTRICTED");
        verify(kraken).fetchClosedBars("BNBUSDT", "5m", 100, "run-1");
        verify(binance).fetchClosedBars("BNBUSDT", "5m", 100, "run-1");
        assertThat(routed.primaryProvider()).isEqualTo("KRAKEN");
    }

    @Test
    void mappingErrorDoesNotFallbackToBinance() {
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        when(kraken.fetchClosedBars("BTCUSDT", "5m", 100, "run-1"))
                .thenReturn(failed("KRAKEN_PAIR_RESOLUTION_ERROR"));
        RoutedPublicOhlcvProvider routed = new RoutedPublicOhlcvProvider(
                kraken, binance, "kraken", "binance", true);

        PublicOhlcvProviderResult result = routed.fetchClosedBars("BTCUSDT", "5m", 100, "run-1");

        assertThat(result.reasonCode()).isEqualTo("KRAKEN_PAIR_RESOLUTION_ERROR");
        verify(binance, never()).fetchClosedBars("BTCUSDT", "5m", 100, "run-1");
    }

    @Test
    void invalidResponseAndOhlcUnknownPairDoNotFallback() {
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        RoutedPublicOhlcvProvider routed = new RoutedPublicOhlcvProvider(
                kraken, binance, "kraken", "binance", true);
        when(kraken.fetchClosedBars("BTCUSDT", "5m", 100, "run-1"))
                .thenReturn(failed("INVALID_RESPONSE"));
        when(kraken.fetchClosedBars("ETHUSDT", "5m", 100, "run-2"))
                .thenReturn(failed("KRAKEN_OHLC_UNKNOWN_PAIR"));

        assertThat(routed.fetchClosedBars("BTCUSDT", "5m", 100, "run-1").reasonCode())
                .isEqualTo("INVALID_RESPONSE");
        assertThat(routed.fetchClosedBars("ETHUSDT", "5m", 100, "run-2").reasonCode())
                .isEqualTo("KRAKEN_OHLC_UNKNOWN_PAIR");
        verify(binance, never()).fetchClosedBars("BTCUSDT", "5m", 100, "run-1");
        verify(binance, never()).fetchClosedBars("ETHUSDT", "5m", 100, "run-2");
    }

    @Test
    void networkFailureMayFallback() {
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        when(kraken.fetchClosedBars("BTCUSDT", "5m", 100, "run-1")).thenReturn(failed("TIMEOUT"));
        when(binance.fetchClosedBars("BTCUSDT", "5m", 100, "run-1")).thenReturn(failed("GEO_RESTRICTED"));
        RoutedPublicOhlcvProvider routed = new RoutedPublicOhlcvProvider(
                kraken, binance, "kraken", "binance", true);

        assertThat(routed.fetchClosedBars("BTCUSDT", "5m", 100, "run-1").reasonCode())
                .isEqualTo("GEO_RESTRICTED");
        verify(binance).fetchClosedBars("BTCUSDT", "5m", 100, "run-1");
    }

    private static PublicOhlcvProviderResult failed(String reason) {
        return new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, reason, null);
    }
}
