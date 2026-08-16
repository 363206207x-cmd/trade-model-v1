package org.example.trademodel.telegram;

public record TelegramClientResult(
        boolean success,
        int httpStatus,
        TelegramReadinessState readinessState,
        String providerReference,
        String botUsername,
        String errorCode,
        String errorMessage,
        Integer retryAfterSeconds,
        boolean retryable) {

    public static TelegramClientResult success(int status, String providerReference, String botUsername) {
        return new TelegramClientResult(true, status, TelegramReadinessState.READY,
                providerReference, botUsername, null, null, null, false);
    }

    public static TelegramClientResult failure(int status, TelegramReadinessState state,
                                               String errorCode, String errorMessage,
                                               Integer retryAfterSeconds, boolean retryable) {
        return new TelegramClientResult(false, status, state, null, null,
                errorCode, errorMessage, retryAfterSeconds, retryable);
    }
}
