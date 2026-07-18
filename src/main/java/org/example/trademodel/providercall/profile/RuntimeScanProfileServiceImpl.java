package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCircuitBreaker;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ProviderScanUniverseSource;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.scan.ScanUniverseInput;
import org.example.trademodel.providercall.scan.ScanUniverseResolver;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class RuntimeScanProfileServiceImpl implements RuntimeScanProfileService {
    private final ProviderScanUniverseSource universeSource;
    private final ScanUniverseResolver resolver;
    private final ScanProfileTransitionService transitionService;
    private final ProviderCallProperties properties;
    private final ProviderRefreshStateRegistry refreshRegistry;
    private final ProviderRateBudgetManager budgetManager;
    private final ProviderCircuitBreaker circuitBreaker;

    public RuntimeScanProfileServiceImpl(ProviderScanUniverseSource universeSource,
                                         ScanUniverseResolver resolver,
                                         ScanProfileTransitionService transitionService,
                                         ProviderCallProperties properties,
                                         ProviderRefreshStateRegistry refreshRegistry,
                                         ProviderRateBudgetManager budgetManager,
                                         ProviderCircuitBreaker circuitBreaker) {
        this.universeSource = universeSource;
        this.resolver = resolver;
        this.transitionService = transitionService;
        this.properties = properties;
        this.refreshRegistry = refreshRegistry;
        this.budgetManager = budgetManager;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public RuntimeScanProfileResponse get(String symbol) {
        String normalized = requiredSymbol(symbol);
        ScanUniverseInput input = universeSource.currentUniverse();
        ScanPlanItem item = resolver.resolve(input).stream()
                .filter(candidate -> normalized.equals(candidate.symbol())).findFirst().orElse(null);
        ProfileTransitionResult transition = transitionService.current(normalized,
                "runtime-profile-" + UUID.randomUUID());
        RuntimeScanProfile profile = item == null ? transition.effectiveProfile() : item.effectiveProfile();
        AssetPriority priority = item == null ? AssetPriority.P3_DISCOVERY : item.effectivePriority();
        String reason = item == null ? transition.effectiveReason() : item.escalationReason();
        return new RuntimeScanProfileResponse(normalized, input.baseProfile(), profile, priority, reason,
                transition.effectiveSince(), transition.nextDowngradeEligibleAt(),
                properties.intervalSeconds(profile, priority, ProviderDatasetType.PRICE),
                properties.intervalSeconds(profile, priority, ProviderDatasetType.DERIVATIVES),
                refreshRegistry.findByProviderSymbol(normalized, ProviderDatasetType.PRICE),
                refreshRegistry.findByProviderSymbol(normalized, ProviderDatasetType.DERIVATIVES),
                budgetManager.state("BINANCE", circuitBreaker.state("BINANCE")));
    }

    private static String requiredSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
