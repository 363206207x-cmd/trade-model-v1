package org.example.trademodel.providercall.coinglass;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallAuditLog;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderCircuitBreaker;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.ProviderSingleFlightGuard;
import org.example.trademodel.providercall.SnapshotCacheService;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityDirectory;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderCapabilityState;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoinGlassHttp451CapabilityPropagationTest {

    @Test
    void openInterest451PropagationBlocksSubsequentCall() {
        TestContext context = context();

        ProviderCallResult<?> first = context.openInterest.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "oi-first");
        ProviderCallResult<?> second = context.openInterest.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "oi-second");

        assertRegionRestricted(first);
        assertRegionRestricted(second);
        assertRestricted(context, ProviderDatasetType.COINGLASS_OPEN_INTEREST);
        verifyRegistryPropagation(context, ProviderDatasetType.COINGLASS_OPEN_INTEREST, "oi-first");
        assertThat(context.transport.calls()).isEqualTo(1);
    }

    @Test
    void funding451PropagationBlocksSubsequentCall() {
        TestContext context = context();

        ProviderCallResult<?> first = context.funding.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "funding-first");
        ProviderCallResult<?> second = context.funding.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "funding-second");

        assertRegionRestricted(first);
        assertRegionRestricted(second);
        assertRestricted(context, ProviderDatasetType.COINGLASS_FUNDING);
        verifyRegistryPropagation(context, ProviderDatasetType.COINGLASS_FUNDING, "funding-first");
        assertThat(context.transport.calls()).isEqualTo(1);
    }

    @Test
    void liquidation451PropagationBlocksOnlyExactDataset() {
        TestContext context = context();

        ProviderCallResult<?> first = context.liquidation.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "liquidation-first");
        ProviderCallResult<?> second = context.liquidation.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "liquidation-second");

        assertRegionRestricted(first);
        assertRegionRestricted(second);
        verifyRegistryPropagation(context, ProviderDatasetType.COINGLASS_LIQUIDATION, "liquidation-first");
        assertThat(context.transport.calls()).isEqualTo(1);
        assertThat(context.registry.inspect("COINGLASS", "BTCUSDT", "1M",
                MarketType.PERPETUAL, ContractType.LINEAR,
                ProviderDatasetType.COINGLASS_LIQUIDATION).capabilityState())
                .isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
        assertThat(context.registry.inspect("COINGLASS", "BTCUSDT", "1M",
                MarketType.PERPETUAL, ContractType.LINEAR,
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO).capabilityState())
                .isEqualTo(ProviderCapabilityState.SUPPORTED);
    }

    @Test
    void longShort451PropagationBlocksSubsequentCall() {
        TestContext context = context();

        ProviderCallResult<?> first = context.longShort.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "long-short-first");
        ProviderCallResult<?> second = context.longShort.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "long-short-second");

        assertRegionRestricted(first);
        assertRegionRestricted(second);
        verifyRegistryPropagation(context, ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, "long-short-first");
        assertThat(context.transport.calls()).isEqualTo(1);
        assertThat(context.registry.inspect("COINGLASS", "BTCUSDT", "1M",
                MarketType.PERPETUAL, ContractType.LINEAR,
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO).capabilityState())
                .isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
    }

    @Test
    void regionRestrictionDoesNotRetryAsFiveHundredAndDoesNotBlockOtherDataset() {
        TestContext context = context();

        context.liquidation.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "liquidation-first");
        context.liquidation.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "liquidation-second");
        ProviderCallResult<?> longShort = context.longShort.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "long-short-first");

        assertRegionRestricted(longShort);
        assertThat(context.transport.calls()).isEqualTo(2);
    }

    private static TestContext context() {
        Instant now = Instant.now();
        CanonicalInstrumentId instrument = new CanonicalInstrumentId(
                "BTC", "USDT", MarketType.PERPETUAL, "BINANCE", ContractType.LINEAR);
        ProviderSymbolMappingRegistry mappings = new ProviderSymbolMappingRegistry(List.of(
                new ProviderSymbolMapping("COINGLASS", instrument, "BTCUSDT", true,
                        "COINGLASS_TEST_V1", List.of("5m", "15m", "1h", "4h"), now)));
        MockEnvironment environment = new MockEnvironment()
                .withProperty("trade-model.providers.coinglass.enabled", "true")
                .withProperty("trade-model.providers.coinglass.external-calls-enabled", "true");
        @SuppressWarnings("unchecked")
        ObjectProvider<ProviderCapabilityDirectory> directories = mock(ObjectProvider.class);
        when(directories.orderedStream()).thenReturn(Stream.empty());
        ProviderCapabilityRegistry registry = spy(new ProviderCapabilityRegistry(
                mappings, environment, 3600, directories));

        CoinGlassProperties properties = new CoinGlassProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        properties.setApiKey("test-key");
        properties.setAdvertisedRpm(300);
        properties.setMaxRetry5xx(2);
        Counting451Transport transport = new Counting451Transport();
        CoinGlassV4Client client = new CoinGlassV4Client(properties, transport,
                new CoinGlassRateLimitMetadataParser(), new ObjectMapper(), java.time.Clock.systemUTC());
        CoinGlassSymbolMapper mapper = new CoinGlassSymbolMapper(mappings);
        CoinGlassV4ProviderAdapter adapter = new CoinGlassV4ProviderAdapter(properties, client, mapper,
                new CoinGlassV4ResponseValidator(), new CoinGlassProviderHealthService());

        ProviderCallProperties callProperties = new ProviderCallProperties();
        callProperties.setEnabled(true);
        callProperties.setExternalCallsEnabled(true);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(callProperties);
        budget.register("COINGLASS", 300);
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(callProperties,
                new SnapshotCacheService(), new ProviderSingleFlightGuard(), budget,
                new ProviderCircuitBreaker(callProperties), new ProviderCallAuditLog());
        CoinGlassLiquidationSnapshotService liquidation = new CoinGlassLiquidationSnapshotService(
                coordinator, properties, mapper, adapter, registry);
        CoinGlassLongShortSnapshotService longShort = new CoinGlassLongShortSnapshotService(
                coordinator, properties, mapper, adapter, registry);
        CoinGlassOpenInterestSnapshotService openInterest = new CoinGlassOpenInterestSnapshotService(
                coordinator, properties, mapper, adapter, registry);
        CoinGlassFundingSnapshotService funding = new CoinGlassFundingSnapshotService(
                coordinator, properties, mapper, adapter, registry);
        return new TestContext(registry, transport, openInterest, funding, liquidation, longShort);
    }

    private static void assertRegionRestricted(ProviderCallResult<?> result) {
        assertThat(result.payload()).isNull();
        assertThat(result.metadata().errorCode()).isEqualTo("REGION_RESTRICTED");
        assertThat(result.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.UNAVAILABLE);
    }

    private static void assertRestricted(TestContext context, ProviderDatasetType datasetType) {
        assertThat(context.registry.inspect("COINGLASS", "BTCUSDT", timeframe(datasetType),
                MarketType.PERPETUAL, ContractType.LINEAR, datasetType).capabilityState())
                .isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
    }

    private static void verifyRegistryPropagation(TestContext context,
                                                  ProviderDatasetType datasetType,
                                                  String traceId) {
        verify(context.registry, times(2)).authorize("COINGLASS", "BTCUSDT", timeframe(datasetType),
                MarketType.PERPETUAL, ContractType.LINEAR, datasetType);
        verify(context.registry, times(1)).record(
                argThat(key -> key.datasetType() == datasetType),
                any(org.example.trademodel.providercall.ProviderAdapterResponse.class), eq(traceId));
    }

    private static String timeframe(ProviderDatasetType datasetType) {
        return datasetType == ProviderDatasetType.COINGLASS_OPEN_INTEREST ? "CURRENT" : "1M";
    }

    private static final class Counting451Transport implements CoinGlassV4HttpTransport {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public CoinGlassHttpResponse get(java.net.URI uri, String authHeaderName,
                                         String apiKey, Duration timeout) {
            calls.incrementAndGet();
            return new CoinGlassHttpResponse(451, "{}", Map.of());
        }

        int calls() {
            return calls.get();
        }
    }

    private record TestContext(ProviderCapabilityRegistry registry,
                               Counting451Transport transport,
                               CoinGlassOpenInterestSnapshotService openInterest,
                               CoinGlassFundingSnapshotService funding,
                               CoinGlassLiquidationSnapshotService liquidation,
                               CoinGlassLongShortSnapshotService longShort) {
    }
}
