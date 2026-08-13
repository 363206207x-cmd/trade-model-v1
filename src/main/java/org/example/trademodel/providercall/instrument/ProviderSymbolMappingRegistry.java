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
        if (mapping == null) {
            mapping = dynamicBinanceUsdt(provider, providerSymbol, marketType);
        }
        return mapping;
    }

    public ProviderSymbolMapping resolve(String provider, CanonicalInstrumentId canonicalInstrumentId) {
        ProviderSymbolMapping mapping = byCanonical.get(new CanonicalLookupKey(provider, canonicalInstrumentId));
        if (mapping == null) {
            mapping = dynamicBinanceUsdt(provider, canonicalInstrumentId);
        }
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
        if (matches.isEmpty()) {
            return dynamicBinanceUsdt("BINANCE", configuredSymbol, marketType).canonicalInstrumentId();
        }
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

    private static ProviderSymbolMapping dynamicBinanceUsdt(String provider,
                                                             String providerSymbol,
                                                             MarketType marketType) {
        String compact = normalizeSymbol(providerSymbol);
        if (!"BINANCE".equalsIgnoreCase(provider) || marketType == null
                || !compact.endsWith("USDT") || compact.length() <= 6) {
            throw new IllegalArgumentException("PROVIDER_SYMBOL_MAPPING_NOT_FOUND");
        }
        String base = compact.substring(0, compact.length() - 4);
        ContractType contractType = marketType == MarketType.SPOT
                ? ContractType.NONE : ContractType.LINEAR;
        return new ProviderSymbolMapping("BINANCE",
                new CanonicalInstrumentId(base, "USDT", marketType, "BINANCE", contractType),
                compact, true, "BINANCE_DYNAMIC_USDT_V1");
    }

    private static ProviderSymbolMapping dynamicBinanceUsdt(String provider,
                                                             CanonicalInstrumentId instrument) {
        if (instrument == null || !"BINANCE".equalsIgnoreCase(instrument.venue())
                || !"USDT".equals(instrument.quoteAsset())) {
            throw new IllegalArgumentException("PROVIDER_SYMBOL_MAPPING_NOT_FOUND");
        }
        return dynamicBinanceUsdt(provider, instrument.baseAsset() + instrument.quoteAsset(),
                instrument.marketType());
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
