package org.example.trademodel.providercall;

import java.time.Instant;

public record ProviderCallAuditEvent(
        String traceId,
        String requestKey,
        AssetPriority priority,
        UnifiedSourceStatus sourceStatus,
        boolean cacheHit,
        boolean staleFallback,
        String reasonCode,
        Instant recordedAt
) {
}
