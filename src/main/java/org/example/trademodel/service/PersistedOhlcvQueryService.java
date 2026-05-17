package org.example.trademodel.service;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;

public interface PersistedOhlcvQueryService {

    PersistedOhlcvReadinessResult evaluateReadiness(
            String symbol,
            String timeframe,
            int requiredWindowSize,
            long maxReadLagMs
    );
}
