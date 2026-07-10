package org.example.trademodel.dto.ohlcv;

import java.time.Instant;
import java.util.List;

public record OhlcvIngestionBatch(
        String provider,
        String providerMarketType,
        String sourceEndpoint,
        OhlcvSourceState sourceState,
        Instant fetchTime,
        String provenanceVersion,
        int sourceVersion,
        String traceId,
        String ingestionRunId,
        List<OhlcvBarInput> bars
) {
    public OhlcvIngestionBatch {
        bars = bars == null ? List.of() : List.copyOf(bars);
    }
}
