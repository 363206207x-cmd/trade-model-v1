package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.ProviderConcurrencyGuard;
import org.example.trademodel.providercall.ProviderHealthRegistry;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.notification.OpportunityScope;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProviderCallRuntimeStatus(
        UserScanProfile userBaseProfile,
        String baseProfileLabel,
        Map<String, RuntimeScanProfile> runtimeProfilesBySymbol,
        Map<String, RuntimeScanProfile> effectiveProfilesBySymbol,
        Map<String, String> effectiveProfileLabels,
        Map<String, List<String>> escalationReasons,
        Map<String, Instant> downgradeEligibleAt,
        String frequencyMatrixVersion,
        int positionPriceRefreshSeconds,
        int watchlistPriceRefreshSeconds,
        int candidatePriceRefreshSeconds,
        int discoveryScanSeconds,
        Map<String, ProviderBudgetState> providerBudgets,
        Map<String, ProviderHealthRegistry.ProviderHealthSnapshot> providerStatuses,
        ProviderConcurrencyGuard.ConcurrencyState concurrency,
        int manualWatchlistCount,
        int autoCandidateCount,
        int discoveryPoolCount,
        OpportunityScope opportunityScope
) {
    public ProviderCallRuntimeStatus {
        runtimeProfilesBySymbol = Map.copyOf(runtimeProfilesBySymbol);
        effectiveProfilesBySymbol = Map.copyOf(effectiveProfilesBySymbol);
        effectiveProfileLabels = Map.copyOf(effectiveProfileLabels);
        escalationReasons = Map.copyOf(escalationReasons);
        downgradeEligibleAt = Map.copyOf(downgradeEligibleAt);
        providerBudgets = Map.copyOf(providerBudgets);
        providerStatuses = Map.copyOf(providerStatuses);
    }
}
