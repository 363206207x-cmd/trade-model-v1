package org.example.trademodel.service.watchlistsource;

import org.example.trademodel.dto.assetpool.AssetPoolAssetDTO;
import org.example.trademodel.dto.assetpool.AssetPoolScanResultDTO;
import org.example.trademodel.dto.assetpool.AssetAnalysisPreviewDTO;
import org.example.trademodel.dto.assetpool.MarketAssetDTO;

import java.util.List;

public interface AssetPoolService {
    List<AssetPoolAssetDTO> listForUser(Long userId);

    List<AssetPoolAssetDTO> listSystemDefaults();

    List<String> listFocusSymbols(Long userId, int limit);

    List<String> listScanSymbols();

    default List<AssetPoolScanTarget> listScanTargets() {
        List<String> symbols = listScanSymbols();
        return symbols == null ? List.of() : symbols.stream()
                .map(AssetPoolScanTarget::system)
                .toList();
    }

    List<MarketAssetDTO> searchMarket(String query, int limit);

    AssetAnalysisPreviewDTO analyzePreviewForUser(Long userId, String symbol, String timeframe);

    AssetPoolAssetDTO addForUser(Long userId, String symbol, boolean focusEnabled);

    List<AssetPoolAssetDTO> addManyForUser(Long userId, List<String> symbols, boolean focusEnabled);

    void removeForUser(Long userId, String symbol);

    void removeManyForUser(Long userId, List<String> symbols);

    List<AssetPoolAssetDTO> restoreDefaults(Long userId);

    boolean isOpportunitySource(String ownerType, Long ownerId, Long assetId, String symbol);

    default Long resolvePoolItemId(String ownerType, Long ownerId, Long assetId, String symbol) {
        return assetId;
    }

    List<AssetPoolScanResultDTO> scanForUser(Long userId, String timeframe);

    List<AssetPoolScanResultDTO> scanSelectedForUser(Long userId, List<String> symbols, String timeframe);
}
