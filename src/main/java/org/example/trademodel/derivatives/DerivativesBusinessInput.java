package org.example.trademodel.derivatives;

import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;

import java.math.BigDecimal;
import java.util.Map;

public record DerivativesBusinessInput(
        String symbol,
        String baseDirection,
        BigDecimal currentPrice,
        BigDecimal comparisonPrice,
        boolean volumeConfirmed,
        Map<String, String> timeframeDirections,
        boolean currentPriceFresh,
        Integer dataQualityScore,
        boolean accountRiskAllowed,
        boolean planBoundaryComplete,
        boolean positionOpen,
        AssetStateEnum currentState,
        DerivativesRiskSnapshot snapshot,
        String traceId,
        String analysisId,
        String ruleVersion
) {
    public DerivativesBusinessInput {
        timeframeDirections = timeframeDirections == null ? Map.of() : Map.copyOf(timeframeDirections);
    }
}
