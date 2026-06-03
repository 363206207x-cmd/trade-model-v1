package org.example.trademodel.validator.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.RuntimeKlineContextSourceBindingDTO;

public class RuntimeKlineContextSourceBindingValidator {

    private static final BigDecimal MIN_OHLCV_COMPLETENESS = new BigDecimal("70");

    private static final String REASON_CONTEXT_MISSING = "RUNTIME_KLINE_CONTEXT_BINDING_MISSING";
    private static final String REASON_STATUS_MISSING = "RUNTIME_KLINE_BINDING_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_UNTRUSTED_SOURCE = "RUNTIME_KLINE_SOURCE_UNTRUSTED";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_ID_MISSING =
            "RUNTIME_KLINE_CONTEXT_ID_MISSING";
    private static final String REASON_SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String REASON_MARKET_MISSING = "MARKET_MISSING";
    private static final String REASON_TIMEFRAME_MISSING = "TIMEFRAME_MISSING";
    private static final String REASON_KLINE_WINDOW_MISSING = "KLINE_WINDOW_MISSING";
    private static final String REASON_LATEST_PRICE_MISSING = "LATEST_PRICE_MISSING";
    private static final String REASON_LATEST_CLOSE_MISSING = "LATEST_CLOSE_MISSING";
    private static final String REASON_OPEN_MISSING = "OPEN_MISSING";
    private static final String REASON_HIGH_MISSING = "HIGH_MISSING";
    private static final String REASON_LOW_MISSING = "LOW_MISSING";
    private static final String REASON_CLOSE_MISSING = "CLOSE_MISSING";
    private static final String REASON_VOLUME_MISSING = "VOLUME_MISSING";
    private static final String REASON_CANDLE_CLOSED_MISSING = "CANDLE_CLOSED_MISSING";
    private static final String REASON_CANDLE_NOT_CLOSED = "CANDLE_NOT_CLOSED";
    private static final String REASON_OHLCV_COMPLETENESS_MISSING =
            "OHLCV_COMPLETENESS_MISSING";
    private static final String REASON_OHLCV_COMPLETENESS_LOW = "OHLCV_COMPLETENESS_LOW";
    private static final String REASON_FRESHNESS_STALE = "FRESHNESS_STALE";
    private static final String REASON_FRESHNESS_UNKNOWN = "FRESHNESS_UNKNOWN";
    private static final String REASON_FRESHNESS_MISSING = "FRESHNESS_MISSING";
    private static final String REASON_WICK_ONLY_INCOMPLETE = "WICK_ONLY_INCOMPLETE";
    private static final String REASON_WICK_UNKNOWN = "WICK_UNKNOWN";
    private static final String REASON_WICK_MISSING = "WICK_MISSING";
    private static final String REASON_SEVERE_GAP = "SEVERE_GAP";
    private static final String REASON_GAP_UNKNOWN = "GAP_UNKNOWN";
    private static final String REASON_GAP_MISSING = "GAP_MISSING";
    private static final String REASON_LIQUIDITY_SEVERELY_DEGRADED =
            "LIQUIDITY_SEVERELY_DEGRADED";
    private static final String REASON_LIQUIDITY_SEVERELY_DEGRADED_EXECUTABLE_ATTEMPT =
            "LIQUIDITY_SEVERELY_DEGRADED_EXECUTABLE_ATTEMPT";
    private static final String REASON_LIQUIDITY_UNKNOWN = "LIQUIDITY_UNKNOWN";
    private static final String REASON_LIQUIDITY_MISSING = "LIQUIDITY_MISSING";
    private static final String REASON_LIQUIDITY_SEVERITY_MISSING =
            "LIQUIDITY_SEVERITY_MISSING";
    private static final String REASON_STAMPEDE_CONFIRMED = "STAMPEDE_CONFIRMED";
    private static final String REASON_STAMPEDE_SUSPECTED = "STAMPEDE_SUSPECTED";
    private static final String REASON_STAMPEDE_UNKNOWN = "STAMPEDE_UNKNOWN";
    private static final String REASON_STAMPEDE_MISSING = "STAMPEDE_MISSING";
    private static final String REASON_SOURCE_TRACE_REFS_MISSING =
            "SOURCE_TRACE_REFS_MISSING";
    private static final String REASON_SOURCE_TRACE_REF_BLANK = "SOURCE_TRACE_REF_BLANK";
    private static final String REASON_MARKET_DATA_SOURCE_REF_MISSING =
            "MARKET_DATA_SOURCE_REF_MISSING";
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

    public ValidationResult validate(RuntimeKlineContextSourceBindingDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        if (containsForbiddenExecutableSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        RuntimeKlineContextSourceBindingDTO.BindingStatus status = context.getBindingStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_RUNTIME_KLINE_BINDING -> validateReviewOnlyBinding(context);
            case REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED -> validateDegradedBinding(context);
        };
    }

    private static ValidationResult validateIncomplete(RuntimeKlineContextSourceBindingDTO context) {
        if (isBlank(context.getMissingReason())) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(List.of(context.getMissingReason()));
    }

    private static ValidationResult validateBlockedFailClosed(RuntimeKlineContextSourceBindingDTO context) {
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

    private static ValidationResult validateReviewOnlyBinding(RuntimeKlineContextSourceBindingDTO context) {
        ValidationResult blockedState = blockedRuntimeState(context);
        if (blockedState != null) {
            return blockedState;
        }
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        List<String> reasons = requiredBindingReasons(context);
        reasons.addAll(incompleteRuntimeStateReasons(context));
        if (!reasons.isEmpty()) {
            return ValidationResult.incomplete(reasons);
        }
        return ValidationResult.reviewOnlyRuntimeKlineBinding(List.of());
    }

    private static ValidationResult validateDegradedBinding(RuntimeKlineContextSourceBindingDTO context) {
        ValidationResult blockedState = blockedRuntimeState(context);
        if (blockedState != null) {
            return blockedState;
        }
        if (!context.isTrustedSource()) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UNTRUSTED_SOURCE));
        }
        List<String> reasons = requiredBindingReasons(context);
        reasons.addAll(incompleteRuntimeStateReasons(context));
        if (isBlank(context.getMissingReason())) {
            reasons.add(REASON_MISSING_REASON_REQUIRED);
        }
        if (!reasons.isEmpty()) {
            return ValidationResult.incomplete(reasons);
        }
        return ValidationResult.reviewOnlyRuntimeKlineBindingDegraded(List.of(context.getMissingReason()));
    }

    private static List<String> requiredBindingReasons(RuntimeKlineContextSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getRuntimeKlineContextId())) {
            reasons.add(REASON_RUNTIME_KLINE_CONTEXT_ID_MISSING);
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
        if (isBlank(context.getKlineWindow())) {
            reasons.add(REASON_KLINE_WINDOW_MISSING);
        }
        if (context.getLatestPrice() == null) {
            reasons.add(REASON_LATEST_PRICE_MISSING);
        }
        if (context.getLatestClose() == null) {
            reasons.add(REASON_LATEST_CLOSE_MISSING);
        }
        if (context.getOpen() == null) {
            reasons.add(REASON_OPEN_MISSING);
        }
        if (context.getHigh() == null) {
            reasons.add(REASON_HIGH_MISSING);
        }
        if (context.getLow() == null) {
            reasons.add(REASON_LOW_MISSING);
        }
        if (context.getClose() == null) {
            reasons.add(REASON_CLOSE_MISSING);
        }
        if (context.getVolume() == null) {
            reasons.add(REASON_VOLUME_MISSING);
        }
        if (context.getCandleClosed() == null) {
            reasons.add(REASON_CANDLE_CLOSED_MISSING);
        } else if (!context.getCandleClosed()) {
            reasons.add(REASON_CANDLE_NOT_CLOSED);
        }
        if (context.getOhlcvCompleteness() == null) {
            reasons.add(REASON_OHLCV_COMPLETENESS_MISSING);
        } else if (context.getOhlcvCompleteness().compareTo(MIN_OHLCV_COMPLETENESS) < 0) {
            reasons.add(REASON_OHLCV_COMPLETENESS_LOW);
        }
        reasons.addAll(sourceTraceRefReasons(context.getSourceTraceRefs()));
        if (isBlank(context.getMarketDataSourceRef())) {
            reasons.add(REASON_MARKET_DATA_SOURCE_REF_MISSING);
        }
        if (isBlank(context.getObservedAt())) {
            reasons.add(REASON_OBSERVED_AT_MISSING);
        }
        if (isBlank(context.getLiquiditySeverity())) {
            reasons.add(REASON_LIQUIDITY_SEVERITY_MISSING);
        }
        return reasons;
    }

    private static List<String> incompleteRuntimeStateReasons(RuntimeKlineContextSourceBindingDTO context) {
        List<String> reasons = new ArrayList<>();
        reasons.addAll(freshnessReasons(context.getFreshnessStatus()));
        reasons.addAll(wickReasons(context.getWickStatus()));
        reasons.addAll(gapIncompleteReasons(context.getGapStatus()));
        reasons.addAll(liquidityReasons(context.getLiquidityState()));
        reasons.addAll(stampedeIncompleteReasons(context.getStampedeState()));
        return reasons;
    }

    private static ValidationResult blockedRuntimeState(RuntimeKlineContextSourceBindingDTO context) {
        if (context.getGapStatus() == RuntimeKlineContextSourceBindingDTO.GapStatus.SEVERE_GAP) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SEVERE_GAP));
        }
        if (context.getStampedeState() == RuntimeKlineContextSourceBindingDTO.StampedeState.CONFIRMED) {
            return ValidationResult.blockedFailClosed(List.of(REASON_STAMPEDE_CONFIRMED));
        }
        if (context.getLiquidityState() == RuntimeKlineContextSourceBindingDTO.LiquidityState.SEVERELY_DEGRADED
                && pointsToExecutableAttempt(context.getBlockedReason())) {
            return ValidationResult.blockedFailClosed(
                    List.of(REASON_LIQUIDITY_SEVERELY_DEGRADED_EXECUTABLE_ATTEMPT)
            );
        }
        return null;
    }

    private static List<String> freshnessReasons(RuntimeKlineContextSourceBindingDTO.FreshnessStatus status) {
        if (status == null) {
            return List.of(REASON_FRESHNESS_MISSING);
        }
        return switch (status) {
            case FRESH -> List.of();
            case STALE -> List.of(REASON_FRESHNESS_STALE);
            case UNKNOWN -> List.of(REASON_FRESHNESS_UNKNOWN);
        };
    }

    private static List<String> wickReasons(RuntimeKlineContextSourceBindingDTO.WickStatus status) {
        if (status == null) {
            return List.of(REASON_WICK_MISSING);
        }
        return switch (status) {
            case NONE, WICK_CONFIRMED -> List.of();
            case WICK_ONLY -> List.of(REASON_WICK_ONLY_INCOMPLETE);
            case UNKNOWN -> List.of(REASON_WICK_UNKNOWN);
        };
    }

    private static List<String> gapIncompleteReasons(RuntimeKlineContextSourceBindingDTO.GapStatus status) {
        if (status == null) {
            return List.of(REASON_GAP_MISSING);
        }
        return switch (status) {
            case NONE, MINOR_GAP, SEVERE_GAP -> List.of();
            case UNKNOWN -> List.of(REASON_GAP_UNKNOWN);
        };
    }

    private static List<String> liquidityReasons(RuntimeKlineContextSourceBindingDTO.LiquidityState state) {
        if (state == null) {
            return List.of(REASON_LIQUIDITY_MISSING);
        }
        return switch (state) {
            case NORMAL, DEGRADED -> List.of();
            case SEVERELY_DEGRADED -> List.of(REASON_LIQUIDITY_SEVERELY_DEGRADED);
            case UNKNOWN -> List.of(REASON_LIQUIDITY_UNKNOWN);
        };
    }

    private static List<String> stampedeIncompleteReasons(RuntimeKlineContextSourceBindingDTO.StampedeState state) {
        if (state == null) {
            return List.of(REASON_STAMPEDE_MISSING);
        }
        return switch (state) {
            case NONE, CONFIRMED -> List.of();
            case SUSPECTED -> List.of(REASON_STAMPEDE_SUSPECTED);
            case UNKNOWN -> List.of(REASON_STAMPEDE_UNKNOWN);
        };
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

    private static boolean safetyFlagsRequiredTrue(RuntimeKlineContextSourceBindingDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(RuntimeKlineContextSourceBindingDTO context) {
        List<String> outputs = new ArrayList<>();
        if (context.getBindingStatus() != null) {
            outputs.add(context.getBindingStatus().name());
        }
        outputs.add(context.getMarketDataSourceRef());
        outputs.add(context.getMissingReason());
        outputs.add(context.getBlockedReason());
        if (context.getSourceTraceRefs() != null) {
            outputs.addAll(context.getSourceTraceRefs());
        }

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

    private static boolean pointsToExecutableAttempt(String blockedReason) {
        if (blockedReason == null) {
            return false;
        }
        String normalizedReason = blockedReason.toLowerCase();
        return normalizedReason.contains("executable") || normalizedReason.contains("attempt");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public enum ValidationStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_RUNTIME_KLINE_BINDING,
        REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
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

        public static ValidationResult reviewOnlyRuntimeKlineBinding(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING,
                    true,
                    false,
                    false,
                    reasons
            );
        }

        public static ValidationResult reviewOnlyRuntimeKlineBindingDegraded(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED,
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
