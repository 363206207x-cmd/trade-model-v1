package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicKlineFetchResult;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinancePublicOhlcvProviderTest {
    private static final long SETTLEMENT_DELAY_MS = 30_000L;
    private static final Instant FETCHED_AT = Instant.parse("2026-07-10T10:05:01Z");

    @Mock
    private RealMarketDataFetcherService fetcher;

    @ParameterizedTest
    @ValueSource(ints = {100, 101, 102})
    void returnsLatestTargetWindowWhenEnoughSettledBarsExist(int settledCount) {
        List<String[]> rows = settledRows(FETCHED_AT, settledCount);
        when(fetcher.fetchKlinesDetailed("BTCUSDT", "5m", 102))
                .thenReturn(ready(rows));

        PublicOhlcvProviderResult result = provider().fetchClosedBars(
                "BTCUSDT", "5m", 100, "run-window");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch()).isNotNull();
        assertThat(result.batch().provider()).isEqualTo("BINANCE_PUBLIC");
        assertThat(result.batch().providerMarketType()).isEqualTo("SPOT");
        assertThat(result.batch().sourceEndpoint()).isEqualTo("/api/v3/klines");
        assertThat(result.batch().bars()).hasSize(100);
        assertThat(result.batch().bars()).allSatisfy(bar -> {
            assertThat(bar.symbol()).isEqualTo("BTCUSDT");
            assertThat(bar.timeframe()).isEqualTo("5m");
            assertThat(bar.open()).isEqualByComparingTo("100.00");
            assertThat(bar.high()).isEqualByComparingTo("102.00");
            assertThat(bar.low()).isEqualByComparingTo("99.00");
            assertThat(bar.close()).isEqualByComparingTo("101.00");
            assertThat(bar.closed()).isTrue();
        });
        assertThat(result.batch().bars().get(0).closeTimeMs())
                .isEqualTo(Long.parseLong(rows.get(settledCount - 100)[6]));
        assertThat(result.batch().bars().get(99).closeTimeMs())
                .isEqualTo(Long.parseLong(rows.get(settledCount - 1)[6]));
        verify(fetcher).fetchKlinesDetailed("BTCUSDT", "5m", 102);
    }

    @Test
    void unsettledRowsDoNotPreventACompleteSettledTargetWindow() {
        List<String[]> rows = new ArrayList<>(settledRows(FETCHED_AT, 100));
        rows.add(row(FETCHED_AT.toEpochMilli() - 66L, "999.00"));
        rows.add(row(FETCHED_AT.toEpochMilli() + 1L, "999.00"));
        when(fetcher.fetchKlinesDetailed("BTCUSDT", "5m", 102))
                .thenReturn(ready(rows));

        PublicOhlcvProviderResult result = provider().fetchClosedBars(
                "BTCUSDT", "5m", 100, "run-mixed-window");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch()).isNotNull();
        assertThat(result.batch().bars()).hasSize(100)
                .allMatch(bar -> FETCHED_AT.toEpochMilli() - bar.closeTimeMs() >= SETTLEMENT_DELAY_MS);
    }

    @ParameterizedTest
    @ValueSource(longs = {66L, 313L, 328L, 29_999L, 0L, -1L})
    void filtersOnlyTheUnsettledOrFutureRowWithoutDegradingTheBatch(long unsettledAgeMs) {
        long fetchTimeMs = FETCHED_AT.toEpochMilli();
        String[] unsettled = row(fetchTimeMs - unsettledAgeMs, "999.00");
        String[] settled = row(fetchTimeMs - SETTLEMENT_DELAY_MS, "101.00");
        when(fetcher.fetchKlinesDetailed("BTCUSDT", "5m", 3))
                .thenReturn(ready(List.of(unsettled, settled)));

        PublicOhlcvProviderResult result = provider().fetchClosedBars(
                "BTCUSDT", "5m", 1, "run-boundary");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch()).isNotNull();
        assertThat(result.batch().bars()).singleElement().satisfies(bar -> {
            assertThat(bar.closeTimeMs()).isEqualTo(fetchTimeMs - SETTLEMENT_DELAY_MS);
            assertThat(bar.close()).isEqualByComparingTo("101.00");
        });
    }

    @Test
    void acceptsThirtySecondBoundaryAndOlderBars() {
        long fetchTimeMs = FETCHED_AT.toEpochMilli();
        when(fetcher.fetchKlinesDetailed("BTCUSDT", "5m", 4))
                .thenReturn(ready(List.of(
                        row(fetchTimeMs - 30_000L, "101.00"),
                        row(fetchTimeMs - 30_001L, "100.00"))));

        PublicOhlcvProviderResult result = provider().fetchClosedBars(
                "BTCUSDT", "5m", 2, "run-exact-boundary");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        assertThat(result.batch().bars()).extracting(bar -> fetchTimeMs - bar.closeTimeMs())
                .containsExactly(30_001L, 30_000L);
    }

    @Test
    void insufficientSettledBarsWaitWithoutPartialBatch() {
        when(fetcher.fetchKlinesDetailed("BTCUSDT", "5m", 102))
                .thenReturn(ready(settledRows(FETCHED_AT, 99)));

        PublicOhlcvProviderResult result = provider().fetchClosedBars(
                "BTCUSDT", "5m", 100, "run-insufficient");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.WAITING_SYNC);
        assertThat(result.reasonCode()).isEqualTo("PUBLIC_OHLCV_INSUFFICIENT_SETTLED_BARS");
        assertThat(result.batch()).isNull();
    }

    @Test
    void rawRequestLimitIsCappedAtProviderMaximum() {
        when(fetcher.fetchKlinesDetailed("BTCUSDT", "5m", 500))
                .thenReturn(new PublicKlineFetchResult(
                        OhlcvSourceState.ERROR, "PROVIDER_UNAVAILABLE", FETCHED_AT, List.of()));

        PublicOhlcvProviderResult result = provider().fetchClosedBars(
                "BTCUSDT", "5m", 500, "run-max");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.ERROR);
        verify(fetcher).fetchKlinesDetailed("BTCUSDT", "5m", 500);
    }

    @Test
    void disabledPublicProviderMakesNoExternalCall() {
        BinancePublicOhlcvProvider provider = new BinancePublicOhlcvProvider(
                fetcher, false, false, SETTLEMENT_DELAY_MS);

        PublicOhlcvProviderResult result = provider.fetchClosedBars(
                "BTCUSDT", "5m", 100, "run-disabled");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.DISABLED);
        verifyNoInteractions(fetcher);
    }

    @Test
    void http451OpensProcessCircuitAndPreventsRepeatedCalls() {
        when(fetcher.fetchKlinesDetailed("BNBUSDT", "5m", 102))
                .thenReturn(new PublicKlineFetchResult(OhlcvSourceState.ERROR, "GEO_RESTRICTED",
                        FETCHED_AT, List.of(), 451));
        BinancePublicOhlcvProvider provider = provider();

        PublicOhlcvProviderResult first = provider.fetchClosedBars(
                "BNBUSDT", "5m", 100, "run-1");
        PublicOhlcvProviderResult second = provider.fetchClosedBars(
                "BTCUSDT", "5m", 100, "run-2");

        assertThat(first.reasonCode()).isEqualTo("REGION_RESTRICTED");
        assertThat(second.reasonCode()).isEqualTo("REGION_RESTRICTED");
        assertThat(provider.isGeoRestrictedCircuitOpen()).isTrue();
        verify(fetcher, times(1)).fetchKlinesDetailed("BNBUSDT", "5m", 102);
        verify(fetcher, never()).fetchKlinesDetailed("BTCUSDT", "5m", 102);
    }

    @Test
    void noPrivateAccountOrOrderEndpointAndNoApiKeyRequiredForPublicIngestion() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/market/client/impl/BinancePublicOhlcvProvider.java"));

        assertThat(source).contains("/api/v3/klines");
        assertThat(source).doesNotContain("/api/v3/account", "/api/v3/order", "api-key", "apiSecret");
        assertThat(BinancePublicOhlcvProvider.class.getConstructors()).singleElement().satisfies(constructor ->
                assertThat(constructor.getParameterTypes()).containsExactly(
                        RealMarketDataFetcherService.class, boolean.class, boolean.class, long.class));
    }

    private BinancePublicOhlcvProvider provider() {
        return new BinancePublicOhlcvProvider(fetcher, true, true, SETTLEMENT_DELAY_MS);
    }

    private static PublicKlineFetchResult ready(List<String[]> rows) {
        return new PublicKlineFetchResult(OhlcvSourceState.READY, null, FETCHED_AT, rows);
    }

    private static List<String[]> settledRows(Instant fetchedAt, int count) {
        long latestCloseTimeMs = fetchedAt.toEpochMilli() - SETTLEMENT_DELAY_MS;
        List<String[]> rows = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            long closeTimeMs = latestCloseTimeMs - ((long) (count - index - 1) * 300_000L);
            rows.add(row(closeTimeMs, "101.00"));
        }
        return rows;
    }

    private static String[] row(long closeTimeMs, String closePrice) {
        return new String[]{
                Long.toString(closeTimeMs - 299_999L), "100.00", "102.00", "99.00", closePrice,
                "1000.00", Long.toString(closeTimeMs), "100000.00", "100", "500.00", "50000.00", "0"
        };
    }
}
