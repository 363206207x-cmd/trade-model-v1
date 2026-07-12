package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutedPublicOhlcvProviderTest {
    @Test
    void unsupportedKrakenPairAndGeoRestrictedFallbackStayAssetScoped() {
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

    private static PublicOhlcvProviderResult failed(String reason) {
        return new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, reason, null);
    }
}
