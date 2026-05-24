package org.example.trademodel.service.watchlistscan;

import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistRuntimeSnapshotDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;

public class DefaultWatchlistScanGuardWiringAssembler implements WatchlistScanGuardWiringAssembler {

    private static final String REASON_GUARD_MISSING = "GUARD_MISSING";
    private static final String REASON_GUARD_RESULT_MISSING = "GUARD_RESULT_MISSING";

    private final WatchlistScanGuardValidator guardValidator;

    public DefaultWatchlistScanGuardWiringAssembler() {
        this(new DefaultWatchlistScanGuardValidator());
    }

    public DefaultWatchlistScanGuardWiringAssembler(WatchlistScanGuardValidator guardValidator) {
        this.guardValidator = guardValidator;
    }

    @Override
    public WatchlistScanResultDTO assembleReviewOnlyResult(WatchlistRuntimeSnapshotDTO snapshot) {
        if (guardValidator == null) {
            return WatchlistScanResultDTO.incomplete(symbolOf(snapshot), List.of(REASON_GUARD_MISSING));
        }

        WatchlistScanResultDTO result = guardValidator.validate(snapshot);
        if (result == null) {
            return WatchlistScanResultDTO.incomplete(symbolOf(snapshot), List.of(REASON_GUARD_RESULT_MISSING));
        }

        return result;
    }

    private static String symbolOf(WatchlistRuntimeSnapshotDTO snapshot) {
        return snapshot == null ? null : snapshot.getSymbol();
    }
}
