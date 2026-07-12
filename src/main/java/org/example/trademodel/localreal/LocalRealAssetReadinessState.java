package org.example.trademodel.localreal;

public enum LocalRealAssetReadinessState {
    NO_DATA,
    BOOTSTRAPPING,
    READY,
    STALE,
    DEGRADED,
    UNAVAILABLE
}
