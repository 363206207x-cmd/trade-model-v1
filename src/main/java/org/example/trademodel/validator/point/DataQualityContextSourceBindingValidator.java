package org.example.trademodel.validator.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.DataQualityContextSourceBindingDTO;
import org.example.trademodel.service.support.DataQualityCircuitBreakerPolicy;

public class DataQualityContextSourceBindingValidator {

    private static final BigDecimal MIN_REVIEW_ONLY_SCORE = new BigDecimal("85");

    private static final String REASON_CONTEXT_MISSING = "DATA_QUALITY_CONTEXT_BINDING_MISSING";
    private static final String REASON_STATUS_MISSING = "DATA_QUALITY_BINDING_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_UNTRUSTED_SOURCE = "DATA_QUALITY_SOURCE_UNTRUSTED";
    private static final String REASON_HARD_THRESHOLD_BLOCKED = "HARD_THRESHOLD_BLOCKED";
    private static final String REASON_DATA_QUALITY_CONTEXT_ID_MISSING = "DATA_QUALITY_CONTEXT_ID_MISSING";
    private static final String REASON_SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String REASON_MARKET_MISSING = "MARKET_MISSING";
    private static final String REASON_TIMEFRAME_MISSING = "TIMEFRAME_MISSING";
    private static final String REASON_SOURCE_TRACE_REFS_MISSING = "SOURCE_TRACE_REFS_MISSING";
    private static final String REASON_SOURCE_TRACE_REF_BLANK = "SOURCE_TRACE_REF_BLANK";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING =
            "RUNTIME_KLINE_CONTEXT_REF_MISSING";
    private static final String REASON_DATA_QUALITY_SCORE_MISSING = "DATA_QUALITY_SCORE_MISSING";
    private static final String REASON_DATA_QUALITY_SCORE_LOW = "DATA_QUALITY_SCORE_LOW";
    private static final String REASON_DATA_QUALITY_SCORE_DEGRADED = "DATA_QUALITY_SCORE_DEGRADED";
    private static final String REASON_DATA_QUALITY_GRADE_MISSING = "DATA_QUALITY_GRADE_MISSING";
    private static final String REASON_DATA_QUALITY_GRADE_UNKNOWN = "DATA_QUALITY_GRADE_UNKNOWN";
    private static final String REASON_HARD_THRESHOLD_STATUS_MISSING =
            "HARD_THRESHOLD_STATUS_MISSING";
    private static final String REASON_WARNING_THRESHOLD_STATUS_MISSING =
            "WARNING_THRESHOLD_STATUS_MISSING";
    private static final String REASON_WARNING_THRESHOLD_DEGRADED =
            "WARNING_THRESHOLD_DEGRADED";
    private static final String REASON_SOURCE_TRACE_COMPLETENESS_MISSING =
            "SOURCE_TRACE_COMPLETENESS_MISSING";
    private static final String REASON_RUNTIME_KLINE_COMPLETENESS_MISSING =
            "RUNTIME_KLINE_COMPLETENESS_MISSING";
    private static final String REASON_OHLCV_COMPLETENESS_MISSING =
            "OHLCV_COMPLETENESS_MISSING";
    private static final String REASON_FRESHNESS_SCORE_MISSING = "FRESHNESS_SCORE_MISSING";
    private static final String REASON_MULTITIMEFRAME_CONSISTENCY_MISSING =
            "MULTITIMEFRAME_CONSISTENCY_MISSING";
    private static final String REASON_COMPLETENESS_SCORE_LOW = "COMPLETENESS_SCORE_LOW";
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
            "push opportunity"
    );

    public ValidationResult validate(DataQualityContextSourceBindingDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        if (containsForbiddenExecutableSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        DataQualityContextSourceBindingDTO.BindingStatus status = context.getBindingStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_DATA_QUALITY_BINDING -> validateReviewOnlyBinding(context);
            case REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED -> validateDegradedBinding(context);
        };
    }

    private static ValidationResult validateIncomplete(DataQualityContextSourceBindingDTO context) {
        if (isBlank(context.getMissingReason())) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(List.of(context.getMissingReason()));
    }

    private static ValidationResult validateBlockedFailClosed(DataQualityContextSourceBindingDTO context) {
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

    private static ValidationResult validateReviewOnlyBinding(DataQualityContextSourceBindingDTO context) {
        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }
        List<String> incompleteReasons = requiredBindingReasons(context);
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        ValidationResult thresholdResult = thresholdResult(context);
        if (thresholdResult != null) {
            return thresholdResult;
        }
        return ValidationResult.reviewOnlyDataQualityBinding(List.of());
    }

    private static ValidationResult validateDegradedBinding(DataQualityContextSourceBindingDTO context) {
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

        ValidationResult thresholdResult = thresholdResult(context);
        if (thresholdResult != null) {
            return thresholdResult;
        }
        return ValidationResult.reviewOnlyDataQualityBindingDegraded(List.of(context.getMissingReason()));
    }

    private static ValidationResult blockedState(DataQualityContextSourceBindingDTO context) {
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        if (Boolean.FALSE.equals(context.getHardThresholdPassed())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_HARD_THRESHOLD_BLOCKED));
        }
        return null;
    }

    private static ValidationResult thresholdResult(DataQualityContextSourceBindingDTO context) {
        if (!DataQualityCircuitBreakerPolicy.passes(context.getDataQualityScore())) {
            return ValidationResult.incomplete(List.of(REASON_DATA_QUALITY_SCORE_LOW));
        }
        if (context.getDataQualityScore().compareTo(MIN_REVIEW_ONLY_SCORE) < 0) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyDataQualityBindingDegraded(List.of(REASON_DATA_QUALITY_SCORE_DEGRADED));
            }
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        if (Boolean.FALSE.equals(context.getWarningThresholdPassed())) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyDataQualityBindingDegraded(List.of(REASON_WARNING_THRESHOLD_DEGRADED));
            }
            return ValidationResult.incomplete(List.of(REASON_WARNING_THRESHOLD_DEGRADED));
        }
        return null;
    }

    private static List<String> requiredBindingReasons(DataQualityContextSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getDataQualityContextId())) {
            reasons.add(REASON_DATA_QUALITY_CONTEXT_ID_MISSING);
        }
        if (isBlank(context.getSymbol())) {
            reasons.add(REASON_SYMBOL_MISSING);
        }
        if (isBlank(context.getMarket())) {
            reasons.add(REASON_MARKET_MISSING);
        }
        if (isBlank(context.getTimeframe())) {
            reasons.add(REASON_TIMEFRAME_MISSING);
        }
        reasons.addAll(sourceTraceRefReasons(context.getSourceTraceRefs()));
        if (isBlank(context.getRuntimeKlineContextRef())) {
            reasons.add(REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING);
        }
        if (context.getDataQualityScore() == null) {
            reasons.add(REASON_DATA_QUALITY_SCORE_MISSING);
        }
        if (context.getDataQualityGrade() == null) {
            reasons.add(REASON_DATA_QUALITY_GRADE_MISSING);
        } else if (context.getDataQualityGrade() == DataQualityContextSourceBindingDTO.DataQualityGrade.UNKNOWN) {
            reasons.add(REASON_DATA_QUALITY_GRADE_UNKNOWN);
        }
        if (context.getHardThresholdPassed() == null) {
            reasons.add(REASON_HARD_THRESHOLD_STATUS_MISSING);
        }
        if (context.getWarningThresholdPassed() == null) {
            reasons.add(REASON_WARNING_THRESHOLD_STATUS_MISSING);
        }
        reasons.addAll(completenessScoreReasons(context));
        if (isBlank(context.getObservedAt())) {
            reasons.add(REASON_OBSERVED_AT_MISSING);
        }
        return reasons;
    }

    private static List<String> completenessScoreReasons(DataQualityContextSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        addCompletenessReason(context.getSourceTraceCompletenessScore(),
                REASON_SOURCE_TRACE_COMPLETENESS_MISSING, reasons);
        addCompletenessReason(context.getRuntimeKlineCompletenessScore(),
                REASON_RUNTIME_KLINE_COMPLETENESS_MISSING, reasons);
        addCompletenessReason(context.getOhlcvCompletenessScore(), REASON_OHLCV_COMPLETENESS_MISSING, reasons);
        addCompletenessReason(context.getFreshnessScore(), REASON_FRESHNESS_SCORE_MISSING, reasons);
        addCompletenessReason(context.getMultiTimeframeConsistencyScore(),
                REASON_MULTITIMEFRAME_CONSISTENCY_MISSING, reasons);
        return reasons;
    }

    private static void addCompletenessReason(BigDecimal score, String missingReason, List<String> reasons) {
        if (score == null) {
            reasons.add(missingReason);
        } else if (score.compareTo(DataQualityCircuitBreakerPolicy.MIN_PASS_SCORE_DECIMAL) < 0) {
            reasons.add(REASON_COMPLETENESS_SCORE_LOW);
        }
    }

    private static List<String> sourceTraceRefReasons(List<String> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of(REASON_SOURCE_TRACE_REFS_MISSING);
        }
        for (String ref : refs) {
            if (isBlank(ref)) {
                return List.of(REASON_SOURCE_TRACE_REF_BLANK);
            }
        }
        return List.of();
    }

    private static boolean hasDegradedExplanation(DataQualityContextSourceBindingDTO context) {
        return !context.getDegradedReasons().isEmpty() || !isBlank(context.getMissingReason());
    }

    private static boolean safetyFlagsRequiredTrue(DataQualityContextSourceBindingDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(DataQualityContextSourceBindingDTO context) {
        List<String> outputs = new ArrayList<>();
        if (context.getBindingStatus() != null) {
            outputs.add(context.getBindingStatus().name());
        }
        outputs.add(context.getRuntimeKlineContextRef());
        outputs.add(context.getMissingReason());
        outputs.add(context.getBlockedReason());
        outputs.addAll(context.getSourceTraceRefs());
        outputs.addAll(context.getMissingFields());
        outputs.addAll(context.getDegradedReasons());
        outputs.addAll(context.getBlockedReasons());

        for (String output : outputs) {
            String normalizedOutput = output == null ? "" : output.toLowerCase();
            for (String forbiddenSemantic : FORBIDDEN_EXECUTABLE_SEMANTICS) {
                if (normalizedOutput.contains(forbiddenSemantic)) {
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
        REVIEW_ONLY_DATA_QUALITY_BINDING,
        REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
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

        public static ValidationResult reviewOnlyDataQualityBinding(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_DATA_QUALITY_BINDING,
                    true,
                    false,
                    false,
                    reasons
            );
        }

        public static ValidationResult reviewOnlyDataQualityBindingDegraded(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED,
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
