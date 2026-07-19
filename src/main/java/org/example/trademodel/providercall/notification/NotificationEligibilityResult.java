package org.example.trademodel.providercall.notification;

import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.List;

public record NotificationEligibilityResult(
        NotificationType type,
        CanonicalInstrumentId canonicalInstrumentId,
        boolean eligible,
        String dedupKey,
        List<String> reasonCodes,
        UserScanProfile baseProfile,
        RuntimeScanProfile effectiveProfile,
        List<String> profileReasonCodes,
        String frequencyMatrixVersion,
        boolean notTradeInstruction,
        boolean manualDecisionRequired
) {
    public NotificationEligibilityResult {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
        notTradeInstruction = true;
        manualDecisionRequired = true;
    }
}
