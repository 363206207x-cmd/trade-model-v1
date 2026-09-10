package org.example.trademodel.assetcard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Public market-only card projection. Never a canonical Decision, Plan or UserPosition. */
public record AssetCardSnapshot(
        String symbol, String assetName, BigDecimal spotPrice, Instant latestPriceAt,
        Signal signal, Risk risk, Health health, Instant cardAsOf, long snapshotVersion,
        String featureVersion, String modelVersion, String calibrationVersion) {

    public enum Direction {
        STRONG_LONG, LONG, WEAK_LONG, STRONG_SHORT, SHORT, WEAK_SHORT, RANGE, WATCH;
        public boolean longSide() { return this == STRONG_LONG || this == LONG || this == WEAK_LONG; }
        public boolean shortSide() { return this == STRONG_SHORT || this == SHORT || this == WEAK_SHORT; }
        public boolean directional() { return longSide() || shortSide(); }
    }

    public enum RiskType { CHASE, SHOCK, REVERSAL, CROWDING, LIQUIDATION, LIQUIDITY, EVENT, DATA }

    public record Signal(Direction direction, String status, Integer calibratedConfidence,
                         Double pLong, Double pShort, String oneHourState, String fourHourTrend,
                         Instant signalAsOf) {
        public Signal {
            // The supplied integer is never a second/legacy confidence source.
            Double probability = direction == null ? null : direction.longSide() ? pLong
                    : direction.shortSide() ? pShort : null;
            calibratedConfidence = "VALID".equals(status) && validProbability(probability)
                    ? (int) Math.round(probability * 100.0) : null;
            if (!validProbability(pLong)) pLong = null;
            if (!validProbability(pShort)) pShort = null;
        }

        public static Signal unavailable(String status, Instant asOf) {
            return new Signal(null, status, null, null, null, "数据不足", "数据不足", asOf);
        }

        public Signal invalidated() {
            return new Signal(direction, "INVALIDATED", null, null, null, oneHourState, fourHourTrend, signalAsOf);
        }

        private static boolean validProbability(Double value) {
            return value != null && Double.isFinite(value) && value >= 0 && value <= 1;
        }
    }

    public record RiskItem(String type, String assessmentStatus, String level, String evidenceValue,
                           String source, Instant asOf, String reason, String unit) {
        public RiskItem(String type, String assessmentStatus, String level, String evidenceValue,
                        String source, Instant asOf, String reason) {
            this(type, assessmentStatus, level, evidenceValue, source, asOf, reason, null);
        }
        public RiskItem {
            if (!"ASSESSED".equals(assessmentStatus)) level = null;
        }
        @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
        public boolean invalidatesSignal() {
            return "ASSESSED".equals(assessmentStatus) && "HIGH".equals(level)
                    && java.util.Set.of("SHOCK", "REVERSAL", "LIQUIDITY", "DATA").contains(String.valueOf(type));
        }
        public static RiskItem unknown(RiskType type, String reason) {
            return new RiskItem(type.name(), "UNKNOWN", null, null, null, null, reason);
        }
    }

    public record Risk(String overallLevel, List<RiskItem> items, Instant riskAsOf) {
        public Risk { items = items == null ? List.of() : List.copyOf(items); }
        public static Risk unknown(String reason) {
            return new Risk(null, Arrays.stream(RiskType.values()).map(t -> RiskItem.unknown(t, reason)).toList(), null);
        }
    }

    public record Health(String status, String reason, Instant asOf) {}

    public static AssetCardSnapshot unavailable(String symbol, String name, String reason) {
        return new AssetCardSnapshot(symbol, name, null, null, Signal.unavailable("INSUFFICIENT_DATA", null),
                Risk.unknown(reason), new Health("INSUFFICIENT_DATA", reason, null), null, 0, null, null, null);
    }
}
