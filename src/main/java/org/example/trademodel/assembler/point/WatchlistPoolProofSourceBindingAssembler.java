package org.example.trademodel.assembler.point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.WatchlistPoolProofSourceBindingDTO;
import org.example.trademodel.validator.point.WatchlistPoolProofSourceBindingValidator;

public class WatchlistPoolProofSourceBindingAssembler {

    private static final String REASON_INPUT_MISSING = "WATCHLIST_POOL_PROOF_BINDING_INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final WatchlistPoolProofSourceBindingValidator validator;

    public WatchlistPoolProofSourceBindingAssembler() {
        this(new WatchlistPoolProofSourceBindingValidator());
    }

    public WatchlistPoolProofSourceBindingAssembler(WatchlistPoolProofSourceBindingValidator validator) {
        this.validator = validator == null ? new WatchlistPoolProofSourceBindingValidator() : validator;
    }

    public AssembledWatchlistPoolProofSourceBinding assemble(AssemblyInput input) {
        WatchlistPoolProofSourceBindingDTO context = contextFrom(input);
        WatchlistPoolProofSourceBindingValidator.ValidationResult validationResult = validator.validate(context);
        return new AssembledWatchlistPoolProofSourceBinding(context, validationResult);
    }

    private WatchlistPoolProofSourceBindingDTO contextFrom(AssemblyInput input) {
        if (input == null) {
            return WatchlistPoolProofSourceBindingDTO.incomplete(
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

        WatchlistPoolProofSourceBindingDTO.BindingStatus requestedStatus = input.getRequestedStatus();
        if (WatchlistPoolProofSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return WatchlistPoolProofSourceBindingDTO.blockedFailClosed(
                    input.getWatchlistPoolProofContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolRef(),
                    input.getBlockedReasons(),
                    input.getBlockedCandidateBoundaryLabel(),
                    requiredOrFallback(input.getBlockedReason(), REASON_BLOCKED_REASON_REQUIRED)
            );
        }

        if (WatchlistPoolProofSourceBindingDTO.BindingStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED
                .equals(requestedStatus)) {
            return WatchlistPoolProofSourceBindingDTO.degraded(
                    input.getWatchlistPoolProofContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolRef(),
                    input.getWatchlistPoolVersion(),
                    input.getWatchlistPoolEnabled(),
                    input.getWatchlistPoolEmpty(),
                    input.getWatchlistPoolMember(),
                    input.getWatchlistMembershipSource(),
                    input.getWatchlistMembershipObservedAt(),
                    input.getWatchlistMembershipExpiresAt(),
                    input.getProofFresh(),
                    input.getProofStale(),
                    input.getDisplaySlotOnly(),
                    input.getDefaultDisplaySlot(),
                    input.getDisplaySlotRef(),
                    input.getPromotedToHomeCandidate(),
                    input.getLowFrequencyScanCandidate(),
                    input.getAuditRef(),
                    input.getOperatorRef(),
                    input.getMembershipReason(),
                    input.getProofReason(),
                    input.getAllowedCandidateBoundaryLabel(),
                    input.getBlockedCandidateBoundaryLabel(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED),
                    input.getTrustedSource()
            );
        }

        if (WatchlistPoolProofSourceBindingDTO.BindingStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING
                .equals(requestedStatus)) {
            return WatchlistPoolProofSourceBindingDTO.reviewOnly(
                    input.getWatchlistPoolProofContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getPrimaryTimeframe(),
                    input.getSourceTraceRefs(),
                    input.getRuntimeKlineContextRef(),
                    input.getDataQualityContextRef(),
                    input.getMultiTimeframeContextRef(),
                    input.getRiskActionGuardContextRef(),
                    input.getWatchlistPoolRef(),
                    input.getWatchlistPoolVersion(),
                    input.getWatchlistPoolEnabled(),
                    input.getWatchlistPoolEmpty(),
                    input.getWatchlistPoolMember(),
                    input.getWatchlistMembershipSource(),
                    input.getWatchlistMembershipObservedAt(),
                    input.getWatchlistMembershipExpiresAt(),
                    input.getProofFresh(),
                    input.getProofStale(),
                    input.getDisplaySlotOnly(),
                    input.getDefaultDisplaySlot(),
                    input.getDisplaySlotRef(),
                    input.getPromotedToHomeCandidate(),
                    input.getLowFrequencyScanCandidate(),
                    input.getAuditRef(),
                    input.getOperatorRef(),
                    input.getMembershipReason(),
                    input.getProofReason(),
                    input.getAllowedCandidateBoundaryLabel(),
                    input.getBlockedCandidateBoundaryLabel(),
                    input.getMissingFields(),
                    input.getDegradedReasons(),
                    input.getBlockedReasons(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    input.getTrustedSource()
            );
        }

        String fallback = WatchlistPoolProofSourceBindingDTO.BindingStatus.INCOMPLETE.equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return WatchlistPoolProofSourceBindingDTO.incomplete(
                input.getWatchlistPoolProofContextId(),
                input.getSymbol(),
                input.getMarket(),
                input.getPrimaryTimeframe(),
                input.getSourceTraceRefs(),
                input.getRuntimeKlineContextRef(),
                input.getDataQualityContextRef(),
                input.getMultiTimeframeContextRef(),
                input.getRiskActionGuardContextRef(),
                input.getWatchlistPoolRef(),
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
        private final String watchlistPoolProofContextId;
        private final String symbol;
        private final String market;
        private final String primaryTimeframe;
        private final List<String> sourceTraceRefs;
        private final String runtimeKlineContextRef;
        private final String dataQualityContextRef;
        private final String multiTimeframeContextRef;
        private final String riskActionGuardContextRef;
        private final String watchlistPoolRef;
        private final String watchlistPoolVersion;
        private final Boolean watchlistPoolEnabled;
        private final Boolean watchlistPoolEmpty;
        private final Boolean watchlistPoolMember;
        private final String watchlistMembershipSource;
        private final String watchlistMembershipObservedAt;
        private final String watchlistMembershipExpiresAt;
        private final Boolean proofFresh;
        private final Boolean proofStale;
        private final Boolean displaySlotOnly;
        private final Boolean defaultDisplaySlot;
        private final String displaySlotRef;
        private final Boolean promotedToHomeCandidate;
        private final Boolean lowFrequencyScanCandidate;
        private final String auditRef;
        private final String operatorRef;
        private final String membershipReason;
        private final String proofReason;
        private final String allowedCandidateBoundaryLabel;
        private final String blockedCandidateBoundaryLabel;
        private final List<String> missingFields;
        private final List<String> degradedReasons;
        private final List<String> blockedReasons;
        private final String observedAt;
        private final String createdAt;
        private final String missingReason;
        private final String blockedReason;
        private final Boolean trustedSource;
        private final WatchlistPoolProofSourceBindingDTO.BindingStatus requestedStatus;

        private AssemblyInput(
                String watchlistPoolProofContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardContextRef,
                String watchlistPoolRef,
                String watchlistPoolVersion,
                Boolean watchlistPoolEnabled,
                Boolean watchlistPoolEmpty,
                Boolean watchlistPoolMember,
                String watchlistMembershipSource,
                String watchlistMembershipObservedAt,
                String watchlistMembershipExpiresAt,
                Boolean proofFresh,
                Boolean proofStale,
                Boolean displaySlotOnly,
                Boolean defaultDisplaySlot,
                String displaySlotRef,
                Boolean promotedToHomeCandidate,
                Boolean lowFrequencyScanCandidate,
                String auditRef,
                String operatorRef,
                String membershipReason,
                String proofReason,
                String allowedCandidateBoundaryLabel,
                String blockedCandidateBoundaryLabel,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                WatchlistPoolProofSourceBindingDTO.BindingStatus requestedStatus
        ) {
            this.watchlistPoolProofContextId = watchlistPoolProofContextId;
            this.symbol = symbol;
            this.market = market;
            this.primaryTimeframe = primaryTimeframe;
            this.sourceTraceRefs = copy(sourceTraceRefs);
            this.runtimeKlineContextRef = runtimeKlineContextRef;
            this.dataQualityContextRef = dataQualityContextRef;
            this.multiTimeframeContextRef = multiTimeframeContextRef;
            this.riskActionGuardContextRef = riskActionGuardContextRef;
            this.watchlistPoolRef = watchlistPoolRef;
            this.watchlistPoolVersion = watchlistPoolVersion;
            this.watchlistPoolEnabled = watchlistPoolEnabled;
            this.watchlistPoolEmpty = watchlistPoolEmpty;
            this.watchlistPoolMember = watchlistPoolMember;
            this.watchlistMembershipSource = watchlistMembershipSource;
            this.watchlistMembershipObservedAt = watchlistMembershipObservedAt;
            this.watchlistMembershipExpiresAt = watchlistMembershipExpiresAt;
            this.proofFresh = proofFresh;
            this.proofStale = proofStale;
            this.displaySlotOnly = displaySlotOnly;
            this.defaultDisplaySlot = defaultDisplaySlot;
            this.displaySlotRef = displaySlotRef;
            this.promotedToHomeCandidate = promotedToHomeCandidate;
            this.lowFrequencyScanCandidate = lowFrequencyScanCandidate;
            this.auditRef = auditRef;
            this.operatorRef = operatorRef;
            this.membershipReason = membershipReason;
            this.proofReason = proofReason;
            this.allowedCandidateBoundaryLabel = allowedCandidateBoundaryLabel;
            this.blockedCandidateBoundaryLabel = blockedCandidateBoundaryLabel;
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
                String watchlistPoolProofContextId,
                String symbol,
                String market,
                String primaryTimeframe,
                List<String> sourceTraceRefs,
                String runtimeKlineContextRef,
                String dataQualityContextRef,
                String multiTimeframeContextRef,
                String riskActionGuardContextRef,
                String watchlistPoolRef,
                String watchlistPoolVersion,
                Boolean watchlistPoolEnabled,
                Boolean watchlistPoolEmpty,
                Boolean watchlistPoolMember,
                String watchlistMembershipSource,
                String watchlistMembershipObservedAt,
                String watchlistMembershipExpiresAt,
                Boolean proofFresh,
                Boolean proofStale,
                Boolean displaySlotOnly,
                Boolean defaultDisplaySlot,
                String displaySlotRef,
                Boolean promotedToHomeCandidate,
                Boolean lowFrequencyScanCandidate,
                String auditRef,
                String operatorRef,
                String membershipReason,
                String proofReason,
                String allowedCandidateBoundaryLabel,
                String blockedCandidateBoundaryLabel,
                List<String> missingFields,
                List<String> degradedReasons,
                List<String> blockedReasons,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                WatchlistPoolProofSourceBindingDTO.BindingStatus requestedStatus
        ) {
            return new AssemblyInput(
                    watchlistPoolProofContextId,
                    symbol,
                    market,
                    primaryTimeframe,
                    sourceTraceRefs,
                    runtimeKlineContextRef,
                    dataQualityContextRef,
                    multiTimeframeContextRef,
                    riskActionGuardContextRef,
                    watchlistPoolRef,
                    watchlistPoolVersion,
                    watchlistPoolEnabled,
                    watchlistPoolEmpty,
                    watchlistPoolMember,
                    watchlistMembershipSource,
                    watchlistMembershipObservedAt,
                    watchlistMembershipExpiresAt,
                    proofFresh,
                    proofStale,
                    displaySlotOnly,
                    defaultDisplaySlot,
                    displaySlotRef,
                    promotedToHomeCandidate,
                    lowFrequencyScanCandidate,
                    auditRef,
                    operatorRef,
                    membershipReason,
                    proofReason,
                    allowedCandidateBoundaryLabel,
                    blockedCandidateBoundaryLabel,
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

        public String getWatchlistPoolProofContextId() {
            return watchlistPoolProofContextId;
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

        public String getWatchlistPoolRef() {
            return watchlistPoolRef;
        }

        public String getWatchlistPoolVersion() {
            return watchlistPoolVersion;
        }

        public Boolean getWatchlistPoolEnabled() {
            return watchlistPoolEnabled;
        }

        public Boolean getWatchlistPoolEmpty() {
            return watchlistPoolEmpty;
        }

        public Boolean getWatchlistPoolMember() {
            return watchlistPoolMember;
        }

        public String getWatchlistMembershipSource() {
            return watchlistMembershipSource;
        }

        public String getWatchlistMembershipObservedAt() {
            return watchlistMembershipObservedAt;
        }

        public String getWatchlistMembershipExpiresAt() {
            return watchlistMembershipExpiresAt;
        }

        public Boolean getProofFresh() {
            return proofFresh;
        }

        public Boolean getProofStale() {
            return proofStale;
        }

        public Boolean getDisplaySlotOnly() {
            return displaySlotOnly;
        }

        public Boolean getDefaultDisplaySlot() {
            return defaultDisplaySlot;
        }

        public String getDisplaySlotRef() {
            return displaySlotRef;
        }

        public Boolean getPromotedToHomeCandidate() {
            return promotedToHomeCandidate;
        }

        public Boolean getLowFrequencyScanCandidate() {
            return lowFrequencyScanCandidate;
        }

        public String getAuditRef() {
            return auditRef;
        }

        public String getOperatorRef() {
            return operatorRef;
        }

        public String getMembershipReason() {
            return membershipReason;
        }

        public String getProofReason() {
            return proofReason;
        }

        public String getAllowedCandidateBoundaryLabel() {
            return allowedCandidateBoundaryLabel;
        }

        public String getBlockedCandidateBoundaryLabel() {
            return blockedCandidateBoundaryLabel;
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

        public Boolean getTrustedSource() {
            return trustedSource;
        }

        public WatchlistPoolProofSourceBindingDTO.BindingStatus getRequestedStatus() {
            return requestedStatus;
        }

        private static List<String> copy(List<String> values) {
            if (values == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(values));
        }
    }

    public static class AssembledWatchlistPoolProofSourceBinding {
        private final WatchlistPoolProofSourceBindingDTO context;
        private final WatchlistPoolProofSourceBindingValidator.ValidationResult validationResult;

        private AssembledWatchlistPoolProofSourceBinding(
                WatchlistPoolProofSourceBindingDTO context,
                WatchlistPoolProofSourceBindingValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public WatchlistPoolProofSourceBindingDTO getContext() {
            return context;
        }

        public WatchlistPoolProofSourceBindingValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
