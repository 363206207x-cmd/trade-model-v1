package org.example.trademodel.positionmonitorlog;

import java.util.Locale;

public enum PositionMonitorSuggestedActionEnum {
    HOLD,
    MANUAL_REVIEW,
    RECHECK_PLAN,
    RISK_REVIEW;

    public static PositionMonitorSuggestedActionEnum parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("suggested_action is required");
        }
        try {
            return PositionMonitorSuggestedActionEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("suggested_action must be one of HOLD, MANUAL_REVIEW, RECHECK_PLAN, RISK_REVIEW");
        }
    }
}
