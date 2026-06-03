package org.example.trademodel.validator.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationSourceBindingDTO;

public class SourceOwnedCandidateIntegrationSourceBindingValidator {

    private static final BigDecimal MINIMUM_REVIEW_ONLY_COMPLETENESS = new BigDecimal("85");

    private static final String REASON_CONTEXT_MISSING = "CANDIDATE_INTEGRATION_BINDING_MISSING";
    private static final String REASON_STATUS_MISSING = "BINDING_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_DEGRADED_REASON_REQUIRED = "DEGRADED_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_UNTRUSTED_SOURCE = "CANDIDATE_INTEGRATION_SOURCE_UNTRUSTED";
    private static final String REASON_CONTEXT_ID_MISSING = "CANDIDATE_INTEGRATION_CONTEXT_ID_MISSING";
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
    private static final String REASON_WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING =
            "WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING";
    private static final String REASON_COMPLETENESS_SCORE_MISSING =
            "SOURCE_BINDING_COMPLETENESS_SCORE_MISSING";
    private static final String REASON_COMPLETENESS_SCORE_TOO_LOW =
            "SOURCE_BINDING_COMPLETENESS_SCORE_TOO_LOW";
    private static final String REASON_REQUIRED_SOURCES_PRESENT_MISSING =
            "ALL_REQUIRED_SOURCES_PRESENT_MISSING";
    private static final String REASON_REQUIRED_SOURCES_TRUSTED_MISSING =
            "ALL_REQUIRED_SOURCES_TRUSTED_MISSING";
    private static final String REASON_REQUIRED_SOURCES_REVIEW_ONLY_MISSING =
            "ALL_REQUIRED_SOURCES_REVIEW_ONLY_MISSING";
    private static final String REASON_REQUIRED_SOURCES_NOT_TRADE_INSTRUCTION_MISSING =
            "ALL_REQUIRED_SOURCES_NOT_TRADE_INSTRUCTION_MISSING";
    private static final String REASON_REQUIRED_SOURCES_MANUAL_REVIEW_REQUIRED_MISSING =
            "ALL_REQUIRED_SOURCES_MANUAL_REVIEW_REQUIRED_MISSING";
    private static final String REASON_REQUIRED_SOURCES_INCOMPLETE_SAFE_MISSING =
            "ALL_REQUIRED_SOURCES_INCOMPLETE_SAFE_MISSING";
    private static final String REASON_ANY_SOURCE_BLOCKED_MISSING = "ANY_SOURCE_BLOCKED_MISSING";
    private static final String REASON_ANY_SOURCE_INCOMPLETE_MISSING = "ANY_SOURCE_INCOMPLETE_MISSING";
    private static final String REASON_ANY_SOURCE_DEGRADED_MISSING = "ANY_SOURCE_DEGRADED_MISSING";
    private static final String REASON_REQUIRED_SOURCE_MISSING = "REQUIRED_SOURCE_MISSING";
    private static final String REASON_REQUIRED_SOURCE_UNTRUSTED = "REQUIRED_SOURCE_UNTRUSTED";
    private static final String REASON_REQUIRED_SOURCE_NOT_REVIEW_ONLY =
            "REQUIRED_SOURCE_NOT_REVIEW_ONLY";
    private static final String REASON_REQUIRED_SOURCE_TRADE_INSTRUCTION =
            "REQUIRED_SOURCE_TRADE_INSTRUCTION";
    private static final String REASON_REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED =
            "REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED";
    private static final String REASON_REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE =
            "REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE";
    private static final String REASON_UPSTREAM_SOURCE_BLOCKED = "UPSTREAM_SOURCE_BLOCKED";
    private static final String REASON_UPSTREAM_SOURCE_INCOMPLETE = "UPSTREAM_SOURCE_INCOMPLETE";
    private static final String REASON_UPSTREAM_SOURCE_DEGRADED = "UPSTREAM_SOURCE_DEGRADED";
    private static final String REASON_RISK_ACTION_GUARD_BOUNDARY_BLOCKED =
            "RISK_ACTION_GUARD_BOUNDARY_BLOCKED";
    private static final String REASON_WATCHLIST_POOL_PROOF_BOUNDARY_BLOCKED =
            "WATCHLIST_POOL_PROOF_BOUNDARY_BLOCKED";
    private static final String REASON_WATCHLIST_POOL_PROOF_STALE =
            "WATCHLIST_POOL_PROOF_STALE";
    private static final String REASON_DISPLAY_SLOT_NOT_POOL_PROOF =
            "DISPLAY_SLOT_NOT_WATCHLIST_POOL_PROOF";

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
            "submit" + "ord" + "er",
            "one-shot exit"
    );

    public ValidationResult validate(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        if (containsForbiddenExecutableSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus status = context.getBindingStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING -> validateReviewOnlyBinding(context);
            case REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED ->
                    validateDegradedBinding(context);
        };
    }

    private static ValidationResult validateIncomplete(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
        if (isBlank(context.getMissingReason())) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(List.of(context.getMissingReason()));
    }

    private static ValidationResult validateBlockedFailClosed(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
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

    private static ValidationResult validateReviewOnlyBinding(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }

        List<String> incompleteReasons = requiredBindingReasons(context);
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        ValidationResult sourceSummaryState = sourceSummaryState(context);
        if (sourceSummaryState != null) {
            return sourceSummaryState;
        }

        return ValidationResult.reviewOnlySourceOwnedCandidateIntegrationBinding(List.of());
    }

    private static ValidationResult validateDegradedBinding(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }

        List<String> incompleteReasons = requiredBindingReasons(context);
        if (isBlank(context.getMissingReason())) {
            incompleteReasons.add(REASON_MISSING_REASON_REQUIRED);
        }
        if (missingDegradedExplanation(context)) {
            incompleteReasons.add(REASON_DEGRADED_REASON_REQUIRED);
        }
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        ValidationResult sourceSummaryState = sourceSummaryState(context);
        if (sourceSummaryState != null && sourceSummaryState.isBlockedFailClosed()) {
            return sourceSummaryState;
        }
        if (sourceSummaryState != null && sourceSummaryState.isIncomplete()) {
            return sourceSummaryState;
        }

        return ValidationResult.reviewOnlySourceOwnedCandidateIntegrationBindingDegraded(
                List.of(context.getMissingReason())
        );
    }

    private static ValidationResult blockedState(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesTrusted())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_UNTRUSTED));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesReviewOnly())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_NOT_REVIEW_ONLY));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesNotTradeInstruction())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_TRADE_INSTRUCTION));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesManualReviewRequired())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesIncompleteSafe())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE));
        }
        if (Boolean.TRUE.equals(context.getAnySourceBlocked())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UPSTREAM_SOURCE_BLOCKED));
        }
        if (containsRiskActionGuardBlockedStatus(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_RISK_ACTION_GUARD_BOUNDARY_BLOCKED));
        }
        if (containsWatchlistPoolBlockedStatus(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_WATCHLIST_POOL_PROOF_BOUNDARY_BLOCKED));
        }
        if (displaySlotTreatedAsPoolProof(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_DISPLAY_SLOT_NOT_POOL_PROOF));
        }
        return null;
    }

    private static ValidationResult sourceSummaryState(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesPresent())) {
            return ValidationResult.incomplete(List.of(REASON_REQUIRED_SOURCE_MISSING));
        }
        if (Boolean.TRUE.equals(context.getAnySourceIncomplete())) {
            return ValidationResult.incomplete(List.of(REASON_UPSTREAM_SOURCE_INCOMPLETE));
        }
        if (containsWatchlistPoolStaleStatus(context)) {
            return ValidationResult.incomplete(List.of(REASON_WATCHLIST_POOL_PROOF_STALE));
        }
        if (context.getSourceBindingCompletenessScore().compareTo(MINIMUM_REVIEW_ONLY_COMPLETENESS) < 0) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlySourceOwnedCandidateIntegrationBindingDegraded(
                        List.of(REASON_COMPLETENESS_SCORE_TOO_LOW)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_COMPLETENESS_SCORE_TOO_LOW));
        }
        if (Boolean.TRUE.equals(context.getAnySourceDegraded())) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlySourceOwnedCandidateIntegrationBindingDegraded(
                        List.of(REASON_UPSTREAM_SOURCE_DEGRADED)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_DEGRADED_REASON_REQUIRED));
        }
        return null;
    }

    private static List<String> requiredBindingReasons(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getCandidateIntegrationContextId())) {
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
        if (isBlank(context.getWatchlistPoolProofContextRef())) {
            reasons.add(REASON_WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING);
        }
        if (context.getSourceBindingCompletenessScore() == null) {
            reasons.add(REASON_COMPLETENESS_SCORE_MISSING);
        }
        if (context.getAllRequiredSourcesPresent() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_PRESENT_MISSING);
        }
        if (context.getAllRequiredSourcesTrusted() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_TRUSTED_MISSING);
        }
        if (context.getAllRequiredSourcesReviewOnly() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_REVIEW_ONLY_MISSING);
        }
        if (context.getAllRequiredSourcesNotTradeInstruction() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_NOT_TRADE_INSTRUCTION_MISSING);
        }
        if (context.getAllRequiredSourcesManualReviewRequired() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_MANUAL_REVIEW_REQUIRED_MISSING);
        }
        if (context.getAllRequiredSourcesIncompleteSafe() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_INCOMPLETE_SAFE_MISSING);
        }
        if (context.getAnySourceBlocked() == null) {
            reasons.add(REASON_ANY_SOURCE_BLOCKED_MISSING);
        }
        if (context.getAnySourceIncomplete() == null) {
            reasons.add(REASON_ANY_SOURCE_INCOMPLETE_MISSING);
        }
        if (context.getAnySourceDegraded() == null) {
            reasons.add(REASON_ANY_SOURCE_DEGRADED_MISSING);
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

    private static boolean containsRiskActionGuardBlockedStatus(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        return containsAny(context.getRiskActionGuardStatus(), List.of(
                "BLOCKED",
                "STAMPEDE",
                "WICK_ONLY_" + "REVERSE",
                "LIQUIDITY_DEGRADED_ONE_SHOT_EXIT"
        ));
    }

    private static boolean containsWatchlistPoolBlockedStatus(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        return containsAny(context.getWatchlistPoolProofStatus(), List.of(
                "DISABLED",
                "EMPTY",
                "NON_MEMBER",
                "UNTRUSTED"
        ));
    }

    private static boolean containsWatchlistPoolStaleStatus(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        return containsAny(context.getWatchlistPoolProofStatus(), List.of("STALE"));
    }

    private static boolean displaySlotTreatedAsPoolProof(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        return containsAny(context.getCandidateBoundaryLabel(), List.of("DISPLAY_SLOT_PROOF", "DEFAULT_HOME_PROOF"))
                || containsAny(context.getCandidateUnavailableReason(), List.of("DISPLAY_SLOT_PROOF"))
                || containsAny(context.getCandidateBlockedReason(), List.of("DISPLAY_SLOT_PROOF"))
                || containsAny(context.getCandidateDegradedReason(), List.of("DISPLAY_SLOT_PROOF"))
                || containsAny(context.getBlockedReasons(), List.of("DISPLAY_SLOT_PROOF", "DEFAULT_HOME_PROOF"));
    }

    private static boolean missingDegradedExplanation(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
        return isBlank(context.getCandidateDegradedReason()) && context.getDegradedReasons().isEmpty();
    }

    private static boolean hasDegradedExplanation(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
        return !context.getDegradedReasons().isEmpty()
                || !isBlank(context.getCandidateDegradedReason())
                || !isBlank(context.getMissingReason());
    }

    private static boolean safetyFlagsRequiredTrue(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(
            SourceOwnedCandidateIntegrationSourceBindingDTO context
    ) {
        List<String> outputs = new ArrayList<>();
        outputs.add(context.getCandidateIntegrationContextId());
        outputs.add(context.getSymbol());
        outputs.add(context.getMarket());
        outputs.add(context.getPrimaryTimeframe());
        outputs.add(context.getRuntimeKlineContextRef());
        outputs.add(context.getDataQualityContextRef());
        outputs.add(context.getMultiTimeframeContextRef());
        outputs.add(context.getRiskActionGuardContextRef());
        outputs.add(context.getWatchlistPoolProofContextRef());
        outputs.add(context.getSourceTraceStatus());
        outputs.add(context.getRuntimeKlineStatus());
        outputs.add(context.getDataQualityStatus());
        outputs.add(context.getMultiTimeframeStatus());
        outputs.add(context.getRiskActionGuardStatus());
        outputs.add(context.getWatchlistPoolProofStatus());
        outputs.add(context.getCandidateBoundaryLabel());
        outputs.add(context.getCandidateUnavailableReason());
        outputs.add(context.getCandidateBlockedReason());
        outputs.add(context.getCandidateDegradedReason());
        outputs.add(context.getMissingReason());
        outputs.add(context.getBlockedReason());
        outputs.addAll(context.getSourceTraceRefs());
        outputs.addAll(context.getSourceOwnedTraceRefs());
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
            if (normalized.contains(needle.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(List<String> values, List<String> needles) {
        for (String value : values) {
            if (containsAny(value, needles)) {
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
        REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING,
        REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
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
            this.validForReviewOnly =
                    status == ValidationStatus.REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING
                            || status == ValidationStatus
                            .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED;
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

        public static ValidationResult reviewOnlySourceOwnedCandidateIntegrationBinding(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING,
                    reasons
            );
        }

        public static ValidationResult reviewOnlySourceOwnedCandidateIntegrationBindingDegraded(
                List<String> reasons
        ) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED,
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
