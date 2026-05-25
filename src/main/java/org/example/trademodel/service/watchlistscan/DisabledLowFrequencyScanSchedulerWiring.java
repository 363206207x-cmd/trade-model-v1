package org.example.trademodel.service.watchlistscan;

import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;

public class DisabledLowFrequencyScanSchedulerWiring {

    private static final String REASON_DISABLED_BY_DEFAULT = "SCHEDULER_WIRING_DISABLED_BY_DEFAULT";
    private static final String REASON_REQUEST_MISSING = "REQUEST_MISSING";
    private static final String REASON_SCHEDULER_WIRING_BLOCKED = "SCHEDULER_WIRING_BLOCKED";
    private static final String REASON_WATCHLIST_POOL_ONLY_REQUIRED = "WATCHLIST_POOL_ONLY_REQUIRED";
    private static final String REASON_ORCHESTRATOR_MISSING = "ORCHESTRATOR_MISSING";
    private static final String REASON_ORCHESTRATOR_FAILED = "ORCHESTRATOR_FAILED";
    private static final String REASON_ORCHESTRATOR_RESULT_MISSING = "ORCHESTRATOR_RESULT_MISSING";
    private static final String REASON_ORCHESTRATOR_RESULT_UNSAFE = "ORCHESTRATOR_RESULT_UNSAFE";

    private final LowFrequencyWatchlistScanOrchestrator orchestrator;
    private final boolean enabled;

    public DisabledLowFrequencyScanSchedulerWiring() {
        this(null, false);
    }

    public DisabledLowFrequencyScanSchedulerWiring(
            LowFrequencyWatchlistScanOrchestrator orchestrator,
            boolean enabled
    ) {
        this.orchestrator = orchestrator;
        this.enabled = enabled;
    }

    public WatchlistScanResultDTO runOnce(RuntimeSourceReadRequestDTO request) {
        String symbol = request == null ? null : request.getSymbol();

        if (!enabled) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_DISABLED_BY_DEFAULT)
            );
        }

        if (request == null) {
            return WatchlistScanResultDTO.incomplete(
                    null,
                    List.of(REASON_REQUEST_MISSING, REASON_SCHEDULER_WIRING_BLOCKED)
            );
        }

        if (!Boolean.TRUE.equals(request.getWatchlistPoolOnly())) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_WATCHLIST_POOL_ONLY_REQUIRED)
            );
        }

        if (orchestrator == null) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_ORCHESTRATOR_MISSING)
            );
        }

        try {
            WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(request);
            if (result == null) {
                return WatchlistScanResultDTO.incomplete(
                        symbol,
                        List.of(REASON_ORCHESTRATOR_RESULT_MISSING)
                );
            }
            if (!isSafeReviewOnlyResult(result)) {
                return WatchlistScanResultDTO.incomplete(
                        symbol,
                        List.of(REASON_ORCHESTRATOR_RESULT_UNSAFE)
                );
            }
            return result;
        } catch (RuntimeException ex) {
            return WatchlistScanResultDTO.incomplete(
                    symbol,
                    List.of(REASON_ORCHESTRATOR_FAILED)
            );
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean isSafeReviewOnlyResult(WatchlistScanResultDTO result) {
        return Boolean.TRUE.equals(result.getManualReviewRequired())
                && Boolean.TRUE.equals(result.getNotTradeInstruction())
                && !Boolean.TRUE.equals(result.getOpportunityPushAllowed())
                && !Boolean.TRUE.equals(result.getCandidateAttentionAllowed())
                && !Boolean.TRUE.equals(result.getPromoteToHomeAllowed())
                && !Boolean.TRUE.equals(result.getReadinessUpgraded())
                && !Boolean.TRUE.equals(result.getTradingActionCreated())
                && !Boolean.TRUE.equals(result.getEntryStopTpRrGenerated());
    }
}
