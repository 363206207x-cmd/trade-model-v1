package org.example.trademodel.providercall.profile;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProviderCallProfileResolver {
    public ProfileResolution resolve(UserScanProfile baseProfile,
                                     AssetPriority priority,
                                     RuntimeScanProfile runtimeEscalation,
                                     String runtimeReason,
                                     Instant downgradeEligibleAt) {
        RuntimeScanProfile userFloor = toRuntime(baseProfile);
        RuntimeScanProfile positionFloor = RuntimeScanProfile.LOW;
        RuntimeScanProfile effective = RuntimeScanProfile.max(userFloor, positionFloor, runtimeEscalation);
        List<String> reasons = new ArrayList<>();
        if (baseProfile == UserScanProfile.HIGH) reasons.add("MANUAL_HIGH");
        if (priority == AssetPriority.P0_POSITION) reasons.add("ACTIVE_POSITION");
        if (runtimeEscalation != null && runtimeEscalation.rank() > userFloor.rank()) {
            reasons.add(runtimeReason == null || runtimeReason.isBlank() ? "RUNTIME_ESCALATION" : runtimeReason);
        }
        if (reasons.isEmpty()) reasons.add(baseProfile == UserScanProfile.AUTO
                ? "AUTO_BASE_STANDARD" : "CONFIGURED_BASE_PROFILE");
        return new ProfileResolution(baseProfile == null ? UserScanProfile.AUTO : baseProfile,
                effective, reasons, downgradeEligibleAt);
    }

    private static RuntimeScanProfile toRuntime(UserScanProfile profile) {
        if (profile == null || profile == UserScanProfile.AUTO) return RuntimeScanProfile.STANDARD;
        return RuntimeScanProfile.valueOf(profile.name());
    }

    public record ProfileResolution(
            UserScanProfile baseProfile,
            RuntimeScanProfile effectiveProfile,
            List<String> reasonCodes,
            Instant downgradeEligibleAt
    ) {
        public ProfileResolution {
            reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        }
    }
}
