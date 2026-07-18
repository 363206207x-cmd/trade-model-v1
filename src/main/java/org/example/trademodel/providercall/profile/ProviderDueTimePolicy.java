package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProviderDueTimePolicy {
    private final ProviderCallProperties properties;

    public ProviderDueTimePolicy(ProviderCallProperties properties) {
        this.properties = properties;
    }

    public Instant dueAt(Instant lastRefresh,
                         Instant asOf,
                         RuntimeScanProfile profile,
                         AssetPriority priority,
                         ProviderDatasetType datasetType) {
        if (asOf == null) throw new IllegalArgumentException("asOf is required");
        if (lastRefresh == null) return asOf;
        return lastRefresh.plusSeconds(intervalSeconds(profile, priority, datasetType));
    }

    public boolean isDue(Instant lastRefresh,
                         Instant asOf,
                         RuntimeScanProfile profile,
                         AssetPriority priority,
                         ProviderDatasetType datasetType) {
        return !dueAt(lastRefresh, asOf, profile, priority, datasetType).isAfter(asOf);
    }

    public int intervalSeconds(RuntimeScanProfile profile,
                               AssetPriority priority,
                               ProviderDatasetType datasetType) {
        return properties.intervalSeconds(profile, priority, datasetType);
    }
}
