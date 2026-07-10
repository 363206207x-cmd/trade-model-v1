package org.example.trademodel.derivatives;

import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DerivativesBusinessAssessment(
        String symbol,
        String baseDirection,
        UnifiedSourceStatus sourceStatus,
        SnapshotFreshnessStatus freshnessStatus,
        String evidenceAvailability,
        Instant providerDataTime,
        Instant fetchTime,
        List<DerivativesEvidenceItem> evidence,
        Map<String, Double> scoreDeltas,
        int dataQualityDiscount,
        int confidenceAdjustment,
        String riskAdjustment,
        String planMode,
        AssetStateEnum opportunityState,
        String pushMode,
        boolean confirmEligible,
        boolean pushRecheckAllowed,
        boolean needsRevalidation,
        boolean hotResetCandidate,
        int driverConflictDelta,
        int executionInstabilityDelta,
        int microstructureTrapDelta,
        int causeEffectDivergenceDelta,
        List<String> availableDatasets,
        List<String> missingDatasets,
        List<String> degradedDatasets,
        List<String> reasonCodes,
        List<String> configFallbackReasons,
        String traceId,
        String analysisId,
        String ruleVersion,
        boolean positionOpen
) {
    public DerivativesBusinessAssessment {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        scoreDeltas = scoreDeltas == null ? Map.of() : Map.copyOf(scoreDeltas);
        availableDatasets = availableDatasets == null ? List.of() : List.copyOf(availableDatasets);
        missingDatasets = missingDatasets == null ? List.of() : List.copyOf(missingDatasets);
        degradedDatasets = degradedDatasets == null ? List.of() : List.copyOf(degradedDatasets);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        configFallbackReasons = configFallbackReasons == null ? List.of() : List.copyOf(configFallbackReasons);
    }

    public boolean blocksConfirmPlan() {
        return !confirmEligible || "WARNING_ONLY".equals(planMode) || "PREPARE_ONLY".equals(planMode);
    }

    public boolean isHighRisk() {
        return "HIGH".equals(riskAdjustment);
    }
}
