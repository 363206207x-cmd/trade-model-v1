package org.example.trademodel.dto.assetpool;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper PIN_JSON = new ObjectMapper();

    @JsonProperty("homePinOrder")
    public Integer homePinOrder() {
        if (userId == null || userId <= 0 || extJson == null || extJson.isBlank()
                || "TRACKING_STOPPED".equals(watchStatus)) return null;
        try {
            var value = PIN_JSON.readTree(extJson).path("homePinOrder");
            return value.isIntegralNumber() && value.canConvertToInt()
                    && value.intValue() >= 1 && value.intValue() <= 6 ? value.intValue() : null;
        } catch (java.io.IOException | IllegalArgumentException malformedMetadata) {
            return null;
        }
    }

    @JsonProperty("homePinned")
    public boolean homePinned() {
        return homePinOrder() != null;
    }

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
