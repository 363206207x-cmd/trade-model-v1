package org.example.trademodel.dto.assetpool;

public record AssetPoolAssetDTO(
        String symbol,
        String displayName,
        String marketType,
        String quoteAsset,
        boolean focusEnabled,
        int sortOrder,
        String sourceType) {
}
