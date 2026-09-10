package org.example.trademodel.assetcard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.mapper.AssetCardMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssetCardMarketDataServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");
    private final AssetCardProperties properties = new AssetCardProperties();
    private final AssetCardMapper mapper = mock(AssetCardMapper.class);
    private final AssetCardMarketDataService service = new AssetCardMarketDataService(properties, new ObjectMapper(), mapper);

    @org.junit.jupiter.api.AfterEach
    void releaseResources() { service.close(); }

    @Test
    void defaultsDisableNetworkAndUseShadowWithoutFuturesFallback() {
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getModelMode().name()).isEqualTo("SHADOW");
        service.reconcileSubscriptions(List.of("BTCUSDT", "ETHUSDT", "BTCUSDT"));
        service.ensureConnected();
        assertThat(service.subscribedSymbols()).containsExactlyInAnyOrder("BTCUSDT", "ETHUSDT");
        assertThat(service.quote("BTCUSDT", NOW)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void onlyRealSpotTradeUpdatesPriceAndOldOrCrossSymbolFramesAreRejected() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        List<AssetCardMarketDataService.MarketUpdate> events = new ArrayList<>();
        service.addListener(events::add);
        service.acceptMessage(trade("btcusdt@trade", "BTCUSDT", 42, "100.25", NOW), NOW);
        service.acceptMessage(trade("btcusdt@trade", "BTCUSDT", 41, "90", NOW), NOW);
        service.acceptMessage(trade("btcusdt@trade", "ETHUSDT", 50, "99", NOW), NOW);
        service.acceptMessage("{\"stream\":\"btcusdt@markPrice\",\"data\":{\"e\":\"markPriceUpdate\",\"s\":\"BTCUSDT\",\"p\":\"888\"}}", NOW);
        var quote = service.quote("BTCUSDT", NOW).orElseThrow();
        assertThat(quote.price()).isEqualByComparingTo("100.25");
        assertThat(quote.observedAt()).isEqualTo(NOW);
        assertThat(quote.availableAt()).isEqualTo(NOW);
        assertThat(events).extracting(AssetCardMarketDataService.MarketUpdate::type).containsExactly("PRICE");
        verifyNoInteractions(mapper);
    }

    @Test
    void staleAndNotYetAvailableQuotesAreNeverCurrentPrices() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(trade("btcusdt@trade", "BTCUSDT", 1, "100", NOW), NOW.plusSeconds(1));
        assertThat(service.quote("BTCUSDT", NOW)).isEmpty();
        assertThat(service.quote("BTCUSDT", NOW.plusSeconds(2))).isPresent();
        assertThat(service.quote("BTCUSDT", NOW.plus(properties.getPriceTtl()).plusSeconds(1))).isEmpty();
    }

    @Test
    void onlyClosedBarsPersistOnceAndReadHasNoRegistrationOrWrites() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        List<AssetCardMarketDataService.MarketUpdate> events = new ArrayList<>();
        service.addListener(events::add);
        when(mapper.upsertClosedBar(any())).thenReturn(1, 0);
        service.acceptMessage(kline(false), NOW);
        verifyNoInteractions(mapper);
        service.acceptMessage(kline(true), NOW);
        service.acceptMessage(kline(true), NOW);
        verify(mapper, times(1)).upsertClosedBar(any());
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo("BAR");
            assertThat(event.interval()).isEqualTo("5m");
        });
        var before = service.subscribedSymbols();
        service.bars("BTCUSDT", "5m", NOW, 100);
        service.quote("ETHUSDT", NOW);
        assertThat(service.subscribedSymbols()).isEqualTo(before);
        verify(mapper, times(1)).upsertClosedBar(any());
    }

    @Test
    void partialDepthCannotMasqueradeAsSynchronizedFullDepthOrSetPrice() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage("{\"stream\":\"btcusdt@depth20@100ms\",\"data\":{\"lastUpdateId\":12,\"bids\":[[\"99\",\"2\"]],\"asks\":[[\"101\",\"3\"]]}}", NOW);
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        assertThat(service.quote("BTCUSDT", NOW)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void snapshotAndBufferedDiffMustJoinBeforePublishingWithRealAvailabilityAndCoverage() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[[\"99.99\",\"4\"]]", "[]"), NOW);
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), NOW.plusMillis(200));
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        var book = service.book("BTCUSDT", NOW.plusMillis(200)).orElseThrow();
        assertThat(book.sequence()).isEqualTo(12);
        assertThat(book.observedAt()).isEqualTo(NOW);
        assertThat(book.availableAt()).isEqualTo(NOW.plusMillis(200));
        assertThat(book.timestampBasis()).isEqualTo("EXCHANGE_EVENT");
        assertThat(book.source()).isEqualTo("BINANCE_SPOT_DIFF_DEPTH");
        assertThat(book.bids().get(0).quantity()).isEqualByComparingTo("4");
        assertThat(book.coversBasisPoints(25)).isTrue();
        assertThat(book.coversBasisPoints(500)).isFalse();
        assertThat(service.quote("BTCUSDT", NOW.plusMillis(200))).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void diffGapInvalidatesBookAndRequiresNewSnapshotWhileDuplicateNeverRewinds() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), NOW);
        service.acceptMessage(depth(10, 11, NOW, "[[\"99.99\",\"999\"]]", "[]"), NOW);
        assertThat(service.book("BTCUSDT", NOW).orElseThrow().sequence()).isEqualTo(12);
        service.acceptMessage(depth(15, 16, NOW.plusSeconds(1), "[]", "[]"), NOW.plusSeconds(1));
        assertThat(service.book("BTCUSDT", NOW.plusSeconds(1))).isEmpty();
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), NOW.plusSeconds(1));
        assertThat(service.book("BTCUSDT", NOW.plusSeconds(1))).isEmpty();
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(15), NOW.plusSeconds(2));
        assertThat(service.book("BTCUSDT", NOW.plusSeconds(2)).orElseThrow().sequence()).isEqualTo(16);
    }

    @Test
    void pointInTimeBookNeverUsesLaterDepthAndSnapshotCopiesAreBounded() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), NOW);
        service.acceptMessage(depth(13, 13, NOW.plusSeconds(1), "[[\"99.99\",\"7\"]]", "[]"), NOW.plusSeconds(1));
        assertThat(service.book("BTCUSDT", NOW.plusMillis(999)).orElseThrow().sequence()).isEqualTo(12);
        assertThat(service.book("BTCUSDT", NOW.plusSeconds(1)).orElseThrow().sequence()).isEqualTo(13);
        for (int i = 2; i < 80; i++)
            service.acceptMessage(depth(12 + i, 12 + i, NOW.plusMillis(i * 100L + 1000), "[]", "[]"), NOW.plusMillis(i * 100L + 1000));
        assertThat(service.retainedBookSnapshotCount("BTCUSDT")).isLessThanOrEqualTo(16);
        assertThat(service.retainedBookSnapshotCount("BTCUSDT")).isLessThan(12);
        assertThat(service.book("BTCUSDT", NOW.plusSeconds(80))).isEmpty();
    }

    @Test
    void zeroQuantityRemovesLevelAndPeripheralUpdatesDoNotInventCoverage() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), NOW);
        service.acceptMessage(depth(13, 13, NOW.plusSeconds(1), "[[\"99.99\",\"0\"],[\"50\",\"100\"]]", "[[\"150\",\"100\"]]"), NOW.plusSeconds(1));
        var book = service.book("BTCUSDT", NOW.plusSeconds(1)).orElseThrow();
        assertThat(book.bids()).noneMatch(level -> level.price().compareTo(new BigDecimal("99.99")) == 0);
        assertThat(book.bidCoverageFloor()).isEqualByComparingTo("99");
        assertThat(book.askCoverageCeiling()).isEqualByComparingTo("101");
        assertThat(book.coversBasisPoints(500)).isFalse();
    }

    @Test
    void bootstrapBudgetIsGlobalAndRetriesHonorExchangeCooldownWithoutNetwork() {
        assertThat(service.claimDepthBootstrapBudget("BTCUSDT", NOW)).isTrue();
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW.plusSeconds(59))).isFalse();
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW.plusSeconds(60))).isTrue();
        service.recordDepthBootstrapFailure("ETHUSDT", 429, "120", NOW.plusSeconds(61));
        assertThat(service.claimDepthBootstrapBudget("BTCUSDT", NOW.plusSeconds(180))).isFalse();
        assertThat(service.claimDepthBootstrapBudget("BTCUSDT", NOW.plusSeconds(181))).isTrue();
        service.recordDepthBootstrapFailure("BTCUSDT", 418, "Thu, 10 Sep 2026 12:10:00 GMT", NOW.plusSeconds(182));
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW.plusSeconds(599))).isFalse();
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW.plusSeconds(600))).isTrue();
        service.ensureConnected(); // enabled=false still prevents any real request.
        verifyNoInteractions(mapper);
    }

    @Test
    void oldHttpRateLimitStillCoolsGlobalBudgetButOldSuccessCannotPopulateNewSocketEpoch() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        Object oldState = ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "depthStates")).get("BTCUSDT");
        java.net.http.WebSocket oldSocket = mock(java.net.http.WebSocket.class);
        java.net.http.WebSocket newSocket = mock(java.net.http.WebSocket.class);
        properties.setEnabled(true); // No lifecycle/network method is called; both sockets are inert mocks.
        org.springframework.test.util.ReflectionTestUtils.setField(service, "socket", newSocket);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 2L);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "completeDepthBootstrap", "BTCUSDT", oldSocket,
                1L, oldState, depthResponse(200, depthSnapshot(11), null), null, NOW);
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "completeDepthBootstrap", "BTCUSDT", oldSocket,
                1L, oldState, depthResponse(429, "", "120"), null, NOW);
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW.plusSeconds(119))).isFalse();
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW.plusSeconds(120))).isTrue();
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        verifyNoInteractions(oldSocket, newSocket, mapper);
    }

    @Test
    void oldSnapshotCannotPopulateReplacementDepthStateEvenOnTheSameConnection() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        Object oldState = ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "depthStates")).get("BTCUSDT");
        service.reconcileSubscriptions(List.of());
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(20, 22, NOW, "[]", "[]"), NOW);
        java.net.http.WebSocket currentSocket = mock(java.net.http.WebSocket.class);
        properties.setEnabled(true);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "socket", currentSocket);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 2L);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "completeDepthBootstrap", "BTCUSDT", currentSocket,
                2L, oldState, depthResponse(200, depthSnapshot(21), null), null, NOW);
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        Object currentState = ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "depthStates")).get("BTCUSDT");
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "completeDepthBootstrap", "BTCUSDT", currentSocket,
                2L, currentState, depthResponse(200, depthSnapshot(21), null), null, NOW);
        assertThat(service.book("BTCUSDT", NOW).orElseThrow().sequence()).isEqualTo(22);
        verifyNoInteractions(currentSocket, mapper);
    }

    @SuppressWarnings("unchecked")
    private java.net.http.HttpResponse<String> depthResponse(int status, String body, String retryAfter) {
        java.net.http.HttpResponse<String> response = mock(java.net.http.HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(retryAfter == null ? java.util.Map.of()
                : java.util.Map.of("Retry-After", List.of(retryAfter)), (name, value) -> true));
        return response;
    }

    @Test
    void excessiveBufferedDepthIsBoundedAndRemovalClearsHistory() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        for (int i = 1; i <= 600; i++) service.acceptMessage(depth(i, i, NOW, "[]", "[]"), NOW);
        assertThat(service.bufferedDepthEventCount("BTCUSDT")).isLessThanOrEqualTo(256);
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        service.reconcileSubscriptions(List.of());
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(599), NOW);
        assertThat(service.bufferedDepthEventCount("BTCUSDT")).isZero();
        assertThat(service.retainedBookSnapshotCount("BTCUSDT")).isZero();
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
    }

    @Test
    void snapshotConfigurationOnlyAllowsPublicSpotAndBundleChecksumIsExplicit() {
        assertThat(properties.getModelBundleSha256()).isEmpty();
        assertThat(properties.getSpotDepthSnapshotUri().toString()).isEqualTo("https://api.binance.com/api/v3/depth");
        for (String endpoint : List.of("https://fapi.binance.com/fapi/v1/depth", "http://api.binance.com/api/v3/depth",
                "https://other.invalid/api/v3/depth", "https://api.binance.com/api/v3/order", "https://api.binance.com:444/api/v3/depth"))
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> properties.setSpotDepthSnapshotUri(java.net.URI.create(endpoint)))
                    .isInstanceOf(IllegalArgumentException.class);
        properties.setModelBundleSha256("a".repeat(64));
        assertThat(properties.getModelBundleSha256()).isEqualTo("a".repeat(64));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> properties.setModelBundleSha256("short"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String depth(long first, long last, Instant time, String bids, String asks) {
        return "{\"stream\":\"btcusdt@depth@100ms\",\"data\":{\"e\":\"depthUpdate\",\"s\":\"BTCUSDT\",\"E\":"
                + time.toEpochMilli() + ",\"U\":" + first + ",\"u\":" + last + ",\"b\":" + bids + ",\"a\":" + asks + "}}";
    }

    private String depthSnapshot(long version) {
        return "{\"lastUpdateId\":" + version + ",\"bids\":[[\"99.99\",\"2\"],[\"99\",\"3\"]],\"asks\":[[\"100.01\",\"2\"],[\"101\",\"3\"]]}";
    }

    @Test
    void removedSymbolsCannotReappearFromOldStreamFrames() {
        service.reconcileSubscriptions(List.of("BTCUSDT", "ETHUSDT"));
        service.acceptMessage(trade("btcusdt@trade", "BTCUSDT", 1, "100", NOW), NOW);
        service.reconcileSubscriptions(List.of("ETHUSDT"));
        service.acceptMessage(trade("btcusdt@trade", "BTCUSDT", 2, "101", NOW), NOW);
        assertThat(service.quote("BTCUSDT", NOW)).isEmpty();
        assertThat(service.subscribedSymbols()).containsExactly("ETHUSDT");
    }

    @Test
    void existingBarsAreReadWithoutChangingTheirAvailabilityOrOverridingOwnSnapshot() {
        var existing = new AssetCardMarketDataService.SpotBar("BTCUSDT", "5m", NOW.minusSeconds(600), NOW.minusSeconds(300).minusMillis(1),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, null, null, NOW.minusSeconds(290));
        var own = new AssetCardMarketDataService.SpotBar("BTCUSDT", "5m", NOW.minusSeconds(300), NOW.minusMillis(1),
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, null, null, NOW);
        when(mapper.selectClosedBars("BTCUSDT", "5m", NOW, 100)).thenReturn(List.of(own));
        when(mapper.selectExistingSpotBars("BTCUSDT", "5m", NOW, 100)).thenReturn(List.of(existing));
        assertThat(service.bars("BTCUSDT", "5m", NOW, 100)).containsExactly(existing, own);
        assertThat(service.subscribedSymbols()).isEmpty();
        verify(mapper, never()).upsertClosedBar(any());
        verify(mapper, never()).nextSnapshotVersion(any());
    }

    @Test
    void sourceConfigurationRejectsFuturesPrivateOrArbitraryEndpoints() {
        for (String endpoint : List.of("wss://fstream.binance.com/stream", "https://stream.binance.com/stream",
                "wss://attacker.invalid/stream", "wss://user:secret@stream.binance.com/stream")) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> properties.setSpotStreamBaseUri(java.net.URI.create(endpoint)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void fullOldOwnWindowCannotHideNewerPersistedSpotClose() {
        var old = new AssetCardMarketDataService.SpotBar("BTCUSDT", "5m", NOW.minusSeconds(600), NOW.minusSeconds(300).minusMillis(1),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, null, null, NOW.minusSeconds(290));
        var latest = new AssetCardMarketDataService.SpotBar("BTCUSDT", "5m", NOW.minusSeconds(300), NOW.minusMillis(1),
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, null, null, NOW);
        when(mapper.selectClosedBars("BTCUSDT", "5m", NOW, 1)).thenReturn(List.of(old));
        when(mapper.selectExistingSpotBars("BTCUSDT", "5m", NOW, 1)).thenReturn(List.of(latest));
        assertThat(service.bars("BTCUSDT", "5m", NOW, 1)).containsExactly(latest);
        verify(mapper, never()).upsertClosedBar(any());
    }

    private String kline(boolean closed) {
        long start = NOW.minus(Duration.ofMinutes(5)).toEpochMilli();
        long end = NOW.toEpochMilli() - 1;
        return "{\"stream\":\"btcusdt@kline_5m\",\"data\":{\"e\":\"kline\",\"s\":\"BTCUSDT\",\"E\":" + NOW.toEpochMilli()
                + ",\"k\":{\"i\":\"5m\",\"t\":" + start + ",\"T\":" + end + ",\"x\":" + closed
                + ",\"o\":\"100\",\"h\":\"102\",\"l\":\"99\",\"c\":\"101\",\"v\":\"10\",\"V\":\"6\",\"n\":10}}}";
    }

    private String trade(String stream, String symbol, long id, String price, Instant time) {
        return "{\"stream\":\"" + stream + "\",\"data\":{\"e\":\"trade\",\"s\":\"" + symbol
                + "\",\"t\":" + id + ",\"p\":\"" + price + "\",\"q\":\"1\",\"T\":" + time.toEpochMilli() + "}}";
    }
}
