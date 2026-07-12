package org.example.trademodel.localreal;

import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.ohlcv.OhlcvFreshnessStatus;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.PersistedOhlcvIngestionScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LocalRealDataCoordinatorTest {
    @Mock PersistedOhlcvIngestionScheduler ingestionScheduler;
    @Mock AnalysisSchedulerService analysisSchedulerService;

    @Test
    void bootstrapRequestsSixAssetsAndFourTimeframesAndWaitsForReadiness() {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        when(ingestionScheduler.ingestOne(anyString(), anyString())).thenReturn(readyIngestion());
        when(analysisSchedulerService.marketDataReady(anyString())).thenReturn(false);
        LocalRealDataCoordinator coordinator = coordinator(readiness);

        coordinator.bootstrap();

        ArgumentCaptor<String> symbols = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> timeframes = ArgumentCaptor.forClass(String.class);
        verify(ingestionScheduler, org.mockito.Mockito.times(24)).ingestOne(symbols.capture(), timeframes.capture());
        assertThat(symbols.getAllValues()).containsAll(LocalRealDataCoordinator.SYMBOLS);
        assertThat(timeframes.getAllValues()).containsAll(LocalRealDataCoordinator.TIMEFRAMES);
        verify(analysisSchedulerService, never()).runScheduledCycle();
        assertThat(readiness.state()).isEqualTo(LocalRealReadinessState.DEGRADED);
        coordinator.shutdown();
    }

    @Test
    void analysisRunsAfterMinimumBarsAvailableForAllSixAssets() {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        when(analysisSchedulerService.marketDataReady(anyString())).thenReturn(true);
        List<AnalysisRunResult> results = successfulResults(
                "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");
        when(analysisSchedulerService.runScheduledCycle()).thenReturn(results);
        LocalRealDataCoordinator coordinator = coordinator(readiness);

        coordinator.bootstrap();

        assertThat(readiness.state()).isEqualTo(LocalRealReadinessState.DASHBOARD_READY);
        verify(ingestionScheduler, never()).ingestOne(anyString(), anyString());
        verify(analysisSchedulerService).runScheduledCycle();
        coordinator.shutdown();
    }

    @Test
    void fiveSuccessfulAssetsMakeDashboardReadyWhileUnsupportedBnbStaysUnavailable() {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        when(ingestionScheduler.ingestOne(anyString(), anyString())).thenAnswer(invocation ->
                "BNBUSDT".equals(invocation.getArgument(0))
                        ? new OhlcvIngestionResult(OhlcvSourceState.ERROR, null, 0, 0, 0,
                        List.of("PAIR_NOT_SUPPORTED_OR_GEO_RESTRICTED"))
                        : readyIngestion());
        when(analysisSchedulerService.marketDataReady(anyString())).thenAnswer(invocation ->
                !"BNBUSDT".equals(invocation.getArgument(0)));
        List<AnalysisRunResult> results = successfulResults(
                "BTCUSDT", "ETHUSDT", "SOLUSDT", "XRPUSDT", "DOGEUSDT");
        when(analysisSchedulerService.runScheduledCycle()).thenReturn(results);
        LocalRealDataCoordinator coordinator = coordinator(readiness);

        coordinator.bootstrap();

        assertThat(readiness.state()).isEqualTo(LocalRealReadinessState.DASHBOARD_READY);
        assertThat(readiness.readyAssetCount()).isEqualTo(5);
        assertThat(readiness.asset("BNBUSDT").state()).isEqualTo(LocalRealAssetReadinessState.UNAVAILABLE);
        assertThat(readiness.asset("BNBUSDT").reasonCode())
                .isEqualTo("PAIR_NOT_SUPPORTED_OR_GEO_RESTRICTED");
        coordinator.shutdown();
    }

    @Test
    void publicProviderFailureProducesDegradedState() {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        when(ingestionScheduler.ingestOne(anyString(), anyString())).thenReturn(new OhlcvIngestionResult(
                OhlcvSourceState.ERROR, null, 0, 0, 0, List.of("PUBLIC_KLINE_FETCH_ERROR")));
        when(analysisSchedulerService.marketDataReady(anyString())).thenReturn(false);
        LocalRealDataCoordinator coordinator = coordinator(readiness);

        coordinator.bootstrap();

        assertThat(readiness.state()).isEqualTo(LocalRealReadinessState.DEGRADED);
        assertThat(readiness.reasonCode()).isEqualTo("PUBLIC_OHLCV_BOOTSTRAP_DEGRADED");
        coordinator.shutdown();
    }

    @Test
    void previousFailedRunDoesNotBlockNewSuccessfulRun() {
        LocalRealReadinessService readiness = new LocalRealReadinessService();
        when(analysisSchedulerService.marketDataReady(anyString())).thenReturn(true);
        AnalysisRunResult failed = mock(AnalysisRunResult.class);
        AnalysisRunResult success = mock(AnalysisRunResult.class);
        when(success.isSuccessfulAnalysisAvailable()).thenReturn(true);
        when(success.getSymbol()).thenReturn("BTCUSDT");
        when(analysisSchedulerService.runScheduledCycle())
                .thenReturn(List.of(failed), List.of(success));
        LocalRealDataCoordinator coordinator = coordinator(readiness);

        coordinator.bootstrap();
        assertThat(readiness.state()).isEqualTo(LocalRealReadinessState.DEGRADED);

        coordinator.recoverWhenMarketBecomesReady();

        assertThat(readiness.state()).isEqualTo(LocalRealReadinessState.DASHBOARD_PARTIAL);
        assertThat(readiness.asset("BTCUSDT").state()).isEqualTo(LocalRealAssetReadinessState.READY);
        verify(analysisSchedulerService, org.mockito.Mockito.times(2)).runScheduledCycle();
        verify(ingestionScheduler, never()).ingestOne(anyString(), anyString());
        coordinator.shutdown();
    }

    private LocalRealDataCoordinator coordinator(LocalRealReadinessService readiness) {
        return new LocalRealDataCoordinator(ingestionScheduler, analysisSchedulerService, readiness);
    }

    private static OhlcvIngestionResult readyIngestion() {
        return new OhlcvIngestionResult(OhlcvSourceState.READY, OhlcvFreshnessStatus.FRESH,
                100, 0, 0, List.of());
    }

    private static List<AnalysisRunResult> successfulResults(String... symbols) {
        return java.util.Arrays.stream(symbols).map(symbol -> {
            AnalysisRunResult result = mock(AnalysisRunResult.class);
            when(result.isSuccessfulAnalysisAvailable()).thenReturn(true);
            when(result.getSymbol()).thenReturn(symbol);
            return result;
        }).toList();
    }
}
