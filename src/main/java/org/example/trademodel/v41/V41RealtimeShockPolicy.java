package org.example.trademodel.v41;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Independent real-time safety layer; it never rewrites structural direction. */
public final class V41RealtimeShockPolicy {
    public static final String VERSION = "V41-REALTIME-SHOCK-1";

    private V41RealtimeShockPolicy() {
    }

    public static Assessment assess(Input input) {
        if (input == null || !positive(input.markPrice()) || !positive(input.atr1h())) {
            return new Assessment(VERSION, input == null ? null : input.structuralDirection(),
                    "DATA_SOURCE_DEGRADED", "SUSPENDED", List.of("BINANCE_PRICE_UNAVAILABLE"), 100, 20);
        }
        List<String> reasons = new ArrayList<>();
        if (!input.binanceFresh()) reasons.add("BINANCE_DATA_STALE");
        if (!input.derivativesFresh()) reasons.add("DERIVATIVES_DATA_UNAVAILABLE");
        if (!input.directionFresh()) {
            reasons.add("DIRECTION_REANALYSIS_REQUIRED");
            return new Assessment(VERSION, input.structuralDirection(), "DIRECTION_STALE",
                    "SUSPENDED", List.copyOf(reasons), 88, 45);
        }

        boolean bullish = input.structuralDirection() != null && input.structuralDirection().contains("BULLISH");
        boolean bearish = input.structuralDirection() != null && input.structuralDirection().contains("BEARISH");
        boolean invalidated = input.invalidationCrossCount() >= 2 && positive(input.invalidationLevel())
                && ((bullish && input.markPrice().compareTo(input.invalidationLevel()) <= 0)
                || (bearish && input.markPrice().compareTo(input.invalidationLevel()) >= 0));
        if (invalidated) {
            reasons.add("STRUCTURE_INVALIDATION_CROSSED");
            return new Assessment(VERSION, input.structuralDirection(), "STRUCTURE_INVALIDATED",
                    "INVALIDATED", List.copyOf(reasons), 100, 20);
        }
        BigDecimal adverseAtr = adverseMove(input, bullish, bearish);
        boolean extreme997 = atLeast(input.adversePercentile(), "99.7");
        boolean extreme999 = atLeast(input.adversePercentile(), "99.9");
        boolean secondary95 = atLeast(input.leveragePercentile(), "95")
                || atLeast(input.liquidityPercentile(), "95") || input.crossSourceDivergence();
        if (extreme999 || (extreme997 && secondary95) || input.crossSourceDivergence()) {
            reasons.add(input.crossSourceDivergence() ? "CROSS_SOURCE_PRICE_DIVERGENCE" : "EXTREME_PRICE_SHOCK");
            return new Assessment(VERSION, input.structuralDirection(),
                    bullish ? "RAPID_DROP" : bearish ? "RAPID_RISE" : "ADVERSE_MOVE_WARNING",
                    "SUSPENDED", List.copyOf(reasons), 95, 40);
        }
        boolean rapidAbsoluteMove = max(abs(input.return1mPct()), abs(input.return5mPct()))
                .compareTo(new BigDecimal("0.40")) >= 0;
        if (atLeast(input.adversePercentile(), "99")
                || adverseAtr.compareTo(new BigDecimal("0.75")) >= 0 || rapidAbsoluteMove) {
            reasons.add("ADVERSE_MOVE_WARNING");
            return new Assessment(VERSION, input.structuralDirection(),
                    bullish ? "RAPID_DROP" : bearish ? "RAPID_RISE" : "ADVERSE_MOVE_WARNING",
                    "SUSPENDED", List.copyOf(reasons), 78, 55);
        }
        if (!input.binanceFresh()) {
            return new Assessment(VERSION, input.structuralDirection(), "DATA_SOURCE_DEGRADED",
                    "SUSPENDED", List.copyOf(reasons), 85, 45);
        }
        return new Assessment(VERSION, input.structuralDirection(), "NORMAL", "ACTIVE",
                List.copyOf(reasons), reasons.isEmpty() ? 0 : 45, 100);
    }

    private static BigDecimal adverseMove(Input input, boolean bullish, boolean bearish) {
        BigDecimal pct = bullish ? min(input.return1mPct(), input.return5mPct()).negate()
                : bearish ? max(input.return1mPct(), input.return5mPct())
                : max(input.return1mPct().abs(), input.return5mPct().abs());
        return input.markPrice().multiply(pct.abs()).divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP)
                .divide(input.atr1h(), 10, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b == null ? BigDecimal.ZERO : b;
        return b == null ? a : a.min(b);
    }

    private static BigDecimal max(BigDecimal a, BigDecimal b) {
        if (a == null) return b == null ? BigDecimal.ZERO : b;
        return b == null ? a : a.max(b);
    }

    private static BigDecimal abs(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.abs();
    }

    private static boolean atLeast(BigDecimal value, String threshold) {
        return value != null && value.compareTo(new BigDecimal(threshold)) >= 0;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public record Input(String structuralDirection, BigDecimal invalidationLevel,
                        BigDecimal markPrice, BigDecimal atr1h,
                        BigDecimal return1mPct, BigDecimal return5mPct,
                        BigDecimal adversePercentile, BigDecimal leveragePercentile,
                        BigDecimal liquidityPercentile, boolean derivativesFresh,
                        boolean binanceFresh, boolean directionFresh, boolean crossSourceDivergence,
                        int invalidationCrossCount) {
    }

    public record Assessment(String version, String structuralDirection, String realtimeState,
                             String effectiveExecutionState, List<String> reasonCodes, int severityScore,
                             int confidenceCap) {
    }
}
