package org.example.trademodel.positionmonitorlog;

import java.util.Locale;

public enum PositionMonitorSuggestedActionEnum {
    CONTINUE_HOLD,
    NO_ADD_POSITION,
    REDUCE_POSITION,
    TIGHTEN_STOP,
    MOVE_STOP,
    PARTIAL_TAKE_PROFIT,
    WAIT_CONFIRMATION,
    RECORD_CLOSE_REVIEW;

    public static PositionMonitorSuggestedActionEnum parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("suggested_action is required");
        }
        try {
            return PositionMonitorSuggestedActionEnum.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("suggested_action is not a supported manual advisory action");
        }
    }

    public boolean isAllowedFor(PositionMonitorConclusionEnum conclusion) {
        if (conclusion == null) {
            return false;
        }
        return switch (conclusion) {
            case LOGIC_VALID -> this == CONTINUE_HOLD;
            case LOGIC_WEAKENED -> this == NO_ADD_POSITION;
            case PLAN_INVALIDATED -> this == WAIT_CONFIRMATION;
            case NEAR_STOP_LOSS -> this == TIGHTEN_STOP || this == MOVE_STOP;
            case NEAR_TAKE_PROFIT -> this == PARTIAL_TAKE_PROFIT;
            case HIGH_RISK_OBSERVATION -> this == REDUCE_POSITION || this == WAIT_CONFIRMATION;
            case WAIT_USER_CONFIRM_CLOSE -> this == RECORD_CLOSE_REVIEW || this == WAIT_CONFIRMATION;
        };
    }
}
