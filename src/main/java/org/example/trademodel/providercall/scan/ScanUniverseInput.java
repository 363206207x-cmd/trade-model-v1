package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ScanUniverseInput(
        List<String> coreAssets,
        List<PositionScanAsset> positions,
        List<String> candidateAssets,
        List<String> poolAssets,
        UserScanProfile baseProfile,
        RuntimeScanProfile automaticProfile,
        RuntimeScanProfile positionMonitorProfile,
        RuntimeScanProfile poolProfile,
        Map<String, RuntimeScanProfile> symbolEscalations,
        Map<String, String> escalationReasons,
        Map<DatasetRefreshKey, Instant> lastRefreshes,
        Instant now
) {
    public ScanUniverseInput {
        coreAssets = coreAssets == null ? List.of() : List.copyOf(coreAssets);
        positions = positions == null ? List.of() : List.copyOf(positions);
        candidateAssets = candidateAssets == null ? List.of() : List.copyOf(candidateAssets);
        poolAssets = poolAssets == null ? List.of() : List.copyOf(poolAssets);
        symbolEscalations = symbolEscalations == null ? Map.of() : Map.copyOf(symbolEscalations);
        escalationReasons = escalationReasons == null ? Map.of() : Map.copyOf(escalationReasons);
        lastRefreshes = lastRefreshes == null ? Map.of() : Map.copyOf(lastRefreshes);
        now = now == null ? Instant.now() : now;
    }

    public record DatasetRefreshKey(String symbol, ProviderDatasetType datasetType) {}
}
