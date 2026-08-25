package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;

public interface PersistedOhlcvQueryService {

    default String primarySourceProvider() {
        return null;
    }

    default String primarySourceMarketType() {
        return null;
    }

    PersistedOhlcvReadinessResult evaluateReadiness(
            String symbol,
            String timeframe,
            int requiredWindowSize,
            long maxReadLagMs
    );

    default PersistedOhlcvReadinessResult evaluateReadinessForSource(
            String symbol,
            String timeframe,
            int requiredWindowSize,
            long maxReadLagMs,
            String provider,
            String providerMarketType
    ) {
        throw new UnsupportedOperationException("SOURCE_OWNED_OHLCV_QUERY_NOT_IMPLEMENTED");
    }
}
