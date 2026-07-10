package org.example.trademodel.dto.ohlcv;

import java.time.Instant;

public record OhlcvIngestionHealth(
        String symbol,
        String timeframe,
        OhlcvSourceState state,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        String lastReasonCode,
        OhlcvSourceState nextRunStatus
) {
}
