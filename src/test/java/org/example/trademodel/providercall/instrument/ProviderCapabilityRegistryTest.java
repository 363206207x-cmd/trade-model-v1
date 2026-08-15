package org.example.trademodel.providercall.instrument;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderCapabilityRegistryTest {
    private static final Instant NOW = Instant.parse("2026-08-15T10:00:00Z");

    @Test
    void exactAdaUsdtMappingIsSupportedWithoutQuoteSubstitution() {
        ProviderCapabilityRegistry registry = registry(NOW.minusSeconds(60), 300, "true");

        ProviderInstrumentCapability capability = registry.best("ADA/USDT", "5m");

        assertThat(capability.capabilityState()).isEqualTo(ProviderCapabilityState.SUPPORTED);
        assertThat(capability.canonicalAssetId()).contains("ADA", "USDT", "SPOT", "BINANCE");
        assertThat(capability.providerSymbol()).isEqualTo("ADAUSDT");
        assertThat(capability.sourceVersion()).isEqualTo("BINANCE_SPOT_EXCHANGE_INFO_V1");
    }

    @Test
    void unsupportedTimeframeAndStaleCapabilityFailClosed() {
        ProviderCapabilityRegistry fresh = registry(NOW.minusSeconds(60), 300, "true");
        ProviderCapabilityRegistry stale = registry(NOW.minusSeconds(301), 300, "true");

        assertThat(fresh.best("ADAUSDT", "1d").capabilityState())
                .isEqualTo(ProviderCapabilityState.UNSUPPORTED_TIMEFRAME);
        assertThat(stale.best("ADAUSDT", "5m").capabilityState())
                .isEqualTo(ProviderCapabilityState.STALE_CAPABILITY);
    }

    @Test
    void regionRestrictedRuntimeObservationOverridesStaticSupportForThatProviderAndTimeframe() {
        ProviderCapabilityRegistry registry = registry(NOW.minusSeconds(60), 300, "true");
        registry.recordOhlcv("BINANCE", "ADAUSDT", "5m", "ADAUSDT",
                "BINANCE_RUNTIME_V1",
                new PublicOhlcvProviderResult(OhlcvSourceState.ERROR, "REGION_RESTRICTED", null));

        ProviderInstrumentCapability capability = registry.best("ADAUSDT", "5m");

        assertThat(capability.capabilityState()).isEqualTo(ProviderCapabilityState.REGION_RESTRICTED);
        assertThat(capability.failureReason()).isEqualTo("REGION_RESTRICTED");
        assertThat(capability.observedAt()).isEqualTo(NOW);
    }

    @Test
    void missingExactMappingIsUnsupportedRatherThanSynthesized() {
        ProviderCapabilityRegistry registry = registry(NOW.minusSeconds(60), 300, "true");

        ProviderInstrumentCapability capability = registry.best("AAVEUSDT", "5m");

        assertThat(capability.capabilityState()).isEqualTo(ProviderCapabilityState.UNSUPPORTED_SYMBOL);
        assertThat(capability.failureReason()).isEqualTo("NO_EXACT_PROVIDER_MAPPING");
    }

    private static ProviderCapabilityRegistry registry(Instant verifiedAt, long freshnessSeconds, String enabled) {
        ProviderSymbolMapping mapping = new ProviderSymbolMapping(
                "BINANCE",
                new CanonicalInstrumentId("ADA", "USDT", MarketType.SPOT, "BINANCE", ContractType.NONE),
                "ADAUSDT",
                true,
                "BINANCE_SPOT_EXCHANGE_INFO_V1",
                List.of("5m", "15m", "1h", "4h"),
                verifiedAt);
        ProviderSymbolMappingRegistry mappings = new ProviderSymbolMappingRegistry(List.of(mapping));
        MockEnvironment environment = new MockEnvironment()
                .withProperty("trade-model.ohlcv.binance.enabled", enabled)
                .withProperty("trade-model.ohlcv.binance.external-calls-enabled", enabled);
        return new ProviderCapabilityRegistry(mappings, environment, freshnessSeconds,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
