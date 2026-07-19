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
        String strategyVersion,
        String ruleVersion,
        String directionFamily,
        String candidateState,
        String triggerLogicType,
        UserScanProfile baseProfile,
        RuntimeScanProfile effectiveProfile,
        List<String> profileReasonCodes,
        String frequencyMatrixVersion
) {
    public CandidatePromotionRequest(
            CanonicalInstrumentId canonicalInstrumentId,
            boolean promotionConditionsSatisfied,
            boolean hardInvalidated,
            String evidenceHash,
            UserScanProfile baseProfile,
            RuntimeScanProfile effectiveProfile,
            List<String> profileReasonCodes,
            String frequencyMatrixVersion) {
        this(canonicalInstrumentId, promotionConditionsSatisfied, hardInvalidated, evidenceHash,
                "V1", "V1", "UNSPECIFIED", "ELIGIBLE",
                "STANDARD", baseProfile, effectiveProfile, profileReasonCodes, frequencyMatrixVersion);
    }

    public CandidatePromotionRequest {
        profileReasonCodes = profileReasonCodes == null ? List.of() : List.copyOf(profileReasonCodes);
    }

    public CandidateLogicIdentity logicIdentity() {
        return new CandidateLogicIdentity(canonicalInstrumentId, strategyVersion, ruleVersion,
                directionFamily, candidateState, triggerLogicType);
    }
}
