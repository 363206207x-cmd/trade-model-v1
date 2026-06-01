package org.example.trademodel.service.point;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyPointProposalDTO;
import org.example.trademodel.dto.point.ReviewOnlyPointProposalDisplayDTO;

public class ReviewOnlyPointProposalDisplayAssembler {

    private static final String REASON_INPUT_MISSING =
            "REVIEW_ONLY_POINT_PROPOSAL_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED =
            "REVIEW_ONLY_POINT_PROPOSAL_FAIL_CLOSED";
    private static final String REASON_INPUT_INCOMPLETE =
            "REVIEW_ONLY_POINT_PROPOSAL_INCOMPLETE";
    private static final String REASON_DISPLAY_REVIEW_ONLY =
            "REVIEW_ONLY_POINT_PROPOSAL_DISPLAY_GATE";
    private static final String REASON_RISK_ACTION_GUARD_REQUIRED =
            "RISK_ACTION_GUARD_REQUIRED";
    private static final String STATUS_REVIEW_ONLY =
            "REVIEW_ONLY_POINT_PROPOSAL_CLOSURE_DISPLAY_GATE";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_INCOMPLETE = "INCOMPLETE_FAIL_CLOSED";
    private static final String STEP_WAIT_FOR_REVIEW =
            "WAIT_FOR_REVIEW";
    private static final String STEP_BLOCKED_BY_POINT_PROPOSAL =
            "BLOCKED_BY_POINT_PROPOSAL";
    private static final String STEP_BLOCKED_BY_RISK_ACTION_GUARD =
            "BLOCKED_BY_RISK_ACTION_GUARD";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only display gate requires source trace, runtime kline context, recheck, Risk Action Guard, and manual review.";
    private static final String MESSAGE_INCOMPLETE =
            "Review-only display gate is incomplete-safe and hides proposal values.";
    private static final String MESSAGE_BLOCKED =
            "Review-only display gate remains blocked and fail-closed.";

    public ReviewOnlyPointProposalDisplayDTO assemble(ReviewOnlyPointProposalDTO input) {
        if (input == null) {
            return ReviewOnlyPointProposalDisplayDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_INCOMPLETE,
                    REASON_INPUT_MISSING,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_POINT_PROPOSAL,
                    MESSAGE_INCOMPLETE
            );
        }

        if (input.isIncomplete()) {
            return ReviewOnlyPointProposalDisplayDTO.incomplete(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getPointProposalStatus(),
                    STATUS_INCOMPLETE,
                    incompleteReason(input),
                    withReason(input.getBlockingReasons(), REASON_INPUT_INCOMPLETE),
                    input.getRiskBlockers(),
                    incompleteNextStep(input),
                    MESSAGE_INCOMPLETE
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyPointProposalDisplayDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getPointProposalStatus(),
                    STATUS_BLOCKED,
                    blockedReason(input),
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        if (hasRiskBlockers(input)) {
            return ReviewOnlyPointProposalDisplayDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getPointProposalStatus(),
                    STATUS_BLOCKED,
                    STEP_BLOCKED_BY_RISK_ACTION_GUARD,
                    withReason(input.getBlockingReasons(), REASON_RISK_ACTION_GUARD_REQUIRED),
                    input.getRiskBlockers(),
                    STEP_BLOCKED_BY_RISK_ACTION_GUARD,
                    MESSAGE_BLOCKED
            );
        }

        return ReviewOnlyPointProposalDisplayDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getPointProposalStatus(),
                STATUS_REVIEW_ONLY,
                withReason(input.getBlockingReasons(), REASON_DISPLAY_REVIEW_ONLY),
                input.getRiskBlockers(),
                reviewOnlyNextStep(input),
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyPointProposalDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private boolean hasRiskBlockers(ReviewOnlyPointProposalDTO input) {
        return !input.getRiskBlockers().isEmpty();
    }

    private String blockedReason(ReviewOnlyPointProposalDTO input) {
        if (isBlank(input.getPointProposalBlockedReason())) {
            return REASON_INPUT_FAIL_CLOSED;
        }
        return input.getPointProposalBlockedReason();
    }

    private String incompleteReason(ReviewOnlyPointProposalDTO input) {
        if (isBlank(input.getPointProposalBlockedReason())) {
            return REASON_INPUT_INCOMPLETE;
        }
        return input.getPointProposalBlockedReason();
    }

    private String blockedNextStep(ReviewOnlyPointProposalDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_BLOCKED_BY_POINT_PROPOSAL;
        }
        return input.getAllowedNextStep();
    }

    private String incompleteNextStep(ReviewOnlyPointProposalDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_WAIT_FOR_REVIEW;
        }
        return input.getAllowedNextStep();
    }

    private String reviewOnlyNextStep(ReviewOnlyPointProposalDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_WAIT_FOR_REVIEW;
        }
        return input.getAllowedNextStep();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
