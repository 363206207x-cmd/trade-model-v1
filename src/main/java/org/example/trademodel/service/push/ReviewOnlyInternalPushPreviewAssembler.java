package org.example.trademodel.service.push;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidatePreviewGuardDTO;
import org.example.trademodel.dto.push.ReviewOnlyInternalPushPreviewDTO;

public class ReviewOnlyInternalPushPreviewAssembler {

    private static final String REASON_INPUT_MISSING =
            "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED =
            "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_FAIL_CLOSED";
    private static final String REASON_INTERNAL_PUSH_PREVIEW_REVIEW_ONLY =
            "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK";
    private static final String STEP_BLOCKED_BY_CANDIDATE_PREVIEW_GUARD =
            "BLOCKED_BY_CANDIDATE_PREVIEW_GUARD";
    private static final String STEP_READY_INTERNAL_PUSH_PREVIEW =
            "READY_FOR_INTERNAL_PUSH_PREVIEW_REVIEW_ONLY";
    private static final String STEP_READY_PUSH_PREVIEW_CLOSURE =
            "READY_FOR_PUSH_PREVIEW_CLOSURE_REVIEW_ONLY";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_RISK_ACTION_GUARD_RECHECK =
            "WAIT_FOR_RISK_ACTION_GUARD_RECHECK";
    private static final String MESSAGE_BLOCKED =
            "Review-only internal push preview remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only candidate preview guard is ready for push preview closure review only.";

    public ReviewOnlyInternalPushPreviewDTO assemble(ReviewOnlyCandidatePreviewGuardDTO input) {
        if (input == null) {
            return ReviewOnlyInternalPushPreviewDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_BLOCKED,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_CANDIDATE_PREVIEW_GUARD,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyInternalPushPreviewDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getCandidatePreviewGuardStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyInternalPushPreviewDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getCandidatePreviewGuardStatus(),
                STATUS_REVIEW_ONLY,
                withReason(input.getBlockingReasons(), REASON_INTERNAL_PUSH_PREVIEW_REVIEW_ONLY),
                input.getRiskBlockers(),
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyCandidatePreviewGuardDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private String blockedNextStep(ReviewOnlyCandidatePreviewGuardDTO input) {
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_BLOCKED_BY_CANDIDATE_PREVIEW_GUARD;
        }
        return input.getAllowedNextStep();
    }

    private String allowedReviewStep(ReviewOnlyCandidatePreviewGuardDTO input) {
        if (!input.getRiskBlockers().isEmpty()) {
            return STEP_WAIT_FOR_RISK_ACTION_GUARD_RECHECK;
        }
        if (STEP_READY_INTERNAL_PUSH_PREVIEW.equals(input.getAllowedNextStep())
                || STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_PUSH_PREVIEW_CLOSURE;
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
