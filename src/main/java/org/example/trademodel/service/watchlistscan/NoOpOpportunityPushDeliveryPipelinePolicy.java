package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineResultDTO;

public class NoOpOpportunityPushDeliveryPipelinePolicy implements OpportunityPushDeliveryPipelinePolicy {

    private static final String AUDIT_QUEUE_RESULT_MISSING = "AUDIT_QUEUE_RESULT_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String AUDIT_QUEUE_RESULT_UNSAFE = "AUDIT_QUEUE_RESULT_UNSAFE";
    private static final String AUDIT_QUEUE_RESULT_NOT_NOOP_REVIEW_ONLY =
            "AUDIT_QUEUE_RESULT_NOT_NOOP_REVIEW_ONLY";
    private static final String DELIVERY_PIPELINE_POLICY_FAILED = "DELIVERY_PIPELINE_POLICY_FAILED";
    private static final String DELIVERY_PIPELINE_DISABLED_NOOP = "DELIVERY_PIPELINE_DISABLED_NOOP";
    private static final String DELIVERY_PIPELINE_DISABLED_BY_DEFAULT =
            "DELIVERY_PIPELINE_DISABLED_BY_DEFAULT";
    private static final String DELIVERY_PIPELINE_NO_EXTERNAL_PROVIDER =
            "DELIVERY_PIPELINE_NO_EXTERNAL_PROVIDER";
    private static final String DELIVERY_PIPELINE_NO_MESSAGE_OUTPUT =
            "DELIVERY_PIPELINE_NO_MESSAGE_OUTPUT";

    @Override
    public OpportunityPushDeliveryPipelineResultDTO evaluate(
            String symbol,
            OpportunityPushAuditQueueResultDTO queueResult
    ) {
        try {
            if (queueResult == null) {
                return OpportunityPushDeliveryPipelineResultDTO.incomplete(
                        symbol,
                        List.of(AUDIT_QUEUE_RESULT_MISSING)
                );
            }

            String normalizedSymbol = resolveSymbol(symbol, queueResult);
            if (normalizedSymbol == null) {
                return incompleteFromQueueResult(symbol, queueResult, SYMBOL_MISSING);
            }
            if (!isSafeQueueResult(queueResult)) {
                return incompleteFromQueueResult(
                        normalizedSymbol,
                        queueResult,
                        AUDIT_QUEUE_RESULT_UNSAFE
                );
            }

            if (!OpportunityPushAuditQueueStatusEnum.NOOP_REVIEW_ONLY.equals(queueResult.getQueueStatus())) {
                return nonNoopReviewOnlyFromQueueResult(normalizedSymbol, queueResult);
            }

            return OpportunityPushDeliveryPipelineResultDTO.disabledNoop(
                    normalizedSymbol,
                    queueResult.getQueueStatus(),
                    queueResult.getPersistenceStatus(),
                    queueResult.getEnvelopeStatus(),
                    queueResult.getDeliveryDecisionStatus(),
                    queueResult.getSource(),
                    List.of(
                            DELIVERY_PIPELINE_DISABLED_NOOP,
                            DELIVERY_PIPELINE_DISABLED_BY_DEFAULT,
                            DELIVERY_PIPELINE_NO_EXTERNAL_PROVIDER,
                            DELIVERY_PIPELINE_NO_MESSAGE_OUTPUT
                    ),
                    queueResult.getQueueReasons(),
                    queueResult.getPersistenceReasons(),
                    queueResult.getEnvelopeReasons(),
                    queueResult.getDeliveryReasons(),
                    queueResult.getPushReasons(),
                    queueResult.getAttentionReasons(),
                    queueResult.getRiskGuardReasons(),
                    queueResult.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushDeliveryPipelineResultDTO.incomplete(
                    symbol,
                    List.of(DELIVERY_PIPELINE_POLICY_FAILED)
            );
        }
    }

    private static OpportunityPushDeliveryPipelineResultDTO incompleteFromQueueResult(
            String symbol,
            OpportunityPushAuditQueueResultDTO queueResult,
            String reason
    ) {
        return OpportunityPushDeliveryPipelineResultDTO.incomplete(
                symbol,
                queueResult.getQueueStatus(),
                queueResult.getPersistenceStatus(),
                queueResult.getEnvelopeStatus(),
                queueResult.getDeliveryDecisionStatus(),
                queueResult.getSource(),
                queueResult.getQueueReasons(),
                queueResult.getPersistenceReasons(),
                queueResult.getEnvelopeReasons(),
                queueResult.getDeliveryReasons(),
                queueResult.getPushReasons(),
                queueResult.getAttentionReasons(),
                queueResult.getRiskGuardReasons(),
                withReason(queueResult.getBlockingReasons(), reason)
        );
    }

    private static OpportunityPushDeliveryPipelineResultDTO nonNoopReviewOnlyFromQueueResult(
            String symbol,
            OpportunityPushAuditQueueResultDTO queueResult
    ) {
        List<String> blockingReasons = withReason(
                queueResult.getBlockingReasons(),
                AUDIT_QUEUE_RESULT_NOT_NOOP_REVIEW_ONLY
        );
        if (OpportunityPushAuditQueueStatusEnum.BLOCKED.equals(queueResult.getQueueStatus())) {
            return OpportunityPushDeliveryPipelineResultDTO.blocked(
                    symbol,
                    queueResult.getQueueStatus(),
                    queueResult.getPersistenceStatus(),
                    queueResult.getEnvelopeStatus(),
                    queueResult.getDeliveryDecisionStatus(),
                    queueResult.getSource(),
                    queueResult.getQueueReasons(),
                    queueResult.getPersistenceReasons(),
                    queueResult.getEnvelopeReasons(),
                    queueResult.getDeliveryReasons(),
                    queueResult.getPushReasons(),
                    queueResult.getAttentionReasons(),
                    queueResult.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        if (OpportunityPushAuditQueueStatusEnum.DISABLED.equals(queueResult.getQueueStatus())) {
            return OpportunityPushDeliveryPipelineResultDTO.disabled(
                    symbol,
                    queueResult.getQueueStatus(),
                    queueResult.getPersistenceStatus(),
                    queueResult.getEnvelopeStatus(),
                    queueResult.getDeliveryDecisionStatus(),
                    queueResult.getSource(),
                    queueResult.getQueueReasons(),
                    queueResult.getPersistenceReasons(),
                    queueResult.getEnvelopeReasons(),
                    queueResult.getDeliveryReasons(),
                    queueResult.getPushReasons(),
                    queueResult.getAttentionReasons(),
                    queueResult.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        return OpportunityPushDeliveryPipelineResultDTO.incomplete(
                symbol,
                queueResult.getQueueStatus(),
                queueResult.getPersistenceStatus(),
                queueResult.getEnvelopeStatus(),
                queueResult.getDeliveryDecisionStatus(),
                queueResult.getSource(),
                queueResult.getQueueReasons(),
                queueResult.getPersistenceReasons(),
                queueResult.getEnvelopeReasons(),
                queueResult.getDeliveryReasons(),
                queueResult.getPushReasons(),
                queueResult.getAttentionReasons(),
                queueResult.getRiskGuardReasons(),
                blockingReasons
        );
    }

    private static boolean isSafeQueueResult(OpportunityPushAuditQueueResultDTO queueResult) {
        return queueResult.isManualReviewRequired()
                && queueResult.isNotTradeInstruction()
                && queueResult.isAuditOnly()
                && !queueResult.isQueueCreated()
                && !queueResult.isQueued()
                && !queueResult.isEnqueueAttempted()
                && !queueResult.isDequeueAttempted()
                && !queueResult.isWorkerStarted()
                && !queueResult.isPersisted()
                && !queueResult.isPersistenceAttempted()
                && !queueResult.isExternalPushSent()
                && !queueResult.isDeliveryAttempted()
                && !queueResult.isDeliveryEnabled()
                && !queueResult.isReadinessUpgraded()
                && !queueResult.isTradingActionCreated()
                && !queueResult.isEntryStopTpRrGenerated();
    }

    private static String resolveSymbol(String symbol, OpportunityPushAuditQueueResultDTO queueResult) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(queueResult.getSymbol());
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
