package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;

import java.time.Instant;

public record ScanProfileResponse(
        UserScanProfile configuredProfile,
        UserScanProfile positionMonitorProfile,
        UserScanProfile poolProfile,
        boolean autoEscalationEnabled,
        RuntimeScanProfile effectiveProfile,
        String effectiveReason,
        Instant effectiveSince,
        Instant nextDowngradeEligibleAt,
        int positionPriceIntervalSeconds,
        int corePriceIntervalSeconds,
        int candidatePriceIntervalSeconds,
        int poolPriceIntervalSeconds,
        int derivativesIntervalSeconds,
        ProviderBudgetState providerBudgetState
) {
}
