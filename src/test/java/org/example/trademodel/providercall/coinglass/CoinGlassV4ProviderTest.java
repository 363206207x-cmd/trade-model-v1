package org.example.trademodel.providercall.coinglass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderCallAuditLog;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCallRequest;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderCircuitBreaker;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.ProviderRequestKey;
import org.example.trademodel.providercall.ProviderSingleFlightGuard;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotCacheService;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CoinGlassV4ProviderTest {
    private static final Instant NOW = Instant.parse("2026-07-10T10:00:00Z");

    @Test
    void coinglassDisabledReturnsNotConfigured() {
        TestContext context = context("open-interest-success.json");
        context.coinGlassProperties.setEnabled(false);

        ProviderCallResult<CoinGlassOpenInterestSnapshot> result = context.oiService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-disabled");

        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
        assertThat(result.metadata().errorCode()).isEqualTo("COINGLASS_PROVIDER_NOT_CONFIGURED");
        assertThat(context.transport.calls).hasValue(0);
    }

    @Test
    void missingApiKeyReturnsNotConfigured() {
        TestContext context = context("open-interest-success.json");
        context.coinGlassProperties.setApiKey("");

        ProviderCallResult<CoinGlassOpenInterestSnapshot> result = context.oiService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-no-key");

        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
        assertThat(result.metadata().errorCode()).isEqualTo("COINGLASS_API_KEY_MISSING");
        assertThat(context.transport.calls).hasValue(0);
    }

    @Test
    void apiKeyIsNeverLoggedOrSerialized() throws Exception {
        TestContext context = context("open-interest-success.json");
        String serialized = new ObjectMapper().writeValueAsString(context.coinGlassProperties);
        context.oiService.get("BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-secret");

        assertThat(serialized).doesNotContain("fixture-secret-key");
        assertThat(context.transport.lastUri.toString()).doesNotContain("fixture-secret-key");
        assertThat(context.coinGlassProperties.toString()).doesNotContain("fixture-secret-key");
    }

    @Test
    void officialV4OpenInterestResponseMapsCorrectly() {
        ProviderAdapterResponse<CoinGlassOpenInterestSnapshot> response =
                context("open-interest-success.json").adapter.fetchOpenInterest("BTCUSDT");

        assertThat(response.ready()).isTrue();
        assertThat(response.payload().openInterestUsd()).isEqualByComparingTo("1000.00");
        assertThat(response.payload().openInterestChange1m()).isNull();
        assertThat(response.payload().openInterestChange5m()).isEqualByComparingTo("0.34");
        assertThat(response.payload().exchangeConcentrationScore()).isEqualByComparingTo("0.4");
    }

    @Test
    void officialV4FundingResponseMapsCorrectly() {
        ProviderAdapterResponse<CoinGlassFundingSnapshot> response =
                context("funding-success.json").adapter.fetchFunding("BTCUSDT");

        assertThat(response.ready()).isTrue();
        assertThat(response.payload().weightedFundingRate()).isEqualByComparingTo("0.009229");
        assertThat(response.providerDataTime()).isEqualTo(Instant.parse("2026-07-10T09:59:00Z"));
    }

    @Test
    void officialV4LiquidationResponseMapsCorrectly() {
        ProviderAdapterResponse<CoinGlassLiquidationSnapshot> response =
                context("liquidation-success.json").adapter.fetchLiquidation("BTCUSDT");

        assertThat(response.ready()).isTrue();
        assertThat(response.payload().longLiquidationUsd1m()).isEqualByComparingTo("10");
        assertThat(response.payload().longLiquidationUsd5m()).isEqualByComparingTo("60");
        assertThat(response.payload().longLiquidationUsd15m()).isNull();
        assertThat(response.payload().shortLiquidationUsd5m()).isEqualByComparingTo("110");
    }

    @Test
    void officialV4LongShortResponseMapsCorrectly() {
        ProviderAdapterResponse<CoinGlassLongShortSnapshot> response =
                context("long-short-success.json").adapter.fetchLongShortRatio("BTCUSDT");

        assertThat(response.ready()).isTrue();
        assertThat(response.payload().longShortRatio()).isEqualByComparingTo("2.83");
        assertThat(response.payload().longShortRatioSource()).isEqualTo("BINANCE_GLOBAL_ACCOUNT_RATIO");
    }

    @Test
    void providerHealthRecordsSanitizedRateMetadata() {
        TestContext context = context("open-interest-success.json");
        context.adapter.fetchOpenInterest("BTCUSDT");
        CoinGlassProviderHealthService.CoinGlassEndpointHealth health =
                context.health.get(CoinGlassV4ResponseValidator.OI_CAPABILITY);
        assertThat(health.status()).isEqualTo(UnifiedSourceStatus.READY);
        assertThat(health.rateLimit().apiKeyMaxLimit()).isEqualTo(300);
        assertThat(health.rateLimit().apiKeyUseLimit()).isEqualTo(1);
        assertThat(health.toString()).doesNotContain("fixture-secret-key");
    }

    @Test
    void symbolMappingSupportsSixCoreAssets() {
        CoinGlassSymbolMapper mapper = new CoinGlassSymbolMapper();
        assertThat(List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT"))
                .allSatisfy(symbol -> {
                    assertThat(mapper.map(symbol).pairSymbol()).isEqualTo(symbol);
                    assertThat(mapper.map(symbol).coinSymbol()).isEqualTo(symbol.substring(0, symbol.length() - 4));
                });
    }

    @Test
    void invalidSymbolFailsClosed() {
        TestContext context = context("open-interest-success.json");

        ProviderCallResult<CoinGlassOpenInterestSnapshot> result = context.oiService.get(
                "../BTC", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-invalid");

        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.ERROR);
        assertThat(result.metadata().errorCode()).isEqualTo("COINGLASS_SYMBOL_UNSUPPORTED");
        assertThat(context.transport.calls).hasValue(0);
    }

    @Test
    void timestampNormalizesToUtc() {
        ProviderAdapterResponse<CoinGlassFundingSnapshot> response =
                context("funding-success.json").adapter.fetchFunding("BTCUSDT");

        assertThat(response.providerDataTime()).isEqualTo(Instant.parse("2026-07-10T09:59:00Z"));
    }

    @Test
    void missingNumericFieldRemainsNull() throws Exception {
        JsonNode data = fixture("open-interest-success.json").get("data").deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.get(0)).remove("open_interest_change_percent_5m");

        CoinGlassMappingResult<CoinGlassOpenInterestSnapshot> result = new CoinGlassV4ResponseValidator()
                .openInterest(data, new CoinGlassSymbolMapper().map("BTCUSDT"), NOW);

        assertThat(result.payload().openInterestChange5m()).isNull();
    }

    @Test
    void emptyOiDoesNotBecomeZero() {
        ProviderAdapterResponse<CoinGlassOpenInterestSnapshot> response =
                context("empty-data.json").adapter.fetchOpenInterest("BTCUSDT");
        assertThat(response.sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
        assertThat(response.payload()).isNull();
    }

    @Test
    void emptyFundingDoesNotBecomeLowRisk() {
        ProviderAdapterResponse<CoinGlassFundingSnapshot> response =
                context("empty-data.json").adapter.fetchFunding("BTCUSDT");
        assertThat(response.sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
        assertThat(response.payload()).isNull();
    }

    @Test
    void emptyLiquidationDoesNotBecomeZero() {
        ProviderAdapterResponse<CoinGlassLiquidationSnapshot> response =
                context("empty-data.json").adapter.fetchLiquidation("BTCUSDT");
        assertThat(response.sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
        assertThat(response.payload()).isNull();
    }

    @Test
    void emptyDatasetStateUsesSharedCache() {
        TestContext context = context("empty-data.json");
        ProviderCallResult<CoinGlassOpenInterestSnapshot> first = context.oiService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-empty-cache");
        ProviderCallResult<CoinGlassOpenInterestSnapshot> second = context.oiService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-empty-cache");
        assertThat(first.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
        assertThat(second.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.EMPTY_CONFIRMED);
        assertThat(second.metadata().cacheHit()).isTrue();
        assertThat(context.transport.calls).hasValue(1);
    }

    @Test
    void partialDatasetsProduceDegradedSnapshot() {
        TestContext context = context("open-interest-success.json");
        ProviderCallResult<CoinGlassOpenInterestSnapshot> oi = readyOi("trace-partial", NOW);
        ProviderCallResult<CoinGlassFundingSnapshot> missing = failed(
                ProviderDatasetType.COINGLASS_FUNDING, UnifiedSourceStatus.ERROR, "FUNDING_FAILED", "trace-partial");

        ProviderCallResult<DerivativesRiskSnapshot> result = context.assembler.assemble("BTCUSDT", "trace-partial",
                oi, missing, null, null);

        assertThat(result.payload()).isNotNull();
        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.DEGRADED);
        assertThat(result.payload().availableDatasets()).contains("COINGLASS_OPEN_INTEREST");
        assertThat(result.payload().missingDatasets()).contains("COINGLASS_FUNDING");
    }

    @Test
    void allDatasetsProduceReadySnapshot() {
        TestContext context = context("open-interest-success.json", "funding-success.json",
                "liquidation-success.json", "long-short-success.json");

        ProviderCallResult<DerivativesRiskSnapshot> result = context.derivativesService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-all");

        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.READY);
        assertThat(result.payload().availableDatasets()).hasSize(4);
        assertThat(result.payload().fundingExtremityScore()).isNull();
        assertThat(result.payload().liquidationSpikeScore()).isNull();
    }

    @Test
    void staleProviderTimeProducesStaleSnapshot() {
        TestContext context = context("open-interest-success.json", "stale-timestamp.json",
                "liquidation-success.json", "long-short-success.json");

        ProviderCallResult<DerivativesRiskSnapshot> result = context.derivativesService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-stale");

        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.STALE);
        assertThat(result.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.STALE_READABLE);
    }

    @Test
    void malformedResponseProducesError() {
        ProviderAdapterResponse<CoinGlassOpenInterestSnapshot> response =
                context("malformed-data.json").adapter.fetchOpenInterest("BTCUSDT");
        assertThat(response.sourceStatus()).isEqualTo(UnifiedSourceStatus.ERROR);
        assertThat(response.reasonCode()).endsWith(":PROVIDER_DATA_MALFORMED");
    }

    @Test
    void authenticationFailureDoesNotRetry() {
        TestContext context = context(response(401, "error-response.json", Map.of()));
        ProviderCallResult<CoinGlassOpenInterestSnapshot> result = context.oiService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-auth");
        assertThat(context.transport.calls).hasValue(1);
        assertThat(result.metadata().errorCode()).contains("AUTHENTICATION_FAILED");
    }

    @Test
    void rateLimitRespectsRetryAfter() {
        TestContext context = context(response(429, "rate-limited.json", Map.of("Retry-After", List.of("30"))));
        ProviderCallResult<CoinGlassOpenInterestSnapshot> first = context.oiService.get(
                "BTCUSDT", AssetPriority.P2_CANDIDATE, Duration.ofSeconds(60), "trace-rate-1");
        ProviderCallResult<CoinGlassOpenInterestSnapshot> second = context.oiService.get(
                "BTCUSDT", AssetPriority.P2_CANDIDATE, Duration.ofSeconds(60), "trace-rate-2");
        assertThat(first.metadata().errorCode()).contains("RATE_LIMITED");
        assertThat(second.metadata().errorCode()).isEqualTo("PROVIDER_BUDGET_REJECTED");
        assertThat(context.transport.calls).hasValue(1);
    }

    @Test
    void fiveHundredRetriesAreBounded() {
        TestContext context = context(response(500, "error-response.json", Map.of()));
        context.oiService.get("BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-500");
        assertThat(context.transport.calls).hasValue(3);
    }

    @Test
    void timeoutRetryIsBounded() {
        TestContext context = context("open-interest-success.json");
        AtomicInteger calls = new AtomicInteger();
        ProviderCallRequest<String> request = new ProviderCallRequest<>(
                org.example.trademodel.providercall.ProviderCallTestFixtures.key("COINGLASS",
                        ProviderDatasetType.COINGLASS_OPEN_INTEREST, "BTCUSDT", "CURRENT", "TIMEOUT"),
                AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), Duration.ofSeconds(180),
                Duration.ofMillis(20), "trace-timeout", () -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return ProviderAdapterResponse.ready("late", NOW);
        });

        ProviderCallResult<String> result = context.coordinator.execute(request);

        assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_TIMEOUT");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (calls.get() < 2 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(calls).hasValue(2);
    }

    @Test
    void sameSymbolDatasetUsesSharedCache() {
        TestContext context = context("open-interest-success.json");
        context.oiService.get("BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-cache");
        ProviderCallResult<CoinGlassOpenInterestSnapshot> second = context.oiService.get(
                "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-cache");
        assertThat(context.transport.calls).hasValue(1);
        assertThat(second.metadata().cacheHit()).isTrue();
    }

    @Test
    void sameSymbolDatasetUsesSingleFlight() throws Exception {
        TestContext context = context("open-interest-success.json");
        context.transport.delayMillis = 100;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> context.oiService.get(
                    "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-flight"));
            Future<?> second = executor.submit(() -> context.oiService.get(
                    "BTCUSDT", AssetPriority.P1_WATCHLIST, Duration.ofSeconds(60), "trace-flight"));
            first.get();
            second.get();
            assertThat(context.transport.calls).hasValue(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private static TestContext context(String... fixtures) {
        List<CoinGlassV4HttpTransport.CoinGlassHttpResponse> responses = new ArrayList<>();
        for (String fixture : fixtures) responses.add(response(200, fixture, Map.of(
                "API-KEY-MAX-LIMIT", List.of("300"), "API-KEY-USE-LIMIT", List.of("1"))));
        return context(responses.toArray(CoinGlassV4HttpTransport.CoinGlassHttpResponse[]::new));
    }

    private static TestContext context(CoinGlassV4HttpTransport.CoinGlassHttpResponse... responses) {
        MutableClock clock = new MutableClock(NOW);
        CoinGlassProperties coinGlassProperties = new CoinGlassProperties();
        coinGlassProperties.setEnabled(true);
        coinGlassProperties.setExternalCallsEnabled(true);
        coinGlassProperties.setApiKey("fixture-secret-key");
        FakeTransport transport = new FakeTransport(List.of(responses));
        CoinGlassRateLimitMetadataParser rateParser = new CoinGlassRateLimitMetadataParser();
        CoinGlassV4Client client = new CoinGlassV4Client(coinGlassProperties, transport, rateParser,
                new ObjectMapper(), clock);
        CoinGlassProviderHealthService health = new CoinGlassProviderHealthService();
        CoinGlassV4ProviderAdapter adapter = new CoinGlassV4ProviderAdapter(coinGlassProperties, client,
                new CoinGlassSymbolMapper(), new CoinGlassV4ResponseValidator(), health);

        ProviderCallProperties coordinatorProperties = new ProviderCallProperties();
        coordinatorProperties.setEnabled(true);
        coordinatorProperties.setExternalCallsEnabled(true);
        ProviderSingleFlightGuard singleFlight = new ProviderSingleFlightGuard();
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(coordinatorProperties, clock);
        budget.register("COINGLASS", 300);
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(coordinatorProperties,
                new SnapshotCacheService(), singleFlight, budget, new ProviderCircuitBreaker(3, 60, clock),
                new ProviderCallAuditLog(), clock);
        CoinGlassSymbolMapper mapper = new CoinGlassSymbolMapper();
        CoinGlassOpenInterestSnapshotService oiService = new CoinGlassOpenInterestSnapshotService(
                coordinator, coinGlassProperties, mapper, adapter);
        CoinGlassFundingSnapshotService fundingService = new CoinGlassFundingSnapshotService(
                coordinator, coinGlassProperties, mapper, adapter);
        CoinGlassLiquidationSnapshotService liquidationService = new CoinGlassLiquidationSnapshotService(
                coordinator, coinGlassProperties, mapper, adapter);
        CoinGlassLongShortSnapshotService longShortService = new CoinGlassLongShortSnapshotService(
                coordinator, coinGlassProperties, mapper, adapter);
        CoinGlassDerivativesSnapshotAssembler assembler = new CoinGlassDerivativesSnapshotAssembler(
                coinGlassProperties, clock);
        CoinGlassDerivativesSnapshotService derivativesService = new CoinGlassDerivativesSnapshotService(
                coinGlassProperties, oiService, fundingService, liquidationService, longShortService, assembler);
        return new TestContext(coinGlassProperties, transport, coordinator, adapter, health, oiService,
                assembler, derivativesService);
    }

    private static CoinGlassV4HttpTransport.CoinGlassHttpResponse response(
            int status, String fixture, Map<String, List<String>> headers) {
        try (InputStream stream = CoinGlassV4ProviderTest.class.getResourceAsStream(
                "/provider/coinglass/v4/" + fixture)) {
            if (stream == null) throw new IllegalArgumentException("fixture missing: " + fixture);
            return new CoinGlassV4HttpTransport.CoinGlassHttpResponse(status,
                    new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), headers);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static JsonNode fixture(String fixture) throws Exception {
        try (InputStream stream = CoinGlassV4ProviderTest.class.getResourceAsStream(
                "/provider/coinglass/v4/" + fixture)) {
            return new ObjectMapper().readTree(stream);
        }
    }

    private static ProviderCallResult<CoinGlassOpenInterestSnapshot> readyOi(String trace, Instant time) {
        CoinGlassOpenInterestSnapshot payload = new CoinGlassOpenInterestSnapshot("BTCUSDT",
                BigDecimal.TEN, null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                new BigDecimal("0.4"), List.of("Binance"), time, Map.of("openInterestUsd", "fixture"));
        return ready(ProviderDatasetType.COINGLASS_OPEN_INTEREST, payload, trace, time);
    }

    private static <T> ProviderCallResult<T> ready(
            ProviderDatasetType type, T payload, String trace, Instant providerTime) {
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("COINGLASS", type, "BTCUSDT", "1M",
                providerTime, NOW, NOW.plusSeconds(60), UnifiedSourceStatus.READY,
                SnapshotFreshnessStatus.FRESH, trace, "fixture-key", false, false, null, List.of());
        return new ProviderCallResult<>(payload, metadata, null);
    }

    private static <T> ProviderCallResult<T> failed(
            ProviderDatasetType type, UnifiedSourceStatus status, String reason, String trace) {
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("COINGLASS", type, "BTCUSDT", "1M",
                null, NOW, NOW, status, SnapshotFreshnessStatus.UNAVAILABLE, trace,
                "fixture-key", false, false, reason, List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }

    private record TestContext(
            CoinGlassProperties coinGlassProperties,
            FakeTransport transport,
            ProviderCallCoordinator coordinator,
            CoinGlassV4ProviderAdapter adapter,
            CoinGlassProviderHealthService health,
            CoinGlassOpenInterestSnapshotService oiService,
            CoinGlassDerivativesSnapshotAssembler assembler,
            CoinGlassDerivativesSnapshotService derivativesService
    ) {
    }

    private static final class FakeTransport implements CoinGlassV4HttpTransport {
        private final Queue<CoinGlassHttpResponse> responses;
        private final AtomicInteger calls = new AtomicInteger();
        private volatile URI lastUri;
        private volatile long delayMillis;

        private FakeTransport(List<CoinGlassHttpResponse> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public synchronized CoinGlassHttpResponse get(
                URI uri, String authHeaderName, String apiKey, Duration timeout) throws Exception {
            calls.incrementAndGet();
            lastUri = uri;
            if (delayMillis > 0) Thread.sleep(delayMillis);
            if (responses.isEmpty()) throw new IllegalStateException("fixture response missing");
            return responses.size() == 1 ? responses.peek() : responses.remove();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
