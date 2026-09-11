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
}
