package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryDecisionDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushStatusEnum;

public class NoOpOpportunityPushDeliveryPolicy implements OpportunityPushDeliveryPolicy {

    private static final String OPPORTUNITY_PUSH_MISSING = "OPPORTUNITY_PUSH_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String OPPORTUNITY_PUSH_UNSAFE = "OPPORTUNITY_PUSH_UNSAFE";
    private static final String OPPORTUNITY_PUSH_NOT_REVIEW_ONLY = "OPPORTUNITY_PUSH_NOT_REVIEW_ONLY";
    private static final String OPPORTUNITY_PUSH_POLICY_FAILED = "OPPORTUNITY_PUSH_POLICY_FAILED";
    private static final String OPPORTUNITY_PUSH_NOOP_REVIEW_ONLY = "OPPORTUNITY_PUSH_NOOP_REVIEW_ONLY";
    private static final String PUSH_CHANNEL_DISABLED_BY_DEFAULT = "PUSH_CHANNEL_DISABLED_BY_DEFAULT";
    private static final String RISK_ACTION_GUARD_BLOCKED = "RISK_ACTION_GUARD_BLOCKED";
    private static final String STAMPEDE_OR_EXTREME_STRESS_BLOCKS_DELIVERY =
            "STAMPEDE_OR_EXTREME_STRESS_BLOCKS_DELIVERY";
    private static final String LIQUIDITY_DETERIORATION_BLOCKS_DELIVERY =
            "LIQUIDITY_DETERIORATION_BLOCKS_DELIVERY";
    private static final String WICK_ONLY_REVERSAL_BLOCKS_DELIVERY = "WICK_ONLY_REVERSAL_BLOCKS_DELIVERY";

    @Override
    public OpportunityPushDeliveryDecisionDTO evaluate(
            String symbol,
            OpportunityPushDTO opportunityPush,
            List<String> riskGuardReasons
    ) {
        try {
            if (opportunityPush == null) {
                return OpportunityPushDeliveryDecisionDTO.incomplete(symbol, List.of(OPPORTUNITY_PUSH_MISSING));
            }

            String normalizedSymbol = resolveSymbol(symbol, opportunityPush);
            if (normalizedSymbol == null) {
                return OpportunityPushDeliveryDecisionDTO.incomplete(symbol, List.of(SYMBOL_MISSING));
            }
            if (!isSafeOpportunityPush(opportunityPush)) {
                return OpportunityPushDeliveryDecisionDTO.incomplete(
                        normalizedSymbol,
                        withReason(opportunityPush.getBlockingReasons(), OPPORTUNITY_PUSH_UNSAFE)
                );
            }

            List<String> resolvedRiskGuardReasons = mergeReasons(
                    opportunityPush.getRiskGuardReasons(),
                    riskGuardReasons
            );
            List<String> resolvedBlockingReasons = copy(opportunityPush.getBlockingReasons());

            if (!OpportunityPushStatusEnum.REVIEW_ONLY.equals(opportunityPush.getPushStatus())) {
                return OpportunityPushDeliveryDecisionDTO.disabled(
                        normalizedSymbol,
                        opportunityPush.getSource(),
                        opportunityPush.getPushReasons(),
                        opportunityPush.getAttentionReasons(),
                        resolvedRiskGuardReasons,
                        withReason(resolvedBlockingReasons, OPPORTUNITY_PUSH_NOT_REVIEW_ONLY)
                );
            }

            String riskBlocker = resolveRiskBlocker(mergeReasons(resolvedRiskGuardReasons, resolvedBlockingReasons));
            if (riskBlocker != null) {
                return OpportunityPushDeliveryDecisionDTO.blocked(
                        normalizedSymbol,
                        opportunityPush.getSource(),
                        opportunityPush.getPushReasons(),
                        opportunityPush.getAttentionReasons(),
                        resolvedRiskGuardReasons,
                        withReason(resolvedBlockingReasons, riskBlocker)
                );
            }

            return OpportunityPushDeliveryDecisionDTO.reviewOnly(
                    normalizedSymbol,
                    opportunityPush.getSource(),
                    List.of(OPPORTUNITY_PUSH_NOOP_REVIEW_ONLY, PUSH_CHANNEL_DISABLED_BY_DEFAULT),
                    opportunityPush.getPushReasons(),
                    opportunityPush.getAttentionReasons(),
                    resolvedRiskGuardReasons,
                    resolvedBlockingReasons
            );
        } catch (RuntimeException ex) {
            return OpportunityPushDeliveryDecisionDTO.incomplete(symbol, List.of(OPPORTUNITY_PUSH_POLICY_FAILED));
        }
    }

    private static boolean isSafeOpportunityPush(OpportunityPushDTO opportunityPush) {
        return opportunityPush.isManualReviewRequired()
                && opportunityPush.isNotTradeInstruction()
                && !opportunityPush.isExternalPushSent()
                && !opportunityPush.isReadinessUpgraded()
                && !opportunityPush.isTradingActionCreated()
                && !opportunityPush.isEntryStopTpRrGenerated();
    }

    private static String resolveRiskBlocker(List<String> reasons) {
        for (String reason : reasons) {
            String normalizedReason = normalizeReason(reason);
            if (normalizedReason.contains("STAMPEDE") || normalizedReason.contains("EXTREME_STRESS")) {
                return STAMPEDE_OR_EXTREME_STRESS_BLOCKS_DELIVERY;
            }
            if (normalizedReason.contains("LIQUIDITY_DETERIORATION")
                    || (normalizedReason.contains("LIQUIDITY") && normalizedReason.contains("DETERIOR"))) {
                return LIQUIDITY_DETERIORATION_BLOCKS_DELIVERY;
            }
            if (normalizedReason.contains("WICK_ONLY")
                    || normalizedReason.contains("PIN_BAR")
                    || normalizedReason.contains("PINBAR")) {
                return WICK_ONLY_REVERSAL_BLOCKS_DELIVERY;
            }
            if (normalizedReason.contains("RISK_ACTION_GUARD_BLOCKED")
                    || normalizedReason.contains("PUSH_BLOCKED")
                    || normalizedReason.contains("BLOCK_PUSH")) {
                return RISK_ACTION_GUARD_BLOCKED;
            }
        }
        return null;
    }

    private static String resolveSymbol(String symbol, OpportunityPushDTO opportunityPush) {
        if (symbol != null) {
            return normalize(symbol);
        }
        return normalize(opportunityPush.getSymbol());
    }

    private static String normalize(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeReason(String reason) {
        return reason == null ? "" : reason.trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = copy(reasons);
        if (reason != null && !resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }

    private static List<String> mergeReasons(List<String> first, List<String> second) {
        List<String> resolvedReasons = copy(first);
        for (String reason : copy(second)) {
            if (!resolvedReasons.contains(reason)) {
                resolvedReasons.add(reason);
            }
        }
        return resolvedReasons;
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
