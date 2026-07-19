package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.time.Instant;

public record AnalysisInputBundle(
        String symbol,
        CanonicalInstrumentId canonicalInstrumentId,
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
    public AnalysisInputBundle(
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
            Instant analysisTime) {
        this(symbol, currentPrice == null || currentPrice.metadata() == null
                        ? null : currentPrice.metadata().canonicalInstrumentId(),
                ohlcv5m, ohlcv15m, ohlcv1h, ohlcv4h, currentPrice, derivatives, externalContext,
                accountRisk, dataQuality, ruleVersion, traceId, analysisTime);
    }
}
