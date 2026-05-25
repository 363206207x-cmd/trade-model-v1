package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;

import java.util.List;

public interface BatchWatchlistScanOrchestrator {

    BatchWatchlistScanResultEnvelopeDTO scanBatch(
            String batchId,
            String requestId,
            String source,
            List<String> requestedSymbols
    );
}
