package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;

public interface WatchlistScanScoreRule {

    WatchlistScanScoreDTO evaluate(String symbol, BatchWatchlistScanResultEnvelopeDTO batchEnvelope);
}
