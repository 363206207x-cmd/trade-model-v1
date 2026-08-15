package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CoinGlassLongShortSnapshotService
        extends AbstractCoinGlassDatasetSnapshotService<CoinGlassLongShortSnapshot> {
    private final CoinGlassV4ProviderAdapter adapter;

    @org.springframework.beans.factory.annotation.Autowired
    public CoinGlassLongShortSnapshotService(ProviderCallCoordinator coordinator, CoinGlassProperties properties,
                                              CoinGlassSymbolMapper symbolMapper,
                                              CoinGlassV4ProviderAdapter adapter,
                                              ProviderCapabilityRegistry capabilityRegistry) {
        super(ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, "1M", coordinator, properties, symbolMapper,
                capabilityRegistry);
        this.adapter = adapter;
    }

    public CoinGlassLongShortSnapshotService(ProviderCallCoordinator coordinator, CoinGlassProperties properties,
                                              CoinGlassSymbolMapper symbolMapper,
                                              CoinGlassV4ProviderAdapter adapter) {
        super(ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, "1M", coordinator, properties, symbolMapper);
        this.adapter = adapter;
    }

    public ProviderCallResult<CoinGlassLongShortSnapshot> get(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return get(symbol, priority, freshTtl, traceId, () -> adapter.fetchLongShortRatio(symbol));
    }

    public ProviderCallResult<CoinGlassLongShortSnapshot> peek(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return super.peek(symbol, priority, freshTtl, traceId);
    }
}
