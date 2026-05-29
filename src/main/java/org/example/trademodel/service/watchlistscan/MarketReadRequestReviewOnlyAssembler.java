package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.marketread.MarketReadRequestDTO;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationResult;
import org.example.trademodel.dto.marketread.MarketReadRequestGuardValidationStatusEnum;
import org.example.trademodel.dto.marketread.MarketReadReviewOnlyOutputDTO;

public class MarketReadRequestReviewOnlyAssembler {

    private static final String REASON_REQUEST_MISSING = "MARKET_READ_REQUEST_MISSING";
    private static final String REASON_GUARD_RESULT_MISSING = "MARKET_READ_REQUEST_GUARD_RESULT_MISSING";
    private static final String REASON_GUARD_BLOCKED = "MARKET_READ_REQUEST_GUARD_BLOCKED";
    private static final String REASON_REVIEW_ONLY_OUTPUT = "MARKET_READ_REQUEST_REVIEW_ONLY_OUTPUT";
    private static final String STEP_WAIT_FOR_REVIEW = "WAIT_FOR_REVIEW";
    private static final String STEP_FIX_INPUT_CONTRACT = "FIX_INPUT_CONTRACT";
    private static final String STEP_WAIT_FOR_MARKET_READ_AUTHORIZATION = "WAIT_FOR_MARKET_READ_AUTHORIZATION";
    private static final String STEP_BLOCKED_BY_GUARD = "BLOCKED_BY_GUARD";
    private static final String MESSAGE_REVIEW_ONLY_READY =
            "Market read request validation is readable for manual review only.";
    private static final String MESSAGE_BLOCKED =
            "Market read request remains blocked and fail-closed before any market read.";

    public MarketReadReviewOnlyOutputDTO assemble(
            MarketReadRequestDTO request,
            MarketReadRequestGuardValidationResult guardResult
    ) {
        if (request == null) {
            return MarketReadReviewOnlyOutputDTO.blocked(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    statusOf(guardResult),
                    withReason(validationReasonsOf(guardResult), REASON_REQUEST_MISSING),
                    withReason(blockingReasonsOf(guardResult), REASON_REQUEST_MISSING),
                    riskBlockersOf(guardResult),
                    STEP_FIX_INPUT_CONTRACT,
                    MESSAGE_BLOCKED
            );
        }

        if (guardResult == null) {
            return MarketReadReviewOnlyOutputDTO.blocked(
                    request.getSymbol(),
                    request.getRequestId(),
                    request.getSourceContractId(),
                    request.getWatchlistPoolProof(),
                    request.getRequestedTimeframes(),
                    MarketReadRequestGuardValidationStatusEnum.BLOCKED,
                    List.of(REASON_GUARD_RESULT_MISSING),
                    withReason(request.getBlockingReasons(), REASON_GUARD_RESULT_MISSING),
                    request.getRiskBlockers(),
                    STEP_BLOCKED_BY_GUARD,
                    MESSAGE_BLOCKED
            );
        }

        List<String> blockingReasons = merge(request.getBlockingReasons(), guardResult.getBlockingReasons());
        List<String> riskBlockers = merge(request.getRiskBlockers(), guardResult.getRiskBlockers());
        if (guardResult.isBlocked()) {
            return MarketReadReviewOnlyOutputDTO.blocked(
                    request.getSymbol(),
                    request.getRequestId(),
                    request.getSourceContractId(),
                    request.getWatchlistPoolProof(),
                    request.getRequestedTimeframes(),
                    guardResult.getStatus(),
                    guardResult.getValidationReasons(),
                    withReason(blockingReasons, REASON_GUARD_BLOCKED),
                    riskBlockers,
                    allowedBlockedStep(guardResult.getValidationReasons()),
                    MESSAGE_BLOCKED
            );
        }

        return MarketReadReviewOnlyOutputDTO.reviewOnly(
                request.getSymbol(),
                request.getRequestId(),
                request.getSourceContractId(),
                request.getWatchlistPoolProof(),
                request.getRequestedTimeframes(),
                guardResult.getStatus(),
                guardResult.getValidationReasons(),
                withReason(blockingReasons, REASON_REVIEW_ONLY_OUTPUT),
                riskBlockers,
                allowedReviewStep(riskBlockers),
                MESSAGE_REVIEW_ONLY_READY
        );
    }

    private String allowedBlockedStep(List<String> validationReasons) {
        List<String> reasons = copy(validationReasons);
        if (reasons.contains("MISSING_SOURCE_CONTRACT_ID")
                || reasons.contains("MISSING_WATCHLIST_POOL_PROOF")
                || reasons.contains("MISSING_REQUESTED_TIMEFRAMES")
                || reasons.contains("MISSING_SCAN_TIMESTAMP")) {
            return STEP_FIX_INPUT_CONTRACT;
        }
        return STEP_BLOCKED_BY_GUARD;
    }

    private String allowedReviewStep(List<String> riskBlockers) {
        if (riskBlockers == null || riskBlockers.isEmpty()) {
            return STEP_WAIT_FOR_REVIEW;
        }
        return STEP_WAIT_FOR_MARKET_READ_AUTHORIZATION;
    }

    private MarketReadRequestGuardValidationStatusEnum statusOf(MarketReadRequestGuardValidationResult guardResult) {
        return guardResult == null
                ? MarketReadRequestGuardValidationStatusEnum.BLOCKED
                : guardResult.getStatus();
    }

    private List<String> validationReasonsOf(MarketReadRequestGuardValidationResult guardResult) {
        return guardResult == null ? List.of() : guardResult.getValidationReasons();
    }

    private List<String> blockingReasonsOf(MarketReadRequestGuardValidationResult guardResult) {
        return guardResult == null ? List.of() : guardResult.getBlockingReasons();
    }

    private List<String> riskBlockersOf(MarketReadRequestGuardValidationResult guardResult) {
        return guardResult == null ? List.of() : guardResult.getRiskBlockers();
    }

    private List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = copy(reasons);
        if (!resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }

    private List<String> merge(List<String> first, List<String> second) {
        List<String> merged = copy(first);
        for (String value : copy(second)) {
            if (!merged.contains(value)) {
                merged.add(value);
            }
        }
        return merged;
    }

    private <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
