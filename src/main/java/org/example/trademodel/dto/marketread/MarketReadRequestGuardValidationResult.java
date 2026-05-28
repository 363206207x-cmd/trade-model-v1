package org.example.trademodel.dto.marketread;

import java.util.ArrayList;
import java.util.List;

public class MarketReadRequestGuardValidationResult {

    private static final String REASON_FAIL_CLOSED = "MARKET_READ_REQUEST_GUARD_FAIL_CLOSED";
    private static final String REASON_REVIEW_ONLY = "MARKET_READ_REQUEST_GUARD_REVIEW_ONLY";

    private final MarketReadRequestGuardValidationStatusEnum status;
    private final List<String> validationReasons;
    private final List<String> blockingReasons;
    private final List<String> riskBlockers;
    private final boolean reviewOnly;
    private final boolean failClosed;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;

    private MarketReadRequestGuardValidationResult(
            MarketReadRequestGuardValidationStatusEnum status,
            List<String> validationReasons,
            List<String> blockingReasons,
            List<String> riskBlockers
    ) {
        this.status = status == null ? MarketReadRequestGuardValidationStatusEnum.BLOCKED : status;
        this.validationReasons = copy(validationReasons);
        this.blockingReasons = copy(blockingReasons);
        this.riskBlockers = copy(riskBlockers);
        this.reviewOnly = true;
        this.failClosed = true;
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
    }

    public static MarketReadRequestGuardValidationResult blocked(
            List<String> validationReasons,
            List<String> blockingReasons,
            List<String> riskBlockers
    ) {
        return new MarketReadRequestGuardValidationResult(
                MarketReadRequestGuardValidationStatusEnum.BLOCKED,
                withReason(validationReasons, REASON_FAIL_CLOSED),
                blockingReasons,
                riskBlockers
        );
    }

    public static MarketReadRequestGuardValidationResult reviewOnly(
            List<String> validationReasons,
            List<String> blockingReasons,
            List<String> riskBlockers
    ) {
        return new MarketReadRequestGuardValidationResult(
                MarketReadRequestGuardValidationStatusEnum.REVIEW_ONLY,
                withReason(validationReasons, REASON_REVIEW_ONLY),
                blockingReasons,
                riskBlockers
        );
    }

    public MarketReadRequestGuardValidationStatusEnum getStatus() {
        return status;
    }

    public List<String> getValidationReasons() {
        return copy(validationReasons);
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public List<String> getRiskBlockers() {
        return copy(riskBlockers);
    }

    public boolean isReviewOnly() {
        return reviewOnly;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isBlocked() {
        return MarketReadRequestGuardValidationStatusEnum.BLOCKED.equals(status);
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = copy(reasons);
        if (reason != null && !resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
