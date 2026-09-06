package org.example.trademodel.v41;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Pure rule owner for the frozen closed-4h/closed-1h structural direction. */
public final class V41StructuralDirectionEngine {
    public static final String VERSION = "V41-DIRECTION-4H1H-2";
    private static final BigDecimal FOUR_HOUR_WEIGHT = new BigDecimal("0.55");
    private static final BigDecimal ONE_HOUR_WEIGHT = new BigDecimal("0.45");

    private V41StructuralDirectionEngine() {
    }

    public static Assessment assess(List<String[]> closed1h, List<String[]> closed4h) {
        List<Bar> oneHour = parse(closed1h);
        List<Bar> fourHour = parse(closed4h);
        if (oneHour.size() < 24 || fourHour.size() < 20) {
            return new Assessment(VERSION, "WAIT", "INSUFFICIENT_DATA", null, null, null,
                    null, null, "闭合1小时或4小时样本不足", fingerprint(oneHour, fourHour),
                    new Calibration(0, null, null, null));
        }

        BigDecimal atr1h = atr(last(oneHour, 24), 14);
        BigDecimal atr4h = atr(last(fourHour, 20), 14);
        if (!positive(atr1h) || !positive(atr4h)) {
            return new Assessment(VERSION, "WAIT", "INSUFFICIENT_VOLATILITY", null, null, null,
                    null, null, "稳健波动率样本不足", fingerprint(oneHour, fourHour),
                    new Calibration(0, null, null, null));
        }

        List<Bar> trendWindow = last(fourHour, 20);
        BigDecimal slope4h = robustSlope(last(fourHour, 12), atr4h);
        BigDecimal structure4h = structureScore(trendWindow);
        BigDecimal center4h = centerAndSlopeScore(trendWindow, atr4h);
        BigDecimal trend4h = weighted(slope4h, "0.50", structure4h, "0.30", center4h, "0.20");

        List<Bar> stateWindow = last(oneHour, 24);
        BigDecimal momentum1h = momentumScore(stateWindow, atr1h);
        BigDecimal structure1h = structureScore(last(oneHour, 12));
        BigDecimal pullback1h = pullbackScore(last(oneHour, 12), atr1h, momentum1h);
        BigDecimal acceleration1h = accelerationScore(stateWindow, atr1h);
        BigDecimal state1h = weighted(momentum1h, "0.35", structure1h, "0.30",
                pullback1h, "0.20", acceleration1h, "0.15");

        BigDecimal score = trend4h.multiply(FOUR_HOUR_WEIGHT)
                .add(state1h.multiply(ONE_HOUR_WEIGHT));
        boolean coreConflict = trend4h.signum() != 0 && state1h.signum() != 0
                && trend4h.signum() != state1h.signum()
                && trend4h.subtract(state1h).abs().compareTo(new BigDecimal("35")) >= 0;
        String direction = coreConflict ? "WAIT"
                : classifyWithHysteresis(score, trend4h, state1h, oneHour, atr1h);
        String state = coreConflict ? "MULTI_TIMEFRAME_CONFLICT" : "READY";
        String context = coreConflict
                ? (state1h.signum() < 0 ? "1小时偏空·4小时趋势偏多" : "1小时偏多·4小时趋势偏空")
                : timeframeContext(state1h, trend4h);
        BigDecimal invalidation = invalidation(direction, last(oneHour, 12), atr1h);
        return new Assessment(VERSION, direction, state, score, trend4h, state1h,
                atr1h.setScale(8, RoundingMode.HALF_UP), invalidation, context,
                fingerprint(oneHour, fourHour), walkForwardCalibration(oneHour));
    }

    public static String classify(BigDecimal score) {
        if (score == null) return "WAIT";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "STRONG_BULLISH";
        if (score.compareTo(new BigDecimal("35")) >= 0) return "BULLISH";
        if (score.compareTo(new BigDecimal("15")) >= 0) return "WEAK_BULLISH";
        if (score.compareTo(new BigDecimal("-14")) >= 0) return "RANGE";
        if (score.compareTo(new BigDecimal("-35")) > 0) return "WEAK_BEARISH";
        if (score.compareTo(new BigDecimal("-70")) > 0) return "BEARISH";
        return "STRONG_BEARISH";
    }

    private static String classifyWithHysteresis(BigDecimal score, BigDecimal trend4h,
                                                 BigDecimal state1h, List<Bar> oneHour,
                                                 BigDecimal currentAtr) {
        String current = classify(score);
        if (oneHour.size() < 25 || !positive(currentAtr)) {
            return downgradeUnconfirmedStrong(current, trend4h, state1h, null);
        }
        List<Bar> previousBars = oneHour.subList(0, oneHour.size() - 1);
        BigDecimal previousAtr = atr(last(previousBars, 24), 14);
        if (!positive(previousAtr)) return downgradeUnconfirmedStrong(current, trend4h, state1h, null);
        List<Bar> previousWindow = last(previousBars, 24);
        BigDecimal previousState = weighted(momentumScore(previousWindow, previousAtr), "0.35",
                structureScore(last(previousBars, 12)), "0.30",
                pullbackScore(last(previousBars, 12), previousAtr,
                        momentumScore(previousWindow, previousAtr)), "0.20",
                accelerationScore(previousWindow, previousAtr), "0.15");
        BigDecimal previousScore = trend4h.multiply(FOUR_HOUR_WEIGHT)
                .add(previousState.multiply(ONE_HOUR_WEIGHT));
        String previous = classify(previousScore);
        boolean structureBroken = structureBroken(current, oneHour);

        if ("STRONG_BULLISH".equals(current)) {
            return "STRONG_BULLISH".equals(previous) && trend4h.signum() > 0 && state1h.signum() > 0
                    && !structureBroken ? current : "BULLISH";
        }
        if ("STRONG_BEARISH".equals(current)) {
            return "STRONG_BEARISH".equals(previous) && trend4h.signum() < 0 && state1h.signum() < 0
                    && !structureBroken ? current : "BEARISH";
        }
        if ("STRONG_BULLISH".equals(previous) && score.compareTo(new BigDecimal("60")) >= 0
                && trend4h.signum() > 0 && state1h.signum() > 0 && !structureBroken) {
            return "STRONG_BULLISH";
        }
        if ("STRONG_BEARISH".equals(previous) && score.compareTo(new BigDecimal("-60")) <= 0
                && trend4h.signum() < 0 && state1h.signum() < 0 && !structureBroken) {
            return "STRONG_BEARISH";
        }
        if (("BULLISH".equals(previous) || "STRONG_BULLISH".equals(previous))
                && score.compareTo(new BigDecimal("25")) >= 0 && !structureBroken) {
            return "BULLISH";
        }
        if (("BEARISH".equals(previous) || "STRONG_BEARISH".equals(previous))
                && score.compareTo(new BigDecimal("-25")) <= 0 && !structureBroken) {
            return "BEARISH";
        }
        return current;
    }

    private static String downgradeUnconfirmedStrong(String direction, BigDecimal trend4h,
                                                     BigDecimal state1h, String fallback) {
        if ("STRONG_BULLISH".equals(direction)) {
            return trend4h.signum() > 0 && state1h.signum() > 0 ? "BULLISH" : "WAIT";
        }
        if ("STRONG_BEARISH".equals(direction)) {
            return trend4h.signum() < 0 && state1h.signum() < 0 ? "BEARISH" : "WAIT";
        }
        return fallback == null ? direction : fallback;
    }

    private static boolean structureBroken(String direction, List<Bar> bars) {
        if (bars.size() < 13) return false;
        Bar current = bars.get(bars.size() - 1);
        List<Bar> prior = bars.subList(bars.size() - 13, bars.size() - 1);
        if (direction != null && direction.contains("BULLISH")) {
            BigDecimal priorLow = prior.stream().map(Bar::low).min(Comparator.naturalOrder()).orElseThrow();
            return current.close.compareTo(priorLow) <= 0;
        }
        if (direction != null && direction.contains("BEARISH")) {
            BigDecimal priorHigh = prior.stream().map(Bar::high).max(Comparator.naturalOrder()).orElseThrow();
            return current.close.compareTo(priorHigh) >= 0;
        }
        return false;
    }

    private static BigDecimal robustSlope(List<Bar> bars, BigDecimal scale) {
        List<BigDecimal> slopes = new ArrayList<>();
        for (int i = 0; i < bars.size(); i++) {
            for (int j = i + 1; j < bars.size(); j++) {
                slopes.add(bars.get(j).close.subtract(bars.get(i).close)
                        .divide(scale.multiply(BigDecimal.valueOf(j - i)), 10, RoundingMode.HALF_UP));
            }
        }
        return squash(median(slopes).multiply(new BigDecimal("45")));
    }

    private static BigDecimal structureScore(List<Bar> bars) {
        if (bars.size() < 3) return BigDecimal.ZERO;
        int positive = 0;
        int negative = 0;
        for (int i = 1; i < bars.size(); i++) {
            Bar previous = bars.get(i - 1);
            Bar current = bars.get(i);
            if (current.high.compareTo(previous.high) > 0) positive++;
            else if (current.high.compareTo(previous.high) < 0) negative++;
            if (current.low.compareTo(previous.low) > 0) positive++;
            else if (current.low.compareTo(previous.low) < 0) negative++;
        }
        int total = positive + negative;
        return total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(positive - negative).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal centerAndSlopeScore(List<Bar> bars, BigDecimal scale) {
        List<BigDecimal> typical = bars.stream()
                .map(bar -> bar.high.add(bar.low).add(bar.close)
                        .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP)).toList();
        BigDecimal center = median(typical);
        BigDecimal centerDistance = bars.get(bars.size() - 1).close.subtract(center)
                .divide(scale, 10, RoundingMode.HALF_UP);
        BigDecimal centerSlope = median(typical.subList(typical.size() / 2, typical.size()))
                .subtract(median(typical.subList(0, typical.size() / 2)))
                .divide(scale, 10, RoundingMode.HALF_UP);
        return squash(centerDistance.multiply(new BigDecimal("28"))
                .add(centerSlope.multiply(new BigDecimal("12"))));
    }

    private static BigDecimal momentumScore(List<Bar> bars, BigDecimal scale) {
        BigDecimal close = bars.get(bars.size() - 1).close;
        BigDecimal m3 = close.subtract(bars.get(bars.size() - 4).close).divide(scale, 10, RoundingMode.HALF_UP);
        BigDecimal m6 = close.subtract(bars.get(bars.size() - 7).close).divide(scale, 10, RoundingMode.HALF_UP);
        BigDecimal m12 = close.subtract(bars.get(bars.size() - 13).close).divide(scale, 10, RoundingMode.HALF_UP);
        return squash(m3.multiply(new BigDecimal("12")).add(m6.multiply(new BigDecimal("7")))
                .add(m12.multiply(new BigDecimal("4"))));
    }

    private static BigDecimal pullbackScore(List<Bar> bars, BigDecimal scale, BigDecimal momentum) {
        BigDecimal high = bars.stream().map(Bar::high).max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal low = bars.stream().map(Bar::low).min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal close = bars.get(bars.size() - 1).close;
        BigDecimal fromHigh = high.subtract(close).divide(scale, 10, RoundingMode.HALF_UP);
        BigDecimal fromLow = close.subtract(low).divide(scale, 10, RoundingMode.HALF_UP);
        BigDecimal signed = momentum.signum() >= 0 ? BigDecimal.valueOf(2).subtract(fromHigh)
                : fromLow.subtract(BigDecimal.valueOf(2));
        return squash(signed.multiply(new BigDecimal("32")));
    }

    private static BigDecimal accelerationScore(List<Bar> bars, BigDecimal scale) {
        int end = bars.size() - 1;
        BigDecimal recent = bars.get(end).close.subtract(bars.get(end - 3).close);
        BigDecimal previous = bars.get(end - 3).close.subtract(bars.get(end - 6).close);
        return squash(recent.subtract(previous).divide(scale, 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("35")));
    }

    private static BigDecimal invalidation(String direction, List<Bar> bars, BigDecimal atr) {
        if (direction == null || direction.equals("WAIT") || direction.equals("RANGE")) return null;
        if (direction.contains("BULLISH")) {
            BigDecimal low = bars.stream().map(Bar::low).min(Comparator.naturalOrder()).orElseThrow();
            return low.subtract(atr.multiply(new BigDecimal("0.25"))).setScale(8, RoundingMode.HALF_UP);
        }
        BigDecimal high = bars.stream().map(Bar::high).max(Comparator.naturalOrder()).orElseThrow();
        return high.add(atr.multiply(new BigDecimal("0.25"))).setScale(8, RoundingMode.HALF_UP);
    }

    private static BigDecimal atr(List<Bar> bars, int period) {
        if (bars.size() < period + 1) return null;
        List<BigDecimal> ranges = new ArrayList<>();
        for (int i = bars.size() - period; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            BigDecimal previousClose = bars.get(i - 1).close;
            ranges.add(bar.high.subtract(bar.low).max(bar.high.subtract(previousClose).abs())
                    .max(bar.low.subtract(previousClose).abs()));
        }
        return median(ranges);
    }

    private static List<Bar> parse(List<String[]> source) {
        if (source == null) return List.of();
        List<Bar> result = new ArrayList<>();
        for (String[] row : source) {
            if (row == null || row.length < 5) continue;
            try {
                BigDecimal open = new BigDecimal(row[1]);
                BigDecimal suppliedHigh = new BigDecimal(row[2]);
                BigDecimal suppliedLow = new BigDecimal(row[3]);
                BigDecimal close = new BigDecimal(row[4]);
                BigDecimal high = suppliedHigh.max(suppliedLow).max(open).max(close);
                BigDecimal low = suppliedHigh.min(suppliedLow).min(open).min(close);
                if (positive(open) && positive(high) && positive(low) && positive(close)) {
                    result.add(new Bar(open, high, low, close));
                }
            } catch (RuntimeException ignored) {
                // Malformed bars are excluded; minimum sample rules remain fail closed.
            }
        }
        return List.copyOf(result);
    }

    private static List<Bar> last(List<Bar> bars, int count) {
        return bars.subList(Math.max(0, bars.size() - count), bars.size());
    }

    private static BigDecimal weighted(Object... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < values.length; i += 2) {
            total = total.add(((BigDecimal) values[i]).multiply(new BigDecimal((String) values[i + 1])));
        }
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal median(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        List<BigDecimal> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle))
                .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
    }

    private static BigDecimal squash(BigDecimal value) {
        double result = Math.tanh(value.doubleValue() / 50.0) * 100.0;
        return BigDecimal.valueOf(result).setScale(4, RoundingMode.HALF_UP);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String timeframeContext(BigDecimal one, BigDecimal four) {
        return "1小时" + (one.signum() > 0 ? "偏多" : one.signum() < 0 ? "偏空" : "中性")
                + "·4小时趋势" + (four.signum() > 0 ? "偏多" : four.signum() < 0 ? "偏空" : "中性");
    }

    private static String fingerprint(List<Bar> oneHour, List<Bar> fourHour) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(VERSION.getBytes(StandardCharsets.UTF_8));
            for (Bar bar : oneHour) digest.update(bar.toString().getBytes(StandardCharsets.UTF_8));
            for (Bar bar : fourHour) digest.update(bar.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static Calibration walkForwardCalibration(List<Bar> bars) {
        List<Integer> outcomes = new ArrayList<>();
        for (int i = 20; i + 4 < bars.size(); i++) {
            List<Bar> history = bars.subList(0, i + 1);
            BigDecimal scale = atr(last(history, Math.min(20, history.size())), 14);
            if (!positive(scale)) continue;
            BigDecimal momentum = bars.get(i).close.subtract(bars.get(i - 6).close);
            if (momentum.signum() == 0) continue;
            boolean bullish = momentum.signum() > 0;
            BigDecimal target = bullish ? bars.get(i).close.add(scale)
                    : bars.get(i).close.subtract(scale);
            BigDecimal stop = bullish ? bars.get(i).close.subtract(scale.multiply(new BigDecimal("0.75")))
                    : bars.get(i).close.add(scale.multiply(new BigDecimal("0.75")));
            Integer outcome = null;
            for (int j = i + 1; j <= i + 4; j++) {
                Bar next = bars.get(j);
                boolean stopped = bullish ? next.low.compareTo(stop) <= 0 : next.high.compareTo(stop) >= 0;
                boolean hit = bullish ? next.high.compareTo(target) >= 0 : next.low.compareTo(target) <= 0;
                if (stopped || hit) {
                    outcome = hit && !stopped ? 1 : 0;
                    break;
                }
            }
            outcomes.add(outcome == null ? 0 : outcome);
        }
        if (outcomes.isEmpty()) return new Calibration(0, null, null, null);
        double rate = outcomes.stream().mapToInt(Integer::intValue).average().orElse(0.0) * 100.0;
        double probability = rate / 100.0;
        double brier = outcomes.stream().mapToDouble(value -> Math.pow(probability - value, 2)).average().orElse(1.0);
        double calibrationError = Math.abs(probability
                - outcomes.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        return new Calibration(outcomes.size(), rate, brier, calibrationError);
    }

    private record Bar(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
    }

    public record Assessment(String version, String direction, String state,
                             BigDecimal directionScore, BigDecimal trend4h, BigDecimal state1h,
                             BigDecimal atr1h, BigDecimal invalidationLevel, String contextLabel,
                             String snapshotFingerprint, Calibration calibration) {
    }

    public record Calibration(int sampleCount, Double walkForwardHitRate,
                              Double brierScore, Double calibrationError) {
    }
}
