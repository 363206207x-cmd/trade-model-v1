package org.example.trademodel.validator.point;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyNumericPointProposalDTO;

public class NumericPointSafetyValidator {

    private static final String REASON_PROPOSAL_MISSING = "PROPOSAL_MISSING";
    private static final String REASON_STATUS_MISSING = "STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_REQUIRED = "FAIL_CLOSED_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_SOURCE_TRACE_REF_MISSING = "SOURCE_TRACE_REF_MISSING";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING = "RUNTIME_KLINE_CONTEXT_REF_MISSING";
    private static final String REASON_DATA_QUALITY_CONTEXT_REF_MISSING = "DATA_QUALITY_CONTEXT_REF_MISSING";
    private static final String REASON_MULTI_TIMEFRAME_CONTEXT_REF_MISSING = "MULTI_TIMEFRAME_CONTEXT_REF_MISSING";
    private static final String REASON_RISK_ACTION_GUARD_REF_MISSING = "RISK_ACTION_GUARD_REF_MISSING";
    private static final String REASON_WATCHLIST_POOL_PROOF_MISSING = "WATCHLIST_POOL_PROOF_MISSING";
    private static final String REASON_ENTRY_REVIEW_POINT_MISSING = "ENTRY_REVIEW_POINT_MISSING";
    private static final String REASON_STOP_REVIEW_POINT_MISSING = "STOP_REVIEW_POINT_MISSING";
    private static final String REASON_TAKE_PROFIT_REVIEW_LEVEL_MISSING = "TAKE_PROFIT_REVIEW_LEVEL_MISSING";
    private static final String REASON_RISK_REWARD_REVIEW_FIELD_MISSING = "RISK_REWARD_REVIEW_FIELD_MISSING";
    private static final String REASON_FORBIDDEN_SEMANTIC_DETECTED = "FORBIDDEN_SEMANTIC_DETECTED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

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

    public ValidationResult validate(ReviewOnlyNumericPointProposalDTO proposal) {
        if (proposal == null) {
            return ValidationResult.incomplete(List.of(REASON_PROPOSAL_MISSING));
        }

        if (containsForbiddenExecutableSemantic(proposal)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_SEMANTIC_DETECTED));
        }

        if (!safetyFlagsRequiredTrue(proposal)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        ReviewOnlyNumericPointProposalDTO.ProposalStatus status = proposal.getProposalStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(proposal);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(proposal);
            case REVIEW_ONLY_NUMERIC_POINT_CANDIDATE -> validateCandidate(proposal);
            case REVIEW_ONLY_NUMERIC_POINT_DEGRADED -> validateDegraded(proposal);
            case RECHECK_REQUIRED, MANUAL_REVIEW_REQUIRED ->
                    ValidationResult.incomplete(List.of(REASON_UNSUPPORTED_STATUS));
        };
    }

    private static ValidationResult validateIncomplete(ReviewOnlyNumericPointProposalDTO proposal) {
        List<String> missingReasons = proposal.getMissingReasons();
        if (isEmpty(missingReasons)) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(missingReasons);
    }

    private static ValidationResult validateBlockedFailClosed(ReviewOnlyNumericPointProposalDTO proposal) {
        List<String> reasons = new ArrayList<>();
        if (!proposal.isFailClosed()) {
            reasons.add(REASON_FAIL_CLOSED_REQUIRED);
        }
        if (isEmpty(proposal.getBlockedReasons())) {
            reasons.add(REASON_BLOCKED_REASON_REQUIRED);
        }
        if (!reasons.isEmpty()) {
            return ValidationResult.blockedFailClosed(reasons);
        }
        return ValidationResult.blockedFailClosed(proposal.getBlockedReasons());
    }

    private static ValidationResult validateCandidate(ReviewOnlyNumericPointProposalDTO proposal) {
        List<String> reasons = requiredContractRefReasons(proposal);
        if (proposal.getEntry() == null) {
            reasons.add(REASON_ENTRY_REVIEW_POINT_MISSING);
        }
        if (proposal.getStop() == null) {
            reasons.add(REASON_STOP_REVIEW_POINT_MISSING);
        }
        if (proposal.getTakeProfitLevels().isEmpty()) {
            reasons.add(REASON_TAKE_PROFIT_REVIEW_LEVEL_MISSING);
        }
        if (proposal.getRiskReward() == null) {
            reasons.add(REASON_RISK_REWARD_REVIEW_FIELD_MISSING);
        }
        if (!reasons.isEmpty()) {
            return ValidationResult.incomplete(reasons);
        }
        return ValidationResult.reviewOnlyNumericPointCandidate(List.of());
    }

    private static ValidationResult validateDegraded(ReviewOnlyNumericPointProposalDTO proposal) {
        List<String> reasons = requiredContractRefReasons(proposal);
        if (isEmpty(proposal.getMissingReasons())) {
            reasons.add(REASON_MISSING_REASON_REQUIRED);
        }
        if (!reasons.isEmpty()) {
            return ValidationResult.incomplete(reasons);
        }
        return ValidationResult.reviewOnlyNumericPointDegraded(proposal.getMissingReasons());
    }

    private static List<String> requiredContractRefReasons(ReviewOnlyNumericPointProposalDTO proposal) {
        List<String> reasons = new ArrayList<>();
        if (proposal.getSourceTraceRefs().isEmpty()) {
            reasons.add(REASON_SOURCE_TRACE_REF_MISSING);
        }
        if (proposal.getRuntimeKlineContextRefs().isEmpty()) {
            reasons.add(REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING);
        }
        if (isBlank(proposal.getDataQualityContextRef())) {
            reasons.add(REASON_DATA_QUALITY_CONTEXT_REF_MISSING);
        }
        if (isBlank(proposal.getMultiTimeframeContextRef())) {
            reasons.add(REASON_MULTI_TIMEFRAME_CONTEXT_REF_MISSING);
        }
        if (isBlank(proposal.getRiskActionGuardRef())) {
            reasons.add(REASON_RISK_ACTION_GUARD_REF_MISSING);
        }
        if (isBlank(proposal.getWatchlistPoolProof())) {
            reasons.add(REASON_WATCHLIST_POOL_PROOF_MISSING);
        }
        return reasons;
    }

    private static boolean safetyFlagsRequiredTrue(ReviewOnlyNumericPointProposalDTO proposal) {
        return proposal.isReviewOnly()
                && proposal.isNotTradeInstruction()
                && proposal.isManualReviewRequired()
                && proposal.isRecheckRequired()
                && proposal.isRiskActionGuardRequired()
                && proposal.isSourceTraceRequired()
                && proposal.isRuntimeKlineContextRequired()
                && proposal.isDataQualityRequired()
                && proposal.isMultiTimeframeRequired()
                && proposal.isIncompleteSafe();
    }

    private static boolean containsForbiddenExecutableSemantic(ReviewOnlyNumericPointProposalDTO proposal) {
        List<String> outputs = new ArrayList<>();
        if (proposal.getProposalStatus() != null) {
            outputs.add(proposal.getProposalStatus().name());
        }
        outputs.addAll(proposal.getMissingReasons());
        outputs.addAll(proposal.getBlockedReasons());
        outputs.addAll(proposal.getForbiddenSemantics());

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

    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty() || values.stream().allMatch(NumericPointSafetyValidator::isBlank);
    }

    public enum ValidationStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_NUMERIC_POINT_CANDIDATE,
        REVIEW_ONLY_NUMERIC_POINT_DEGRADED
    }

    public static class ValidationResult {
        private final ValidationStatus status;
        private final boolean validForReviewOnly;
        private final boolean incomplete;
        private final boolean blockedFailClosed;
        private final boolean recheckRequired;
        private final boolean manualReviewRequired;
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
            this.recheckRequired = true;
            this.manualReviewRequired = true;
            this.reasons = copy(reasons);
        }

        public static ValidationResult incomplete(List<String> reasons) {
            return new ValidationResult(ValidationStatus.INCOMPLETE, false, true, false, reasons);
        }

        public static ValidationResult blockedFailClosed(List<String> reasons) {
            return new ValidationResult(ValidationStatus.BLOCKED_FAIL_CLOSED, false, false, true, reasons);
        }

        public static ValidationResult reviewOnlyNumericPointCandidate(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE,
                    true,
                    false,
                    false,
                    reasons
            );
        }

        public static ValidationResult reviewOnlyNumericPointDegraded(List<String> reasons) {
            return new ValidationResult(
                    ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED,
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

        public boolean isRecheckRequired() {
            return recheckRequired;
        }

        public boolean isManualReviewRequired() {
            return manualReviewRequired;
        }

        public List<String> getReasons() {
            return copy(reasons);
        }

        private static List<String> copy(List<String> values) {
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        }
    }
}
