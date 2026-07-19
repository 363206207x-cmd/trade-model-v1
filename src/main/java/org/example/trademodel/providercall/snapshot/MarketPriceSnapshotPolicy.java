package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;

import java.math.BigDecimal;

public final class MarketPriceSnapshotPolicy {
    private MarketPriceSnapshotPolicy() {
    }

    public static boolean hasPositivePrice(ProviderCallResult<MarketPriceSnapshot> result) {
        return result != null && result.payload() != null && positive(result.payload().lastPrice());
    }

    public static boolean isFresh(ProviderCallResult<MarketPriceSnapshot> result) {
        return hasPositivePrice(result) && result.metadata() != null
                && result.metadata().freshnessStatus() == SnapshotFreshnessStatus.FRESH;
    }

    public static boolean isStale(ProviderCallResult<MarketPriceSnapshot> result) {
        return hasPositivePrice(result) && result.metadata() != null
                && result.metadata().freshnessStatus() == SnapshotFreshnessStatus.STALE_READABLE;
    }

    public static String failureCode(ProviderCallResult<MarketPriceSnapshot> result) {
        if (isStale(result)) return "QUOTE_STALE";
        if (result != null && result.metadata() != null && result.metadata().errorCode() != null) {
            return result.metadata().errorCode();
        }
        return "QUOTE_UNAVAILABLE";
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
