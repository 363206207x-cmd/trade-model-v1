package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.dto.assetpool.MarketAssetDTO;

import java.util.List;

public interface MarketAssetCatalog {
    List<MarketAssetDTO> search(String query, int limit);

    MarketAssetDTO requireTradable(String symbol);
}
