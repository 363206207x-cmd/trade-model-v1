package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.profile.FrequencyMatrixVersionService;
import org.example.trademodel.providercall.profile.ProviderCallProfileResolver;
import org.example.trademodel.providercall.profile.ProviderDueTimePolicy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class ScanUniverseResolver {
    private final ProviderCallProperties properties;
    private final AssetPriorityResolver priorityResolver;
    private final ProviderCallProfileResolver profileResolver;
    private final ProviderDueTimePolicy dueTimePolicy;
    private final FrequencyMatrixVersionService versionService;

    public ScanUniverseResolver(ProviderCallProperties properties,
                                AssetPriorityResolver priorityResolver,
                                ProviderCallProfileResolver profileResolver,
                                ProviderDueTimePolicy dueTimePolicy,
                                FrequencyMatrixVersionService versionService) {
        this.properties = properties;
        this.priorityResolver = priorityResolver;
        this.profileResolver = profileResolver;
        this.dueTimePolicy = dueTimePolicy;
        this.versionService = versionService;
    }

    public List<ScanPlanItem> resolve(ScanUniverseInput input) {
        List<PrioritizedAsset> assets = priorityResolver.resolve(
                input.watchlistAssets().stream().limit(properties.getMaxWatchlistAssets()).toList(),
                input.positions(),
                input.candidateAssets().stream().limit(properties.getMaxCandidateAssets()).toList(),
                input.discoveryAssets().stream().limit(properties.getMaxDiscoveryAssets()).toList());
        String frequencyVersion = versionService.currentVersion();
        List<ScanPlanItem> result = new ArrayList<>(assets.size());
        for (PrioritizedAsset asset : assets) {
            RuntimeScanProfile runtimeEscalation = input.symbolEscalations().get(asset.canonicalInstrumentId());
            String runtimeReason = input.escalationReasons().get(asset.canonicalInstrumentId());
            ProviderCallProfileResolver.ProfileResolution profile = profileResolver.resolve(input.baseProfile(),
                    asset.priority(), RuntimeScanProfile.max(input.automaticProfile(), runtimeEscalation),
                    runtimeReason, null);
            result.add(toPlan(asset, profile, input, frequencyVersion));
        }
        return List.copyOf(result);
    }

    private ScanPlanItem toPlan(PrioritizedAsset asset,
                                ProviderCallProfileResolver.ProfileResolution profile,
                                ScanUniverseInput input,
                                String frequencyVersion) {
        RuntimeScanProfile effective = profile.effectiveProfile();
        Instant priceDue = dueAt(input, asset, ProviderDatasetType.PRICE, effective);
        Instant derivativesDue = dueAt(input, asset, ProviderDatasetType.DERIVATIVES, effective);
        Instant ohlcvDue = dueAt(input, asset, ProviderDatasetType.OHLCV, effective);
        Instant externalDue = dueAt(input, asset, ProviderDatasetType.EXTERNAL_CONTEXT, effective);
        Instant analysisDue = analysisDueAt(input, asset, effective);
        Set<ProviderDatasetType> due = EnumSet.noneOf(ProviderDatasetType.class);
        if (!priceDue.isAfter(input.now())) due.add(ProviderDatasetType.PRICE);
        if (!derivativesDue.isAfter(input.now())) due.add(ProviderDatasetType.DERIVATIVES);
        if (!ohlcvDue.isAfter(input.now())) due.add(ProviderDatasetType.OHLCV);
        if (!externalDue.isAfter(input.now())) due.add(ProviderDatasetType.EXTERNAL_CONTEXT);
        return new ScanPlanItem(asset.canonicalInstrumentId(), asset.providerSymbol(), asset.priority(), due,
                priceDue, ohlcvDue, derivativesDue, externalDue, analysisDue, profile.baseProfile(), effective,
                profile.reasonCodes(), frequencyVersion);
    }

    private Instant dueAt(ScanUniverseInput input,
                          PrioritizedAsset asset,
                          ProviderDatasetType dataset,
                          RuntimeScanProfile profile) {
        Instant previous = input.lastRefreshes().get(new ScanUniverseInput.DatasetRefreshKey(
                asset.canonicalInstrumentId(), dataset));
        return dueTimePolicy.dueAt(previous, input.now(), profile, asset.priority(), dataset);
    }

    private Instant analysisDueAt(ScanUniverseInput input,
                                  PrioritizedAsset asset,
                                  RuntimeScanProfile profile) {
        Instant previous = input.lastRefreshes().get(new ScanUniverseInput.DatasetRefreshKey(
                asset.canonicalInstrumentId(), ProviderDatasetType.AI_REVIEW));
        return previous == null ? input.now()
                : previous.plusSeconds(properties.fullAnalysisDebounceSeconds(profile));
    }
}
