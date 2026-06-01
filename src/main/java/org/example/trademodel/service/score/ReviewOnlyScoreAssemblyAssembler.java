package org.example.trademodel.service.score;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.score.ReviewOnlyScoreAssemblyDTO;
import org.example.trademodel.dto.score.ReviewOnlyScoreInputPrecheckDTO;

public class ReviewOnlyScoreAssemblyAssembler {

    private static final String REASON_INPUT_MISSING = "REVIEW_ONLY_SCORE_INPUT_PRECHECK_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED = "REVIEW_ONLY_SCORE_INPUT_PRECHECK_FAIL_CLOSED";
    private static final String REASON_ASSEMBLY_REVIEW_ONLY = "REVIEW_ONLY_SCORE_ASSEMBLY";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_SCORE_ASSEMBLY";
    private static final String STEP_BLOCKED_BY_SCORE_PRECHECK = "BLOCKED_BY_SCORE_PRECHECK";
    private static final String STEP_READY_SCORE_ASSEMBLY = "READY_FOR_REVIEW_ONLY_SCORE_ASSEMBLY";
    private static final String STEP_READY_SCORE_HANDOFF =
            "READY_FOR_SCORE_TO_CANDIDATE_HANDOFF_REVIEW_ONLY";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_SCORE_AUTHORIZATION =
            "WAIT_FOR_SCORE_CALCULATION_AUTHORIZATION";
    private static final String MESSAGE_BLOCKED =
            "Review-only score assembly remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only score input precheck is ready for score handoff review only.";

    public ReviewOnlyScoreAssemblyDTO assemble(ReviewOnlyScoreInputPrecheckDTO input) {
        if (input == null) {
            return ReviewOnlyScoreAssemblyDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_BLOCKED,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_SCORE_PRECHECK,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyScoreAssemblyDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getScoreInputPrecheckStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyScoreAssemblyDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getScoreInputPrecheckStatus(),
                STATUS_REVIEW_ONLY,
                withReason(input.getBlockingReasons(), REASON_ASSEMBLY_REVIEW_ONLY),
                input.getRiskBlockers(),
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyScoreInputPrecheckDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private String blockedNextStep(ReviewOnlyScoreInputPrecheckDTO input) {
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_BLOCKED_BY_SCORE_PRECHECK;
        }
        return input.getAllowedNextStep();
    }

    private String allowedReviewStep(ReviewOnlyScoreInputPrecheckDTO input) {
        if (!input.getRiskBlockers().isEmpty()) {
            return STEP_WAIT_FOR_SCORE_AUTHORIZATION;
        }
        if (STEP_READY_SCORE_ASSEMBLY.equals(input.getAllowedNextStep())
                || STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_SCORE_HANDOFF;
        }
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_WAIT_FOR_REVIEW;
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
