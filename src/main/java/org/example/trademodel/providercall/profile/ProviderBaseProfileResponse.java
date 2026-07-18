package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.UserScanProfile;

import java.time.Instant;

public record ProviderBaseProfileResponse(
        UserScanProfile profile,
        String profileLabel,
        String persistenceStatus,
        Instant changedAt
) {
}
