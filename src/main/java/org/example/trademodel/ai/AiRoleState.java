package org.example.trademodel.ai;

/** Runtime state of one AI role. It never substitutes for collection state. */
public enum AiRoleState {
    READY,
    PARTIAL,
    FALLBACK,
    UNAVAILABLE,
    ERROR
}
