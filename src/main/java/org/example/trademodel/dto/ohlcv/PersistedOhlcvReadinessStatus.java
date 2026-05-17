package org.example.trademodel.dto.ohlcv;

public enum PersistedOhlcvReadinessStatus {
    FRESH,
    STALE,
    PARTIAL,
    MISSING,
    UNKNOWN,
    INVALID
}
