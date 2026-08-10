package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguredWatchlistAssetSource implements WatchlistAssetSource {
    private final WatchlistProperties properties;
    private final ProviderSymbolMappingRegistry registry;
    private final AssetPoolService assetPoolService;

    public ConfiguredWatchlistAssetSource(WatchlistProperties properties, ProviderSymbolMappingRegistry registry) {
        this(properties, registry, null);
    }

    @Autowired
    public ConfiguredWatchlistAssetSource(WatchlistProperties properties,
                                          ProviderSymbolMappingRegistry registry,
                                          AssetPoolService assetPoolService) {
        this.properties = properties;
        this.registry = registry;
        this.assetPoolService = assetPoolService;
    }

    @Override
    public List<CanonicalInstrumentId> currentWatchlist() {
        if (properties.getMaxAssets() <= 0) return List.of();
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
