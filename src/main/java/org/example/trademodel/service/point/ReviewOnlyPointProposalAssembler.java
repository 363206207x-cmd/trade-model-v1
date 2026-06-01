package org.example.trademodel.service.point;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyPointBoundaryGateDTO;
import org.example.trademodel.dto.point.ReviewOnlyPointProposalDTO;

public class ReviewOnlyPointProposalAssembler {

    private static final String REASON_INPUT_MISSING =
            "REVIEW_ONLY_POINT_BOUNDARY_GATE_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED =
            "REVIEW_ONLY_POINT_BOUNDARY_GATE_FAIL_CLOSED";
    private static final String REASON_INPUT_INCOMPLETE =
            "REVIEW_ONLY_POINT_BOUNDARY_GATE_INCOMPLETE";
    private static final String REASON_POINT_BOUNDARY_NOT_ALLOWED =
            "POINT_BOUNDARY_GATE_NOT_ALLOWED";
    private static final String REASON_SOURCE_OWNED_INPUT_REQUIRED =
            "INCOMPLETE_SOURCE_OWNED_POINT_INPUT";
    private static final String REASON_SOURCE_TRACE_REQUIRED =
            "SOURCE_TRACE_REQUIRED";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_REQUIRED =
            "RUNTIME_KLINE_CONTEXT_REQUIRED";
    private static final String REASON_INCOMPLETE_DATA_QUALITY =
            "INCOMPLETE_DATA_QUALITY";
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
    private static final String STEP_BLOCKED_BY_POINT_BOUNDARY_GATE =
            "BLOCKED_BY_POINT_BOUNDARY_GATE";
    private static final String STEP_BLOCKED_BY_RISK_ACTION_GUARD =
            "BLOCKED_BY_RISK_ACTION_GUARD";
    private static final String STEP_INCOMPLETE_SOURCE_TRACE =
            "INCOMPLETE_SOURCE_TRACE";
    private static final String STEP_INCOMPLETE_MULTI_TIMEFRAME =
            "INCOMPLETE_MULTI_TIMEFRAME_CONFIRMATION";
    private static final String STEP_WAIT_SOURCE_OWNED_INPUT =
            "WAIT_FOR_SOURCE_OWNED_POINT_INPUT";
    private static final String MESSAGE_BLOCKED =
            "Source-owned review-only point proposal remains blocked and fail-closed.";
    private static final String MESSAGE_INCOMPLETE =
            "Source-owned review-only point proposal is incomplete; no point values are generated.";
    private static final String MESSAGE_WAITING_FOR_SOURCE_INPUT =
            "Source-owned review-only point proposal is waiting for source trace and runtime kline context.";

    public ReviewOnlyPointProposalDTO assemble(ReviewOnlyPointBoundaryGateDTO input) {
        if (input == null) {
            return ReviewOnlyPointProposalDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    STATUS_INCOMPLETE,
                    false,
                    REASON_INPUT_MISSING,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_POINT_BOUNDARY_GATE,
                    MESSAGE_INCOMPLETE
            );
        }

        if (input.isIncomplete()) {
            return incomplete(
                    input,
                    REASON_INPUT_INCOMPLETE,
                    incompleteNextStep(input),
                    input.isPointProposalAllowed()
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyPointProposalDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getReadinessGateStatus(),
                    input.getPointBoundaryGateStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED,
                    blockedReason(input, REASON_INPUT_FAIL_CLOSED)
            );
        }

        if (!input.isPointProposalAllowed()) {
            return incomplete(
                    input,
                    blockedReason(input, REASON_POINT_BOUNDARY_NOT_ALLOWED),
                    STEP_BLOCKED_BY_POINT_BOUNDARY_GATE,
                    false
            );
        }

        if (hasRiskBlockers(input)) {
            return ReviewOnlyPointProposalDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getReadinessGateStatus(),
                    input.getPointBoundaryGateStatus(),
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
            return incomplete(input, REASON_INCOMPLETE_SOURCE_CONTRACT, STEP_INCOMPLETE_SOURCE_TRACE, false);
        }

        if (isBlank(input.getWatchlistPoolProof())) {
            return incomplete(input, REASON_INCOMPLETE_WATCHLIST_PROOF, STEP_INCOMPLETE_SOURCE_TRACE, false);
        }

        if (input.getRequestedTimeframes().isEmpty()) {
            return incomplete(input, REASON_INCOMPLETE_TIMEFRAMES, STEP_INCOMPLETE_MULTI_TIMEFRAME, false);
        }

        return ReviewOnlyPointProposalDTO.incomplete(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getReadinessGateStatus(),
                input.getPointBoundaryGateStatus(),
                STATUS_INCOMPLETE,
                true,
                REASON_SOURCE_OWNED_INPUT_REQUIRED,
                sourceOwnedIncompleteReasons(input.getBlockingReasons()),
                input.getRiskBlockers(),
                STEP_WAIT_SOURCE_OWNED_INPUT,
                MESSAGE_WAITING_FOR_SOURCE_INPUT
        );
    }

    private boolean isBlocked(ReviewOnlyPointBoundaryGateDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private boolean hasRiskBlockers(ReviewOnlyPointBoundaryGateDTO input) {
        return !input.getRiskBlockers().isEmpty();
    }

    private ReviewOnlyPointProposalDTO incomplete(
            ReviewOnlyPointBoundaryGateDTO input,
            String reason,
            String nextStep,
            boolean pointProposalAllowed
    ) {
        return ReviewOnlyPointProposalDTO.incomplete(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getReadinessGateStatus(),
                input.getPointBoundaryGateStatus(),
                STATUS_INCOMPLETE,
                pointProposalAllowed,
                reason,
                withReason(input.getBlockingReasons(), reason),
                input.getRiskBlockers(),
                nextStep,
                MESSAGE_INCOMPLETE
        );
    }

    private String blockedNextStep(ReviewOnlyPointBoundaryGateDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_BLOCKED_BY_POINT_BOUNDARY_GATE;
        }
        return input.getAllowedNextStep();
    }

    private String incompleteNextStep(ReviewOnlyPointBoundaryGateDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_INCOMPLETE_SOURCE_TRACE;
        }
        return input.getAllowedNextStep();
    }

    private String blockedReason(ReviewOnlyPointBoundaryGateDTO input, String fallbackReason) {
        if (isBlank(input.getPointProposalBlockedReason())) {
            return fallbackReason;
        }
        return input.getPointProposalBlockedReason();
    }

    private List<String> sourceOwnedIncompleteReasons(List<String> reasons) {
        List<String> resolvedReasons = withReason(reasons, REASON_SOURCE_OWNED_INPUT_REQUIRED);
        resolvedReasons = withReason(resolvedReasons, REASON_SOURCE_TRACE_REQUIRED);
        resolvedReasons = withReason(resolvedReasons, REASON_RUNTIME_KLINE_CONTEXT_REQUIRED);
        resolvedReasons = withReason(resolvedReasons, REASON_INCOMPLETE_DATA_QUALITY);
        resolvedReasons = withReason(resolvedReasons, REASON_RECHECK_REQUIRED);
        return resolvedReasons;
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
