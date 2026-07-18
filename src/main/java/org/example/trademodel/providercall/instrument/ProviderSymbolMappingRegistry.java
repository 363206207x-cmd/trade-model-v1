package org.example.trademodel.providercall.instrument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProviderSymbolMappingRegistry {
    private final Map<ProviderLookupKey, ProviderSymbolMapping> byProviderSymbol;
    private final Map<CanonicalLookupKey, ProviderSymbolMapping> byCanonical;
    private final List<ProviderSymbolMapping> mappings;

    @Autowired
    public ProviderSymbolMappingRegistry(InstrumentMappingProperties properties) {
        this(properties.getMappings().stream().map(InstrumentMappingProperties.Mapping::toDomain).toList());
    }

    public ProviderSymbolMappingRegistry(List<ProviderSymbolMapping> configuredMappings) {
        Map<ProviderLookupKey, ProviderSymbolMapping> providerIndex = new LinkedHashMap<>();
        Map<CanonicalLookupKey, ProviderSymbolMapping> canonicalIndex = new LinkedHashMap<>();
        List<ProviderSymbolMapping> enabled = configuredMappings == null ? List.of()
                : configuredMappings.stream().filter(ProviderSymbolMapping::enabled).toList();
        for (ProviderSymbolMapping mapping : enabled) {
            ProviderLookupKey providerKey = new ProviderLookupKey(mapping.provider(),
                    mapping.normalizedProviderSymbol(), mapping.canonicalInstrumentId().marketType());
            CanonicalLookupKey canonicalKey = new CanonicalLookupKey(mapping.provider(), mapping.canonicalInstrumentId());
            if (providerIndex.putIfAbsent(providerKey, mapping) != null) {
                throw new IllegalStateException("duplicate provider symbol mapping: " + providerKey);
            }
            if (canonicalIndex.putIfAbsent(canonicalKey, mapping) != null) {
                throw new IllegalStateException("duplicate canonical provider mapping: " + canonicalKey);
            }
        }
        this.byProviderSymbol = Map.copyOf(providerIndex);
        this.byCanonical = Map.copyOf(canonicalIndex);
        this.mappings = List.copyOf(enabled);
    }

    public ProviderSymbolMapping resolve(String provider, String providerSymbol, MarketType marketType) {
        ProviderSymbolMapping mapping = byProviderSymbol.get(new ProviderLookupKey(provider,
                normalizeSymbol(providerSymbol), marketType));
        if (mapping == null) throw new IllegalArgumentException("PROVIDER_SYMBOL_MAPPING_NOT_FOUND");
        return mapping;
    }

    public ProviderSymbolMapping resolve(String provider, CanonicalInstrumentId canonicalInstrumentId) {
        ProviderSymbolMapping mapping = byCanonical.get(new CanonicalLookupKey(provider, canonicalInstrumentId));
        if (mapping == null) throw new IllegalArgumentException("PROVIDER_SYMBOL_MAPPING_NOT_FOUND");
        return mapping;
    }

    public CanonicalInstrumentId resolveConfiguredInstrument(String configuredSymbol,
                                                             MarketType marketType,
                                                             String venue,
                                                             ContractType contractType) {
        String compact = normalizeSymbol(configuredSymbol);
        List<CanonicalInstrumentId> matches = mappings.stream()
                .map(ProviderSymbolMapping::canonicalInstrumentId)
                .filter(id -> id.marketType() == marketType && id.contractType() == contractType)
                .filter(id -> id.venue().equalsIgnoreCase(venue))
                .filter(id -> (id.baseAsset() + id.quoteAsset()).equals(compact))
                .distinct()
                .toList();
        if (matches.size() != 1) throw new IllegalArgumentException("CONFIGURED_INSTRUMENT_MAPPING_NOT_UNIQUE");
        return matches.get(0);
    }

    public List<ProviderSymbolMapping> snapshot() {
        return mappings;
    }

    private static String normalizeSymbol(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("providerSymbol is required");
        return value.trim().toUpperCase(Locale.ROOT)
                .replace("/", "").replace("-", "").replace("_", "");
    }

    private record ProviderLookupKey(String provider, String symbol, MarketType marketType) {
        private ProviderLookupKey {
            provider = provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
        }
    }

    private record CanonicalLookupKey(String provider, CanonicalInstrumentId canonicalInstrumentId) {
        private CanonicalLookupKey {
            provider = provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
        }
    }
}
