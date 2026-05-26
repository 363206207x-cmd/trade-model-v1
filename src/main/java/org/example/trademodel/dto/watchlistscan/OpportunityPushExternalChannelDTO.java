package org.example.trademodel.dto.watchlistscan;

import java.util.ArrayList;
import java.util.List;

public class OpportunityPushExternalChannelDTO {

    private static final String REASON_INCOMPLETE = "INCOMPLETE";
    private static final String REASON_BLOCKED = "BLOCKED";
    private static final String REASON_DISABLED = "DISABLED";

    private final String symbol;
    private final OpportunityPushExternalChannelStatusEnum externalChannelStatus;
    private final OpportunityPushProviderChannelStatusEnum providerChannelStatus;
    private final OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus;
    private final OpportunityPushDeliveryPipelineStatusEnum pipelineStatus;
    private final OpportunityPushAuditQueueStatusEnum queueStatus;
    private final OpportunityPushAuditPersistenceStatusEnum persistenceStatus;
    private final OpportunityPushAuditEnvelopeStatusEnum envelopeStatus;
    private final OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus;
    private final String source;
    private final List<String> externalChannelReasons;
    private final List<String> providerChannelReasons;
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
    private final boolean externalChannelEnabled;
    private final boolean externalChannelSelected;
    private final boolean externalChannelConfigured;
    private final boolean externalChannelCredentialRequired;
    private final boolean externalChannelCredentialUsed;
    private final boolean liveExternalCallAttempted;
    private final boolean messageRendered;
    private final boolean messageSent;
    private final boolean externalPushSent;
    private final boolean deliveryAttempted;
    private final boolean deliveryEnabled;
    private final boolean providerChannelEnabled;
    private final boolean providerSelected;
    private final boolean providerCredentialRequired;
    private final boolean providerCredentialUsed;
    private final boolean liveProviderCallAttempted;
    private final boolean messageEnvelopeCreated;
    private final boolean messageRenderable;
    private final boolean messageSendable;
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

    private OpportunityPushExternalChannelDTO(
            String symbol,
            OpportunityPushExternalChannelStatusEnum externalChannelStatus,
            OpportunityPushProviderChannelStatusEnum providerChannelStatus,
            OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> externalChannelReasons,
            List<String> providerChannelReasons,
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
        this.externalChannelStatus = externalChannelStatus == null
                ? OpportunityPushExternalChannelStatusEnum.INCOMPLETE
                : externalChannelStatus;
        this.providerChannelStatus = providerChannelStatus;
        this.messageEnvelopeStatus = messageEnvelopeStatus;
        this.pipelineStatus = pipelineStatus;
        this.queueStatus = queueStatus;
        this.persistenceStatus = persistenceStatus;
        this.envelopeStatus = envelopeStatus;
        this.deliveryDecisionStatus = deliveryDecisionStatus;
        this.source = source;
        this.externalChannelReasons = copy(externalChannelReasons);
        this.providerChannelReasons = copy(providerChannelReasons);
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
        this.externalChannelEnabled = false;
        this.externalChannelSelected = false;
        this.externalChannelConfigured = false;
        this.externalChannelCredentialRequired = false;
        this.externalChannelCredentialUsed = false;
        this.liveExternalCallAttempted = false;
        this.messageRendered = false;
        this.messageSent = false;
        this.externalPushSent = false;
        this.deliveryAttempted = false;
        this.deliveryEnabled = false;
        this.providerChannelEnabled = false;
        this.providerSelected = false;
        this.providerCredentialRequired = false;
        this.providerCredentialUsed = false;
        this.liveProviderCallAttempted = false;
        this.messageEnvelopeCreated = false;
        this.messageRenderable = false;
        this.messageSendable = false;
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

    public static OpportunityPushExternalChannelDTO incomplete(
            String symbol,
            List<String> blockingReasons
    ) {
        return new OpportunityPushExternalChannelDTO(
                symbol,
                OpportunityPushExternalChannelStatusEnum.INCOMPLETE,
                null,
                null,
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
                List.of(),
                List.of(),
                withReason(blockingReasons, REASON_INCOMPLETE)
        );
    }

    public static OpportunityPushExternalChannelDTO incomplete(
            String symbol,
            OpportunityPushProviderChannelStatusEnum providerChannelStatus,
            OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> providerChannelReasons,
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
        return new OpportunityPushExternalChannelDTO(
                symbol,
                OpportunityPushExternalChannelStatusEnum.INCOMPLETE,
                providerChannelStatus,
                messageEnvelopeStatus,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                providerChannelReasons,
                messageEnvelopeReasons,
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

    public static OpportunityPushExternalChannelDTO blocked(
            String symbol,
            OpportunityPushProviderChannelStatusEnum providerChannelStatus,
            OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> providerChannelReasons,
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
        return new OpportunityPushExternalChannelDTO(
                symbol,
                OpportunityPushExternalChannelStatusEnum.BLOCKED,
                providerChannelStatus,
                messageEnvelopeStatus,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                providerChannelReasons,
                messageEnvelopeReasons,
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

    public static OpportunityPushExternalChannelDTO disabled(
            String symbol,
            OpportunityPushProviderChannelStatusEnum providerChannelStatus,
            OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> providerChannelReasons,
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
        return new OpportunityPushExternalChannelDTO(
                symbol,
                OpportunityPushExternalChannelStatusEnum.DISABLED,
                providerChannelStatus,
                messageEnvelopeStatus,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                List.of(),
                providerChannelReasons,
                messageEnvelopeReasons,
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

    public static OpportunityPushExternalChannelDTO disabledNoop(
            String symbol,
            OpportunityPushProviderChannelStatusEnum providerChannelStatus,
            OpportunityPushMessageEnvelopeStatusEnum messageEnvelopeStatus,
            OpportunityPushDeliveryPipelineStatusEnum pipelineStatus,
            OpportunityPushAuditQueueStatusEnum queueStatus,
            OpportunityPushAuditPersistenceStatusEnum persistenceStatus,
            OpportunityPushAuditEnvelopeStatusEnum envelopeStatus,
            OpportunityPushDeliveryDecisionStatusEnum deliveryDecisionStatus,
            String source,
            List<String> externalChannelReasons,
            List<String> providerChannelReasons,
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
        return new OpportunityPushExternalChannelDTO(
                symbol,
                OpportunityPushExternalChannelStatusEnum.DISABLED_NOOP,
                providerChannelStatus,
                messageEnvelopeStatus,
                pipelineStatus,
                queueStatus,
                persistenceStatus,
                envelopeStatus,
                deliveryDecisionStatus,
                source,
                externalChannelReasons,
                providerChannelReasons,
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

    public OpportunityPushExternalChannelStatusEnum getExternalChannelStatus() {
        return externalChannelStatus;
    }

    public OpportunityPushProviderChannelStatusEnum getProviderChannelStatus() {
        return providerChannelStatus;
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

    public List<String> getExternalChannelReasons() {
        return copy(externalChannelReasons);
    }

    public List<String> getProviderChannelReasons() {
        return copy(providerChannelReasons);
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

    public boolean isExternalChannelEnabled() {
        return externalChannelEnabled;
    }

    public boolean isExternalChannelSelected() {
        return externalChannelSelected;
    }

    public boolean isExternalChannelConfigured() {
        return externalChannelConfigured;
    }

    public boolean isExternalChannelCredentialRequired() {
        return externalChannelCredentialRequired;
    }

    public boolean isExternalChannelCredentialUsed() {
        return externalChannelCredentialUsed;
    }

    public boolean isLiveExternalCallAttempted() {
        return liveExternalCallAttempted;
    }

    public boolean isMessageRendered() {
        return messageRendered;
    }

    public boolean isMessageSent() {
        return messageSent;
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

    public boolean isProviderChannelEnabled() {
        return providerChannelEnabled;
    }

    public boolean isProviderSelected() {
        return providerSelected;
    }

    public boolean isProviderCredentialRequired() {
        return providerCredentialRequired;
    }

    public boolean isProviderCredentialUsed() {
        return providerCredentialUsed;
    }

    public boolean isLiveProviderCallAttempted() {
        return liveProviderCallAttempted;
    }

    public boolean isMessageEnvelopeCreated() {
        return messageEnvelopeCreated;
    }

    public boolean isMessageRenderable() {
        return messageRenderable;
    }

    public boolean isMessageSendable() {
        return messageSendable;
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

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = reasons == null ? new ArrayList<>() : new ArrayList<>(reasons);
        if (reason != null && !resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }
}
