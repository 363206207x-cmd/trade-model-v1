package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.market.client.OpenInterestClient;
import org.example.trademodel.market.client.PerpFundingRateClient;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.CoordinatedProviderSnapshotQueryService;
import org.example.trademodel.providercall.CoordinatedProviderSnapshotRefreshService;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderCallAuditLog;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderCircuitBreaker;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.ProviderRequestKey;
import org.example.trademodel.providercall.ProviderRequestKeyFactory;
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
import org.example.trademodel.providercall.snapshot.BinanceDerivativesSnapshotService;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BinanceHttp451CapabilityPropagationTest {

    @Test
    void currentPrice451WritesRegionRestricted() throws Exception {
        TestContext context = context();
        HttpClient http = http451();
        MarketPriceSnapshotService service = priceService(
                context, new BinanceMarketQuoteClient(new ObjectMapper(), http));

        ProviderCallResult<?> result = service.get("BTCUSDT", AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "price-451-first");

        assertRegionRestricted(result);
        assertThat(context.registry.inspect("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.SPOT, ContractType.NONE, ProviderDatasetType.PRICE).capabilityState())
                .isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
        verify(context.registry, times(1)).record(
                argThat(key -> key.datasetType() == ProviderDatasetType.PRICE),
                any(ProviderAdapterResponse.class), eq("price-451-first"));
        verify(http, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void currentPrice451BlocksSubsequentCall() throws Exception {
        TestContext context = context();
        HttpClient http = http451();
        MarketPriceSnapshotService service = priceService(
                context, new BinanceMarketQuoteClient(new ObjectMapper(), http));

        service.get("BTCUSDT", AssetPriority.P0_POSITION, Duration.ofSeconds(5), "price-451-first");
        ProviderCallResult<?> second = service.get("BTCUSDT", AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "price-451-second");

        assertRegionRestricted(second);
        verify(context.registry, times(2)).authorize("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.SPOT, ContractType.NONE, ProviderDatasetType.PRICE);
        verify(http, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void funding451WritesRegionRestrictedAndBlocksSubsequentCall() throws Exception {
        TestContext context = context();
        HttpClient fundingHttp = http451();
        OpenInterestClient openInterest = mock(OpenInterestClient.class);
        BinanceDerivativesSnapshotService service = derivativesService(context,
                new BinanceUsdtMPerpFundingClient(new ObjectMapper(), fundingHttp), openInterest);

        ProviderCallResult<?> first = service.get("BTCUSDT", AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "funding-451-first");
        ProviderCallResult<?> second = service.get("BTCUSDT", AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "funding-451-second");

        assertRegionRestricted(first);
        assertRegionRestricted(second);
        assertThat(context.registry.inspect("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR, ProviderDatasetType.FUNDING).capabilityState())
                .isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
        assertThat(first.payload()).isNull();
        verify(context.registry, times(2)).authorize("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR, ProviderDatasetType.FUNDING);
        verify(context.registry, times(1)).record(
                argThat(key -> key.datasetType() == ProviderDatasetType.FUNDING),
                any(ProviderAdapterResponse.class), eq("funding-451-first"));
        verify(fundingHttp, times(1)).send(any(HttpRequest.class), any());
        verifyNoInteractions(openInterest);
    }

    @Test
    void openInterest451WritesRegionRestrictedAndBlocksSubsequentCall() throws Exception {
        TestContext context = context();
        PerpFundingRateClient funding = mock(PerpFundingRateClient.class);
        when(funding.fetchLastFundingRateResult("BTCUSDT"))
                .thenReturn(ProviderAdapterResponse.ready(new BigDecimal("0.0001"), Instant.now()));
        HttpClient openInterestHttp = http451();
        BinanceDerivativesSnapshotService service = derivativesService(context, funding,
                new BinanceUsdtMOpenInterestClient(new ObjectMapper(), openInterestHttp));

        ProviderCallResult<?> first = service.get("BTCUSDT", AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "oi-451-first");
        ProviderCallResult<?> second = service.get("BTCUSDT", AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "oi-451-second");

        assertRegionRestricted(first);
        assertRegionRestricted(second);
        assertThat(context.registry.inspect("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR, ProviderDatasetType.OPEN_INTEREST).capabilityState())
                .isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
        assertThat(first.payload()).isNull();
        verify(context.registry, times(2)).authorize("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR, ProviderDatasetType.OPEN_INTEREST);
        verify(context.registry, times(1)).record(
                argThat(key -> key.datasetType() == ProviderDatasetType.OPEN_INTEREST),
                any(ProviderAdapterResponse.class), eq("oi-451-first"));
        verify(openInterestHttp, times(1)).send(any(HttpRequest.class), any());
        verify(funding, times(1)).fetchLastFundingRateResult("BTCUSDT");
        verify(funding, never()).fetchLastFundingRate("BTCUSDT");
    }

    @Test
    void regionRestrictionCapabilityScopeIsDatasetSpecific() {
        TestContext context = context();
        ProviderRequestKeyFactory factory = new ProviderRequestKeyFactory(context.mappings);
        ProviderSymbolMapping perpetual = context.mappings.resolve(
                "BINANCE", "BTCUSDT", MarketType.PERPETUAL);
        ProviderRequestKey funding = factory.create("BINANCE", ProviderDatasetType.FUNDING,
                perpetual, "GLOBAL", Duration.ofSeconds(5), Instant.now());

        context.registry.record(funding,
                ProviderAdapterResponse.failed(org.example.trademodel.providercall.UnifiedSourceStatus.ERROR,
                        451, "REGION_RESTRICTED", null), "scope-trace");

        assertThat(context.registry.inspect("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR, ProviderDatasetType.FUNDING).capabilityState())
                .isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
        assertThat(context.registry.inspect("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR, ProviderDatasetType.OPEN_INTEREST).capabilityState())
                .isEqualTo(ProviderCapabilityState.SUPPORTED);
        assertThat(context.registry.inspect("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR, ProviderDatasetType.PRICE).capabilityState())
                .isEqualTo(ProviderCapabilityState.SUPPORTED);
    }

    private static MarketPriceSnapshotService priceService(TestContext context,
                                                           BinanceMarketQuoteClient client) {
        return new MarketPriceSnapshotService(
                new CoordinatedProviderSnapshotQueryService(context.coordinator),
                new CoordinatedProviderSnapshotRefreshService(context.coordinator),
                client, context.mappings, new ProviderRequestKeyFactory(context.mappings),
                context.registry, Clock.systemUTC());
    }

    private static BinanceDerivativesSnapshotService derivativesService(
            TestContext context, PerpFundingRateClient funding, OpenInterestClient openInterest) {
        return new BinanceDerivativesSnapshotService(context.coordinator, funding, openInterest,
                context.mappings, new ProviderRequestKeyFactory(context.mappings),
                context.registry, Clock.systemUTC());
    }

    private static TestContext context() {
        Instant now = Instant.now();
        ProviderSymbolMappingRegistry mappings = new ProviderSymbolMappingRegistry(List.of(
                mapping(MarketType.SPOT, ContractType.NONE, now),
                mapping(MarketType.PERPETUAL, ContractType.LINEAR, now)));
        MockEnvironment environment = new MockEnvironment()
                .withProperty("trade-model.provider-call.enabled", "true")
                .withProperty("trade-model.provider-call.external-calls-enabled", "true")
                .withProperty("trade-model.ohlcv.binance.enabled", "true")
                .withProperty("trade-model.ohlcv.binance.external-calls-enabled", "true");
        @SuppressWarnings("unchecked")
        ObjectProvider<ProviderCapabilityDirectory> directories = mock(ObjectProvider.class);
        when(directories.orderedStream()).thenReturn(Stream.empty());
        ProviderCapabilityRegistry registry = org.mockito.Mockito.spy(new ProviderCapabilityRegistry(
                mappings, environment, 3600, directories));

        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties);
        budget.register("BINANCE", 1200);
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(properties,
                new SnapshotCacheService(), new ProviderSingleFlightGuard(), budget,
                new ProviderCircuitBreaker(properties), new ProviderCallAuditLog());
        return new TestContext(mappings, registry, coordinator);
    }

    private static ProviderSymbolMapping mapping(MarketType marketType,
                                                 ContractType contractType,
                                                 Instant verifiedAt) {
        CanonicalInstrumentId instrument = new CanonicalInstrumentId(
                "BTC", "USDT", marketType, "BINANCE", contractType);
        return new ProviderSymbolMapping("BINANCE", instrument, "BTCUSDT", true,
                "BINANCE_TEST_V1", List.of("5m", "15m", "1h", "4h"), verifiedAt);
    }

    @SuppressWarnings("unchecked")
    private static HttpClient http451() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(451);
        when(response.body()).thenReturn("{\"code\":451}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return client;
    }

    private static void assertRegionRestricted(ProviderCallResult<?> result) {
        assertThat(result.payload()).isNull();
        assertThat(result.metadata().errorCode()).isEqualTo("REGION_RESTRICTED");
        assertThat(result.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.UNAVAILABLE);
    }

    private record TestContext(ProviderSymbolMappingRegistry mappings,
                               ProviderCapabilityRegistry registry,
                               ProviderCallCoordinator coordinator) {
    }
}
