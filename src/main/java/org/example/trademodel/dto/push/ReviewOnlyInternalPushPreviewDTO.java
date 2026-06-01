package org.example.trademodel.dto.push;

import java.util.ArrayList;
import java.util.List;

public class ReviewOnlyInternalPushPreviewDTO {

    private final String symbol;
    private final String requestId;
    private final String sourceContractId;
    private final String watchlistPoolProof;
    private final List<String> requestedTimeframes;
    private final String candidatePreviewGuardStatus;
    private final String internalPushPreviewStatus;
    private final boolean recheckRequired;
    private final boolean riskActionGuardRequired;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean failClosed;
    private final boolean blocked;
    private final List<String> blockingReasons;
    private final List<String> riskBlockers;
    private final String allowedNextStep;
    private final String reviewOnlyMessage;

    private ReviewOnlyInternalPushPreviewDTO(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String candidatePreviewGuardStatus,
            String internalPushPreviewStatus,
            boolean failClosed,
            boolean blocked,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        this.symbol = symbol;
        this.requestId = requestId;
        this.sourceContractId = sourceContractId;
        this.watchlistPoolProof = watchlistPoolProof;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.candidatePreviewGuardStatus = candidatePreviewGuardStatus;
        this.internalPushPreviewStatus = internalPushPreviewStatus;
        this.recheckRequired = true;
        this.riskActionGuardRequired = true;
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.failClosed = failClosed;
        this.blocked = blocked;
        this.blockingReasons = copy(blockingReasons);
        this.riskBlockers = copy(riskBlockers);
        this.allowedNextStep = allowedNextStep;
        this.reviewOnlyMessage = reviewOnlyMessage;
    }

    public static ReviewOnlyInternalPushPreviewDTO reviewOnly(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String candidatePreviewGuardStatus,
            String internalPushPreviewStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        return new ReviewOnlyInternalPushPreviewDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                candidatePreviewGuardStatus,
                internalPushPreviewStatus,
                false,
                false,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                reviewOnlyMessage
        );
    }

    public static ReviewOnlyInternalPushPreviewDTO blocked(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String candidatePreviewGuardStatus,
            String internalPushPreviewStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        return new ReviewOnlyInternalPushPreviewDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                candidatePreviewGuardStatus,
                internalPushPreviewStatus,
                true,
                true,
                blockingReasons,
                riskBlockers,
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

    public String getCandidatePreviewGuardStatus() {
        return candidatePreviewGuardStatus;
    }

    public String getInternalPushPreviewStatus() {
        return internalPushPreviewStatus;
    }

    public boolean isRecheckRequired() {
        return recheckRequired;
    }

    public boolean isRiskActionGuardRequired() {
        return riskActionGuardRequired;
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
