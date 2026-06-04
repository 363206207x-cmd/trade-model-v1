package org.example.trademodel.dto.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceOwnedCandidateIntegrationRuntimeCandidateDTO {

    public enum RuntimeStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_RUNTIME_CANDIDATE,
        REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED
    }

    private final String runtimeCandidateContextId;
    private final String symbol;
    private final String market;
    private final String primaryTimeframe;
    private final String sourceOwnedCandidateIntegrationSourceBindingRef;
    private final String sourceOwnedCandidateIntegrationValidationStatus;
    private final List<String> sourceOwnedCandidateIntegrationValidationReasons;
    private final RuntimeStatus candidateRuntimeStatus;
    private final BigDecimal sourceBindingCompletenessScore;
    private final String sourceBindingCompletenessSummary;
    private final List<String> sourceTraceRefs;
    private final String runtimeKlineContextRef;
    private final String dataQualityContextRef;
    private final String multiTimeframeContextRef;
    private final String riskActionGuardContextRef;
    private final String watchlistPoolProofContextRef;
    private final Boolean allRequiredSourcesPresent;
    private final Boolean allRequiredSourcesTrusted;
    private final Boolean allRequiredSourcesReviewOnly;
    private final Boolean allRequiredSourcesNotTradeInstruction;
    private final Boolean allRequiredSourcesManualReviewRequired;
    private final Boolean allRequiredSourcesIncompleteSafe;
    private final Boolean anySourceBlocked;
    private final Boolean anySourceIncomplete;
    private final Boolean anySourceDegraded;
    private final Boolean watchlistPoolMember;
    private final Boolean watchlistPoolProofFresh;
    private final Boolean riskActionGuardBlocked;
    private final Boolean riskActionGuardStampede;
    private final Boolean runtimeKlineStale;
    private final Boolean dataQualityPassed;
    private final Boolean multiTimeframeConfirmed;
    private final String candidateUnavailableReason;
    private final String candidateBlockedReason;
    private final String candidateDegradedReason;
    private final String missingReason;
    private final String blockedReason;
    private final List<String> missingFields;
    private final List<String> degradedReasons;
    private final List<String> blockedReasons;
    private final String observedAt;
    private final String createdAt;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean incompleteSafe;
    private final boolean failClosed;

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO(
            String runtimeCandidateContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            String sourceOwnedCandidateIntegrationSourceBindingRef,
            String sourceOwnedCandidateIntegrationValidationStatus,
            List<String> sourceOwnedCandidateIntegrationValidationReasons,
            RuntimeStatus candidateRuntimeStatus,
            BigDecimal sourceBindingCompletenessScore,
            String sourceBindingCompletenessSummary,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            Boolean allRequiredSourcesPresent,
            Boolean allRequiredSourcesTrusted,
            Boolean allRequiredSourcesReviewOnly,
            Boolean allRequiredSourcesNotTradeInstruction,
            Boolean allRequiredSourcesManualReviewRequired,
            Boolean allRequiredSourcesIncompleteSafe,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            Boolean watchlistPoolMember,
            Boolean watchlistPoolProofFresh,
            Boolean riskActionGuardBlocked,
            Boolean riskActionGuardStampede,
            Boolean runtimeKlineStale,
            Boolean dataQualityPassed,
            Boolean multiTimeframeConfirmed,
            String candidateUnavailableReason,
            String candidateBlockedReason,
            String candidateDegradedReason,
            String missingReason,
            String blockedReason,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            RuntimeStatus runtimeStatus
    ) {
        this.runtimeCandidateContextId = runtimeCandidateContextId;
        this.symbol = symbol;
        this.market = market;
        this.primaryTimeframe = primaryTimeframe;
        this.sourceOwnedCandidateIntegrationSourceBindingRef = sourceOwnedCandidateIntegrationSourceBindingRef;
        this.sourceOwnedCandidateIntegrationValidationStatus = sourceOwnedCandidateIntegrationValidationStatus;
        this.sourceOwnedCandidateIntegrationValidationReasons =
                immutableCopy(sourceOwnedCandidateIntegrationValidationReasons);
        this.candidateRuntimeStatus = runtimeStatus;
        this.sourceBindingCompletenessScore = sourceBindingCompletenessScore;
        this.sourceBindingCompletenessSummary = sourceBindingCompletenessSummary;
        this.sourceTraceRefs = immutableCopy(sourceTraceRefs);
        this.runtimeKlineContextRef = runtimeKlineContextRef;
        this.dataQualityContextRef = dataQualityContextRef;
        this.multiTimeframeContextRef = multiTimeframeContextRef;
        this.riskActionGuardContextRef = riskActionGuardContextRef;
        this.watchlistPoolProofContextRef = watchlistPoolProofContextRef;
        this.allRequiredSourcesPresent = allRequiredSourcesPresent;
        this.allRequiredSourcesTrusted = allRequiredSourcesTrusted;
        this.allRequiredSourcesReviewOnly = allRequiredSourcesReviewOnly;
        this.allRequiredSourcesNotTradeInstruction = allRequiredSourcesNotTradeInstruction;
        this.allRequiredSourcesManualReviewRequired = allRequiredSourcesManualReviewRequired;
        this.allRequiredSourcesIncompleteSafe = allRequiredSourcesIncompleteSafe;
        this.anySourceBlocked = anySourceBlocked;
        this.anySourceIncomplete = anySourceIncomplete;
        this.anySourceDegraded = anySourceDegraded;
        this.watchlistPoolMember = watchlistPoolMember;
        this.watchlistPoolProofFresh = watchlistPoolProofFresh;
        this.riskActionGuardBlocked = riskActionGuardBlocked;
        this.riskActionGuardStampede = riskActionGuardStampede;
        this.runtimeKlineStale = runtimeKlineStale;
        this.dataQualityPassed = dataQualityPassed;
        this.multiTimeframeConfirmed = multiTimeframeConfirmed;
        this.candidateUnavailableReason = candidateUnavailableReason;
        this.candidateBlockedReason = candidateBlockedReason;
        this.candidateDegradedReason = candidateDegradedReason;
        this.missingReason = missingReason;
        this.blockedReason = blockedReason;
        this.missingFields = immutableCopy(missingFields);
        this.degradedReasons = immutableCopy(degradedReasons);
        this.blockedReasons = immutableCopy(blockedReasons);
        this.observedAt = observedAt;
        this.createdAt = createdAt;
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.incompleteSafe = true;
        this.failClosed = runtimeStatus == RuntimeStatus.BLOCKED_FAIL_CLOSED;
    }

    public static SourceOwnedCandidateIntegrationRuntimeCandidateDTO incomplete(
            String runtimeCandidateContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            String sourceOwnedCandidateIntegrationSourceBindingRef,
            String sourceOwnedCandidateIntegrationValidationStatus,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            List<String> missingFields,
            String candidateUnavailableReason,
            String missingReason
    ) {
        String requiredMissingReason = requiredAny(missingReason, candidateUnavailableReason, "missingReason");
        return new SourceOwnedCandidateIntegrationRuntimeCandidateDTO(
                runtimeCandidateContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceOwnedCandidateIntegrationSourceBindingRef,
                sourceOwnedCandidateIntegrationValidationStatus,
                List.of(),
                RuntimeStatus.INCOMPLETE,
                null,
                null,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                candidateUnavailableReason,
                null,
                null,
                requiredMissingReason,
                null,
                missingFields,
                List.of(),
                List.of(),
                null,
                null,
                RuntimeStatus.INCOMPLETE
        );
    }

    public static SourceOwnedCandidateIntegrationRuntimeCandidateDTO blockedFailClosed(
            String runtimeCandidateContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            String sourceOwnedCandidateIntegrationSourceBindingRef,
            String sourceOwnedCandidateIntegrationValidationStatus,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            List<String> blockedReasons,
            String candidateBlockedReason,
            String blockedReason
    ) {
        String requiredBlockedReason = requiredAny(blockedReason, candidateBlockedReason, "blockedReason");
        return new SourceOwnedCandidateIntegrationRuntimeCandidateDTO(
                runtimeCandidateContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceOwnedCandidateIntegrationSourceBindingRef,
                sourceOwnedCandidateIntegrationValidationStatus,
                List.of(),
                RuntimeStatus.BLOCKED_FAIL_CLOSED,
                null,
                null,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                null,
                null,
                Boolean.TRUE,
                null,
                null,
                null,
                null,
                null,
                candidateBlockedReason,
                null,
                null,
                requiredBlockedReason,
                List.of(),
                List.of(),
                blockedReasons,
                null,
                null,
                RuntimeStatus.BLOCKED_FAIL_CLOSED
        );
    }

    public static SourceOwnedCandidateIntegrationRuntimeCandidateDTO degraded(
            String runtimeCandidateContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            String sourceOwnedCandidateIntegrationSourceBindingRef,
            String sourceOwnedCandidateIntegrationValidationStatus,
            List<String> sourceOwnedCandidateIntegrationValidationReasons,
            BigDecimal sourceBindingCompletenessScore,
            String sourceBindingCompletenessSummary,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            Boolean allRequiredSourcesPresent,
            Boolean allRequiredSourcesTrusted,
            Boolean allRequiredSourcesReviewOnly,
            Boolean allRequiredSourcesNotTradeInstruction,
            Boolean allRequiredSourcesManualReviewRequired,
            Boolean allRequiredSourcesIncompleteSafe,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            Boolean watchlistPoolMember,
            Boolean watchlistPoolProofFresh,
            Boolean riskActionGuardBlocked,
            Boolean riskActionGuardStampede,
            Boolean runtimeKlineStale,
            Boolean dataQualityPassed,
            Boolean multiTimeframeConfirmed,
            String candidateUnavailableReason,
            String candidateDegradedReason,
            String missingReason,
            List<String> missingFields,
            List<String> degradedReasons,
            String observedAt,
            String createdAt
    ) {
        requireCandidateReason(candidateDegradedReason, degradedReasons, "candidateDegradedReason");
        return new SourceOwnedCandidateIntegrationRuntimeCandidateDTO(
                runtimeCandidateContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceOwnedCandidateIntegrationSourceBindingRef,
                sourceOwnedCandidateIntegrationValidationStatus,
                sourceOwnedCandidateIntegrationValidationReasons,
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED,
                sourceBindingCompletenessScore,
                sourceBindingCompletenessSummary,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                allRequiredSourcesPresent,
                allRequiredSourcesTrusted,
                allRequiredSourcesReviewOnly,
                allRequiredSourcesNotTradeInstruction,
                allRequiredSourcesManualReviewRequired,
                allRequiredSourcesIncompleteSafe,
                anySourceBlocked,
                anySourceIncomplete,
                anySourceDegraded,
                watchlistPoolMember,
                watchlistPoolProofFresh,
                riskActionGuardBlocked,
                riskActionGuardStampede,
                runtimeKlineStale,
                dataQualityPassed,
                multiTimeframeConfirmed,
                candidateUnavailableReason,
                null,
                candidateDegradedReason,
                missingReason,
                null,
                missingFields,
                degradedReasons,
                List.of(),
                observedAt,
                createdAt,
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED
        );
    }

    public static SourceOwnedCandidateIntegrationRuntimeCandidateDTO reviewOnly(
            String runtimeCandidateContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            String sourceOwnedCandidateIntegrationSourceBindingRef,
            String sourceOwnedCandidateIntegrationValidationStatus,
            List<String> sourceOwnedCandidateIntegrationValidationReasons,
            BigDecimal sourceBindingCompletenessScore,
            String sourceBindingCompletenessSummary,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            Boolean allRequiredSourcesPresent,
            Boolean allRequiredSourcesTrusted,
            Boolean allRequiredSourcesReviewOnly,
            Boolean allRequiredSourcesNotTradeInstruction,
            Boolean allRequiredSourcesManualReviewRequired,
            Boolean allRequiredSourcesIncompleteSafe,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            Boolean watchlistPoolMember,
            Boolean watchlistPoolProofFresh,
            Boolean riskActionGuardBlocked,
            Boolean riskActionGuardStampede,
            Boolean runtimeKlineStale,
            Boolean dataQualityPassed,
            Boolean multiTimeframeConfirmed,
            String candidateUnavailableReason,
            String candidateBlockedReason,
            String candidateDegradedReason,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt
    ) {
        return new SourceOwnedCandidateIntegrationRuntimeCandidateDTO(
                runtimeCandidateContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceOwnedCandidateIntegrationSourceBindingRef,
                sourceOwnedCandidateIntegrationValidationStatus,
                sourceOwnedCandidateIntegrationValidationReasons,
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                sourceBindingCompletenessScore,
                sourceBindingCompletenessSummary,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                allRequiredSourcesPresent,
                allRequiredSourcesTrusted,
                allRequiredSourcesReviewOnly,
                allRequiredSourcesNotTradeInstruction,
                allRequiredSourcesManualReviewRequired,
                allRequiredSourcesIncompleteSafe,
                anySourceBlocked,
                anySourceIncomplete,
                anySourceDegraded,
                watchlistPoolMember,
                watchlistPoolProofFresh,
                riskActionGuardBlocked,
                riskActionGuardStampede,
                runtimeKlineStale,
                dataQualityPassed,
                multiTimeframeConfirmed,
                candidateUnavailableReason,
                candidateBlockedReason,
                candidateDegradedReason,
                null,
                null,
                missingFields,
                degradedReasons,
                blockedReasons,
                observedAt,
                createdAt,
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE
        );
    }

    public String getRuntimeCandidateContextId() {
        return runtimeCandidateContextId;
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

    public String getSourceOwnedCandidateIntegrationSourceBindingRef() {
        return sourceOwnedCandidateIntegrationSourceBindingRef;
    }

    public String getSourceOwnedCandidateIntegrationValidationStatus() {
        return sourceOwnedCandidateIntegrationValidationStatus;
    }

    public List<String> getSourceOwnedCandidateIntegrationValidationReasons() {
        return sourceOwnedCandidateIntegrationValidationReasons;
    }

    public RuntimeStatus getCandidateRuntimeStatus() {
        return candidateRuntimeStatus;
    }

    public BigDecimal getSourceBindingCompletenessScore() {
        return sourceBindingCompletenessScore;
    }

    public String getSourceBindingCompletenessSummary() {
        return sourceBindingCompletenessSummary;
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

    public String getMultiTimeframeContextRef() {
        return multiTimeframeContextRef;
    }

    public String getRiskActionGuardContextRef() {
        return riskActionGuardContextRef;
    }

    public String getWatchlistPoolProofContextRef() {
        return watchlistPoolProofContextRef;
    }

    public Boolean getAllRequiredSourcesPresent() {
        return allRequiredSourcesPresent;
    }

    public Boolean getAllRequiredSourcesTrusted() {
        return allRequiredSourcesTrusted;
    }

    public Boolean getAllRequiredSourcesReviewOnly() {
        return allRequiredSourcesReviewOnly;
    }

    public Boolean getAllRequiredSourcesNotTradeInstruction() {
        return allRequiredSourcesNotTradeInstruction;
    }

    public Boolean getAllRequiredSourcesManualReviewRequired() {
        return allRequiredSourcesManualReviewRequired;
    }

    public Boolean getAllRequiredSourcesIncompleteSafe() {
        return allRequiredSourcesIncompleteSafe;
    }

    public Boolean getAnySourceBlocked() {
        return anySourceBlocked;
    }

    public Boolean getAnySourceIncomplete() {
        return anySourceIncomplete;
    }

    public Boolean getAnySourceDegraded() {
        return anySourceDegraded;
    }

    public Boolean getWatchlistPoolMember() {
        return watchlistPoolMember;
    }

    public Boolean getWatchlistPoolProofFresh() {
        return watchlistPoolProofFresh;
    }

    public Boolean getRiskActionGuardBlocked() {
        return riskActionGuardBlocked;
    }

    public Boolean getRiskActionGuardStampede() {
        return riskActionGuardStampede;
    }

    public Boolean getRuntimeKlineStale() {
        return runtimeKlineStale;
    }

    public Boolean getDataQualityPassed() {
        return dataQualityPassed;
    }

    public Boolean getMultiTimeframeConfirmed() {
        return multiTimeframeConfirmed;
    }

    public String getCandidateUnavailableReason() {
        return candidateUnavailableReason;
    }

    public String getCandidateBlockedReason() {
        return candidateBlockedReason;
    }

    public String getCandidateDegradedReason() {
        return candidateDegradedReason;
    }

    public String getMissingReason() {
        return missingReason;
    }

    public String getBlockedReason() {
        return blockedReason;
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

    private static List<String> immutableCopy(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static String requiredAny(String firstReason, String secondReason, String fieldName) {
        if (hasText(firstReason)) {
            return firstReason;
        }
        if (hasText(secondReason)) {
            return secondReason;
        }
        throw new IllegalArgumentException(fieldName + " is required");
    }

    private static void requireCandidateReason(String candidateReason, List<String> reasons, String fieldName) {
        if (hasText(candidateReason)) {
            return;
        }
        if (reasons != null && reasons.stream().anyMatch(SourceOwnedCandidateIntegrationRuntimeCandidateDTO::hasText)) {
            return;
        }
        throw new IllegalArgumentException(fieldName + " is required");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
