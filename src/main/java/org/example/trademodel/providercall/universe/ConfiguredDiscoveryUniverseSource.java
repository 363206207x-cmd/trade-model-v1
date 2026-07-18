package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguredDiscoveryUniverseSource implements DiscoveryUniverseSource {
    private final DiscoveryProperties properties;
    private final ProviderSymbolMappingRegistry registry;

    public ConfiguredDiscoveryUniverseSource(DiscoveryProperties properties,
                                             ProviderSymbolMappingRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @Override
    public List<CanonicalInstrumentId> currentDiscoveryUniverse() {
        if (!properties.isEnabled() || properties.getMaxAssets() <= 0) return List.of();
        return properties.getSymbols().stream()
                .map(symbol -> registry.resolveConfiguredInstrument(symbol, properties.getMarketType(),
                        properties.getVenue(), properties.getContractType()))
                .distinct()
                .limit(properties.getMaxAssets())
                .toList();
    }
}
