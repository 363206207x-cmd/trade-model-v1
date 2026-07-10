package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;

import java.time.Instant;
import java.util.Set;

public record ScanPlanItem(
        String symbol,
        AssetPriority effectivePriority,
        Set<ProviderDatasetType> dueDatasets,
        Instant priceDueAt,
        Instant ohlcvDueAt,
        Instant derivativesDueAt,
        Instant externalContextDueAt,
        Instant analysisDueAt,
        RuntimeScanProfile effectiveProfile,
        String escalationReason
) {
    public ScanPlanItem {
        dueDatasets = dueDatasets == null ? Set.of() : Set.copyOf(dueDatasets);
    }
}
