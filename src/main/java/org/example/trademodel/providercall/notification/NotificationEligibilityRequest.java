package org.example.trademodel.providercall.notification;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.List;

public record NotificationEligibilityRequest(
        NotificationType type,
        OpportunityScope scope,
        NotificationOrigin origin,
        CanonicalInstrumentId canonicalInstrumentId,
        AssetPriority priority,
        String strategyVersion,
        String evidenceHash,
        String planId,
        String riskLevel,
        boolean candidatePromoted,
        boolean triggered,
        boolean dataFresh,
        boolean fourTimeframesComplete,
        boolean dataQualityPassed,
        boolean entryComplete,
        boolean stopComplete,
        boolean takeProfitComplete,
        boolean rewardRiskComputable,
        boolean planNotExpired,
        boolean riskGatePassed,
        boolean confusedBlocked,
        boolean hotResetReviewComplete,
        boolean pushRecheckPassed,
        boolean systemDataWarning,
        UserScanProfile baseProfile,
        RuntimeScanProfile effectiveProfile,
        List<String> profileReasonCodes,
        String frequencyMatrixVersion
) {
    public NotificationEligibilityRequest {
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
    }
}
