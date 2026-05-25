package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.CandidateAttentionDTO;
import org.example.trademodel.dto.watchlistscan.CandidateAttentionStatusEnum;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDTO;

public class DefaultOpportunityPushRule implements OpportunityPushRule {

    private static final String CANDIDATE_ATTENTION_MISSING = "CANDIDATE_ATTENTION_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String CANDIDATE_ATTENTION_UNSAFE = "CANDIDATE_ATTENTION_UNSAFE";
    private static final String CANDIDATE_ATTENTION_NOT_REVIEW_ONLY = "CANDIDATE_ATTENTION_NOT_REVIEW_ONLY";
    private static final String OPPORTUNITY_PUSH_REVIEW_ONLY = "OPPORTUNITY_PUSH_REVIEW_ONLY";
    private static final String OPPORTUNITY_PUSH_RULE_FAILED = "OPPORTUNITY_PUSH_RULE_FAILED";
    private static final String RISK_ACTION_GUARD_BLOCKED = "RISK_ACTION_GUARD_BLOCKED";
    private static final String STAMPEDE_OR_EXTREME_STRESS_BLOCKS_PUSH =
            "STAMPEDE_OR_EXTREME_STRESS_BLOCKS_PUSH";
    private static final String LIQUIDITY_DETERIORATION_BLOCKS_EXECUTION_LIKE_PUSH =
            "LIQUIDITY_DETERIORATION_BLOCKS_EXECUTION_LIKE_PUSH";
    private static final String WICK_ONLY_REVERSAL_BLOCKED = "WICK_ONLY_REVERSAL_BLOCKED";

    @Override
    public OpportunityPushDTO evaluate(
            String symbol,
            CandidateAttentionDTO candidateAttention,
            List<String> riskGuardReasons
    ) {
        try {
            if (candidateAttention == null) {
                return OpportunityPushDTO.incomplete(symbol, List.of(CANDIDATE_ATTENTION_MISSING));
            }
            String normalizedSymbol = normalize(symbol);
            if (normalizedSymbol == null) {
                return OpportunityPushDTO.incomplete(symbol, List.of(SYMBOL_MISSING));
            }
            if (!isSafeCandidate(candidateAttention)) {
                return OpportunityPushDTO.incomplete(normalizedSymbol, List.of(CANDIDATE_ATTENTION_UNSAFE));
            }
            if (!CandidateAttentionStatusEnum.REVIEW_ONLY.equals(candidateAttention.getAttentionStatus())) {
                return OpportunityPushDTO.incomplete(
                        normalizedSymbol,
                        withReason(candidateAttention.getBlockingReasons(), CANDIDATE_ATTENTION_NOT_REVIEW_ONLY)
                );
            }

            List<String> resolvedRiskGuardReasons = copy(riskGuardReasons);
            String riskBlocker = resolveRiskBlocker(resolvedRiskGuardReasons);
            if (riskBlocker != null) {
                return OpportunityPushDTO.blocked(
                        normalizedSymbol,
                        withReason(resolvedRiskGuardReasons, riskBlocker)
                );
            }

            return OpportunityPushDTO.reviewOnly(
                    normalizedSymbol,
                    candidateAttention.getSource(),
                    List.of(OPPORTUNITY_PUSH_REVIEW_ONLY),
                    candidateAttention.getAttentionReasons(),
                    resolvedRiskGuardReasons,
                    candidateAttention.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return OpportunityPushDTO.incomplete(symbol, List.of(OPPORTUNITY_PUSH_RULE_FAILED));
        }
    }

    private static boolean isSafeCandidate(CandidateAttentionDTO candidateAttention) {
        return candidateAttention.isManualReviewRequired()
                && candidateAttention.isNotTradeInstruction()
                && !candidateAttention.isOpportunityPushAllowed()
                && !candidateAttention.isPromoteToHomeAllowed()
                && !candidateAttention.isReadinessUpgraded()
                && !candidateAttention.isTradingActionCreated()
                && !candidateAttention.isEntryStopTpRrGenerated();
    }

    private static String resolveRiskBlocker(List<String> riskGuardReasons) {
        for (String reason : riskGuardReasons) {
            String normalizedReason = normalizeReason(reason);
            if (normalizedReason.contains("STAMPEDE") || normalizedReason.contains("EXTREME_STRESS")) {
                return STAMPEDE_OR_EXTREME_STRESS_BLOCKS_PUSH;
            }
            if (normalizedReason.contains("LIQUIDITY_DETERIORATION")
                    || (normalizedReason.contains("LIQUIDITY") && normalizedReason.contains("DETERIOR"))) {
                return LIQUIDITY_DETERIORATION_BLOCKS_EXECUTION_LIKE_PUSH;
            }
            if (normalizedReason.contains("WICK_ONLY")
                    || normalizedReason.contains("PIN_BAR")
                    || normalizedReason.contains("PINBAR")) {
                return WICK_ONLY_REVERSAL_BLOCKED;
            }
            if (normalizedReason.contains("RISK_ACTION_GUARD_BLOCKED")
                    || normalizedReason.contains("PUSH_BLOCKED")
                    || normalizedReason.contains("BLOCK_PUSH")) {
                return RISK_ACTION_GUARD_BLOCKED;
            }
        }
        return null;
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

    private static <T> List<T> copy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
