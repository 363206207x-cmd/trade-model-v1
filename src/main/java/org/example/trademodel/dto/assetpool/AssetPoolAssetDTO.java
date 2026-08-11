package org.example.trademodel.dto.assetpool;

public record AssetPoolAssetDTO(
        Long assetId,
        String symbol,
        String displayName,
        String marketType,
        String quoteAsset,
        boolean focusEnabled,
        int sortOrder,
        String sourceType) {
}
