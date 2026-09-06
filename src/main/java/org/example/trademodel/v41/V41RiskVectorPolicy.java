package org.example.trademodel.v41;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Concrete, independently scored asset-opportunity risk vector. */
public final class V41RiskVectorPolicy {
    public static final String VERSION = "V41-RISK-VECTOR-1";

    private V41RiskVectorPolicy() {
    }

    public static RiskVector assess(Input input) {
        Instant observedAt = input.observedAt() == null ? Instant.now() : input.observedAt();
        List<RiskItem> items = List.of(
                item("CHASE_RISK", "追高风险", input.chase(), "价格距结构中枢", input.source(), observedAt, "价格回到结构入场区"),
                item("RAPID_MOVE_RISK", "急涨急跌", input.rapidMove(), "1m/5m稳健极值", input.source(), observedAt, "连续两根闭合5m退出冲击"),
                item("TREND_REVERSAL_RISK", "趋势反转", input.trendReversal(), "1h与4h结构", input.source(), observedAt, "1h与4h重新同向"),
                item("CROWDING_RISK", "仓位拥挤", input.crowding(), "资金费率与多空拥挤", input.source(), observedAt, "拥挤度回到历史正常区间"),
                item("LIQUIDATION_RISK", "清算放大", input.liquidation(), "清算强度", input.source(), observedAt, "清算强度回落"),
                item("LIQUIDITY_RISK", "流动性恶化", input.liquidity(), "价差、深度与滑点", input.source(), observedAt, "价差与有效深度恢复"),
                item("EVENT_RISK", "事件风险", input.event(), "事件窗口", input.source(), observedAt, "事件窗口结束并完成重算"),
                item("DATA_RISK", "数据风险", input.data(), "来源新鲜度与一致性", input.source(), observedAt, "必需数据恢复新鲜且一致"));
        List<RiskItem> sorted = items.stream().sorted(Comparator.comparingInt(RiskItem::score).reversed()).toList();
        List<RiskItem> primary = sorted.subList(0, Math.min(2, sorted.size()));
        int weighted = (int) Math.round(input.chase() * .15 + input.rapidMove() * .20
                + input.trendReversal() * .20 + input.crowding() * .10 + input.liquidation() * .10
                + input.liquidity() * .10 + input.event() * .05 + input.data() * .10);
        int overall = Math.max(sorted.get(0).score(), weighted);
        String label = primary.stream().map(risk -> risk.riskTypeLabel() + "（" + severityLabel(risk.score()) + "）")
                .reduce((left, right) -> left + "·" + right).orElse("暂无明显风险");
        return new RiskVector(VERSION, items, List.copyOf(primary), overall, label);
    }

    private static RiskItem item(String type, String label, int score, String evidence,
                                 String source, Instant observedAt, String recovery) {
        int normalized = Math.max(0, Math.min(100, score));
        return new RiskItem(type, label, normalized, severity(normalized), evidence,
                source == null ? "SOURCE_UNAVAILABLE" : source, observedAt, recovery);
    }

    private static String severity(int score) {
        return score >= 70 ? "HIGH" : score >= 35 ? "MEDIUM" : "LOW";
    }

    private static String severityLabel(int score) {
        return score >= 70 ? "高" : score >= 35 ? "中" : "低";
    }

    public record Input(int chase, int rapidMove, int trendReversal, int crowding,
                        int liquidation, int liquidity, int event, int data,
                        String source, Instant observedAt) {
    }

    public record RiskItem(String riskType, String riskTypeLabel, int score, String severity,
                           String primaryEvidence, String source, Instant observedAt,
                           String recoveryCondition) {
    }

    public record RiskVector(String version, List<RiskItem> items, List<RiskItem> primaryItems,
                             int overallRisk, String displayLabel) {
    }
}
