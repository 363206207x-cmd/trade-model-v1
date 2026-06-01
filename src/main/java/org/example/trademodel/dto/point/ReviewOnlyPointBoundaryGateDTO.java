package org.example.trademodel.dto.point;

import java.util.ArrayList;
import java.util.List;

public class ReviewOnlyPointBoundaryGateDTO {

    private final String symbol;
    private final String requestId;
    private final String sourceContractId;
    private final String watchlistPoolProof;
    private final List<String> requestedTimeframes;
    private final String readinessGateStatus;
    private final String pointBoundaryGateStatus;
    private final boolean recheckRequired;
    private final boolean riskActionGuardRequired;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean failClosed;
    private final boolean blocked;
    private final boolean incomplete;
    private final List<String> blockingReasons;
    private final List<String> riskBlockers;
    private final String allowedNextStep;
    private final String reviewOnlyMessage;
    private final boolean pointProposalAllowed;
    private final String pointProposalBlockedReason;

    private ReviewOnlyPointBoundaryGateDTO(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            boolean failClosed,
            boolean blocked,
            boolean incomplete,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage,
            boolean pointProposalAllowed,
            String pointProposalBlockedReason
    ) {
        this.symbol = symbol;
        this.requestId = requestId;
        this.sourceContractId = sourceContractId;
        this.watchlistPoolProof = watchlistPoolProof;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.readinessGateStatus = readinessGateStatus;
        this.pointBoundaryGateStatus = pointBoundaryGateStatus;
        this.recheckRequired = true;
        this.riskActionGuardRequired = true;
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.failClosed = failClosed;
        this.blocked = blocked;
        this.incomplete = incomplete;
        this.blockingReasons = copy(blockingReasons);
        this.riskBlockers = copy(riskBlockers);
        this.allowedNextStep = allowedNextStep;
        this.reviewOnlyMessage = reviewOnlyMessage;
        this.pointProposalAllowed = pointProposalAllowed;
        this.pointProposalBlockedReason = pointProposalBlockedReason;
    }

    public static ReviewOnlyPointBoundaryGateDTO reviewOnly(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage,
            boolean pointProposalAllowed,
            String pointProposalBlockedReason
    ) {
        return new ReviewOnlyPointBoundaryGateDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                readinessGateStatus,
                pointBoundaryGateStatus,
                false,
                false,
                false,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                reviewOnlyMessage,
                pointProposalAllowed,
                pointProposalBlockedReason
        );
    }

    public static ReviewOnlyPointBoundaryGateDTO blocked(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage,
            String pointProposalBlockedReason
    ) {
        return new ReviewOnlyPointBoundaryGateDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                readinessGateStatus,
                pointBoundaryGateStatus,
                true,
                true,
                false,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                reviewOnlyMessage,
                false,
                pointProposalBlockedReason
        );
    }

    public static ReviewOnlyPointBoundaryGateDTO incomplete(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage,
            String pointProposalBlockedReason
    ) {
        return new ReviewOnlyPointBoundaryGateDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                readinessGateStatus,
                pointBoundaryGateStatus,
                true,
                true,
                true,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                reviewOnlyMessage,
                false,
                pointProposalBlockedReason
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

    public String getReadinessGateStatus() {
        return readinessGateStatus;
    }

    public String getPointBoundaryGateStatus() {
        return pointBoundaryGateStatus;
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

    public boolean isIncomplete() {
        return incomplete;
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

    public boolean isPointProposalAllowed() {
        return pointProposalAllowed;
    }

    public String getPointProposalBlockedReason() {
        return pointProposalBlockedReason;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
