package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderRefreshStateRegistry {
    private final Map<Key, ProviderRefreshObservation> observations = new ConcurrentHashMap<>();

    public void record(ProviderRefreshObservation observation) {
        if (observation != null && observation.canonicalInstrumentId() != null
                && observation.datasetType() != null) {
            observations.put(new Key(observation.canonicalInstrumentId(), observation.datasetType(),
                    observation.timeframe()), observation);
        }
    }

    public ProviderRefreshObservation get(CanonicalInstrumentId instrument, ProviderDatasetType datasetType) {
        return observations.values().stream()
                .filter(item -> item.canonicalInstrumentId().equals(instrument))
                .filter(item -> item.datasetType() == datasetType)
                .max(java.util.Comparator.comparing(ProviderRefreshObservation::attemptedAt,
                        java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .orElse(null);
    }

    public ProviderRefreshObservation findByProviderSymbol(String symbol, ProviderDatasetType datasetType) {
        if (symbol == null) return null;
        return observations.values().stream()
                .filter(item -> item.datasetType() == datasetType)
                .filter(item -> symbol.equalsIgnoreCase(item.providerSymbol()))
                .findFirst().orElse(null);
    }

    public Map<ScanUniverseInput.DatasetRefreshKey, Instant> lastAttempts() {
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> result = new LinkedHashMap<>();
        observations.forEach((key, value) -> {
            if (value.attemptedAt() != null) {
                result.merge(new ScanUniverseInput.DatasetRefreshKey(key.canonicalInstrumentId(), key.datasetType()),
                        value.attemptedAt(), (left, right) -> left.isAfter(right) ? left : right);
            }
        });
        return Map.copyOf(result);
    }

    public Map<String, ProviderRefreshObservation> snapshot() {
        Map<String, ProviderRefreshObservation> result = new LinkedHashMap<>();
        observations.forEach((key, value) -> result.put(
                key.canonicalInstrumentId().canonical() + "|" + key.datasetType() + "|" + key.timeframe(), value));
        return Map.copyOf(result);
    }

    private record Key(CanonicalInstrumentId canonicalInstrumentId, ProviderDatasetType datasetType,
                       String timeframe) {
    }
}
