package org.example.trademodel.service.score;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.evidence.ReviewOnlyNormalizedEvidenceDTO;
import org.example.trademodel.dto.score.ReviewOnlyScoreInputPrecheckDTO;

public class ReviewOnlyScoreInputPrecheckAssembler {

    private static final String REASON_INPUT_MISSING = "REVIEW_ONLY_NORMALIZED_EVIDENCE_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED = "REVIEW_ONLY_NORMALIZED_EVIDENCE_FAIL_CLOSED";
    private static final String REASON_PRECHECK_REVIEW_ONLY = "REVIEW_ONLY_SCORE_INPUT_PRECHECK";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_SCORE_INPUT_PRECHECK";
    private static final String STEP_BLOCKED_BY_EVIDENCE_NORMALIZATION =
            "BLOCKED_BY_EVIDENCE_NORMALIZATION";
    private static final String STEP_READY_SCORE_INPUT_PRECHECK =
            "READY_FOR_SCORE_INPUT_PRECHECK_REVIEW_ONLY";
    private static final String STEP_READY_SCORE_ASSEMBLY = "READY_FOR_REVIEW_ONLY_SCORE_ASSEMBLY";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_SCORE_AUTHORIZATION = "WAIT_FOR_SCORE_AUTHORIZATION";
    private static final String MESSAGE_BLOCKED =
            "Review-only score input precheck remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only normalized evidence is ready for score assembly review only.";

    public ReviewOnlyScoreInputPrecheckDTO assemble(ReviewOnlyNormalizedEvidenceDTO input) {
        if (input == null) {
            return ReviewOnlyScoreInputPrecheckDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_BLOCKED,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_EVIDENCE_NORMALIZATION,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyScoreInputPrecheckDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getEvidenceNormalizationStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyScoreInputPrecheckDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getEvidenceNormalizationStatus(),
                STATUS_REVIEW_ONLY,
                withReason(input.getBlockingReasons(), REASON_PRECHECK_REVIEW_ONLY),
                input.getRiskBlockers(),
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyNormalizedEvidenceDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private String blockedNextStep(ReviewOnlyNormalizedEvidenceDTO input) {
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_BLOCKED_BY_EVIDENCE_NORMALIZATION;
        }
        return input.getAllowedNextStep();
    }

    private String allowedReviewStep(ReviewOnlyNormalizedEvidenceDTO input) {
        if (!input.getRiskBlockers().isEmpty()) {
            return STEP_WAIT_FOR_SCORE_AUTHORIZATION;
        }
        if (STEP_READY_SCORE_INPUT_PRECHECK.equals(input.getAllowedNextStep())
                || STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_SCORE_ASSEMBLY;
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
