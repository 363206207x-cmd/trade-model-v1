package org.example.trademodel.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic four-timeframe market-bias contract.
 * Each timeframe uses the complete supplied window, from first open to last close.
 */
public final class MarketBiasPolicy {

    private MarketBiasPolicy() {
    }

    public static String classify(List<String[]> bars5m,
                                  List<String[]> bars15m,
                                  List<String[]> bars1h,
                                  List<String[]> bars4h) {
        List<WindowDirection> directions = List.of(
                direction(bars5m), direction(bars15m), direction(bars1h), direction(bars4h));
        if (directions.contains(WindowDirection.MISSING)) {
            return "WAIT";
        }
        long bullish = directions.stream().filter(WindowDirection.BULLISH::equals).count();
        long bearish = directions.stream().filter(WindowDirection.BEARISH::equals).count();
        if (bullish == 4) return "STRONG_BULLISH";
        if (bullish == 3) return "BULLISH";
        if (bearish == 4) return "STRONG_BEARISH";
        if (bearish == 3) return "BEARISH";
        if (bullish > bearish) return "WEAK_BULLISH";
        if (bearish > bullish) return "WEAK_BEARISH";
        return "RANGE";
    }

    public static boolean converged(List<String[]> bars1h, List<String[]> bars4h,
                                    String marketBias) {
        WindowDirection oneHour = direction(bars1h);
        WindowDirection fourHour = direction(bars4h);
        if (oneHour == WindowDirection.MISSING || fourHour == WindowDirection.MISSING
                || oneHour == WindowDirection.FLAT || fourHour == WindowDirection.FLAT
                || oneHour != fourHour) {
            return false;
        }
        return "STRONG_BULLISH".equals(marketBias) || "BULLISH".equals(marketBias)
                || "STRONG_BEARISH".equals(marketBias) || "BEARISH".equals(marketBias);
    }

    public static boolean bullishFamily(String marketBias) {
        return "STRONG_BULLISH".equals(marketBias) || "BULLISH".equals(marketBias)
                || "WEAK_BULLISH".equals(marketBias);
    }

    public static boolean bearishFamily(String marketBias) {
        return "STRONG_BEARISH".equals(marketBias) || "BEARISH".equals(marketBias)
                || "WEAK_BEARISH".equals(marketBias);
    }

    static WindowDirection direction(List<String[]> bars) {
        if (bars == null || bars.isEmpty()) return WindowDirection.MISSING;
        String[] first = bars.get(0);
        String[] last = bars.get(bars.size() - 1);
        if (first == null || last == null || first.length <= 1 || last.length <= 4) {
            return WindowDirection.MISSING;
        }
        try {
            int comparison = new BigDecimal(last[4]).compareTo(new BigDecimal(first[1]));
            if (comparison > 0) return WindowDirection.BULLISH;
            if (comparison < 0) return WindowDirection.BEARISH;
            return WindowDirection.FLAT;
        } catch (RuntimeException ignored) {
            return WindowDirection.MISSING;
        }
    }

    enum WindowDirection {
        BULLISH,
        BEARISH,
        FLAT,
        MISSING
    }
}
