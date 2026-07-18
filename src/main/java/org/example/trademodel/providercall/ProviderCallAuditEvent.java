package org.example.trademodel.providercall;

import java.time.Instant;
import java.util.List;

public record ProviderCallAuditEvent(
        String traceId,
        String requestKey,
        AssetPriority priority,
        UserScanProfile baseProfile,
        RuntimeScanProfile effectiveProfile,
        List<String> profileReasonCodes,
        String frequencyMatrixVersion,
        UnifiedSourceStatus sourceStatus,
        boolean cacheHit,
        boolean staleFallback,
        String reasonCode,
        Instant recordedAt
) {
    public ProviderCallAuditEvent {
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
    }
}
