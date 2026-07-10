package org.example.trademodel.providercall;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public record ProviderCallRequest<T>(
        ProviderRequestKey key,
        AssetPriority priority,
        Duration freshTtl,
        Duration staleTtl,
        Duration timeout,
        String traceId,
        int maxRetry5xx,
        int maxRetryTimeout,
        Supplier<ProviderAdapterResponse<T>> adapterCall
) {
    public ProviderCallRequest(ProviderRequestKey key,
                               AssetPriority priority,
                               Duration freshTtl,
                               Duration staleTtl,
                               Duration timeout,
                               String traceId,
                               Supplier<ProviderAdapterResponse<T>> adapterCall) {
        this(key, priority, freshTtl, staleTtl, timeout, traceId, 2, 1, adapterCall);
    }

    public ProviderCallRequest {
        key = Objects.requireNonNull(key, "key");
        priority = Objects.requireNonNull(priority, "priority");
        freshTtl = positive(freshTtl, "freshTtl");
        staleTtl = positive(staleTtl, "staleTtl");
        timeout = positive(timeout, "timeout");
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId is required");
        if (maxRetry5xx < 0) throw new IllegalArgumentException("maxRetry5xx must not be negative");
        if (maxRetryTimeout < 0) throw new IllegalArgumentException("maxRetryTimeout must not be negative");
        adapterCall = Objects.requireNonNull(adapterCall, "adapterCall");
    }

    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
