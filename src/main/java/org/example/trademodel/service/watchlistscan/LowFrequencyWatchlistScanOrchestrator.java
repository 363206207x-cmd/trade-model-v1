package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;

public interface LowFrequencyWatchlistScanOrchestrator {

    WatchlistScanResultDTO scanSingleSymbol(RuntimeSourceReadRequestDTO request);
}
