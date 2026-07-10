package org.example.trademodel.providercall.snapshot;

import java.time.Instant;

public record AnalysisInputBundle(
        String symbol,
        OhlcvSnapshotReference ohlcv5m,
        OhlcvSnapshotReference ohlcv15m,
        OhlcvSnapshotReference ohlcv1h,
        OhlcvSnapshotReference ohlcv4h,
        MarketPriceSnapshot currentPrice,
        DerivativesRiskSnapshot derivatives,
        ExternalContextSnapshot externalContext,
        AccountRiskSnapshot accountRisk,
        DataQualitySnapshot dataQuality,
        String ruleVersion,
        String traceId,
        Instant analysisTime
) {
}
