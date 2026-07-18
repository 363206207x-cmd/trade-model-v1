package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguredWatchlistAssetSource implements WatchlistAssetSource {
    private final WatchlistProperties properties;
    private final ProviderSymbolMappingRegistry registry;

    public ConfiguredWatchlistAssetSource(WatchlistProperties properties, ProviderSymbolMappingRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @Override
    public List<CanonicalInstrumentId> currentWatchlist() {
        if (properties.getMaxAssets() <= 0) return List.of();
        return properties.getSymbols().stream()
                .map(symbol -> registry.resolveConfiguredInstrument(symbol, properties.getMarketType(),
                        properties.getVenue(), properties.getContractType()))
                .distinct()
                .limit(properties.getMaxAssets())
                .toList();
    }
}
