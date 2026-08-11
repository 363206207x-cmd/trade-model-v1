package org.example.trademodel.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Read-only ranking result for one Home focus-asset slot.
 */
public record HomeTopAssetProjection(
        Long assetId,
        String symbol,
        Integer opportunityScore,
        String confidence,
        String riskLevel,
        String planMode,
        String aiDecisionResult,
        Integer dataQuality,
        String rankingReason,
        String analysisId,
        String opportunityId,
        String opportunityState,
        @JsonIgnore DecisionResultVO sourceDecision) {
}
