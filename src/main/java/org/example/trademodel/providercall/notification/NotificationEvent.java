package org.example.trademodel.providercall.notification;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

import java.time.Instant;

public record NotificationEvent(
        NotificationType type,
        CanonicalInstrumentId canonicalInstrumentId,
        String dedupKey,
        String evidenceHash,
        String planId,
        String riskLevel,
        Instant createdAt,
        boolean notTradeInstruction,
        boolean manualDecisionRequired
) {
    public NotificationEvent {
        notTradeInstruction = true;
        manualDecisionRequired = true;
    }
}
