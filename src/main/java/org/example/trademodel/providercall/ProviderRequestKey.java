package org.example.trademodel.providercall;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.Locale;
import java.util.Objects;

public record ProviderRequestKey(
        String provider,
        ProviderDatasetType datasetType,
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        String timeframe,
        String timeBucket,
        String sourceVersion
) {
    public ProviderRequestKey {
        provider = normalizeRequired(provider, "provider");
        datasetType = Objects.requireNonNull(datasetType, "datasetType");
        canonicalInstrumentId = Objects.requireNonNull(canonicalInstrumentId, "canonicalInstrumentId");
        providerSymbol = normalizeRequired(providerSymbol, "providerSymbol");
        timeframe = normalizeRequired(timeframe, "timeframe");
        timeBucket = normalizeRequired(timeBucket, "timeBucket");
        sourceVersion = normalizeRequired(sourceVersion, "sourceVersion");
    }

    public String canonical() {
        return String.join("|", provider, datasetType.name(), canonicalInstrumentId.canonical(),
                providerSymbol, timeframe, timeBucket, sourceVersion);
    }

    public String symbol() {
        return providerSymbol;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
