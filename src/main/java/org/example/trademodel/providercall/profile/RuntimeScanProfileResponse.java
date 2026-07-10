package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.scan.ProviderRefreshObservation;

import java.time.Instant;

public record RuntimeScanProfileResponse(
        String symbol,
        UserScanProfile configuredProfile,
        RuntimeScanProfile effectiveProfile,
        AssetPriority priority,
        String effectiveReason,
        Instant effectiveSince,
        Instant nextDowngradeEligibleAt,
        int priceIntervalSeconds,
        int derivativesIntervalSeconds,
        ProviderRefreshObservation lastPriceStatus,
        ProviderRefreshObservation lastDerivativesStatus,
        ProviderBudgetState providerBudgetState
) {
}
