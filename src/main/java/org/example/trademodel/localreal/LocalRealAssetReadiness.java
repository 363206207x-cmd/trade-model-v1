package org.example.trademodel.localreal;

import java.time.Instant;

public record LocalRealAssetReadiness(
        String symbol,
        LocalRealAssetReadinessState state,
        String provider,
        String reasonCode,
        Instant updatedAt
) {
}
