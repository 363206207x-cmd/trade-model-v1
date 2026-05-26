package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceResultDTO;

public class NoOpOpportunityPushAuditEnvelopePersistencePort implements OpportunityPushAuditEnvelopePersistencePort {

    private static final String AUDIT_ENVELOPE_MISSING = "AUDIT_ENVELOPE_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String AUDIT_ENVELOPE_UNSAFE = "AUDIT_ENVELOPE_UNSAFE";
    private static final String AUDIT_ENVELOPE_NOT_AUDIT_ONLY = "AUDIT_ENVELOPE_NOT_AUDIT_ONLY";
    private static final String AUDIT_PERSISTENCE_POLICY_FAILED = "AUDIT_PERSISTENCE_POLICY_FAILED";
    private static final String AUDIT_ENVELOPE_NOOP_REVIEW_ONLY = "AUDIT_ENVELOPE_NOOP_REVIEW_ONLY";
    private static final String AUDIT_PERSISTENCE_DISABLED_BY_DEFAULT =
            "AUDIT_PERSISTENCE_DISABLED_BY_DEFAULT";

    @Override
    public OpportunityPushAuditPersistenceResultDTO evaluate(
            String symbol,
            OpportunityPushAuditEnvelopeDTO auditEnvelope
    ) {
        try {
            if (auditEnvelope == null) {
                return OpportunityPushAuditPersistenceResultDTO.incomplete(
                        symbol,
                        List.of(AUDIT_ENVELOPE_MISSING)
                );
            }

            String normalizedSymbol = resolveSymbol(symbol, auditEnvelope);
            if (normalizedSymbol == null) {
                return incompleteFromEnvelope(symbol, auditEnvelope, SYMBOL_MISSING);
            }
            if (!isSafeAuditEnvelope(auditEnvelope)) {
                return incompleteFromEnvelope(normalizedSymbol, auditEnvelope, AUDIT_ENVELOPE_UNSAFE);
            }

            if (!OpportunityPushAuditEnvelopeStatusEnum.AUDIT_ONLY.equals(auditEnvelope.getEnvelopeStatus())) {
                return nonAuditOnlyFromEnvelope(normalizedSymbol, auditEnvelope);
            }

            return OpportunityPushAuditPersistenceResultDTO.noopReviewOnly(
                    normalizedSymbol,
                    auditEnvelope.getEnvelopeStatus(),
                    auditEnvelope.getDeliveryDecisionStatus(),
                    auditEnvelope.getSource(),
                    List.of(AUDIT_ENVELOPE_NOOP_REVIEW_ONLY, AUDIT_PERSISTENCE_DISABLED_BY_DEFAULT),
                    auditEnvelope.getEnvelopeReasons(),
                    auditEnvelope.getDeliveryReasons(),
                    auditEnvelope.getPushReasons(),
                    auditEnvelope.getAttentionReasons(),
                    auditEnvelope.getRiskGuardReasons(),
                    auditEnvelope.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushAuditPersistenceResultDTO.incomplete(
                    symbol,
                    List.of(AUDIT_PERSISTENCE_POLICY_FAILED)
            );
        }
    }

    private static OpportunityPushAuditPersistenceResultDTO incompleteFromEnvelope(
            String symbol,
            OpportunityPushAuditEnvelopeDTO auditEnvelope,
            String reason
    ) {
        return OpportunityPushAuditPersistenceResultDTO.incomplete(
                symbol,
                auditEnvelope.getEnvelopeStatus(),
                auditEnvelope.getDeliveryDecisionStatus(),
                auditEnvelope.getSource(),
                auditEnvelope.getEnvelopeReasons(),
                auditEnvelope.getDeliveryReasons(),
                auditEnvelope.getPushReasons(),
                auditEnvelope.getAttentionReasons(),
                auditEnvelope.getRiskGuardReasons(),
                withReason(auditEnvelope.getBlockingReasons(), reason)
        );
    }

    private static OpportunityPushAuditPersistenceResultDTO nonAuditOnlyFromEnvelope(
            String symbol,
            OpportunityPushAuditEnvelopeDTO auditEnvelope
    ) {
        List<String> blockingReasons = withReason(
                auditEnvelope.getBlockingReasons(),
                AUDIT_ENVELOPE_NOT_AUDIT_ONLY
        );
        if (OpportunityPushAuditEnvelopeStatusEnum.BLOCKED.equals(auditEnvelope.getEnvelopeStatus())) {
            return OpportunityPushAuditPersistenceResultDTO.blocked(
                    symbol,
                    auditEnvelope.getEnvelopeStatus(),
                    auditEnvelope.getDeliveryDecisionStatus(),
                    auditEnvelope.getSource(),
                    auditEnvelope.getEnvelopeReasons(),
                    auditEnvelope.getDeliveryReasons(),
                    auditEnvelope.getPushReasons(),
                    auditEnvelope.getAttentionReasons(),
                    auditEnvelope.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        if (OpportunityPushAuditEnvelopeStatusEnum.DISABLED.equals(auditEnvelope.getEnvelopeStatus())) {
            return OpportunityPushAuditPersistenceResultDTO.disabled(
                    symbol,
                    auditEnvelope.getEnvelopeStatus(),
                    auditEnvelope.getDeliveryDecisionStatus(),
                    auditEnvelope.getSource(),
                    auditEnvelope.getEnvelopeReasons(),
                    auditEnvelope.getDeliveryReasons(),
                    auditEnvelope.getPushReasons(),
                    auditEnvelope.getAttentionReasons(),
                    auditEnvelope.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        return OpportunityPushAuditPersistenceResultDTO.incomplete(
                symbol,
                auditEnvelope.getEnvelopeStatus(),
                auditEnvelope.getDeliveryDecisionStatus(),
                auditEnvelope.getSource(),
                auditEnvelope.getEnvelopeReasons(),
                auditEnvelope.getDeliveryReasons(),
                auditEnvelope.getPushReasons(),
                auditEnvelope.getAttentionReasons(),
                auditEnvelope.getRiskGuardReasons(),
                blockingReasons
        );
    }

    private static boolean isSafeAuditEnvelope(OpportunityPushAuditEnvelopeDTO auditEnvelope) {
        return auditEnvelope.isManualReviewRequired()
                && auditEnvelope.isNotTradeInstruction()
                && auditEnvelope.isAuditOnly()
                && !auditEnvelope.isPersisted()
                && !auditEnvelope.isQueued()
                && !auditEnvelope.isExternalPushSent()
                && !auditEnvelope.isDeliveryAttempted()
                && !auditEnvelope.isDeliveryEnabled()
                && !auditEnvelope.isReadinessUpgraded()
                && !auditEnvelope.isTradingActionCreated()
                && !auditEnvelope.isEntryStopTpRrGenerated();
    }

    private static String resolveSymbol(String symbol, OpportunityPushAuditEnvelopeDTO auditEnvelope) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(auditEnvelope.getSymbol());
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
