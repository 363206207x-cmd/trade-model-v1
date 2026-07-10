package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;

public record OhlcvSnapshotReference(
        String symbol,
        String timeframe,
        Long latestClosedBarTimeMs,
        Integer closedBarCount,
        String authoritativeStore,
        ProviderSnapshotMetadata metadata
) {
}
