package org.example.trademodel.dto.ohlcv;

import java.time.Instant;

public record PublicProviderHealthSnapshot(
        String provider,
        String status,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        boolean circuitOpen,
        String lastFailureCode
) {
}
