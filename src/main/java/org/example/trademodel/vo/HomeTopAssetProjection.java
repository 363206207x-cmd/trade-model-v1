package org.example.trademodel.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * Read-only ranking result for one Home focus-asset slot.
 */
public record HomeTopAssetProjection(
        Long assetId,
        String symbol,
        String name,
        Integer opportunityScore,
        String finalMarketBias,
        String confidence,
        String riskLevel,
        String finalPlanMode,
        String aiDecisionResult,
        Integer dataQuality,
        String freshness,
        Long freshnessAgeSeconds,
        Long stabilitySeconds,
        Integer priorityScore,
        String rankingReason,
        String analysisId,
        String opportunityId,
        String opportunityState,
        String primaryOpportunityId,
        String primaryTimeframe,
        String primaryPlanMode,
        Integer secondaryOpportunityCount,
        String timeframeConflictState,
        LocalDateTime analysisTime,
        @JsonIgnore DecisionResultVO sourceDecision) {

    /** Compatibility accessor retained while callers migrate to the frozen name. */
    public String planMode() {
        return finalPlanMode;
    }
}
