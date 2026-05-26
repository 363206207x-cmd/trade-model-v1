package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class OpportunityPushDeliveryDecisionDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_BLOCKED = "BLOCKED";
    private static final String REASON_DISABLED = "DISABLED";

    private final String symbol;
    private final OpportunityPushDeliveryDecisionStatusEnum decisionStatus;
    private final String source;
    private final List<String> deliveryReasons;
    private final List<String> pushReasons;
    private final List<String> attentionReasons;
    private final List<String> riskGuardReasons;
    private final List<String> blockingReasons;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;
    private final boolean externalPushSent;
    private final boolean deliveryAttempted;
    private final boolean deliveryEnabled;
    private final boolean readinessUpgraded;
    private final boolean tradingActionCreated;
    private final boolean entryStopTpRrGenerated;

    private OpportunityPushDeliveryDecisionDTO(
            String symbol,
            OpportunityPushDeliveryDecisionStatusEnum decisionStatus,
            String source,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.decisionStatus = decisionStatus == null
                ? OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE
                : decisionStatus;
        this.source = source;
        this.deliveryReasons = copy(deliveryReasons);
        this.pushReasons = copy(pushReasons);
        this.attentionReasons = copy(attentionReasons);
        this.riskGuardReasons = copy(riskGuardReasons);
        this.blockingReasons = copy(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.externalPushSent = false;
        this.deliveryAttempted = false;
        this.deliveryEnabled = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static OpportunityPushDeliveryDecisionDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new OpportunityPushDeliveryDecisionDTO(
                symbol,
                OpportunityPushDeliveryDecisionStatusEnum.INCOMPLETE,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static OpportunityPushDeliveryDecisionDTO blocked(
            String symbol,
            String source,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushDeliveryDecisionDTO(
                symbol,
                OpportunityPushDeliveryDecisionStatusEnum.BLOCKED,
                source,
                List.of(),
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_BLOCKED)
        );
    }

    public static OpportunityPushDeliveryDecisionDTO disabled(
            String symbol,
            String source,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushDeliveryDecisionDTO(
                symbol,
                OpportunityPushDeliveryDecisionStatusEnum.DISABLED,
                source,
                List.of(),
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_DISABLED)
        );
    }

    public static OpportunityPushDeliveryDecisionDTO reviewOnly(
            String symbol,
            String source,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushDeliveryDecisionDTO(
                symbol,
                OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY,
                source,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                blockingReasons
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public OpportunityPushDeliveryDecisionStatusEnum getDecisionStatus() {
        return decisionStatus;
    }

    public String getSource() {
        return source;
    }

    public List<String> getDeliveryReasons() {
        return copy(deliveryReasons);
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

    public boolean isDeliveryAttempted() {
        return deliveryAttempted;
    }

    public boolean isDeliveryEnabled() {
        return deliveryEnabled;
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
