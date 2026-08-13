package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisSchedulerLocalRealReadinessGateTest {
    @Mock AnalysisRunOrchestrator orchestrator;
    @Mock PersistedOhlcvQueryService queryService;
    @Mock AssetPoolService assetPoolService;

    private AnalysisSchedulerService service;

    @BeforeEach
    void setUp() {
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        properties.getScheduler().setSymbols(List.of("BTCUSDT"));
        properties.getScheduler().setTimeframes(List.of("5m", "15m", "1h", "4h"));
        properties.getScheduler().setRequiredMarketTimeframes(List.of("5m", "15m", "1h", "4h"));
        properties.getScheduler().setRequiredClosedBars(100);
        when(assetPoolService.listScanSymbols()).thenReturn(List.of("BTCUSDT"));
        service = new AnalysisSchedulerService(orchestrator, properties,
                Clock.fixed(Instant.parse("2026-07-13T00:00:00Z"), ZoneOffset.UTC), assetPoolService);
        service.setPersistedOhlcvQueryService(queryService);
    }

    @Test
    void analysisWaitsForAllRequiredTimeframes() {
        when(queryService.evaluateReadiness(eq("BTCUSDT"), any(), eq(100), anyLong()))
                .thenReturn(readiness(PersistedOhlcvReadinessStatus.FRESH));
        when(queryService.evaluateReadiness(eq("BTCUSDT"), eq("1h"), eq(100), anyLong()))
                .thenReturn(readiness(PersistedOhlcvReadinessStatus.PARTIAL));

        assertThat(service.runScheduledCycle()).isEmpty();
        verify(orchestrator, never()).run(any());
    }

    @Test
    void schedulerUsesPersistedKrakenBarsAndAnalysisRunSucceedsAfterRealBarsReady() {
        when(queryService.evaluateReadiness(eq("BTCUSDT"), any(), eq(100), anyLong()))
                .thenReturn(readiness(PersistedOhlcvReadinessStatus.FRESH));
        AnalysisRunResult result = org.mockito.Mockito.mock(AnalysisRunResult.class);
        when(orchestrator.run(any())).thenReturn(result);

        assertThat(service.runScheduledCycle()).containsExactly(result, result, result, result);
        verify(orchestrator, org.mockito.Mockito.times(4)).run(any());
    }

    private static PersistedOhlcvReadinessResult readiness(PersistedOhlcvReadinessStatus status) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setStatus(status);
        return result;
    }
}
