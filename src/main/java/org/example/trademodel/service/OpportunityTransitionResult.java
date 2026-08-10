package org.example.trademodel.service;

import org.example.trademodel.enums.AssetStateEnum;

import java.time.LocalDateTime;

public record OpportunityTransitionResult(
        String opportunityId,
        String symbol,
        AssetStateEnum previousState,
        AssetStateEnum state,
        boolean changed,
        boolean suppressed,
        String reason,
        String triggerSource,
        String executionPermission,
        LocalDateTime occurredAt) {
}
