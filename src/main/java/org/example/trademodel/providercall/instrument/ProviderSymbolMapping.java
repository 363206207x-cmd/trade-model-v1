package org.example.trademodel.providercall.instrument;

import java.util.Locale;
import java.util.Objects;

public record ProviderSymbolMapping(
        String provider,
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        boolean enabled,
        String sourceVersion
) {
    public ProviderSymbolMapping {
        provider = required(provider, "provider");
        canonicalInstrumentId = Objects.requireNonNull(canonicalInstrumentId, "canonicalInstrumentId");
        providerSymbol = required(providerSymbol, "providerSymbol");
        sourceVersion = required(sourceVersion, "sourceVersion");
    }

    public String normalizedProviderSymbol() {
        return providerSymbol.replace("/", "").replace("-", "").replace("_", "");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
