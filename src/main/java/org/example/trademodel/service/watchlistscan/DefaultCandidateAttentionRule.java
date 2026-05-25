package org.example.trademodel.service.watchlistscan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.CandidateAttentionDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreStatusEnum;

public class DefaultCandidateAttentionRule implements CandidateAttentionRule {

    private static final String SCANSCORE_MISSING = "SCANSCORE_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String SCANSCORE_UNSAFE = "SCANSCORE_UNSAFE";
    private static final String SCANSCORE_NOT_REVIEW_ONLY = "SCANSCORE_NOT_REVIEW_ONLY";
    private static final String CANDIDATE_ATTENTION_REVIEW_ONLY = "CANDIDATE_ATTENTION_REVIEW_ONLY";
    private static final String CANDIDATE_ATTENTION_RULE_FAILED = "CANDIDATE_ATTENTION_RULE_FAILED";

    @Override
    public CandidateAttentionDTO evaluate(String symbol, WatchlistScanScoreDTO score) {
        try {
            if (score == null) {
                return CandidateAttentionDTO.incomplete(symbol, List.of(SCANSCORE_MISSING));
            }
            String normalizedSymbol = normalize(symbol);
            if (normalizedSymbol == null) {
                return CandidateAttentionDTO.incomplete(symbol, List.of(SYMBOL_MISSING));
            }
            if (!isSafeScore(score)) {
                return CandidateAttentionDTO.incomplete(normalizedSymbol, List.of(SCANSCORE_UNSAFE));
            }
            if (!WatchlistScanScoreStatusEnum.REVIEW_ONLY.equals(score.getScoreStatus())) {
                return CandidateAttentionDTO.incomplete(
                        normalizedSymbol,
                        withReason(score.getBlockingReasons(), SCANSCORE_NOT_REVIEW_ONLY)
                );
            }
            return CandidateAttentionDTO.reviewOnly(
                    normalizedSymbol,
                    score.getSource(),
                    List.of(CANDIDATE_ATTENTION_REVIEW_ONLY),
                    score.getScoreReasons(),
                    score.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return CandidateAttentionDTO.incomplete(symbol, List.of(CANDIDATE_ATTENTION_RULE_FAILED));
        }
    }

    private static boolean isSafeScore(WatchlistScanScoreDTO score) {
        return score.isManualReviewRequired()
                && score.isNotTradeInstruction()
                && !score.isOpportunityPushAllowed()
                && !score.isCandidateAttentionAllowed()
                && !score.isPromoteToHomeAllowed()
                && !score.isReadinessUpgraded()
                && !score.isTradingActionCreated()
                && !score.isEntryStopTpRrGenerated();
    }

    private static String normalize(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> withReason(List<String> reasons, String reason) {
        List<String> resolvedReasons = new ArrayList<>();
        if (reasons != null) {
            resolvedReasons.addAll(reasons);
        }
        if (reason != null && !resolvedReasons.contains(reason)) {
            resolvedReasons.add(reason);
        }
        return resolvedReasons;
    }
}
