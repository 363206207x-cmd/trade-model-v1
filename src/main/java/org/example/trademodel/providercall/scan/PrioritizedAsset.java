package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;

public record PrioritizedAsset(
        CanonicalInstrumentId canonicalInstrumentId,
        String providerSymbol,
        AssetPriority priority
) {
    public String symbol() {
        return providerSymbol;
    }
}
