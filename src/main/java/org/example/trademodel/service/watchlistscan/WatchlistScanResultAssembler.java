package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;

public interface WatchlistScanResultAssembler {

    WatchlistScanResultDTO assemble(RuntimeSourceReadResultDTO runtimeSourceReadResult);
}
