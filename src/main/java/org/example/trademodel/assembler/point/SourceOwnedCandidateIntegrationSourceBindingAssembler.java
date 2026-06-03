package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationSourceBindingDTO;
import org.example.trademodel.validator.point.SourceOwnedCandidateIntegrationSourceBindingValidator;

public class SourceOwnedCandidateIntegrationSourceBindingAssembler {

    private static final String REASON_INPUT_MISSING =
            "SOURCE_OWNED_CANDIDATE_INTEGRATION_INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS =
            "UNSUPPORTED_SOURCE_OWNED_CANDIDATE_INTEGRATION_STATUS";

    private final SourceOwnedCandidateIntegrationSourceBindingValidator validator;

    public SourceOwnedCandidateIntegrationSourceBindingAssembler() {
        this(new SourceOwnedCandidateIntegrationSourceBindingValidator());
    }

    public SourceOwnedCandidateIntegrationSourceBindingAssembler(
            SourceOwnedCandidateIntegrationSourceBindingValidator validator
    ) {
        this.validator = validator == null
                ? new SourceOwnedCandidateIntegrationSourceBindingValidator()
                : validator;
    }

    public AssembledSourceOwnedCandidateIntegrationSourceBinding assemble(AssemblyInput input) {
        SourceOwnedCandidateIntegrationSourceBindingDTO context = contextFrom(input);
        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult validationResult =
                validator.validate(context);
        return new AssembledSourceOwnedCandidateIntegrationSourceBinding(context, validationResult);
    }

    private SourceOwnedCandidateIntegrationSourceBindingDTO contextFrom(AssemblyInput input) {
        if (input == null) {
            return SourceOwnedCandidateIntegrationSourceBindingDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    REASON_INPUT_MISSING
            );
        }

        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus requestedStatus =
                input.getRequestedStatus();
        if (SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED
                .equals(requestedStatus)) {
            return SourceOwnedCandidateIntegrationSourceBindingDTO.blockedFailClosed(
                    input.getCandidateIntegrationContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolProofContextRef(),
                    input.getBlockedReasons(),
                    input.getCandidateBlockedReason(),
                    requiredOrFallback(input.getBlockedReason(), REASON_BLOCKED_REASON_REQUIRED)
            );
        }

        if (SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
                .equals(requestedStatus)) {
            return SourceOwnedCandidateIntegrationSourceBindingDTO.degraded(
                    input.getCandidateIntegrationContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolProofContextRef(),
                    input.getSourceTraceStatus(),
                    input.getRuntimeKlineStatus(),
                    input.getDataQualityStatus(),
                    input.getMultiTimeframeStatus(),
                    input.getRiskActionGuardStatus(),
                    input.getWatchlistPoolProofStatus(),
                    input.getSourceBindingCompletenessScore(),
                    input.getAllRequiredSourcesPresent(),
                    input.getAllRequiredSourcesTrusted(),
                    input.getAllRequiredSourcesReviewOnly(),
                    input.getAllRequiredSourcesNotTradeInstruction(),
                    input.getAllRequiredSourcesManualReviewRequired(),
                    input.getAllRequiredSourcesIncompleteSafe(),
                    input.getAnySourceBlocked(),
                    input.getAnySourceIncomplete(),
                    input.getAnySourceDegraded(),
                    input.getCandidateBoundaryLabel(),
                    input.getCandidateUnavailableReason(),
                    input.getCandidateDegradedReason(),
                    input.getSourceOwnedTraceRefs(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED),
                    input.getTrustedSource()
            );
        }

        if (SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING
                .equals(requestedStatus)) {
            return SourceOwnedCandidateIntegrationSourceBindingDTO.reviewOnly(
                    input.getCandidateIntegrationContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolProofContextRef(),
                    input.getSourceTraceStatus(),
                    input.getRuntimeKlineStatus(),
                    input.getDataQualityStatus(),
                    input.getMultiTimeframeStatus(),
                    input.getRiskActionGuardStatus(),
                    input.getWatchlistPoolProofStatus(),
                    input.getSourceBindingCompletenessScore(),
                    input.getAllRequiredSourcesPresent(),
                    input.getAllRequiredSourcesTrusted(),
                    input.getAllRequiredSourcesReviewOnly(),
                    input.getAllRequiredSourcesNotTradeInstruction(),
                    input.getAllRequiredSourcesManualReviewRequired(),
                    input.getAllRequiredSourcesIncompleteSafe(),
                    input.getAnySourceBlocked(),
                    input.getAnySourceIncomplete(),
                    input.getAnySourceDegraded(),
                    input.getCandidateBoundaryLabel(),
                    input.getCandidateUnavailableReason(),
                    input.getCandidateBlockedReason(),
                    input.getCandidateDegradedReason(),
                    input.getSourceOwnedTraceRefs(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getBlockedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    input.getTrustedSource()
            );
        }

        String fallback = SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.INCOMPLETE
                .equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return SourceOwnedCandidateIntegrationSourceBindingDTO.incomplete(
                input.getCandidateIntegrationContextId(),
                input.getSymbol(),
                input.getMarket(),
                input.getPrimaryTimeframe(),
                input.getSourceTraceRefs(),
                input.getRuntimeKlineContextRef(),
                input.getDataQualityContextRef(),
                input.getMultiTimeframeContextRef(),
                input.getRiskActionGuardContextRef(),
                input.getWatchlistPoolProofContextRef(),
                input.getMissingFields(),
                requiredOrFallback(input.getMissingReason(), fallback)
        );
    }

    private static String requiredOrFallback(String reason, String fallbackReason) {
        return isBlank(reason) ? fallbackReason : reason;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class AssemblyInput {
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
        private final Boolean trustedSource;
        private final SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus requestedStatus;

        private AssemblyInput(
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
                SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus requestedStatus
        ) {
            this.candidateIntegrationContextId = candidateIntegrationContextId;
            this.symbol = symbol;
            this.market = market;
            this.primaryTimeframe = primaryTimeframe;
            this.sourceTraceRefs = copy(sourceTraceRefs);
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
            this.sourceOwnedTraceRefs = copy(sourceOwnedTraceRefs);
            this.missingFields = copy(missingFields);
            this.degradedReasons = copy(degradedReasons);
            this.blockedReasons = copy(blockedReasons);
            this.observedAt = observedAt;
            this.createdAt = createdAt;
            this.missingReason = missingReason;
            this.blockedReason = blockedReason;
            this.trustedSource = trustedSource;
            this.requestedStatus = requestedStatus;
        }

        public static AssemblyInput of(
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
                SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus requestedStatus
        ) {
            return new AssemblyInput(
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
                    missingReason,
                    blockedReason,
                    trustedSource,
                    requestedStatus
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
            return copy(sourceTraceRefs);
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
            return copy(sourceOwnedTraceRefs);
        }

        public List<String> getMissingFields() {
            return copy(missingFields);
        }

        public List<String> getDegradedReasons() {
            return copy(degradedReasons);
        }

        public List<String> getBlockedReasons() {
            return copy(blockedReasons);
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

        public Boolean getTrustedSource() {
            return trustedSource;
        }

        public SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus getRequestedStatus() {
            return requestedStatus;
        }

        private static List<String> copy(List<String> values) {
            return values == null ? Collections.emptyList() : new ArrayList<>(values);
        }
    }

    public static class AssembledSourceOwnedCandidateIntegrationSourceBinding {
        private final SourceOwnedCandidateIntegrationSourceBindingDTO context;
        private final SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult validationResult;

        private AssembledSourceOwnedCandidateIntegrationSourceBinding(
                SourceOwnedCandidateIntegrationSourceBindingDTO context,
                SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public SourceOwnedCandidateIntegrationSourceBindingDTO getContext() {
            return context;
        }

        public SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
