package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;

import java.util.List;

public interface AssetPoolService {
    List<AssetPoolAssetDTO> listForUser(Long userId);

    List<String> listFocusSymbols(Long userId, int limit);

    List<String> listScanSymbols();

    List<MarketAssetDTO> searchMarket(String query, int limit);

    AssetPoolAssetDTO addForUser(Long userId, String symbol, boolean focusEnabled);

    void removeForUser(Long userId, String symbol);

    List<AssetPoolAssetDTO> restoreDefaults(Long userId);

    boolean isOpportunitySource(String symbol);

    List<AssetPoolScanResultDTO> scanForUser(Long userId, String timeframe);
}
