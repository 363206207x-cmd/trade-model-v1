package org.example.trademodel.positionmonitor;

public enum PositionMonitorDataStateEnum {
    NO_POSITION,
    OPEN_MONITORING,
    PARTIAL,
    WAITING_MONITOR_DATA,
    RISK_ESCALATED,
    PLAN_INVALIDATED,
    CLOSED
}
