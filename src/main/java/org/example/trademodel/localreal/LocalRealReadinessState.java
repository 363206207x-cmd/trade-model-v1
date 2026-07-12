package org.example.trademodel.localreal;

public enum LocalRealReadinessState {
    STARTING,
    MARKET_BOOTSTRAPPING,
    MARKET_READY,
    ANALYSIS_RUNNING,
    DASHBOARD_READY,
    DASHBOARD_PARTIAL,
    DEGRADED,
    FAILED
}
