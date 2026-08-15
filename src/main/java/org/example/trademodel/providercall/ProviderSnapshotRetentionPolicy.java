package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Duration;

/** Cache retention is a dataset policy, not a property of whichever consumer refreshed first. */
@Service
public class ProviderSnapshotRetentionPolicy {

    public Duration staleRetention(ProviderDatasetType datasetType) {
        if (datasetType == null) throw new IllegalArgumentException("datasetType is required");
        return switch (datasetType) {
            case PRICE -> Duration.ofMinutes(2);
            case OHLCV -> Duration.ofMinutes(10);
            case DERIVATIVES, FUNDING, OPEN_INTEREST, COINGLASS_OPEN_INTEREST, COINGLASS_FUNDING,
                    COINGLASS_LIQUIDATION, COINGLASS_LONG_SHORT_RATIO -> Duration.ofMinutes(3);
            case EXTERNAL_CONTEXT -> Duration.ofMinutes(30);
            case AI_REVIEW -> Duration.ofMinutes(10);
        };
    }
}
