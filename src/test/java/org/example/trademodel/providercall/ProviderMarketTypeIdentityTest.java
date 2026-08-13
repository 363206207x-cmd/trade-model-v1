package org.example.trademodel.providercall;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.providercall.coinglass.CoinGlassDerivativesSnapshotAssembler;
import org.example.trademodel.providercall.coinglass.CoinGlassDerivativesSnapshotService;
import org.example.trademodel.providercall.coinglass.CoinGlassFundingSnapshotService;
import org.example.trademodel.providercall.coinglass.CoinGlassLiquidationSnapshotService;
import org.example.trademodel.providercall.coinglass.CoinGlassLongShortSnapshotService;
import org.example.trademodel.providercall.coinglass.CoinGlassOpenInterestSnapshotService;
import org.example.trademodel.providercall.coinglass.CoinGlassProperties;
import org.example.trademodel.providercall.coinglass.CoinGlassSymbolMapper;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.providercall.snapshot.AnalysisInputBundleAssembler;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.snapshot.OhlcvSnapshotReference;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ProviderMarketTypeIdentityTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");
    private static final CanonicalInstrumentId SPOT = ProviderCallTestFixtures.spot("BTCUSDT");
    private static final CanonicalInstrumentId PERPETUAL = ProviderCallTestFixtures.perpetual("BTCUSDT");

    @Test
    void perpetualScanUsesPerpetualPriceMappingAndFailsClosedOnMissingProviderResult() {
        ProviderSnapshotQueryService query = mock(ProviderSnapshotQueryService.class);
        ProviderSnapshotRefreshService refresh = mock(ProviderSnapshotRefreshService.class);
        MarketQuoteClient quoteClient = mock(MarketQuoteClient.class);
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        MarketPriceSnapshotService service = new MarketPriceSnapshotService(query, refresh, quoteClient,
                registry, new ProviderRequestKeyFactory(registry), Clock.fixed(NOW, ZoneOffset.UTC));

        ProviderCallResult<?> result = service.get(PERPETUAL, AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "perpetual-price");

        assertThat(result.metadata().canonicalInstrumentId()).isEqualTo(PERPETUAL);
        assertThat(result.metadata().errorCode()).isEqualTo("PROVIDER_RESULT_MISSING");
        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
        verifyNoInteractions(query, quoteClient);
    }

    @Test
    void perpetualScanUsesPerpetualOhlcvMapping() {
        ProviderCallCoordinator coordinator = mock(ProviderCallCoordinator.class);
        PublicOhlcvProvider provider = mock(PublicOhlcvProvider.class);
        PersistedOhlcvIngestionService writer = mock(PersistedOhlcvIngestionService.class);
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        CoordinatedOhlcvSnapshotService service = new CoordinatedOhlcvSnapshotService(coordinator,
                provider, writer, registry, new ProviderRequestKeyFactory(registry),
                Clock.fixed(NOW, ZoneOffset.UTC));

        ProviderCallResult<?> result = service.refresh(PERPETUAL, "5m", 100,
                AssetPriority.P0_POSITION, "perpetual-ohlcv");

        assertThat(result.metadata().canonicalInstrumentId()).isEqualTo(PERPETUAL);
        assertThat(result.metadata().errorCode()).isEqualTo("PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED");
        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
        verifyNoInteractions(coordinator, provider, writer);
    }

    @Test
    void spotAndPerpetualSnapshotsNeverShareCacheEntry() {
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        ProviderRequestKeyFactory factory = new ProviderRequestKeyFactory(registry);
        ProviderRequestKey spot = factory.create("BINANCE", ProviderDatasetType.PRICE, SPOT,
                "GLOBAL", Duration.ofSeconds(5), NOW);
        ProviderRequestKey perpetual = factory.create("BINANCE", ProviderDatasetType.PRICE, PERPETUAL,
                "GLOBAL", Duration.ofSeconds(5), NOW);

        assertThat(spot.providerSymbol()).isEqualTo(perpetual.providerSymbol());
        assertThat(spot.snapshotKey()).isNotEqualTo(perpetual.snapshotKey());
        assertThat(spot.snapshotKey().canonicalInstrumentId().marketType())
                .isNotEqualTo(perpetual.snapshotKey().canonicalInstrumentId().marketType());
    }

    @Test
    void spotPriceCannotBeRecordedAsPerpetualObservation() {
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        ProviderRequestKey spot = new ProviderRequestKeyFactory(registry).create("BINANCE",
                ProviderDatasetType.PRICE, SPOT, "GLOBAL", Duration.ofSeconds(5), NOW);

        assertThat(spot.snapshotKey().canonicalInstrumentId()).isEqualTo(SPOT);
        assertThat(spot.snapshotKey().canonicalInstrumentId()).isNotEqualTo(PERPETUAL);
    }

    @Test
    void spotReferenceMustBeExplicitlyLabelled() {
        String trace = "market-identity-trace";
        MarketPriceSnapshot spotPrice = new MarketPriceSnapshot("BTCUSDT", new BigDecimal("65000"),
                null, null, null, null, null, null, null, null, "BINANCE", NOW,
                metadata(SPOT, ProviderDatasetType.PRICE, "GLOBAL", trace));
        List<OhlcvSnapshotReference> perpetualBars = List.of("5m", "15m", "1h", "4h").stream()
                .map(timeframe -> new OhlcvSnapshotReference("BTCUSDT", timeframe, 1L, 100,
                        "fixture", metadata(PERPETUAL, ProviderDatasetType.OHLCV, timeframe, trace)))
                .toList();

        assertThatThrownBy(() -> new AnalysisInputBundleAssembler(Clock.fixed(NOW, ZoneOffset.UTC))
                .assemble(PERPETUAL, "BTCUSDT", perpetualBars, spotPrice, null, null,
                        null, null, "rule-v1", trace))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("market identity mismatch");
    }

    @Test
    void fundingCannotAttachToSpotInstrument() {
        CoinGlassOpenInterestSnapshotService oi = mock(CoinGlassOpenInterestSnapshotService.class);
        CoinGlassFundingSnapshotService funding = mock(CoinGlassFundingSnapshotService.class);
        CoinGlassLiquidationSnapshotService liquidation = mock(CoinGlassLiquidationSnapshotService.class);
        CoinGlassLongShortSnapshotService longShort = mock(CoinGlassLongShortSnapshotService.class);
        CoinGlassDerivativesSnapshotAssembler assembler = mock(CoinGlassDerivativesSnapshotAssembler.class);
        ProviderSymbolMappingRegistry registry = new ProviderSymbolMappingRegistry(List.of(
                new ProviderSymbolMapping("COINGLASS", PERPETUAL, "BTCUSDT", true,
                        "COINGLASS_TEST_V1")));
        CoinGlassDerivativesSnapshotService service = new CoinGlassDerivativesSnapshotService(
                new CoinGlassProperties(), oi, funding, liquidation, longShort, assembler,
                new CoinGlassSymbolMapper(registry));

        ProviderCallResult<?> result = service.get(SPOT, AssetPriority.P0_POSITION,
                Duration.ofSeconds(60), "spot-derivatives");

        assertThat(result.metadata().sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
        assertThat(result.metadata().errorCode()).isEqualTo("DERIVATIVES_REQUIRE_PERPETUAL_INSTRUMENT");
        verifyNoInteractions(oi, funding, liquidation, longShort, assembler);
    }

    @Test
    void unknownPerpetualPriceProviderFailsClosed() {
        ProviderSnapshotQueryService query = mock(ProviderSnapshotQueryService.class);
        ProviderSnapshotRefreshService refresh = mock(ProviderSnapshotRefreshService.class);
        MarketQuoteClient quoteClient = mock(MarketQuoteClient.class);
        ProviderSymbolMappingRegistry registry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        MarketPriceSnapshotService service = new MarketPriceSnapshotService(query, refresh, quoteClient,
                registry, new ProviderRequestKeyFactory(registry), Clock.fixed(NOW, ZoneOffset.UTC));

        ProviderCallResult<?> result = service.get(PERPETUAL, AssetPriority.P0_POSITION,
                Duration.ofSeconds(5), "unknown-perpetual-client");

        assertThat(result.payload()).isNull();
        assertThat(result.metadata().freshnessStatus()).isEqualTo(SnapshotFreshnessStatus.UNAVAILABLE);
        assertThat(result.metadata().sourceStatus()).isNotEqualTo(UnifiedSourceStatus.READY);
    }

    private static ProviderSnapshotMetadata metadata(CanonicalInstrumentId instrument,
                                                     ProviderDatasetType type,
                                                     String timeframe,
                                                     String traceId) {
        ProviderRequestKey key = new ProviderRequestKey("BINANCE", type, instrument, "BTCUSDT",
                timeframe, "bucket", instrument.marketType().name() + "_V1");
        return new ProviderSnapshotMetadata("BINANCE", type, instrument, "BTCUSDT", timeframe,
                NOW, NOW, NOW.plusSeconds(60), 0L, UnifiedSourceStatus.READY,
                SnapshotFreshnessStatus.FRESH, traceId, key.canonical(), key.sourceVersion(),
                false, false, null, List.of());
    }
}
