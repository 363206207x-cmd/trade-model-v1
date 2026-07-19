package org.example.trademodel.providercall;

import org.example.trademodel.providercall.scan.ProviderDatasetRefreshPort;
import org.example.trademodel.providercall.scan.ProviderScanCoordinatorScheduler;
import org.example.trademodel.providercall.scan.ProviderScanPlanService;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderScanCoordinatorSchedulerTest {

    @Test
    void schedulerUsesExecutionPlanOnceForFourDueDatasets() {
        Fixture fixture = fixture(true, true, true);
        ScanPlanItem item = item(Set.of(ProviderDatasetType.PRICE, ProviderDatasetType.OHLCV,
                ProviderDatasetType.DERIVATIVES, ProviderDatasetType.EXTERNAL_CONTEXT));
        when(fixture.planService.planForExecution(anyString())).thenReturn(List.of(item));

        int refreshed = fixture.scheduler.scanOnce();

        assertThat(refreshed).isEqualTo(4);
        verify(fixture.planService, times(1)).planForExecution(
                org.mockito.ArgumentMatchers.startsWith("provider-scan-cycle-"));
        verify(fixture.planService, never()).currentPlan();
        verify(fixture.refreshPort, times(1)).refresh(eq(item), eq(ProviderDatasetType.PRICE));
        verify(fixture.refreshPort, times(1)).refresh(eq(item), eq(ProviderDatasetType.OHLCV));
        verify(fixture.refreshPort, times(1)).refresh(eq(item), eq(ProviderDatasetType.DERIVATIVES));
        verify(fixture.refreshPort, times(1)).refresh(eq(item), eq(ProviderDatasetType.EXTERNAL_CONTEXT));
    }

    @Test
    void schedulerDisabledDoesNotEvaluateProfiles() {
        Fixture fixture = fixture(false, true, true);

        assertThat(fixture.scheduler.scanOnce()).isZero();

        verifyNoInteractions(fixture.planService, fixture.refreshPortProvider, fixture.refreshPort);
    }

    @Test
    void providerCallDisabledDoesNotEvaluateProfiles() {
        Fixture fixture = fixture(true, false, true);

        assertThat(fixture.scheduler.scanOnce()).isZero();

        verifyNoInteractions(fixture.planService, fixture.refreshPortProvider, fixture.refreshPort);
    }

    @Test
    void missingRefreshPortDoesNotEvaluateProfiles() {
        Fixture fixture = fixture(true, true, false);

        assertThat(fixture.scheduler.scanOnce()).isZero();

        verify(fixture.refreshPortProvider).getIfAvailable();
        verifyNoInteractions(fixture.planService, fixture.refreshPort);
    }

    @Test
    void auditFailurePreventsRefreshExecution() {
        Fixture fixture = fixture(true, true, true);
        when(fixture.planService.planForExecution(anyString()))
                .thenThrow(new IllegalStateException("profile transition audit unavailable"));

        assertThatThrownBy(fixture.scheduler::scanOnce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("profile transition audit unavailable");

        verify(fixture.planService, times(1)).planForExecution(anyString());
        verifyNoInteractions(fixture.refreshPort);
    }

    @SuppressWarnings("unchecked")
    private static Fixture fixture(boolean globalEnabled, boolean providerEnabled, boolean portAvailable) {
        ProviderCallProperties properties = new ProviderCallProperties();
        properties.setEnabled(providerEnabled);
        properties.setSchedulerEnabled(true);
        ProviderScanPlanService planService = mock(ProviderScanPlanService.class);
        ObjectProvider<ProviderDatasetRefreshPort> refreshPortProvider = mock(ObjectProvider.class);
        ProviderDatasetRefreshPort refreshPort = mock(ProviderDatasetRefreshPort.class);
        when(refreshPortProvider.getIfAvailable()).thenReturn(portAvailable ? refreshPort : null);
        ProviderScanCoordinatorScheduler scheduler = new ProviderScanCoordinatorScheduler(
                properties, globalEnabled, planService, refreshPortProvider);
        return new Fixture(scheduler, planService, refreshPortProvider, refreshPort);
    }

    private static ScanPlanItem item(Set<ProviderDatasetType> dueDatasets) {
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        return new ScanPlanItem(ProviderCallTestFixtures.perpetual("BTCUSDT"), "BTCUSDT",
                AssetPriority.P0_POSITION, dueDatasets, now, now, now, now, now,
                UserScanProfile.AUTO, RuntimeScanProfile.HIGH, List.of("HIGH_RISK"), "v-test");
    }

    private record Fixture(
            ProviderScanCoordinatorScheduler scheduler,
            ProviderScanPlanService planService,
            ObjectProvider<ProviderDatasetRefreshPort> refreshPortProvider,
            ProviderDatasetRefreshPort refreshPort) {
    }
}
