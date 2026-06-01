package org.example.trademodel.service.evidence;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.evidence.ReviewOnlyEvidenceScoreEntryDTO;
import org.example.trademodel.dto.evidence.ReviewOnlyNormalizedEvidenceDTO;

public class ReviewOnlyEvidenceNormalizationAssembler {

    private static final String REASON_INPUT_MISSING = "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED = "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_FAIL_CLOSED";
    private static final String REASON_NORMALIZATION_REVIEW_ONLY = "REVIEW_ONLY_EVIDENCE_NORMALIZATION";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_EVIDENCE_NORMALIZATION";
    private static final String STEP_BLOCKED_BY_ENTRY = "BLOCKED_BY_EVIDENCE_ENTRY";
    private static final String STEP_READY_NORMALIZATION =
            "READY_FOR_EVIDENCE_NORMALIZATION_REVIEW_ONLY";
    private static final String STEP_READY_SCORE_INPUT_PRECHECK =
            "READY_FOR_SCORE_INPUT_PRECHECK_REVIEW_ONLY";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_SCORE_AUTHORIZATION = "WAIT_FOR_SCORE_AUTHORIZATION";
    private static final String MESSAGE_BLOCKED =
            "Review-only evidence normalization remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only evidence/score entry is ready for score input precheck review only.";

    public ReviewOnlyNormalizedEvidenceDTO assemble(ReviewOnlyEvidenceScoreEntryDTO input) {
        if (input == null) {
            return ReviewOnlyNormalizedEvidenceDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    STATUS_BLOCKED,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_ENTRY,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyNormalizedEvidenceDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getScanOutputStatus(),
                    input.getEntryStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyNormalizedEvidenceDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getScanOutputStatus(),
                input.getEntryStatus(),
                STATUS_REVIEW_ONLY,
                withReason(input.getBlockingReasons(), REASON_NORMALIZATION_REVIEW_ONLY),
                input.getRiskBlockers(),
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyEvidenceScoreEntryDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private String blockedNextStep(ReviewOnlyEvidenceScoreEntryDTO input) {
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_BLOCKED_BY_ENTRY;
        }
        return input.getAllowedNextStep();
    }

    private String allowedReviewStep(ReviewOnlyEvidenceScoreEntryDTO input) {
        if (!input.getRiskBlockers().isEmpty()) {
            return STEP_WAIT_FOR_SCORE_AUTHORIZATION;
        }
        if (STEP_READY_NORMALIZATION.equals(input.getAllowedNextStep())
                || STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_SCORE_INPUT_PRECHECK;
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
