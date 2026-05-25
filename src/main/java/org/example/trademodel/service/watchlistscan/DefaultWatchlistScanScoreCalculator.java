package org.example.trademodel.service.watchlistscan;

import java.util.List;
import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;

public class DefaultWatchlistScanScoreCalculator implements WatchlistScanScoreCalculator {

    private static final String SCANSCORE_RULE_MISSING = "SCANSCORE_RULE_MISSING";
    private static final String BATCH_ENVELOPE_MISSING = "BATCH_ENVELOPE_MISSING";
    private static final String SYMBOL_MISSING = "SYMBOL_MISSING";
    private static final String SCANSCORE_RESULT_MISSING = "SCANSCORE_RESULT_MISSING";
    private static final String SCANSCORE_RESULT_UNSAFE = "SCANSCORE_RESULT_UNSAFE";
    private static final String SCANSCORE_CALCULATION_FAILED = "SCANSCORE_CALCULATION_FAILED";

    private final WatchlistScanScoreRule scoreRule;

    public DefaultWatchlistScanScoreCalculator(WatchlistScanScoreRule scoreRule) {
        this.scoreRule = scoreRule;
    }

    @Override
    public WatchlistScanScoreDTO calculate(
            String symbol,
            BatchWatchlistScanResultEnvelopeDTO batchEnvelope
    ) {
        if (scoreRule == null) {
            return WatchlistScanScoreDTO.incomplete(symbol, List.of(SCANSCORE_RULE_MISSING));
        }
        if (batchEnvelope == null) {
            return WatchlistScanScoreDTO.incomplete(symbol, List.of(BATCH_ENVELOPE_MISSING));
        }
        if (isBlank(symbol)) {
            return WatchlistScanScoreDTO.incomplete(symbol, List.of(SYMBOL_MISSING));
        }
        try {
            WatchlistScanScoreDTO result = scoreRule.evaluate(symbol, batchEnvelope);
            if (result == null) {
                return WatchlistScanScoreDTO.incomplete(symbol, List.of(SCANSCORE_RESULT_MISSING));
            }
            if (!isSafe(result)) {
                return WatchlistScanScoreDTO.incomplete(symbol, List.of(SCANSCORE_RESULT_UNSAFE));
            }
            return result;
        } catch (RuntimeException ex) {
            return WatchlistScanScoreDTO.incomplete(symbol, List.of(SCANSCORE_CALCULATION_FAILED));
        }
    }

    private static boolean isSafe(WatchlistScanScoreDTO result) {
        return result.isManualReviewRequired()
                && result.isNotTradeInstruction()
                && !result.isOpportunityPushAllowed()
                && !result.isCandidateAttentionAllowed()
                && !result.isPromoteToHomeAllowed()
                && !result.isReadinessUpgraded()
                && !result.isTradingActionCreated()
                && !result.isEntryStopTpRrGenerated();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
