package org.example.trademodel.providercall.candidate;

import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.List;

public record CandidatePromotionRequest(
        CanonicalInstrumentId canonicalInstrumentId,
        boolean promotionConditionsSatisfied,
        boolean hardInvalidated,
        String evidenceHash,
        UserScanProfile baseProfile,
        RuntimeScanProfile effectiveProfile,
        List<String> profileReasonCodes,
        String frequencyMatrixVersion
) {
    public CandidatePromotionRequest {
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
    }
}
