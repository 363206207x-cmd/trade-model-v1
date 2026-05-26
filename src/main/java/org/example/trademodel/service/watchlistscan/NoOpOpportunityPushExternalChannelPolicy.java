package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushExternalChannelDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushProviderChannelDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushProviderChannelStatusEnum;

public class NoOpOpportunityPushExternalChannelPolicy implements OpportunityPushExternalChannelPolicy {

    @Override
    public OpportunityPushExternalChannelDTO evaluate(
            String symbol,
            OpportunityPushProviderChannelDTO providerChannel
    ) {
        try {
            if (providerChannel == null) {
                return OpportunityPushExternalChannelDTO.incomplete(
                        symbol,
                        List.of("PROVIDER_CHANNEL_MISSING")
                );
            }

            String normalizedSymbol = resolveSymbol(symbol, providerChannel);
            if (normalizedSymbol == null) {
                return incompleteFromChannel(symbol, providerChannel, "SYMBOL_MISSING");
            }
            if (!isSafeChannel(providerChannel)) {
                return incompleteFromChannel(
                        normalizedSymbol,
                        providerChannel,
                        "PROVIDER_CHANNEL_UNSAFE"
                );
            }

            if (!OpportunityPushProviderChannelStatusEnum.DISABLED_NOOP
                    .equals(providerChannel.getProviderChannelStatus())) {
                return nonDisabledNoopFromChannel(normalizedSymbol, providerChannel);
            }

            return OpportunityPushExternalChannelDTO.disabledNoop(
                    normalizedSymbol,
                    providerChannel.getProviderChannelStatus(),
                    providerChannel.getMessageEnvelopeStatus(),
                    providerChannel.getPipelineStatus(),
                    providerChannel.getQueueStatus(),
                    providerChannel.getPersistenceStatus(),
                    providerChannel.getEnvelopeStatus(),
                    providerChannel.getDeliveryDecisionStatus(),
                    providerChannel.getSource(),
                    List.of(
                            "EXTERNAL_CHANNEL_DISABLED_NOOP",
                            "EXTERNAL_CHANNEL_DISABLED_BY_DEFAULT",
                            "EXTERNAL_CHANNEL_NO_INTEGRATION",
                            "EXTERNAL_CHANNEL_NO_CREDENTIAL",
                            "EXTERNAL_CHANNEL_NO_LIVE_CALL",
                            "EXTERNAL_CHANNEL_NO_MESSAGE_OUTPUT"
                    ),
                    providerChannel.getProviderChannelReasons(),
                    providerChannel.getMessageEnvelopeReasons(),
                    providerChannel.getPipelineReasons(),
                    providerChannel.getQueueReasons(),
                    providerChannel.getPersistenceReasons(),
                    providerChannel.getEnvelopeReasons(),
                    providerChannel.getDeliveryReasons(),
                    providerChannel.getPushReasons(),
                    providerChannel.getAttentionReasons(),
                    providerChannel.getRiskGuardReasons(),
                    providerChannel.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushExternalChannelDTO.incomplete(
                    symbol,
                    List.of("EXTERNAL_CHANNEL_POLICY_FAILED")
            );
        }
    }

    private static OpportunityPushExternalChannelDTO incompleteFromChannel(
            String symbol,
            OpportunityPushProviderChannelDTO providerChannel,
            String reason
    ) {
        return OpportunityPushExternalChannelDTO.incomplete(
                symbol,
                providerChannel.getProviderChannelStatus(),
                providerChannel.getMessageEnvelopeStatus(),
                providerChannel.getPipelineStatus(),
                providerChannel.getQueueStatus(),
                providerChannel.getPersistenceStatus(),
                providerChannel.getEnvelopeStatus(),
                providerChannel.getDeliveryDecisionStatus(),
                providerChannel.getSource(),
                providerChannel.getProviderChannelReasons(),
                providerChannel.getMessageEnvelopeReasons(),
                providerChannel.getPipelineReasons(),
                providerChannel.getQueueReasons(),
                providerChannel.getPersistenceReasons(),
                providerChannel.getEnvelopeReasons(),
                providerChannel.getDeliveryReasons(),
                providerChannel.getPushReasons(),
                providerChannel.getAttentionReasons(),
                providerChannel.getRiskGuardReasons(),
                withReason(providerChannel.getBlockingReasons(), reason)
        );
    }

    private static OpportunityPushExternalChannelDTO nonDisabledNoopFromChannel(
            String symbol,
            OpportunityPushProviderChannelDTO providerChannel
    ) {
        List<String> blockingReasons = withReason(
                providerChannel.getBlockingReasons(),
                "PROVIDER_CHANNEL_NOT_DISABLED_NOOP"
        );
        if (OpportunityPushProviderChannelStatusEnum.BLOCKED
                .equals(providerChannel.getProviderChannelStatus())) {
            return OpportunityPushExternalChannelDTO.blocked(
                    symbol,
                    providerChannel.getProviderChannelStatus(),
                    providerChannel.getMessageEnvelopeStatus(),
                    providerChannel.getPipelineStatus(),
                    providerChannel.getQueueStatus(),
                    providerChannel.getPersistenceStatus(),
                    providerChannel.getEnvelopeStatus(),
                    providerChannel.getDeliveryDecisionStatus(),
                    providerChannel.getSource(),
                    providerChannel.getProviderChannelReasons(),
                    providerChannel.getMessageEnvelopeReasons(),
                    providerChannel.getPipelineReasons(),
                    providerChannel.getQueueReasons(),
                    providerChannel.getPersistenceReasons(),
                    providerChannel.getEnvelopeReasons(),
                    providerChannel.getDeliveryReasons(),
                    providerChannel.getPushReasons(),
                    providerChannel.getAttentionReasons(),
                    providerChannel.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        if (OpportunityPushProviderChannelStatusEnum.DISABLED
                .equals(providerChannel.getProviderChannelStatus())) {
            return OpportunityPushExternalChannelDTO.disabled(
                    symbol,
                    providerChannel.getProviderChannelStatus(),
                    providerChannel.getMessageEnvelopeStatus(),
                    providerChannel.getPipelineStatus(),
                    providerChannel.getQueueStatus(),
                    providerChannel.getPersistenceStatus(),
                    providerChannel.getEnvelopeStatus(),
                    providerChannel.getDeliveryDecisionStatus(),
                    providerChannel.getSource(),
                    providerChannel.getProviderChannelReasons(),
                    providerChannel.getMessageEnvelopeReasons(),
                    providerChannel.getPipelineReasons(),
                    providerChannel.getQueueReasons(),
                    providerChannel.getPersistenceReasons(),
                    providerChannel.getEnvelopeReasons(),
                    providerChannel.getDeliveryReasons(),
                    providerChannel.getPushReasons(),
                    providerChannel.getAttentionReasons(),
                    providerChannel.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        return OpportunityPushExternalChannelDTO.incomplete(
                symbol,
                providerChannel.getProviderChannelStatus(),
                providerChannel.getMessageEnvelopeStatus(),
                providerChannel.getPipelineStatus(),
                providerChannel.getQueueStatus(),
                providerChannel.getPersistenceStatus(),
                providerChannel.getEnvelopeStatus(),
                providerChannel.getDeliveryDecisionStatus(),
                providerChannel.getSource(),
                providerChannel.getProviderChannelReasons(),
                providerChannel.getMessageEnvelopeReasons(),
                providerChannel.getPipelineReasons(),
                providerChannel.getQueueReasons(),
                providerChannel.getPersistenceReasons(),
                providerChannel.getEnvelopeReasons(),
                providerChannel.getDeliveryReasons(),
                providerChannel.getPushReasons(),
                providerChannel.getAttentionReasons(),
                providerChannel.getRiskGuardReasons(),
                blockingReasons
        );
    }

    private static boolean isSafeChannel(OpportunityPushProviderChannelDTO providerChannel) {
        return providerChannel.isManualReviewRequired()
                && providerChannel.isNotTradeInstruction()
                && providerChannel.isAuditOnly()
                && !providerChannel.isProviderChannelEnabled()
                && !providerChannel.isProviderSelected()
                && !providerChannel.isProviderCredentialRequired()
                && !providerChannel.isProviderCredentialUsed()
                && !providerChannel.isLiveProviderCallAttempted()
                && !providerChannel.isMessageRendered()
                && !providerChannel.isMessageSent()
                && !providerChannel.isExternalPushSent()
                && !providerChannel.isDeliveryAttempted()
                && !providerChannel.isDeliveryEnabled()
                && !providerChannel.isMessageEnvelopeCreated()
                && !providerChannel.isMessageRenderable()
                && !providerChannel.isMessageSendable()
                && !providerChannel.isDeliveryPipelineEnabled()
                && !providerChannel.isPipelineStarted()
                && !providerChannel.isQueueCreated()
                && !providerChannel.isQueued()
                && !providerChannel.isEnqueueAttempted()
                && !providerChannel.isDequeueAttempted()
                && !providerChannel.isWorkerStarted()
                && !providerChannel.isPersisted()
                && !providerChannel.isPersistenceAttempted()
                && !providerChannel.isReadinessUpgraded()
                && !providerChannel.isTradingActionCreated()
                && !providerChannel.isEntryStopTpRrGenerated();
    }

    private static String resolveSymbol(
            String symbol,
            OpportunityPushProviderChannelDTO providerChannel
    ) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(providerChannel.getSymbol());
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
