package org.example.trademodel.dto.assetpool;

import java.time.Instant;

public record AssetPoolScanResultDTO(
        String symbol,
        String analysisId,
        String status,
        String reasonCode,
        Long assetId,
        String provider,
        String state,
        Integer dataQuality,
        String failureReason,
        Instant observedAt
) {
    public AssetPoolScanResultDTO(String symbol, String analysisId, String status, String reasonCode) {
        this(symbol, analysisId, status, reasonCode, null, null, status, null, reasonCode, null);
    }
}
