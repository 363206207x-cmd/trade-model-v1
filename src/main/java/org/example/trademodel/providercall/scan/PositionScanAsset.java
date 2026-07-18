package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

public record PositionScanAsset(
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        String status
) {
    public String symbol() {
        return providerSymbol;
    }

    public boolean active() {
        return "OPEN".equalsIgnoreCase(status) || "PARTIALLY_CLOSED".equalsIgnoreCase(status);
    }
}
