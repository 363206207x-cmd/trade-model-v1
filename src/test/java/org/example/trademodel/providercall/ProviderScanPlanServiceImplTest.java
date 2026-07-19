package org.example.trademodel.providercall;

import org.example.trademodel.providercall.scan.ProviderScanPlanServiceImpl;
import org.example.trademodel.providercall.scan.ProviderScanUniverseSource;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.scan.ScanUniverseInput;
import org.example.trademodel.providercall.scan.ScanUniverseResolver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderScanPlanServiceImplTest {

    @Test
    void currentPlanIsPureReadOnly() {
        ProviderScanUniverseSource source = mock(ProviderScanUniverseSource.class);
        ScanUniverseResolver resolver = mock(ScanUniverseResolver.class);
        ScanUniverseInput input = input();
        List<ScanPlanItem> expected = List.of();
        when(source.currentUniverse()).thenReturn(input);
        when(resolver.resolve(input)).thenReturn(expected);
        ProviderScanPlanServiceImpl service = new ProviderScanPlanServiceImpl(source, resolver);

        assertThat(service.currentPlan()).isSameAs(expected);

        verify(source).currentUniverse();
        verify(source, never()).evaluateUniverseForExecution(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void planForExecutionUsesExplicitMutatingUniversePath() {
        ProviderScanUniverseSource source = mock(ProviderScanUniverseSource.class);
        ScanUniverseResolver resolver = mock(ScanUniverseResolver.class);
        ScanUniverseInput input = input();
        List<ScanPlanItem> expected = List.of();
        when(source.evaluateUniverseForExecution("scan-cycle-1")).thenReturn(input);
        when(resolver.resolve(input)).thenReturn(expected);
        ProviderScanPlanServiceImpl service = new ProviderScanPlanServiceImpl(source, resolver);

        assertThat(service.planForExecution("scan-cycle-1")).isSameAs(expected);

        verify(source).evaluateUniverseForExecution("scan-cycle-1");
        verify(source, never()).currentUniverse();
    }

    private static ScanUniverseInput input() {
        return new ScanUniverseInput(List.of(), List.of(), List.of(), List.of(), UserScanProfile.AUTO,
                RuntimeScanProfile.LOW, Map.of(), Map.of(), Map.of(),
                Instant.parse("2026-07-19T00:00:00Z"));
    }
}
