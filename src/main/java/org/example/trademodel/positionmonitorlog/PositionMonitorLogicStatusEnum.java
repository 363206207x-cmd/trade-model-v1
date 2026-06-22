package org.example.trademodel.positionmonitorlog;

import java.util.Locale;

public enum PositionMonitorLogicStatusEnum {
    LOGIC_VALID,
    LOGIC_WEAKENED,
    PLAN_INVALIDATED,
    HIGH_RISK;

    public static PositionMonitorLogicStatusEnum parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("logic_status is required");
        }
        try {
            return PositionMonitorLogicStatusEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("logic_status must be one of LOGIC_VALID, LOGIC_WEAKENED, PLAN_INVALIDATED, HIGH_RISK");
        }
    }
}
