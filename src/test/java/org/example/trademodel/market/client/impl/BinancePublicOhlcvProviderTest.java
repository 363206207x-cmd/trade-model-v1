package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicKlineFetchResult;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinancePublicOhlcvProviderTest {
    @Mock
    private RealMarketDataFetcherService fetcher;

    @Test
    void publicProviderResultMapsToValidatedOhlcvBar() {
        Instant fetchedAt = Instant.parse("2026-07-10T10:05:01Z");
        String[] row = {
                "1783677600000", "100.00", "102.00", "99.00", "101.00", "1000.00",
                "1783677899999", "100000.00", "100", "500.00", "50000.00", "0"
        };
        when(fetcher.fetchKlinesDetailed("BTCUSDT", "5m", 100))
                .thenReturn(new PublicKlineFetchResult(OhlcvSourceState.READY, null, fetchedAt,
                        Collections.singletonList(row)));
        BinancePublicOhlcvProvider provider = new BinancePublicOhlcvProvider(fetcher, true, true);

        PublicOhlcvProviderResult result = provider.fetchClosedBars("BTCUSDT", "5m", 100, "run-1");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch()).isNotNull();
        assertThat(result.batch().provider()).isEqualTo("BINANCE_PUBLIC");
        assertThat(result.batch().sourceEndpoint()).isEqualTo("/api/v3/klines");
        assertThat(result.batch().bars()).singleElement().satisfies(bar -> {
            assertThat(bar.symbol()).isEqualTo("BTCUSDT");
            assertThat(bar.timeframe()).isEqualTo("5m");
            assertThat(bar.open()).isEqualByComparingTo("100.00");
            assertThat(bar.high()).isEqualByComparingTo("102.00");
            assertThat(bar.low()).isEqualByComparingTo("99.00");
            assertThat(bar.close()).isEqualByComparingTo("101.00");
            assertThat(bar.closed()).isTrue();
        });
    }

    @Test
    void disabledPublicProviderMakesNoExternalCall() {
        BinancePublicOhlcvProvider provider = new BinancePublicOhlcvProvider(fetcher, false, false);

        PublicOhlcvProviderResult result = provider.fetchClosedBars("BTCUSDT", "5m", 100, "run-1");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.DISABLED);
        verify(fetcher, never()).fetchKlinesDetailed("BTCUSDT", "5m", 100);
    }

    @Test
    void noPrivateAccountOrOrderEndpointAndNoApiKeyRequiredForPublicIngestion() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/market/client/impl/BinancePublicOhlcvProvider.java"));

        assertThat(source).contains("/api/v3/klines");
        assertThat(source).doesNotContain("/api/v3/account", "/api/v3/order", "api-key", "apiSecret");
        assertThat(BinancePublicOhlcvProvider.class.getConstructors()).singleElement().satisfies(constructor ->
                assertThat(constructor.getParameterTypes()).containsExactly(
                        RealMarketDataFetcherService.class, boolean.class, boolean.class));
    }
}
