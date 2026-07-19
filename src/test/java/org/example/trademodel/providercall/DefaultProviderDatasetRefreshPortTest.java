package org.example.trademodel.providercall;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.providercall.scan.DefaultProviderDatasetRefreshPort;
import org.example.trademodel.providercall.scan.ProviderRefreshObservation;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.coinglass.CoinGlassDerivativesSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProviderDatasetRefreshPortTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");
    private MarketPriceSnapshotService priceService;
    private CoordinatedOhlcvSnapshotService ohlcvService;
    private ProviderRefreshStateRegistry registry;
    private PersistedOhlcvBarMapper ohlcvBarMapper;
    private CoinGlassDerivativesSnapshotService derivativesService;
    private ProviderSymbolMappingRegistry mappingRegistry;
    private DefaultProviderDatasetRefreshPort port;

    @BeforeEach void setUp() {
        priceService = mock(MarketPriceSnapshotService.class);
        ohlcvService = mock(CoordinatedOhlcvSnapshotService.class);
        ohlcvBarMapper = mock(PersistedOhlcvBarMapper.class);
        derivativesService = mock(CoinGlassDerivativesSnapshotService.class);
        mappingRegistry = ProviderCallTestFixtures.binanceRegistry("BTCUSDT");
        registry = new ProviderRefreshStateRegistry();
        port = new DefaultProviderDatasetRefreshPort(priceService, ohlcvService, ohlcvBarMapper,
                mappingRegistry, derivativesService, new ProviderCallProperties(), registry,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void derivativesRefreshPortUsesCoinGlassSnapshotService() {
        when(derivativesService.get(any(CanonicalInstrumentId.class), any(), any(), anyString()))
                .thenReturn((ProviderCallResult) result(ProviderDatasetType.DERIVATIVES));
        port.refresh(item(), ProviderDatasetType.DERIVATIVES);
        ProviderRefreshObservation out = registry.get(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                ProviderDatasetType.DERIVATIVES);
        verify(derivativesService).get(eq(ProviderCallTestFixtures.perpetual("BTCUSDT")),
                any(), any(), anyString());
        assertThat(out.sourceStatus()).isEqualTo(UnifiedSourceStatus.READY);
    }

    @Test void routineScanDoesNotInvokeAi() {
        port.refresh(item(), ProviderDatasetType.AI_REVIEW);
        assertThat(registry.get(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                ProviderDatasetType.AI_REVIEW).sourceStatus())
                .isEqualTo(UnifiedSourceStatus.DISABLED);
        verify(priceService, never()).get(any(CanonicalInstrumentId.class), any(), any(), anyString());
    }

    @Test void scanPlanCanonicalIdentitySurvivesEntireRefreshPath() {
        when(priceService.get(any(CanonicalInstrumentId.class), any(), any(), anyString()))
                .thenReturn(result(ProviderDatasetType.PRICE));
        port.refresh(item(), ProviderDatasetType.PRICE);
        verify(priceService).get(eq(ProviderCallTestFixtures.perpetual("BTCUSDT")), any(), any(), anyString());
        assertThat(registry.get(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                ProviderDatasetType.PRICE).sourceStatus()).isEqualTo(UnifiedSourceStatus.READY);
    }

    @Test void fourTimeframeRefreshRecordsPerTimeframeResult() {
        when(ohlcvService.refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString()))
                .thenReturn((ProviderCallResult) result(ProviderDatasetType.OHLCV));
        port.refresh(item(), ProviderDatasetType.OHLCV);
        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            verify(ohlcvService).refresh(eq(ProviderCallTestFixtures.perpetual("BTCUSDT")),
                    eq(timeframe), anyInt(), any(), anyString());
        }
        assertThat(registry.snapshot().keySet()).allMatch(key -> key.contains("|OHLCV|"));
        assertThat(registry.snapshot()).hasSize(4);
    }

    @Test void oneTimeframeFailureDoesNotRewriteOtherResults() {
        when(ohlcvService.refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString()))
                .thenAnswer(invocation -> "15m".equals(invocation.getArgument(1))
                        ? failedResult(ProviderDatasetType.OHLCV, "15M_FAILED")
                        : result(ProviderDatasetType.OHLCV));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            verify(ohlcvService).refresh(any(CanonicalInstrumentId.class), eq(timeframe), anyInt(), any(), anyString());
        }
        assertThat(registry.snapshot().values().stream()
                .filter(observation -> "15m".equals(observation.timeframe()))
                .map(ProviderRefreshObservation::reasonCode)).containsExactly("15M_FAILED");
        assertThat(registry.snapshot().values().stream()
                .filter(observation -> observation.sourceStatus() == UnifiedSourceStatus.READY)).hasSize(3);
    }

    @Test void ohlcvRefreshWaitsUntilNextClosedBarIsDue() {
        PersistedOhlcvBarDO latest = validBar("BINANCE_PUBLIC", "USDT_PERP");
        when(ohlcvBarMapper.selectLatestClosedWindowBySource(anyString(), anyString(), anyString(),
                anyString(), anyInt())).thenReturn(List.of(latest));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        verify(ohlcvService, never()).refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString());
        assertThat(registry.get(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                ProviderDatasetType.OHLCV).reasonCode())
                .isEqualTo("NO_NEW_CLOSED_BAR_DUE");
    }

    @Test void recentSpotBarCannotSuppressPerpetualRefresh() {
        stubRowsByIdentity(validBar("BINANCE_PUBLIC", "SPOT"));
        when(ohlcvService.refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString()))
                .thenAnswer(invocation -> ohlcvResult(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                        invocation.getArgument(1), UnifiedSourceStatus.NOT_CONFIGURED,
                        "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED"));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        verify(ohlcvService, times(4)).refresh(eq(ProviderCallTestFixtures.perpetual("BTCUSDT")),
                anyString(), anyInt(), any(), anyString());
        verify(ohlcvBarMapper, times(4)).selectLatestClosedWindowBySource(eq("BTCUSDT"), anyString(),
                eq("BINANCE_PUBLIC"), eq("USDT_PERP"), eq(1));
    }

    @Test void recentSpotBarCannotProducePerpetualReadyObservation() {
        stubRowsByIdentity(validBar("BINANCE_PUBLIC", "SPOT"));
        when(ohlcvService.refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString()))
                .thenAnswer(invocation -> ohlcvResult(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                        invocation.getArgument(1), UnifiedSourceStatus.NOT_CONFIGURED,
                        "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED"));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        assertThat(registry.snapshot()).hasSize(4);
        assertThat(registry.snapshot().values())
                .allMatch(value -> value.sourceStatus() == UnifiedSourceStatus.NOT_CONFIGURED)
                .noneMatch(value -> value.sourceStatus() == UnifiedSourceStatus.READY);
    }

    @Test void unsupportedPerpetualOhlcvReturnsNotConfiguredForAllFourTimeframes() {
        when(ohlcvService.refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString()))
                .thenAnswer(invocation -> ohlcvResult(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                        invocation.getArgument(1), UnifiedSourceStatus.NOT_CONFIGURED,
                        "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED"));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        assertThat(registry.snapshot().values()).hasSize(4)
                .allMatch(value -> value.sourceStatus() == UnifiedSourceStatus.NOT_CONFIGURED)
                .allMatch(value -> "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED".equals(value.reasonCode()));
    }

    @Test void recentSpotBarSuppressesOnlySpotRefresh() {
        stubRowsByIdentity(validBar("BINANCE_PUBLIC", "SPOT"));

        port.refresh(item(ProviderCallTestFixtures.spot("BTCUSDT")), ProviderDatasetType.OHLCV);

        verify(ohlcvService, never()).refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString());
        assertThat(registry.snapshot().values()).hasSize(4)
                .allMatch(value -> value.sourceStatus() == UnifiedSourceStatus.READY)
                .allMatch(value -> "SPOT".equals(value.providerMarketType()));
    }

    @Test void recentPerpetualBarSuppressesOnlyPerpetualRefresh() {
        stubRowsByIdentity(validBar("BINANCE_PUBLIC", "USDT_PERP"));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        verify(ohlcvService, never()).refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString());
        assertThat(registry.snapshot().values()).hasSize(4)
                .allMatch(value -> "USDT_PERP".equals(value.providerMarketType()));
    }

    @Test void spotAndPerpetualDueStateAreIndependent() {
        stubRowsByIdentity(validBar("BINANCE_PUBLIC", "SPOT"),
                validBar("BINANCE_PUBLIC", "USDT_PERP"));

        port.refresh(item(ProviderCallTestFixtures.spot("BTCUSDT")), ProviderDatasetType.OHLCV);
        port.refresh(item(), ProviderDatasetType.OHLCV);

        assertThat(registry.snapshot()).hasSize(8);
        assertThat(registry.snapshot().values()).extracting(ProviderRefreshObservation::providerMarketType)
                .containsOnly("SPOT", "USDT_PERP");
        verify(ohlcvService, never()).refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString());
    }

    @Test void noNewClosedBarDueRequiresMatchingMarketIdentity() {
        PersistedOhlcvBarDO wrongMarket = validBar("BINANCE_PUBLIC", "SPOT");
        when(ohlcvBarMapper.selectLatestClosedWindowBySource(anyString(), anyString(), anyString(),
                anyString(), anyInt())).thenReturn(List.of(wrongMarket));
        when(ohlcvService.refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString()))
                .thenAnswer(invocation -> ohlcvResult(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                        invocation.getArgument(1), UnifiedSourceStatus.NOT_CONFIGURED,
                        "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED"));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        assertThat(registry.snapshot().values())
                .noneMatch(value -> "NO_NEW_CLOSED_BAR_DUE".equals(value.reasonCode()));
    }

    @Test void fourTimeframeRegistryPreservesMarketIdentity() {
        when(ohlcvService.refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString()))
                .thenAnswer(invocation -> ohlcvResult(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                        invocation.getArgument(1), UnifiedSourceStatus.NOT_CONFIGURED,
                        "PERPETUAL_OHLCV_PROVIDER_NOT_CONFIGURED"));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        assertThat(registry.snapshot().values()).hasSize(4)
                .allMatch(value -> value.canonicalInstrumentId().equals(
                        ProviderCallTestFixtures.perpetual("BTCUSDT")))
                .allMatch(value -> "BINANCE".equals(value.provider()))
                .allMatch(value -> "USDT_PERP".equals(value.providerMarketType()))
                .allMatch(value -> "BINANCE_USDM_TEST_V1".equals(value.sourceVersion()));
        assertThat(registry.snapshot().values()).extracting(ProviderRefreshObservation::timeframe)
                .containsExactlyInAnyOrder("5m", "15m", "1h", "4h");
    }

    private static ScanPlanItem item() {
        return item(ProviderCallTestFixtures.perpetual("BTCUSDT"));
    }

    private static ScanPlanItem item(CanonicalInstrumentId instrument) {
        return new ScanPlanItem(instrument, "BTCUSDT",
                AssetPriority.P0_POSITION, Set.of(), NOW, NOW, NOW, NOW, NOW, UserScanProfile.AUTO,
                RuntimeScanProfile.STANDARD, List.of("test"), "FM-TEST");
    }

    private void stubRowsByIdentity(PersistedOhlcvBarDO... bars) {
        when(ohlcvBarMapper.selectLatestClosedWindowBySource(anyString(), anyString(), anyString(),
                anyString(), anyInt())).thenAnswer(invocation -> List.of(bars).stream()
                .filter(bar -> invocation.<String>getArgument(2).equalsIgnoreCase(bar.getProvider()))
                .filter(bar -> invocation.<String>getArgument(3).equalsIgnoreCase(bar.getProviderMarketType()))
                .limit(1)
                .toList());
    }

    private static PersistedOhlcvBarDO validBar(String provider, String providerMarketType) {
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setCloseTimeMs(NOW.minusSeconds(60).toEpochMilli());
        bar.setClosed(true);
        bar.setProvider(provider);
        bar.setProviderMarketType(providerMarketType);
        bar.setSourceStatus("READY");
        bar.setFreshnessStatus("FRESH");
        bar.setQualityStatus("OK");
        bar.setSourceVersion(1);
        bar.setIsDeleted(0);
        return bar;
    }

    private static ProviderCallResult<OhlcvIngestionResult> ohlcvResult(
            CanonicalInstrumentId instrument,
            String timeframe,
            UnifiedSourceStatus status,
            String reason) {
        String sourceVersion = instrument.marketType().name().equals("SPOT")
                ? "BINANCE_SPOT_TEST_V1" : "BINANCE_USDM_TEST_V1";
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("BINANCE", ProviderDatasetType.OHLCV,
                instrument, "BTCUSDT", timeframe, null, NOW, NOW, 0L, status,
                SnapshotFreshnessStatus.UNAVAILABLE, "trace", "key", sourceVersion,
                false, false, reason, List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }

    private static ProviderCallResult<MarketPriceSnapshot> result(ProviderDatasetType type) {
        Instant now = Instant.now();
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("TEST", type, "BTCUSDT", "GLOBAL",
                now, now, now.plusSeconds(30), UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH,
                "trace", "key", false, false, null, List.of());
        return new ProviderCallResult<>(null, metadata, null);
    }

    private static ProviderCallResult failedResult(ProviderDatasetType type, String reason) {
        Instant now = Instant.now();
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("TEST", type, "BTCUSDT", "GLOBAL",
                null, now, now, UnifiedSourceStatus.ERROR, SnapshotFreshnessStatus.UNAVAILABLE,
                "trace", "key", false, false, reason, List.of(reason));
        return new ProviderCallResult<>(null, metadata, null);
    }
}
