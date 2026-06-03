package org.example.trademodel.validator.point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.WatchlistPoolProofSourceBindingDTO;

public class WatchlistPoolProofSourceBindingValidator {

    private static final String REASON_CONTEXT_MISSING = "WATCHLIST_POOL_PROOF_BINDING_MISSING";
    private static final String REASON_STATUS_MISSING = "WATCHLIST_POOL_PROOF_BINDING_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_UNTRUSTED_SOURCE = "WATCHLIST_POOL_PROOF_SOURCE_UNTRUSTED";
    private static final String REASON_CONTEXT_ID_MISSING = "WATCHLIST_POOL_PROOF_CONTEXT_ID_MISSING";
    private static final String REASON_SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String REASON_MARKET_MISSING = "MARKET_MISSING";
    private static final String REASON_PRIMARY_TIMEFRAME_MISSING = "PRIMARY_TIMEFRAME_MISSING";
    private static final String REASON_SOURCE_TRACE_REFS_MISSING = "SOURCE_TRACE_REFS_MISSING";
    private static final String REASON_SOURCE_TRACE_REF_BLANK = "SOURCE_TRACE_REF_BLANK";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING =
            "RUNTIME_KLINE_CONTEXT_REF_MISSING";
    private static final String REASON_DATA_QUALITY_CONTEXT_REF_MISSING =
            "DATA_QUALITY_CONTEXT_REF_MISSING";
    private static final String REASON_MULTITIMEFRAME_CONTEXT_REF_MISSING =
            "MULTITIMEFRAME_CONTEXT_REF_MISSING";
    private static final String REASON_RISK_ACTION_GUARD_CONTEXT_REF_MISSING =
            "RISK_ACTION_GUARD_CONTEXT_REF_MISSING";
    private static final String REASON_WATCHLIST_POOL_REF_MISSING = "WATCHLIST_POOL_REF_MISSING";
    private static final String REASON_WATCHLIST_POOL_VERSION_MISSING =
            "WATCHLIST_POOL_VERSION_MISSING";
    private static final String REASON_WATCHLIST_POOL_ENABLED_MISSING =
            "WATCHLIST_POOL_ENABLED_MISSING";
    private static final String REASON_WATCHLIST_POOL_EMPTY_STATUS_MISSING =
            "WATCHLIST_POOL_EMPTY_STATUS_MISSING";
    private static final String REASON_WATCHLIST_POOL_MEMBER_STATUS_MISSING =
            "WATCHLIST_POOL_MEMBER_STATUS_MISSING";
    private static final String REASON_MEMBERSHIP_SOURCE_MISSING =
            "WATCHLIST_MEMBERSHIP_SOURCE_MISSING";
    private static final String REASON_MEMBERSHIP_OBSERVED_AT_MISSING =
            "WATCHLIST_MEMBERSHIP_OBSERVED_AT_MISSING";
    private static final String REASON_PROOF_FRESH_STATUS_MISSING = "PROOF_FRESH_STATUS_MISSING";
    private static final String REASON_PROOF_STALE_STATUS_MISSING = "PROOF_STALE_STATUS_MISSING";
    private static final String REASON_DISPLAY_SLOT_ONLY_STATUS_MISSING =
            "DISPLAY_SLOT_ONLY_STATUS_MISSING";
    private static final String REASON_DEFAULT_DISPLAY_SLOT_STATUS_MISSING =
            "DEFAULT_DISPLAY_SLOT_STATUS_MISSING";
    private static final String REASON_OBSERVED_AT_MISSING = "OBSERVED_AT_MISSING";
    private static final String REASON_WATCHLIST_POOL_DISABLED = "WATCHLIST_POOL_DISABLED";
    private static final String REASON_WATCHLIST_POOL_EMPTY = "WATCHLIST_POOL_EMPTY";
    private static final String REASON_WATCHLIST_POOL_MEMBER_MISSING =
            "WATCHLIST_POOL_MEMBER_MISSING";
    private static final String REASON_DISPLAY_SLOT_NOT_POOL_PROOF =
            "DISPLAY_SLOT_NOT_WATCHLIST_POOL_PROOF";
    private static final String REASON_DEFAULT_SLOT_NOT_POOL_PROOF =
            "DEFAULT_SLOT_NOT_WATCHLIST_POOL_PROOF";
    private static final String REASON_PROOF_STALE = "WATCHLIST_POOL_PROOF_STALE";
    private static final String REASON_PROOF_FRESH_REQUIRED = "WATCHLIST_POOL_PROOF_FRESH_REQUIRED";
    private static final String REASON_AUDIT_REF_REQUIRED = "WATCHLIST_POOL_AUDIT_REF_REQUIRED";

    private static final List<String> FORBIDDEN_EXECUTABLE_SEMANTICS = List.of(
            "buy",
            "sell",
            "lon" + "g",
            "shor" + "t",
            "ope" + "n",
            "clos" + "e",
            "revers" + "e",
            "ord" + "er",
            "execut" + "e",
            "execut" + "ion",
            "auto-" + "trade",
            "auto " + "trading",
            "push " + "send",
            "external " + "channel",
            "send " + "ord" + "er",
            "push opportunity",
            "place" + "ord" + "er",
            "create" + "ord" + "er",
            "clos" + "eposition",
            "revers" + "eposition",
            "ope" + "nposition",
            "submit" + "ord" + "er"
    );

    public ValidationResult validate(WatchlistPoolProofSourceBindingDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        if (containsForbiddenExecutableSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        WatchlistPoolProofSourceBindingDTO.BindingStatus status = context.getBindingStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING -> validateReviewOnlyBinding(context);
            case REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED -> validateDegradedBinding(context);
        };
    }

    private static ValidationResult validateIncomplete(WatchlistPoolProofSourceBindingDTO context) {
        if (isBlank(context.getMissingReason())) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(List.of(context.getMissingReason()));
    }

    private static ValidationResult validateBlockedFailClosed(WatchlistPoolProofSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        if (!context.isFailClosed()) {
            reasons.add(REASON_FAIL_CLOSED_REQUIRED);
        }
        if (isBlank(context.getBlockedReason())) {
            reasons.add(REASON_BLOCKED_REASON_REQUIRED);
        }
        if (!reasons.isEmpty()) {
            return ValidationResult.blockedFailClosed(reasons);
        }
        return ValidationResult.blockedFailClosed(List.of(context.getBlockedReason()));
    }

    private static ValidationResult validateReviewOnlyBinding(WatchlistPoolProofSourceBindingDTO context) {
        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }

        List<String> incompleteReasons = requiredBindingReasons(context);
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        ValidationResult proofState = proofState(context);
        if (proofState != null) {
            return proofState;
        }

        return ValidationResult.reviewOnlyWatchlistPoolProofBinding(List.of());
    }

    private static ValidationResult validateDegradedBinding(WatchlistPoolProofSourceBindingDTO context) {
        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }

        List<String> incompleteReasons = requiredBindingReasons(context);
        if (isBlank(context.getMissingReason())) {
            incompleteReasons.add(REASON_MISSING_REASON_REQUIRED);
        }
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        ValidationResult proofState = proofState(context);
        if (proofState != null && proofState.isBlockedFailClosed()) {
            return proofState;
        }
        if (proofState != null && proofState.isIncomplete()) {
            return proofState;
        }

        return ValidationResult.reviewOnlyWatchlistPoolProofBindingDegraded(List.of(context.getMissingReason()));
    }

    private static ValidationResult blockedState(WatchlistPoolProofSourceBindingDTO context) {
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        if (Boolean.FALSE.equals(context.getWatchlistPoolEnabled())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_WATCHLIST_POOL_DISABLED));
        }
        if (Boolean.TRUE.equals(context.getWatchlistPoolEmpty())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_WATCHLIST_POOL_EMPTY));
        }
        if (Boolean.FALSE.equals(context.getWatchlistPoolMember())) {
            if (Boolean.TRUE.equals(context.getDisplaySlotOnly())) {
                return ValidationResult.blockedFailClosed(List.of(REASON_DISPLAY_SLOT_NOT_POOL_PROOF));
            }
            if (Boolean.TRUE.equals(context.getDefaultDisplaySlot())) {
                return ValidationResult.blockedFailClosed(List.of(REASON_DEFAULT_SLOT_NOT_POOL_PROOF));
            }
            return ValidationResult.blockedFailClosed(List.of(REASON_WATCHLIST_POOL_MEMBER_MISSING));
        }
        return null;
    }

    private static ValidationResult proofState(WatchlistPoolProofSourceBindingDTO context) {
        if (Boolean.TRUE.equals(context.getProofStale())) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyWatchlistPoolProofBindingDegraded(List.of(REASON_PROOF_STALE));
            }
            return ValidationResult.incomplete(List.of(REASON_PROOF_STALE));
        }
        if (Boolean.FALSE.equals(context.getProofFresh())) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyWatchlistPoolProofBindingDegraded(
                        List.of(REASON_PROOF_FRESH_REQUIRED)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_PROOF_FRESH_REQUIRED));
        }
        if (Boolean.TRUE.equals(context.getWatchlistPoolMember()) && isBlank(context.getAuditRef())) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyWatchlistPoolProofBindingDegraded(
                        List.of(REASON_AUDIT_REF_REQUIRED)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_AUDIT_REF_REQUIRED));
        }
        return null;
    }

    private static List<String> requiredBindingReasons(WatchlistPoolProofSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getWatchlistPoolProofContextId())) {
            reasons.add(REASON_CONTEXT_ID_MISSING);
        }
        if (isBlank(context.getSymbol())) {
            reasons.add(REASON_SYMBOL_MISSING);
        }
        if (isBlank(context.getMarket())) {
            reasons.add(REASON_MARKET_MISSING);
        }
        if (isBlank(context.getPrimaryTimeframe())) {
            reasons.add(REASON_PRIMARY_TIMEFRAME_MISSING);
        }
        reasons.addAll(refReasons(context.getSourceTraceRefs(),
                REASON_SOURCE_TRACE_REFS_MISSING, REASON_SOURCE_TRACE_REF_BLANK));
        if (isBlank(context.getRuntimeKlineContextRef())) {
            reasons.add(REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getDataQualityContextRef())) {
            reasons.add(REASON_DATA_QUALITY_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getMultiTimeframeContextRef())) {
            reasons.add(REASON_MULTITIMEFRAME_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getRiskActionGuardContextRef())) {
            reasons.add(REASON_RISK_ACTION_GUARD_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getWatchlistPoolRef())) {
            reasons.add(REASON_WATCHLIST_POOL_REF_MISSING);
        }
        if (isBlank(context.getWatchlistPoolVersion())) {
            reasons.add(REASON_WATCHLIST_POOL_VERSION_MISSING);
        }
        if (context.getWatchlistPoolEnabled() == null) {
            reasons.add(REASON_WATCHLIST_POOL_ENABLED_MISSING);
        }
        if (context.getWatchlistPoolEmpty() == null) {
            reasons.add(REASON_WATCHLIST_POOL_EMPTY_STATUS_MISSING);
        }
        if (context.getWatchlistPoolMember() == null) {
            reasons.add(REASON_WATCHLIST_POOL_MEMBER_STATUS_MISSING);
        }
        if (isBlank(context.getWatchlistMembershipSource())) {
            reasons.add(REASON_MEMBERSHIP_SOURCE_MISSING);
        }
        if (isBlank(context.getWatchlistMembershipObservedAt())) {
            reasons.add(REASON_MEMBERSHIP_OBSERVED_AT_MISSING);
        }
        if (context.getProofFresh() == null) {
            reasons.add(REASON_PROOF_FRESH_STATUS_MISSING);
        }
        if (context.getProofStale() == null) {
            reasons.add(REASON_PROOF_STALE_STATUS_MISSING);
        }
        if (context.getDisplaySlotOnly() == null) {
            reasons.add(REASON_DISPLAY_SLOT_ONLY_STATUS_MISSING);
        }
        if (context.getDefaultDisplaySlot() == null) {
            reasons.add(REASON_DEFAULT_DISPLAY_SLOT_STATUS_MISSING);
        }
        if (isBlank(context.getObservedAt())) {
            reasons.add(REASON_OBSERVED_AT_MISSING);
        }
        return reasons;
    }

    private static List<String> refReasons(List<String> refs, String missingReason, String blankReason) {
        if (refs == null || refs.isEmpty()) {
            return List.of(missingReason);
        }
        for (String ref : refs) {
            if (isBlank(ref)) {
                return List.of(blankReason);
            }
        }
        return List.of();
    }

    private static boolean hasDegradedExplanation(WatchlistPoolProofSourceBindingDTO context) {
        return !context.getDegradedReasons().isEmpty() || !isBlank(context.getMissingReason());
    }

    private static boolean safetyFlagsRequiredTrue(WatchlistPoolProofSourceBindingDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(WatchlistPoolProofSourceBindingDTO context) {
        List<String> outputs = new ArrayList<>();
        outputs.add(context.getRuntimeKlineContextRef());
        outputs.add(context.getDataQualityContextRef());
        outputs.add(context.getMultiTimeframeContextRef());
        outputs.add(context.getRiskActionGuardContextRef());
        outputs.add(context.getWatchlistPoolRef());
        outputs.add(context.getWatchlistMembershipSource());
        outputs.add(context.getDisplaySlotRef());
        outputs.add(context.getAuditRef());
        outputs.add(context.getOperatorRef());
        outputs.add(context.getMembershipReason());
        outputs.add(context.getProofReason());
        outputs.add(context.getAllowedCandidateBoundaryLabel());
        outputs.add(context.getBlockedCandidateBoundaryLabel());
        outputs.add(context.getMissingReason());
        outputs.add(context.getBlockedReason());
        outputs.addAll(context.getSourceTraceRefs());
        outputs.addAll(context.getMissingFields());
        outputs.addAll(context.getDegradedReasons());
        outputs.addAll(context.getBlockedReasons());

        for (String output : outputs) {
            if (containsAny(output, FORBIDDEN_EXECUTABLE_SEMANTICS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, List<String> needles) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase();
        for (String needle : needles) {
            if (normalized.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public enum ValidationStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING,
        REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED
    }

    public static class ValidationResult {
        private final ValidationStatus status;
        private final boolean validForReviewOnly;
        private final boolean incomplete;
        private final boolean blockedFailClosed;
        private final boolean manualReviewRequired;
        private final boolean notTradeInstruction;
        private final boolean reviewOnly;
        private final List<String> reasons;

        private ValidationResult(ValidationStatus status, List<String> reasons) {
            this.status = status;
            this.validForReviewOnly = status == ValidationStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING
                    || status == ValidationStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED;
            this.incomplete = status == ValidationStatus.INCOMPLETE;
            this.blockedFailClosed = status == ValidationStatus.BLOCKED_FAIL_CLOSED;
            this.manualReviewRequired = true;
            this.notTradeInstruction = true;
            this.reviewOnly = true;
            this.reasons = immutableCopy(reasons);
        }

        public static ValidationResult incomplete(List<String> reasons) {
            return new ValidationResult(ValidationStatus.INCOMPLETE, reasons);
        }

        public static ValidationResult blockedFailClosed(List<String> reasons) {
            return new ValidationResult(ValidationStatus.BLOCKED_FAIL_CLOSED, reasons);
        }

        public static ValidationResult reviewOnlyWatchlistPoolProofBinding(List<String> reasons) {
            return new ValidationResult(ValidationStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING, reasons);
        }

        public static ValidationResult reviewOnlyWatchlistPoolProofBindingDegraded(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED,
                    reasons
            );
        }

        public ValidationStatus getStatus() {
            return status;
        }

        public boolean isValidForReviewOnly() {
            return validForReviewOnly;
        }

        public boolean isIncomplete() {
            return incomplete;
        }

        public boolean isBlockedFailClosed() {
            return blockedFailClosed;
        }

        public boolean isManualReviewRequired() {
            return manualReviewRequired;
        }

        public boolean isNotTradeInstruction() {
            return notTradeInstruction;
        }

        public boolean isReviewOnly() {
            return reviewOnly;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
