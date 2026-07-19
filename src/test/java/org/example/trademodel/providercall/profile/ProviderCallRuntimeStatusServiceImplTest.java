package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCircuitBreaker;
import org.example.trademodel.providercall.ProviderCircuitState;
import org.example.trademodel.providercall.ProviderConcurrencyGuard;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderHealthRegistry;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.ProviderCallTestFixtures;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.candidate.AutoCandidateRegistry;
import org.example.trademodel.providercall.notification.NotificationEligibilityProperties;
import org.example.trademodel.providercall.notification.OpportunityScope;
import org.example.trademodel.providercall.scan.ProviderScanPlanService;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.universe.DiscoveryUniverseSource;
import org.example.trademodel.providercall.universe.WatchlistAssetSource;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderCallRuntimeStatusServiceImplTest {

    @Test
    void oneHundredRuntimeStatusQueriesRemainOnReadOnlyPlanPath() {
        Fixture fixture = fixture();

        for (int index = 0; index < 100; index++) {
            assertThat(fixture.service.currentStatus().effectiveProfilesBySymbol())
                    .containsEntry("BTCUSDT", RuntimeScanProfile.HIGH);
        }

        verify(fixture.planService, times(100)).currentPlan();
        verify(fixture.planService, never()).planForExecution(anyString());
        verify(fixture.transitionService, times(100)).current("BTCUSDT", "runtime-status-query");
        verify(fixture.transitionService, never()).evaluate(anyString(), any(), any(), anyString());
    }

    @Test
    void invalidWatchlistConfigDoesNotBreakRuntimeStatus() {
        Fixture fixture = fixture();
        when(fixture.watchlistSource.currentWatchlist())
                .thenThrow(new IllegalArgumentException("invalid optional watchlist configuration"));
        when(fixture.discoverySource.currentDiscoveryUniverse()).thenReturn(List.of(
                ProviderCallTestFixtures.perpetual("ETHUSDT"),
                ProviderCallTestFixtures.perpetual("SOLUSDT")));

        ProviderCallRuntimeStatus status = fixture.service.currentStatus();

        assertThat(status.manualWatchlistCount()).isZero();
        assertThat(status.discoveryPoolCount()).isEqualTo(2);
        assertThat(status.providerBudgets()).isNotEmpty();
        assertThat(status.providerStatuses()).isNotEmpty();
        verify(fixture.planService).currentPlan();
        verify(fixture.planService, never()).planForExecution(anyString());
        verify(fixture.transitionService).current("BTCUSDT", "runtime-status-query");
        verify(fixture.transitionService, never()).evaluate(anyString(), any(), any(), anyString());
    }

    @Test
    void invalidDiscoveryConfigDoesNotBreakRuntimeStatus() {
        Fixture fixture = fixture();
        when(fixture.watchlistSource.currentWatchlist()).thenReturn(List.of(
                ProviderCallTestFixtures.perpetual("ETHUSDT")));
        when(fixture.discoverySource.currentDiscoveryUniverse())
                .thenThrow(new IllegalArgumentException("invalid optional discovery configuration"));

        ProviderCallRuntimeStatus status = fixture.service.currentStatus();

        assertThat(status.manualWatchlistCount()).isOne();
        assertThat(status.discoveryPoolCount()).isZero();
        assertThat(status.effectiveProfilesBySymbol()).containsEntry("BTCUSDT", RuntimeScanProfile.HIGH);
    }

    @Test
    void bothOptionalUniverseSourcesInvalidReturnZeroCounts() {
        Fixture fixture = fixture();
        when(fixture.watchlistSource.currentWatchlist()).thenThrow(new IllegalStateException("watchlist invalid"));
        when(fixture.discoverySource.currentDiscoveryUniverse())
                .thenThrow(new IllegalStateException("discovery invalid"));

        ProviderCallRuntimeStatus status = fixture.service.currentStatus();

        assertThat(status.manualWatchlistCount()).isZero();
        assertThat(status.discoveryPoolCount()).isZero();
        assertThat(status.providerBudgets()).containsKeys("BINANCE", "COINGLASS", "AI", "EXTERNAL_CONTEXT");
        assertThat(status.providerStatuses()).containsKeys("BINANCE", "COINGLASS", "AI", "EXTERNAL_CONTEXT");
        assertThat(status.runtimeProfilesBySymbol()).containsEntry("BTCUSDT", RuntimeScanProfile.HIGH);
    }

    @Test
    void nullOptionalUniverseListsReturnZeroCounts() {
        Fixture fixture = fixture();
        when(fixture.watchlistSource.currentWatchlist()).thenReturn(null);
        when(fixture.discoverySource.currentDiscoveryUniverse()).thenReturn(null);

        ProviderCallRuntimeStatus status = fixture.service.currentStatus();

        assertThat(status.manualWatchlistCount()).isZero();
        assertThat(status.discoveryPoolCount()).isZero();
    }

    @Test
    void repeatedBadUniverseStatusQueriesRemainReadOnly() {
        Fixture fixture = fixture();
        when(fixture.watchlistSource.currentWatchlist()).thenThrow(new IllegalArgumentException("watchlist invalid"));
        when(fixture.discoverySource.currentDiscoveryUniverse())
                .thenThrow(new IllegalArgumentException("discovery invalid"));

        for (int index = 0; index < 100; index++) {
            ProviderCallRuntimeStatus status = fixture.service.currentStatus();
            assertThat(status.manualWatchlistCount()).isZero();
            assertThat(status.discoveryPoolCount()).isZero();
        }

        verify(fixture.planService, times(100)).currentPlan();
        verify(fixture.planService, never()).planForExecution(anyString());
        verify(fixture.transitionService, times(100)).current("BTCUSDT", "runtime-status-query");
        verify(fixture.transitionService, never()).evaluate(anyString(), any(), any(), anyString());
    }

    private static Fixture fixture() {
        ProviderCallProfilePreferenceService preferenceService = mock(ProviderCallProfilePreferenceService.class);
        ProviderScanPlanService planService = mock(ProviderScanPlanService.class);
        ScanProfileTransitionService transitionService = mock(ScanProfileTransitionService.class);
        ProviderCallProperties properties = mock(ProviderCallProperties.class);
        FrequencyMatrixVersionService versionService = mock(FrequencyMatrixVersionService.class);
        ProviderRateBudgetManager budgetManager = mock(ProviderRateBudgetManager.class);
        ProviderCircuitBreaker circuitBreaker = mock(ProviderCircuitBreaker.class);
        ProviderHealthRegistry healthRegistry = mock(ProviderHealthRegistry.class);
        ProviderConcurrencyGuard concurrencyGuard = mock(ProviderConcurrencyGuard.class);
        WatchlistAssetSource watchlistSource = mock(WatchlistAssetSource.class);
        DiscoveryUniverseSource discoverySource = mock(DiscoveryUniverseSource.class);
        AutoCandidateRegistry candidateRegistry = mock(AutoCandidateRegistry.class);
        NotificationEligibilityProperties notificationProperties = mock(NotificationEligibilityProperties.class);
        ScanPlanItem item = planItem();
        when(preferenceService.getBaseProfile()).thenReturn(UserScanProfile.AUTO);
        when(planService.currentPlan()).thenReturn(List.of(item));
        when(transitionService.current("BTCUSDT", "runtime-status-query")).thenReturn(
                new ProfileTransitionResult("BTCUSDT", RuntimeScanProfile.HIGH, RuntimeScanProfile.HIGH,
                        "HIGH_RISK", Instant.parse("2026-07-19T00:00:00Z"),
                        Instant.parse("2026-07-19T00:05:00Z"), "v-test", false, "read"));
        when(versionService.currentVersion()).thenReturn("v-test");
        when(circuitBreaker.state(anyString())).thenReturn(ProviderCircuitState.CLOSED);
        when(budgetManager.state(anyString(), any())).thenAnswer(invocation -> new ProviderBudgetState(
                invocation.getArgument(0), 60, 48, 0.8d, 0.2d, 0, 60,
                null, ProviderCircuitState.CLOSED, null));
        when(healthRegistry.get(anyString(), any())).thenAnswer(invocation ->
                new ProviderHealthRegistry.ProviderHealthSnapshot(invocation.getArgument(0),
                        UnifiedSourceStatus.WAITING_SYNC, ProviderCircuitState.CLOSED,
                        null, null, null));
        when(concurrencyGuard.state()).thenReturn(new ProviderConcurrencyGuard.ConcurrencyState(
                8, 3, 32, 0, 0, 0, 2));
        when(watchlistSource.currentWatchlist()).thenReturn(List.of());
        when(discoverySource.currentDiscoveryUniverse()).thenReturn(List.of());
        when(notificationProperties.getOpportunityScope()).thenReturn(OpportunityScope.WATCHLIST_AND_DISCOVERY);
        ProviderCallRuntimeStatusServiceImpl service = new ProviderCallRuntimeStatusServiceImpl(
                preferenceService, planService, transitionService, properties, versionService,
                budgetManager, circuitBreaker, healthRegistry, concurrencyGuard, watchlistSource,
                discoverySource, candidateRegistry, notificationProperties,
                Clock.fixed(Instant.parse("2026-07-19T00:00:00Z"), ZoneOffset.UTC));
        return new Fixture(service, planService, transitionService, watchlistSource, discoverySource);
    }

    private record Fixture(
            ProviderCallRuntimeStatusServiceImpl service,
            ProviderScanPlanService planService,
            ScanProfileTransitionService transitionService,
            WatchlistAssetSource watchlistSource,
            DiscoveryUniverseSource discoverySource) {
    }

    private static ScanPlanItem planItem() {
        Instant now = Instant.parse("2026-07-19T00:00:00Z");
        return new ScanPlanItem(ProviderCallTestFixtures.perpetual("BTCUSDT"), "BTCUSDT",
                AssetPriority.P0_POSITION, Set.of(ProviderDatasetType.PRICE), now, now, now, now, now,
                UserScanProfile.AUTO, RuntimeScanProfile.HIGH, List.of("HIGH_RISK"), "v-test");
    }
}
