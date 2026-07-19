package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.UserScanProfile;

import java.time.Instant;

public interface ProviderCallProfilePreferenceService {
    UserScanProfile getBaseProfile();

    ProfilePreferenceChange setBaseProfile(UserScanProfile profile, String actor, String reason);

    record ProfilePreferenceChange(
            UserScanProfile previousProfile,
            UserScanProfile currentProfile,
            String currentProfileLabel,
            String actor,
            String reason,
            Instant changedAt,
            String persistenceStatus
    ) {
    }
}
