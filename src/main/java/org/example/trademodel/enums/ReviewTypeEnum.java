package org.example.trademodel.enums;

/** Frozen v4.1 review coverage. */
public enum ReviewTypeEnum {
    EXECUTED_VALID,
    EXECUTED_INVALID,
    MISSED_VALID,
    MISSED_INVALID,
    PUSHED_NOT_FILLED_VALID,
    BLOCKED_BY_RISK_VALID,
    USER_DEVIATION;

    public static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return valueOf(value.trim().toUpperCase()).name();
    }
}
