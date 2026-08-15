package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CoinGlassOpenInterestSnapshotService
        extends AbstractCoinGlassDatasetSnapshotService<CoinGlassOpenInterestSnapshot> {
    private final CoinGlassV4ProviderAdapter adapter;

    @org.springframework.beans.factory.annotation.Autowired
    public CoinGlassOpenInterestSnapshotService(ProviderCallCoordinator coordinator, CoinGlassProperties properties,
                                                 CoinGlassSymbolMapper symbolMapper,
                                                 CoinGlassV4ProviderAdapter adapter,
                                                 ProviderCapabilityRegistry capabilityRegistry) {
        super(ProviderDatasetType.COINGLASS_OPEN_INTEREST, "CURRENT", coordinator, properties, symbolMapper,
                capabilityRegistry);
        this.adapter = adapter;
    }

    public CoinGlassOpenInterestSnapshotService(ProviderCallCoordinator coordinator, CoinGlassProperties properties,
                                                 CoinGlassSymbolMapper symbolMapper,
                                                 CoinGlassV4ProviderAdapter adapter) {
        super(ProviderDatasetType.COINGLASS_OPEN_INTEREST, "CURRENT", coordinator, properties, symbolMapper);
        this.adapter = adapter;
    }

    public ProviderCallResult<CoinGlassOpenInterestSnapshot> get(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return get(symbol, priority, freshTtl, traceId, () -> adapter.fetchOpenInterest(symbol));
    }

    public ProviderCallResult<CoinGlassOpenInterestSnapshot> peek(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return super.peek(symbol, priority, freshTtl, traceId);
    }
}
