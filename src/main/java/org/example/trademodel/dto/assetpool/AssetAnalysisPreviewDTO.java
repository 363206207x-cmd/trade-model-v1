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
        AssetAnalysisVO analysis,
        String taskId,
        String taskState,
        String taskStage) {

    public AssetAnalysisPreviewDTO(String symbol, String timeframe, String analysisId, String traceId,
                                   String status, String reasonCode, boolean previewOnly,
                                   boolean poolMutationPerformed, boolean opportunityPersisted,
                                   boolean candidatePersisted, boolean finalPlanPersisted,
                                   AssetAnalysisVO analysis) {
        this(symbol, timeframe, analysisId, traceId, status, reasonCode, previewOnly,
                poolMutationPerformed, opportunityPersisted, candidatePersisted, finalPlanPersisted,
                analysis, null, null, null);
    }

    public AssetAnalysisPreviewDTO withTask(String id, String state, String stage) {
        return new AssetAnalysisPreviewDTO(symbol, timeframe, analysisId, traceId, status, reasonCode,
                previewOnly, poolMutationPerformed, opportunityPersisted, candidatePersisted,
                finalPlanPersisted, analysis, id, state, stage);
    }
}
