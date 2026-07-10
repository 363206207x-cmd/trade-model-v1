package org.example.trademodel.providercall;

import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.providercall.scan.DefaultProviderDatasetRefreshPort;
import org.example.trademodel.providercall.scan.ProviderRefreshObservation;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProviderDatasetRefreshPortTest {
    private MarketPriceSnapshotService priceService;
    private CoordinatedOhlcvSnapshotService ohlcvService;
    private ProviderRefreshStateRegistry registry;
    private PersistedOhlcvBarMapper ohlcvBarMapper;
    private DefaultProviderDatasetRefreshPort port;

    @BeforeEach void setUp() {
        priceService = mock(MarketPriceSnapshotService.class);
        ohlcvService = mock(CoordinatedOhlcvSnapshotService.class);
        ohlcvBarMapper = mock(PersistedOhlcvBarMapper.class);
        registry = new ProviderRefreshStateRegistry();
        port = new DefaultProviderDatasetRefreshPort(priceService, ohlcvService, ohlcvBarMapper,
                new ProviderCallProperties(), registry);
    }

    @Test void derivativesRemainNotConfiguredWithoutCoinGlass() {
        port.refresh(item(), ProviderDatasetType.DERIVATIVES);
        ProviderRefreshObservation out = registry.get("BTCUSDT", ProviderDatasetType.DERIVATIVES);
        assertThat(out.sourceStatus()).isEqualTo(UnifiedSourceStatus.NOT_CONFIGURED);
        assertThat(out.reasonCode()).isEqualTo("COINGLASS_NOT_CONFIGURED");
    }

    @Test void routineScanDoesNotInvokeAi() {
        port.refresh(item(), ProviderDatasetType.AI_REVIEW);
        assertThat(registry.get("BTCUSDT", ProviderDatasetType.AI_REVIEW).sourceStatus())
                .isEqualTo(UnifiedSourceStatus.DISABLED);
        verify(priceService, never()).get(anyString(), any(), any(), anyString());
    }

    @Test void priceRefreshRoutesThroughMarketPriceSnapshotService() {
        when(priceService.get(anyString(), any(), any(), anyString())).thenReturn(result(ProviderDatasetType.PRICE));
        port.refresh(item(), ProviderDatasetType.PRICE);
        verify(priceService).get(anyString(), any(), any(), anyString());
        assertThat(registry.get("BTCUSDT", ProviderDatasetType.PRICE).sourceStatus()).isEqualTo(UnifiedSourceStatus.READY);
    }

    @Test void ohlcvRefreshUsesAllFourPrimaryTimeframes() {
        when(ohlcvService.refresh(anyString(), anyString(), anyInt(), any(), anyString()))
                .thenReturn((ProviderCallResult) result(ProviderDatasetType.OHLCV));
        port.refresh(item(), ProviderDatasetType.OHLCV);
        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            verify(ohlcvService).refresh(anyString(), org.mockito.ArgumentMatchers.eq(timeframe), anyInt(), any(), anyString());
        }
    }

    @Test void ohlcvRefreshWaitsUntilNextClosedBarIsDue() {
        PersistedOhlcvBarDO latest = new PersistedOhlcvBarDO();
        latest.setCloseTimeMs(Instant.now().minusSeconds(60).toEpochMilli());
        when(ohlcvBarMapper.selectLatestClosedWindow(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(latest));

        port.refresh(item(), ProviderDatasetType.OHLCV);

        verify(ohlcvService, never()).refresh(anyString(), anyString(), anyInt(), any(), anyString());
        assertThat(registry.get("BTCUSDT", ProviderDatasetType.OHLCV).reasonCode())
                .isEqualTo("NO_NEW_CLOSED_BAR_DUE");
    }

    private static ScanPlanItem item() {
        Instant now = Instant.now();
        return new ScanPlanItem("BTCUSDT", AssetPriority.P0_POSITION, Set.of(), now, now, now, now, now,
                RuntimeScanProfile.STANDARD, "test");
    }

    private static ProviderCallResult<MarketPriceSnapshot> result(ProviderDatasetType type) {
        Instant now = Instant.now();
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("TEST", type, "BTCUSDT", "GLOBAL",
                now, now, now.plusSeconds(30), UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH,
                "trace", "key", false, false, null, List.of());
        return new ProviderCallResult<>(null, metadata, null);
    }
}
