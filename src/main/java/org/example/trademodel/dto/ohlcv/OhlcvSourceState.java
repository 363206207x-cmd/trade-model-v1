package org.example.trademodel.dto.ohlcv;

public enum OhlcvSourceState {
    NOT_CONFIGURED,
    WAITING_SYNC,
    READY,
    EMPTY_CONFIRMED,
    STALE,
    DEGRADED,
    ERROR,
    DISABLED
}
