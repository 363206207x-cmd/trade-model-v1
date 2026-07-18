package org.example.trademodel.providercall.adapter;

import org.example.trademodel.enums.AiRoleEnum;

import java.util.Map;

public record AiReviewSnapshot(
        Map<AiRoleEnum, String> roleStatuses,
        String evidenceHash,
        String ruleVersion
) {
    public AiReviewSnapshot {
        roleStatuses = roleStatuses == null ? Map.of() : Map.copyOf(roleStatuses);
    }
}
