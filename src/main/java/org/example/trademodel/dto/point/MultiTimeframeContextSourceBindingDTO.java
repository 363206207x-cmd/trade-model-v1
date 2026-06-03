package org.example.trademodel.dto.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiTimeframeContextSourceBindingDTO {

    public enum BindingStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_MULTITIMEFRAME_BINDING,
        REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
    }

    private final String multiTimeframeContextId;
    private final String symbol;
    private final String market;
    private final String primaryTimeframe;
    private final List<String> sourceTraceRefs;
    private final String runtimeKlineContextRef;
    private final String dataQualityContextRef;
    private final List<String> timeframeRefs;
    private final List<BigDecimal> timeframeScores;
    private final List<String> timeframeDirections;
    private final List<BigDecimal> timeframeWeights;
    private final List<String> alignedTimeframes;
    private final List<String> conflictedTimeframes;
    private final List<String> missingTimeframes;
    private final List<String> staleTimeframes;
    private final String dominantDirection;
    private final BigDecimal alignmentScore;
    private final BigDecimal conflictScore;
    private final BigDecimal weightedAgreementScore;
    private final Boolean minimumRequiredTimeframesPassed;
    private final Boolean dataQualityPassed;
    private final Boolean hardThresholdPassed;
    private final Boolean warningThresholdPassed;
    private final List<String> missingFields;
    private final List<String> degradedReasons;
    private final List<String> blockedReasons;
    private final String observedAt;
    private final String createdAt;
    private final String missingReason;
    private final String blockedReason;
    private final boolean trustedSource;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean incompleteSafe;
    private final boolean failClosed;
    private final BindingStatus bindingStatus;

    private MultiTimeframeContextSourceBindingDTO(
            String multiTimeframeContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            List<String> timeframeRefs,
            List<BigDecimal> timeframeScores,
            List<String> timeframeDirections,
            List<BigDecimal> timeframeWeights,
            List<String> alignedTimeframes,
            List<String> conflictedTimeframes,
            List<String> missingTimeframes,
            List<String> staleTimeframes,
            String dominantDirection,
            BigDecimal alignmentScore,
            BigDecimal conflictScore,
            BigDecimal weightedAgreementScore,
            Boolean minimumRequiredTimeframesPassed,
            Boolean dataQualityPassed,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            Boolean trustedSource,
            BindingStatus bindingStatus
    ) {
        this.multiTimeframeContextId = multiTimeframeContextId;
        this.symbol = symbol;
        this.market = market;
        this.primaryTimeframe = primaryTimeframe;
        this.sourceTraceRefs = immutableCopy(sourceTraceRefs);
        this.runtimeKlineContextRef = runtimeKlineContextRef;
        this.dataQualityContextRef = dataQualityContextRef;
        this.timeframeRefs = immutableCopy(timeframeRefs);
        this.timeframeScores = immutableCopy(timeframeScores);
        this.timeframeDirections = immutableCopy(timeframeDirections);
        this.timeframeWeights = immutableCopy(timeframeWeights);
        this.alignedTimeframes = immutableCopy(alignedTimeframes);
        this.conflictedTimeframes = immutableCopy(conflictedTimeframes);
        this.missingTimeframes = immutableCopy(missingTimeframes);
        this.staleTimeframes = immutableCopy(staleTimeframes);
        this.dominantDirection = dominantDirection;
        this.alignmentScore = alignmentScore;
        this.conflictScore = conflictScore;
        this.weightedAgreementScore = weightedAgreementScore;
        this.minimumRequiredTimeframesPassed = minimumRequiredTimeframesPassed;
        this.dataQualityPassed = dataQualityPassed;
        this.hardThresholdPassed = hardThresholdPassed;
        this.warningThresholdPassed = warningThresholdPassed;
        this.missingFields = immutableCopy(missingFields);
        this.degradedReasons = immutableCopy(degradedReasons);
        this.blockedReasons = immutableCopy(blockedReasons);
        this.observedAt = observedAt;
        this.createdAt = createdAt;
        this.missingReason = missingReason;
        this.blockedReason = blockedReason;
        this.trustedSource = bindingStatus != BindingStatus.BLOCKED_FAIL_CLOSED && Boolean.TRUE.equals(trustedSource);
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.incompleteSafe = true;
        this.failClosed = bindingStatus == BindingStatus.BLOCKED_FAIL_CLOSED;
        this.bindingStatus = bindingStatus;
    }

    public static MultiTimeframeContextSourceBindingDTO incomplete(
            String multiTimeframeContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            List<String> timeframeRefs,
            List<String> missingFields,
            String missingReason
    ) {
        return new MultiTimeframeContextSourceBindingDTO(
                multiTimeframeContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                timeframeRefs,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                missingFields,
                List.of(),
                List.of(),
                null,
                null,
                requiredReason(missingReason, "missingReason"),
                null,
                Boolean.TRUE,
                BindingStatus.INCOMPLETE
        );
    }

    public static MultiTimeframeContextSourceBindingDTO blockedFailClosed(
            String multiTimeframeContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            List<String> timeframeRefs,
            List<String> blockedReasons,
            String blockedReason
    ) {
        return new MultiTimeframeContextSourceBindingDTO(
                multiTimeframeContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                timeframeRefs,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                blockedReasons,
                null,
                null,
                null,
                requiredReason(blockedReason, "blockedReason"),
                Boolean.FALSE,
                BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    public static MultiTimeframeContextSourceBindingDTO degraded(
            String multiTimeframeContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            List<String> timeframeRefs,
            List<BigDecimal> timeframeScores,
            List<String> timeframeDirections,
            List<BigDecimal> timeframeWeights,
            List<String> alignedTimeframes,
            List<String> conflictedTimeframes,
            List<String> missingTimeframes,
            List<String> staleTimeframes,
            String dominantDirection,
            BigDecimal alignmentScore,
            BigDecimal conflictScore,
            BigDecimal weightedAgreementScore,
            Boolean minimumRequiredTimeframesPassed,
            Boolean dataQualityPassed,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            List<String> missingFields,
            List<String> degradedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            Boolean trustedSource
    ) {
        return new MultiTimeframeContextSourceBindingDTO(
                multiTimeframeContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                timeframeRefs,
                timeframeScores,
                timeframeDirections,
                timeframeWeights,
                alignedTimeframes,
                conflictedTimeframes,
                missingTimeframes,
                staleTimeframes,
                dominantDirection,
                alignmentScore,
                conflictScore,
                weightedAgreementScore,
                minimumRequiredTimeframesPassed,
                dataQualityPassed,
                hardThresholdPassed,
                warningThresholdPassed,
                missingFields,
                degradedReasons,
                List.of(),
                observedAt,
                createdAt,
                requiredReason(missingReason, "missingReason"),
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
        );
    }

    public static MultiTimeframeContextSourceBindingDTO reviewOnly(
            String multiTimeframeContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            List<String> timeframeRefs,
            List<BigDecimal> timeframeScores,
            List<String> timeframeDirections,
            List<BigDecimal> timeframeWeights,
            List<String> alignedTimeframes,
            List<String> conflictedTimeframes,
            List<String> missingTimeframes,
            List<String> staleTimeframes,
            String dominantDirection,
            BigDecimal alignmentScore,
            BigDecimal conflictScore,
            BigDecimal weightedAgreementScore,
            Boolean minimumRequiredTimeframesPassed,
            Boolean dataQualityPassed,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            Boolean trustedSource
    ) {
        return new MultiTimeframeContextSourceBindingDTO(
                multiTimeframeContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                timeframeRefs,
                timeframeScores,
                timeframeDirections,
                timeframeWeights,
                alignedTimeframes,
                conflictedTimeframes,
                missingTimeframes,
                staleTimeframes,
                dominantDirection,
                alignmentScore,
                conflictScore,
                weightedAgreementScore,
                minimumRequiredTimeframesPassed,
                dataQualityPassed,
                hardThresholdPassed,
                warningThresholdPassed,
                missingFields,
                degradedReasons,
                blockedReasons,
                observedAt,
                createdAt,
                null,
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING
        );
    }

    public String getMultiTimeframeContextId() {
        return multiTimeframeContextId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getMarket() {
        return market;
    }

    public String getPrimaryTimeframe() {
        return primaryTimeframe;
    }

    public List<String> getSourceTraceRefs() {
        return sourceTraceRefs;
    }

    public String getRuntimeKlineContextRef() {
        return runtimeKlineContextRef;
    }

    public String getDataQualityContextRef() {
        return dataQualityContextRef;
    }

    public List<String> getTimeframeRefs() {
        return timeframeRefs;
    }

    public List<BigDecimal> getTimeframeScores() {
        return timeframeScores;
    }

    public List<String> getTimeframeDirections() {
        return timeframeDirections;
    }

    public List<BigDecimal> getTimeframeWeights() {
        return timeframeWeights;
    }

    public List<String> getAlignedTimeframes() {
        return alignedTimeframes;
    }

    public List<String> getConflictedTimeframes() {
        return conflictedTimeframes;
    }

    public List<String> getMissingTimeframes() {
        return missingTimeframes;
    }

    public List<String> getStaleTimeframes() {
        return staleTimeframes;
    }

    public String getDominantDirection() {
        return dominantDirection;
    }

    public BigDecimal getAlignmentScore() {
        return alignmentScore;
    }

    public BigDecimal getConflictScore() {
        return conflictScore;
    }

    public BigDecimal getWeightedAgreementScore() {
        return weightedAgreementScore;
    }

    public Boolean getMinimumRequiredTimeframesPassed() {
        return minimumRequiredTimeframesPassed;
    }

    public Boolean getDataQualityPassed() {
        return dataQualityPassed;
    }

    public Boolean getHardThresholdPassed() {
        return hardThresholdPassed;
    }

    public Boolean getWarningThresholdPassed() {
        return warningThresholdPassed;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public List<String> getDegradedReasons() {
        return degradedReasons;
    }

    public List<String> getBlockedReasons() {
        return blockedReasons;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getMissingReason() {
        return missingReason;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public boolean isTrustedSource() {
        return trustedSource;
    }

    public boolean isReviewOnly() {
        return reviewOnly;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isIncompleteSafe() {
        return incompleteSafe;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public BindingStatus getBindingStatus() {
        return bindingStatus;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String requiredReason(String reason, String fieldName) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return reason;
    }
}
