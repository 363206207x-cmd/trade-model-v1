package org.example.trademodel.validator.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.MultiTimeframeContextSourceBindingDTO;

public class MultiTimeframeContextSourceBindingValidator {

    private static final BigDecimal MIN_ALIGNMENT_SCORE = new BigDecimal("70");
    private static final BigDecimal MIN_WEIGHTED_AGREEMENT_SCORE = new BigDecimal("70");
    private static final BigDecimal HIGH_CONFLICT_SCORE = new BigDecimal("50");
    private static final BigDecimal BLOCKED_CONFLICT_SCORE = new BigDecimal("85");

    private static final String REASON_CONTEXT_MISSING = "MULTITIMEFRAME_CONTEXT_BINDING_MISSING";
    private static final String REASON_STATUS_MISSING = "MULTITIMEFRAME_BINDING_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_UNTRUSTED_SOURCE = "MULTITIMEFRAME_SOURCE_UNTRUSTED";
    private static final String REASON_HARD_THRESHOLD_BLOCKED = "HARD_THRESHOLD_BLOCKED";
    private static final String REASON_HIGH_CONFLICT_BLOCKED = "HIGH_TIMEFRAME_CONFLICT_BLOCKED";
    private static final String REASON_MULTITIMEFRAME_CONTEXT_ID_MISSING =
            "MULTITIMEFRAME_CONTEXT_ID_MISSING";
    private static final String REASON_SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String REASON_MARKET_MISSING = "MARKET_MISSING";
    private static final String REASON_PRIMARY_TIMEFRAME_MISSING = "PRIMARY_TIMEFRAME_MISSING";
    private static final String REASON_SOURCE_TRACE_REFS_MISSING = "SOURCE_TRACE_REFS_MISSING";
    private static final String REASON_SOURCE_TRACE_REF_BLANK = "SOURCE_TRACE_REF_BLANK";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING =
            "RUNTIME_KLINE_CONTEXT_REF_MISSING";
    private static final String REASON_DATA_QUALITY_CONTEXT_REF_MISSING =
            "DATA_QUALITY_CONTEXT_REF_MISSING";
    private static final String REASON_TIMEFRAME_REFS_MISSING = "TIMEFRAME_REFS_MISSING";
    private static final String REASON_TIMEFRAME_REF_BLANK = "TIMEFRAME_REF_BLANK";
    private static final String REASON_ALIGNMENT_SCORE_MISSING = "ALIGNMENT_SCORE_MISSING";
    private static final String REASON_ALIGNMENT_SCORE_LOW = "ALIGNMENT_SCORE_LOW";
    private static final String REASON_CONFLICT_SCORE_MISSING = "CONFLICT_SCORE_MISSING";
    private static final String REASON_CONFLICT_SCORE_DEGRADED = "CONFLICT_SCORE_DEGRADED";
    private static final String REASON_WEIGHTED_AGREEMENT_SCORE_MISSING =
            "WEIGHTED_AGREEMENT_SCORE_MISSING";
    private static final String REASON_WEIGHTED_AGREEMENT_SCORE_LOW =
            "WEIGHTED_AGREEMENT_SCORE_LOW";
    private static final String REASON_MINIMUM_TIMEFRAMES_MISSING =
            "MINIMUM_TIMEFRAMES_STATUS_MISSING";
    private static final String REASON_MINIMUM_TIMEFRAMES_NOT_PASSED =
            "MINIMUM_TIMEFRAMES_NOT_PASSED";
    private static final String REASON_DATA_QUALITY_STATUS_MISSING =
            "DATA_QUALITY_STATUS_MISSING";
    private static final String REASON_DATA_QUALITY_NOT_PASSED = "DATA_QUALITY_NOT_PASSED";
    private static final String REASON_HARD_THRESHOLD_STATUS_MISSING =
            "HARD_THRESHOLD_STATUS_MISSING";
    private static final String REASON_WARNING_THRESHOLD_STATUS_MISSING =
            "WARNING_THRESHOLD_STATUS_MISSING";
    private static final String REASON_WARNING_THRESHOLD_DEGRADED =
            "WARNING_THRESHOLD_DEGRADED";
    private static final String REASON_MISSING_TIMEFRAMES_PRESENT =
            "MISSING_TIMEFRAMES_PRESENT";
    private static final String REASON_STALE_TIMEFRAMES_PRESENT =
            "STALE_TIMEFRAMES_PRESENT";
    private static final String REASON_OBSERVED_AT_MISSING = "OBSERVED_AT_MISSING";

    private static final List<String> FORBIDDEN_EXECUTABLE_SEMANTICS = List.of(
            "buy",
            "sell",
            "long",
            "short",
            "open long",
            "open short",
            "close position",
            "reverse",
            "reverse position",
            "market close",
            "market cut",
            "order",
            "execute",
            "execution",
            "auto-trade",
            "auto trading",
            "take-profit order",
            "stop-loss order",
            "send order",
            "push opportunity",
            "placeorder",
            "createorder",
            "closeposition",
            "reverseposition"
    );

    public ValidationResult validate(MultiTimeframeContextSourceBindingDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        if (containsForbiddenExecutableSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        MultiTimeframeContextSourceBindingDTO.BindingStatus status = context.getBindingStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_MULTITIMEFRAME_BINDING -> validateReviewOnlyBinding(context);
            case REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED -> validateDegradedBinding(context);
        };
    }

    private static ValidationResult validateIncomplete(MultiTimeframeContextSourceBindingDTO context) {
        if (isBlank(context.getMissingReason())) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(List.of(context.getMissingReason()));
    }

    private static ValidationResult validateBlockedFailClosed(MultiTimeframeContextSourceBindingDTO context) {
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

    private static ValidationResult validateReviewOnlyBinding(MultiTimeframeContextSourceBindingDTO context) {
        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }

        List<String> incompleteReasons = requiredBindingReasons(context);
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        ValidationResult scoreState = scoreState(context);
        if (scoreState != null) {
            return scoreState;
        }
        return ValidationResult.reviewOnlyMultiTimeframeBinding(List.of());
    }

    private static ValidationResult validateDegradedBinding(MultiTimeframeContextSourceBindingDTO context) {
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

        ValidationResult scoreState = scoreState(context);
        if (scoreState != null) {
            return scoreState;
        }
        return ValidationResult.reviewOnlyMultiTimeframeBindingDegraded(List.of(context.getMissingReason()));
    }

    private static ValidationResult blockedState(MultiTimeframeContextSourceBindingDTO context) {
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        if (Boolean.FALSE.equals(context.getHardThresholdPassed())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_HARD_THRESHOLD_BLOCKED));
        }
        if (context.getConflictScore() != null
                && context.getConflictScore().compareTo(BLOCKED_CONFLICT_SCORE) > 0
                && hasBlockedExplanation(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_HIGH_CONFLICT_BLOCKED));
        }
        return null;
    }

    private static ValidationResult scoreState(MultiTimeframeContextSourceBindingDTO context) {
        if (context.getAlignmentScore().compareTo(MIN_ALIGNMENT_SCORE) < 0) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyMultiTimeframeBindingDegraded(List.of(REASON_ALIGNMENT_SCORE_LOW));
            }
            return ValidationResult.incomplete(List.of(REASON_ALIGNMENT_SCORE_LOW));
        }
        if (context.getWeightedAgreementScore().compareTo(MIN_WEIGHTED_AGREEMENT_SCORE) < 0) {
            return ValidationResult.incomplete(List.of(REASON_WEIGHTED_AGREEMENT_SCORE_LOW));
        }
        if (context.getConflictScore().compareTo(HIGH_CONFLICT_SCORE) > 0) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyMultiTimeframeBindingDegraded(
                        List.of(REASON_CONFLICT_SCORE_DEGRADED)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        if (Boolean.FALSE.equals(context.getWarningThresholdPassed())) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyMultiTimeframeBindingDegraded(
                        List.of(REASON_WARNING_THRESHOLD_DEGRADED)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_WARNING_THRESHOLD_DEGRADED));
        }
        if (!context.getMissingTimeframes().isEmpty()) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyMultiTimeframeBindingDegraded(
                        List.of(REASON_MISSING_TIMEFRAMES_PRESENT)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_MISSING_TIMEFRAMES_PRESENT));
        }
        if (!context.getStaleTimeframes().isEmpty()) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyMultiTimeframeBindingDegraded(
                        List.of(REASON_STALE_TIMEFRAMES_PRESENT)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_STALE_TIMEFRAMES_PRESENT));
        }
        return null;
    }

    private static List<String> requiredBindingReasons(MultiTimeframeContextSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getMultiTimeframeContextId())) {
            reasons.add(REASON_MULTITIMEFRAME_CONTEXT_ID_MISSING);
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
        reasons.addAll(refReasons(context.getTimeframeRefs(),
                REASON_TIMEFRAME_REFS_MISSING, REASON_TIMEFRAME_REF_BLANK));
        if (context.getAlignmentScore() == null) {
            reasons.add(REASON_ALIGNMENT_SCORE_MISSING);
        }
        if (context.getConflictScore() == null) {
            reasons.add(REASON_CONFLICT_SCORE_MISSING);
        }
        if (context.getWeightedAgreementScore() == null) {
            reasons.add(REASON_WEIGHTED_AGREEMENT_SCORE_MISSING);
        }
        if (context.getMinimumRequiredTimeframesPassed() == null) {
            reasons.add(REASON_MINIMUM_TIMEFRAMES_MISSING);
        } else if (!context.getMinimumRequiredTimeframesPassed()) {
            reasons.add(REASON_MINIMUM_TIMEFRAMES_NOT_PASSED);
        }
        if (context.getDataQualityPassed() == null) {
            reasons.add(REASON_DATA_QUALITY_STATUS_MISSING);
        } else if (!context.getDataQualityPassed()) {
            reasons.add(REASON_DATA_QUALITY_NOT_PASSED);
        }
        if (context.getHardThresholdPassed() == null) {
            reasons.add(REASON_HARD_THRESHOLD_STATUS_MISSING);
        }
        if (context.getWarningThresholdPassed() == null) {
            reasons.add(REASON_WARNING_THRESHOLD_STATUS_MISSING);
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

    private static boolean hasDegradedExplanation(MultiTimeframeContextSourceBindingDTO context) {
        return !context.getDegradedReasons().isEmpty() || !isBlank(context.getMissingReason());
    }

    private static boolean hasBlockedExplanation(MultiTimeframeContextSourceBindingDTO context) {
        return !context.getBlockedReasons().isEmpty() || !isBlank(context.getBlockedReason());
    }

    private static boolean safetyFlagsRequiredTrue(MultiTimeframeContextSourceBindingDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(MultiTimeframeContextSourceBindingDTO context) {
        List<String> outputs = new ArrayList<>();
        if (context.getBindingStatus() != null) {
            outputs.add(context.getBindingStatus().name());
        }
        outputs.add(context.getRuntimeKlineContextRef());
        outputs.add(context.getDataQualityContextRef());
        outputs.add(context.getDominantDirection());
        outputs.add(context.getMissingReason());
        outputs.add(context.getBlockedReason());
        outputs.addAll(context.getSourceTraceRefs());
        outputs.addAll(context.getTimeframeRefs());
        outputs.addAll(context.getTimeframeDirections());
        outputs.addAll(context.getAlignedTimeframes());
        outputs.addAll(context.getConflictedTimeframes());
        outputs.addAll(context.getMissingTimeframes());
        outputs.addAll(context.getStaleTimeframes());
        outputs.addAll(context.getMissingFields());
        outputs.addAll(context.getDegradedReasons());
        outputs.addAll(context.getBlockedReasons());

        for (String output : outputs) {
            String normalizedOutput = output == null ? "" : output.toLowerCase().replace("_", "");
            for (String forbiddenSemantic : FORBIDDEN_EXECUTABLE_SEMANTICS) {
                if (normalizedOutput.contains(forbiddenSemantic.replace(" ", ""))) {
                    return true;
                }
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
        REVIEW_ONLY_MULTITIMEFRAME_BINDING,
        REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
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

        private ValidationResult(
                ValidationStatus status,
                boolean validForReviewOnly,
                boolean incomplete,
                boolean blockedFailClosed,
                List<String> reasons
        ) {
            this.status = status;
            this.validForReviewOnly = validForReviewOnly;
            this.incomplete = incomplete;
            this.blockedFailClosed = blockedFailClosed;
            this.manualReviewRequired = true;
            this.notTradeInstruction = true;
            this.reviewOnly = true;
            this.reasons = copy(reasons);
        }

        public static ValidationResult incomplete(List<String> reasons) {
            return new ValidationResult(ValidationStatus.INCOMPLETE, false, true, false, reasons);
        }

        public static ValidationResult blockedFailClosed(List<String> reasons) {
            return new ValidationResult(ValidationStatus.BLOCKED_FAIL_CLOSED, false, false, true, reasons);
        }

        public static ValidationResult reviewOnlyMultiTimeframeBinding(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING,
                    true,
                    false,
                    false,
                    reasons
            );
        }

        public static ValidationResult reviewOnlyMultiTimeframeBindingDegraded(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED,
                    true,
                    false,
                    false,
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
            return copy(reasons);
        }

        private static List<String> copy(List<String> values) {
            return values == null ? Collections.emptyList() : new ArrayList<>(values);
        }
    }
}
