package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguredDiscoveryUniverseSource implements DiscoveryUniverseSource {
    private final DiscoveryProperties properties;
    private final ProviderSymbolMappingRegistry registry;
    private final AssetPoolService assetPoolService;

    public ConfiguredDiscoveryUniverseSource(DiscoveryProperties properties,
                                             ProviderSymbolMappingRegistry registry) {
        this(properties, registry, null);
    }

    @Autowired
    public ConfiguredDiscoveryUniverseSource(DiscoveryProperties properties,
                                             ProviderSymbolMappingRegistry registry,
                                             AssetPoolService assetPoolService) {
        this.properties = properties;
        this.registry = registry;
        this.assetPoolService = assetPoolService;
    }

    @Override
    public List<CanonicalInstrumentId> currentDiscoveryUniverse() {
        if (!properties.isEnabled() || properties.getMaxAssets() <= 0) return List.of();
        List<String> symbols = assetPoolService == null
                ? properties.getSymbols() : assetPoolService.listScanSymbols();
        return symbols.stream()
                .map(symbol -> registry.resolveConfiguredInstrument(symbol, properties.getMarketType(),
                        properties.getVenue(), properties.getContractType()))
                .distinct()
                .limit(properties.getMaxAssets())
                .toList();
    }
}
