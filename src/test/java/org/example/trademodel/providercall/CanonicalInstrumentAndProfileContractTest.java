package org.example.trademodel.providercall;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.profile.FrequencyMatrixVersionService;
import org.example.trademodel.providercall.profile.ProviderCallProfileResolver;
import org.example.trademodel.providercall.profile.ProviderDueTimePolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanonicalInstrumentAndProfileContractTest {
    private static final Instant AS_OF = Instant.parse("2026-07-19T10:00:00Z");

    @Test
    void providerSpellingsResolveToOneCanonicalInstrument() {
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");

        CanonicalInstrumentId compact = registry.resolve("BINANCE", "BTCUSDT", MarketType.PERPETUAL)
                .canonicalInstrumentId();
        CanonicalInstrumentId slash = registry.resolve("BINANCE", "BTC/USDT", MarketType.PERPETUAL)
                .canonicalInstrumentId();
        CanonicalInstrumentId dash = registry.resolve("BINANCE", "BTC-USDT", MarketType.PERPETUAL)
                .canonicalInstrumentId();

        assertThat(compact).isEqualTo(slash).isEqualTo(dash);
    }

    @Test
    void spotAndPerpetualNeverShareCanonicalIdentity() {
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        CanonicalInstrumentId spot = registry.resolve("BINANCE", "BTCUSDT", MarketType.SPOT)
                .canonicalInstrumentId();
        CanonicalInstrumentId perpetual = registry.resolve("BINANCE", "BTCUSDT", MarketType.PERPETUAL)
                .canonicalInstrumentId();

        assertThat(spot).isNotEqualTo(perpetual);
        assertThat(spot.marketType()).isEqualTo(MarketType.SPOT);
        assertThat(perpetual.marketType()).isEqualTo(MarketType.PERPETUAL);
    }

    @Test
    void dynamicBinanceUsdtSymbolGetsCanonicalIdentityButUnsupportedQuoteFailsClosed() {
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        assertThat(registry.resolve("BINANCE", "AAVEUSDT", MarketType.PERPETUAL)
                .canonicalInstrumentId().displaySymbol()).isEqualTo("AAVE/USDT");
        assertThatThrownBy(() -> registry.resolve("BINANCE", "UNKNOWNBTC", MarketType.PERPETUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PROVIDER_SYMBOL_MAPPING_NOT_FOUND");
    }

    @Test
    void providerRequestKeyCarriesCanonicalIdentityAndSourceVersion() {
        ProviderRequestKey key = ProviderCallTestFixtures.key(
                "BINANCE", ProviderDatasetType.OHLCV, "BTCUSDT", "15m", "bucket-1");
        assertThat(key.canonical()).contains(key.canonicalInstrumentId().canonical(), "BINANCE_TEST_V1", "15M");
    }

    @Test
    void lowStandardAndHighProduceDifferentDueTimes() {
        ProviderCallProperties properties = new ProviderCallProperties();
        ProviderDueTimePolicy policy = new ProviderDueTimePolicy(properties);
        Instant previous = AS_OF.minusSeconds(1);

        assertThat(policy.dueAt(previous, AS_OF, RuntimeScanProfile.LOW,
                AssetPriority.P1_WATCHLIST, ProviderDatasetType.PRICE))
                .isEqualTo(previous.plusSeconds(60));
        assertThat(policy.dueAt(previous, AS_OF, RuntimeScanProfile.STANDARD,
                AssetPriority.P1_WATCHLIST, ProviderDatasetType.PRICE))
                .isEqualTo(previous.plusSeconds(30));
        assertThat(policy.dueAt(previous, AS_OF, RuntimeScanProfile.HIGH,
                AssetPriority.P1_WATCHLIST, ProviderDatasetType.PRICE))
                .isEqualTo(previous.plusSeconds(15));
    }

    @Test
    void autoPositionCarriesSafetyReasonAndManualHighNeverDowngrades() {
        ProviderCallProfileResolver resolver = new ProviderCallProfileResolver();
        var autoPosition = resolver.resolve(UserScanProfile.AUTO, AssetPriority.P0_POSITION,
                RuntimeScanProfile.LOW, null, null);
        var manualHigh = resolver.resolve(UserScanProfile.HIGH, AssetPriority.P3_DISCOVERY,
                RuntimeScanProfile.LOW, null, null);

        assertThat(autoPosition.effectiveProfile()).isEqualTo(RuntimeScanProfile.STANDARD);
        assertThat(autoPosition.reasonCodes()).contains("ACTIVE_POSITION");
        assertThat(manualHigh.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(manualHigh.reasonCodes()).contains("MANUAL_HIGH");
    }

    @Test
    void lowPositionPriceSafetyFloorNeverExceedsFifteenSeconds() {
        ProviderCallProperties properties = new ProviderCallProperties();
        assertThat(properties.intervalSeconds(RuntimeScanProfile.LOW,
                AssetPriority.P0_POSITION, ProviderDatasetType.PRICE)).isLessThanOrEqualTo(15);
    }

    @Test
    void identicalFrequencyConfigurationProducesStableVersion() {
        String left = new FrequencyMatrixVersionService(new ProviderCallProperties()).currentVersion();
        String right = new FrequencyMatrixVersionService(new ProviderCallProperties()).currentVersion();
        assertThat(left).isEqualTo(right).startsWith("FM-");
    }

    @Test
    void dueTimeIsIndependentFromJvmDefaultTimezone() {
        TimeZone original = TimeZone.getDefault();
        try {
            Instant utc = dueInZone("UTC");
            Instant shanghai = dueInZone("Asia/Shanghai");
            Instant newYork = dueInZone("America/New_York");
            assertThat(utc).isEqualTo(shanghai).isEqualTo(newYork);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static Instant dueInZone(String zone) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        return new ProviderDueTimePolicy(new ProviderCallProperties()).dueAt(
                AS_OF, AS_OF, RuntimeScanProfile.HIGH,
                AssetPriority.P2_CANDIDATE, ProviderDatasetType.PRICE);
    }
}
