package org.example.trademodel.service.readiness;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.push.ReviewOnlyInternalPushPreviewDTO;
import org.example.trademodel.dto.readiness.ReviewOnlyReadinessGateDTO;

public class ReviewOnlyReadinessGateAssembler {

    private static final String REASON_INPUT_MISSING =
            "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_MISSING";
    private static final String REASON_INPUT_FAIL_CLOSED =
            "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_FAIL_CLOSED";
    private static final String REASON_READINESS_GATE_REVIEW_ONLY =
            "REVIEW_ONLY_READINESS_GATE";
    private static final String REASON_INCOMPLETE_SOURCE_CONTRACT =
            "INCOMPLETE_SOURCE_TRACE";
    private static final String REASON_INCOMPLETE_WATCHLIST_PROOF =
            "INCOMPLETE_WATCHLIST_POOL_PROOF";
    private static final String REASON_INCOMPLETE_TIMEFRAMES =
            "INCOMPLETE_DATA_QUALITY";
    private static final String REASON_RECHECK_REQUIRED =
            "RECHECK_REQUIRED";
    private static final String REASON_RISK_ACTION_GUARD_REQUIRED =
            "RISK_ACTION_GUARD_REQUIRED";
    private static final String STATUS_BLOCKED = "BLOCKED_FAIL_CLOSED";
    private static final String STATUS_INCOMPLETE = "INCOMPLETE_FAIL_CLOSED";
    private static final String STATUS_REVIEW_ONLY = "REVIEW_ONLY_READINESS_GATE";
    private static final String STEP_BLOCKED_BY_INTERNAL_PUSH_PREVIEW =
            "BLOCKED_BY_INTERNAL_PUSH_PREVIEW";
    private static final String STEP_BLOCKED_BY_RISK_ACTION_GUARD =
            "BLOCKED_BY_RISK_ACTION_GUARD";
    private static final String STEP_INCOMPLETE_SOURCE_TRACE =
            "INCOMPLETE_SOURCE_TRACE";
    private static final String STEP_INCOMPLETE_DATA_QUALITY =
            "INCOMPLETE_DATA_QUALITY";
    private static final String STEP_READY_POINT_BOUNDARY =
            "READY_FOR_POINT_BOUNDARY_REVIEW_ONLY";
    private static final String MESSAGE_BLOCKED =
            "Review-only readiness gate remains blocked and fail-closed.";
    private static final String MESSAGE_INCOMPLETE =
            "Review-only readiness gate is incomplete and cannot advance.";
    private static final String MESSAGE_REVIEW_ONLY =
            "Review-only internal push preview can enter point boundary review only.";

    public ReviewOnlyReadinessGateDTO assemble(ReviewOnlyInternalPushPreviewDTO input) {
        if (input == null) {
            return ReviewOnlyReadinessGateDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    STATUS_INCOMPLETE,
                    List.of(REASON_INPUT_MISSING),
                    List.of(),
                    STEP_BLOCKED_BY_INTERNAL_PUSH_PREVIEW,
                    MESSAGE_INCOMPLETE
            );
        }

        if (isBlocked(input)) {
            return ReviewOnlyReadinessGateDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getInternalPushPreviewStatus(),
                    STATUS_BLOCKED,
                    withReason(input.getBlockingReasons(), REASON_INPUT_FAIL_CLOSED),
                    input.getRiskBlockers(),
                    blockedNextStep(input),
                    MESSAGE_BLOCKED
            );
        }

        if (hasRiskBlockers(input)) {
            return ReviewOnlyReadinessGateDTO.blocked(
                    input.getSymbol(),
                    input.getRequestId(),
                    input.getSourceContractId(),
                    input.getWatchlistPoolProof(),
                    input.getRequestedTimeframes(),
                    input.getInternalPushPreviewStatus(),
                    STATUS_BLOCKED,
                    withReason(
                            withReason(input.getBlockingReasons(), REASON_RISK_ACTION_GUARD_REQUIRED),
                            REASON_RECHECK_REQUIRED
                    ),
                    input.getRiskBlockers(),
                    STEP_BLOCKED_BY_RISK_ACTION_GUARD,
                    MESSAGE_BLOCKED
            );
        }

        if (isBlank(input.getSourceContractId())) {
            return incomplete(input, REASON_INCOMPLETE_SOURCE_CONTRACT, STEP_INCOMPLETE_SOURCE_TRACE);
        }

        if (isBlank(input.getWatchlistPoolProof())) {
            return incomplete(input, REASON_INCOMPLETE_WATCHLIST_PROOF, STEP_INCOMPLETE_SOURCE_TRACE);
        }

        if (input.getRequestedTimeframes().isEmpty()) {
            return incomplete(input, REASON_INCOMPLETE_TIMEFRAMES, STEP_INCOMPLETE_DATA_QUALITY);
        }

        return ReviewOnlyReadinessGateDTO.reviewOnly(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getInternalPushPreviewStatus(),
                STATUS_REVIEW_ONLY,
                withReason(
                        withReason(input.getBlockingReasons(), REASON_READINESS_GATE_REVIEW_ONLY),
                        REASON_RECHECK_REQUIRED
                ),
                input.getRiskBlockers(),
                STEP_READY_POINT_BOUNDARY,
                MESSAGE_REVIEW_ONLY
        );
    }

    private boolean isBlocked(ReviewOnlyInternalPushPreviewDTO input) {
        return input.isFailClosed() || input.isBlocked();
    }

    private boolean hasRiskBlockers(ReviewOnlyInternalPushPreviewDTO input) {
        return !input.getRiskBlockers().isEmpty();
    }

    private ReviewOnlyReadinessGateDTO incomplete(
            ReviewOnlyInternalPushPreviewDTO input,
            String reason,
            String nextStep
    ) {
        return ReviewOnlyReadinessGateDTO.incomplete(
                input.getSymbol(),
                input.getRequestId(),
                input.getSourceContractId(),
                input.getWatchlistPoolProof(),
                input.getRequestedTimeframes(),
                input.getInternalPushPreviewStatus(),
                STATUS_INCOMPLETE,
                withReason(input.getBlockingReasons(), reason),
                input.getRiskBlockers(),
                nextStep,
                MESSAGE_INCOMPLETE
        );
    }

    private String blockedNextStep(ReviewOnlyInternalPushPreviewDTO input) {
        if (isBlank(input.getAllowedNextStep())) {
            return STEP_BLOCKED_BY_INTERNAL_PUSH_PREVIEW;
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
