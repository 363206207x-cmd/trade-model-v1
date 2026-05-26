package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueResultDTO;

public class NoOpOpportunityPushAuditQueuePort implements OpportunityPushAuditQueuePort {

    private static final String PERSISTENCE_RESULT_MISSING = "PERSISTENCE_RESULT_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String PERSISTENCE_RESULT_UNSAFE = "PERSISTENCE_RESULT_UNSAFE";
    private static final String PERSISTENCE_RESULT_NOT_NOOP_REVIEW_ONLY =
            "PERSISTENCE_RESULT_NOT_NOOP_REVIEW_ONLY";
    private static final String AUDIT_QUEUE_POLICY_FAILED = "AUDIT_QUEUE_POLICY_FAILED";
    private static final String AUDIT_QUEUE_NOOP_REVIEW_ONLY = "AUDIT_QUEUE_NOOP_REVIEW_ONLY";
    private static final String AUDIT_QUEUE_DISABLED_BY_DEFAULT = "AUDIT_QUEUE_DISABLED_BY_DEFAULT";

    @Override
    public OpportunityPushAuditQueueResultDTO evaluate(
            String symbol,
            OpportunityPushAuditPersistenceResultDTO persistenceResult
    ) {
        try {
            if (persistenceResult == null) {
                return OpportunityPushAuditQueueResultDTO.incomplete(
                        symbol,
                        List.of(PERSISTENCE_RESULT_MISSING)
                );
            }

            String normalizedSymbol = resolveSymbol(symbol, persistenceResult);
            if (normalizedSymbol == null) {
                return incompleteFromPersistenceResult(symbol, persistenceResult, SYMBOL_MISSING);
            }
            if (!isSafePersistenceResult(persistenceResult)) {
                return incompleteFromPersistenceResult(
                        normalizedSymbol,
                        persistenceResult,
                        PERSISTENCE_RESULT_UNSAFE
                );
            }

            if (!OpportunityPushAuditPersistenceStatusEnum.NOOP_REVIEW_ONLY.equals(
                    persistenceResult.getPersistenceStatus()
            )) {
                return nonNoopReviewOnlyFromPersistenceResult(normalizedSymbol, persistenceResult);
            }

            return OpportunityPushAuditQueueResultDTO.noopReviewOnly(
                    normalizedSymbol,
                    persistenceResult.getPersistenceStatus(),
                    persistenceResult.getEnvelopeStatus(),
                    persistenceResult.getDeliveryDecisionStatus(),
                    persistenceResult.getSource(),
                    List.of(AUDIT_QUEUE_NOOP_REVIEW_ONLY, AUDIT_QUEUE_DISABLED_BY_DEFAULT),
                    persistenceResult.getPersistenceReasons(),
                    persistenceResult.getEnvelopeReasons(),
                    persistenceResult.getDeliveryReasons(),
                    persistenceResult.getPushReasons(),
                    persistenceResult.getAttentionReasons(),
                    persistenceResult.getRiskGuardReasons(),
                    persistenceResult.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushAuditQueueResultDTO.incomplete(
                    symbol,
                    List.of(AUDIT_QUEUE_POLICY_FAILED)
            );
        }
    }

    private static OpportunityPushAuditQueueResultDTO incompleteFromPersistenceResult(
            String symbol,
            OpportunityPushAuditPersistenceResultDTO persistenceResult,
            String reason
    ) {
        return OpportunityPushAuditQueueResultDTO.incomplete(
                symbol,
                persistenceResult.getPersistenceStatus(),
                persistenceResult.getEnvelopeStatus(),
                persistenceResult.getDeliveryDecisionStatus(),
                persistenceResult.getSource(),
                persistenceResult.getPersistenceReasons(),
                persistenceResult.getEnvelopeReasons(),
                persistenceResult.getDeliveryReasons(),
                persistenceResult.getPushReasons(),
                persistenceResult.getAttentionReasons(),
                persistenceResult.getRiskGuardReasons(),
                withReason(persistenceResult.getBlockingReasons(), reason)
        );
    }

    private static OpportunityPushAuditQueueResultDTO nonNoopReviewOnlyFromPersistenceResult(
            String symbol,
            OpportunityPushAuditPersistenceResultDTO persistenceResult
    ) {
        List<String> blockingReasons = withReason(
                persistenceResult.getBlockingReasons(),
                PERSISTENCE_RESULT_NOT_NOOP_REVIEW_ONLY
        );
        if (OpportunityPushAuditPersistenceStatusEnum.BLOCKED.equals(persistenceResult.getPersistenceStatus())) {
            return OpportunityPushAuditQueueResultDTO.blocked(
                    symbol,
                    persistenceResult.getPersistenceStatus(),
                    persistenceResult.getEnvelopeStatus(),
                    persistenceResult.getDeliveryDecisionStatus(),
                    persistenceResult.getSource(),
                    persistenceResult.getPersistenceReasons(),
                    persistenceResult.getEnvelopeReasons(),
                    persistenceResult.getDeliveryReasons(),
                    persistenceResult.getPushReasons(),
                    persistenceResult.getAttentionReasons(),
                    persistenceResult.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        if (OpportunityPushAuditPersistenceStatusEnum.DISABLED.equals(persistenceResult.getPersistenceStatus())) {
            return OpportunityPushAuditQueueResultDTO.disabled(
                    symbol,
                    persistenceResult.getPersistenceStatus(),
                    persistenceResult.getEnvelopeStatus(),
                    persistenceResult.getDeliveryDecisionStatus(),
                    persistenceResult.getSource(),
                    persistenceResult.getPersistenceReasons(),
                    persistenceResult.getEnvelopeReasons(),
                    persistenceResult.getDeliveryReasons(),
                    persistenceResult.getPushReasons(),
                    persistenceResult.getAttentionReasons(),
                    persistenceResult.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        return OpportunityPushAuditQueueResultDTO.incomplete(
                symbol,
                persistenceResult.getPersistenceStatus(),
                persistenceResult.getEnvelopeStatus(),
                persistenceResult.getDeliveryDecisionStatus(),
                persistenceResult.getSource(),
                persistenceResult.getPersistenceReasons(),
                persistenceResult.getEnvelopeReasons(),
                persistenceResult.getDeliveryReasons(),
                persistenceResult.getPushReasons(),
                persistenceResult.getAttentionReasons(),
                persistenceResult.getRiskGuardReasons(),
                blockingReasons
        );
    }

    private static boolean isSafePersistenceResult(OpportunityPushAuditPersistenceResultDTO persistenceResult) {
        return persistenceResult.isManualReviewRequired()
                && persistenceResult.isNotTradeInstruction()
                && persistenceResult.isAuditOnly()
                && !persistenceResult.isQueueCreated()
                && !persistenceResult.isQueued()
                && !persistenceResult.isPersisted()
                && !persistenceResult.isPersistenceAttempted()
                && !persistenceResult.isExternalPushSent()
                && !persistenceResult.isDeliveryAttempted()
                && !persistenceResult.isDeliveryEnabled()
                && !persistenceResult.isReadinessUpgraded()
                && !persistenceResult.isTradingActionCreated()
                && !persistenceResult.isEntryStopTpRrGenerated();
    }

    private static String resolveSymbol(String symbol, OpportunityPushAuditPersistenceResultDTO persistenceResult) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(persistenceResult.getSymbol());
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
