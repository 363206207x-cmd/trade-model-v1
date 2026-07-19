package org.example.trademodel.providercall.candidate;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.util.Locale;
import java.util.Objects;

/** Stable thesis identity used for promotion confirmation; evidence content is intentionally excluded. */
public record CandidateLogicIdentity(
        CanonicalInstrumentId canonicalInstrumentId,
        String strategyVersion,
        String ruleVersion,
        String directionFamily,
        String candidateState,
        String triggerLogicType
) {
    public CandidateLogicIdentity {
        canonicalInstrumentId = Objects.requireNonNull(canonicalInstrumentId, "canonicalInstrumentId");
        strategyVersion = normalize(strategyVersion, "strategyVersion");
        ruleVersion = normalize(ruleVersion, "ruleVersion");
        directionFamily = normalize(directionFamily, "directionFamily");
        candidateState = normalize(candidateState, "candidateState");
        triggerLogicType = normalize(triggerLogicType, "triggerLogicType");
    }

    public boolean resetsPromotion() {
        return switch (candidateState) {
            case "OBSERVING", "CONFUSED", "COOLING", "INVALIDATED" -> true;
            default -> false;
        };
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
