package org.example.trademodel.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

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
