package org.example.trademodel.dto.marketread;

import java.util.ArrayList;
import java.util.List;

public class MarketReadReviewOnlyOutputDTO {

    private final String symbol;
    private final String requestId;
    private final String sourceContractId;
    private final String watchlistPoolProof;
    private final List<String> requestedTimeframes;
    private final MarketReadRequestGuardValidationStatusEnum guardValidationStatus;
    private final List<String> validationReasons;
    private final List<String> blockingReasons;
    private final List<String> riskBlockers;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean failClosed;
    private final String allowedNextStep;
    private final String reviewOnlyMessage;

    private MarketReadReviewOnlyOutputDTO(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            MarketReadRequestGuardValidationStatusEnum guardValidationStatus,
            List<String> validationReasons,
            List<String> blockingReasons,
            List<String> riskBlockers,
            boolean failClosed,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        this.symbol = symbol;
        this.requestId = requestId;
        this.sourceContractId = sourceContractId;
        this.watchlistPoolProof = watchlistPoolProof;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.guardValidationStatus = guardValidationStatus == null
                ? MarketReadRequestGuardValidationStatusEnum.BLOCKED
                : guardValidationStatus;
        this.validationReasons = copy(validationReasons);
        this.blockingReasons = copy(blockingReasons);
        this.riskBlockers = copy(riskBlockers);
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.failClosed = failClosed;
        this.allowedNextStep = allowedNextStep;
        this.reviewOnlyMessage = reviewOnlyMessage;
    }

    public static MarketReadReviewOnlyOutputDTO reviewOnly(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            MarketReadRequestGuardValidationStatusEnum guardValidationStatus,
            List<String> validationReasons,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        return new MarketReadReviewOnlyOutputDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                guardValidationStatus,
                validationReasons,
                blockingReasons,
                riskBlockers,
                false,
                allowedNextStep,
                reviewOnlyMessage
        );
    }

    public static MarketReadReviewOnlyOutputDTO blocked(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            MarketReadRequestGuardValidationStatusEnum guardValidationStatus,
            List<String> validationReasons,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        return new MarketReadReviewOnlyOutputDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                guardValidationStatus,
                validationReasons,
                blockingReasons,
                riskBlockers,
                true,
                allowedNextStep,
                reviewOnlyMessage
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSourceContractId() {
        return sourceContractId;
    }

    public String getWatchlistPoolProof() {
        return watchlistPoolProof;
    }

    public List<String> getRequestedTimeframes() {
        return copy(requestedTimeframes);
    }

    public MarketReadRequestGuardValidationStatusEnum getGuardValidationStatus() {
        return guardValidationStatus;
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

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public String getAllowedNextStep() {
        return allowedNextStep;
    }

    public String getReviewOnlyMessage() {
        return reviewOnlyMessage;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
