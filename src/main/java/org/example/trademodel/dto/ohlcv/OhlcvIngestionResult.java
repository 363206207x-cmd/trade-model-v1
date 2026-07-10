package org.example.trademodel.dto.ohlcv;

import java.util.List;

public record OhlcvIngestionResult(
        OhlcvSourceState sourceState,
        OhlcvFreshnessStatus freshnessStatus,
        int insertedCount,
        int idempotentCount,
        int rejectedCount,
        List<String> reasonCodes
) {
    public OhlcvIngestionResult {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }

    public boolean accepted() {
        return sourceState == OhlcvSourceState.READY || sourceState == OhlcvSourceState.STALE;
    }

    public boolean ready() {
        return sourceState == OhlcvSourceState.READY && freshnessStatus == OhlcvFreshnessStatus.FRESH;
    }
}
