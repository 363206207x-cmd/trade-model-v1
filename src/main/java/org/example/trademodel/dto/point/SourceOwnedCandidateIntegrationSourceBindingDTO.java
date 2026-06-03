package org.example.trademodel.dto.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceOwnedCandidateIntegrationSourceBindingDTO {

    public enum BindingStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING,
        REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
    }

    private final String candidateIntegrationContextId;
    private final String symbol;
    private final String market;
    private final String primaryTimeframe;
    private final List<String> sourceTraceRefs;
    private final String runtimeKlineContextRef;
    private final String dataQualityContextRef;
    private final String multiTimeframeContextRef;
    private final String riskActionGuardContextRef;
    private final String watchlistPoolProofContextRef;
    private final String sourceTraceStatus;
    private final String runtimeKlineStatus;
    private final String dataQualityStatus;
    private final String multiTimeframeStatus;
    private final String riskActionGuardStatus;
    private final String watchlistPoolProofStatus;
    private final BigDecimal sourceBindingCompletenessScore;
    private final Boolean allRequiredSourcesPresent;
    private final Boolean allRequiredSourcesTrusted;
    private final Boolean allRequiredSourcesReviewOnly;
    private final Boolean allRequiredSourcesNotTradeInstruction;
    private final Boolean allRequiredSourcesManualReviewRequired;
    private final Boolean allRequiredSourcesIncompleteSafe;
    private final Boolean anySourceBlocked;
    private final Boolean anySourceIncomplete;
    private final Boolean anySourceDegraded;
    private final String candidateBoundaryLabel;
    private final String candidateUnavailableReason;
    private final String candidateBlockedReason;
    private final String candidateDegradedReason;
    private final List<String> sourceOwnedTraceRefs;
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

    private SourceOwnedCandidateIntegrationSourceBindingDTO(
            String candidateIntegrationContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            String sourceTraceStatus,
            String runtimeKlineStatus,
            String dataQualityStatus,
            String multiTimeframeStatus,
            String riskActionGuardStatus,
            String watchlistPoolProofStatus,
            BigDecimal sourceBindingCompletenessScore,
            Boolean allRequiredSourcesPresent,
            Boolean allRequiredSourcesTrusted,
            Boolean allRequiredSourcesReviewOnly,
            Boolean allRequiredSourcesNotTradeInstruction,
            Boolean allRequiredSourcesManualReviewRequired,
            Boolean allRequiredSourcesIncompleteSafe,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            String candidateBoundaryLabel,
            String candidateUnavailableReason,
            String candidateBlockedReason,
            String candidateDegradedReason,
            List<String> sourceOwnedTraceRefs,
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
        this.candidateIntegrationContextId = candidateIntegrationContextId;
        this.symbol = symbol;
        this.market = market;
        this.primaryTimeframe = primaryTimeframe;
        this.sourceTraceRefs = immutableCopy(sourceTraceRefs);
        this.runtimeKlineContextRef = runtimeKlineContextRef;
        this.dataQualityContextRef = dataQualityContextRef;
        this.multiTimeframeContextRef = multiTimeframeContextRef;
        this.riskActionGuardContextRef = riskActionGuardContextRef;
        this.watchlistPoolProofContextRef = watchlistPoolProofContextRef;
        this.sourceTraceStatus = sourceTraceStatus;
        this.runtimeKlineStatus = runtimeKlineStatus;
        this.dataQualityStatus = dataQualityStatus;
        this.multiTimeframeStatus = multiTimeframeStatus;
        this.riskActionGuardStatus = riskActionGuardStatus;
        this.watchlistPoolProofStatus = watchlistPoolProofStatus;
        this.sourceBindingCompletenessScore = sourceBindingCompletenessScore;
        this.allRequiredSourcesPresent = allRequiredSourcesPresent;
        this.allRequiredSourcesTrusted = allRequiredSourcesTrusted;
        this.allRequiredSourcesReviewOnly = allRequiredSourcesReviewOnly;
        this.allRequiredSourcesNotTradeInstruction = allRequiredSourcesNotTradeInstruction;
        this.allRequiredSourcesManualReviewRequired = allRequiredSourcesManualReviewRequired;
        this.allRequiredSourcesIncompleteSafe = allRequiredSourcesIncompleteSafe;
        this.anySourceBlocked = anySourceBlocked;
        this.anySourceIncomplete = anySourceIncomplete;
        this.anySourceDegraded = anySourceDegraded;
        this.candidateBoundaryLabel = candidateBoundaryLabel;
        this.candidateUnavailableReason = candidateUnavailableReason;
        this.candidateBlockedReason = candidateBlockedReason;
        this.candidateDegradedReason = candidateDegradedReason;
        this.sourceOwnedTraceRefs = immutableCopy(sourceOwnedTraceRefs);
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

    public static SourceOwnedCandidateIntegrationSourceBindingDTO incomplete(
            String candidateIntegrationContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            List<String> missingFields,
            String missingReason
    ) {
        return new SourceOwnedCandidateIntegrationSourceBindingDTO(
                candidateIntegrationContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
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
                "SOURCE_BINDING_UNAVAILABLE",
                null,
                null,
                List.of(),
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

    public static SourceOwnedCandidateIntegrationSourceBindingDTO blockedFailClosed(
            String candidateIntegrationContextId,
            String symbol,
            String market,
            String primaryTimeframe,
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
        return new SourceOwnedCandidateIntegrationSourceBindingDTO(
                candidateIntegrationContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
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
                candidateBlockedReason,
                null,
                List.of(),
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

    public static SourceOwnedCandidateIntegrationSourceBindingDTO degraded(
            String candidateIntegrationContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            String sourceTraceStatus,
            String runtimeKlineStatus,
            String dataQualityStatus,
            String multiTimeframeStatus,
            String riskActionGuardStatus,
            String watchlistPoolProofStatus,
            BigDecimal sourceBindingCompletenessScore,
            Boolean allRequiredSourcesPresent,
            Boolean allRequiredSourcesTrusted,
            Boolean allRequiredSourcesReviewOnly,
            Boolean allRequiredSourcesNotTradeInstruction,
            Boolean allRequiredSourcesManualReviewRequired,
            Boolean allRequiredSourcesIncompleteSafe,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            String candidateBoundaryLabel,
            String candidateUnavailableReason,
            String candidateDegradedReason,
            List<String> sourceOwnedTraceRefs,
            List<String> missingFields,
            List<String> degradedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            Boolean trustedSource
    ) {
        return new SourceOwnedCandidateIntegrationSourceBindingDTO(
                candidateIntegrationContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                sourceTraceStatus,
                runtimeKlineStatus,
                dataQualityStatus,
                multiTimeframeStatus,
                riskActionGuardStatus,
                watchlistPoolProofStatus,
                sourceBindingCompletenessScore,
                allRequiredSourcesPresent,
                allRequiredSourcesTrusted,
                allRequiredSourcesReviewOnly,
                allRequiredSourcesNotTradeInstruction,
                allRequiredSourcesManualReviewRequired,
                allRequiredSourcesIncompleteSafe,
                anySourceBlocked,
                anySourceIncomplete,
                anySourceDegraded,
                candidateBoundaryLabel,
                candidateUnavailableReason,
                null,
                candidateDegradedReason,
                sourceOwnedTraceRefs,
                missingFields,
                degradedReasons,
                List.of(),
                observedAt,
                createdAt,
                requiredReason(missingReason, "missingReason"),
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
        );
    }

    public static SourceOwnedCandidateIntegrationSourceBindingDTO reviewOnly(
            String candidateIntegrationContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            String sourceTraceStatus,
            String runtimeKlineStatus,
            String dataQualityStatus,
            String multiTimeframeStatus,
            String riskActionGuardStatus,
            String watchlistPoolProofStatus,
            BigDecimal sourceBindingCompletenessScore,
            Boolean allRequiredSourcesPresent,
            Boolean allRequiredSourcesTrusted,
            Boolean allRequiredSourcesReviewOnly,
            Boolean allRequiredSourcesNotTradeInstruction,
            Boolean allRequiredSourcesManualReviewRequired,
            Boolean allRequiredSourcesIncompleteSafe,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            String candidateBoundaryLabel,
            String candidateUnavailableReason,
            String candidateBlockedReason,
            String candidateDegradedReason,
            List<String> sourceOwnedTraceRefs,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            Boolean trustedSource
    ) {
        return new SourceOwnedCandidateIntegrationSourceBindingDTO(
                candidateIntegrationContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                sourceTraceStatus,
                runtimeKlineStatus,
                dataQualityStatus,
                multiTimeframeStatus,
                riskActionGuardStatus,
                watchlistPoolProofStatus,
                sourceBindingCompletenessScore,
                allRequiredSourcesPresent,
                allRequiredSourcesTrusted,
                allRequiredSourcesReviewOnly,
                allRequiredSourcesNotTradeInstruction,
                allRequiredSourcesManualReviewRequired,
                allRequiredSourcesIncompleteSafe,
                anySourceBlocked,
                anySourceIncomplete,
                anySourceDegraded,
                candidateBoundaryLabel,
                candidateUnavailableReason,
                candidateBlockedReason,
                candidateDegradedReason,
                sourceOwnedTraceRefs,
                missingFields,
                degradedReasons,
                blockedReasons,
                observedAt,
                createdAt,
                null,
                null,
                trustedSource,
                BindingStatus.REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING
        );
    }

    public String getCandidateIntegrationContextId() {
        return candidateIntegrationContextId;
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

    public String getMultiTimeframeContextRef() {
        return multiTimeframeContextRef;
    }

    public String getRiskActionGuardContextRef() {
        return riskActionGuardContextRef;
    }

    public String getWatchlistPoolProofContextRef() {
        return watchlistPoolProofContextRef;
    }

    public String getSourceTraceStatus() {
        return sourceTraceStatus;
    }

    public String getRuntimeKlineStatus() {
        return runtimeKlineStatus;
    }

    public String getDataQualityStatus() {
        return dataQualityStatus;
    }

    public String getMultiTimeframeStatus() {
        return multiTimeframeStatus;
    }

    public String getRiskActionGuardStatus() {
        return riskActionGuardStatus;
    }

    public String getWatchlistPoolProofStatus() {
        return watchlistPoolProofStatus;
    }

    public BigDecimal getSourceBindingCompletenessScore() {
        return sourceBindingCompletenessScore;
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

    public String getCandidateBoundaryLabel() {
        return candidateBoundaryLabel;
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

    public List<String> getSourceOwnedTraceRefs() {
        return sourceOwnedTraceRefs;
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

    private static List<String> immutableCopy(List<String> values) {
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
