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
        Instant recordedAt,
        String attemptId,
        int attemptNumber,
        ProviderCallAuditPhase phase
) {
    public ProviderCallAuditEvent {
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
        attemptNumber = Math.max(0, attemptNumber);
        phase = phase == null ? ProviderCallAuditPhase.REQUEST_RESULT : phase;
    }

    public ProviderCallAuditEvent(
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
            Instant recordedAt) {
        this(traceId, requestKey, priority, baseProfile, effectiveProfile, profileReasonCodes,
                frequencyMatrixVersion, sourceStatus, cacheHit, staleFallback, reasonCode,
                recordedAt, null, 0, ProviderCallAuditPhase.REQUEST_RESULT);
    }
}
