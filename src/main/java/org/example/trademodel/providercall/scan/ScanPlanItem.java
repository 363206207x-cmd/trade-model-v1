package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record ScanPlanItem(
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        AssetPriority effectivePriority,
        Set<ProviderDatasetType> dueDatasets,
        Instant priceDueAt,
        Instant ohlcvDueAt,
        Instant derivativesDueAt,
        Instant externalContextDueAt,
        Instant analysisDueAt,
        UserScanProfile baseProfile,
        RuntimeScanProfile effectiveProfile,
        List<String> profileReasonCodes,
        String frequencyMatrixVersion
) {
    public ScanPlanItem {
        dueDatasets = dueDatasets == null ? Set.of() : Set.copyOf(dueDatasets);
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
        if (frequencyMatrixVersion == null || frequencyMatrixVersion.isBlank()) {
            throw new IllegalArgumentException("frequencyMatrixVersion is required");
        }
    }

    public String symbol() {
        return providerSymbol;
    }

    public String escalationReason() {
        return profileReasonCodes.isEmpty() ? "PROFILE_REASON_UNAVAILABLE" : profileReasonCodes.get(0);
    }
}
