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
        when(ingestionScheduler.ingestOne(anyString(), anyString())).thenReturn(readyIngestion());
        when(analysisSchedulerService.marketDataReady(anyString())).thenReturn(true);
        AnalysisRunResult successful = org.mockito.Mockito.mock(AnalysisRunResult.class);
        when(successful.isSuccessfulAnalysisAvailable()).thenReturn(true);
        when(analysisSchedulerService.runScheduledCycle())
                .thenReturn(List.of(successful, successful, successful, successful, successful, successful));
        LocalRealDataCoordinator coordinator = coordinator(readiness);

        coordinator.bootstrap();

        assertThat(readiness.state()).isEqualTo(LocalRealReadinessState.DASHBOARD_READY);
        verify(analysisSchedulerService).runScheduledCycle();
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

    private LocalRealDataCoordinator coordinator(LocalRealReadinessService readiness) {
        return new LocalRealDataCoordinator(ingestionScheduler, analysisSchedulerService, readiness);
    }

    private static OhlcvIngestionResult readyIngestion() {
        return new OhlcvIngestionResult(OhlcvSourceState.READY, OhlcvFreshnessStatus.FRESH,
                100, 0, 0, List.of());
    }
}
