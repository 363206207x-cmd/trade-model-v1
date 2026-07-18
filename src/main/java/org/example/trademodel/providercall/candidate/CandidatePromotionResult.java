package org.example.trademodel.providercall.candidate;

import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.time.Instant;
import java.util.List;

public record CandidatePromotionResult(
        CanonicalInstrumentId canonicalInstrumentId,
        CandidatePromotionStatus status,
        boolean candidateActive,
        boolean promotionEventEligible,
        String evidenceHash,
        UserScanProfile baseProfile,
        RuntimeScanProfile effectiveProfile,
        List<String> profileReasonCodes,
        String frequencyMatrixVersion,
        List<String> reasonCodes,
        Instant evaluatedAt,
        Instant expiresAt
) {
    public CandidatePromotionResult {
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
