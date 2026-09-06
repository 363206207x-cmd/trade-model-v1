package org.example.trademodel.v41;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** Builds a non-trading conditional rule plan for every formal direction. */
public final class V41StructuralPlanPolicy {
    public static final String VERSION = "V41-STRUCTURAL-PLAN-2";

    private V41StructuralPlanPolicy() {
    }

    public static Plan build(Input input) {
        if (input == null || !directional(input.direction()) || !positive(input.price()) || !positive(input.atr())) {
            return new Plan(VERSION, input == null ? null : input.direction(), null, null, null,
                    null, null, null, null, "WAITING", "方向或闭合行情不足",
                    "等待1小时与4小时结构形成可验证方向", input == null ? null : input.analysisId(),
                    input == null ? null : input.decisionId(), input == null ? null : input.snapshotVersion(),
                    input == null ? null : input.dataSourceSummary(), input == null ? null : input.generatedAt());
        }
        boolean bullish = input.direction().contains("BULLISH");
        BigDecimal entryLow = bullish ? input.price().subtract(input.atr().multiply(new BigDecimal("0.35")))
                : input.price().add(input.atr().multiply(new BigDecimal("0.10")));
        BigDecimal entryHigh = bullish ? input.price().subtract(input.atr().multiply(new BigDecimal("0.10")))
                : input.price().add(input.atr().multiply(new BigDecimal("0.35")));
        BigDecimal stop = positive(input.invalidationLevel()) ? input.invalidationLevel()
                : bullish ? input.price().subtract(input.atr()) : input.price().add(input.atr());
        BigDecimal risk = bullish ? input.price().subtract(stop).abs() : stop.subtract(input.price()).abs();
        if (!positive(risk)) risk = input.atr();
        BigDecimal target1 = bullish ? input.price().add(risk.multiply(new BigDecimal("1.5")))
                : input.price().subtract(risk.multiply(new BigDecimal("1.5")));
        BigDecimal target2 = bullish ? input.price().add(risk.multiply(new BigDecimal("2.2")))
                : input.price().subtract(risk.multiply(new BigDecimal("2.2")));
        String state = input.suspended() ? "SUSPENDED" : "WAITING_TRIGGER";
        String reason = input.suspended() ? text(input.blockReason(), "实时冲击，暂停执行")
                : bullish ? "等待回踩进入结构区并确认" : "等待反弹进入结构区并确认";
        String recovery = input.suspended() ? "新AnalysisRun完成重验且冲击、流动性恢复"
                : "触发后重新验证行情新鲜度、风险与结构失效位";
        return new Plan(VERSION, input.direction(), fmt(entryLow) + " – " + fmt(entryHigh),
                bullish ? "回踩企稳且1小时结构未破" : "反弹受阻且1小时结构未破",
                stop, stop, target1, target2, new BigDecimal("1.5"), state, reason, recovery,
                input.analysisId(), input.decisionId(), input.snapshotVersion(), input.dataSourceSummary(),
                input.generatedAt());
    }

    private static String fmt(BigDecimal value) {
        return value.setScale(value.abs().compareTo(new BigDecimal("10")) >= 0 ? 2 : 6,
                RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static boolean directional(String value) {
        return value != null && (value.contains("BULLISH") || value.contains("BEARISH"));
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record Input(String direction, BigDecimal price, BigDecimal atr, BigDecimal invalidationLevel,
                        String analysisId, String decisionId, Long snapshotVersion, Instant generatedAt,
                        String dataSourceSummary, boolean suspended, String blockReason) {
    }

    public record Plan(String version, String direction, String entryZone, String entryTrigger,
                       BigDecimal invalidationLevel, BigDecimal stopLoss, BigDecimal takeProfit1,
                       BigDecimal takeProfit2, BigDecimal riskRewardRatio, String planState,
                       String blockedOrWaitingReason, String recoveryCondition, String analysisId,
                       String decisionId, Long snapshotVersion, String dataSourceSummary,
                       Instant generatedAt) {
    }
}
