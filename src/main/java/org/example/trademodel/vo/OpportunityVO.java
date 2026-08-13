package org.example.trademodel.vo;

import org.example.trademodel.entity.AssetStateDO;

import java.time.LocalDateTime;

/** Read-only projection of the canonical AssetState/Opportunity owner. */
public record OpportunityVO(
        String opportunityId,
        Long poolItemId,
        Long assetId,
        String analysisId,
        String symbol,
        String timeframe,
        String state,
        Integer opportunityScore,
        String confidence,
        String risk,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String ruleVersion,
        String extJson,
        String traceId) {

    public static OpportunityVO from(AssetStateDO row) {
        if (row == null) return null;
        return new OpportunityVO(
                row.getOpportunityId(), row.getPoolItemId(), row.getAssetId(), row.getLastAnalysisId(),
                row.getSymbol(), row.getTimeframe(), row.getState() == null ? null : row.getState().name(),
                row.getOpportunityScore(), row.getConfidence(), row.getRisk(), row.getCreatedAt(),
                row.getUpdatedAt(), row.getRuleVersion(), row.getExtJson(), row.getTraceId());
    }
}
