package org.example.trademodel.service.candidate;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidateHandoffDTO;
import org.example.trademodel.dto.score.ReviewOnlyScoreAssemblyDTO;

public class ReviewOnlyScoreToCandidateHandoffAssembler {

    private static final String REASON_INPUT_MISSING = "REVIEW_ONLY_SCORE_ASSEMBLY_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED = "REVIEW_ONLY_SCORE_ASSEMBLY_FAIL_CLOSED";
    private static final String REASON_HANDOFF_REVIEW_ONLY = "REVIEW_ONLY_CANDIDATE_HANDOFF";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_CANDIDATE_HANDOFF";
    private static final String STEP_BLOCKED_BY_SCORE_ASSEMBLY = "BLOCKED_BY_SCORE_ASSEMBLY";
    private static final String STEP_READY_SCORE_HANDOFF =
            "READY_FOR_SCORE_TO_CANDIDATE_HANDOFF_REVIEW_ONLY";
    private static final String STEP_READY_CANDIDATE_ATTENTION =
            "READY_FOR_REVIEW_ONLY_CANDIDATE_ATTENTION";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_CANDIDATE_AUTHORIZATION =
            "WAIT_FOR_CANDIDATE_AUTHORIZATION";
    private static final String MESSAGE_BLOCKED =
            "Review-only candidate handoff remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only score assembly is ready for candidate attention review only.";

    public ReviewOnlyCandidateHandoffDTO assemble(ReviewOnlyScoreAssemblyDTO input) {
        if (input == null) {
            return ReviewOnlyCandidateHandoffDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_BLOCKED,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_SCORE_ASSEMBLY,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyCandidateHandoffDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getScoreAssemblyStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyCandidateHandoffDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getScoreAssemblyStatus(),
                STATUS_REVIEW_ONLY,
                withReason(input.getBlockingReasons(), REASON_HANDOFF_REVIEW_ONLY),
                input.getRiskBlockers(),
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyScoreAssemblyDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private String blockedNextStep(ReviewOnlyScoreAssemblyDTO input) {
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_BLOCKED_BY_SCORE_ASSEMBLY;
        }
        return input.getAllowedNextStep();
    }

    private String allowedReviewStep(ReviewOnlyScoreAssemblyDTO input) {
        if (!input.getRiskBlockers().isEmpty()) {
            return STEP_WAIT_FOR_CANDIDATE_AUTHORIZATION;
        }
        if (STEP_READY_SCORE_HANDOFF.equals(input.getAllowedNextStep())
                || STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_CANDIDATE_ATTENTION;
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
