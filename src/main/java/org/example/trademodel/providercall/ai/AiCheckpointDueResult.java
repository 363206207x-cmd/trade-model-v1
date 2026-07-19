package org.example.trademodel.providercall.ai;

import org.example.trademodel.enums.AiRoleEnum;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record AiCheckpointDueResult(
        AiCheckpointDueStatus status,
        AiParticipationDepth depth,
        Set<AiRoleEnum> roles,
        List<String> reasonCodes,
        Instant nextEligibleAt
) {
    public AiCheckpointDueResult {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
