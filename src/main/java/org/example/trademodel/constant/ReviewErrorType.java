package org.example.trademodel.constant;

/**
 * 复盘错误类型受控词表（与前端 select 选项一致）。
 */
public enum ReviewErrorType {

    DATA_ISSUE,
    RULE_TOO_LOOSE,
    RULE_TOO_STRICT,
    TF_CONFLICT_MISJUDGED,
    EVENT_RISK_UNDERWEIGHT,
    LEVERAGE_RISK_UNDERWEIGHT,
    LIQUIDITY_TRAP_MISSED,
    AI_OVERRULE_BIAS,
    PLAN_EXECUTION_MISMATCH,
    UNKNOWN;

    /**
     * {@code null}（表示未填/空）合法；非空时必须为本枚举常量名。
     */
    public static void validateAllowedOrThrow(String trimmedOrNull) {
        if (trimmedOrNull == null) {
            return;
        }
        try {
            ReviewErrorType.valueOf(trimmedOrNull);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported errorType: " + trimmedOrNull);
        }
    }
}
