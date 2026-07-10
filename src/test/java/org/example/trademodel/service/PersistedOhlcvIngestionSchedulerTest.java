package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.OhlcvFreshnessStatus;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistedOhlcvIngestionSchedulerTest {
    @Mock
    private PublicOhlcvProvider provider;
    @Mock
    private PersistedOhlcvIngestionService ingestionService;

    @Test
    void schedulerIsProductionDefaultOff() throws Exception {
        String prod = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        PersistedOhlcvIngestionScheduler scheduler = scheduler(false, false);

        scheduler.ingestScheduled();

        assertThat(prod).contains("TRADE_MODEL_OHLCV_INGESTION_SCHEDULER_ENABLED:false");
        verify(provider, never()).fetchClosedBars(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void schedulerDoesNotOverlapSameSymbolTimeframe() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        OhlcvIngestionBatch batch = new OhlcvIngestionBatch("BINANCE_PUBLIC", "SPOT", "/api/v3/klines",
                OhlcvSourceState.READY, Instant.now(), "v1", 1, "trace", "run", List.of());
        when(provider.fetchClosedBars(anyString(), anyString(), anyInt(), anyString())).thenAnswer(invocation -> {
            entered.countDown();
            release.await();
            return new PublicOhlcvProviderResult(OhlcvSourceState.READY, null, batch);
        });
        when(ingestionService.ingest(batch)).thenReturn(new OhlcvIngestionResult(
                OhlcvSourceState.READY, OhlcvFreshnessStatus.FRESH, 1, 0, 0, List.of()));
        PersistedOhlcvIngestionScheduler scheduler = scheduler(true, true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<OhlcvIngestionResult> first = executor.submit(() -> scheduler.ingestOne("BTCUSDT", "5m"));
            entered.await();

            OhlcvIngestionResult overlap = scheduler.ingestOne("BTCUSDT", "5m");

            assertThat(overlap.sourceState()).isEqualTo(OhlcvSourceState.WAITING_SYNC);
            assertThat(overlap.reasonCodes()).contains("INGESTION_ALREADY_RUNNING");
            release.countDown();
            assertThat(first.get().ready()).isTrue();
            verify(provider).fetchClosedBars(anyString(), anyString(), anyInt(), anyString());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private PersistedOhlcvIngestionScheduler scheduler(boolean global, boolean enabled) {
        return new PersistedOhlcvIngestionScheduler(provider, ingestionService, global, enabled,
                "BTCUSDT", "5m,15m,1h,4h", 100);
    }
}
