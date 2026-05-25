package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DefaultBatchWatchlistScanOrchestrator implements BatchWatchlistScanOrchestrator {

    private static final String REQUEST_REASON = "batch-watchlist-scan";
    private static final String BATCH_SCAN_DISABLED_BY_DEFAULT = "BATCH_SCAN_DISABLED_BY_DEFAULT";
    private static final String REQUESTED_SYMBOLS_MISSING = "REQUESTED_SYMBOLS_MISSING";
    private static final String SINGLE_SYMBOL_ORCHESTRATOR_MISSING = "SINGLE_SYMBOL_ORCHESTRATOR_MISSING";
    private static final String INVALID_SYMBOL = "INVALID_SYMBOL";
    private static final String DUPLICATE_SYMBOL = "DUPLICATE_SYMBOL";
    private static final String ALL_SYMBOLS_REJECTED = "ALL_SYMBOLS_REJECTED";
    private static final String SINGLE_SYMBOL_RESULT_MISSING = "SINGLE_SYMBOL_RESULT_MISSING";
    private static final String SINGLE_SYMBOL_ORCHESTRATOR_FAILED = "SINGLE_SYMBOL_ORCHESTRATOR_FAILED";
    private static final String SINGLE_SYMBOL_RESULT_UNSAFE = "SINGLE_SYMBOL_RESULT_UNSAFE";
    private static final String BLOCKED_NOT_WATCHLIST = "BLOCKED_NOT_WATCHLIST";
    private static final String ALL_RESULTS_INCOMPLETE = "ALL_RESULTS_INCOMPLETE";

    private final LowFrequencyWatchlistScanOrchestrator singleSymbolOrchestrator;
    private final boolean enabled;

    public DefaultBatchWatchlistScanOrchestrator(
            LowFrequencyWatchlistScanOrchestrator singleSymbolOrchestrator,
            boolean enabled
    ) {
        this.singleSymbolOrchestrator = singleSymbolOrchestrator;
        this.enabled = enabled;
    }

    @Override
    public BatchWatchlistScanResultEnvelopeDTO scanBatch(
            String batchId,
            String requestId,
            String source,
            List<String> requestedSymbols
    ) {
        if (!enabled) {
            return BatchWatchlistScanResultEnvelopeDTO.incomplete(
                    batchId,
                    requestId,
                    source,
                    requestedSymbols,
                    List.of(BATCH_SCAN_DISABLED_BY_DEFAULT)
            );
        }
        if (requestedSymbols == null || requestedSymbols.isEmpty()) {
            return BatchWatchlistScanResultEnvelopeDTO.incomplete(
                    batchId,
                    requestId,
                    source,
                    requestedSymbols,
                    List.of(REQUESTED_SYMBOLS_MISSING)
            );
        }
        if (singleSymbolOrchestrator == null) {
            return BatchWatchlistScanResultEnvelopeDTO.incomplete(
                    batchId,
                    requestId,
                    source,
                    requestedSymbols,
                    List.of(SINGLE_SYMBOL_ORCHESTRATOR_MISSING)
            );
        }

        BatchSymbols symbols = normalizeSymbols(requestedSymbols);
        if (symbols.acceptedSymbols.isEmpty()) {
            List<String> reasons = new ArrayList<>(symbols.blockingReasons);
            addReason(reasons, ALL_SYMBOLS_REJECTED);
            return BatchWatchlistScanResultEnvelopeDTO.of(
                    batchId,
                    requestId,
                    source,
                    requestedSymbols,
                    symbols.acceptedSymbols,
                    symbols.rejectedSymbols,
                    symbols.missingSymbols,
                    symbols.duplicateSymbols,
                    symbols.invalidSymbols,
                    symbols.nonWatchlistSymbols,
                    List.of(),
                    reasons
            );
        }

        List<WatchlistScanResultDTO> results = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>(symbols.blockingReasons);
        for (String symbol : symbols.acceptedSymbols) {
            WatchlistScanResultDTO result = scanSingleSymbol(symbol, source);
            if (isNonWatchlist(result)) {
                addSymbol(symbols.nonWatchlistSymbols, symbol);
                addSymbol(symbols.rejectedSymbols, symbol);
                addReason(blockingReasons, BLOCKED_NOT_WATCHLIST);
            }
            results.add(result);
        }

        if (allResultsIncomplete(results)) {
            addReason(blockingReasons, ALL_RESULTS_INCOMPLETE);
        }

        return BatchWatchlistScanResultEnvelopeDTO.of(
                batchId,
                requestId,
                source,
                requestedSymbols,
                symbols.acceptedSymbols,
                symbols.rejectedSymbols,
                symbols.missingSymbols,
                symbols.duplicateSymbols,
                symbols.invalidSymbols,
                symbols.nonWatchlistSymbols,
                results,
                blockingReasons
        );
    }

    private WatchlistScanResultDTO scanSingleSymbol(String symbol, String source) {
        try {
            RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                    symbol,
                    source,
                    REQUEST_REASON
            );
            WatchlistScanResultDTO result = singleSymbolOrchestrator.scanSingleSymbol(request);
            if (result == null) {
                return WatchlistScanResultDTO.incomplete(symbol, List.of(SINGLE_SYMBOL_RESULT_MISSING));
            }
            if (!isSafeResult(result)) {
                return WatchlistScanResultDTO.incomplete(symbol, List.of(SINGLE_SYMBOL_RESULT_UNSAFE));
            }
            return result;
        } catch (RuntimeException ex) {
            return WatchlistScanResultDTO.incomplete(symbol, List.of(SINGLE_SYMBOL_ORCHESTRATOR_FAILED));
        }
    }

    private static BatchSymbols normalizeSymbols(List<String> requestedSymbols) {
        Set<String> seen = new LinkedHashSet<>();
        BatchSymbols symbols = new BatchSymbols();
        for (String requestedSymbol : requestedSymbols) {
            String normalized = normalize(requestedSymbol);
            if (normalized == null) {
                symbols.missingSymbols.add(requestedSymbol);
                symbols.invalidSymbols.add(requestedSymbol);
                symbols.rejectedSymbols.add(requestedSymbol);
                addReason(symbols.blockingReasons, INVALID_SYMBOL);
                continue;
            }
            if (!seen.add(normalized)) {
                symbols.duplicateSymbols.add(normalized);
                symbols.rejectedSymbols.add(normalized);
                addReason(symbols.blockingReasons, DUPLICATE_SYMBOL);
                continue;
            }
            symbols.acceptedSymbols.add(normalized);
        }
        return symbols;
    }

    private static String normalize(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean isSafeResult(WatchlistScanResultDTO result) {
        return Boolean.TRUE.equals(result.getManualReviewRequired())
                && Boolean.TRUE.equals(result.getNotTradeInstruction())
                && !Boolean.TRUE.equals(result.getOpportunityPushAllowed())
                && !Boolean.TRUE.equals(result.getCandidateAttentionAllowed())
                && !Boolean.TRUE.equals(result.getPromoteToHomeAllowed())
                && !Boolean.TRUE.equals(result.getReadinessUpgraded())
                && !Boolean.TRUE.equals(result.getTradingActionCreated())
                && !Boolean.TRUE.equals(result.getEntryStopTpRrGenerated());
    }

    private static boolean isNonWatchlist(WatchlistScanResultDTO result) {
        return result != null
                && (WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST.equals(result.getScanStatus())
                || result.getBlockingReasons().contains(BLOCKED_NOT_WATCHLIST));
    }

    private static boolean allResultsIncomplete(List<WatchlistScanResultDTO> results) {
        if (results.isEmpty()) {
            return false;
        }
        for (WatchlistScanResultDTO result : results) {
            if (!WatchlistScanStatusEnum.INCOMPLETE.equals(result.getScanStatus())) {
                return false;
            }
        }
        return true;
    }

    private static void addReason(List<String> reasons, String reason) {
        if (reason != null && !reasons.contains(reason)) {
            reasons.add(reason);
        }
    }

    private static void addSymbol(List<String> symbols, String symbol) {
        if (symbol != null && !symbols.contains(symbol)) {
            symbols.add(symbol);
        }
    }

    private static class BatchSymbols {
        private final List<String> acceptedSymbols = new ArrayList<>();
        private final List<String> rejectedSymbols = new ArrayList<>();
        private final List<String> missingSymbols = new ArrayList<>();
        private final List<String> duplicateSymbols = new ArrayList<>();
        private final List<String> invalidSymbols = new ArrayList<>();
        private final List<String> nonWatchlistSymbols = new ArrayList<>();
        private final List<String> blockingReasons = new ArrayList<>();
    }
}
