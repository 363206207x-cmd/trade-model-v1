package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeDTO;

public class NoOpOpportunityPushMessageEnvelopeAssembler implements OpportunityPushMessageEnvelopeAssembler {

    private static final String DELIVERY_PIPELINE_RESULT_MISSING = "DELIVERY_PIPELINE_RESULT_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String DELIVERY_PIPELINE_RESULT_UNSAFE = "DELIVERY_PIPELINE_RESULT_UNSAFE";
    private static final String DELIVERY_PIPELINE_RESULT_NOT_DISABLED_NOOP =
            "DELIVERY_PIPELINE_RESULT_NOT_DISABLED_NOOP";
    private static final String MESSAGE_ENVELOPE_ASSEMBLER_FAILED =
            "MESSAGE_ENVELOPE_ASSEMBLER_FAILED";
    private static final String MESSAGE_ENVELOPE_DISABLED_NOOP = "MESSAGE_ENVELOPE_DISABLED_NOOP";
    private static final String MESSAGE_ENVELOPE_DISABLED_BY_DEFAULT =
            "MESSAGE_ENVELOPE_DISABLED_BY_DEFAULT";
    private static final String MESSAGE_ENVELOPE_NO_FINAL_TEXT = "MESSAGE_ENVELOPE_NO_FINAL_TEXT";
    private static final String MESSAGE_ENVELOPE_NO_SEND_OUTPUT = "MESSAGE_ENVELOPE_NO_SEND_OUTPUT";
    private static final String MESSAGE_ENVELOPE_NO_PROVIDER = "MESSAGE_ENVELOPE_NO_PROVIDER";

    @Override
    public OpportunityPushMessageEnvelopeDTO evaluate(
            String symbol,
            OpportunityPushDeliveryPipelineResultDTO pipelineResult
    ) {
        try {
            if (pipelineResult == null) {
                return OpportunityPushMessageEnvelopeDTO.incomplete(
                        symbol,
                        List.of(DELIVERY_PIPELINE_RESULT_MISSING)
                );
            }

            String normalizedSymbol = resolveSymbol(symbol, pipelineResult);
            if (normalizedSymbol == null) {
                return incompleteFromPipelineResult(symbol, pipelineResult, SYMBOL_MISSING);
            }
            if (!isSafePipelineResult(pipelineResult)) {
                return incompleteFromPipelineResult(
                        normalizedSymbol,
                        pipelineResult,
                        DELIVERY_PIPELINE_RESULT_UNSAFE
                );
            }

            if (!OpportunityPushDeliveryPipelineStatusEnum.DISABLED_NOOP
                    .equals(pipelineResult.getPipelineStatus())) {
                return nonDisabledNoopFromPipelineResult(normalizedSymbol, pipelineResult);
            }

            return OpportunityPushMessageEnvelopeDTO.disabledNoop(
                    normalizedSymbol,
                    pipelineResult.getPipelineStatus(),
                    pipelineResult.getQueueStatus(),
                    pipelineResult.getPersistenceStatus(),
                    pipelineResult.getEnvelopeStatus(),
                    pipelineResult.getDeliveryDecisionStatus(),
                    pipelineResult.getSource(),
                    List.of(
                            MESSAGE_ENVELOPE_DISABLED_NOOP,
                            MESSAGE_ENVELOPE_DISABLED_BY_DEFAULT,
                            MESSAGE_ENVELOPE_NO_FINAL_TEXT,
                            MESSAGE_ENVELOPE_NO_SEND_OUTPUT,
                            MESSAGE_ENVELOPE_NO_PROVIDER
                    ),
                    pipelineResult.getPipelineReasons(),
                    pipelineResult.getQueueReasons(),
                    pipelineResult.getPersistenceReasons(),
                    pipelineResult.getEnvelopeReasons(),
                    pipelineResult.getDeliveryReasons(),
                    pipelineResult.getPushReasons(),
                    pipelineResult.getAttentionReasons(),
                    pipelineResult.getRiskGuardReasons(),
                    pipelineResult.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushMessageEnvelopeDTO.incomplete(
                    symbol,
                    List.of(MESSAGE_ENVELOPE_ASSEMBLER_FAILED)
            );
        }
    }

    private static OpportunityPushMessageEnvelopeDTO incompleteFromPipelineResult(
            String symbol,
            OpportunityPushDeliveryPipelineResultDTO pipelineResult,
            String reason
    ) {
        return OpportunityPushMessageEnvelopeDTO.incomplete(
                symbol,
                pipelineResult.getPipelineStatus(),
                pipelineResult.getQueueStatus(),
                pipelineResult.getPersistenceStatus(),
                pipelineResult.getEnvelopeStatus(),
                pipelineResult.getDeliveryDecisionStatus(),
                pipelineResult.getSource(),
                pipelineResult.getPipelineReasons(),
                pipelineResult.getQueueReasons(),
                pipelineResult.getPersistenceReasons(),
                pipelineResult.getEnvelopeReasons(),
                pipelineResult.getDeliveryReasons(),
                pipelineResult.getPushReasons(),
                pipelineResult.getAttentionReasons(),
                pipelineResult.getRiskGuardReasons(),
                withReason(pipelineResult.getBlockingReasons(), reason)
        );
    }

    private static OpportunityPushMessageEnvelopeDTO nonDisabledNoopFromPipelineResult(
            String symbol,
            OpportunityPushDeliveryPipelineResultDTO pipelineResult
    ) {
        List<String> blockingReasons = withReason(
                pipelineResult.getBlockingReasons(),
                DELIVERY_PIPELINE_RESULT_NOT_DISABLED_NOOP
        );
        if (OpportunityPushDeliveryPipelineStatusEnum.BLOCKED
                .equals(pipelineResult.getPipelineStatus())) {
            return OpportunityPushMessageEnvelopeDTO.blocked(
                    symbol,
                    pipelineResult.getPipelineStatus(),
                    pipelineResult.getQueueStatus(),
                    pipelineResult.getPersistenceStatus(),
                    pipelineResult.getEnvelopeStatus(),
                    pipelineResult.getDeliveryDecisionStatus(),
                    pipelineResult.getSource(),
                    pipelineResult.getPipelineReasons(),
                    pipelineResult.getQueueReasons(),
                    pipelineResult.getPersistenceReasons(),
                    pipelineResult.getEnvelopeReasons(),
                    pipelineResult.getDeliveryReasons(),
                    pipelineResult.getPushReasons(),
                    pipelineResult.getAttentionReasons(),
                    pipelineResult.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        if (OpportunityPushDeliveryPipelineStatusEnum.DISABLED
                .equals(pipelineResult.getPipelineStatus())) {
            return OpportunityPushMessageEnvelopeDTO.disabled(
                    symbol,
                    pipelineResult.getPipelineStatus(),
                    pipelineResult.getQueueStatus(),
                    pipelineResult.getPersistenceStatus(),
                    pipelineResult.getEnvelopeStatus(),
                    pipelineResult.getDeliveryDecisionStatus(),
                    pipelineResult.getSource(),
                    pipelineResult.getPipelineReasons(),
                    pipelineResult.getQueueReasons(),
                    pipelineResult.getPersistenceReasons(),
                    pipelineResult.getEnvelopeReasons(),
                    pipelineResult.getDeliveryReasons(),
                    pipelineResult.getPushReasons(),
                    pipelineResult.getAttentionReasons(),
                    pipelineResult.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        return OpportunityPushMessageEnvelopeDTO.incomplete(
                symbol,
                pipelineResult.getPipelineStatus(),
                pipelineResult.getQueueStatus(),
                pipelineResult.getPersistenceStatus(),
                pipelineResult.getEnvelopeStatus(),
                pipelineResult.getDeliveryDecisionStatus(),
                pipelineResult.getSource(),
                pipelineResult.getPipelineReasons(),
                pipelineResult.getQueueReasons(),
                pipelineResult.getPersistenceReasons(),
                pipelineResult.getEnvelopeReasons(),
                pipelineResult.getDeliveryReasons(),
                pipelineResult.getPushReasons(),
                pipelineResult.getAttentionReasons(),
                pipelineResult.getRiskGuardReasons(),
                blockingReasons
        );
    }

    private static boolean isSafePipelineResult(OpportunityPushDeliveryPipelineResultDTO pipelineResult) {
        return pipelineResult.isManualReviewRequired()
                && pipelineResult.isNotTradeInstruction()
                && pipelineResult.isAuditOnly()
                && !pipelineResult.isDeliveryPipelineEnabled()
                && !pipelineResult.isPipelineStarted()
                && !pipelineResult.isProviderSelected()
                && !pipelineResult.isMessageRendered()
                && !pipelineResult.isMessageSent()
                && !pipelineResult.isExternalPushSent()
                && !pipelineResult.isDeliveryAttempted()
                && !pipelineResult.isDeliveryEnabled()
                && !pipelineResult.isQueueCreated()
                && !pipelineResult.isQueued()
                && !pipelineResult.isEnqueueAttempted()
                && !pipelineResult.isDequeueAttempted()
                && !pipelineResult.isWorkerStarted()
                && !pipelineResult.isPersisted()
                && !pipelineResult.isPersistenceAttempted()
                && !pipelineResult.isReadinessUpgraded()
                && !pipelineResult.isTradingActionCreated()
                && !pipelineResult.isEntryStopTpRrGenerated();
    }

    private static String resolveSymbol(
            String symbol,
            OpportunityPushDeliveryPipelineResultDTO pipelineResult
    ) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(pipelineResult.getSymbol());
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
