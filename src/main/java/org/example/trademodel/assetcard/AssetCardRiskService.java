package org.example.trademodel.assetcard;

import java.time.Instant;
import java.util.*;

/** Pure independent card risk calculation. No canonical result, persistence or external calls. */
public final class AssetCardRiskService {
    public static final String RULE_VERSION = "V42_DIRECTIONAL_RISK_1";
    private static final Map<String, List<String>> COMPONENTS = Map.of(
            "CHASE", List.of("structuralCenterDistanceAtr", "extensionAtr"),
            "SHOCK", List.of("return1m", "return5m", "volatility1m", "volatility5m"),
            "REVERSAL", List.of("slope5m", "slope1h", "slope4h"),
            "CROWDING", List.of("fundingRate", "logLongShortRatio", "crowdingOpenInterestChange1h"),
            "LIQUIDATION", List.of("liquidationImbalance"),
            "LIQUIDITY", List.of("spreadBps", "depth10Bps", "depth25Bps", "bookImbalance"));
    private static final Map<String, String> UNITS = Map.ofEntries(
            Map.entry("structuralCenterDistanceAtr", "ATR_MULTIPLE"), Map.entry("extensionAtr", "ATR_MULTIPLE"),
            Map.entry("volatility1m", "LOG_RETURN_STD"), Map.entry("volatility5m", "LOG_RETURN_STD"),
            Map.entry("return1m", "LOG_RETURN"), Map.entry("return5m", "LOG_RETURN"),
            Map.entry("slope5m", "ATR_MULTIPLE"), Map.entry("slope1h", "ATR_MULTIPLE"), Map.entry("slope4h", "ATR_MULTIPLE"),
            Map.entry("fundingRate", "RATE"), Map.entry("logLongShortRatio", "LOG_RATIO"),
            Map.entry("crowdingOpenInterestChange1h", "PERCENT"),
            Map.entry("longLiquidation", "QUOTE_CURRENCY"), Map.entry("shortLiquidation", "QUOTE_CURRENCY"),
            Map.entry("liquidationImbalance", "RATIO"), Map.entry("spreadBps", "BASIS_POINTS"),
            Map.entry("depth10Bps", "QUOTE_CURRENCY"), Map.entry("depth25Bps", "QUOTE_CURRENCY"),
            Map.entry("bookImbalance", "RATIO"));

    public record Metric(double value, String unit, String source, Instant observedAt, Instant availableAt, Instant expiresAt) {
        public boolean freshAt(Instant at) {
            return at != null && Double.isFinite(value) && source != null && !source.isBlank()
                    && unit != null && observedAt != null && availableAt != null && expiresAt != null
                    && !observedAt.isAfter(at) && !availableAt.isAfter(at) && !availableAt.isBefore(observedAt)
                    && !expiresAt.isBefore(at);
        }
    }
    public record Input(String symbol, Instant asOf, AssetCardSnapshot.SignalSide side,
                        AssetCardSnapshot.Direction direction, Instant signalAsOf, String riskVersion, Map<String, Metric> metrics,
                        Map<String, AssetCardModelBundle.RiskDistribution> distributions,
                        AssetCardSnapshot.RiskItem eventRisk, boolean coreDataComplete, boolean coreSourceLost,
                        String structuralBreach) {
        public Input {
            if (symbol == null || !symbol.matches("[A-Z0-9]{2,32}") || asOf == null)
                throw new IllegalArgumentException("Card risk identity required");
            if (side != null && side != (direction == null ? AssetCardSnapshot.SignalSide.NON_DIRECTIONAL : direction.signalSide()))
                throw new IllegalArgumentException("Card risk side and direction disagree");
            if (signalAsOf != null && signalAsOf.isAfter(asOf))
                throw new IllegalArgumentException("Card risk signal identity is in the future");
            metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
            distributions = distributions == null ? Map.of() : Map.copyOf(distributions);
        }
        /** Existing stored/caller shape is readable but cannot authorize a bound directional result. */
        public Input(String symbol, Instant asOf, Map<String, Metric> metrics,
                     Map<String, AssetCardModelBundle.RiskDistribution> distributions,
                     AssetCardSnapshot.RiskItem eventRisk, boolean coreDataComplete, boolean coreSourceLost, String structuralBreach) {
            this(symbol, asOf, null, null, null, null, metrics, distributions, eventRisk, coreDataComplete, coreSourceLost, structuralBreach);
        }
    }
    /** Invalidation is a recommendation for the card state machine, never an opposite direction. */
    public record Result(AssetCardSnapshot.Risk risk, boolean invalidate) {}

    public Result evaluate(Input input) {
        Objects.requireNonNull(input);
        if (input.side() == null || input.riskVersion() == null || input.riskVersion().isBlank()
                || input.side() != AssetCardSnapshot.SignalSide.NON_DIRECTIONAL && input.signalAsOf() == null)
            return new Result(AssetCardSnapshot.Risk.unknown("风险方向身份或版本尚未完成校验"), false);
        List<AssetCardSnapshot.RiskItem> items = new ArrayList<>();
        for (var type : AssetCardSnapshot.RiskType.values()) {
            var item = switch (type) {
                case EVENT -> event(input);
                case DATA -> data(input);
                case REVERSAL -> input.side() != AssetCardSnapshot.SignalSide.NON_DIRECTIONAL
                        && input.structuralBreach() != null && !input.structuralBreach().isBlank()
                        ? new AssetCardSnapshot.RiskItem("REVERSAL", "ASSESSED", "HIGH", input.structuralBreach(),
                            "BINANCE_SPOT_STRUCTURE", input.asOf(), input.structuralBreach(), "STRUCTURAL_BREACH", true) : component(type, input);
                default -> component(type, input);
            };
            items.add(item);
        }
        String overall = items.stream().anyMatch(i -> "HIGH".equals(i.level())) ? "HIGH"
                : items.stream().anyMatch(i -> "MEDIUM".equals(i.level())) ? "MEDIUM"
                : items.stream().allMatch(i -> "ASSESSED".equals(i.assessmentStatus())) ? "LOW" : null;
        Instant riskAt = items.stream().map(AssetCardSnapshot.RiskItem::asOf).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        Set<String> marketKeys = new HashSet<>();
        for (var type : AssetCardSnapshot.RiskType.values()) {
            if (type == AssetCardSnapshot.RiskType.DATA || type == AssetCardSnapshot.RiskType.EVENT) continue;
            for (String key : components(type, input.side())) {
                marketKeys.add("crowdingOpenInterestChange1h".equals(key) ? "openInterestChange1h" : key);
                if ("crowdingOpenInterestChange1h".equals(key)) marketKeys.add("priceReturn1h");
            }
        }
        Instant marketAt = marketKeys.stream().map(input.metrics()::get).filter(Objects::nonNull)
                .filter(m -> m.freshAt(input.asOf())).map(Metric::observedAt).max(Comparator.naturalOrder()).orElse(null);
        return new Result(new AssetCardSnapshot.Risk(overall, items, riskAt, input.side(), input.direction(),
                input.signalAsOf(), marketAt, input.riskVersion()),
                items.stream().anyMatch(AssetCardSnapshot.RiskItem::invalidatesSignal));
    }

    private AssetCardSnapshot.RiskItem component(AssetCardSnapshot.RiskType type, Input input) {
        List<String> keys = components(type, input.side());
        if (keys.isEmpty()) return AssetCardSnapshot.RiskItem.unknown(type, "无方向状态尚无独立、已验证的该项风险分布；不沿用先前方向");
        String level = "LOW";
        List<String> values = new ArrayList<>(), sources = new ArrayList<>(), reasons = new ArrayList<>(), missing = new ArrayList<>();
        Set<String> units = new TreeSet<>();
        Instant observedAt = null;
        for (String key : keys) {
            Metric fact = input.metrics().get("crowdingOpenInterestChange1h".equals(key) ? "openInterestChange1h" : key);
            var distribution = input.distributions().get(key);
            if (fact == null || !fact.freshAt(input.asOf()) || !UNITS.get(key).equals(fact.unit())
                    || distribution == null || distribution.asOf().isAfter(input.asOf())
                    || !fact.unit().equals(distribution.unit()) || !validDomain(key, fact.value())
                    || !input.symbol().equals(distribution.symbol()) || !input.side().name().equals(distribution.side())
                    || !key.equals(distribution.metricKey()) || !input.riskVersion().equals(distribution.riskVersion())
                    || distribution.higherIsWorse() != higherIsWorse(key, input.side())) {
                missing.add(key);
                continue;
            }
            String conditionEvidence = "";
            boolean adverseCondition = true;
            if ("crowdingOpenInterestChange1h".equals(key)) {
                Metric price = input.metrics().get("priceReturn1h");
                if (price == null || !price.freshAt(input.asOf()) || !"LOG_RETURN".equals(price.unit())) {
                    missing.add("priceReturn1h");
                    continue;
                }
                adverseCondition = fact.value() > 0 && (input.side() == AssetCardSnapshot.SignalSide.LONG
                        ? price.value() > 0 : price.value() < 0);
                conditionEvidence = "；priceReturn1h=" + price.value() + " " + price.unit() + " (" + price.source() + ")";
                if (observedAt == null || price.observedAt().isAfter(observedAt)) observedAt = price.observedAt();
            }
            double percentile = distribution.adversePercentile(fact.value());
            String own = !adverseCondition ? "NONE" : percentile >= distribution.highPercentile() ? "HIGH"
                    : percentile >= distribution.mediumPercentile() ? "MEDIUM" : "LOW";
            if (rank(own) > rank(level)) level = own;
            values.add(key + "=" + fact.value() + " " + fact.unit() + conditionEvidence); units.add(fact.unit());
            sources.add(key + ":" + fact.source());
            reasons.add(adverseCondition ? key + " " + input.side() + " 历史不利分位=" + percentile + "；中/高阈值="
                    + distribution.mediumPercentile() + "/" + distribution.highPercentile()
                    : "未平仓量和同周期价格未共同形成当前方向的拥挤建立证据");
            if (observedAt == null || fact.observedAt().isAfter(observedAt)) observedAt = fact.observedAt();
        }
        // Missing metrics never establish LOW; independently evidenced high/medium must not disappear.
        if (!missing.isEmpty() && "LOW".equals(level))
            return AssetCardSnapshot.RiskItem.unknown(type, "缺少新鲜、同单位证据或已验证历史分布：" + String.join("、", missing));
        if (!missing.isEmpty()) reasons.add("另有未完成指标：" + String.join("、", missing));
        return new AssetCardSnapshot.RiskItem(type.name(), "ASSESSED", level, String.join("；", values),
                String.join("；", sources), observedAt, String.join("；", reasons),
                units.size() == 1 ? units.iterator().next() : "PER_METRIC");
    }

    private List<String> components(AssetCardSnapshot.RiskType type, AssetCardSnapshot.SignalSide side) {
        if (side == AssetCardSnapshot.SignalSide.NON_DIRECTIONAL) return switch (type) {
            case SHOCK -> List.of("volatility1m", "volatility5m");
            case LIQUIDITY -> List.of("spreadBps", "depth10Bps", "depth25Bps");
            default -> List.of();
        };
        if (type == AssetCardSnapshot.RiskType.LIQUIDATION)
            return List.of(side == AssetCardSnapshot.SignalSide.LONG ? "longLiquidation" : "shortLiquidation", "liquidationImbalance");
        return COMPONENTS.get(type.name());
    }

    private boolean higherIsWorse(String key, AssetCardSnapshot.SignalSide side) {
        if (key.startsWith("depth")) return false;
        if (Set.of("spreadBps", "volatility1m", "volatility5m", "longLiquidation", "shortLiquidation", "crowdingOpenInterestChange1h").contains(key)) return true;
        boolean higherAdverseForLong = Set.of("structuralCenterDistanceAtr", "extensionAtr", "fundingRate", "logLongShortRatio", "liquidationImbalance").contains(key);
        return side == AssetCardSnapshot.SignalSide.LONG ? higherAdverseForLong : !higherAdverseForLong;
    }

    private boolean validDomain(String key, double value) {
        return !(key.startsWith("depth") || key.startsWith("volatility") || key.endsWith("Liquidation") || key.equals("spreadBps")) || value >= 0;
    }

    private AssetCardSnapshot.RiskItem event(Input input) {
        var fact = input.eventRisk();
        if (fact != null && "EVENT".equals(fact.type()) && "ASSESSED".equals(fact.assessmentStatus())
                && Set.of("NONE", "LOW", "MEDIUM", "HIGH").contains(String.valueOf(fact.level()))
                && fact.asOf() != null && !fact.asOf().isAfter(input.asOf())
                && fact.source() != null && !fact.source().isBlank() && fact.evidenceValue() != null && !fact.evidenceValue().isBlank()) return fact;
        return AssetCardSnapshot.RiskItem.unknown(AssetCardSnapshot.RiskType.EVENT, "尚无当前窗口内完整事件覆盖证据");
    }
    private AssetCardSnapshot.RiskItem data(Input input) {
        if (input.coreSourceLost()) return new AssetCardSnapshot.RiskItem("DATA", "ASSESSED", "HIGH", "CORE_SOURCE_LOST",
                "BINANCE_SPOT_HEALTH", input.asOf(), "核心现货源失去新鲜数据，原信号需重新确认", "SOURCE_STATE", true);
        if (input.coreDataComplete()) return new AssetCardSnapshot.RiskItem("DATA", "ASSESSED", "NONE", "CORE_DATA_COMPLETE",
                "BINANCE_SPOT_HEALTH", input.asOf(), "核心数据身份、新鲜度及完整性检查通过", "SOURCE_STATE");
        return AssetCardSnapshot.RiskItem.unknown(AssetCardSnapshot.RiskType.DATA, "核心现货数据尚未完整或未完成首次校验");
    }

    public List<AssetCardSnapshot.RiskItem> activeItems(AssetCardSnapshot.Risk risk) {
        if (risk == null) return List.of();
        return risk.items().stream().filter(i -> "ASSESSED".equals(i.assessmentStatus()) && rank(i.level()) >= 2)
                .sorted(Comparator.comparingInt((AssetCardSnapshot.RiskItem i) -> rank(i.level())).reversed()
                        .thenComparing(i -> !i.invalidatesSignal())
                        .thenComparing(AssetCardSnapshot.RiskItem::asOf, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AssetCardSnapshot.RiskItem::type)).limit(3).toList();
    }
    private static int rank(String level) { return "HIGH".equals(level) ? 3 : "MEDIUM".equals(level) ? 2 : "LOW".equals(level) ? 1 : 0; }
}
