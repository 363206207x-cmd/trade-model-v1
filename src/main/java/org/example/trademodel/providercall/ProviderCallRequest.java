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
        Supplier<ProviderAdapterResponse<T>> adapterCall
) {
    public ProviderCallRequest {
        key = Objects.requireNonNull(key, "key");
        priority = Objects.requireNonNull(priority, "priority");
        freshTtl = positive(freshTtl, "freshTtl");
        staleTtl = positive(staleTtl, "staleTtl");
        timeout = positive(timeout, "timeout");
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId is required");
        adapterCall = Objects.requireNonNull(adapterCall, "adapterCall");
    }

    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
