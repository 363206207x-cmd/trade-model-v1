package org.example.trademodel.positionmonitor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class PositionMonitorPolicy {
    public static final BigDecimal NEAR_STOP_LOSS_DISTANCE_RATIO = new BigDecimal("0.02");
    public static final BigDecimal NEAR_TAKE_PROFIT_DISTANCE_RATIO = new BigDecimal("0.02");

    private PositionMonitorPolicy() {
    }

    public static boolean stopLossBreached(String side, BigDecimal currentPrice, BigDecimal stopLoss) {
        if (!hasPositive(currentPrice) || !hasPositive(stopLoss)) {
            return false;
        }
        String normalizedSide = normalize(side);
        if ("LONG".equals(normalizedSide)) {
            return currentPrice.compareTo(stopLoss) <= 0;
        }
        if ("SHORT".equals(normalizedSide)) {
            return currentPrice.compareTo(stopLoss) >= 0;
        }
        return false;
    }

    public static boolean nearStopLoss(String side, BigDecimal currentPrice, BigDecimal stopLoss) {
        if (!hasPositive(currentPrice) || !hasPositive(stopLoss) || stopLossBreached(side, currentPrice, stopLoss)) {
            return false;
        }
        String normalizedSide = normalize(side);
        BigDecimal distance;
        if ("LONG".equals(normalizedSide)) {
            distance = currentPrice.subtract(stopLoss);
        } else if ("SHORT".equals(normalizedSide)) {
            distance = stopLoss.subtract(currentPrice);
        } else {
            return false;
        }
        return ratio(distance, currentPrice).compareTo(NEAR_STOP_LOSS_DISTANCE_RATIO) <= 0;
    }

    public static boolean takeProfitReached(String side, BigDecimal currentPrice, BigDecimal takeProfit) {
        if (!hasPositive(currentPrice) || !hasPositive(takeProfit)) {
            return false;
        }
        String normalizedSide = normalize(side);
        if ("LONG".equals(normalizedSide)) {
            return currentPrice.compareTo(takeProfit) >= 0;
        }
        if ("SHORT".equals(normalizedSide)) {
            return currentPrice.compareTo(takeProfit) <= 0;
        }
        return false;
    }

    public static boolean nearTakeProfit(String side, BigDecimal currentPrice, BigDecimal takeProfit) {
        if (!hasPositive(currentPrice) || !hasPositive(takeProfit) || takeProfitReached(side, currentPrice, takeProfit)) {
            return false;
        }
        String normalizedSide = normalize(side);
        BigDecimal distance;
        if ("LONG".equals(normalizedSide)) {
            distance = takeProfit.subtract(currentPrice);
        } else if ("SHORT".equals(normalizedSide)) {
            distance = currentPrice.subtract(takeProfit);
        } else {
            return false;
        }
        return ratio(distance, currentPrice).compareTo(NEAR_TAKE_PROFIT_DISTANCE_RATIO) <= 0;
    }

    public static int riskRank(String riskLevel) {
        String normalized = normalize(riskLevel);
        if ("HIGH".equals(normalized)) {
            return 3;
        }
        if ("MEDIUM".equals(normalized)) {
            return 2;
        }
        if ("LOW".equals(normalized)) {
            return 1;
        }
        return 0;
    }

    public static String normalizeRiskLevel(String riskLevel) {
        String normalized = normalize(riskLevel);
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized)) {
            return normalized;
        }
        return "HIGH";
    }

    private static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (!hasPositive(numerator) || !hasPositive(denominator)) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 8, RoundingMode.HALF_UP).abs();
    }

    private static boolean hasPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
