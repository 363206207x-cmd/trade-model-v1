package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class OpportunityPushAuditQueueResultDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_BLOCKED = "BLOCKED";
    private static final String REASON_DISABLED = "DISABLED";

    private final String symbol;
    private final OpportunityPushAuditQueueStatusEnum queueStatus;
    private final OpportunityPushAuditPersistenceStatusEnum persistenceStatus;
    private final OpportunityPushAuditEnvelopeStatusEnum envelopeStatus;
    private final OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus;
    private final String source;
    private final List<String> queueReasons;
    private final List<String> persistenceReasons;
    private final List<String> envelopeReasons;
    private final List<String> deliveryReasons;
    private final List<String> pushReasons;
    private final List<String> attentionReasons;
    private final List<String> riskGuardReasons;
    private final List<String> blockingReasons;
    private final boolean manualReviewRequired;
    private final boolean notTradeInstruction;
    private final boolean auditOnly;
    private final boolean queueCreated;
    private final boolean queued;
    private final boolean enqueueAttempted;
    private final boolean dequeueAttempted;
    private final boolean workerStarted;
    private final boolean persisted;
    private final boolean persistenceAttempted;
    private final boolean externalPushSent;
    private final boolean deliveryAttempted;
    private final boolean deliveryEnabled;
    private final boolean readinessUpgraded;
    private final boolean tradingActionCreated;
    private final boolean entryStopTpRrGenerated;

    private OpportunityPushAuditQueueResultDTO(
            String symbol,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> queueReasons,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        this.symbol = symbol;
        this.queueStatus = queueStatus == null ? OpportunityPushAuditQueueStatusEnum.INCOMPLETE : queueStatus;
        this.persistenceStatus = persistenceStatus;
        this.envelopeStatus = envelopeStatus;
        this.deliveryDecisionStatus = deliveryDecisionStatus;
        this.source = source;
        this.queueReasons = copy(queueReasons);
        this.persistenceReasons = copy(persistenceReasons);
        this.envelopeReasons = copy(envelopeReasons);
        this.deliveryReasons = copy(deliveryReasons);
        this.pushReasons = copy(pushReasons);
        this.attentionReasons = copy(attentionReasons);
        this.riskGuardReasons = copy(riskGuardReasons);
        this.blockingReasons = copy(blockingReasons);
        this.manualReviewRequired = true;
        this.notTradeInstruction = true;
        this.auditOnly = true;
        this.queueCreated = false;
        this.queued = false;
        this.enqueueAttempted = false;
        this.dequeueAttempted = false;
        this.workerStarted = false;
        this.persisted = false;
        this.persistenceAttempted = false;
        this.externalPushSent = false;
        this.deliveryAttempted = false;
        this.deliveryEnabled = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static OpportunityPushAuditQueueResultDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new OpportunityPushAuditQueueResultDTO(
                symbol,
                OpportunityPushAuditQueueStatusEnum.INCOMPLETE,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static OpportunityPushAuditQueueResultDTO incomplete(
            String symbol,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushAuditQueueResultDTO(
                symbol,
                OpportunityPushAuditQueueStatusEnum.INCOMPLETE,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                persistenceReasons,
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static OpportunityPushAuditQueueResultDTO blocked(
            String symbol,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushAuditQueueResultDTO(
                symbol,
                OpportunityPushAuditQueueStatusEnum.BLOCKED,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                persistenceReasons,
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_BLOCKED)
        );
    }

    public static OpportunityPushAuditQueueResultDTO disabled(
            String symbol,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushAuditQueueResultDTO(
                symbol,
                OpportunityPushAuditQueueStatusEnum.DISABLED,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                persistenceReasons,
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_DISABLED)
        );
    }

    public static OpportunityPushAuditQueueResultDTO noopReviewOnly(
            String symbol,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> queueReasons,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushAuditQueueResultDTO(
                symbol,
                OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                queueReasons,
                persistenceReasons,
                envelopeReasons,
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

    public OpportunityPushAuditQueueStatusEnum getQueueStatus() {
        return queueStatus;
    }

    public OpportunityPushAuditPersistenceStatusEnum getPersistenceStatus() {
        return persistenceStatus;
    }

    public OpportunityPushAuditEnvelopeStatusEnum getEnvelopeStatus() {
        return envelopeStatus;
    }

    public OpportunityPushDeliveryDecisionStatusEnum getDeliveryDecisionStatus() {
        return deliveryDecisionStatus;
    }

    public String getSource() {
        return source;
    }

    public List<String> getQueueReasons() {
        return copy(queueReasons);
    }

    public List<String> getPersistenceReasons() {
        return copy(persistenceReasons);
    }

    public List<String> getEnvelopeReasons() {
        return copy(envelopeReasons);
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

    public boolean isAuditOnly() {
        return auditOnly;
    }

    public boolean isQueueCreated() {
        return queueCreated;
    }

    public boolean isQueued() {
        return queued;
    }

    public boolean isEnqueueAttempted() {
        return enqueueAttempted;
    }

    public boolean isDequeueAttempted() {
        return dequeueAttempted;
    }

    public boolean isWorkerStarted() {
        return workerStarted;
    }

    public boolean isPersisted() {
        return persisted;
    }

    public boolean isPersistenceAttempted() {
        return persistenceAttempted;
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
