package org.example.trademodel.providercall;

import java.util.Locale;
import java.util.Objects;

public record ProviderRequestKey(
        String provider,
        ProviderDatasetType datasetType,
        String symbol,
        String timeframe,
        String timeBucket
) {
    public ProviderRequestKey {
        provider = normalizeRequired(provider, "provider");
        datasetType = Objects.requireNonNull(datasetType, "datasetType");
        symbol = normalizeRequired(symbol, "symbol");
        timeframe = normalizeRequired(timeframe, "timeframe");
        timeBucket = normalizeRequired(timeBucket, "timeBucket");
    }

    public String canonical() {
        return String.join("|", provider, datasetType.name(), symbol, timeframe, timeBucket);
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
