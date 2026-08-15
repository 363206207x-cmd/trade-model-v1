package org.example.trademodel.providercall.instrument;

import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.time.Instant;

public record ProviderSymbolMapping(
        String provider,
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        boolean enabled,
        String sourceVersion,
        List<String> supportedTimeframes,
        Instant verifiedAt
) {
    public ProviderSymbolMapping(String provider,
                                 CanonicalInstrumentId canonicalInstrumentId,
                                 String providerSymbol,
                                 boolean enabled,
                                 String sourceVersion) {
        this(provider, canonicalInstrumentId, providerSymbol, enabled, sourceVersion,
                List.of("5m", "15m", "1h", "4h"), null);
    }

    public ProviderSymbolMapping {
        provider = required(provider, "provider");
        canonicalInstrumentId = Objects.requireNonNull(canonicalInstrumentId, "canonicalInstrumentId");
        providerSymbol = required(providerSymbol, "providerSymbol");
        sourceVersion = required(sourceVersion, "sourceVersion");
        supportedTimeframes = supportedTimeframes == null ? List.of() : supportedTimeframes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public String normalizedProviderSymbol() {
        return providerSymbol.replace("/", "").replace("-", "").replace("_", "");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
