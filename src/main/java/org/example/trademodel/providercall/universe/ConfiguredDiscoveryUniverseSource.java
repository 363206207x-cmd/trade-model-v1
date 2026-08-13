package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ConfiguredDiscoveryUniverseSource implements DiscoveryUniverseSource {
    private final DiscoveryProperties properties;
    private final ProviderSymbolMappingRegistry registry;
    private final AssetPoolService assetPoolService;

    public ConfiguredDiscoveryUniverseSource(DiscoveryProperties properties,
                                             ProviderSymbolMappingRegistry registry,
                                             AssetPoolService assetPoolService) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.assetPoolService = Objects.requireNonNull(assetPoolService, "assetPoolService");
    }

    @Override
    public List<CanonicalInstrumentId> currentDiscoveryUniverse() {
        if (!properties.isEnabled() || properties.getMaxAssets() <= 0) return List.of();
        return assetPoolService.listScanSymbols().stream()
                .map(symbol -> registry.resolveConfiguredInstrument(symbol, properties.getMarketType(),
                        properties.getVenue(), properties.getContractType()))
                .distinct()
                .limit(properties.getMaxAssets())
                .toList();
    }
}
