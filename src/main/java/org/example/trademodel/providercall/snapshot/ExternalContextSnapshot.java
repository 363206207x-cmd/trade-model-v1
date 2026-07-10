package org.example.trademodel.providercall.snapshot;

import org.example.trademodel.providercall.ProviderSnapshotMetadata;

import java.util.List;

public record ExternalContextSnapshot(
        String symbol,
        List<String> materialEvents,
        List<String> reasonCodes,
        ProviderSnapshotMetadata metadata
) {
    public ExternalContextSnapshot {
        materialEvents = materialEvents == null ? List.of() : List.copyOf(materialEvents);
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
