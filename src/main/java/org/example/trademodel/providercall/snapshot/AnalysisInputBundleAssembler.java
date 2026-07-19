package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

@Service
public class AnalysisInputBundleAssembler {
    private final Clock clock;

    public AnalysisInputBundleAssembler() {
        this(Clock.systemUTC());
    }

    public AnalysisInputBundleAssembler(Clock clock) {
        this.clock = clock;
    }

    public AnalysisInputBundle assemble(
            String symbol,
            List<OhlcvSnapshotReference> ohlcv,
            MarketPriceSnapshot currentPrice,
            DerivativesRiskSnapshot derivatives,
            ExternalContextSnapshot externalContext,
            AccountRiskSnapshot accountRisk,
            DataQualitySnapshot dataQuality,
            String ruleVersion,
            String traceId) {
        if (currentPrice == null || currentPrice.metadata() == null
                || currentPrice.metadata().canonicalInstrumentId() == null) {
            throw new IllegalArgumentException("canonical current price snapshot required");
        }
        return assemble(currentPrice.metadata().canonicalInstrumentId(), symbol, ohlcv, currentPrice,
                derivatives, externalContext, accountRisk, dataQuality, ruleVersion, traceId);
    }

    public AnalysisInputBundle assemble(
            CanonicalInstrumentId canonicalInstrumentId,
            String symbol,
            List<OhlcvSnapshotReference> ohlcv,
            MarketPriceSnapshot currentPrice,
            DerivativesRiskSnapshot derivatives,
            ExternalContextSnapshot externalContext,
            AccountRiskSnapshot accountRisk,
            DataQualitySnapshot dataQuality,
            String ruleVersion,
            String traceId) {
        if (canonicalInstrumentId == null) throw new IllegalArgumentException("canonicalInstrumentId is required");
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId is required");
        List<OhlcvSnapshotReference> refs = ohlcv == null ? List.of() : List.copyOf(ohlcv);
        OhlcvSnapshotReference m5 = required(refs, "5m", symbol, canonicalInstrumentId);
        OhlcvSnapshotReference m15 = required(refs, "15m", symbol, canonicalInstrumentId);
        OhlcvSnapshotReference h1 = required(refs, "1h", symbol, canonicalInstrumentId);
        OhlcvSnapshotReference h4 = required(refs, "4h", symbol, canonicalInstrumentId);
        requireInstrument(currentPrice == null ? null : currentPrice.metadata(), canonicalInstrumentId, "price");
        requireTrace(currentPrice == null ? null : currentPrice.metadata().traceId(), traceId, "price");
        requireTrace(m5.metadata().traceId(), traceId, "5m");
        requireTrace(m15.metadata().traceId(), traceId, "15m");
        requireTrace(h1.metadata().traceId(), traceId, "1h");
        requireTrace(h4.metadata().traceId(), traceId, "4h");
        if (derivatives != null) {
            requireTrace(derivatives.metadata().traceId(), traceId, "derivatives");
            requireInstrument(derivatives.metadata(), canonicalInstrumentId, "derivatives");
        }
        if (externalContext != null) requireTrace(externalContext.metadata().traceId(), traceId, "externalContext");
        return new AnalysisInputBundle(symbol, canonicalInstrumentId, m5, m15, h1, h4, currentPrice, derivatives, externalContext,
                accountRisk, dataQuality, ruleVersion, traceId, clock.instant());
    }

    private static OhlcvSnapshotReference required(List<OhlcvSnapshotReference> refs, String timeframe,
                                                   String symbol, CanonicalInstrumentId canonicalInstrumentId) {
        return refs.stream()
                .filter(Objects::nonNull)
                .filter(ref -> timeframe.equals(ref.timeframe()) && symbol.equalsIgnoreCase(ref.symbol()))
                .filter(ref -> ref.metadata() != null
                        && canonicalInstrumentId.equals(ref.metadata().canonicalInstrumentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("authoritative " + timeframe + " OHLCV reference required"));
    }

    private static void requireInstrument(org.example.trademodel.providercall.ProviderSnapshotMetadata metadata,
                                          CanonicalInstrumentId expected,
                                          String source) {
        if (metadata == null || !expected.equals(metadata.canonicalInstrumentId())) {
            throw new IllegalArgumentException(source + " snapshot market identity mismatch");
        }
    }

    private static void requireTrace(String actual, String expected, String source) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(source + " snapshot is not from the locked analysis trace");
        }
    }
}
