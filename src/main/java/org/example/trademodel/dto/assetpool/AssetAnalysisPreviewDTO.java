package org.example.trademodel.dto.assetpool;

import org.example.trademodel.vo.AssetAnalysisVO;

public record AssetAnalysisPreviewDTO(
        String symbol,
        String timeframe,
        String analysisId,
        String traceId,
        String status,
        String reasonCode,
        boolean previewOnly,
        boolean poolMutationPerformed,
        boolean opportunityPersisted,
        boolean candidatePersisted,
        boolean finalPlanPersisted,
        AssetAnalysisVO analysis) {
}
