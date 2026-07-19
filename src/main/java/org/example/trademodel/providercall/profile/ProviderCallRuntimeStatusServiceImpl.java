package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCircuitBreaker;
import org.example.trademodel.providercall.ProviderConcurrencyGuard;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderHealthRegistry;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.candidate.AutoCandidateRegistry;
import org.example.trademodel.providercall.notification.NotificationEligibilityProperties;
import org.example.trademodel.providercall.scan.ProviderScanPlanService;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.universe.DiscoveryUniverseSource;
import org.example.trademodel.providercall.universe.WatchlistAssetSource;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class ProviderCallRuntimeStatusServiceImpl implements ProviderCallRuntimeStatusService {
    private static final List<String> PROVIDERS = List.of("BINANCE", "COINGLASS", "AI", "EXTERNAL_CONTEXT");

    private final ProviderCallProfilePreferenceService preferenceService;
    private final ProviderScanPlanService scanPlanService;
    private final ScanProfileTransitionService transitionService;
    private final ProviderCallProperties properties;
    private final FrequencyMatrixVersionService versionService;
    private final ProviderRateBudgetManager budgetManager;
    private final ProviderCircuitBreaker circuitBreaker;
    private final ProviderHealthRegistry healthRegistry;
    private final ProviderConcurrencyGuard concurrencyGuard;
    private final WatchlistAssetSource watchlistSource;
    private final DiscoveryUniverseSource discoverySource;
    private final AutoCandidateRegistry candidateRegistry;
    private final NotificationEligibilityProperties notificationProperties;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderCallRuntimeStatusServiceImpl(
            ProviderCallProfilePreferenceService preferenceService,
            ProviderScanPlanService scanPlanService,
            ScanProfileTransitionService transitionService,
            ProviderCallProperties properties,
            FrequencyMatrixVersionService versionService,
            ProviderRateBudgetManager budgetManager,
            ProviderCircuitBreaker circuitBreaker,
            ProviderHealthRegistry healthRegistry,
            ProviderConcurrencyGuard concurrencyGuard,
            WatchlistAssetSource watchlistSource,
            DiscoveryUniverseSource discoverySource,
            AutoCandidateRegistry candidateRegistry,
            NotificationEligibilityProperties notificationProperties) {
        this(preferenceService, scanPlanService, transitionService, properties, versionService,
                budgetManager, circuitBreaker, healthRegistry, concurrencyGuard, watchlistSource,
                discoverySource, candidateRegistry, notificationProperties, Clock.systemUTC());
    }

    ProviderCallRuntimeStatusServiceImpl(
            ProviderCallProfilePreferenceService preferenceService,
            ProviderScanPlanService scanPlanService,
            ScanProfileTransitionService transitionService,
            ProviderCallProperties properties,
            FrequencyMatrixVersionService versionService,
            ProviderRateBudgetManager budgetManager,
            ProviderCircuitBreaker circuitBreaker,
            ProviderHealthRegistry healthRegistry,
            ProviderConcurrencyGuard concurrencyGuard,
            WatchlistAssetSource watchlistSource,
            DiscoveryUniverseSource discoverySource,
            AutoCandidateRegistry candidateRegistry,
            NotificationEligibilityProperties notificationProperties,
            Clock clock) {
        this.preferenceService = preferenceService;
        this.scanPlanService = scanPlanService;
        this.transitionService = transitionService;
        this.properties = properties;
        this.versionService = versionService;
        this.budgetManager = budgetManager;
        this.circuitBreaker = circuitBreaker;
        this.healthRegistry = healthRegistry;
        this.concurrencyGuard = concurrencyGuard;
        this.watchlistSource = watchlistSource;
        this.discoverySource = discoverySource;
        this.candidateRegistry = candidateRegistry;
        this.notificationProperties = notificationProperties;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public ProviderCallRuntimeStatus currentStatus() {
        UserScanProfile base = preferenceService.getBaseProfile();
        RuntimeScanProfile baseRuntime = base == UserScanProfile.AUTO
                ? RuntimeScanProfile.STANDARD : RuntimeScanProfile.valueOf(base.name());
        Map<String, RuntimeScanProfile> runtime = new LinkedHashMap<>();
        Map<String, RuntimeScanProfile> effective = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, List<String>> reasons = new LinkedHashMap<>();
        Map<String, Instant> downgrade = new LinkedHashMap<>();
        for (ScanPlanItem item : scanPlanService.currentPlan()) {
            String symbol = item.providerSymbol();
            ProfileTransitionResult transition = transitionService.current(symbol, "runtime-status-query");
            runtime.put(symbol, transition.effectiveProfile());
            effective.put(symbol, item.effectiveProfile());
            labels.put(symbol, runtimeLabel(item.effectiveProfile()));
            reasons.put(symbol, item.profileReasonCodes());
            if (transition.nextDowngradeEligibleAt() != null) {
                downgrade.put(symbol, transition.nextDowngradeEligibleAt());
            }
        }
        Map<String, ProviderBudgetState> budgets = new LinkedHashMap<>();
        Map<String, ProviderHealthRegistry.ProviderHealthSnapshot> health = new LinkedHashMap<>();
        for (String provider : PROVIDERS) {
            budgets.put(provider, budgetManager.state(provider, circuitBreaker.state(provider)));
            health.put(provider, healthRegistry.get(provider, circuitBreaker.state(provider)));
        }
        return new ProviderCallRuntimeStatus(base,
                ProviderCallProfilePreferenceServiceImpl.label(base), runtime, effective, labels, reasons,
                downgrade, versionService.currentVersion(),
                properties.intervalSeconds(baseRuntime, AssetPriority.P0_POSITION, ProviderDatasetType.PRICE),
                properties.intervalSeconds(baseRuntime, AssetPriority.P1_WATCHLIST, ProviderDatasetType.PRICE),
                properties.intervalSeconds(baseRuntime, AssetPriority.P2_CANDIDATE, ProviderDatasetType.PRICE),
                properties.intervalSeconds(baseRuntime, AssetPriority.P3_DISCOVERY, ProviderDatasetType.PRICE),
                budgets, health, concurrencyGuard.state(),
                safeOptionalUniverseCount(watchlistSource::currentWatchlist),
                candidateRegistry.countAt(clock.instant()),
                safeOptionalUniverseCount(discoverySource::currentDiscoveryUniverse),
                notificationProperties.getOpportunityScope());
    }

    private static int safeOptionalUniverseCount(Supplier<? extends Collection<?>> source) {
        try {
            Collection<?> values = source.get();
            return values == null ? 0 : Math.max(0, values.size());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static String runtimeLabel(RuntimeScanProfile profile) {
        return switch (profile) {
            case LOW -> "低频";
            case STANDARD -> "标准";
            case HIGH -> "高频";
            case EMERGENCY -> "紧急";
        };
    }
}
