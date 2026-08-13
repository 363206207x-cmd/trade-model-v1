package org.example.trademodel.ai;

/** Overall data state for one role response, separate from role and collection state. */
public enum AiRoleDataState {
    READY,
    INSUFFICIENT_DATA,
    SOURCE_UNAVAILABLE,
    STALE,
    AI_FAILED,
    AI_TIMEOUT,
    FALLBACK_RULE_ONLY
}
