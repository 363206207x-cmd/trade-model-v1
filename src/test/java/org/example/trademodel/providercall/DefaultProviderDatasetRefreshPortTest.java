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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProviderDatasetRefreshPortTest {
    private MarketPriceSnapshotService priceService;
    private CoordinatedOhlcvSnapshotService ohlcvService;
    private ProviderRefreshStateRegistry registry;
    private PersistedOhlcvBarMapper ohlcvBarMapper;
    private CoinGlassDerivativesSnapshotService derivativesService;
    private DefaultProviderDatasetRefreshPort port;

    @BeforeEach void setUp() {
        priceService = mock(MarketPriceSnapshotService.class);
        ohlcvService = mock(CoordinatedOhlcvSnapshotService.class);
        ohlcvBarMapper = mock(PersistedOhlcvBarMapper.class);
        derivativesService = mock(CoinGlassDerivativesSnapshotService.class);
        registry = new ProviderRefreshStateRegistry();
        port = new DefaultProviderDatasetRefreshPort(priceService, ohlcvService, ohlcvBarMapper,
                derivativesService, new ProviderCallProperties(), registry);
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
        PersistedOhlcvBarDO latest = new PersistedOhlcvBarDO();
        latest.setCloseTimeMs(Instant.now().minusSeconds(60).toEpochMilli());
        when(ohlcvBarMapper.selectLatestClosedWindow(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(latest));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        verify(ohlcvService, never()).refresh(any(CanonicalInstrumentId.class), anyString(), anyInt(), any(), anyString());
        assertThat(registry.get(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                ProviderDatasetType.OHLCV).reasonCode())
                .isEqualTo("NO_NEW_CLOSED_BAR_DUE");
    }

    private static ScanPlanItem item() {
        Instant now = Instant.now();
        return new ScanPlanItem(ProviderCallTestFixtures.perpetual("BTCUSDT"), "BTCUSDT",
                AssetPriority.P0_POSITION, Set.of(), now, now, now, now, now, UserScanProfile.AUTO,
                RuntimeScanProfile.STANDARD, List.of("test"), "FM-TEST");
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
