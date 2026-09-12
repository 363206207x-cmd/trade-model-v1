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

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardMarketDataServiceTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path windowDirectory;
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");
    private final AssetCardProperties properties = new AssetCardProperties();
    private final AssetCardMapper mapper = mock(AssetCardMapper.class);
    private final org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment()
            .withProperty("trade-model.provider-call.external-calls-enabled", "true");
    private final AssetCardMarketDataService service = new AssetCardMarketDataService(properties, new ObjectMapper(), mapper, environment);

    @org.junit.jupiter.api.AfterEach
    void releaseResources() { service.close(); }

    @org.junit.jupiter.api.BeforeEach
    void explicitFixtureWriterReadiness() {
        properties.setWriterEnabled(true);
        service.setWriterReadiness(() -> true); // Test-only permission evidence; no real database or grants.
    }

    @Test
    void defaultsDisableNetworkAndUseShadowWithoutFuturesFallback() {
        properties.setWriterEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isExternalCallsEnabled()).isFalse();
        assertThat(properties.isWriterEnabled()).isFalse();
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
        assertThat(service.quote("BTCUSDT", NOW.plus(properties.getPriceTtl()).minusNanos(1))).isPresent();
        assertThat(service.quote("BTCUSDT", NOW.plus(properties.getPriceTtl()))).isEmpty();
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
        properties.setDepthWeightBudgetPerMinute(500);
        assertThat(service.claimDepthBootstrapBudget("BTCUSDT", NOW)).isTrue();
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW)).isTrue();
        assertThat(service.claimDepthBootstrapBudget("SOLUSDT", NOW.plusSeconds(59))).isFalse();
        assertThat(service.claimDepthBootstrapBudget("SOLUSDT", NOW.plusSeconds(60))).isTrue();
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
        configureWindowFixture();
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        Object oldState = ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "depthStates")).get("BTCUSDT");
        java.net.http.WebSocket oldSocket = mock(java.net.http.WebSocket.class);
        java.net.http.WebSocket newSocket = mock(java.net.http.WebSocket.class);
        properties.setEnabled(true); // No lifecycle/network method is called; both sockets are inert mocks.
        properties.setExternalCallsEnabled(true);
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
        assertThat(service.collectionStatus()).isEqualTo("PROVIDER_QUOTA_OR_AUTH_FAILURE");
        verify(newSocket).abort();
        verifyNoInteractions(oldSocket);
        verify(mapper, atLeastOnce()).storageUsage(); verifyNoMoreInteractions(mapper);
    }

    @Test
    void oldSnapshotCannotPopulateReplacementDepthStateEvenOnTheSameConnection() {
        configureWindowFixture();
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        Object oldState = ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "depthStates")).get("BTCUSDT");
        service.reconcileSubscriptions(List.of());
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(depth(20, 22, NOW, "[]", "[]"), NOW);
        java.net.http.WebSocket currentSocket = mock(java.net.http.WebSocket.class);
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "socket", currentSocket);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 2L);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "completeDepthBootstrap", "BTCUSDT", currentSocket,
                2L, oldState, depthResponse(200, depthSnapshot(21), null), null, NOW);
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        Object currentState = ((java.util.Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(service, "depthStates")).get("BTCUSDT");
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "completeDepthBootstrap", "BTCUSDT", currentSocket,
                2L, currentState, depthResponse(200, depthSnapshot(21), null), null, NOW);
        assertThat(service.book("BTCUSDT", NOW).orElseThrow().sequence()).isEqualTo(22);
        verifyNoInteractions(currentSocket);
        verify(mapper, atLeastOnce()).storageUsage(); verifyNoMoreInteractions(mapper);
    }

    @SuppressWarnings("unchecked")
    private java.net.http.HttpResponse<String> depthResponse(int status, String body, String retryAfter) {
        java.net.http.HttpResponse<String> response = mock(java.net.http.HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(retryAfter == null
                ? java.util.Map.of("X-MBX-USED-WEIGHT-1M", List.of("250"))
                : java.util.Map.of("Retry-After", List.of(retryAfter), "X-MBX-USED-WEIGHT-1M", List.of("250")), (name, value) -> true));
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
        return "{\"stream\":\"" + stream.replace("@trade", "@aggTrade") + "\",\"data\":{\"e\":\"aggTrade\",\"s\":\"" + symbol
                + "\",\"a\":" + id + ",\"p\":\"" + price + "\",\"q\":\"1\",\"T\":" + time.toEpochMilli() + "}}";
    }

    @Test
    void aggregateTradeIdentityIsNotRawTradeAndEveryObservationSurvivesPriceCoalescing() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        List<AssetCardMarketDataService.SpotQuote> observations = new ArrayList<>();
        service.addTradeListener(observations::add);
        for (int i = 1; i <= 100; i++) service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", i, "100", NOW), NOW);
        service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", 100, "999", NOW), NOW);
        service.acceptMessage("{\"stream\":\"btcusdt@trade\",\"data\":{\"e\":\"trade\",\"s\":\"BTCUSDT\",\"t\":101,\"p\":\"999\",\"q\":\"1\",\"T\":" + NOW.toEpochMilli() + "}}", NOW);
        assertThat(observations).hasSize(100);
        assertThat(observations.get(99).source()).isEqualTo("BINANCE_SPOT_AGG_TRADE");
        assertThat(observations.get(99).tradeId()).isEqualTo(100);
        assertThat(service.quote("BTCUSDT", NOW).orElseThrow().price()).isEqualByComparingTo("100");
        assertThat(service.runtimeMetrics().coalescedPrices()).isEqualTo(99);
    }

    @Test
    void existingDatabaseBarStillNotifiesThisInstanceOnce() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        when(mapper.upsertClosedBar(any())).thenReturn(0);
        List<AssetCardMarketDataService.MarketUpdate> events = new ArrayList<>();
        service.addListener(events::add);
        service.acceptMessage(kline(true), NOW);
        service.acceptMessage(kline(true), NOW);
        assertThat(events).extracting(AssetCardMarketDataService.MarketUpdate::type).containsExactly("BAR");
        verify(mapper, times(1)).upsertClosedBar(any());
    }

    @Test
    void blockedBarPersistenceCannotBlockAggregateTradeOrDepthQueues() throws Exception {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var observed = new java.util.concurrent.CountDownLatch(1);
        when(mapper.upsertClosedBar(any())).thenAnswer(call -> { entered.countDown(); release.await(3, java.util.concurrent.TimeUnit.SECONDS); return 1; });
        service.addTradeListener(value -> observed.countDown());
        try {
            service.enqueueMessage(kline(true), NOW);
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            service.enqueueMessage(trade("btcusdt@aggTrade", "BTCUSDT", 1, "100", NOW), NOW);
            service.enqueueMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
            assertThat(observed.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(service.quote("BTCUSDT", NOW)).isPresent();
        } finally { release.countDown(); }
        assertThat(service.awaitQueues(Duration.ofSeconds(2))).isTrue();
        assertThat(service.bufferedDepthEventCount("BTCUSDT")).isEqualTo(1);
        assertThat(service.runtimeMetrics().barQueueDepth()).isZero();
    }

    @Test
    void depthCoverageLossRebuildsOnlyRiskAndNeverDeletesRealPrice() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", 1, "100", NOW), NOW);
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        String narrow = "{\"lastUpdateId\":11,\"bids\":[[\"99.99\",\"2\"]],\"asks\":[[\"100.01\",\"2\"]]}";
        service.acceptDepthSnapshot("BTCUSDT", narrow, NOW);
        assertThat(service.book("BTCUSDT", NOW)).isEmpty();
        assertThat(service.quote("BTCUSDT", NOW)).isPresent();
        assertThat(service.runtimeMetrics().depthRecoveries()).isEqualTo(1);
        service.acceptMessage(depth(13, 14, NOW.plusSeconds(1), "[]", "[]"), NOW.plusSeconds(1));
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(13), NOW.plusSeconds(1));
        assertThat(service.book("BTCUSDT", NOW.plusSeconds(1)).orElseThrow().coversBasisPoints(25)).isTrue();
    }

    @Test
    void incrementalMembershipAndRolloverDoNotClearUnchangedQuotesOrBooks() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", 1, "100", NOW), NOW);
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), NOW);
        service.reconcileSubscriptions(List.of("BTCUSDT", "ETHUSDT"), List.of("ETHUSDT", "NOTAMEMBER"));
        assertThat(service.depthBootstrapOrder()).containsExactly("ETHUSDT", "BTCUSDT");
        assertThat(service.quote("BTCUSDT", NOW)).isPresent();
        assertThat(service.book("BTCUSDT", NOW)).isPresent();
        assertThat(service.streamUri(service.subscribedSymbols()).toString()).contains("@aggTrade").doesNotContain("@trade/");
    }

    @Test
    void networkRequiresBothExplicitCardSwitchAndExistingProductionPolicy() {
        var env = new org.springframework.mock.env.MockEnvironment();
        try (var guarded = new AssetCardMarketDataService(properties, new ObjectMapper(), mapper, env)) {
            properties.setEnabled(true);
            assertThat(guarded.networkAllowed()).isFalse();
            properties.setExternalCallsEnabled(true);
            assertThat(guarded.networkAllowed()).isFalse();
            env.setProperty("trade-model.provider-call.external-calls-enabled", "yes");
            assertThat(guarded.networkAllowed()).isFalse();
            env.setProperty("trade-model.provider-call.external-calls-enabled", "true");
            assertThat(guarded.networkAllowed()).isTrue();
            env.setActiveProfiles("prod");
            assertThat(guarded.networkAllowed()).isFalse();
            env.setProperty("trade-model.production.scheduler-policy", "EXPLICIT_OPT_IN");
            env.setProperty("trade-model.schedulers.enabled", "true");
            env.setProperty("trade-model.provider-call.enabled", "true");
            assertThat(guarded.networkAllowed()).isTrue();
            env.setProperty("trade-model.provider-call.external-calls-enabled", "false");
            assertThat(guarded.networkAllowed()).isFalse();
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void retentionMustBeExplicitAndLongEnoughForMatureLabels() {
        assertThat(properties.getBarRetention()).isEqualTo(Duration.ZERO);
        assertThat(properties.getFeatureRetention()).isEqualTo(Duration.ZERO);
        assertThat(properties.getTradeRetention()).isEqualTo(Duration.ZERO);
        assertThat(properties.getLabelRetention()).isEqualTo(Duration.ZERO);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> properties.setTradeRetention(Duration.ofHours(4)))
                .isInstanceOf(IllegalArgumentException.class);
        properties.setTradeRetention(Duration.ofHours(5));
        assertThat(properties.getTradeRetention()).isEqualTo(Duration.ofHours(5));
    }

    @Test
    void unreadyOrFailedWriterCannotSuppressLocalClosedBarInference() {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        service.setWriterReadiness(() -> false);
        List<AssetCardMarketDataService.MarketUpdate> events = new ArrayList<>();
        service.addListener(events::add);
        service.acceptMessage(kline(true), NOW);
        assertThat(events).extracting(AssetCardMarketDataService.MarketUpdate::type)
                .containsExactly("PERSISTENCE_FAILURE", "BAR");
        verify(mapper, never()).upsertClosedBar(any());
    }

    @Test
    void observationBackpressureReportsGapButRetainsTheActualLatestPrice() throws Exception {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        List<AssetCardMarketDataService.MarketUpdate> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        service.addListener(events::add);
        service.addTradeListener(value -> { entered.countDown(); try { release.await(5, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); } });
        try {
            service.enqueueMessage(trade("btcusdt@aggTrade", "BTCUSDT", 1, "100", NOW), NOW);
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            for (int i = 2; i <= 2052; i++) service.enqueueMessage(trade("btcusdt@aggTrade", "BTCUSDT", i, "101", NOW), NOW);
            assertThat(service.runtimeMetrics().tradeDrops()).isGreaterThan(0);
            assertThat(service.runtimeMetrics().tradeQueueDepth()).isLessThanOrEqualTo(2048);
            assertThat(events).anyMatch(event -> event.type().equals("OBSERVATION_GAP"));
            assertThat(service.quote("BTCUSDT", NOW).orElseThrow().tradeId()).isEqualTo(2052);
            assertThat(service.quote("BTCUSDT", NOW).orElseThrow().price()).isEqualByComparingTo("101");
        } finally { release.countDown(); }
        assertThat(service.awaitQueues(Duration.ofSeconds(3))).isTrue();
    }

    @Test
    void replacementConnectionPreservesDataAndStaleCloseCannotInvalidateIt() {
        configureWindowFixture();
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        properties.setEnabled(true); properties.setExternalCallsEnabled(true);
        var first = mock(java.net.http.WebSocket.class);
        var replacement = mock(java.net.http.WebSocket.class);
        service.installConnection(first, java.util.Set.of("BTCUSDT"), NOW);
        service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", 10, "100", NOW), NOW);
        service.acceptMessage(depth(10, 12, NOW, "[]", "[]"), NOW);
        service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), NOW);
        service.installConnection(replacement, java.util.Set.of("BTCUSDT"), NOW.plusSeconds(1));
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "lost", first, java.util.Set.of("BTCUSDT"));
        assertThat(service.quote("BTCUSDT", NOW.plusSeconds(1))).isPresent();
        assertThat(service.book("BTCUSDT", NOW.plusSeconds(1))).isPresent();
        verify(first).abort();
        verifyNoInteractions(replacement);
        verify(mapper, atLeastOnce()).storageUsage(); verifyNoMoreInteractions(mapper);
    }

    @Test
    void depthIpWeightFromAnotherCallerConstrainsOurReservationAndResetsAtTheMinute() {
        service.observeIpWeight(1999, NOW);
        assertThat(service.claimDepthBootstrapBudget("BTCUSDT", NOW.plusSeconds(1))).isFalse();
        service.observeIpWeight(0, NOW.plusSeconds(2));
        assertThat(service.claimDepthBootstrapBudget("ETHUSDT", NOW.plusSeconds(3))).isFalse();
        assertThat(service.claimDepthBootstrapBudget("BTCUSDT", NOW.plusSeconds(60))).isTrue();
        verifyNoInteractions(mapper);
    }

    @Test
    void incrementalSubscribeRequiresAckAndUnsubscribeDoesNotRebuildUnchangedBook() {
        configureWindowFixture();
        properties.setEnabled(true); properties.setExternalCallsEnabled(true);
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        var connection = mock(java.net.http.WebSocket.class);
        when(connection.sendText(any(), eq(true))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(connection));
        service.installConnection(connection, java.util.Set.of("BTCUSDT"), NOW);
        service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", 10, "100", NOW), NOW);
        service.reconcileSubscriptions(List.of("BTCUSDT", "ETHUSDT"));
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "reconcileConnection", NOW);
        var message = org.mockito.ArgumentCaptor.forClass(CharSequence.class);
        verify(connection).sendText(message.capture(), eq(true));
        assertThat(message.getValue().toString()).contains("SUBSCRIBE", "ethusdt@aggTrade").doesNotContain("btcusdt@");
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(service, "connectionSymbols"))
                .isEqualTo(java.util.Set.of("BTCUSDT"));
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "transportControl", "{\"id\":1,\"result\":null}", NOW);
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(service, "connectionSymbols"))
                .isEqualTo(java.util.Set.of("BTCUSDT", "ETHUSDT"));
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "reconcileConnection", NOW.plusSeconds(1));
        verify(connection, times(2)).sendText(message.capture(), eq(true));
        assertThat(message.getValue().toString()).contains("UNSUBSCRIBE", "ethusdt@aggTrade").doesNotContain("btcusdt@");
        assertThat(service.quote("BTCUSDT", NOW)).isPresent();
        verify(connection, never()).abort();
    }

    @Test
    void lostConnectionHasBoundedBackoffAndCannotReplayDuplicateTradeObservations() {
        configureWindowFixture();
        properties.setEnabled(true); properties.setExternalCallsEnabled(true);
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        var connection = mock(java.net.http.WebSocket.class);
        service.installConnection(connection, java.util.Set.of("BTCUSDT"), NOW);
        List<AssetCardMarketDataService.SpotQuote> observed = new ArrayList<>();
        service.addTradeListener(observed::add);
        service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", 10, "100", NOW), NOW);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "lost", connection, java.util.Set.of("BTCUSDT"));
        assertThat(service.quote("BTCUSDT", NOW)).isEmpty();
        assertThat(service.runtimeMetrics().reconnects()).isEqualTo(1);
        for (int i = 0; i < 20; i++) org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "connectionFailed", NOW);
        assertThat(service.runtimeMetrics().reconnectAfter()).isEqualTo(NOW.plusSeconds(120));
        service.acceptMessage(trade("btcusdt@aggTrade", "BTCUSDT", 10, "100", NOW), NOW);
        assertThat(observed).hasSize(1);
        assertThat(service.quote("BTCUSDT", NOW)).isEmpty();
        verify(mapper, atLeastOnce()).storageUsage(); verifyNoMoreInteractions(mapper);
    }

    private void configureWindowFixture() {
        try { windowDirectory=windowDirectory.toRealPath(); java.nio.file.Files.setPosixFilePermissions(windowDirectory, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")); }
        catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
        var window = properties.getCollectionWindow();
        window.setId("isolated-window"); window.setStartsAt(NOW); window.setEndsAt(NOW.plus(Duration.ofHours(8)));
        window.setStateDirectory(windowDirectory); window.setSymbols(java.util.Set.of("BTCUSDT", "ETHUSDT"));
        window.setSharedIpWeightAllowancePerMinute(1000); window.setSharedIpWeightLimitPerMinute(6000);
        window.setSharedIpHeadroomConfirmedAt(NOW); window.setMinimumFreeBytes(1);
        when(mapper.storageUsage()).thenReturn(new AssetCardMapper.StorageUsage(0,0,0,100,100,true));
        var lease = (AssetCardMarketDataService.CollectionLease)org.springframework.test.util.ReflectionTestUtils.getField(service,"collectionLease");
        assertThat(lease.check(NOW)).as(lease.status()).isTrue();
    }

    @Test void stoppedWindowRejectsClosedBarReplayWithoutRefreshingItsLedger() throws Exception {
        configureWindowFixture();
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        List<AssetCardMarketDataService.MarketUpdate> events = new ArrayList<>();
        service.addListener(events::add);
        service.stopCollection("OPERATOR_STOPPED_CARD_WINDOW", NOW.plusSeconds(1));
        byte[] stoppedLedger = java.nio.file.Files.readAllBytes(windowDirectory.resolve("isolated-window.json"));
        clearInvocations(mapper);
        service.acceptMessage(kline(true), NOW.plusSeconds(2));
        verifyNoInteractions(mapper);
        assertThat(events).noneMatch(event -> event.type().equals("BAR"));
        assertThat(java.nio.file.Files.readAllBytes(windowDirectory.resolve("isolated-window.json"))).isEqualTo(stoppedLedger);
        assertThat(service.collectionStatus()).isEqualTo("OPERATOR_STOPPED_CARD_WINDOW");
    }

    @Test void persistenceSlotsAllowBoundedParallelIoButStopRejectsQueuedAndFutureWrites() throws Exception {
        configureWindowFixture();
        properties.getWriter().setMaximumPoolSize(2);
        Object lease = org.springframework.test.util.ReflectionTestUtils.getField(service, "collectionLease");
        var workers = java.util.concurrent.Executors.newFixedThreadPool(3);
        var entered = new java.util.concurrent.CountDownLatch(2);
        var release = new java.util.concurrent.CountDownLatch(1);
        var unexpected = new java.util.concurrent.atomic.AtomicInteger();
        Runnable io = () -> {
            assertThat(Thread.holdsLock(service)).isFalse();
            assertThat(Thread.holdsLock(lease)).isFalse();
            entered.countDown();
            try { assertThat(release.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
        };
        try {
            var first = workers.submit(() -> persist(NOW, io));
            var second = workers.submit(() -> persist(NOW, io));
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(persistenceCount("persistenceInFlight")).isEqualTo(2);
            var queued = workers.submit(() -> persist(NOW, unexpected::incrementAndGet));
            await(() -> persistenceCount("persistenceWaiting") == 1);
            service.stopCollection("OPERATOR_STOPPED_CARD_WINDOW", NOW.plusSeconds(1));
            byte[] stoppedLedger = java.nio.file.Files.readAllBytes(windowDirectory.resolve("isolated-window.json"));
            assertThat(queued.get(1, java.util.concurrent.TimeUnit.SECONDS)).isFalse();
            assertThat(persist(NOW.plusSeconds(2), unexpected::incrementAndGet)).isFalse();
            assertThat(persistenceCount("persistenceInFlight")).isEqualTo(2);
            release.countDown();
            assertThat(first.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(persistenceCount("persistenceInFlight")).isZero();
            assertThat(persistenceCount("persistenceWaiting")).isZero();
            assertThat(persistenceCount("persistenceCompleted")).isEqualTo(2);
            assertThat(persistenceCount("persistenceRejected")).isEqualTo(2);
            assertThat(unexpected).hasValue(0);
            assertThat(java.nio.file.Files.readAllBytes(windowDirectory.resolve("isolated-window.json"))).isEqualTo(stoppedLedger);
        } finally {
            release.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void persistenceSlotTimeoutAndFailureReleaseDoNotExtendLeaseOrSwallowIoFailure() throws Exception {
        configureWindowFixture();
        properties.getWriter().setMaximumPoolSize(1);
        properties.getWriter().setConnectionTimeout(Duration.ofMillis(250));
        var workers = java.util.concurrent.Executors.newSingleThreadExecutor();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var unexpected = new java.util.concurrent.atomic.AtomicInteger();
        byte[] originalLedger = java.nio.file.Files.readAllBytes(windowDirectory.resolve("isolated-window.json"));
        try {
            var first = workers.submit(() -> persist(NOW, () -> {
                entered.countDown();
                try { assertThat(release.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
            }));
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            long started = System.nanoTime();
            assertThat(persist(NOW, unexpected::incrementAndGet)).isFalse();
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isBetween(Duration.ofMillis(200), Duration.ofSeconds(2));
            release.countDown(); assertThat(first.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(persistenceCount("persistenceInFlight")).isZero();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> persist(NOW, () -> {
                throw new IllegalStateException("ISOLATED_CARD_IO_FAILURE");
            })).isInstanceOf(IllegalStateException.class).hasMessage("ISOLATED_CARD_IO_FAILURE");
            assertThat(persistenceCount("persistenceInFlight")).isZero();
            assertThat(persistenceCount("persistenceCompleted")).isEqualTo(2);
            assertThat(persist(NOW.plus(Duration.ofHours(8)), unexpected::incrementAndGet)).isFalse();
            assertThat(unexpected).hasValue(0);
            assertThat(java.nio.file.Files.readAllBytes(windowDirectory.resolve("isolated-window.json"))).isEqualTo(originalLedger);
        } finally {
            release.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void queuedClosedBarRechecksEpochAfterPersistenceSlotBecomesAvailable() throws Exception {
        properties.getWriter().setMaximumPoolSize(1);
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 1L);
        var workers = java.util.concurrent.Executors.newSingleThreadExecutor();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        try {
            var occupied = workers.submit(() -> persist(NOW, () -> {
                entered.countDown();
                try { assertThat(release.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
            }));
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "enqueueMessage", kline(true), NOW, 1L);
            await(() -> persistenceCount("persistenceWaiting") == 1);
            synchronized (service) { org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 2L); }
            release.countDown(); assertThat(occupied.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(service.awaitQueues(Duration.ofSeconds(2))).isTrue();
            verify(mapper, never()).upsertClosedBar(any());
            assertThat(persistenceCount("persistenceInFlight")).isZero();
        } finally {
            release.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void livePersistenceSlotTimeoutReportsBarFailureInsteadOfSilentlyDroppingClosedBoundary() throws Exception {
        properties.getWriter().setMaximumPoolSize(1);
        properties.getWriter().setConnectionTimeout(Duration.ofMillis(250));
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        List<AssetCardMarketDataService.MarketUpdate> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        service.addListener(events::add);
        var workers = java.util.concurrent.Executors.newSingleThreadExecutor();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        try {
            var occupied = workers.submit(() -> persist(NOW, () -> {
                entered.countDown();
                try { assertThat(release.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
            }));
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            service.acceptMessage(kline(true), NOW);
            assertThat(events).extracting(AssetCardMarketDataService.MarketUpdate::type).containsExactly("PERSISTENCE_FAILURE", "BAR");
            verify(mapper, never()).upsertClosedBar(any());
            release.countDown(); assertThat(occupied.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void closeWaitsForAdmittedIoBeforeRecordingCleanShutdown() throws Exception {
        configureLoopback(null);
        var lease = (AssetCardMarketDataService.CollectionLease)org.springframework.test.util.ReflectionTestUtils.getField(service, "collectionLease");
        assertThat(lease.check(Instant.now())).isTrue();
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        try {
            var writing = workers.submit(() -> persist(Instant.now(), () -> {
                entered.countDown();
                try { assertThat(release.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
            }));
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var closing = workers.submit(service::close);
            await(() -> Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.getField(service, "stopped")));
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> closing.get(100, java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            assertThat(ledger().path("cleanShutdown").asBoolean()).isFalse();
            assertThat(persist(Instant.now(), () -> { throw new AssertionError("No post-close write"); })).isFalse();
            release.countDown(); assertThat(writing.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            closing.get(1, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(ledger().path("cleanShutdown").asBoolean()).isTrue();
            assertThat(persistenceCount("persistenceInFlight")).isZero();
        } finally {
            release.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void closeDrainTimeoutPersistsStopAndDoesNotGrantRestartAContinuation() throws Exception {
        configureLoopback(null); properties.getWriter().setConnectionTimeout(Duration.ofMillis(250));
        var lease = (AssetCardMarketDataService.CollectionLease)org.springframework.test.util.ReflectionTestUtils.getField(service, "collectionLease");
        assertThat(lease.check(Instant.now())).isTrue();
        var workers = java.util.concurrent.Executors.newSingleThreadExecutor();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        try {
            var writing = workers.submit(() -> persist(Instant.now(), () -> {
                entered.countDown();
                try { assertThat(release.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
            }));
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            long start = System.nanoTime(); service.close();
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isBetween(Duration.ofMillis(200), Duration.ofSeconds(2));
            assertThat(ledger().path("state").asText()).isEqualTo("STOPPED");
            assertThat(ledger().path("reason").asText()).isEqualTo("PERSISTENCE_DRAIN_TIMEOUT");
            assertThat(ledger().path("cleanShutdown").asBoolean()).isFalse();
            var restarted = new AssetCardMarketDataService.CollectionLease(properties.getCollectionWindow(), new ObjectMapper(), mapper::storageUsage);
            assertThat(restarted.check(Instant.now())).isFalse();
            assertThat(restarted.status()).isEqualTo("PERSISTENCE_DRAIN_TIMEOUT");
            release.countDown(); assertThat(writing.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(persistenceCount("persistenceCompleted")).isEqualTo(1);
        } finally {
            release.countDown(); workers.shutdownNow();
            assertThat(workers.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    private boolean persist(Instant at, Runnable action) {
        return Boolean.TRUE.equals(org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "persistCard", at, action));
    }

    private long persistenceCount(String method) {
        Number value = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, method);
        return value.longValue();
    }

    @Test void absoluteEightHourWindowExpiresAcrossRestartAndCannotBeRebased() throws Exception {
        configureWindowFixture();
        var window=properties.getCollectionWindow();
        var first=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(first.check(NOW.plusSeconds(1))).isTrue();
        assertThat(first.accepting(NOW.plus(Duration.ofHours(8)))).isFalse();
        assertThat(first.check(NOW.plus(Duration.ofHours(8)))).isFalse();
        assertThat(first.status()).isEqualTo("WINDOW_EXPIRED");
        var restarted=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(restarted.check(NOW.plus(Duration.ofHours(8)).plusSeconds(1))).isFalse();
        assertThat(restarted.status()).isEqualTo("WINDOW_EXPIRED");
        window.setStartsAt(NOW.plusSeconds(60)); window.setEndsAt(NOW.plusSeconds(60).plus(Duration.ofHours(8)));
        assertThat(restarted.check(NOW.plusSeconds(61))).isFalse();
        assertThat(restarted.status()).isEqualTo("COLLECTION_LEDGER_OR_STORAGE_UNAVAILABLE");
    }

    @Test void durableRestReservationsAndStopSurviveTwoInstancesAndRestart() throws Exception {
        var window=budgetFixture("rest-window"); window.setMaximumRestRequests(2);
        var a=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        var b=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(a.reserve("REST",NOW)).isTrue(); assertThat(b.reserve("REST",NOW.plusSeconds(1))).isTrue();
        assertThat(a.reserve("REST",NOW.plusSeconds(2))).isFalse();
        assertThat(a.status()).isEqualTo("REST_TOTAL_BUDGET_EXHAUSTED");
        assertThat(b.check(NOW.plusSeconds(20))).isFalse();
        assertThat(b.status()).isEqualTo("REST_TOTAL_BUDGET_EXHAUSTED");
    }

    @Test void missingOrDeletedLedgerNeverGrantsFreshBudget() throws Exception {
        var window=budgetFixture("lost-ledger");
        var a=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(a.check(NOW)).isTrue();
        java.nio.file.Files.delete(windowDirectory.resolve("lost-ledger.json")); // only this disposable fixture file
        var restarted=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(restarted.check(NOW.plusSeconds(1))).isFalse();
        assertThat(restarted.status()).isEqualTo("COLLECTION_LEDGER_OR_STORAGE_UNAVAILABLE");
    }

    @Test void everyStorageBoundaryStopsAndKeepsTerminalReasonAcrossRestart() throws Exception {
        var window=budgetFixture("storage-window"); window.setMaximumNewRows(10);
        window.setMaximumDatabaseGrowthBytes(10); window.setMaximumWalGrowthBytes(10); window.setMinimumFreeBytes(100);
        var metrics=new java.util.concurrent.atomic.AtomicReference<>(new AssetCardMapper.StorageUsage(0,0,0,100,100,true));
        var space=new java.util.concurrent.atomic.AtomicLong(1000);
        var lease=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),metrics::get,space::get);
        assertThat(lease.check(NOW)).isTrue();
        metrics.set(new AssetCardMapper.StorageUsage(0,0,10,100,100,true));
        assertThat(lease.check(NOW.plusSeconds(15))).isFalse();
        assertThat(lease.status()).isEqualTo("DATABASE_ROW_LIMIT");
        assertThat(new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),metrics::get,space::get)
                .check(NOW.plusSeconds(16))).isFalse();
        for (String kind:List.of("DB", "WAL", "FREE", "ERROR")) {
            window.setId("storage-"+kind); metrics.set(new AssetCardMapper.StorageUsage(0,0,0,100,100,true)); space.set(1000);
            var current=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),() -> {
                if (metrics.get()==null) throw new IllegalStateException("isolated-unavailable"); return metrics.get();
            },space::get);
            assertThat(current.check(NOW)).isTrue();
            if (kind.equals("DB")) metrics.set(new AssetCardMapper.StorageUsage(0,0,0,110,100,true));
            if (kind.equals("WAL")) metrics.set(new AssetCardMapper.StorageUsage(0,0,0,100,110,true));
            if (kind.equals("FREE")) space.set(99);
            if (kind.equals("ERROR")) metrics.set(null);
            assertThat(current.check(NOW.plusSeconds(15))).isFalse();
            assertThat(current.status()).isEqualTo(switch(kind) {
                case "DB" -> "DATABASE_GROWTH_LIMIT"; case "WAL" -> "WAL_GROWTH_LIMIT";
                case "FREE" -> "STORAGE_FREE_SPACE_LIMIT"; default -> "STORAGE_MEASUREMENT_FAILED";
            });
        }
    }

    @Test void collectionWindowRejectsUnknownQuotaLongDurationAndExtraSymbols() throws Exception {
        var window=budgetFixture("invalid-window");
        window.setEndsAt(NOW.plusSeconds(8*3600+1)); assertThat(window.valid()).isFalse();
        window.setEndsAt(NOW.plusSeconds(8*3600)); window.setSharedIpWeightLimitPerMinute(0); assertThat(window.valid()).isFalse();
        window.setSharedIpWeightLimitPerMinute(6000); window.setSharedIpHeadroomConfirmedAt(NOW.minusSeconds(301)); assertThat(window.valid()).isFalse();
        window.setSharedIpHeadroomConfirmedAt(NOW);
        service.reconcileSubscriptions(List.of("BTCUSDT","ETHUSDT","SOLUSDT"));
        assertThat(service.subscribedSymbols()).containsExactlyInAnyOrder("BTCUSDT","ETHUSDT");
        assertThat(properties.isEnabled()).isFalse(); // quota checks never enable a scheduler
    }

    @Test void connectionAndControlBudgetsPersistAndDoNotAffectOtherSchedulerSwitches() throws Exception {
        var window=budgetFixture("connection-budget"); window.setMaximumConnectionAttempts(1); window.setMaximumControlMessages(1);
        for (String kind:List.of("CONNECTION","CONTROL")) {
            window.setId("limited-"+kind);
            var first=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
            assertThat(first.reserve(kind,NOW)).isTrue();
            var restarted=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
            assertThat(restarted.reserve(kind,NOW.plusSeconds(1))).isFalse();
            assertThat(restarted.status()).isEqualTo(kind.equals("CONNECTION") ? "WS_CONNECTION_BUDGET_EXHAUSTED" : "WS_CONTROL_BUDGET_EXHAUSTED");
        }
        properties.setEnabled(true); properties.setExternalCallsEnabled(true);
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        var socket=mock(java.net.http.WebSocket.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"socket",socket);
        service.stopCollection("OPERATOR_STOPPED_CARD_WINDOW",NOW.plusSeconds(2));
        verify(socket).abort();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isExternalCallsEnabled()).isTrue();
        assertThat(service.subscribedSymbols()).containsExactly("BTCUSDT");
        assertThat(service.collectionAccepting(NOW.plusSeconds(2))).isFalse();
        // A later stop must not overwrite the first durable cause or reset its allowance.
        assertThat(service.collectionStatus()).isEqualTo("WS_CONTROL_BUDGET_EXHAUSTED");
    }

    @Test void unsafeLedgerPermissionsAndClockRollbackFailClosed() throws Exception {
        var window=budgetFixture("protected-ledger");
        java.nio.file.Files.setPosixFilePermissions(windowDirectory,java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        var first=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(first.check(NOW)).isFalse();
        assertThat(first.status()).isEqualTo("COLLECTION_LEDGER_OR_STORAGE_UNAVAILABLE");
        java.nio.file.Files.setPosixFilePermissions(windowDirectory,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        assertThat(first.check(NOW.plusSeconds(40))).isFalse(); // fixing the directory cannot reset a terminal stop
        window.setId("clock-fixture");
        var clock=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(clock.check(NOW.plusSeconds(40))).isTrue();
        var restarted=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage);
        assertThat(restarted.check(NOW.plusSeconds(20))).isFalse();
        assertThat(restarted.status()).isEqualTo("CLOCK_MOVED_BACKWARDS");
    }

    @Test void stopBeforeStartPersistsWithoutAWriterAndSurvivesRestart() throws Exception {
        var window=budgetFixture("prestart-stop");
        var first=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),() -> {
            throw new IllegalStateException("No writer before start");
        },() -> 1L,"first-process");
        first.stop("ARCHIVE_VERIFICATION_OR_STORAGE_FAILURE",NOW.minusSeconds(10));
        assertThat(first.status()).isEqualTo("ARCHIVE_VERIFICATION_OR_STORAGE_FAILURE");
        var restarted=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage,() -> 1L,"next-process");
        assertThat(restarted.check(NOW)).isFalse();
        assertThat(restarted.status()).isEqualTo("ARCHIVE_VERIFICATION_OR_STORAGE_FAILURE");
    }

    @Test void failedStopPersistenceCannotResumeOpenLedgerAfterIoRecovers() throws Exception {
        var window=budgetFixture("failed-stop");
        var first=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage,() -> 1L,"first-process");
        assertThat(first.check(NOW)).isTrue();
        var ledger=windowDirectory.resolve(window.getId()+".json");
        java.nio.file.Files.setPosixFilePermissions(ledger,java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--"));
        first.stop("ARCHIVE_VERIFICATION_OR_STORAGE_FAILURE",NOW.plusSeconds(1));
        assertThat(first.accepting(NOW.plusSeconds(1))).isFalse();
        java.nio.file.Files.setPosixFilePermissions(ledger,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        assertThat(first.check(NOW.plusSeconds(2))).isFalse();
        var next=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage,() -> 1L,"next-process");
        assertThat(next.check(NOW.plusSeconds(3))).isFalse();
    }

    @Test void cleanRestartRetainsBudgetButUncleanProcessCannotReopenOldAllowance() throws Exception {
        var window=budgetFixture("restart-identity"); window.setMaximumRestRequests(2);
        var first=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage,() -> 1L,"first-process");
        assertThat(first.reserve("REST",NOW)).isTrue(); first.release(NOW.plusSeconds(1));
        var clean=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage,() -> 1L,"clean-process");
        assertThat(clean.reserve("REST",NOW.plusSeconds(2))).isTrue();
        var unclean=new AssetCardMarketDataService.CollectionLease(window,new ObjectMapper(),mapper::storageUsage,() -> 1L,"unclean-process");
        assertThat(unclean.check(NOW.plusSeconds(3))).isFalse();
        assertThat(unclean.status()).isEqualTo("UNVERIFIED_PREVIOUS_COLLECTION_PROCESS");
        assertThat(unclean.reserve("REST",NOW.plusSeconds(4))).isFalse();
        assertThat(new ObjectMapper().readTree(java.nio.file.Files.readAllBytes(windowDirectory.resolve(window.getId()+".json"))).path("rest").asLong()).isEqualTo(2);
    }

    private AssetCardProperties.CollectionWindow budgetFixture(String id) throws Exception {
        windowDirectory=windowDirectory.toRealPath();
        java.nio.file.Files.setPosixFilePermissions(windowDirectory,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        var window=properties.getCollectionWindow(); window.setId(id); window.setStartsAt(NOW);
        window.setEndsAt(NOW.plus(Duration.ofHours(8))); window.setStateDirectory(windowDirectory);
        window.setSymbols(java.util.Set.of("BTCUSDT","ETHUSDT"));
        window.setSharedIpWeightAllowancePerMinute(1000); window.setSharedIpWeightLimitPerMinute(6000); window.setSharedIpHeadroomConfirmedAt(NOW);
        window.setMinimumFreeBytes(1);
        when(mapper.storageUsage()).thenReturn(new AssetCardMapper.StorageUsage(0,0,0,100,100,true));
        return window;
    }

    @Test void loopbackRuntimeEvidenceIsBoundedAndNeverInventsReceivedFrameTimes() throws Exception {
        var logger = (ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(AssetCardMarketDataService.class);
        var captured = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        captured.start(); logger.addAppender(captured);
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server); service.ensureConnected(); var peer = server.next();
            await(() -> currentSocket() != null);
            service.ensureConnected();
            List<String> initial = runtimeLines(captured);
            assertThat(initial).isNotEmpty();
            assertThat(initial.get(initial.size() - 1)).contains("lastReceivedFrameAt=null", "lastReceivedMarketAt=null");
            for (int i = 0; i < 12; i++) service.ensureConnected();
            assertThat(runtimeLines(captured)).hasSize(initial.size());
            peer.text(trade("btcusdt@aggTrade", "BTCUSDT", 901, "100", Instant.now().minusMillis(5)));
            await(() -> service.quote("BTCUSDT", Instant.now()).isPresent());
            org.springframework.test.util.ReflectionTestUtils.setField(service, "lastRuntimeLogAt", Instant.now().minusSeconds(61));
            service.ensureConnected();
            List<String> active = runtimeLines(captured);
            assertThat(active).hasSize(initial.size() + 1);
            String latest = active.get(active.size() - 1);
            assertThat(latest).contains("utc=", "epoch=", "attempt=", "queues=", "drops=", "reconnects=",
                    "lease=RUNNING", "persistenceInFlight=0", "persistenceCompleted=0");
            assertThat(latest).doesNotContain("lastReceivedFrameAt=null", "lastReceivedMarketAt=null",
                    "ws://", "wss://", "http://", "https://", windowDirectory.toString(), "BTCUSDT", "aggTrade");
            service.stopCollection("OPERATOR_STOPPED_CARD_WINDOW", Instant.now());
            service.ensureConnected();
            List<String> terminal = runtimeLines(captured);
            assertThat(terminal).hasSize(active.size() + 1);
            assertThat(terminal.get(terminal.size() - 1)).contains("lease=OPERATOR_STOPPED_CARD_WINDOW");
            for (int i = 0; i < 12; i++) service.ensureConnected();
            assertThat(runtimeLines(captured)).hasSize(terminal.size());
            assertThat(server.connections).hasValue(1);
        } finally { logger.detachAppender(captured); captured.stop(); }
    }

    private static List<String> runtimeLines(ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> captured) {
        return captured.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("[asset-card] Spot runtime ")).toList();
    }

    @Test void loopbackExpiredCheckAbortsExplicitlyAndRecoversWithoutResettingAllowance() throws Exception {
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server);
            service.ensureConnected();
            var first = server.next();
            await(() -> currentSocket() != null);
            first.text(trade("btcusdt@aggTrade", "BTCUSDT", 10, "100", Instant.now().minusMillis(5)));
            await(() -> service.quote("BTCUSDT", Instant.now()).isPresent());
            Object lease = org.springframework.test.util.ReflectionTestUtils.getField(service, "collectionLease");
            org.springframework.test.util.ReflectionTestUtils.setField(lease, "checkedAt", Instant.now().minusSeconds(20));
            first.text(trade("btcusdt@aggTrade", "BTCUSDT", 11, "101", Instant.now().minusMillis(5)));
            await(() -> currentSocket() == null);
            assertThat(service.quote("BTCUSDT", Instant.now())).isEmpty();
            reconnectWithinExistingWindow();
            var second = server.next();
            await(() -> currentSocket() != null);
            second.text(trade("btcusdt@aggTrade", "BTCUSDT", 12, "102", Instant.now().minusMillis(5)));
            await(() -> service.quote("BTCUSDT", Instant.now()).map(q -> q.tradeId() == 12).orElse(false));
            assertThat(ledger().path("connections").asLong()).isEqualTo(2);
            assertThat(ledger().path("state").asText()).isEqualTo("OPEN");
        }
    }

    @Test void loopbackSilentConnectionRetiresButPingAndOtherAssetTrafficKeepItAlive() throws Exception {
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server);
            service.ensureConnected(); var first = server.next();
            await(() -> currentSocket() != null);
            var old = currentSocket();
            // Connection-wide liveness is independent of whether ETH has traded.
            first.text(trade("btcusdt@aggTrade", "BTCUSDT", 1, "100", Instant.now().minusMillis(5)));
            await(() -> service.quote("BTCUSDT", Instant.now()).isPresent());
            first.frame(9, new byte[]{42});
            await(() -> first.pongs.get() == 1);
            service.ensureConnected();
            assertThat(currentSocket()).isSameAs(old);
            assertThat(service.quote("ETHUSDT", Instant.now())).isEmpty();
            org.springframework.test.util.ReflectionTestUtils.setField(service, "lastFrameAt", Instant.now().minusSeconds(121));
            service.ensureConnected();
            await(() -> currentSocket() == null);
            reconnectWithinExistingWindow();
            server.next(); await(() -> currentSocket() != null);
            assertThat(currentSocket()).isNotSameAs(old);
            assertThat(first.pongs.get()).isEqualTo(1); // Java17 automatic Pong, no duplicate application heartbeat.
        }
    }

    @Test void loopbackRetiredHandshakeCannotInstallAfterWriterRecovery() throws Exception {
        try (var server = new LoopbackSpotServer(101, true)) {
            configureLoopback(server);
            service.ensureConnected(); var delayed = server.next();
            service.setWriterReadiness(() -> false); service.ensureConnected();
            service.setWriterReadiness(() -> true); service.ensureConnected();
            var replacement = server.next();
            await(() -> currentSocket() != null);
            var current = currentSocket();
            delayed.releaseHandshake.countDown();
            replacement.text(trade("btcusdt@aggTrade", "BTCUSDT", 2, "102", Instant.now().minusMillis(5)));
            await(() -> service.quote("BTCUSDT", Instant.now()).isPresent());
            assertThat(currentSocket()).isSameAs(current);
            assertThat(ledger().path("connections").asLong()).isEqualTo(2);
        }
    }

    @Test void loopbackMalformedAndUnsubscribedFramesCannotRenewMarketLiveness() throws Exception {
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server); service.ensureConnected(); var peer = server.next();
            await(() -> currentSocket() != null);
            Instant expired = Instant.now().minusSeconds(121);
            org.springframework.test.util.ReflectionTestUtils.setField(service, "lastMarketFrameAt", expired);
            for (String invalid : List.of("not-json", "{\"stream\":\"btcusdt@kline_5m\",\"data\":{\"e\":\"kline\",\"s\":\"BTCUSDT\"}}",
                    trade("solusdt@aggTrade", "SOLUSDT", 1, "100", Instant.now().minusMillis(5)))) {
                int priorPongs = peer.pongs.get(); peer.text(invalid); peer.frame(9, new byte[]{7});
                await(() -> peer.pongs.get() > priorPongs); // ordered text delivery completed through the real listener.
                assertThat(org.springframework.test.util.ReflectionTestUtils.getField(service, "lastMarketFrameAt")).isEqualTo(expired);
            }
            service.ensureConnected();
            assertThat(currentSocket()).isNull();
            assertThat(service.runtimeMetrics().reconnects()).isEqualTo(1);
            assertThat(service.quote("SOLUSDT", Instant.now())).isEmpty();
            verify(mapper, never()).upsertClosedBar(any());
        }
    }

    @Test void loopbackOpenKlineRenewsMarketLivenessWithoutBarPersistenceOrInference() throws Exception {
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server); service.ensureConnected(); var peer = server.next();
            await(() -> currentSocket() != null); var opened = currentSocket();
            List<AssetCardMarketDataService.MarketUpdate> events = new java.util.concurrent.CopyOnWriteArrayList<>();
            service.addListener(events::add);
            Instant expired = Instant.now().minusSeconds(121);
            org.springframework.test.util.ReflectionTestUtils.setField(service, "lastMarketFrameAt", expired);
            var frame = new ObjectMapper().readTree(kline(false));
            ((com.fasterxml.jackson.databind.node.ObjectNode)frame.path("data")).put("E", Instant.now().toEpochMilli());
            peer.text(frame.toString()); peer.frame(9, new byte[]{8});
            await(() -> peer.pongs.get() == 1);
            assertThat((Instant)org.springframework.test.util.ReflectionTestUtils.getField(service, "lastMarketFrameAt")).isAfter(expired);
            service.ensureConnected();
            assertThat(currentSocket()).isSameAs(opened);
            assertThat(events).isEmpty();
            assertThat(service.runtimeMetrics().barQueueDepth()).isZero();
            verify(mapper, never()).upsertClosedBar(any());
        }
    }

    @Test void loopbackHandshakeFailureThenAbnormalCloseRecoverWithBoundedAttempts() throws Exception {
        try (var server = new LoopbackSpotServer(503, false)) {
            configureLoopback(server);
            service.ensureConnected(); server.next();
            await(() -> service.runtimeMetrics().reconnects() == 1);
            assertThat(currentSocket()).isNull();
            reconnectWithinExistingWindow(); var second = server.next();
            await(() -> currentSocket() != null);
            second.socket.close();
            await(() -> currentSocket() == null);
            reconnectWithinExistingWindow(); var third = server.next();
            await(() -> currentSocket() != null);
            third.text(trade("btcusdt@aggTrade", "BTCUSDT", 5, "105", Instant.now().minusMillis(5)));
            await(() -> service.quote("BTCUSDT", Instant.now()).isPresent());
            assertThat(ledger().path("connections").asLong()).isEqualTo(3);
            assertThat(service.runtimeMetrics().reconnects()).isEqualTo(2);
        }
    }

    @Test void loopbackNormalCloseRejoinsDepthAndRejectsLateOldFrames() throws Exception {
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server); service.ensureConnected(); var first = server.next();
            await(() -> currentSocket() != null);
            Instant at = Instant.now().minusMillis(5);
            first.text(depth(10, 12, at, "[]", "[]"));
            await(() -> service.bufferedDepthEventCount("BTCUSDT") == 1);
            service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), Instant.now());
            assertThat(service.book("BTCUSDT", Instant.now())).isPresent();
            first.frame(8, new byte[]{3, (byte)232});
            await(() -> currentSocket() == null);
            assertThat(service.book("BTCUSDT", Instant.now())).isEmpty();
            reconnectWithinExistingWindow(); var second = server.next();
            await(() -> currentSocket() != null);
            second.text(depth(30, 32, Instant.now().minusMillis(5), "[]", "[]"));
            await(() -> service.bufferedDepthEventCount("BTCUSDT") == 1);
            service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(11), Instant.now());
            assertThat(service.book("BTCUSDT", Instant.now())).isEmpty();
            service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(31), Instant.now());
            assertThat(service.book("BTCUSDT", Instant.now()).orElseThrow().sequence()).isEqualTo(32);
        }
    }

    @Test void loopbackTerminalBudgetAndExpiryNeverReconnectOrRefreshPrice() throws Exception {
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server); properties.getCollectionWindow().setMaximumConnectionAttempts(1);
            service.ensureConnected(); var first = server.next(); await(() -> currentSocket() != null);
            first.text(trade("btcusdt@aggTrade", "BTCUSDT", 1, "100", Instant.now().minusMillis(5)));
            await(() -> service.quote("BTCUSDT", Instant.now()).isPresent());
            first.socket.close(); await(() -> currentSocket() == null);
            reconnectWithinExistingWindow();
            assertThat(service.collectionStatus()).isEqualTo("WS_CONNECTION_BUDGET_EXHAUSTED");
            for (int i = 0; i < 5; i++) service.ensureConnected();
            assertThat(server.connections.get()).isEqualTo(1);
            assertThat(service.quote("BTCUSDT", Instant.now())).isEmpty();
            assertThat(ledger().path("state").asText()).isEqualTo("STOPPED");
        }
    }

    @Test void loopbackAbsoluteDeadlineStopsSocketWithoutExtendingTheWindow() throws Exception {
        try (var server = new LoopbackSpotServer(101, false)) {
            configureLoopback(server);
            properties.getCollectionWindow().setEndsAt(Instant.now().plusSeconds(2));
            service.ensureConnected(); server.next(); await(() -> currentSocket() != null);
            await(() -> !Instant.now().isBefore(properties.getCollectionWindow().getEndsAt()));
            service.enforceCollectionWindow();
            assertThat(currentSocket()).isNull();
            assertThat(service.collectionStatus()).isEqualTo("WINDOW_EXPIRED");
            for (int i = 0; i < 5; i++) service.ensureConnected();
            assertThat(server.connections.get()).isEqualTo(1);
            assertThat(ledger().path("state").asText()).isEqualTo("STOPPED");
            assertThat(ledger().path("reason").asText()).isEqualTo("WINDOW_EXPIRED");
        }
    }

    @Test void loopbackOnlyOneHandshakeMayBePendingAndOldQueuedObservationsCannotReturn() throws Exception {
        try (var server = new LoopbackSpotServer(101, true)) {
            configureLoopback(server); service.ensureConnected(); var first = server.next();
            for (int i = 0; i < 12; i++) service.ensureConnected();
            assertThat(server.connections.get()).isEqualTo(1);
            first.releaseHandshake.countDown(); await(() -> currentSocket() != null);
            var entered = new java.util.concurrent.CountDownLatch(1);
            var release = new java.util.concurrent.CountDownLatch(1);
            var executors = (java.util.concurrent.ThreadPoolExecutor[])org.springframework.test.util.ReflectionTestUtils.getField(service, "tradeFrames");
            executors[Math.floorMod("BTCUSDT".hashCode(), executors.length)].execute(() -> {
                entered.countDown(); try { release.await(6, java.util.concurrent.TimeUnit.SECONDS); }
                catch (InterruptedException stop) { Thread.currentThread().interrupt(); }
            });
            assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            List<Long> observed = new java.util.concurrent.CopyOnWriteArrayList<>();
            service.addTradeListener(value -> observed.add(value.tradeId()));
            try {
                first.text(trade("btcusdt@aggTrade", "BTCUSDT", 10, "100", Instant.now().minusMillis(5)));
                await(() -> service.quote("BTCUSDT", Instant.now()).isPresent());
                first.socket.close(); await(() -> currentSocket() == null);
                reconnectWithinExistingWindow(); var second = server.next(); await(() -> currentSocket() != null);
                second.text(trade("btcusdt@aggTrade", "BTCUSDT", 11, "101", Instant.now().minusMillis(5)));
                await(() -> service.quote("BTCUSDT", Instant.now()).map(q -> q.tradeId() == 11).orElse(false));
            } finally { release.countDown(); }
            assertThat(service.awaitQueues(Duration.ofSeconds(2))).isTrue();
            assertThat(observed).containsExactly(11L);
        }
    }

    @Test void depthEpochMustBeRecheckedInsideTheBookMutationLock() throws Exception {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 1L);
        synchronized (service) {
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "enqueueMessage",
                    depth(90, 92, NOW, "[]", "[]"), NOW, 1L);
            await(() -> Thread.getAllStackTraces().entrySet().stream().anyMatch(entry ->
                    entry.getKey().getName().startsWith("asset-card-spot-depth-")
                            && entry.getKey().getState() == Thread.State.BLOCKED
                            && java.util.Arrays.stream(entry.getValue()).anyMatch(frame -> frame.getMethodName().equals("acceptDiffDepth"))));
            // The old worker has passed process()'s first epoch check, but cannot yet own the book lock.
            org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 2L);
            service.acceptMessage(depth(30, 32, NOW, "[]", "[]"), NOW);
            service.acceptDepthSnapshot("BTCUSDT", depthSnapshot(31), NOW);
            assertThat(service.book("BTCUSDT", NOW).orElseThrow().sequence()).isEqualTo(32);
        }
        assertThat(service.awaitQueues(Duration.ofSeconds(2))).isTrue();
        assertThat(service.book("BTCUSDT", NOW).orElseThrow().sequence()).isEqualTo(32);
    }

    @Test void retiredBarIoCannotPublishAnOldEpochOrHoldTheGlobalLifecycleLock() throws Exception {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 1L);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        List<AssetCardMarketDataService.MarketUpdate> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        service.addListener(events::add);
        when(mapper.upsertClosedBar(any())).thenAnswer(call -> {
            assertThat(Thread.holdsLock(service)).isFalse();
            entered.countDown(); release.await(3, java.util.concurrent.TimeUnit.SECONDS); return 1;
        });
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "enqueueMessage", kline(true), NOW, 1L);
        assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        synchronized (service) { org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 2L); }
        release.countDown(); assertThat(service.awaitQueues(Duration.ofSeconds(2))).isTrue();
        verify(mapper).upsertClosedBar(any()); // Started immutable historical IO is retained, not rolled back or re-timestamped.
        assertThat(events).isEmpty();
        var cache = (java.util.Map<?, ?>)org.springframework.test.util.ReflectionTestUtils.getField(service, "closedBars");
        assertThat((java.util.Map<?, ?>)cache.get("BTCUSDT|5m")).isEmpty();
    }

    @Test void oldObservationFailureCannotInvalidateNewEpochHealth() throws Exception {
        service.reconcileSubscriptions(List.of("BTCUSDT"));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 1L);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        List<AssetCardMarketDataService.MarketUpdate> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        service.addListener(events::add);
        service.addTradeListener(value -> {
            entered.countDown(); try { release.await(3, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException stop) { Thread.currentThread().interrupt(); }
            throw new IllegalStateException("LOCAL_TEST_FAILURE_NOT_FOR_LOGS");
        });
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "enqueueMessage",
                trade("btcusdt@aggTrade", "BTCUSDT", 1, "100", NOW), NOW, 1L);
        assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        synchronized (service) { org.springframework.test.util.ReflectionTestUtils.setField(service, "connectionEpoch", 2L); }
        release.countDown(); assertThat(service.awaitQueues(Duration.ofSeconds(2))).isTrue();
        assertThat(events).extracting(AssetCardMarketDataService.MarketUpdate::type).containsExactly("PRICE");
    }

    @Test void successfulHandshakeFutureBeforeOnOpenRetainsSingleFlightAndInstallsTheSocket() throws Exception {
        configureLoopback(null);
        var client = mock(java.net.http.HttpClient.class);
        var builder = mock(java.net.http.WebSocket.Builder.class);
        var opening = new java.util.concurrent.CompletableFuture<java.net.http.WebSocket>();
        var listener = new java.util.concurrent.atomic.AtomicReference<java.net.http.WebSocket.Listener>();
        when(client.newWebSocketBuilder()).thenReturn(builder);
        when(builder.connectTimeout(any())).thenReturn(builder);
        when(builder.buildAsync(any(), any())).thenAnswer(call -> { listener.set(call.getArgument(1)); return opening; });
        org.springframework.test.util.ReflectionTestUtils.setField(service, "http", client);
        service.ensureConnected();
        var opened = mock(java.net.http.WebSocket.class);
        // Java17 may complete buildAsync before its separately scheduled onOpen callback runs.
        opening.complete(opened);
        assertThat(((java.util.concurrent.atomic.AtomicBoolean)org.springframework.test.util.ReflectionTestUtils.getField(service, "connecting")).get()).isTrue();
        service.ensureConnected();
        verify(builder, times(1)).buildAsync(any(), any());
        listener.get().onOpen(opened);
        assertThat(currentSocket()).isSameAs(opened);
        assertThat(((java.util.concurrent.atomic.AtomicBoolean)org.springframework.test.util.ReflectionTestUtils.getField(service, "connecting")).get()).isFalse();
        verify(opened).request(1);
        verify(opened, never()).abort();
    }

    private void configureLoopback(LoopbackSpotServer server) throws Exception {
        windowDirectory = windowDirectory.toRealPath();
        java.nio.file.Files.setPosixFilePermissions(windowDirectory, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        Instant now = Instant.now(); var window = properties.getCollectionWindow();
        window.setId("loopback-only"); window.setStartsAt(now.minusSeconds(1)); window.setEndsAt(now.plusSeconds(90));
        window.setStateDirectory(windowDirectory); window.setSymbols(java.util.Set.of("BTCUSDT", "ETHUSDT"));
        window.setSharedIpWeightAllowancePerMinute(1000); window.setSharedIpWeightLimitPerMinute(6000);
        window.setSharedIpHeadroomConfirmedAt(now.minusSeconds(1)); window.setMinimumFreeBytes(1);
        when(mapper.storageUsage()).thenReturn(new AssetCardMapper.StorageUsage(0, 0, 0, 100, 100, true));
        // Test-only loopback injection. The production Binance-only endpoint validator is not weakened.
        if (server != null) org.springframework.test.util.ReflectionTestUtils.setField(properties, "spotStreamBaseUri", server.uri());
        properties.setEnabled(true); properties.setExternalCallsEnabled(true);
        service.reconcileSubscriptions(List.of("BTCUSDT", "ETHUSDT"));
    }

    private java.net.http.WebSocket currentSocket() {
        return (java.net.http.WebSocket)org.springframework.test.util.ReflectionTestUtils.getField(service, "socket");
    }

    private com.fasterxml.jackson.databind.JsonNode ledger() throws Exception {
        return new ObjectMapper().readTree(java.nio.file.Files.readAllBytes(windowDirectory.resolve("loopback-only.json")));
    }

    private void reconnectWithinExistingWindow() throws Exception {
        Instant after = service.runtimeMetrics().reconnectAfter();
        await(() -> !Instant.now().isBefore(after));
        service.ensureConnected();
    }

    private static void await(java.util.function.BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(6);
        while (!condition.getAsBoolean() && System.nanoTime() < until) Thread.sleep(10);
        assertThat(condition.getAsBoolean()).as("bounded loopback transport condition").isTrue();
    }

    /** Test-classpath only: rerun real loopback scenarios against classes extracted from the candidate standard JAR. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected extracted BOOT-INF/classes and candidate JAR");
        var classes = java.nio.file.Path.of(args[0]).toRealPath();
        var jar = java.nio.file.Path.of(args[1]).toRealPath();
        assertThat(classes.getFileName().toString()).isEqualTo("classes");
        assertThat(classes.getParent().getFileName().toString()).as("Not target/classes").isEqualTo("BOOT-INF");
        try (var archive = new java.util.jar.JarFile(jar.toFile())) {
            for (Class<?> type : List.of(AssetCardMarketDataService.class, AssetCardService.class, AssetCardSnapshot.class)) {
                assertThat(java.nio.file.Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath())
                        .as("Runtime class must originate in the extracted standard JAR").isEqualTo(classes);
                String relative = type.getName().replace('.', '/') + ".class";
                var entry = archive.getJarEntry("BOOT-INF/classes/" + relative);
                assertThat(entry).isNotNull();
                byte[] packaged;
                try (var in = archive.getInputStream(entry)) { packaged = in.readAllBytes(); }
                assertThat(java.nio.file.Files.readAllBytes(classes.resolve(relative))).isEqualTo(packaged);
                System.out.println("STANDARD_JAR_CLASS=" + type.getName() + " SHA256="
                        + java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(packaged)));
            }
        }
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        try (var in = new java.security.DigestInputStream(java.nio.file.Files.newInputStream(jar), digest)) {
            in.transferTo(java.io.OutputStream.nullOutputStream());
        }
        System.out.println("STANDARD_JAR_SHA256=" + java.util.HexFormat.of().formatHex(digest.digest()));
        for (String scenario : List.of(
                "loopbackRuntimeEvidenceIsBoundedAndNeverInventsReceivedFrameTimes",
                "loopbackExpiredCheckAbortsExplicitlyAndRecoversWithoutResettingAllowance",
                "loopbackSilentConnectionRetiresButPingAndOtherAssetTrafficKeepItAlive",
                "loopbackMalformedAndUnsubscribedFramesCannotRenewMarketLiveness",
                "loopbackOpenKlineRenewsMarketLivenessWithoutBarPersistenceOrInference",
                "loopbackRetiredHandshakeCannotInstallAfterWriterRecovery",
                "loopbackHandshakeFailureThenAbnormalCloseRecoverWithBoundedAttempts",
                "loopbackNormalCloseRejoinsDepthAndRejectsLateOldFrames",
                "loopbackTerminalBudgetAndExpiryNeverReconnectOrRefreshPrice",
                "loopbackAbsoluteDeadlineStopsSocketWithoutExtendingTheWindow",
                "loopbackOnlyOneHandshakeMayBePendingAndOldQueuedObservationsCannotReturn",
                "depthEpochMustBeRecheckedInsideTheBookMutationLock",
                "retiredBarIoCannotPublishAnOldEpochOrHoldTheGlobalLifecycleLock",
                "oldObservationFailureCannotInvalidateNewEpochHealth",
                "successfulHandshakeFutureBeforeOnOpenRetainsSingleFlightAndInstallsTheSocket")) {
            var fixture = new AssetCardMarketDataServiceTest();
            var temporary = java.nio.file.Files.createTempDirectory("asset-card-standard-jar-loopback-").toRealPath();
            fixture.windowDirectory = temporary;
            try {
                fixture.explicitFixtureWriterReadiness();
                AssetCardMarketDataServiceTest.class.getDeclaredMethod(scenario).invoke(fixture);
                System.out.println("STANDARD_JAR_SCENARIO=" + scenario + " RESULT=PASS");
            } finally {
                try { fixture.releaseResources(); }
                finally {
                    // Only this freshly-created test fixture is removed; no existing ledger or external data is touched.
                    try (var files = java.nio.file.Files.walk(temporary)) {
                        for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.deleteIfExists(file);
                    }
                }
            }
        }
        System.out.println("STANDARD_JAR_STREAM_RECOVERY=PASS\nREAL_STAGING_ACCEPTANCE=NOT_EXECUTED");
    }

    /** Real RFC6455 loopback transport; never resolves or contacts a public Provider. */
    private static final class LoopbackSpotServer implements AutoCloseable {
        private final java.net.ServerSocket server;
        private final java.util.concurrent.BlockingQueue<Peer> accepted = new java.util.concurrent.LinkedBlockingQueue<>();
        private final List<Peer> peers = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final java.util.concurrent.atomic.AtomicInteger connections = new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "card-loopback-test"); thread.setDaemon(true); return thread;
        });
        LoopbackSpotServer(int firstStatus, boolean holdFirst) throws Exception {
            server = new java.net.ServerSocket(0, 8, java.net.InetAddress.getLoopbackAddress());
            workers.execute(() -> {
                while (!server.isClosed()) try {
                    var socket = server.accept(); int count = connections.incrementAndGet();
                    var peer = new Peer(socket, count == 1 ? firstStatus : 101, count == 1 && holdFirst);
                    peers.add(peer); workers.execute(() -> peer.run(accepted));
                } catch (java.io.IOException closed) { if (!server.isClosed()) throw new IllegalStateException(closed); }
            });
        }
        java.net.URI uri() { return java.net.URI.create("ws://127.0.0.1:" + server.getLocalPort() + "/stream"); }
        Peer next() throws Exception { Peer value = accepted.poll(4, java.util.concurrent.TimeUnit.SECONDS); assertThat(value).isNotNull(); return value; }
        @Override public void close() throws Exception {
            server.close(); for (Peer peer : peers) { peer.releaseHandshake.countDown(); peer.socket.close(); }
            workers.shutdownNow(); assertThat(workers.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
        private static final class Peer {
            private final java.net.Socket socket;
            private final int status;
            private final java.util.concurrent.CountDownLatch releaseHandshake;
            private final java.util.concurrent.atomic.AtomicInteger pongs = new java.util.concurrent.atomic.AtomicInteger();
            Peer(java.net.Socket socket, int status, boolean hold) { this.socket = socket; this.status = status; releaseHandshake = new java.util.concurrent.CountDownLatch(hold ? 1 : 0); }
            void run(java.util.concurrent.BlockingQueue<Peer> accepted) {
                try {
                    var in = socket.getInputStream(); var header = new java.io.ByteArrayOutputStream();
                    while (header.size() < 8192) {
                        int value = in.read(); if (value < 0) return; header.write(value);
                        if (header.toString(java.nio.charset.StandardCharsets.US_ASCII).endsWith("\r\n\r\n")) break;
                    }
                    String request = header.toString(java.nio.charset.StandardCharsets.US_ASCII);
                    String key = request.lines().filter(line -> line.toLowerCase(java.util.Locale.ROOT).startsWith("sec-websocket-key:"))
                            .map(line -> line.substring(line.indexOf(':') + 1).trim()).findFirst().orElseThrow();
                    accepted.add(this); releaseHandshake.await(6, java.util.concurrent.TimeUnit.SECONDS);
                    if (status != 101) {
                        socket.getOutputStream().write(("HTTP/1.1 " + status + " Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                        socket.close(); return;
                    }
                    String response = java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-1")
                            .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
                    socket.getOutputStream().write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + response + "\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                    while (!socket.isClosed()) {
                        int first = in.read(); if (first < 0) break; int length = in.read(); if (length < 0) break;
                        boolean masked = (length & 128) != 0; int size = length & 127;
                        if (size == 126) size = (in.read() << 8) | in.read();
                        if (size > 4096 || size == 127) throw new java.io.IOException("bounded fixture frame required");
                        byte[] mask = masked ? in.readNBytes(4) : new byte[0]; byte[] payload = in.readNBytes(size);
                        if (mask.length == 4) for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
                        if ((first & 15) == 10) pongs.incrementAndGet();
                        if ((first & 15) == 8) break;
                    }
                } catch (Exception expectedClosedPeer) { /* Test assertions inspect observable recovery, not a mocked response. */ }
            }
            void text(String value) throws Exception { frame(1, value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            synchronized void frame(int opcode, byte[] value) throws Exception {
                var out = socket.getOutputStream(); out.write(128 | opcode);
                if (value.length < 126) out.write(value.length); else { out.write(126); out.write(value.length >>> 8); out.write(value.length & 255); }
                out.write(value); out.flush();
            }
        }
    }
}
