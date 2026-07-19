package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCallTestFixtures;
import org.example.trademodel.providercall.ProviderCircuitBreaker;
import org.example.trademodel.providercall.ProviderCircuitState;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ProviderScanUniverseSource;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.scan.ScanUniverseInput;
import org.example.trademodel.providercall.scan.ScanUniverseResolver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeScanProfileServiceImplTest {

    @Test
    void singleAssetRuntimeProfileQueryUsesReadOnlyUniverseAndTransitionState() {
        ProviderScanUniverseSource source = mock(ProviderScanUniverseSource.class);
        ScanUniverseResolver resolver = mock(ScanUniverseResolver.class);
        ScanProfileTransitionService transitionService = mock(ScanProfileTransitionService.class);
        ProviderCallProperties properties = mock(ProviderCallProperties.class);
        ProviderRateBudgetManager budgetManager = mock(ProviderRateBudgetManager.class);
        ProviderCircuitBreaker circuitBreaker = mock(ProviderCircuitBreaker.class);
        ScanUniverseInput input = input();
        ScanPlanItem item = item();
        when(source.currentUniverse()).thenReturn(input);
        when(resolver.resolve(input)).thenReturn(List.of(item));
        when(transitionService.current("BTCUSDT", "runtime-profile-query")).thenReturn(
                new ProfileTransitionResult("BTCUSDT", RuntimeScanProfile.HIGH, RuntimeScanProfile.HIGH,
                        "HIGH_RISK", Instant.parse("2026-07-19T00:00:00Z"), null,
                        "v-test", false, "read"));
        when(circuitBreaker.state("BINANCE")).thenReturn(ProviderCircuitState.CLOSED);
        when(budgetManager.state("BINANCE", ProviderCircuitState.CLOSED)).thenReturn(new ProviderBudgetState(
                "BINANCE", 60, 48, 0.8d, 0.2d, 0, 60,
                null, ProviderCircuitState.CLOSED, null));
        RuntimeScanProfileServiceImpl service = new RuntimeScanProfileServiceImpl(source, resolver,
                transitionService, properties, new ProviderRefreshStateRegistry(), budgetManager, circuitBreaker);

        RuntimeScanProfileResponse response = service.get("BTCUSDT");

        assertThat(response.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
        assertThat(response.effectiveReason()).isEqualTo("HIGH_RISK");
        verify(source).currentUniverse();
        verify(source, never()).evaluateUniverseForExecution(anyString());
        verify(transitionService).current("BTCUSDT", "runtime-profile-query");
        verify(transitionService, never()).evaluate(anyString(), any(), any(), anyString());
    }

    private static ScanUniverseInput input() {
        return new ScanUniverseInput(List.of(), List.of(), List.of(), List.of(), UserScanProfile.AUTO,
                RuntimeScanProfile.LOW, Map.of(), Map.of(), Map.of(),
                Instant.parse("2026-07-19T00:00:00Z"));
    }

    private static ScanPlanItem item() {
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        return new ScanPlanItem(ProviderCallTestFixtures.perpetual("BTCUSDT"), "BTCUSDT",
                AssetPriority.P0_POSITION, Set.of(ProviderDatasetType.PRICE), now, now, now, now, now,
                UserScanProfile.AUTO, RuntimeScanProfile.HIGH, List.of("HIGH_RISK"), "v-test");
    }
}
