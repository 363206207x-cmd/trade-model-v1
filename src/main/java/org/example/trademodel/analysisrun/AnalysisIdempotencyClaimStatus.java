package org.example.trademodel.analysisrun;

public enum AnalysisIdempotencyClaimStatus {
    CLAIMED_NEW,
    DUPLICATE_SUCCESS,
    IN_PROGRESS,
    RECOVERED_FAILED,
    RECOVERED_EXPIRED_LEASE,
    RECOVERY_BLOCKED_PARTIAL_STATE,
    MAX_RECOVERY_ATTEMPTS_EXCEEDED
}
