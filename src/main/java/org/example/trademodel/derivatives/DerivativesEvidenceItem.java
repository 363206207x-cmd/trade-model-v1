package org.example.trademodel.derivatives;

import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record DerivativesEvidenceItem(
        DerivativesEvidenceType evidenceType,
        String symbol,
        String direction,
        BigDecimal strength,
        BigDecimal confidence,
        BigDecimal currentValue,
        BigDecimal comparisonValue,
        String timeframe,
        String provider,
        Instant providerDataTime,
        Instant fetchTime,
        UnifiedSourceStatus sourceStatus,
        SnapshotFreshnessStatus freshnessStatus,
        String sourceField,
        String reasonCode,
        String traceId,
        String analysisId,
        String ruleVersion
) {
}
