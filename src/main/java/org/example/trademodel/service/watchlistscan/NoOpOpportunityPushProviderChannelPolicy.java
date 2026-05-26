package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushProviderChannelDTO;

public class NoOpOpportunityPushProviderChannelPolicy implements OpportunityPushProviderChannelPolicy {

    @Override
    public OpportunityPushProviderChannelDTO evaluate(
            String symbol,
            OpportunityPushMessageEnvelopeDTO messageEnvelope
    ) {
        try {
            if (messageEnvelope == null) {
                return OpportunityPushProviderChannelDTO.incomplete(
                        symbol,
                        List.of("MESSAGE_ENVELOPE_MISSING")
                );
            }

            String normalizedSymbol = resolveSymbol(symbol, messageEnvelope);
            if (normalizedSymbol == null) {
                return incompleteFromEnvelope(symbol, messageEnvelope, "SYMBOL_MISSING");
            }
            if (!isSafeEnvelope(messageEnvelope)) {
                return incompleteFromEnvelope(
                        normalizedSymbol,
                        messageEnvelope,
                        "MESSAGE_ENVELOPE_UNSAFE"
                );
            }

            if (!OpportunityPushMessageEnvelopeStatusEnum.DISABLED_NOOP
                    .equals(messageEnvelope.getMessageEnvelopeStatus())) {
                return nonDisabledNoopFromEnvelope(normalizedSymbol, messageEnvelope);
            }

            return OpportunityPushProviderChannelDTO.disabledNoop(
                    normalizedSymbol,
                    messageEnvelope.getMessageEnvelopeStatus(),
                    messageEnvelope.getPipelineStatus(),
                    messageEnvelope.getQueueStatus(),
                    messageEnvelope.getPersistenceStatus(),
                    messageEnvelope.getEnvelopeStatus(),
                    messageEnvelope.getDeliveryDecisionStatus(),
                    messageEnvelope.getSource(),
                    List.of(
                            "PROVIDER_CHANNEL_DISABLED_NOOP",
                            "PROVIDER_CHANNEL_DISABLED_BY_DEFAULT",
                            "PROVIDER_CHANNEL_NO_CREDENTIAL",
                            "PROVIDER_CHANNEL_NO_LIVE_CALL",
                            "PROVIDER_CHANNEL_NO_MESSAGE_OUTPUT"
                    ),
                    messageEnvelope.getMessageEnvelopeReasons(),
                    messageEnvelope.getPipelineReasons(),
                    messageEnvelope.getQueueReasons(),
                    messageEnvelope.getPersistenceReasons(),
                    messageEnvelope.getEnvelopeReasons(),
                    messageEnvelope.getDeliveryReasons(),
                    messageEnvelope.getPushReasons(),
                    messageEnvelope.getAttentionReasons(),
                    messageEnvelope.getRiskGuardReasons(),
                    messageEnvelope.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushProviderChannelDTO.incomplete(
                    symbol,
                    List.of("CHANNEL_POLICY_FAILED")
            );
        }
    }

    private static OpportunityPushProviderChannelDTO incompleteFromEnvelope(
            String symbol,
            OpportunityPushMessageEnvelopeDTO messageEnvelope,
            String reason
    ) {
        return OpportunityPushProviderChannelDTO.incomplete(
                symbol,
                messageEnvelope.getMessageEnvelopeStatus(),
                messageEnvelope.getPipelineStatus(),
                messageEnvelope.getQueueStatus(),
                messageEnvelope.getPersistenceStatus(),
                messageEnvelope.getEnvelopeStatus(),
                messageEnvelope.getDeliveryDecisionStatus(),
                messageEnvelope.getSource(),
                messageEnvelope.getMessageEnvelopeReasons(),
                messageEnvelope.getPipelineReasons(),
                messageEnvelope.getQueueReasons(),
                messageEnvelope.getPersistenceReasons(),
                messageEnvelope.getEnvelopeReasons(),
                messageEnvelope.getDeliveryReasons(),
                messageEnvelope.getPushReasons(),
                messageEnvelope.getAttentionReasons(),
                messageEnvelope.getRiskGuardReasons(),
                withReason(messageEnvelope.getBlockingReasons(), reason)
        );
    }

    private static OpportunityPushProviderChannelDTO nonDisabledNoopFromEnvelope(
            String symbol,
            OpportunityPushMessageEnvelopeDTO messageEnvelope
    ) {
        List<String> blockingReasons = withReason(
                messageEnvelope.getBlockingReasons(),
                "MESSAGE_ENVELOPE_NOT_DISABLED_NOOP"
        );
        if (OpportunityPushMessageEnvelopeStatusEnum.BLOCKED
                .equals(messageEnvelope.getMessageEnvelopeStatus())) {
            return OpportunityPushProviderChannelDTO.blocked(
                    symbol,
                    messageEnvelope.getMessageEnvelopeStatus(),
                    messageEnvelope.getPipelineStatus(),
                    messageEnvelope.getQueueStatus(),
                    messageEnvelope.getPersistenceStatus(),
                    messageEnvelope.getEnvelopeStatus(),
                    messageEnvelope.getDeliveryDecisionStatus(),
                    messageEnvelope.getSource(),
                    messageEnvelope.getMessageEnvelopeReasons(),
                    messageEnvelope.getPipelineReasons(),
                    messageEnvelope.getQueueReasons(),
                    messageEnvelope.getPersistenceReasons(),
                    messageEnvelope.getEnvelopeReasons(),
                    messageEnvelope.getDeliveryReasons(),
                    messageEnvelope.getPushReasons(),
                    messageEnvelope.getAttentionReasons(),
                    messageEnvelope.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        if (OpportunityPushMessageEnvelopeStatusEnum.DISABLED
                .equals(messageEnvelope.getMessageEnvelopeStatus())) {
            return OpportunityPushProviderChannelDTO.disabled(
                    symbol,
                    messageEnvelope.getMessageEnvelopeStatus(),
                    messageEnvelope.getPipelineStatus(),
                    messageEnvelope.getQueueStatus(),
                    messageEnvelope.getPersistenceStatus(),
                    messageEnvelope.getEnvelopeStatus(),
                    messageEnvelope.getDeliveryDecisionStatus(),
                    messageEnvelope.getSource(),
                    messageEnvelope.getMessageEnvelopeReasons(),
                    messageEnvelope.getPipelineReasons(),
                    messageEnvelope.getQueueReasons(),
                    messageEnvelope.getPersistenceReasons(),
                    messageEnvelope.getEnvelopeReasons(),
                    messageEnvelope.getDeliveryReasons(),
                    messageEnvelope.getPushReasons(),
                    messageEnvelope.getAttentionReasons(),
                    messageEnvelope.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        return OpportunityPushProviderChannelDTO.incomplete(
                symbol,
                messageEnvelope.getMessageEnvelopeStatus(),
                messageEnvelope.getPipelineStatus(),
                messageEnvelope.getQueueStatus(),
                messageEnvelope.getPersistenceStatus(),
                messageEnvelope.getEnvelopeStatus(),
                messageEnvelope.getDeliveryDecisionStatus(),
                messageEnvelope.getSource(),
                messageEnvelope.getMessageEnvelopeReasons(),
                messageEnvelope.getPipelineReasons(),
                messageEnvelope.getQueueReasons(),
                messageEnvelope.getPersistenceReasons(),
                messageEnvelope.getEnvelopeReasons(),
                messageEnvelope.getDeliveryReasons(),
                messageEnvelope.getPushReasons(),
                messageEnvelope.getAttentionReasons(),
                messageEnvelope.getRiskGuardReasons(),
                blockingReasons
        );
    }

    private static boolean isSafeEnvelope(OpportunityPushMessageEnvelopeDTO messageEnvelope) {
        return messageEnvelope.isManualReviewRequired()
                && messageEnvelope.isNotTradeInstruction()
                && messageEnvelope.isAuditOnly()
                && !messageEnvelope.isMessageEnvelopeCreated()
                && !messageEnvelope.isMessageRenderable()
                && !messageEnvelope.isMessageRendered()
                && !messageEnvelope.isMessageSendable()
                && !messageEnvelope.isMessageSent()
                && !messageEnvelope.isProviderSelected()
                && !messageEnvelope.isExternalPushSent()
                && !messageEnvelope.isDeliveryAttempted()
                && !messageEnvelope.isDeliveryEnabled()
                && !messageEnvelope.isDeliveryPipelineEnabled()
                && !messageEnvelope.isPipelineStarted()
                && !messageEnvelope.isQueueCreated()
                && !messageEnvelope.isQueued()
                && !messageEnvelope.isEnqueueAttempted()
                && !messageEnvelope.isDequeueAttempted()
                && !messageEnvelope.isWorkerStarted()
                && !messageEnvelope.isPersisted()
                && !messageEnvelope.isPersistenceAttempted()
                && !messageEnvelope.isReadinessUpgraded()
                && !messageEnvelope.isTradingActionCreated()
                && !messageEnvelope.isEntryStopTpRrGenerated();
    }

    private static String resolveSymbol(
            String symbol,
            OpportunityPushMessageEnvelopeDTO messageEnvelope
    ) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(messageEnvelope.getSymbol());
    }

    private static String normalize(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = reasons == null ? new ArrayList<>() : new ArrayList<>(reasons);
        if (reason != null && !resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }
}
