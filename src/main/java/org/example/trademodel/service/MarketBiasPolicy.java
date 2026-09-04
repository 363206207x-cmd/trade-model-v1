package org.example.trademodel.service;

import org.example.trademodel.config.FundamentalAiV41Properties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frozen four-timeframe market-bias mapping. Direction classification, weighted
 * convergence and trend-score dispersion all fail closed on missing windows.
 */
public final class MarketBiasPolicy {

    private MarketBiasPolicy() {
    }

    public static String classify(List<String[]> bars5m,
                                  List<String[]> bars15m,
                                  List<String[]> bars1h,
                                  List<String[]> bars4h) {
        return classify(bars5m, bars15m, bars1h, bars4h,
                FundamentalAiV41Properties.contractFixture().getMultiTimeframe());
    }

    public static String classify(List<String[]> bars5m,
                                  List<String[]> bars15m,
                                  List<String[]> bars1h,
                                  List<String[]> bars4h,
                                  FundamentalAiV41Properties.MultiTimeframe config) {
        FundamentalAiV41Properties fixture = FundamentalAiV41Properties.contractFixture();
        return assessDirection(bars5m, bars15m, bars1h, bars4h, config,
                fixture.getNormalization()).ruleMarketBias();
    }

    /**
     * Production direction owner. 4h and 1h alone establish direction; 15m and
     * 5m remain visible as trigger/micro-risk facts and cannot reverse it.
     */
    public static DirectionAssessment assessDirection(
            List<String[]> bars5m,
            List<String[]> bars15m,
            List<String[]> bars1h,
            List<String[]> bars4h,
            FundamentalAiV41Properties.MultiTimeframe config,
            FundamentalAiV41Properties.Normalization normalization) {
        requireConfig(config);
        requireNormalization(normalization);
        BigDecimal fourHour = normalizedDirectionScore(bars4h, normalization);
        BigDecimal oneHour = normalizedDirectionScore(bars1h, normalization);
        BigDecimal fifteenMinute = normalizedDirectionScore(bars15m, normalization);
        BigDecimal fiveMinute = normalizedDirectionScore(bars5m, normalization);
        if (fourHour == null || oneHour == null) {
            return new DirectionAssessment("WAIT", "INSUFFICIENT_DATA", null,
                    fourHour, oneHour, fifteenMinute, fiveMinute,
                    normalization.getVersion());
        }
        boolean opposingCoreDirections = fourHour.signum() != 0 && oneHour.signum() != 0
                && fourHour.signum() != oneHour.signum();
        boolean configuredOppositionConflict = opposingCoreDirections
                && fourHour.subtract(oneHour).abs()
                .compareTo(config.getMaximumTrendScoreDifference()) > 0;
        boolean highMagnitudeOppositionConflict = opposingCoreDirections
                && fourHour.abs().compareTo(BigDecimal.valueOf(35)) >= 0
                && oneHour.abs().compareTo(BigDecimal.valueOf(35)) >= 0;
        boolean criticalConflict = configuredOppositionConflict || highMagnitudeOppositionConflict;
        BigDecimal structural = fourHour.multiply(config.getFourHourWeight())
                .add(oneHour.multiply(config.getOneHourWeight()))
                .setScale(4, RoundingMode.HALF_UP);
        String bias = criticalConflict ? "WAIT" : classifyStructuralBias(structural);
        boolean strongDispersion = ("STRONG_BULLISH".equals(bias) || "STRONG_BEARISH".equals(bias))
                && fourHour.signum() == oneHour.signum()
                && fourHour.subtract(oneHour).abs()
                .compareTo(config.getMaximumTrendScoreDifference()) > 0;
        if (strongDispersion) {
            bias = "STRONG_BULLISH".equals(bias) ? "BULLISH" : "BEARISH";
        }
        return new DirectionAssessment(bias,
                criticalConflict ? "MULTI_TIMEFRAME_CONFLICT" : "READY",
                structural, fourHour, oneHour, fifteenMinute, fiveMinute,
                normalization.getVersion());
    }

    public static Map<String, Map<String, Object>> describeTimeframes(
            List<String[]> bars5m,
            List<String[]> bars15m,
            List<String[]> bars1h,
            List<String[]> bars4h,
            FundamentalAiV41Properties.MultiTimeframe config) {
        requireConfig(config);
        Map<String, Map<String, Object>> details = new LinkedHashMap<>();
        details.put("4h", describe(bars4h, config.getFourHourWeight()));
        details.put("1h", describe(bars1h, config.getOneHourWeight()));
        details.put("15m", describe(bars15m, config.getFifteenMinuteWeight()));
        details.put("5m", describe(bars5m, config.getFiveMinuteWeight()));
        return details;
    }

    public static Map<String, Map<String, Object>> describeTimeframes(
            List<String[]> bars5m,
            List<String[]> bars15m,
            List<String[]> bars1h,
            List<String[]> bars4h,
            FundamentalAiV41Properties.MultiTimeframe config,
            FundamentalAiV41Properties.Normalization normalization) {
        DirectionAssessment direction = assessDirection(
                bars5m, bars15m, bars1h, bars4h, config, normalization);
        Map<String, Map<String, Object>> details = new LinkedHashMap<>();
        details.put("4h", normalizedDetail(bars4h, direction.normalized4hDirectionScore(), "PRIMARY_DIRECTION", normalization));
        details.put("1h", normalizedDetail(bars1h, direction.normalized1hDirectionScore(), "OPPORTUNITY_STRUCTURE", normalization));
        details.put("15m", normalizedDetail(bars15m, direction.normalized15mDirectionScore(), "TRIGGER_TIMING", normalization));
        details.put("5m", normalizedDetail(bars5m, direction.normalized5mDirectionScore(), "MICRO_RISK_FILTER", normalization));
        return details;
    }

    public static boolean converged(List<String[]> bars1h, List<String[]> bars4h,
                                    String marketBias) {
        FundamentalAiV41Properties.MultiTimeframe config =
                FundamentalAiV41Properties.contractFixture().getMultiTimeframe();
        WindowAssessment oneHour = assessment(bars1h, config.getOneHourWeight());
        WindowAssessment fourHour = assessment(bars4h, config.getFourHourWeight());
        return convergedAssessments(List.of(oneHour, fourHour), marketBias, 2,
                config.getOneHourWeight().add(config.getFourHourWeight()),
                config.getMaximumTrendScoreDifference());
    }

    public static boolean converged(List<String[]> bars5m,
                                    List<String[]> bars15m,
                                    List<String[]> bars1h,
                                    List<String[]> bars4h,
                                    String marketBias,
                                    FundamentalAiV41Properties.MultiTimeframe config) {
        requireConfig(config);
        return convergedAssessments(List.of(
                        assessment(bars5m, config.getFiveMinuteWeight()),
                        assessment(bars15m, config.getFifteenMinuteWeight()),
                        assessment(bars1h, config.getOneHourWeight()),
                        assessment(bars4h, config.getFourHourWeight())),
                marketBias,
                config.getMinimumAlignedCount(),
                config.getMinimumAlignedWeight(),
                config.getMaximumTrendScoreDifference());
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

    private static boolean convergedAssessments(List<WindowAssessment> windows,
                                                String marketBias,
                                                int minimumAlignedCount,
                                                BigDecimal minimumAlignedWeight,
                                                BigDecimal maximumTrendScoreDifference) {
        WindowDirection expected = bullishFamily(marketBias) ? WindowDirection.BULLISH
                : bearishFamily(marketBias) ? WindowDirection.BEARISH : null;
        if (expected == null || windows.stream().anyMatch(WindowAssessment::missing)) return false;
        List<WindowAssessment> aligned = new ArrayList<>();
        for (WindowAssessment window : windows) {
            if (window.direction() == expected) aligned.add(window);
        }
        if (aligned.size() < minimumAlignedCount) return false;
        BigDecimal alignedWeight = aligned.stream().map(WindowAssessment::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (alignedWeight.compareTo(minimumAlignedWeight) < 0) return false;
        BigDecimal min = aligned.stream().map(WindowAssessment::trendScore)
                .min(BigDecimal::compareTo).orElse(null);
        BigDecimal max = aligned.stream().map(WindowAssessment::trendScore)
                .max(BigDecimal::compareTo).orElse(null);
        return min != null && max != null
                && max.subtract(min).abs().compareTo(maximumTrendScoreDifference) <= 0;
    }

    private static WindowAssessment assessment(List<String[]> bars, BigDecimal weight) {
        WindowDirection direction = direction(bars);
        BigDecimal score = trendScore(bars);
        return new WindowAssessment(direction, score, weight);
    }

    private static Map<String, Object> describe(List<String[]> bars, BigDecimal weight) {
        WindowAssessment value = assessment(bars, weight);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("state", value.missing() ? "INSUFFICIENT_DATA" : "FOUND");
        detail.put("direction", value.direction().name());
        detail.put("trendScore", value.trendScore());
        detail.put("weight", value.weight());
        detail.put("barCount", bars == null ? 0 : bars.size());
        return detail;
    }

    private static BigDecimal directionWeight(List<WindowAssessment> windows, WindowDirection direction) {
        return windows.stream()
                .filter(window -> window.direction() == direction)
                .map(WindowAssessment::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal trendScore(List<String[]> bars) {
        if (bars == null || bars.isEmpty()) return null;
        String[] first = bars.get(0);
        String[] last = bars.get(bars.size() - 1);
        if (first == null || last == null || first.length <= 1 || last.length <= 4) return null;
        try {
            BigDecimal open = new BigDecimal(first[1]);
            BigDecimal close = new BigDecimal(last[4]);
            if (open.signum() == 0) return null;
            BigDecimal percent = close.subtract(open)
                    .divide(open, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            return BigDecimal.valueOf(50).add(percent)
                    .max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static BigDecimal normalizedDirectionScore(
            List<String[]> bars,
            FundamentalAiV41Properties.Normalization normalization) {
        if (bars == null || bars.size() < normalization.getMinimumSampleCount()) return null;
        int from = Math.max(0, bars.size() - normalization.getLookback());
        List<BigDecimal> window = new ArrayList<>();
        BigDecimal firstOpen = value(bars.get(from), 1);
        if (firstOpen == null || firstOpen.signum() <= 0) return null;
        for (int i = from; i < bars.size(); i++) {
            BigDecimal close = value(bars.get(i), 4);
            if (close == null || close.signum() <= 0) continue;
            window.add(close.subtract(firstOpen).divide(firstOpen, 12, RoundingMode.HALF_UP));
        }
        if (window.size() < normalization.getMinimumSampleCount()) return null;
        List<BigDecimal> sorted = new ArrayList<>(window);
        sorted.sort(BigDecimal::compareTo);
        BigDecimal lower = percentile(sorted, normalization.getWinsorizeLowerPercentile());
        BigDecimal upper = percentile(sorted, normalization.getWinsorizeUpperPercentile());
        if (lower == null || upper == null || upper.compareTo(lower) == 0) return BigDecimal.ZERO;
        BigDecimal current = window.get(window.size() - 1).max(lower).min(upper);
        long below = sorted.stream().filter(value -> value.compareTo(current) < 0).count();
        long equal = sorted.stream().filter(value -> value.compareTo(current) == 0).count();
        BigDecimal rank = BigDecimal.valueOf(below + equal / 2.0)
                .divide(BigDecimal.valueOf(sorted.size()), 8, RoundingMode.HALF_UP);
        return rank.multiply(BigDecimal.valueOf(200)).subtract(BigDecimal.valueOf(100))
                .max(BigDecimal.valueOf(-100)).min(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    static String classifyStructuralBias(BigDecimal value) {
        if (value.compareTo(BigDecimal.valueOf(70)) >= 0) return "STRONG_BULLISH";
        if (value.compareTo(BigDecimal.valueOf(35)) >= 0) return "BULLISH";
        if (value.compareTo(BigDecimal.valueOf(15)) >= 0) return "WEAK_BULLISH";
        if (value.compareTo(BigDecimal.valueOf(-14)) >= 0) return "RANGE";
        if (value.compareTo(BigDecimal.valueOf(-35)) > 0) return "WEAK_BEARISH";
        if (value.compareTo(BigDecimal.valueOf(-70)) > 0) return "BEARISH";
        return "STRONG_BEARISH";
    }

    private static Map<String, Object> normalizedDetail(
            List<String[]> bars,
            BigDecimal score,
            String responsibility,
            FundamentalAiV41Properties.Normalization normalization) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("state", score == null ? "INSUFFICIENT_SAMPLE" : "FOUND");
        detail.put("normalizedDirectionScore", score);
        detail.put("direction", score == null ? "MISSING"
                : score.signum() > 0 ? "BULLISH" : score.signum() < 0 ? "BEARISH" : "FLAT");
        detail.put("trendScore", score == null ? null
                : score.add(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
        detail.put("responsibility", responsibility);
        detail.put("barCount", bars == null ? 0 : bars.size());
        detail.put("normalizationVersion", normalization.getVersion());
        return detail;
    }

    private static BigDecimal percentile(List<BigDecimal> sorted, BigDecimal percentile) {
        if (sorted == null || sorted.isEmpty() || percentile == null) return null;
        BigDecimal position = percentile.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(sorted.size() - 1));
        int lower = position.intValue();
        int upper = Math.min(sorted.size() - 1, lower + 1);
        BigDecimal fraction = position.subtract(BigDecimal.valueOf(lower));
        return sorted.get(lower).add(sorted.get(upper).subtract(sorted.get(lower)).multiply(fraction));
    }

    private static BigDecimal value(String[] bar, int index) {
        if (bar == null || bar.length <= index || bar[index] == null) return null;
        try {
            return new BigDecimal(bar[index]);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void requireConfig(FundamentalAiV41Properties.MultiTimeframe config) {
        if (config == null || config.getFourHourWeight() == null || config.getOneHourWeight() == null
                || config.getFifteenMinuteWeight() == null || config.getFiveMinuteWeight() == null
                || config.getMinimumAlignedCount() == null || config.getMinimumAlignedWeight() == null
                || config.getMaximumTrendScoreDifference() == null) {
            throw new IllegalStateException("multi-timeframe contract configuration is required");
        }
    }

    private static void requireNormalization(FundamentalAiV41Properties.Normalization config) {
        if (config == null || config.getVersion() == null || config.getLookback() == null
                || config.getMinimumSampleCount() == null
                || config.getWinsorizeLowerPercentile() == null
                || config.getWinsorizeUpperPercentile() == null) {
            throw new IllegalStateException("normalization contract configuration is required");
        }
    }

    public record DirectionAssessment(
            String ruleMarketBias,
            String directionDataState,
            BigDecimal structuralBias,
            BigDecimal normalized4hDirectionScore,
            BigDecimal normalized1hDirectionScore,
            BigDecimal normalized15mDirectionScore,
            BigDecimal normalized5mDirectionScore,
            String normalizationVersion) {
        public boolean structurallyReady() {
            return "READY".equals(directionDataState);
        }
    }

    private record WindowAssessment(WindowDirection direction,
                                    BigDecimal trendScore,
                                    BigDecimal weight) {
        private boolean missing() {
            return direction == WindowDirection.MISSING || trendScore == null || weight == null;
        }
    }

    enum WindowDirection {
        BULLISH,
        BEARISH,
        FLAT,
        MISSING
    }
}
