package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class OpportunityPushDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_BLOCKED = "BLOCKED";
    private static final String REASON_DISABLED = "DISABLED";

    private final String symbol;
    private final OpportunityPushStatusEnum pushStatus;
    private final String source;
    private final List<String> pushReasons;
    private final List<String> attentionReasons;
    private final List<String> riskGuardReasons;
    private final List<String> blockingReasons;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;
    private final boolean externalPushSent;
    private final boolean readinessUpgraded;
    private final boolean tradingActionCreated;
    private final boolean entryStopTpRrGenerated;

    private OpportunityPushDTO(
            String symbol,
            OpportunityPushStatusEnum pushStatus,
            String source,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.pushStatus = pushStatus == null ? OpportunityPushStatusEnum.INCOMPLETE : pushStatus;
        this.source = source;
        this.pushReasons = copy(pushReasons);
        this.attentionReasons = copy(attentionReasons);
        this.riskGuardReasons = copy(riskGuardReasons);
        this.blockingReasons = copy(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.externalPushSent = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static OpportunityPushDTO incomplete(String symbol, List<String> blockingReasons) {
        return new OpportunityPushDTO(
                symbol,
                OpportunityPushStatusEnum.INCOMPLETE,
                null,
                List.of(),
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static OpportunityPushDTO blocked(String symbol, List<String> blockingReasons) {
        return new OpportunityPushDTO(
                symbol,
                OpportunityPushStatusEnum.BLOCKED,
                null,
                List.of(),
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_BLOCKED)
        );
    }

    public static OpportunityPushDTO disabled(String symbol, List<String> blockingReasons) {
        return new OpportunityPushDTO(
                symbol,
                OpportunityPushStatusEnum.DISABLED,
                null,
                List.of(),
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_DISABLED)
        );
    }

    public static OpportunityPushDTO reviewOnly(
            String symbol,
            String source,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushDTO(
                symbol,
                OpportunityPushStatusEnum.REVIEW_ONLY,
                source,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                blockingReasons
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public OpportunityPushStatusEnum getPushStatus() {
        return pushStatus;
    }

    public String getSource() {
        return source;
    }

    public List<String> getPushReasons() {
        return copy(pushReasons);
    }

    public List<String> getAttentionReasons() {
        return copy(attentionReasons);
    }

    public List<String> getRiskGuardReasons() {
        return copy(riskGuardReasons);
    }

    public List<String> getBlockingReasons() {
        return copy(blockingReasons);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isExternalPushSent() {
        return externalPushSent;
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
