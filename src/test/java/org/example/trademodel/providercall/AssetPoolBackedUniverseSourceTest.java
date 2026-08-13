package org.example.trademodel.providercall;

import org.example.trademodel.providercall.universe.ConfiguredDiscoveryUniverseSource;
import org.example.trademodel.providercall.universe.ConfiguredWatchlistAssetSource;
import org.example.trademodel.providercall.universe.DiscoveryProperties;
import org.example.trademodel.providercall.universe.WatchlistProperties;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("core-regression")
class AssetPoolBackedUniverseSourceTest {

    @Test
    void watchlistAndDiscoveryUniversesUseOnlyTheActiveAssetPool() {
        AssetPoolService assetPool = mock(AssetPoolService.class);
        when(assetPool.listScanSymbols()).thenReturn(List.of("AAVEUSDT", "LINKUSDT", "SUIUSDT"));
        var registry = ProviderCallTestFixtures.binanceRegistry("AAVEUSDT", "LINKUSDT", "SUIUSDT");

        WatchlistProperties watchlistProperties = new WatchlistProperties();
        watchlistProperties.setMaxAssets(20);
        DiscoveryProperties discoveryProperties = new DiscoveryProperties();
        discoveryProperties.setEnabled(true);
        discoveryProperties.setMaxAssets(20);

        ConfiguredWatchlistAssetSource watchlist = new ConfiguredWatchlistAssetSource(
                watchlistProperties, registry, assetPool);
        ConfiguredDiscoveryUniverseSource discovery = new ConfiguredDiscoveryUniverseSource(
                discoveryProperties, registry, assetPool);

        assertThat(watchlist.currentWatchlist())
                .extracting(item -> item.baseAsset() + item.quoteAsset())
                .containsExactly("AAVEUSDT", "LINKUSDT", "SUIUSDT");
        assertThat(discovery.currentDiscoveryUniverse())
                .extracting(item -> item.baseAsset() + item.quoteAsset())
                .containsExactly("AAVEUSDT", "LINKUSDT", "SUIUSDT");
    }

    @Test
    void disabledDiscoveryDoesNotReadOrInventAssets() {
        AssetPoolService assetPool = mock(AssetPoolService.class);
        DiscoveryProperties properties = new DiscoveryProperties();
        properties.setEnabled(false);
        ConfiguredDiscoveryUniverseSource source = new ConfiguredDiscoveryUniverseSource(
                properties, ProviderCallTestFixtures.binanceRegistry("BTCUSDT"), assetPool);

        assertThat(source.currentDiscoveryUniverse()).isEmpty();
    }
}
