package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionStatusEnum;

public class NoOpOpportunityPushAuditEnvelopeAssembler implements OpportunityPushAuditEnvelopeAssembler {

    private static final String DELIVERY_DECISION_MISSING = "DELIVERY_DECISION_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String DELIVERY_DECISION_UNSAFE = "DELIVERY_DECISION_UNSAFE";
    private static final String DELIVERY_DECISION_NOT_REVIEW_ONLY = "DELIVERY_DECISION_NOT_REVIEW_ONLY";
    private static final String AUDIT_ENVELOPE_POLICY_FAILED = "AUDIT_ENVELOPE_POLICY_FAILED";
    private static final String OPPORTUNITY_PUSH_AUDIT_ONLY_ENVELOPE = "OPPORTUNITY_PUSH_AUDIT_ONLY_ENVELOPE";
    private static final String AUDIT_ONLY_INTERNAL_ENVELOPE = "AUDIT_ONLY_INTERNAL_ENVELOPE";

    @Override
    public OpportunityPushAuditEnvelopeDTO assemble(
            String symbol,
            OpportunityPushDeliveryDecisionDTO deliveryDecision
    ) {
        try {
            if (deliveryDecision == null) {
                return OpportunityPushAuditEnvelopeDTO.incomplete(symbol, List.of(DELIVERY_DECISION_MISSING));
            }

            String normalizedSymbol = resolveSymbol(symbol, deliveryDecision);
            if (normalizedSymbol == null) {
                return incompleteFromDecision(symbol, deliveryDecision, SYMBOL_MISSING);
            }
            if (!isSafeDeliveryDecision(deliveryDecision)) {
                return incompleteFromDecision(normalizedSymbol, deliveryDecision, DELIVERY_DECISION_UNSAFE);
            }

            if (!OpportunityPushDeliveryDecisionStatusEnum.REVIEW_ONLY.equals(deliveryDecision.getDecisionStatus())) {
                return nonReviewOnlyFromDecision(normalizedSymbol, deliveryDecision);
            }

            return OpportunityPushAuditEnvelopeDTO.auditOnly(
                    normalizedSymbol,
                    deliveryDecision.getDecisionStatus(),
                    deliveryDecision.getSource(),
                    List.of(OPPORTUNITY_PUSH_AUDIT_ONLY_ENVELOPE, AUDIT_ONLY_INTERNAL_ENVELOPE),
                    deliveryDecision.getDeliveryReasons(),
                    deliveryDecision.getPushReasons(),
                    deliveryDecision.getAttentionReasons(),
                    deliveryDecision.getRiskGuardReasons(),
                    deliveryDecision.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushAuditEnvelopeDTO.incomplete(symbol, List.of(AUDIT_ENVELOPE_POLICY_FAILED));
        }
    }

    private static OpportunityPushAuditEnvelopeDTO incompleteFromDecision(
            String symbol,
            OpportunityPushDeliveryDecisionDTO deliveryDecision,
            String reason
    ) {
        return OpportunityPushAuditEnvelopeDTO.incomplete(
                symbol,
                deliveryDecision.getDecisionStatus(),
                deliveryDecision.getSource(),
                deliveryDecision.getDeliveryReasons(),
                deliveryDecision.getPushReasons(),
                deliveryDecision.getAttentionReasons(),
                deliveryDecision.getRiskGuardReasons(),
                withReason(deliveryDecision.getBlockingReasons(), reason)
        );
    }

    private static OpportunityPushAuditEnvelopeDTO nonReviewOnlyFromDecision(
            String symbol,
            OpportunityPushDeliveryDecisionDTO deliveryDecision
    ) {
        List<String> blockingReasons = withReason(
                deliveryDecision.getBlockingReasons(),
                DELIVERY_DECISION_NOT_REVIEW_ONLY
        );
        if (OpportunityPushDeliveryDecisionStatusEnum.BLOCKED.equals(deliveryDecision.getDecisionStatus())) {
            return OpportunityPushAuditEnvelopeDTO.blocked(
                    symbol,
                    deliveryDecision.getDecisionStatus(),
                    deliveryDecision.getSource(),
                    deliveryDecision.getDeliveryReasons(),
                    deliveryDecision.getPushReasons(),
                    deliveryDecision.getAttentionReasons(),
                    deliveryDecision.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        if (OpportunityPushDeliveryDecisionStatusEnum.DISABLED.equals(deliveryDecision.getDecisionStatus())) {
            return OpportunityPushAuditEnvelopeDTO.disabled(
                    symbol,
                    deliveryDecision.getDecisionStatus(),
                    deliveryDecision.getSource(),
                    deliveryDecision.getDeliveryReasons(),
                    deliveryDecision.getPushReasons(),
                    deliveryDecision.getAttentionReasons(),
                    deliveryDecision.getRiskGuardReasons(),
                    blockingReasons
            );
        }
        return OpportunityPushAuditEnvelopeDTO.incomplete(
                symbol,
                deliveryDecision.getDecisionStatus(),
                deliveryDecision.getSource(),
                deliveryDecision.getDeliveryReasons(),
                deliveryDecision.getPushReasons(),
                deliveryDecision.getAttentionReasons(),
                deliveryDecision.getRiskGuardReasons(),
                blockingReasons
        );
    }

    private static boolean isSafeDeliveryDecision(OpportunityPushDeliveryDecisionDTO deliveryDecision) {
        return deliveryDecision.isManualReviewRequired()
                && deliveryDecision.isNotTradeInstruction()
                && !deliveryDecision.isExternalPushSent()
                && !deliveryDecision.isDeliveryAttempted()
                && !deliveryDecision.isDeliveryEnabled()
                && !deliveryDecision.isReadinessUpgraded()
                && !deliveryDecision.isTradingActionCreated()
                && !deliveryDecision.isEntryStopTpRrGenerated();
    }

    private static String resolveSymbol(String symbol, OpportunityPushDeliveryDecisionDTO deliveryDecision) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(deliveryDecision.getSymbol());
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
