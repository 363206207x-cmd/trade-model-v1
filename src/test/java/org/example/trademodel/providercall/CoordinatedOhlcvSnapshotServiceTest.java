package org.example.trademodel.providercall;

import org.example.trademodel.dto.ohlcv.OhlcvFreshnessStatus;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoordinatedOhlcvSnapshotServiceTest {

    @Test
    void ohlcvResultUsesAuthoritativeWriter() {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(true);
        properties.setExternalCallsEnabled(true);
        Clock clock = Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC);
        ProviderRateBudgetManager budget = new ProviderRateBudgetManager(properties, clock);
        budget.register("BINANCE_PUBLIC", 100);
        ProviderCallCoordinator coordinator = new ProviderCallCoordinator(properties, new SnapshotCacheService(),
                new ProviderSingleFlightGuard(), budget, new ProviderCircuitBreaker(3, 60, clock),
                new ProviderCallAuditLog(), clock);
        PublicOhlcvProvider provider = mock(PublicOhlcvProvider.class);
        PersistedOhlcvIngestionService writer = mock(PersistedOhlcvIngestionService.class);
        OhlcvIngestionBatch batch = new OhlcvIngestionBatch("BINANCE_PUBLIC", "SPOT", "/api/v3/klines",
                OhlcvSourceState.READY, clock.instant(), "v1", 1, "trace-1", "run-1", List.of());
        when(provider.fetchClosedBars("BTCUSDT", "5m", 100, "trace-1"))
                .thenReturn(new PublicOhlcvProviderResult(OhlcvSourceState.READY, null, batch));
        when(writer.ingest(batch)).thenReturn(new OhlcvIngestionResult(OhlcvSourceState.READY,
                OhlcvFreshnessStatus.FRESH, 1, 0, 0, List.of()));

        ProviderCallResult<OhlcvIngestionResult> result = new CoordinatedOhlcvSnapshotService(
                coordinator, provider, writer, clock).refresh("BTCUSDT", "5m", 100,
                AssetPriority.P1_CORE, "trace-1");

        assertThat(result.ready()).isTrue();
        verify(writer).ingest(batch);
    }
}
