package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationStatusEnum;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyOutputDTO;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyScanOutputDTO;

public class MarketReadReviewOnlyScanOutputAssembler {

    private static final String REASON_INPUT_MISSING = "MARKET_READ_REVIEW_ONLY_OUTPUT_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED = "MARKET_READ_REVIEW_ONLY_INPUT_FAIL_CLOSED";
    private static final String REASON_SCAN_OUTPUT_REVIEW_ONLY = "MARKET_READ_REVIEW_ONLY_SCAN_OUTPUT";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_SCAN_OUTPUT";
    private static final String STEP_BLOCKED_BY_GUARD = "BLOCKED_BY_GUARD";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_MARKET_READ_AUTHORIZATION = "WAIT_FOR_MARKET_READ_AUTHORIZATION";
    private static final String STEP_READY_REVIEW_ONLY = "READY_FOR_EVIDENCE_SCORE_ENTRY_REVIEW_ONLY";
    private static final String MESSAGE_BLOCKED =
            "Market read review-only scan output remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Market read review-only scan output is ready for the next manual-review skeleton.";

    public MarketReadReviewOnlyScanOutputDTO assemble(MarketReadReviewOnlyOutputDTO input) {
        if (input == null) {
            return MarketReadReviewOnlyScanOutputDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    MarketReadRequestGuardValidationStatusEnum.BLOCKED,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STATUS_BLOCKED,
                    STEP_BLOCKED_BY_GUARD,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return MarketReadReviewOnlyScanOutputDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getGuardValidationStatus(),
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    STATUS_BLOCKED,
                    input.getAllowedNextStep(),
                    MESSAGE_BLOCKED
            );
        }

        return MarketReadReviewOnlyScanOutputDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getGuardValidationStatus(),
                withReason(input.getBlockingReasons(), REASON_SCAN_OUTPUT_REVIEW_ONLY),
                input.getRiskBlockers(),
                STATUS_REVIEW_ONLY,
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(MarketReadReviewOnlyOutputDTO input) {
        return input.isFailClosed()
                || MarketReadRequestGuardValidationStatusEnum.BLOCKED.equals(input.getGuardValidationStatus());
    }

    private String allowedReviewStep(MarketReadReviewOnlyOutputDTO input) {
        if (input.getRiskBlockers().isEmpty()
                && STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_REVIEW_ONLY;
        }
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_WAIT_FOR_REVIEW;
        }
        if (STEP_WAIT_FOR_MARKET_READ_AUTHORIZATION.equals(input.getAllowedNextStep())) {
            return STEP_WAIT_FOR_MARKET_READ_AUTHORIZATION;
        }
        return input.getAllowedNextStep();
    }

    private List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = copy(reasons);
        if (!resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }

    private <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
