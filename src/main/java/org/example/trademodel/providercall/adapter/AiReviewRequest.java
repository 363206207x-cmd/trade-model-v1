package org.example.trademodel.providercall.adapter;

import org.example.trademodel.enums.AiRoleEnum;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.Set;

public record AiReviewRequest(
        CanonicalInstrumentId canonicalInstrumentId,
        Set<AiRoleEnum> roles,
        String evidenceHash,
        String ruleVersion,
        String traceId
) {
    public AiReviewRequest {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
