package org.example.trademodel.assetcard;

import java.time.Instant;
import java.util.*;

/** Pure independent card risk calculation. No canonical result, persistence or external calls. */
public final class AssetCardRiskService {
    private static final Map<String, List<String>> COMPONENTS = Map.of(
            "CHASE", List.of("structuralCenterDistanceAtr", "extensionAtr"),
            "SHOCK", List.of("volatility1m", "volatility5m"),
            "REVERSAL", List.of("timeframeConflict"),
            "CROWDING", List.of("absFundingRate", "absLogLongShortRatio", "absOpenInterestChange1h"),
            "LIQUIDATION", List.of("longLiquidation", "shortLiquidation", "absLiquidationImbalance"),
            "LIQUIDITY", List.of("spreadBps", "depth10Bps", "depth25Bps", "absBookImbalance"));
    private static final Map<String, String> UNITS = Map.ofEntries(
            Map.entry("structuralCenterDistanceAtr", "ATR_MULTIPLE"), Map.entry("extensionAtr", "ATR_MULTIPLE"),
            Map.entry("volatility1m", "LOG_RETURN_STD"), Map.entry("volatility5m", "LOG_RETURN_STD"),
            Map.entry("timeframeConflict", "RATIO"), Map.entry("absFundingRate", "RATE"),
            Map.entry("absLogLongShortRatio", "LOG_RATIO"), Map.entry("absOpenInterestChange1h", "PERCENT"),
            Map.entry("longLiquidation", "QUOTE_CURRENCY"), Map.entry("shortLiquidation", "QUOTE_CURRENCY"),
            Map.entry("absLiquidationImbalance", "RATIO"), Map.entry("spreadBps", "BASIS_POINTS"),
            Map.entry("depth10Bps", "QUOTE_CURRENCY"), Map.entry("depth25Bps", "QUOTE_CURRENCY"),
            Map.entry("absBookImbalance", "RATIO"));

    public record Metric(double value, String unit, String source, Instant observedAt, Instant availableAt, Instant expiresAt) {
        public boolean freshAt(Instant at) {
            return at != null && Double.isFinite(value) && value >= 0 && source != null && !source.isBlank()
                    && unit != null && observedAt != null && availableAt != null && expiresAt != null
                    && !observedAt.isAfter(at) && !availableAt.isAfter(at) && !availableAt.isBefore(observedAt)
                    && !expiresAt.isBefore(at);
        }
    }
    public record Input(String symbol, Instant asOf, Map<String, Metric> metrics,
                        Map<String, AssetCardModelBundle.RiskDistribution> distributions,
                        AssetCardSnapshot.RiskItem eventRisk, boolean coreDataComplete, boolean coreSourceLost,
                        String structuralBreach) {
        public Input {
            if (symbol == null || !symbol.matches("[A-Z0-9]{2,32}") || asOf == null)
                throw new IllegalArgumentException("Card risk identity required");
            metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
            distributions = distributions == null ? Map.of() : Map.copyOf(distributions);
        }
    }
    /** Invalidation is a recommendation for the card state machine, never an opposite direction. */
    public record Result(AssetCardSnapshot.Risk risk, boolean invalidate) {}

    public Result evaluate(Input input) {
        Objects.requireNonNull(input);
        List<AssetCardSnapshot.RiskItem> items = new ArrayList<>();
        for (var type : AssetCardSnapshot.RiskType.values()) {
            var item = switch (type) {
                case EVENT -> event(input);
                case DATA -> data(input);
                case REVERSAL -> input.structuralBreach() != null && !input.structuralBreach().isBlank()
                        ? new AssetCardSnapshot.RiskItem("REVERSAL", "ASSESSED", "HIGH", input.structuralBreach(),
                            "BINANCE_SPOT_STRUCTURE", input.asOf(), input.structuralBreach(), "STRUCTURAL_BREACH") : component(type, input);
                default -> component(type, input);
            };
            items.add(item);
        }
        String overall = items.stream().anyMatch(i -> "HIGH".equals(i.level())) ? "HIGH"
                : items.stream().anyMatch(i -> "MEDIUM".equals(i.level())) ? "MEDIUM"
                : items.stream().allMatch(i -> "ASSESSED".equals(i.assessmentStatus())) ? "LOW" : null;
        Instant riskAt = items.stream().map(AssetCardSnapshot.RiskItem::asOf).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return new Result(new AssetCardSnapshot.Risk(overall, items, riskAt),
                items.stream().anyMatch(AssetCardSnapshot.RiskItem::invalidatesSignal));
    }

    private AssetCardSnapshot.RiskItem component(AssetCardSnapshot.RiskType type, Input input) {
        String level = "LOW";
        List<String> values = new ArrayList<>(), sources = new ArrayList<>(), reasons = new ArrayList<>(), missing = new ArrayList<>();
        Set<String> units = new TreeSet<>();
        Instant observedAt = null;
        for (String key : COMPONENTS.get(type.name())) {
            Metric fact = input.metrics().get(key);
            var distribution = input.distributions().get(key);
            if (fact == null || !fact.freshAt(input.asOf()) || !UNITS.get(key).equals(fact.unit())
                    || distribution == null || distribution.asOf().isAfter(input.asOf())
                    || !fact.unit().equals(distribution.unit()) || distribution.higherIsWorse() == key.startsWith("depth")) {
                missing.add(key);
                continue;
            }
            double percentile = distribution.adversePercentile(fact.value());
            String own = percentile >= distribution.highPercentile() ? "HIGH"
                    : percentile >= distribution.mediumPercentile() ? "MEDIUM" : "LOW";
            if (rank(own) > rank(level)) level = own;
            values.add(key + "=" + fact.value() + " " + fact.unit()); units.add(fact.unit());
            sources.add(key + ":" + fact.source());
            reasons.add(key + " 历史不利分位=" + percentile + "；中/高阈值="
                    + distribution.mediumPercentile() + "/" + distribution.highPercentile());
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
                "BINANCE_SPOT_HEALTH", input.asOf(), "核心现货源失去新鲜数据，原信号需重新确认", "SOURCE_STATE");
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
