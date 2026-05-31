package org.example.trademodel.service.evidence;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.evidence.ReviewOnlyEvidenceScoreEntryDTO;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyScanOutputDTO;

public class ReviewOnlyEvidenceScoreEntryAssembler {

    private static final String REASON_INPUT_MISSING = "REVIEW_ONLY_SCAN_OUTPUT_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED = "REVIEW_ONLY_SCAN_OUTPUT_FAIL_CLOSED";
    private static final String REASON_ENTRY_REVIEW_ONLY = "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY";
    private static final String STEP_BLOCKED_BY_SCAN_OUTPUT = "BLOCKED_BY_SCAN_OUTPUT";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_WAIT_FOR_SCORE_AUTHORIZATION = "WAIT_FOR_SCORE_AUTHORIZATION";
    private static final String STEP_READY_NORMALIZATION =
            "READY_FOR_EVIDENCE_NORMALIZATION_REVIEW_ONLY";
    private static final String STEP_READY_ENTRY =
            "READY_FOR_EVIDENCE_SCORE_ENTRY_REVIEW_ONLY";
    private static final String MESSAGE_BLOCKED =
            "Review-only evidence/score entry remains blocked and fail-closed.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only scan output is ready for evidence/score normalization review only.";

    public ReviewOnlyEvidenceScoreEntryDTO assemble(MarketReadReviewOnlyScanOutputDTO input) {
        if (input == null) {
            return ReviewOnlyEvidenceScoreEntryDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STATUS_BLOCKED,
                    STEP_BLOCKED_BY_SCAN_OUTPUT,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyEvidenceScoreEntryDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getScanOutputStatus(),
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    STATUS_BLOCKED,
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyEvidenceScoreEntryDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getScanOutputStatus(),
                withReason(input.getBlockingReasons(), REASON_ENTRY_REVIEW_ONLY),
                input.getRiskBlockers(),
                STATUS_REVIEW_ONLY,
                allowedReviewStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(MarketReadReviewOnlyScanOutputDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private String blockedNextStep(MarketReadReviewOnlyScanOutputDTO input) {
        if (input.getAllowedNextStep() == null || input.getAllowedNextStep().isBlank()) {
            return STEP_BLOCKED_BY_SCAN_OUTPUT;
        }
        return input.getAllowedNextStep();
    }

    private String allowedReviewStep(MarketReadReviewOnlyScanOutputDTO input) {
        if (!input.getRiskBlockers().isEmpty()) {
            return STEP_WAIT_FOR_SCORE_AUTHORIZATION;
        }
        if (STEP_READY_ENTRY.equals(input.getAllowedNextStep())
                || STEP_WAIT_FOR_REVIEW.equals(input.getAllowedNextStep())) {
            return STEP_READY_NORMALIZATION;
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
