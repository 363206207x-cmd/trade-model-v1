package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderDatasetType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderRefreshStateRegistry {
    private final Map<Key, ProviderRefreshObservation> observations = new ConcurrentHashMap<>();

    public void record(ProviderRefreshObservation observation) {
        if (observation != null && observation.symbol() != null && observation.datasetType() != null) {
            observations.put(new Key(observation.symbol(), observation.datasetType()), observation);
        }
    }

    public ProviderRefreshObservation get(String symbol, ProviderDatasetType datasetType) {
        return observations.get(new Key(symbol, datasetType));
    }

    public Map<ScanUniverseInput.DatasetRefreshKey, Instant> lastAttempts() {
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> result = new LinkedHashMap<>();
        observations.forEach((key, value) -> {
            if (value.attemptedAt() != null) {
                result.put(new ScanUniverseInput.DatasetRefreshKey(key.symbol(), key.datasetType()), value.attemptedAt());
            }
        });
        return Map.copyOf(result);
    }

    private record Key(String symbol, ProviderDatasetType datasetType) {
    }
}
