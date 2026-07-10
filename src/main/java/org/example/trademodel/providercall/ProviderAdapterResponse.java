package org.example.trademodel.providercall;

import java.time.Instant;

public record ProviderAdapterResponse<T>(
        T payload,
        UnifiedSourceStatus sourceStatus,
        Instant providerDataTime,
        int httpStatus,
        Long retryAfterSeconds,
        String reasonCode
) {
    public static <T> ProviderAdapterResponse<T> ready(T payload, Instant providerDataTime) {
        return new ProviderAdapterResponse<>(payload, UnifiedSourceStatus.READY, providerDataTime, 200, null, null);
    }

    public static <T> ProviderAdapterResponse<T> failed(
            UnifiedSourceStatus status, int httpStatus, String reasonCode, Long retryAfterSeconds) {
        return new ProviderAdapterResponse<>(null, status, null, httpStatus, retryAfterSeconds, reasonCode);
    }

    public boolean ready() {
        return sourceStatus == UnifiedSourceStatus.READY && payload != null;
    }
}
