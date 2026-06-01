package org.example.trademodel.service.point;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyPointBoundaryGateDTO;
import org.example.trademodel.dto.readiness.ReviewOnlyReadinessGateDTO;

public class ReviewOnlyPointBoundaryGateAssembler {

    private static final String REASON_INPUT_MISSING =
            "REVIEW_ONLY_READINESS_GATE_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED =
            "REVIEW_ONLY_READINESS_GATE_FAIL_CLOSED";
    private static final String REASON_INPUT_INCOMPLETE =
            "REVIEW_ONLY_READINESS_GATE_INCOMPLETE";
    private static final String REASON_POINT_BOUNDARY_REVIEW_ONLY =
            "REVIEW_ONLY_POINT_BOUNDARY_GATE";
    private static final String REASON_INCOMPLETE_SOURCE_CONTRACT =
            "INCOMPLETE_SOURCE_TRACE";
    private static final String REASON_INCOMPLETE_WATCHLIST_PROOF =
            "INCOMPLETE_WATCHLIST_POOL_PROOF";
    private static final String REASON_INCOMPLETE_TIMEFRAMES =
            "INCOMPLETE_MULTI_TIMEFRAME_CONFIRMATION";
    private static final String REASON_RECHECK_REQUIRED =
            "RECHECK_REQUIRED";
    private static final String REASON_RISK_ACTION_GUARD_REQUIRED =
            "RISK_ACTION_GUARD_REQUIRED";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_INCOMPLETE = "INCOMPLETE_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_POINT_BOUNDARY_GATE";
    private static final String STEP_BLOCKED_BY_READINESS_GATE =
            "BLOCKED_BY_READINESS_GATE";
    private static final String STEP_BLOCKED_BY_RISK_ACTION_GUARD =
            "BLOCKED_BY_RISK_ACTION_GUARD";
    private static final String STEP_INCOMPLETE_SOURCE_TRACE =
            "INCOMPLETE_SOURCE_TRACE";
    private static final String STEP_INCOMPLETE_MULTI_TIMEFRAME =
            "INCOMPLETE_MULTI_TIMEFRAME_CONFIRMATION";
    private static final String STEP_READY_REVIEW_ONLY_PROPOSAL =
            "READY_FOR_REVIEW_ONLY_POINT_PROPOSAL";
    private static final String MESSAGE_BLOCKED =
            "Review-only point boundary gate remains blocked and fail-closed.";
    private static final String MESSAGE_INCOMPLETE =
            "Review-only point boundary gate is incomplete and cannot advance.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only readiness gate may enter source-owned proposal review only; no values are generated.";

    public ReviewOnlyPointBoundaryGateDTO assemble(ReviewOnlyReadinessGateDTO input) {
        if (input == null) {
            return ReviewOnlyPointBoundaryGateDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_INCOMPLETE,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_READINESS_GATE,
                    MESSAGE_INCOMPLETE,
                    REASON_INPUT_MISSING
            );
        }

        if (input.isIncomplete()) {
            return ReviewOnlyPointBoundaryGateDTO.incomplete(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getReadinessGateStatus(),
                    STATUS_INCOMPLETE,
                    withReason(input.getBlockingReasons(), REASON_INPUT_INCOMPLETE),
                    input.getRiskBlockers(),
                    incompleteNextStep(input),
                    MESSAGE_INCOMPLETE,
                    REASON_INPUT_INCOMPLETE
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyPointBoundaryGateDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getReadinessGateStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED,
                    STEP_BLOCKED_BY_READINESS_GATE
            );
        }

        if (hasRiskBlockers(input)) {
            return ReviewOnlyPointBoundaryGateDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getReadinessGateStatus(),
                    STATUS_BLOCKED,
                    withReason(
                            withReason(input.getBlockingReasons(), REASON_RISK_ACTION_GUARD_REQUIRED),
                            REASON_RECHECK_REQUIRED
                    ),
                    input.getRiskBlockers(),
                    STEP_BLOCKED_BY_RISK_ACTION_GUARD,
                    MESSAGE_BLOCKED,
                    STEP_BLOCKED_BY_RISK_ACTION_GUARD
            );
        }

        if (isBlank(input.getSourceContractId())) {
            return incomplete(input, REASON_INCOMPLETE_SOURCE_CONTRACT, STEP_INCOMPLETE_SOURCE_TRACE);
        }

        if (isBlank(input.getWatchlistPoolProof())) {
            return incomplete(input, REASON_INCOMPLETE_WATCHLIST_PROOF, STEP_INCOMPLETE_SOURCE_TRACE);
        }

        if (input.getRequestedTimeframes().isEmpty()) {
            return incomplete(input, REASON_INCOMPLETE_TIMEFRAMES, STEP_INCOMPLETE_MULTI_TIMEFRAME);
        }

        return ReviewOnlyPointBoundaryGateDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getReadinessGateStatus(),
                STATUS_REVIEW_ONLY,
                withReason(
                        withReason(input.getBlockingReasons(), REASON_POINT_BOUNDARY_REVIEW_ONLY),
                        REASON_RECHECK_REQUIRED
                ),
                input.getRiskBlockers(),
                STEP_READY_REVIEW_ONLY_PROPOSAL,
                MESSAGE_REVIEW_ONLY,
                true,
                null
        );
    }

    private boolean isBlocked(ReviewOnlyReadinessGateDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private boolean hasRiskBlockers(ReviewOnlyReadinessGateDTO input) {
        return !input.getRiskBlockers().isEmpty();
    }

    private ReviewOnlyPointBoundaryGateDTO incomplete(
            ReviewOnlyReadinessGateDTO input,
            String reason,
            String nextStep
    ) {
        return ReviewOnlyPointBoundaryGateDTO.incomplete(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getReadinessGateStatus(),
                STATUS_INCOMPLETE,
                withReason(input.getBlockingReasons(), reason),
                input.getRiskBlockers(),
                nextStep,
                MESSAGE_INCOMPLETE,
                reason
        );
    }

    private String blockedNextStep(ReviewOnlyReadinessGateDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_BLOCKED_BY_READINESS_GATE;
        }
        return input.getAllowedNextStep();
    }

    private String incompleteNextStep(ReviewOnlyReadinessGateDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_INCOMPLETE_SOURCE_TRACE;
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
