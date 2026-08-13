package org.example.trademodel.dto.assetpool;

import java.time.LocalDateTime;

public record AssetPoolAssetDTO(
        Long assetId,
        String symbol,
        String displayName,
        String marketType,
        String quoteAsset,
        boolean focusEnabled,
        int sortOrder,
        String sourceType,
        Long poolItemId,
        Long userId,
        String name,
        String source,
        String watchStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version,
        String extJson) {

    public AssetPoolAssetDTO(Long assetId,
                             String symbol,
                             String displayName,
                             String marketType,
                             String quoteAsset,
                             boolean focusEnabled,
                             int sortOrder,
                             String sourceType) {
        this(assetId, symbol, displayName, marketType, quoteAsset, focusEnabled, sortOrder, sourceType,
                assetId, null, displayName, sourceType, "OBSERVING", null, null, 1, null);
    }
}
