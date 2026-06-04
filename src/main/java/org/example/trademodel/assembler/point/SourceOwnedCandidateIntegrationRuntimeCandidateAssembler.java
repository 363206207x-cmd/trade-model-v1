package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationRuntimeCandidateDTO;
import org.example.trademodel.validator.point.SourceOwnedCandidateIntegrationRuntimeCandidateValidator;

public class SourceOwnedCandidateIntegrationRuntimeCandidateAssembler {

    private static final String REASON_INPUT_MISSING =
            "SOURCE_OWNED_CANDIDATE_RUNTIME_INPUT_MISSING";
    private static final String REASON_UNSUPPORTED_STATUS =
            "UNSUPPORTED_SOURCE_OWNED_CANDIDATE_RUNTIME_STATUS";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_DEGRADED_REASON_REQUIRED = "DEGRADED_REASON_REQUIRED";

    private final SourceOwnedCandidateIntegrationRuntimeCandidateValidator validator;

    public SourceOwnedCandidateIntegrationRuntimeCandidateAssembler() {
        this(new SourceOwnedCandidateIntegrationRuntimeCandidateValidator());
    }

    public SourceOwnedCandidateIntegrationRuntimeCandidateAssembler(
            SourceOwnedCandidateIntegrationRuntimeCandidateValidator validator
    ) {
        this.validator = validator == null
                ? new SourceOwnedCandidateIntegrationRuntimeCandidateValidator()
                : validator;
    }

    public AssembledSourceOwnedCandidateIntegrationRuntimeCandidate assemble(RuntimeAssemblyInput input) {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context = contextFrom(input);
        SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult validationResult =
                validator.validate(context);
        return new AssembledSourceOwnedCandidateIntegrationRuntimeCandidate(context, validationResult);
    }

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO contextFrom(RuntimeAssemblyInput input) {
        if (input == null) {
            return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.incomplete(
                    null,
                    null,
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
                    REASON_INPUT_MISSING,
                    REASON_INPUT_MISSING
            );
        }

        SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus requestedStatus =
                input.getRequestedRuntimeStatus();

        if (SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.BLOCKED_FAIL_CLOSED
                .equals(requestedStatus)) {
            return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.blockedFailClosed(
                    input.getRuntimeCandidateContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceOwnedCandidateIntegrationSourceBindingRef(),
                    input.getSourceOwnedCandidateIntegrationValidationStatus(),
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

        if (SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus
                .REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED.equals(requestedStatus)) {
            return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.degraded(
                    input.getRuntimeCandidateContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceOwnedCandidateIntegrationSourceBindingRef(),
                    input.getSourceOwnedCandidateIntegrationValidationStatus(),
                    input.getSourceOwnedCandidateIntegrationValidationReasons(),
                    input.getSourceBindingCompletenessScore(),
                    input.getSourceBindingCompletenessSummary(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolProofContextRef(),
                    input.getAllRequiredSourcesPresent(),
                    input.getAllRequiredSourcesTrusted(),
                    input.getAllRequiredSourcesReviewOnly(),
                    input.getAllRequiredSourcesNotTradeInstruction(),
                    input.getAllRequiredSourcesManualReviewRequired(),
                    input.getAllRequiredSourcesIncompleteSafe(),
                    input.getAnySourceBlocked(),
                    input.getAnySourceIncomplete(),
                    input.getAnySourceDegraded(),
                    input.getWatchlistPoolMember(),
                    input.getWatchlistPoolProofFresh(),
                    input.getRiskActionGuardBlocked(),
                    input.getRiskActionGuardStampede(),
                    input.getRuntimeKlineStale(),
                    input.getDataQualityPassed(),
                    input.getMultiTimeframeConfirmed(),
                    input.getCandidateUnavailableReason(),
                    degradedReasonOrFallback(input),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt()
            );
        }

        if (SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE
                .equals(requestedStatus)) {
            return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.reviewOnly(
                    input.getRuntimeCandidateContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceOwnedCandidateIntegrationSourceBindingRef(),
                    input.getSourceOwnedCandidateIntegrationValidationStatus(),
                    input.getSourceOwnedCandidateIntegrationValidationReasons(),
                    input.getSourceBindingCompletenessScore(),
                    input.getSourceBindingCompletenessSummary(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolProofContextRef(),
                    input.getAllRequiredSourcesPresent(),
                    input.getAllRequiredSourcesTrusted(),
                    input.getAllRequiredSourcesReviewOnly(),
                    input.getAllRequiredSourcesNotTradeInstruction(),
                    input.getAllRequiredSourcesManualReviewRequired(),
                    input.getAllRequiredSourcesIncompleteSafe(),
                    input.getAnySourceBlocked(),
                    input.getAnySourceIncomplete(),
                    input.getAnySourceDegraded(),
                    input.getWatchlistPoolMember(),
                    input.getWatchlistPoolProofFresh(),
                    input.getRiskActionGuardBlocked(),
                    input.getRiskActionGuardStampede(),
                    input.getRuntimeKlineStale(),
                    input.getDataQualityPassed(),
                    input.getMultiTimeframeConfirmed(),
                    input.getCandidateUnavailableReason(),
                    input.getCandidateBlockedReason(),
                    input.getCandidateDegradedReason(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getBlockedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt()
            );
        }

        String fallback = SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.INCOMPLETE
                .equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.incomplete(
                input.getRuntimeCandidateContextId(),
                input.getSymbol(),
                input.getMarket(),
                input.getPrimaryTimeframe(),
                input.getSourceOwnedCandidateIntegrationSourceBindingRef(),
                input.getSourceOwnedCandidateIntegrationValidationStatus(),
                input.getSourceTraceRefs(),
                input.getRuntimeKlineContextRef(),
                input.getDataQualityContextRef(),
                input.getMultiTimeframeContextRef(),
                input.getRiskActionGuardContextRef(),
                input.getWatchlistPoolProofContextRef(),
                input.getMissingFields(),
                input.getCandidateUnavailableReason(),
                requiredOrFallback(input.getMissingReason(), fallback)
        );
    }

    private static String degradedReasonOrFallback(RuntimeAssemblyInput input) {
        if (!isBlank(input.getCandidateDegradedReason())) {
            return input.getCandidateDegradedReason();
        }
        return hasAnyText(input.getDegradedReasons()) ? null : REASON_DEGRADED_REASON_REQUIRED;
    }

    private static String requiredOrFallback(String reason, String fallbackReason) {
        return isBlank(reason) ? fallbackReason : reason;
    }

    private static boolean hasAnyText(List<String> values) {
        if (values == null) {
            return false;
        }
        return values.stream().anyMatch(value -> !isBlank(value));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class RuntimeAssemblyInput {
        private final String runtimeCandidateContextId;
        private final String symbol;
        private final String market;
        private final String primaryTimeframe;
        private final SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus requestedRuntimeStatus;
        private final String sourceOwnedCandidateIntegrationSourceBindingRef;
        private final String sourceOwnedCandidateIntegrationValidationStatus;
        private final List<String> sourceOwnedCandidateIntegrationValidationReasons;
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

        private RuntimeAssemblyInput(
                String runtimeCandidateContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus requestedRuntimeStatus,
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
                String missingReason,
                String blockedReason,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt
        ) {
            this.runtimeCandidateContextId = runtimeCandidateContextId;
            this.symbol = symbol;
            this.market = market;
            this.primaryTimeframe = primaryTimeframe;
            this.requestedRuntimeStatus = requestedRuntimeStatus;
            this.sourceOwnedCandidateIntegrationSourceBindingRef = sourceOwnedCandidateIntegrationSourceBindingRef;
            this.sourceOwnedCandidateIntegrationValidationStatus = sourceOwnedCandidateIntegrationValidationStatus;
            this.sourceOwnedCandidateIntegrationValidationReasons =
                    copy(sourceOwnedCandidateIntegrationValidationReasons);
            this.sourceBindingCompletenessScore = sourceBindingCompletenessScore;
            this.sourceBindingCompletenessSummary = sourceBindingCompletenessSummary;
            this.sourceTraceRefs = copy(sourceTraceRefs);
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
            this.missingFields = copy(missingFields);
            this.degradedReasons = copy(degradedReasons);
            this.blockedReasons = copy(blockedReasons);
            this.observedAt = observedAt;
            this.createdAt = createdAt;
        }

        public static RuntimeAssemblyInput of(
                String runtimeCandidateContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus requestedRuntimeStatus,
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
                String missingReason,
                String blockedReason,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt
        ) {
            return new RuntimeAssemblyInput(
                    runtimeCandidateContextId,
                    symbol,
                    market,
                    primaryTimeframe,
                    requestedRuntimeStatus,
                    sourceOwnedCandidateIntegrationSourceBindingRef,
                    sourceOwnedCandidateIntegrationValidationStatus,
                    sourceOwnedCandidateIntegrationValidationReasons,
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
                    missingReason,
                    blockedReason,
                    missingFields,
                    degradedReasons,
                    blockedReasons,
                    observedAt,
                    createdAt
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

        public SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus getRequestedRuntimeStatus() {
            return requestedRuntimeStatus;
        }

        public String getSourceOwnedCandidateIntegrationSourceBindingRef() {
            return sourceOwnedCandidateIntegrationSourceBindingRef;
        }

        public String getSourceOwnedCandidateIntegrationValidationStatus() {
            return sourceOwnedCandidateIntegrationValidationStatus;
        }

        public List<String> getSourceOwnedCandidateIntegrationValidationReasons() {
            return copy(sourceOwnedCandidateIntegrationValidationReasons);
        }

        public BigDecimal getSourceBindingCompletenessScore() {
            return sourceBindingCompletenessScore;
        }

        public String getSourceBindingCompletenessSummary() {
            return sourceBindingCompletenessSummary;
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

        private static List<String> copy(List<String> values) {
            return values == null ? Collections.emptyList() : new ArrayList<>(values);
        }
    }

    public static class AssembledSourceOwnedCandidateIntegrationRuntimeCandidate {
        private final SourceOwnedCandidateIntegrationRuntimeCandidateDTO context;
        private final SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult validationResult;

        private AssembledSourceOwnedCandidateIntegrationRuntimeCandidate(
                SourceOwnedCandidateIntegrationRuntimeCandidateDTO context,
                SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public SourceOwnedCandidateIntegrationRuntimeCandidateDTO getContext() {
            return context;
        }

        public SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
