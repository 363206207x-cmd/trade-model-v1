package org.example.trademodel.validator.point;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.SourceTraceNumericSourceContextDTO;

public class SourceTraceNumericSourceReadModelValidator {

    private static final String REASON_CONTEXT_MISSING = "SOURCE_TRACE_CONTEXT_MISSING";
    private static final String REASON_STATUS_MISSING = "SOURCE_TRACE_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_FORBIDDEN_SOURCE_TYPE = "FORBIDDEN_SOURCE_TYPE";
    private static final String REASON_UNTRUSTED_SOURCE = "SOURCE_UNTRUSTED";
    private static final String REASON_SOURCE_TRACE_ID_MISSING = "SOURCE_TRACE_ID_MISSING";
    private static final String REASON_SOURCE_OWNER_MISSING = "SOURCE_OWNER_MISSING";
    private static final String REASON_SOURCE_TYPE_MISSING = "SOURCE_TYPE_MISSING";
    private static final String REASON_SOURCE_CONTRACT_ID_MISSING = "SOURCE_CONTRACT_ID_MISSING";
    private static final String REASON_SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String REASON_MARKET_MISSING = "MARKET_MISSING";
    private static final String REASON_TIMEFRAME_MISSING = "TIMEFRAME_MISSING";
    private static final String REASON_NUMERIC_FIELD_NAME_MISSING = "NUMERIC_FIELD_NAME_MISSING";
    private static final String REASON_NUMERIC_FIELD_ROLE_MISSING = "NUMERIC_FIELD_ROLE_MISSING";
    private static final String REASON_SOURCE_REF_MISSING = "SOURCE_REF_MISSING";
    private static final String REASON_OBSERVED_AT_MISSING = "OBSERVED_AT_MISSING";
    private static final String REASON_FRESHNESS_STALE = "FRESHNESS_STALE";
    private static final String REASON_FRESHNESS_UNKNOWN = "FRESHNESS_UNKNOWN";
    private static final String REASON_FRESHNESS_MISSING = "FRESHNESS_MISSING";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING =
            "RUNTIME_KLINE_CONTEXT_REF_MISSING";
    private static final String REASON_DATA_QUALITY_CONTEXT_REF_MISSING =
            "DATA_QUALITY_CONTEXT_REF_MISSING";
    private static final String REASON_MULTI_TIMEFRAME_CONTEXT_REF_MISSING =
            "MULTI_TIMEFRAME_CONTEXT_REF_MISSING";
    private static final String REASON_RISK_ACTION_GUARD_REF_MISSING =
            "RISK_ACTION_GUARD_REF_MISSING";
    private static final String REASON_NUMERIC_VALUE_MISSING = "NUMERIC_VALUE_MISSING";

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

    public ValidationResult validate(SourceTraceNumericSourceContextDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        if (containsForbiddenExecutableSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        SourceTraceNumericSourceContextDTO.SourceTraceStatus status = context.getSourceTraceStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        if (isForbiddenSourceType(context.getSourceType())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SOURCE_TYPE));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_SOURCE_TRACE -> validateReviewOnlySourceTrace(context);
            case REVIEW_ONLY_SOURCE_TRACE_DEGRADED -> validateDegradedSourceTrace(context);
        };
    }

    private static ValidationResult validateIncomplete(SourceTraceNumericSourceContextDTO context) {
        if (isBlank(context.getMissingReason())) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(List.of(context.getMissingReason()));
    }

    private static ValidationResult validateBlockedFailClosed(SourceTraceNumericSourceContextDTO context) {
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

    private static ValidationResult validateReviewOnlySourceTrace(SourceTraceNumericSourceContextDTO context) {
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        List<String> reasons = requiredSourceTraceReasons(context);
        reasons.addAll(numericValueReasons(context));
        if (!reasons.isEmpty()) {
            return ValidationResult.incomplete(reasons);
        }
        return ValidationResult.reviewOnlySourceTrace(List.of());
    }

    private static ValidationResult validateDegradedSourceTrace(SourceTraceNumericSourceContextDTO context) {
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        List<String> reasons = requiredSourceTraceReasons(context);
        if (isBlank(context.getMissingReason())) {
            reasons.add(REASON_MISSING_REASON_REQUIRED);
        }
        if (!reasons.isEmpty()) {
            return ValidationResult.incomplete(reasons);
        }
        return ValidationResult.reviewOnlySourceTraceDegraded(List.of(context.getMissingReason()));
    }

    private static List<String> requiredSourceTraceReasons(SourceTraceNumericSourceContextDTO context) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getSourceTraceId())) {
            reasons.add(REASON_SOURCE_TRACE_ID_MISSING);
        }
        if (isBlank(context.getSourceOwner())) {
            reasons.add(REASON_SOURCE_OWNER_MISSING);
        }
        if (context.getSourceType() == null) {
            reasons.add(REASON_SOURCE_TYPE_MISSING);
        }
        if (isBlank(context.getSourceContractId())) {
            reasons.add(REASON_SOURCE_CONTRACT_ID_MISSING);
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
        if (isBlank(context.getNumericFieldName())) {
            reasons.add(REASON_NUMERIC_FIELD_NAME_MISSING);
        }
        if (context.getNumericFieldRole() == null) {
            reasons.add(REASON_NUMERIC_FIELD_ROLE_MISSING);
        }
        if (isBlank(context.getSourceRef())) {
            reasons.add(REASON_SOURCE_REF_MISSING);
        }
        if (isBlank(context.getObservedAt())) {
            reasons.add(REASON_OBSERVED_AT_MISSING);
        }
        reasons.addAll(freshnessReasons(context.getFreshnessStatus()));
        if (isBlank(context.getRuntimeKlineContextRef())) {
            reasons.add(REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getDataQualityContextRef())) {
            reasons.add(REASON_DATA_QUALITY_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getMultiTimeframeContextRef())) {
            reasons.add(REASON_MULTI_TIMEFRAME_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getRiskActionGuardRef())) {
            reasons.add(REASON_RISK_ACTION_GUARD_REF_MISSING);
        }
        return reasons;
    }

    private static List<String> freshnessReasons(SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus) {
        if (freshnessStatus == null) {
            return List.of(REASON_FRESHNESS_MISSING);
        }
        return switch (freshnessStatus) {
            case FRESH -> List.of();
            case STALE -> List.of(REASON_FRESHNESS_STALE);
            case UNKNOWN -> List.of(REASON_FRESHNESS_UNKNOWN);
        };
    }

    private static List<String> numericValueReasons(SourceTraceNumericSourceContextDTO context) {
        SourceTraceNumericSourceContextDTO.NumericFieldRole role = context.getNumericFieldRole();
        if (role == null || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.SOURCE_ONLY_REFERENCE) {
            return List.of();
        }
        if (isPointValueRole(role) && context.getNumericValue() == null) {
            return List.of(REASON_NUMERIC_VALUE_MISSING);
        }
        if (isZoneRole(role)
                && context.getNumericValue() == null
                && context.getNumericValueLow() == null
                && context.getNumericValueHigh() == null) {
            return List.of(REASON_NUMERIC_VALUE_MISSING);
        }
        return List.of();
    }

    private static boolean isPointValueRole(SourceTraceNumericSourceContextDTO.NumericFieldRole role) {
        return role == SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.STOP_PRICE
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.TAKE_PROFIT_PRICE
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.RISK_REWARD_VALUE;
    }

    private static boolean isZoneRole(SourceTraceNumericSourceContextDTO.NumericFieldRole role) {
        return role == SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_ZONE_LOW
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_ZONE_HIGH
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.STOP_ZONE_LOW
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.STOP_ZONE_HIGH
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.TAKE_PROFIT_ZONE_LOW
                || role == SourceTraceNumericSourceContextDTO.NumericFieldRole.TAKE_PROFIT_ZONE_HIGH;
    }

    private static boolean isForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType sourceType) {
        return sourceType != null && !sourceType.isSourceOwnedEvidence();
    }

    private static boolean safetyFlagsRequiredTrue(SourceTraceNumericSourceContextDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(SourceTraceNumericSourceContextDTO context) {
        List<String> outputs = new ArrayList<>();
        if (context.getSourceTraceStatus() != null) {
            outputs.add(context.getSourceTraceStatus().name());
        }
        outputs.add(context.getSourceOwner());
        outputs.add(context.getSourceContractId());
        outputs.add(context.getNumericFieldName());
        outputs.add(context.getSourceRef());
        outputs.add(context.getMissingReason());
        outputs.add(context.getBlockedReason());

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
        REVIEW_ONLY_SOURCE_TRACE,
        REVIEW_ONLY_SOURCE_TRACE_DEGRADED
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

        public static ValidationResult reviewOnlySourceTrace(List<String> reasons) {
            return new ValidationResult(ValidationStatus.REVIEW_ONLY_SOURCE_TRACE, true, false, false, reasons);
        }

        public static ValidationResult reviewOnlySourceTraceDegraded(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED,
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
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        }
    }
}
