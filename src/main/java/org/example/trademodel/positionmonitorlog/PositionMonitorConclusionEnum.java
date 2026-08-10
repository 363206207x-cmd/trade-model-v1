package org.example.trademodel.positionmonitorlog;

public enum PositionMonitorConclusionEnum {
    LOGIC_VALID,
    LOGIC_WEAKENED,
    PLAN_INVALIDATED,
    NEAR_STOP_LOSS,
    NEAR_TAKE_PROFIT,
    HIGH_RISK_OBSERVATION,
    WAIT_USER_CONFIRM_CLOSE
}
