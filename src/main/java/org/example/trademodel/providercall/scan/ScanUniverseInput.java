package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ScanUniverseInput(
        List<CanonicalInstrumentId> watchlistAssets,
        List<PositionScanAsset> positions,
        List<CanonicalInstrumentId> candidateAssets,
        List<CanonicalInstrumentId> discoveryAssets,
        UserScanProfile baseProfile,
        RuntimeScanProfile automaticProfile,
        Map<CanonicalInstrumentId, RuntimeScanProfile> symbolEscalations,
        Map<CanonicalInstrumentId, String> escalationReasons,
        Map<DatasetRefreshKey, Instant> lastRefreshes,
        Instant now
) {
    public ScanUniverseInput {
        watchlistAssets = watchlistAssets == null ? List.of() : List.copyOf(watchlistAssets);
        positions = positions == null ? List.of() : List.copyOf(positions);
        candidateAssets = candidateAssets == null ? List.of() : List.copyOf(candidateAssets);
        discoveryAssets = discoveryAssets == null ? List.of() : List.copyOf(discoveryAssets);
        symbolEscalations = symbolEscalations == null ? Map.of() : Map.copyOf(symbolEscalations);
        escalationReasons = escalationReasons == null ? Map.of() : Map.copyOf(escalationReasons);
        lastRefreshes = lastRefreshes == null ? Map.of() : Map.copyOf(lastRefreshes);
        if (now == null) throw new IllegalArgumentException("now is required");
    }

    public record DatasetRefreshKey(CanonicalInstrumentId canonicalInstrumentId,
                                    ProviderDatasetType datasetType) {
    }
}
