package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ScanUniverseResolver {
    private final ProviderCallProperties properties;
    private final AssetPriorityResolver priorityResolver;

    public ScanUniverseResolver(ProviderCallProperties properties, AssetPriorityResolver priorityResolver) {
        this.properties = properties;
        this.priorityResolver = priorityResolver;
    }

    public List<ScanPlanItem> resolve(ScanUniverseInput input) {
        List<String> core = bounded(input.coreAssets(), properties.getMaxCoreAssets());
        List<String> candidates = bounded(input.candidateAssets(), properties.getMaxCandidateAssets());
        List<String> pool = bounded(input.poolAssets(), properties.getMaxPoolAssets());
        List<PrioritizedAsset> assets = priorityResolver.resolve(core, input.positions(), candidates, pool);
        List<ScanPlanItem> result = new ArrayList<>(assets.size());
        for (PrioritizedAsset asset : assets) {
            RuntimeScanProfile base = configuredBase(input.baseProfile());
            if (asset.priority() == AssetPriority.P3_POOL && input.poolProfile() != null) {
                base = input.poolProfile();
            }
            RuntimeScanProfile symbolEscalation = input.symbolEscalations().get(asset.symbol());
            RuntimeScanProfile positionFloor = asset.priority() == AssetPriority.P0_POSITION
                    ? input.positionMonitorProfile() : RuntimeScanProfile.LOW;
            RuntimeScanProfile effective = RuntimeScanProfile.max(base, input.automaticProfile(),
                    symbolEscalation, positionFloor);
            if (asset.priority() == AssetPriority.P3_POOL && symbolEscalation != RuntimeScanProfile.EMERGENCY) {
                effective = base;
            }
            result.add(toPlan(asset, effective, input));
        }
        return List.copyOf(result);
    }

    private ScanPlanItem toPlan(PrioritizedAsset asset, RuntimeScanProfile profile, ScanUniverseInput input) {
        int priceSeconds = properties.intervalSeconds(profile, asset.priority(), ProviderDatasetType.PRICE);
        int derivativeSeconds = properties.intervalSeconds(profile, asset.priority(), ProviderDatasetType.DERIVATIVES);
        Instant priceDue = dueAt(input, asset.symbol(), ProviderDatasetType.PRICE, priceSeconds);
        Instant derivativesDue = dueAt(input, asset.symbol(), ProviderDatasetType.DERIVATIVES, derivativeSeconds);
        Instant ohlcvDue = dueAt(input, asset.symbol(), ProviderDatasetType.OHLCV, Math.max(60, priceSeconds));
        Instant externalDue = dueAt(input, asset.symbol(), ProviderDatasetType.EXTERNAL_CONTEXT,
                asset.priority() == AssetPriority.P3_POOL ? 900 : 300);
        Set<ProviderDatasetType> due = EnumSet.noneOf(ProviderDatasetType.class);
        if (!priceDue.isAfter(input.now())) due.add(ProviderDatasetType.PRICE);
        if (!derivativesDue.isAfter(input.now())) due.add(ProviderDatasetType.DERIVATIVES);
        if (!ohlcvDue.isAfter(input.now())) due.add(ProviderDatasetType.OHLCV);
        if (!externalDue.isAfter(input.now())) due.add(ProviderDatasetType.EXTERNAL_CONTEXT);
        String reason = input.escalationReasons().getOrDefault(asset.symbol(),
                asset.priority() == AssetPriority.P0_POSITION ? "ACTIVE_POSITION_SAFETY_FLOOR" : "CONFIGURED_PROFILE");
        return new ScanPlanItem(asset.symbol(), asset.priority(), due, priceDue, ohlcvDue, derivativesDue,
                externalDue, input.now().plusSeconds(properties.getProfiles().getEmergency()
                        .getFullAnalysisDebounceSeconds()), profile, reason);
    }

    private static Instant dueAt(ScanUniverseInput input, String symbol, ProviderDatasetType type, int seconds) {
        Instant previous = input.lastRefreshes().get(new ScanUniverseInput.DatasetRefreshKey(symbol, type));
        return previous == null ? input.now() : previous.plusSeconds(Math.max(1, seconds));
    }

    private static RuntimeScanProfile configuredBase(UserScanProfile profile) {
        if (profile == null || profile == UserScanProfile.AUTO) return RuntimeScanProfile.STANDARD;
        return RuntimeScanProfile.valueOf(profile.name());
    }

    private static List<String> bounded(List<String> source, int max) {
        if (source == null || source.isEmpty() || max <= 0) return List.of();
        return source.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT)).distinct().limit(max).toList();
    }
}
