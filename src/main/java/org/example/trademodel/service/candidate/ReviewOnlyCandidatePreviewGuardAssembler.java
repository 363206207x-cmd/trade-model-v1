package org.example.trademodel.service.candidate;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidateAttentionDTO;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidatePreviewGuardDTO;

public class ReviewOnlyCandidatePreviewGuardAssembler {

    private static final String REASON_INPUT_MISSING = "REVIEW_ONLY_CANDIDATE_ATTENTION_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED = "REVIEW_ONLY_CANDIDATE_ATTENTION_FAIL_CLOSED";
    private static final String REASON_PREVIEW_GUARD_REVIEW_ONLY =
            "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD";
    private static final String STEP_BLOCKED_BY_CANDIDATE_ATTENTION =
            "BLOCKED_BY_CANDIDATE_ATTENTION";
    private static final String STEP_READY_CANDIDATE_PREVIEW =
            "READY_FOR_CANDIDATE_PREVIEW_REVIEW_ONLY";
    private static final String STEP_READY_INTERNAL_PUSH_PREVIEW =
            "READY_FOR_INTERNAL_PUSH_PREVIEW_REVIEW_ONLY";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_CANDIDATE_RANKING_AUTHORIZATION =
            "WAIT_FOR_CANDIDATE_RANKING_AUTHORIZATION";
    private static final String MESSAGE_BLOCKED =
            "Review-only candidate preview guard remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only candidate attention is ready for internal push preview review only.";

    public ReviewOnlyCandidatePreviewGuardDTO assemble(ReviewOnlyCandidateAttentionDTO input) {
        if (input == null) {
            return ReviewOnlyCandidatePreviewGuardDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_BLOCKED,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_CANDIDATE_ATTENTION,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyCandidatePreviewGuardDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getCandidateAttentionStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyCandidatePreviewGuardDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getCandidateAttentionStatus(),
                STATUS_REVIEW_ONLY,
                withReason(input.getBlockingReasons(), REASON_PREVIEW_GUARD_REVIEW_ONLY),
                input.getRiskBlockers(),
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyCandidateAttentionDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private String blockedNextStep(ReviewOnlyCandidateAttentionDTO input) {
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_BLOCKED_BY_CANDIDATE_ATTENTION;
        }
        return input.getAllowedNextStep();
    }

    private String allowedReviewStep(ReviewOnlyCandidateAttentionDTO input) {
        if (!input.getRiskBlockers().isEmpty()) {
            return STEP_WAIT_FOR_CANDIDATE_RANKING_AUTHORIZATION;
        }
        if (STEP_READY_CANDIDATE_PREVIEW.equals(input.getAllowedNextStep())
                || STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_INTERNAL_PUSH_PREVIEW;
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
