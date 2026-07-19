package org.example.trademodel.providercall;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.Locale;
import java.util.Objects;

/** Stable snapshot identity. Refresh cadence and caller TTL are deliberately excluded. */
public record ProviderSnapshotKey(
        String provider,
        ProviderDatasetType datasetType,
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        String timeframe,
        String sourceVersion
) {
    public ProviderSnapshotKey {
        provider = normalizeRequired(provider, "provider");
        datasetType = Objects.requireNonNull(datasetType, "datasetType");
        canonicalInstrumentId = Objects.requireNonNull(canonicalInstrumentId, "canonicalInstrumentId");
        providerSymbol = normalizeRequired(providerSymbol, "providerSymbol");
        timeframe = normalizeRequired(timeframe, "timeframe");
        sourceVersion = normalizeRequired(sourceVersion, "sourceVersion");
    }

    public String canonical() {
        return String.join("|", provider, datasetType.name(), canonicalInstrumentId.canonical(),
                providerSymbol, timeframe, sourceVersion);
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
