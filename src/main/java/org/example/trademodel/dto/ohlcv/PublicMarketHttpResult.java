package org.example.trademodel.dto.ohlcv;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record PublicMarketHttpResult(
        OhlcvSourceState sourceState,
        String reasonCode,
        int httpStatus,
        Instant fetchTime,
        JsonNode payload
) {
    public boolean ready() {
        return sourceState == OhlcvSourceState.READY && payload != null;
    }
}
