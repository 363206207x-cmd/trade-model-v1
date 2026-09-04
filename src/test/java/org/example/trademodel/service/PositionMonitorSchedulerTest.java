package org.example.trademodel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PositionMonitorSchedulerTest {

    @Test
    void defaultDisabledSchedulerDoesNotRunMonitorBatch() {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, false);

        scheduler.monitorOpenUserPositionsScheduled();

        verify(service, never()).monitorClaimedOpenPositionsForSystem();
    }

    @Test
    void enabledSchedulerRunsOpenPositionMonitorBatchOnly() {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, true);

        scheduler.monitorOpenUserPositionsScheduled();

        verify(service).monitorClaimedOpenPositionsForSystem();
    }

    @Test
    void enabledSchedulerLogsSanitizedBatchCounts(CapturedOutput output) {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorBatchResultDTO batch = new PositionMonitorBatchResultDTO();
        batch.setTotalCount(28);
        batch.setSuccessCount(0);
        batch.setFailureCount(28);
        batch.setBlockedCount(0);
        when(service.monitorClaimedOpenPositionsForSystem()).thenReturn(batch);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, true);

        scheduler.monitorOpenUserPositionsScheduled();

        assertThat(output).contains("batch completed total=28 success=0 failure=28 blocked=0")
                .doesNotContain("positionId=")
                .doesNotContain("assetSymbol=");
    }

    @Test
    void duplicateInitialMonitorRequestsAreCoalescedBeforeTheFirstRun() {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, true);

        scheduler.requestInitialMonitor(31L, 41L);
        scheduler.requestInitialMonitor(31L, 41L);

        assertThat(scheduler.pendingInitialMonitorCount()).isEqualTo(1);
        scheduler.monitorInitialRequestsScheduled();
        assertThat(scheduler.pendingInitialMonitorCount()).isZero();
        verify(service).monitorUserPositionForUser(31L, 41L);
    }
}
