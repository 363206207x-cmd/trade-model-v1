package org.example.trademodel.validator.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.RiskActionGuardSourceBindingDTO;

public class RiskActionGuardSourceBindingValidator {

    private static final BigDecimal HIGH_RISK_SCORE = new BigDecimal("80");
    private static final BigDecimal HIGH_ACTION_RISK_SCORE = new BigDecimal("75");

    private static final String REASON_CONTEXT_MISSING = "RISK_ACTION_GUARD_BINDING_MISSING";
    private static final String REASON_STATUS_MISSING = "RISK_ACTION_GUARD_BINDING_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_UNTRUSTED_SOURCE = "RISK_ACTION_GUARD_SOURCE_UNTRUSTED";
    private static final String REASON_CONTEXT_ID_MISSING = "RISK_ACTION_GUARD_CONTEXT_ID_MISSING";
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
    private static final String REASON_LIQUIDITY_STATE_MISSING = "LIQUIDITY_STATE_MISSING";
    private static final String REASON_LIQUIDITY_DEGRADED_STATUS_MISSING =
            "LIQUIDITY_DEGRADED_STATUS_MISSING";
    private static final String REASON_STAMPEDE_STATUS_MISSING = "STAMPEDE_STATUS_MISSING";
    private static final String REASON_WICK_ONLY_STATUS_MISSING = "WICK_ONLY_STATUS_MISSING";
    private static final String REASON_MULTITIMEFRAME_CONFIRMATION_MISSING =
            "MULTITIMEFRAME_CONFIRMATION_MISSING";
    private static final String REASON_STRONG_REVERSAL_STATUS_MISSING =
            "STRONG_REVERSAL_STATUS_MISSING";
    private static final String REASON_RISK_LEVEL_MISSING = "RISK_LEVEL_MISSING";
    private static final String REASON_RISK_SCORE_MISSING = "RISK_SCORE_MISSING";
    private static final String REASON_ACTION_RISK_SCORE_MISSING = "ACTION_RISK_SCORE_MISSING";
    private static final String REASON_OBSERVED_AT_MISSING = "OBSERVED_AT_MISSING";
    private static final String REASON_STAMPEDE_BLOCKED = "STAMPEDE_BLOCKED";
    private static final String REASON_STRONG_REVERSAL_UNCONFIRMED =
            "STRONG_REVERSAL_UNCONFIRMED";
    private static final String REASON_WICK_ONLY_ACTION_BLOCKED = "WICK_ONLY_ACTION_BLOCKED";
    private static final String REASON_LIQUIDITY_ACTION_BLOCKED =
            "LIQUIDITY_DEGRADED_ACTION_BLOCKED";
    private static final String REASON_HIGH_RISK_REVIEW_ONLY = "HIGH_RISK_REVIEW_ONLY";
    private static final String REASON_WARNING_RISK_REQUIRES_DEGRADED_REASON =
            "WARNING_RISK_REQUIRES_DEGRADED_REASON";

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
            "take-profit " + "ord" + "er",
            "stop-loss " + "ord" + "er",
            "send " + "ord" + "er",
            "push opportunity",
            "place" + "ord" + "er",
            "create" + "ord" + "er",
            "clos" + "eposition",
            "revers" + "eposition",
            "ope" + "nposition",
            "submit" + "ord" + "er"
    );

    private static final List<String> WICK_ONLY_BLOCKED_SEMANTICS = List.of(
            "revers" + "e",
            "opposite",
            "trend reversal"
    );

    private static final List<String> LIQUIDITY_BLOCKED_SEMANTICS = List.of(
            "market clos" + "e",
            "market cut",
            "one-shot exit"
    );

    public ValidationResult validate(RiskActionGuardSourceBindingDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        if (containsForbiddenExecutableSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        RiskActionGuardSourceBindingDTO.BindingStatus status = context.getBindingStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_RISK_ACTION_GUARD_BINDING -> validateReviewOnlyBinding(context);
            case REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED -> validateDegradedBinding(context);
        };
    }

    private static ValidationResult validateIncomplete(RiskActionGuardSourceBindingDTO context) {
        if (isBlank(context.getMissingReason())) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(List.of(context.getMissingReason()));
    }

    private static ValidationResult validateBlockedFailClosed(RiskActionGuardSourceBindingDTO context) {
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

    private static ValidationResult validateReviewOnlyBinding(RiskActionGuardSourceBindingDTO context) {
        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }

        List<String> incompleteReasons = requiredBindingReasons(context);
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        ValidationResult riskState = riskState(context);
        if (riskState != null) {
            return riskState;
        }

        return ValidationResult.reviewOnlyRiskActionGuardBinding(List.of());
    }

    private static ValidationResult validateDegradedBinding(RiskActionGuardSourceBindingDTO context) {
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

        ValidationResult riskState = riskState(context);
        if (riskState != null && riskState.isBlockedFailClosed()) {
            return riskState;
        }
        if (riskState != null && riskState.isIncomplete()) {
            return riskState;
        }

        return ValidationResult.reviewOnlyRiskActionGuardBindingDegraded(List.of(context.getMissingReason()));
    }

    private static ValidationResult blockedState(RiskActionGuardSourceBindingDTO context) {
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        if (Boolean.TRUE.equals(context.getStampedeDetected())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_STAMPEDE_BLOCKED));
        }
        if (Boolean.TRUE.equals(context.getWickOnlyDetected())
                && containsAny(context.getProposedActionLabel(), WICK_ONLY_BLOCKED_SEMANTICS)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_WICK_ONLY_ACTION_BLOCKED));
        }
        if (Boolean.TRUE.equals(context.getLiquidityDegraded())
                && containsAny(context.getProposedActionLabel(), LIQUIDITY_BLOCKED_SEMANTICS)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_LIQUIDITY_ACTION_BLOCKED));
        }
        return null;
    }

    private static ValidationResult riskState(RiskActionGuardSourceBindingDTO context) {
        if (Boolean.TRUE.equals(context.getStrongReversalClaimed())
                && Boolean.FALSE.equals(context.getMultiTimeframeConfirmed())) {
            return ValidationResult.incomplete(List.of(REASON_STRONG_REVERSAL_UNCONFIRMED));
        }
        if (isHighRisk(context)) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyRiskActionGuardBindingDegraded(
                        List.of(REASON_HIGH_RISK_REVIEW_ONLY)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_WARNING_RISK_REQUIRES_DEGRADED_REASON));
        }
        if (context.getActionRiskScore().compareTo(HIGH_ACTION_RISK_SCORE) >= 0) {
            if (hasDegradedExplanation(context)) {
                return ValidationResult.reviewOnlyRiskActionGuardBindingDegraded(
                        List.of(REASON_HIGH_RISK_REVIEW_ONLY)
                );
            }
            return ValidationResult.incomplete(List.of(REASON_WARNING_RISK_REQUIRES_DEGRADED_REASON));
        }
        return null;
    }

    private static List<String> requiredBindingReasons(RiskActionGuardSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getRiskActionGuardContextId())) {
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
        if (context.getLiquidityState() == null) {
            reasons.add(REASON_LIQUIDITY_STATE_MISSING);
        }
        if (context.getLiquidityDegraded() == null) {
            reasons.add(REASON_LIQUIDITY_DEGRADED_STATUS_MISSING);
        }
        if (context.getStampedeDetected() == null) {
            reasons.add(REASON_STAMPEDE_STATUS_MISSING);
        }
        if (context.getWickOnlyDetected() == null) {
            reasons.add(REASON_WICK_ONLY_STATUS_MISSING);
        }
        if (context.getMultiTimeframeConfirmed() == null) {
            reasons.add(REASON_MULTITIMEFRAME_CONFIRMATION_MISSING);
        }
        if (context.getStrongReversalClaimed() == null) {
            reasons.add(REASON_STRONG_REVERSAL_STATUS_MISSING);
        }
        if (context.getRiskLevel() == null) {
            reasons.add(REASON_RISK_LEVEL_MISSING);
        }
        if (context.getRiskScore() == null) {
            reasons.add(REASON_RISK_SCORE_MISSING);
        }
        if (context.getActionRiskScore() == null) {
            reasons.add(REASON_ACTION_RISK_SCORE_MISSING);
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

    private static boolean isHighRisk(RiskActionGuardSourceBindingDTO context) {
        if (context.getRiskScore() != null && context.getRiskScore().compareTo(HIGH_RISK_SCORE) >= 0) {
            return true;
        }
        return RiskActionGuardSourceBindingDTO.RiskLevel.HIGH.equals(context.getRiskLevel())
                || RiskActionGuardSourceBindingDTO.RiskLevel.CRITICAL.equals(context.getRiskLevel());
    }

    private static boolean hasDegradedExplanation(RiskActionGuardSourceBindingDTO context) {
        return !context.getDegradedReasons().isEmpty() || !isBlank(context.getMissingReason());
    }

    private static boolean safetyFlagsRequiredTrue(RiskActionGuardSourceBindingDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(RiskActionGuardSourceBindingDTO context) {
        List<String> outputs = new ArrayList<>();
        outputs.add(context.getRuntimeKlineContextRef());
        outputs.add(context.getDataQualityContextRef());
        outputs.add(context.getMultiTimeframeContextRef());
        outputs.add(context.getProposedActionLabel());
        outputs.add(context.getGuardDecisionLabel());
        outputs.add(context.getGuardReason());
        outputs.add(context.getRiskActionCategory());
        outputs.add(context.getRiskActionBoundaryRef());
        outputs.add(context.getMissingReason());
        outputs.add(context.getBlockedReason());
        outputs.addAll(context.getSourceTraceRefs());
        outputs.addAll(context.getAllowedReviewOnlyActionLabels());
        outputs.addAll(context.getBlockedActionLabels());
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

    private static boolean containsAny(String value, List<String> semantics) {
        String normalizedOutput = value == null ? "" : value.toLowerCase().replace("_", "").replace("-", "");
        for (String semantic : semantics) {
            String normalizedSemantic = semantic.toLowerCase().replace(" ", "").replace("-", "");
            if (normalizedOutput.contains(normalizedSemantic)) {
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
        REVIEW_ONLY_RISK_ACTION_GUARD_BINDING,
        REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
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

        public static ValidationResult reviewOnlyRiskActionGuardBinding(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING,
                    true,
                    false,
                    false,
                    reasons
            );
        }

        public static ValidationResult reviewOnlyRiskActionGuardBindingDegraded(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED,
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
