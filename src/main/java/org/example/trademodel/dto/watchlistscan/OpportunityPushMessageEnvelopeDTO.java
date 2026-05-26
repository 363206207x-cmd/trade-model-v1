package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class OpportunityPushMessageEnvelopeDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_BLOCKED = "BLOCKED";
    private static final String REASON_DISABLED = "DISABLED";

    private final String symbol;
    private final OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus;
    private final OpportunityPushDeliveryPipelineStatusEnum pipelineStatus;
    private final OpportunityPushAuditQueueStatusEnum queueStatus;
    private final OpportunityPushAuditPersistenceStatusEnum persistenceStatus;
    private final OpportunityPushAuditEnvelopeStatusEnum envelopeStatus;
    private final OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus;
    private final String source;
    private final List<String> messageEnvelopeReasons;
    private final List<String> pipelineReasons;
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
    private final boolean messageEnvelopeCreated;
    private final boolean messageRenderable;
    private final boolean messageRendered;
    private final boolean messageSendable;
    private final boolean messageSent;
    private final boolean providerSelected;
    private final boolean externalPushSent;
    private final boolean deliveryAttempted;
    private final boolean deliveryEnabled;
    private final boolean deliveryPipelineEnabled;
    private final boolean pipelineStarted;
    private final boolean queueCreated;
    private final boolean queued;
    private final boolean enqueueAttempted;
    private final boolean dequeueAttempted;
    private final boolean workerStarted;
    private final boolean persisted;
    private final boolean persistenceAttempted;
    private final boolean readinessUpgraded;
    private final boolean tradingActionCreated;
    private final boolean entryStopTpRrGenerated;

    private OpportunityPushMessageEnvelopeDTO(
            String symbol,
            OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> messageEnvelopeReasons,
            List<String> pipelineReasons,
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
        this.messageEnvelopeStatus = messageEnvelopeStatus == null
                ? OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE
                : messageEnvelopeStatus;
        this.pipelineStatus = pipelineStatus;
        this.queueStatus = queueStatus;
        this.persistenceStatus = persistenceStatus;
        this.envelopeStatus = envelopeStatus;
        this.deliveryDecisionStatus = deliveryDecisionStatus;
        this.source = source;
        this.messageEnvelopeReasons = copy(messageEnvelopeReasons);
        this.pipelineReasons = copy(pipelineReasons);
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
        this.messageEnvelopeCreated = false;
        this.messageRenderable = false;
        this.messageRendered = false;
        this.messageSendable = false;
        this.messageSent = false;
        this.providerSelected = false;
        this.externalPushSent = false;
        this.deliveryAttempted = false;
        this.deliveryEnabled = false;
        this.deliveryPipelineEnabled = false;
        this.pipelineStarted = false;
        this.queueCreated = false;
        this.queued = false;
        this.enqueueAttempted = false;
        this.dequeueAttempted = false;
        this.workerStarted = false;
        this.persisted = false;
        this.persistenceAttempted = false;
        this.readinessUpgraded = false;
        this.tradingActionCreated = false;
        this.entryStopTpRrGenerated = false;
    }

    public static OpportunityPushMessageEnvelopeDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new OpportunityPushMessageEnvelopeDTO(
                symbol,
                OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE,
                null,
                null,
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
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static OpportunityPushMessageEnvelopeDTO incomplete(
            String symbol,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> pipelineReasons,
            List<String> queueReasons,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushMessageEnvelopeDTO(
                symbol,
                OpportunityPushMessageEnvelopeStatusEnum.INCOMPLETE,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                pipelineReasons,
                queueReasons,
                persistenceReasons,
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static OpportunityPushMessageEnvelopeDTO blocked(
            String symbol,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> pipelineReasons,
            List<String> queueReasons,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushMessageEnvelopeDTO(
                symbol,
                OpportunityPushMessageEnvelopeStatusEnum.BLOCKED,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                pipelineReasons,
                queueReasons,
                persistenceReasons,
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_BLOCKED)
        );
    }

    public static OpportunityPushMessageEnvelopeDTO disabled(
            String symbol,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> pipelineReasons,
            List<String> queueReasons,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushMessageEnvelopeDTO(
                symbol,
                OpportunityPushMessageEnvelopeStatusEnum.DISABLED,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                pipelineReasons,
                queueReasons,
                persistenceReasons,
                envelopeReasons,
                deliveryReasons,
                pushReasons,
                attentionReasons,
                riskGuardReasons,
                withReason(blockingReasons, REASON_DISABLED)
        );
    }

    public static OpportunityPushMessageEnvelopeDTO disabledNoop(
            String symbol,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> messageEnvelopeReasons,
            List<String> pipelineReasons,
            List<String> queueReasons,
            List<String> persistenceReasons,
            List<String> envelopeReasons,
            List<String> deliveryReasons,
            List<String> pushReasons,
            List<String> attentionReasons,
            List<String> riskGuardReasons,
            List<String> blockingReasons
    ) {
        return new OpportunityPushMessageEnvelopeDTO(
                symbol,
                OpportunityPushMessageEnvelopeStatusEnum.DISABLED_NOOP,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                messageEnvelopeReasons,
                pipelineReasons,
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

    public OpportunityPushMessageEnvelopeStatusEnum getMessageEnvelopeStatus() {
        return messageEnvelopeStatus;
    }

    public OpportunityPushDeliveryPipelineStatusEnum getPipelineStatus() {
        return pipelineStatus;
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

    public List<String> getMessageEnvelopeReasons() {
        return copy(messageEnvelopeReasons);
    }

    public List<String> getPipelineReasons() {
        return copy(pipelineReasons);
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

    public boolean isMessageEnvelopeCreated() {
        return messageEnvelopeCreated;
    }

    public boolean isMessageRenderable() {
        return messageRenderable;
    }

    public boolean isMessageRendered() {
        return messageRendered;
    }

    public boolean isMessageSendable() {
        return messageSendable;
    }

    public boolean isMessageSent() {
        return messageSent;
    }

    public boolean isProviderSelected() {
        return providerSelected;
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

    public boolean isDeliveryPipelineEnabled() {
        return deliveryPipelineEnabled;
    }

    public boolean isPipelineStarted() {
        return pipelineStarted;
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
