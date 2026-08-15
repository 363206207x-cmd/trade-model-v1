package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityDirectory;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.example.trademodel.providercall.instrument.ProviderCapabilityState;
import org.example.trademodel.providercall.instrument.ProviderInstrumentCapability;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderCapabilityPreCallGateTest {

    @Test
    void priorReproductionReadsRegistryAndNeverCallsUnsupportedBinance() {
        ProviderCapabilityRegistry registry = spy(registry(List.of(), enabledEnvironment(), List.of(), 3600));
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        RoutedPublicOhlcvProvider router = router(kraken, binance, registry, "binance", "kraken", true);

        PublicOhlcvProviderResult result = router.fetchClosedBars("AAVEUSDT", "5m", 100, "run");

        assertThat(result.batch()).isNull();
        assertThat(result.reasonCode()).isIn("NO_EXACT_PROVIDER_MAPPING", "BINANCE_SYMBOL_NOT_TRADABLE");
        verify(registry).authorize("BINANCE", "AAVEUSDT", "5m", MarketType.SPOT, ContractType.NONE);
        verify(binance, never()).fetchClosedBars(any(), any(), anyInt(), any());
        verify(kraken, never()).fetchClosedBars(any(), any(), anyInt(), any());
    }

    @Test
    void supportedPrimaryIsAuthorizedBeforeExactlyOneMarketCall() {
        ProviderCapabilityRegistry registry = spy(registry(List.of(mapping("KRAKEN", "BTCUSDT", "XBTUSDT")),
                enabledEnvironment(), List.of(), 3600));
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        when(kraken.fetchClosedBars("BTCUSDT", "5m", 100, "run")).thenReturn(ready("KRAKEN"));
        RoutedPublicOhlcvProvider router = router(kraken, binance, registry, "kraken", "binance", true);

        PublicOhlcvProviderResult result = router.fetchClosedBars("BTCUSDT", "5m", 100, "run");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.READY);
        InOrder order = inOrder(registry, kraken);
        order.verify(registry).authorize("KRAKEN", "BTCUSDT", "5m", MarketType.SPOT, ContractType.NONE);
        order.verify(kraken).fetchClosedBars("BTCUSDT", "5m", 100, "run");
        verify(binance, never()).fetchClosedBars(any(), any(), anyInt(), any());
    }

    @Test
    void supportedFallbackRunsOnceOnlyAfterSupportedPrimaryRuntimeFailure() {
        ProviderCapabilityRegistry registry = registry(List.of(
                mapping("KRAKEN", "BTCUSDT", "XBTUSDT"),
                mapping("BINANCE", "BTCUSDT", "BTCUSDT")), enabledEnvironment(), List.of(), 3600);
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        when(kraken.fetchClosedBars("BTCUSDT", "5m", 100, "run")).thenReturn(failed("TIMEOUT"));
        when(binance.fetchClosedBars("BTCUSDT", "5m", 100, "run")).thenReturn(ready("BINANCE_PUBLIC"));
        RoutedPublicOhlcvProvider router = router(kraken, binance, registry, "kraken", "binance", true);

        assertThat(router.fetchClosedBars("BTCUSDT", "5m", 100, "run").sourceState())
                .isEqualTo(OhlcvSourceState.READY);

        verify(kraken, times(1)).fetchClosedBars("BTCUSDT", "5m", 100, "run");
        verify(binance, times(1)).fetchClosedBars("BTCUSDT", "5m", 100, "run");
    }

    @Test
    void unsupportedFallbackHasZeroMarketCalls() {
        ProviderCapabilityRegistry registry = registry(List.of(mapping("KRAKEN", "BTCUSDT", "XBTUSDT")),
                enabledEnvironment(), List.of(), 3600);
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        when(kraken.fetchClosedBars("BTCUSDT", "5m", 100, "run")).thenReturn(failed("TIMEOUT"));
        RoutedPublicOhlcvProvider router = router(kraken, binance, registry, "kraken", "binance", true);

        assertThat(router.fetchClosedBars("BTCUSDT", "5m", 100, "run").reasonCode()).isEqualTo("TIMEOUT");

        verify(binance, never()).fetchClosedBars(any(), any(), anyInt(), any());
    }

    @Test
    void unsupportedTimeframeDisabledAndNotConfiguredAllHaveZeroMarketCalls() {
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);
        ProviderCapabilityRegistry unsupportedTimeframe = registry(
                List.of(mapping("BINANCE", "BTCUSDT", "BTCUSDT")), enabledEnvironment(), List.of(), 3600);
        ProviderCapabilityRegistry disabled = registry(List.of(mapping("BINANCE", "BTCUSDT", "BTCUSDT")),
                new MockEnvironment(), List.of(), 3600);
        MockEnvironment noExternal = new MockEnvironment()
                .withProperty("trade-model.ohlcv.binance.enabled", "true");
        ProviderCapabilityRegistry notConfigured = registry(
                List.of(mapping("BINANCE", "BTCUSDT", "BTCUSDT")), noExternal, List.of(), 3600);

        router(kraken, binance, unsupportedTimeframe, "binance", "kraken", false)
                .fetchClosedBars("BTCUSDT", "1d", 100, "timeframe");
        router(kraken, binance, disabled, "binance", "kraken", false)
                .fetchClosedBars("BTCUSDT", "5m", 100, "disabled");
        router(kraken, binance, notConfigured, "binance", "kraken", false)
                .fetchClosedBars("BTCUSDT", "5m", 100, "not-configured");

        verify(binance, never()).fetchClosedBars(any(), any(), anyInt(), any());
        verify(kraken, never()).fetchClosedBars(any(), any(), anyInt(), any());
    }

    @Test
    void genericProviderCallOptInCannotAuthorizeDisabledOhlcvRoute() {
        MockEnvironment quoteOnly = new MockEnvironment()
                .withProperty("trade-model.provider-call.enabled", "true")
                .withProperty("trade-model.provider-call.external-calls-enabled", "true");
        ProviderCapabilityRegistry registry = registry(
                List.of(mapping("BINANCE", "BTCUSDT", "BTCUSDT")), quoteOnly, List.of(), 3600);
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);
        BinancePublicOhlcvProvider binance = mock(BinancePublicOhlcvProvider.class);

        PublicOhlcvProviderResult result = router(kraken, binance, registry, "binance", "kraken", false)
                .fetchClosedBars("BTCUSDT", "5m", 100, "run");

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.DISABLED);
        verify(binance, never()).fetchClosedBars(any(), any(), anyInt(), any());
        verify(kraken, never()).fetchClosedBars(any(), any(), anyInt(), any());
    }

    @Test
    void regionRestrictionAndSourceUnavailableBlockSubsequentCalls() {
        ProviderCapabilityRegistry regionRegistry = registry(
                List.of(mapping("KRAKEN", "BTCUSDT", "XBTUSDT")), enabledEnvironment(), List.of(), 3600);
        KrakenPublicOhlcvProvider regionKraken = mock(KrakenPublicOhlcvProvider.class);
        when(regionKraken.fetchClosedBars("BTCUSDT", "5m", 100, "one"))
                .thenReturn(failed("REGION_RESTRICTED"));
        RoutedPublicOhlcvProvider regionRouter = router(regionKraken, mock(BinancePublicOhlcvProvider.class),
                regionRegistry, "kraken", "binance", false);

        regionRouter.fetchClosedBars("BTCUSDT", "5m", 100, "one");
        regionRouter.fetchClosedBars("BTCUSDT", "5m", 100, "two");

        verify(regionKraken, times(1)).fetchClosedBars("BTCUSDT", "5m", 100, "one");

        ProviderCapabilityRegistry unavailableRegistry = registry(
                List.of(mapping("KRAKEN", "ETHUSDT", "ETHUSDT")), enabledEnvironment(), List.of(), 3600);
        KrakenPublicOhlcvProvider unavailableKraken = mock(KrakenPublicOhlcvProvider.class);
        when(unavailableKraken.fetchClosedBars("ETHUSDT", "5m", 100, "one"))
                .thenReturn(failed("TIMEOUT"));
        RoutedPublicOhlcvProvider unavailableRouter = router(unavailableKraken,
                mock(BinancePublicOhlcvProvider.class), unavailableRegistry, "kraken", "binance", false);

        unavailableRouter.fetchClosedBars("ETHUSDT", "5m", 100, "one");
        unavailableRouter.fetchClosedBars("ETHUSDT", "5m", 100, "two");

        verify(unavailableKraken, times(1)).fetchClosedBars("ETHUSDT", "5m", 100, "one");
    }

    @Test
    void staleCapabilityUsesDirectoryRevalidationAndNeverMarketProbeOnFailure() {
        ProviderCapabilityDirectory directory = mock(ProviderCapabilityDirectory.class);
        when(directory.provider()).thenReturn("KRAKEN");
        when(directory.verify(any(), any(), any())).thenAnswer(invocation -> {
            CanonicalInstrumentId requested = invocation.getArgument(0);
            Instant now = invocation.getArgument(2);
            return capability(requested, null, ProviderCapabilityState.SOURCE_UNAVAILABLE,
                    "CAPABILITY_DIRECTORY_UNAVAILABLE", now, null);
        });
        ProviderSymbolMapping stale = new ProviderSymbolMapping("KRAKEN",
                new CanonicalInstrumentId("BTC", "USDT", MarketType.SPOT, "KRAKEN", ContractType.NONE),
                "XBTUSDT", true, "STALE", List.of("5m"), Instant.now().minusSeconds(120));
        ProviderCapabilityRegistry registry = registry(List.of(stale), enabledEnvironment(), List.of(directory), 1);
        KrakenPublicOhlcvProvider kraken = mock(KrakenPublicOhlcvProvider.class);

        ProviderInstrumentCapability result = registry.authorize("KRAKEN", "BTCUSDT", "5m",
                MarketType.SPOT, ContractType.NONE);

        assertThat(result.capabilityState()).isEqualTo(ProviderCapabilityState.SOURCE_UNAVAILABLE);
        verify(directory).verify(any(), any(), any());
        verify(kraken, never()).fetchClosedBars(any(), any(), anyInt(), any());
    }

    @Test
    void directoryIdentityMismatchCannotAuthorizeQuoteMarketOrContractSubstitution() {
        ProviderCapabilityDirectory directory = mock(ProviderCapabilityDirectory.class);
        when(directory.provider()).thenReturn("BINANCE");
        when(directory.verify(any(), any(), any())).thenAnswer(invocation -> {
            Instant now = invocation.getArgument(2);
            CanonicalInstrumentId wrong = new CanonicalInstrumentId("BTC", "USDC",
                    MarketType.SPOT, "BINANCE", ContractType.NONE);
            return capability(wrong, "BTCUSDC", ProviderCapabilityState.SUPPORTED, null, now, now);
        });
        ProviderCapabilityRegistry registry = registry(List.of(), enabledEnvironment(), List.of(directory), 3600);

        ProviderInstrumentCapability result = registry.authorize("BINANCE", "BTCUSDT", "5m",
                MarketType.SPOT, ContractType.NONE);

        assertThat(result.capabilityState()).isEqualTo(ProviderCapabilityState.UNSUPPORTED_SYMBOL);
        assertThat(result.failureReason()).isEqualTo("CAPABILITY_EXACT_IDENTITY_MISMATCH");
    }

    @Test
    void directoryMarketTypeMismatchCannotAuthorizeSpotAsPerpetual() {
        ProviderCapabilityDirectory directory = mock(ProviderCapabilityDirectory.class);
        when(directory.provider()).thenReturn("BINANCE");
        when(directory.verify(any(), any(), any())).thenAnswer(invocation -> {
            Instant now = invocation.getArgument(2);
            CanonicalInstrumentId wrong = new CanonicalInstrumentId("BTC", "USDT",
                    MarketType.SPOT, "BINANCE", ContractType.NONE);
            return capability(wrong, "BTCUSDT", ProviderCapabilityState.SUPPORTED, null, now, now);
        });
        ProviderCapabilityRegistry registry = registry(List.of(), enabledEnvironment(), List.of(directory), 3600);

        ProviderInstrumentCapability result = registry.authorize("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.PERPETUAL, ContractType.LINEAR);

        assertThat(result.capabilityState()).isEqualTo(ProviderCapabilityState.UNSUPPORTED_SYMBOL);
        assertThat(result.failureReason()).isEqualTo("CAPABILITY_EXACT_IDENTITY_MISMATCH");
    }

    @Test
    void invalidMarketContractCombinationIsRejectedBeforeDirectoryOrExternalCall() {
        ProviderCapabilityDirectory directory = mock(ProviderCapabilityDirectory.class);
        when(directory.provider()).thenReturn("BINANCE");
        ProviderCapabilityRegistry registry = registry(List.of(), enabledEnvironment(), List.of(directory), 3600);

        assertThatThrownBy(() -> registry.authorize("BINANCE", "BTCUSDT", "GLOBAL",
                MarketType.SPOT, ContractType.LINEAR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spot instruments must use contractType NONE");

        verifyNoInteractions(directory);
    }

    private static RoutedPublicOhlcvProvider router(KrakenPublicOhlcvProvider kraken,
                                                    BinancePublicOhlcvProvider binance,
                                                    ProviderCapabilityRegistry registry,
                                                    String primary,
                                                    String fallback,
                                                    boolean fallbackEnabled) {
        return new RoutedPublicOhlcvProvider(kraken, binance, primary, fallback, fallbackEnabled, registry);
    }

    @SuppressWarnings("unchecked")
    private static ProviderCapabilityRegistry registry(List<ProviderSymbolMapping> mappings,
                                                       MockEnvironment environment,
                                                       List<ProviderCapabilityDirectory> directories,
                                                       long freshnessSeconds) {
        ObjectProvider<ProviderCapabilityDirectory> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(ignored -> directories.stream());
        return new ProviderCapabilityRegistry(new ProviderSymbolMappingRegistry(mappings), environment,
                freshnessSeconds, provider);
    }

    private static MockEnvironment enabledEnvironment() {
        return new MockEnvironment()
                .withProperty("trade-model.ohlcv.kraken.enabled", "true")
                .withProperty("trade-model.ohlcv.kraken.external-calls-enabled", "true")
                .withProperty("trade-model.ohlcv.binance.enabled", "true")
                .withProperty("trade-model.ohlcv.binance.external-calls-enabled", "true")
                .withProperty("trade-model.provider-call.enabled", "true")
                .withProperty("trade-model.provider-call.external-calls-enabled", "true");
    }

    private static ProviderSymbolMapping mapping(String provider, String canonical, String providerSymbol) {
        String base = canonical.substring(0, canonical.length() - 4);
        return new ProviderSymbolMapping(provider,
                new CanonicalInstrumentId(base, "USDT", MarketType.SPOT, provider, ContractType.NONE),
                providerSymbol, true, provider + "_DIRECTORY_V1", List.of("5m", "15m", "1h", "4h"),
                Instant.now());
    }

    private static ProviderInstrumentCapability capability(CanonicalInstrumentId instrument,
                                                           String providerSymbol,
                                                           ProviderCapabilityState state,
                                                           String reason,
                                                           Instant observedAt,
                                                           Instant verifiedAt) {
        return new ProviderInstrumentCapability(instrument.venue(), instrument.canonical(),
                instrument.baseAsset(), instrument.quoteAsset(), instrument.marketType(), instrument.contractType(),
                providerSymbol, List.of("5m", "15m", "1h", "4h"), state,
                instrument.venue() + "_DIRECTORY_V1", verifiedAt, reason, observedAt);
    }

    private static PublicOhlcvProviderResult failed(String reason) {
        return new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, reason, null);
    }

    private static PublicOhlcvProviderResult ready(String provider) {
        OhlcvIngestionBatch batch = new OhlcvIngestionBatch(provider, "SPOT", "/fixture",
                OhlcvSourceState.READY, Instant.now(), provider + "_V1", 1,
                "trace", "run", List.of());
        return new PublicOhlcvProviderResult(OhlcvSourceState.READY, null, batch);
    }
}
