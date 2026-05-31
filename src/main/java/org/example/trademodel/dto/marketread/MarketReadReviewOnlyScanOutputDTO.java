package org.example.trademodel.dto.marketread;

import java.util.ArrayList;
import java.util.List;

public class MarketReadReviewOnlyScanOutputDTO {

    private final String symbol;
    private final String requestId;
    private final String sourceContractId;
    private final String watchlistPoolProof;
    private final List<String> requestedTimeframes;
    private final MarketReadRequestGuardValidationStatusEnum guardValidationStatus;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean failClosed;
    private final boolean blocked;
    private final List<String> blockingReasons;
    private final List<String> riskBlockers;
    private final String scanOutputStatus;
    private final String allowedNextStep;
    private final String reviewOnlyMessage;

    private MarketReadReviewOnlyScanOutputDTO(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            MarketReadRequestGuardValidationStatusEnum guardValidationStatus,
            boolean failClosed,
            boolean blocked,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String scanOutputStatus,
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
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.failClosed = failClosed;
        this.blocked = blocked;
        this.blockingReasons = copy(blockingReasons);
        this.riskBlockers = copy(riskBlockers);
        this.scanOutputStatus = scanOutputStatus;
        this.allowedNextStep = allowedNextStep;
        this.reviewOnlyMessage = reviewOnlyMessage;
    }

    public static MarketReadReviewOnlyScanOutputDTO reviewOnly(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            MarketReadRequestGuardValidationStatusEnum guardValidationStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String scanOutputStatus,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        return new MarketReadReviewOnlyScanOutputDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                guardValidationStatus,
                false,
                false,
                blockingReasons,
                riskBlockers,
                scanOutputStatus,
                allowedNextStep,
                reviewOnlyMessage
        );
    }

    public static MarketReadReviewOnlyScanOutputDTO blocked(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            MarketReadRequestGuardValidationStatusEnum guardValidationStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String scanOutputStatus,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        return new MarketReadReviewOnlyScanOutputDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                guardValidationStatus,
                true,
                true,
                blockingReasons,
                riskBlockers,
                scanOutputStatus,
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

    public boolean isBlocked() {
        return blocked;
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public List<String> getRiskBlockers() {
        return copy(riskBlockers);
    }

    public String getScanOutputStatus() {
        return scanOutputStatus;
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
