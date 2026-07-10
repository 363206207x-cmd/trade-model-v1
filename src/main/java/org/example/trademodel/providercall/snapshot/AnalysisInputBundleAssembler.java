package org.example.trademodel.providercall.snapshot;

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
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId is required");
        List<OhlcvSnapshotReference> refs = ohlcv == null ? List.of() : List.copyOf(ohlcv);
        OhlcvSnapshotReference m5 = required(refs, "5m", symbol);
        OhlcvSnapshotReference m15 = required(refs, "15m", symbol);
        OhlcvSnapshotReference h1 = required(refs, "1h", symbol);
        OhlcvSnapshotReference h4 = required(refs, "4h", symbol);
        requireTrace(currentPrice == null ? null : currentPrice.metadata().traceId(), traceId, "price");
        requireTrace(m5.metadata().traceId(), traceId, "5m");
        requireTrace(m15.metadata().traceId(), traceId, "15m");
        requireTrace(h1.metadata().traceId(), traceId, "1h");
        requireTrace(h4.metadata().traceId(), traceId, "4h");
        if (derivatives != null) requireTrace(derivatives.metadata().traceId(), traceId, "derivatives");
        if (externalContext != null) requireTrace(externalContext.metadata().traceId(), traceId, "externalContext");
        return new AnalysisInputBundle(symbol, m5, m15, h1, h4, currentPrice, derivatives, externalContext,
                accountRisk, dataQuality, ruleVersion, traceId, clock.instant());
    }

    private static OhlcvSnapshotReference required(List<OhlcvSnapshotReference> refs, String timeframe, String symbol) {
        return refs.stream()
                .filter(Objects::nonNull)
                .filter(ref -> timeframe.equals(ref.timeframe()) && symbol.equalsIgnoreCase(ref.symbol()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("authoritative " + timeframe + " OHLCV reference required"));
    }

    private static void requireTrace(String actual, String expected, String source) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(source + " snapshot is not from the locked analysis trace");
        }
    }
}
