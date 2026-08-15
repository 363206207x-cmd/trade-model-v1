package org.example.trademodel.providercall.instrument;

import java.time.Instant;
import java.util.List;

public record ProviderInstrumentCapability(
        String provider,
        String canonicalAssetId,
        String baseAsset,
        String quoteAsset,
        MarketType marketType,
        ContractType contractType,
        String providerSymbol,
        List<String> supportedTimeframes,
        ProviderCapabilityState capabilityState,
        String sourceVersion,
        Instant verifiedAt,
        String failureReason,
        Instant observedAt
) {
    public ProviderInstrumentCapability {
        supportedTimeframes = supportedTimeframes == null ? List.of() : List.copyOf(supportedTimeframes);
    }

    public boolean usableFor(String timeframe) {
        return capabilityState == ProviderCapabilityState.SUPPORTED
                && ("GLOBAL".equals(timeframe)
                || (timeframe != null && supportedTimeframes.contains(timeframe)));
    }

    public CanonicalInstrumentId canonicalInstrumentId() {
        return new CanonicalInstrumentId(baseAsset, quoteAsset, marketType, provider, contractType);
    }

    public ProviderSymbolMapping mapping() {
        return new ProviderSymbolMapping(provider, canonicalInstrumentId(), providerSymbol, true,
                sourceVersion, supportedTimeframes, verifiedAt);
    }
}
