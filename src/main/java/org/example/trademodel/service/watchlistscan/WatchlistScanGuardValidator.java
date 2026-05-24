package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.WatchlistRuntimeSnapshotDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;

public interface WatchlistScanGuardValidator {

    WatchlistScanResultDTO validate(WatchlistRuntimeSnapshotDTO snapshot);
}
