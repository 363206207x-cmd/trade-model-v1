package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.RuntimeScanProfile;

import java.time.Instant;

public record ProfileTransitionResult(
        String symbol,
        RuntimeScanProfile previousProfile,
        RuntimeScanProfile effectiveProfile,
        String effectiveReason,
        Instant effectiveSince,
        Instant nextDowngradeEligibleAt,
        String ruleVersion,
        boolean changed,
        String traceId
) {
}
