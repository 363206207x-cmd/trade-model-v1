package org.example.trademodel.telegram;

public enum TelegramReadinessState {
    DISABLED,
    NOT_CONFIGURED,
    TOKEN_MISSING,
    CHAT_ID_MISSING,
    READY,
    AUTH_FAILED,
    CHAT_UNAVAILABLE,
    RATE_LIMITED,
    PROVIDER_UNAVAILABLE,
    DEGRADED
}
