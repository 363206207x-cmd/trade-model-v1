package org.example.trademodel.positionmonitor;

import org.example.trademodel.enums.MarketBiasEnum;
import org.example.trademodel.positionmonitorlog.PositionReversalStatusEnum;

import java.util.Locale;

/** Maps the current rule-layer direction to the frozen position-reversal contract. */
public final class PositionReversalEvaluator {

    public Assessment evaluate(String positionSide, String currentRuleDirection) {
        String side = normalize(positionSide);
        if (!"LONG".equals(side) && !"SHORT".equals(side)) {
            throw new IllegalArgumentException("position side must be LONG or SHORT");
        }
        MarketBiasEnum bias = parseBias(currentRuleDirection);
        if (bias == null) {
            return Assessment.unavailable();
        }
        if ("LONG".equals(side)) {
            return switch (bias) {
                case WEAK_BEARISH -> Assessment.available(PositionReversalStatusEnum.WEAK_REVERSAL);
                case BEARISH, STRONG_BEARISH ->
                        Assessment.available(PositionReversalStatusEnum.STRONG_REVERSAL);
                default -> Assessment.available(PositionReversalStatusEnum.NO_REVERSAL);
            };
        }
        return switch (bias) {
            case WEAK_BULLISH -> Assessment.available(PositionReversalStatusEnum.WEAK_REVERSAL);
            case BULLISH, STRONG_BULLISH -> Assessment.available(PositionReversalStatusEnum.STRONG_REVERSAL);
            default -> Assessment.available(PositionReversalStatusEnum.NO_REVERSAL);
        };
    }

    private static MarketBiasEnum parseBias(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return MarketBiasEnum.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public record Assessment(boolean sourceAvailable, PositionReversalStatusEnum status) {
        private static Assessment available(PositionReversalStatusEnum status) {
            return new Assessment(true, status);
        }

        private static Assessment unavailable() {
            return new Assessment(false, null);
        }
    }
}
