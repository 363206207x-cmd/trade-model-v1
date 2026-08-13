package org.example.trademodel.providercall.universe;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ConfiguredWatchlistAssetSource implements WatchlistAssetSource {
    private final WatchlistProperties properties;
    private final ProviderSymbolMappingRegistry registry;
    private final AssetPoolService assetPoolService;

    public ConfiguredWatchlistAssetSource(WatchlistProperties properties,
                                          ProviderSymbolMappingRegistry registry,
                                          AssetPoolService assetPoolService) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.assetPoolService = Objects.requireNonNull(assetPoolService, "assetPoolService");
    }

    @Override
    public List<CanonicalInstrumentId> currentWatchlist() {
        if (properties.getMaxAssets() <= 0) return List.of();
        return assetPoolService.listScanSymbols().stream()
                .map(symbol -> registry.resolveConfiguredInstrument(symbol, properties.getMarketType(),
                        properties.getVenue(), properties.getContractType()))
                .distinct()
                .limit(properties.getMaxAssets())
                .toList();
    }
}
