package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;

public interface WatchlistScanScoreCalculator {

    WatchlistScanScoreDTO calculate(String symbol, BatchWatchlistScanResultEnvelopeDTO batchEnvelope);
}
