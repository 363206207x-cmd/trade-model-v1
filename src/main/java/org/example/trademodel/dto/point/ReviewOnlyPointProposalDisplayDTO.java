package org.example.trademodel.dto.point;

import java.util.ArrayList;
import java.util.List;

public class ReviewOnlyPointProposalDisplayDTO {

    private final String symbol;
    private final String requestId;
    private final String sourceContractId;
    private final String watchlistPoolProof;
    private final List<String> requestedTimeframes;
    private final String pointProposalStatus;
    private final String displayGateStatus;
    private final String incompleteReason;
    private final String blockedReason;
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
    private final String safeDisplayMessage;
    private final String entryDisplayText;
    private final String stopDisplayText;
    private final String takeProfitDisplayText;
    private final String riskRewardDisplayText;

    private ReviewOnlyPointProposalDisplayDTO(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String pointProposalStatus,
            String displayGateStatus,
            String incompleteReason,
            String blockedReason,
            boolean failClosed,
            boolean blocked,
            boolean incomplete,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String safeDisplayMessage,
            String entryDisplayText,
            String stopDisplayText,
            String takeProfitDisplayText,
            String riskRewardDisplayText
    ) {
        this.symbol = symbol;
        this.requestId = requestId;
        this.sourceContractId = sourceContractId;
        this.watchlistPoolProof = watchlistPoolProof;
        this.requestedTimeframes = copy(requestedTimeframes);
        this.pointProposalStatus = pointProposalStatus;
        this.displayGateStatus = displayGateStatus;
        this.incompleteReason = incompleteReason;
        this.blockedReason = blockedReason;
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
        this.safeDisplayMessage = safeDisplayMessage;
        this.entryDisplayText = entryDisplayText;
        this.stopDisplayText = stopDisplayText;
        this.takeProfitDisplayText = takeProfitDisplayText;
        this.riskRewardDisplayText = riskRewardDisplayText;
    }

    public static ReviewOnlyPointProposalDisplayDTO reviewOnly(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String pointProposalStatus,
            String displayGateStatus,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String safeDisplayMessage
    ) {
        return new ReviewOnlyPointProposalDisplayDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                pointProposalStatus,
                displayGateStatus,
                null,
                null,
                false,
                false,
                false,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                safeDisplayMessage,
                unavailableText(),
                unavailableText(),
                unavailableText(),
                unavailableText()
        );
    }

    public static ReviewOnlyPointProposalDisplayDTO blocked(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String pointProposalStatus,
            String displayGateStatus,
            String blockedReason,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String safeDisplayMessage
    ) {
        return new ReviewOnlyPointProposalDisplayDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                pointProposalStatus,
                displayGateStatus,
                null,
                blockedReason,
                true,
                true,
                false,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                safeDisplayMessage,
                unavailableText(),
                unavailableText(),
                unavailableText(),
                unavailableText()
        );
    }

    public static ReviewOnlyPointProposalDisplayDTO incomplete(
            String symbol,
            String requestId,
            String sourceContractId,
            String watchlistPoolProof,
            List<String> requestedTimeframes,
            String pointProposalStatus,
            String displayGateStatus,
            String incompleteReason,
            List<String> blockingReasons,
            List<String> riskBlockers,
            String allowedNextStep,
            String safeDisplayMessage
    ) {
        return new ReviewOnlyPointProposalDisplayDTO(
                symbol,
                requestId,
                sourceContractId,
                watchlistPoolProof,
                requestedTimeframes,
                pointProposalStatus,
                displayGateStatus,
                incompleteReason,
                null,
                true,
                true,
                true,
                blockingReasons,
                riskBlockers,
                allowedNextStep,
                safeDisplayMessage,
                unavailableText(),
                unavailableText(),
                unavailableText(),
                unavailableText()
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

    public String getPointProposalStatus() {
        return pointProposalStatus;
    }

    public String getDisplayGateStatus() {
        return displayGateStatus;
    }

    public String getIncompleteReason() {
        return incompleteReason;
    }

    public String getBlockedReason() {
        return blockedReason;
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

    public String getSafeDisplayMessage() {
        return safeDisplayMessage;
    }

    public String getEntryDisplayText() {
        return entryDisplayText;
    }

    public String getStopDisplayText() {
        return stopDisplayText;
    }

    public String getTakeProfitDisplayText() {
        return takeProfitDisplayText;
    }

    public String getRiskRewardDisplayText() {
        return riskRewardDisplayText;
    }

    private static String unavailableText() {
        return "UNAVAILABLE_REVIEW_ONLY_PLACEHOLDER";
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
