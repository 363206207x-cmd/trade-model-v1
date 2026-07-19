package org.example.trademodel.providercall;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class ProviderRequestKeyFactory {
    private final ProviderSymbolMappingRegistry mappingRegistry;

    public ProviderRequestKeyFactory(ProviderSymbolMappingRegistry mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
    }

    public ProviderRequestKey create(String provider,
                                     ProviderDatasetType datasetType,
                                     CanonicalInstrumentId canonicalInstrumentId,
                                     String timeframe,
                                     Duration effectiveTtl,
                                     Instant asOf) {
        ProviderSymbolMapping mapping = mappingRegistry.resolve(provider, canonicalInstrumentId);
        return create(provider, datasetType, mapping, timeframe, effectiveTtl, asOf);
    }

    public ProviderRequestKey create(String provider,
                                     ProviderDatasetType datasetType,
                                     ProviderSymbolMapping mapping,
                                     String timeframe,
                                     Duration effectiveTtl,
                                     Instant asOf) {
        if (effectiveTtl == null || effectiveTtl.isZero() || effectiveTtl.isNegative()) {
            throw new IllegalArgumentException("effectiveTtl must be positive");
        }
        if (asOf == null) throw new IllegalArgumentException("asOf is required");
        long bucketSeconds = Math.max(1L, effectiveTtl.toSeconds());
        String timeBucket = String.valueOf(asOf.getEpochSecond() / bucketSeconds);
        return new ProviderRequestKey(provider, datasetType, mapping.canonicalInstrumentId(),
                mapping.providerSymbol(), timeframe, timeBucket, mapping.sourceVersion());
    }
}
