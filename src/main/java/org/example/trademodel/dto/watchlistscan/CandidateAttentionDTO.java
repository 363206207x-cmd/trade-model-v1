package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class CandidateAttentionDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_DISABLED = "DISABLED";

    private final String symbol;
    private final CandidateAttentionStatusEnum attentionStatus;
    private final String source;
    private final List<String> attentionReasons;
    private final List<String> blockingReasons;
    private final List<String> scoreReasons;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;
    private final boolean opportunityPushAllowed;
    private final boolean promoteToHomeAllowed;
    private final boolean readinessUpgraded;
    private final boolean tradingActionCreated;
    private final boolean entryStopTpRrGenerated;

    private CandidateAttentionDTO(
            String symbol,
            CandidateAttentionStatusEnum attentionStatus,
            String source,
            List<String> attentionReasons,
            List<String> blockingReasons,
            List<String> scoreReasons
    ) {
        this.symbol = symbol;
        this.attentionStatus = attentionStatus == null ? CandidateAttentionStatusEnum.INCOMPLETE : attentionStatus;
        this.source = source;
        this.attentionReasons = copy(attentionReasons);
        this.blockingReasons = copy(blockingReasons);
        this.scoreReasons = copy(scoreReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.opportunityPushAllowed = false;
        this.promoteToHomeAllowed = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static CandidateAttentionDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new CandidateAttentionDTO(
                symbol,
                CandidateAttentionStatusEnum.INCOMPLETE,
                null,
                List.of(),
                withReason(blockingReasons, REASON_INCOMPLETE),
                List.of()
        );
    }

    public static CandidateAttentionDTO disabled(
            String symbol,
            List<String> blockingReasons
    ) {
        return new CandidateAttentionDTO(
                symbol,
                CandidateAttentionStatusEnum.DISABLED,
                null,
                List.of(),
                withReason(blockingReasons, REASON_DISABLED),
                List.of()
        );
    }

    public static CandidateAttentionDTO reviewOnly(
            String symbol,
            String source,
            List<String> attentionReasons,
            List<String> scoreReasons,
            List<String> blockingReasons
    ) {
        return new CandidateAttentionDTO(
                symbol,
                CandidateAttentionStatusEnum.REVIEW_ONLY,
                source,
                attentionReasons,
                blockingReasons,
                scoreReasons
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public CandidateAttentionStatusEnum getAttentionStatus() {
        return attentionStatus;
    }

    public String getSource() {
        return source;
    }

    public List<String> getAttentionReasons() {
        return copy(attentionReasons);
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public List<String> getScoreReasons() {
        return copy(scoreReasons);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isOpportunityPushAllowed() {
        return opportunityPushAllowed;
    }

    public boolean isPromoteToHomeAllowed() {
        return promoteToHomeAllowed;
    }

    public boolean isReadinessUpgraded() {
        return readinessUpgraded;
    }

    public boolean isTradingActionCreated() {
        return tradingActionCreated;
    }

    public boolean isEntryStopTpRrGenerated() {
        return entryStopTpRrGenerated;
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
