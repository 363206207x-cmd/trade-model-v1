package org.example.trademodel.dto.point;

import java.util.ArrayList;
import java.util.List;

public class ReviewOnlyPointProposalDTO {

    private final String symbol;
    private final String requestId;
    private final String sourceContractId;
    private final String watchlistPoolProof;
    private final List<String> requestedTimeframes;
    private final String readinessGateStatus;
    private final String pointBoundaryGateStatus;
    private final String pointProposalStatus;
    private final boolean pointProposalAllowed;
    private final String pointProposalBlockedReason;
    private final boolean sourceTraceRequired;
    private final boolean runtimeKlineContextRequired;
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
    private final String proposedEntry;
    private final String entryZone;
    private final String proposedStop;
    private final String stopZone;
    private final String proposedTakeProfit;
    private final String takeProfitPlan;
    private final String proposedRR;
    private final String riskReward;

    private ReviewOnlyPointProposalDTO(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            String pointProposalStatus,
            boolean pointProposalAllowed,
            String pointProposalBlockedReason,
            boolean failClosed,
            boolean blocked,
            boolean incomplete,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage,
            String proposedEntry,
            String entryZone,
            String proposedStop,
            String stopZone,
            String proposedTakeProfit,
            String takeProfitPlan,
            String proposedRR,
            String riskReward
    ) {
        this.symbol = symbol;
        this.requestId = requestId;
        this.sourceContractId = sourceContractId;
        this.watchlistPoolProof = watchlistPoolProof;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.readinessGateStatus = readinessGateStatus;
        this.pointBoundaryGateStatus = pointBoundaryGateStatus;
        this.pointProposalStatus = pointProposalStatus;
        this.pointProposalAllowed = pointProposalAllowed;
        this.pointProposalBlockedReason = pointProposalBlockedReason;
        this.sourceTraceRequired = true;
        this.runtimeKlineContextRequired = true;
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
        this.proposedEntry = proposedEntry;
        this.entryZone = entryZone;
        this.proposedStop = proposedStop;
        this.stopZone = stopZone;
        this.proposedTakeProfit = proposedTakeProfit;
        this.takeProfitPlan = takeProfitPlan;
        this.proposedRR = proposedRR;
        this.riskReward = riskReward;
    }

    public static ReviewOnlyPointProposalDTO reviewOnly(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            String pointProposalStatus,
            boolean pointProposalAllowed,
            String pointProposalBlockedReason,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage,
            String proposedEntry,
            String entryZone,
            String proposedStop,
            String stopZone,
            String proposedTakeProfit,
            String takeProfitPlan,
            String proposedRR,
            String riskReward
    ) {
        return new ReviewOnlyPointProposalDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                readinessGateStatus,
                pointBoundaryGateStatus,
                pointProposalStatus,
                pointProposalAllowed,
                pointProposalBlockedReason,
                false,
                false,
                false,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                reviewOnlyMessage,
                proposedEntry,
                entryZone,
                proposedStop,
                stopZone,
                proposedTakeProfit,
                takeProfitPlan,
                proposedRR,
                riskReward
        );
    }

    public static ReviewOnlyPointProposalDTO blocked(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            String pointProposalStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage,
            String pointProposalBlockedReason
    ) {
        return new ReviewOnlyPointProposalDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                readinessGateStatus,
                pointBoundaryGateStatus,
                pointProposalStatus,
                false,
                pointProposalBlockedReason,
                true,
                true,
                false,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                reviewOnlyMessage,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static ReviewOnlyPointProposalDTO incomplete(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String readinessGateStatus,
            String pointBoundaryGateStatus,
            String pointProposalStatus,
            boolean pointProposalAllowed,
            String pointProposalBlockedReason,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String reviewOnlyMessage
    ) {
        return new ReviewOnlyPointProposalDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                readinessGateStatus,
                pointBoundaryGateStatus,
                pointProposalStatus,
                pointProposalAllowed,
                pointProposalBlockedReason,
                true,
                true,
                true,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                reviewOnlyMessage,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
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

    public String getPointProposalStatus() {
        return pointProposalStatus;
    }

    public boolean isPointProposalAllowed() {
        return pointProposalAllowed;
    }

    public String getPointProposalBlockedReason() {
        return pointProposalBlockedReason;
    }

    public boolean isSourceTraceRequired() {
        return sourceTraceRequired;
    }

    public boolean isRuntimeKlineContextRequired() {
        return runtimeKlineContextRequired;
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

    public String getProposedEntry() {
        return proposedEntry;
    }

    public String getEntryZone() {
        return entryZone;
    }

    public String getProposedStop() {
        return proposedStop;
    }

    public String getStopZone() {
        return stopZone;
    }

    public String getProposedTakeProfit() {
        return proposedTakeProfit;
    }

    public String getTakeProfitPlan() {
        return takeProfitPlan;
    }

    public String getProposedRR() {
        return proposedRR;
    }

    public String getRiskReward() {
        return riskReward;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
