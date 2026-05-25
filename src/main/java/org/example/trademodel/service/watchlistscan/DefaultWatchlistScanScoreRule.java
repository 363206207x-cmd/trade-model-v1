package org.example.trademodel.service.watchlistscan;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;

public class DefaultWatchlistScanScoreRule implements WatchlistScanScoreRule {

    private static final String BATCH_ENVELOPE_MISSING = "BATCH_ENVELOPE_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String BATCH_ENVELOPE_BLOCKED = "BATCH_ENVELOPE_BLOCKED";
    private static final String BATCH_RESULTS_MISSING = "BATCH_RESULTS_MISSING";
    private static final String SYMBOL_RESULT_MISSING = "SYMBOL_RESULT_MISSING";
    private static final String SYMBOL_RESULT_UNSAFE = "SYMBOL_RESULT_UNSAFE";
    private static final String SCANSCORE_REVIEW_ONLY_SKELETON = "SCANSCORE_REVIEW_ONLY_SKELETON";
    private static final String SCANSCORE_RULE_FAILED = "SCANSCORE_RULE_FAILED";

    private static final List<String> CRITICAL_BATCH_REASONS = List.of(
            "BATCH_SCAN_DISABLED_BY_DEFAULT",
            "REQUESTED_SYMBOLS_MISSING",
            "SINGLE_SYMBOL_ORCHESTRATOR_MISSING",
            "ALL_SYMBOLS_REJECTED",
            "ALL_RESULTS_INCOMPLETE",
            "INCOMPLETE",
            "DISABLED"
    );

    @Override
    public WatchlistScanScoreDTO evaluate(
            String symbol,
            BatchWatchlistScanResultEnvelopeDTO batchEnvelope
    ) {
        try {
            if (batchEnvelope == null) {
                return WatchlistScanScoreDTO.incomplete(symbol, List.of(BATCH_ENVELOPE_MISSING));
            }
            String normalizedSymbol = normalize(symbol);
            if (normalizedSymbol == null) {
                return WatchlistScanScoreDTO.incomplete(symbol, List.of(SYMBOL_MISSING));
            }
            if (hasCriticalBatchReason(batchEnvelope.getBlockingReasons())) {
                return WatchlistScanScoreDTO.incomplete(
                        normalizedSymbol,
                        withReason(batchEnvelope.getBlockingReasons(), BATCH_ENVELOPE_BLOCKED)
                );
            }
            List<WatchlistScanResultDTO> results = batchEnvelope.getResults();
            if (results.isEmpty()) {
                return WatchlistScanScoreDTO.incomplete(normalizedSymbol, List.of(BATCH_RESULTS_MISSING));
            }
            WatchlistScanResultDTO symbolResult = findSymbolResult(normalizedSymbol, results);
            if (symbolResult == null) {
                return WatchlistScanScoreDTO.incomplete(normalizedSymbol, List.of(SYMBOL_RESULT_MISSING));
            }
            if (!isSafeReviewOnlyResult(symbolResult)) {
                return WatchlistScanScoreDTO.incomplete(normalizedSymbol, List.of(SYMBOL_RESULT_UNSAFE));
            }
            return WatchlistScanScoreDTO.reviewOnly(
                    normalizedSymbol,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    batchEnvelope.getSource(),
                    List.of(SCANSCORE_REVIEW_ONLY_SKELETON),
                    symbolResult.getBlockingReasons()
            );
        } catch (RuntimeException ex) {
            return WatchlistScanScoreDTO.incomplete(symbol, List.of(SCANSCORE_RULE_FAILED));
        }
    }

    private static WatchlistScanResultDTO findSymbolResult(
            String symbol,
            List<WatchlistScanResultDTO> results
    ) {
        for (WatchlistScanResultDTO result : results) {
            if (result != null && symbol.equals(normalize(result.getSymbol()))) {
                return result;
            }
        }
        return null;
    }

    private static boolean isSafeReviewOnlyResult(WatchlistScanResultDTO result) {
        return WatchlistScanStatusEnum.REVIEW_ONLY.equals(result.getScanStatus())
                && Boolean.TRUE.equals(result.getManualReviewRequired())
                && Boolean.TRUE.equals(result.getNotTradeInstruction())
                && !Boolean.TRUE.equals(result.getOpportunityPushAllowed())
                && !Boolean.TRUE.equals(result.getCandidateAttentionAllowed())
                && !Boolean.TRUE.equals(result.getPromoteToHomeAllowed())
                && !Boolean.TRUE.equals(result.getReadinessUpgraded())
                && !Boolean.TRUE.equals(result.getTradingActionCreated())
                && !Boolean.TRUE.equals(result.getEntryStopTpRrGenerated());
    }

    private static boolean hasCriticalBatchReason(List<String> blockingReasons) {
        for (String reason : blockingReasons) {
            if (CRITICAL_BATCH_REASONS.contains(reason)) {
                return true;
            }
        }
        return false;
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
