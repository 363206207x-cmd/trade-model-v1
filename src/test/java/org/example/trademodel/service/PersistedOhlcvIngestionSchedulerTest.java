package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.OhlcvFreshnessStatus;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.UserPositionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

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
        OhlcvIngestionBatch batch = batch("BTCUSDT", "5m");
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

    @Test
    void schedulerRotatesAcrossAssetPoolInsteadOfUsingConfiguredFixedSymbols() {
        List<String> poolSymbols = List.of(
                "AUSDT", "BUSDT", "CUSDT", "DUSDT", "EUSDT",
                "FUSDT", "GUSDT", "HUSDT", "IUSDT", "JUSDT");
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        when(assetPoolService.listScanSymbols()).thenReturn(poolSymbols);
        when(provider.fetchClosedBars(anyString(), anyString(), anyInt(), anyString()))
                .thenAnswer(invocation -> new PublicOhlcvProviderResult(
                        OhlcvSourceState.READY, null,
                        batch(invocation.getArgument(0), invocation.getArgument(1))));
        when(ingestionService.ingest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OhlcvIngestionResult(
                        OhlcvSourceState.READY, OhlcvFreshnessStatus.FRESH, 1, 0, 0, List.of()));
        PersistedOhlcvIngestionScheduler scheduler = new PersistedOhlcvIngestionScheduler(
                provider, ingestionService, true, true, "BTCUSDT", "5m,15m,1h,4h", 100, 6);
        scheduler.setAssetPoolService(assetPoolService);

        scheduler.ingestScheduled();
        scheduler.ingestScheduled();

        org.mockito.ArgumentCaptor<String> symbols = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(provider, org.mockito.Mockito.times(48))
                .fetchClosedBars(symbols.capture(), anyString(), anyInt(), anyString());
        assertThat(symbols.getAllValues()).containsAll(poolSymbols);
        assertThat(symbols.getAllValues()).doesNotContain("BTCUSDT");
    }

    @Test
    void activePositionOutsidePoolRemainsInMarketCoverage() {
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        UserPositionMapper userPositionMapper = mock(UserPositionMapper.class);
        when(assetPoolService.listScanSymbols()).thenReturn(List.of("BTCUSDT"));
        UserPositionDO position = new UserPositionDO();
        position.setAssetSymbol("ETH");
        when(userPositionMapper.listClaimedOpenForSystemMonitoring()).thenReturn(List.of(position));
        when(provider.fetchClosedBars(anyString(), anyString(), anyInt(), anyString()))
                .thenAnswer(invocation -> new PublicOhlcvProviderResult(
                        OhlcvSourceState.READY, null,
                        batch(invocation.getArgument(0), invocation.getArgument(1))));
        when(ingestionService.ingest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OhlcvIngestionResult(
                        OhlcvSourceState.READY, OhlcvFreshnessStatus.FRESH, 1, 0, 0, List.of()));
        PersistedOhlcvIngestionScheduler scheduler = new PersistedOhlcvIngestionScheduler(
                provider, ingestionService, true, true, "", "5m,15m,1h,4h", 100, 20);
        scheduler.setAssetPoolService(assetPoolService);
        scheduler.setUserPositionMapper(userPositionMapper);

        scheduler.ingestScheduled();

        org.mockito.ArgumentCaptor<String> symbols = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(provider, org.mockito.Mockito.times(8))
                .fetchClosedBars(symbols.capture(), anyString(), anyInt(), anyString());
        assertThat(symbols.getAllValues()).containsOnly("BTCUSDT", "ETHUSDT");
    }

    @Test
    void providerFailureForOneSymbolDoesNotAbortRemainingMarketCoverage() {
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        when(assetPoolService.listScanSymbols()).thenReturn(List.of("BADUSDT", "GOODUSDT"));
        when(provider.fetchClosedBars(eq("BADUSDT"), anyString(), anyInt(), anyString()))
                .thenThrow(new IllegalStateException("provider unavailable"));
        when(provider.fetchClosedBars(eq("GOODUSDT"), anyString(), anyInt(), anyString()))
                .thenAnswer(invocation -> new PublicOhlcvProviderResult(
                        OhlcvSourceState.READY, null,
                        batch(invocation.getArgument(0), invocation.getArgument(1))));
        when(ingestionService.ingest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OhlcvIngestionResult(
                        OhlcvSourceState.READY, OhlcvFreshnessStatus.FRESH, 1, 0, 0, List.of()));
        PersistedOhlcvIngestionScheduler scheduler = new PersistedOhlcvIngestionScheduler(
                provider, ingestionService, true, true, "", "5m,15m,1h,4h", 100, 20);
        scheduler.setAssetPoolService(assetPoolService);

        scheduler.ingestScheduled();

        verify(provider, org.mockito.Mockito.times(4))
                .fetchClosedBars(eq("BADUSDT"), anyString(), anyInt(), anyString());
        verify(provider, org.mockito.Mockito.times(4))
                .fetchClosedBars(eq("GOODUSDT"), anyString(), anyInt(), anyString());
        verify(ingestionService, org.mockito.Mockito.times(4))
                .ingest(org.mockito.ArgumentMatchers.argThat(batch -> "GOODUSDT".equals(batch.bars().get(0).symbol())));
    }

    @Test
    void nonBinanceOrOpenBarBatchFailsClosedBeforePersistence() {
        OhlcvIngestionBatch kraken = new OhlcvIngestionBatch(
                "KRAKEN", "SPOT", "/kraken", OhlcvSourceState.READY,
                Instant.now(), "v1", 1, "trace", "run",
                List.of(bar("BTCUSDT", "5m", true)));
        when(provider.fetchClosedBars(eq("BTCUSDT"), eq("5m"), eq(100), anyString()))
                .thenReturn(new PublicOhlcvProviderResult(OhlcvSourceState.READY, null, kraken));
        PersistedOhlcvIngestionScheduler scheduler = scheduler(true, true);

        OhlcvIngestionResult result = scheduler.ingestOne("BTCUSDT", "5m");

        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCodes()).contains("BINANCE_SOURCE_OWNERSHIP_REQUIRED");
        verify(ingestionService, never()).ingest(org.mockito.ArgumentMatchers.any());
    }

    private PersistedOhlcvIngestionScheduler scheduler(boolean global, boolean enabled) {
        return new PersistedOhlcvIngestionScheduler(provider, ingestionService, global, enabled,
                "BTCUSDT", "5m,15m,1h,4h", 100, 2);
    }

    private static OhlcvIngestionBatch batch(String symbol, String timeframe) {
        return new OhlcvIngestionBatch(
                "BINANCE_PUBLIC", "SPOT", "/api/v3/klines",
                OhlcvSourceState.READY, Instant.now(), "v1", 1,
                "trace", "run", List.of(bar(symbol, timeframe, true)));
    }

    private static OhlcvBarInput bar(String symbol, String timeframe, boolean closed) {
        return new OhlcvBarInput(
                symbol, timeframe, 1L, 2L,
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
                1L, BigDecimal.ONE, BigDecimal.ONE, closed);
    }
}
