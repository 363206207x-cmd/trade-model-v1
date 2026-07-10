package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CoinGlassFundingSnapshotService
        extends AbstractCoinGlassDatasetSnapshotService<CoinGlassFundingSnapshot> {
    private final CoinGlassV4ProviderAdapter adapter;

    public CoinGlassFundingSnapshotService(ProviderCallCoordinator coordinator, CoinGlassProperties properties,
                                            CoinGlassSymbolMapper symbolMapper, CoinGlassV4ProviderAdapter adapter) {
        super(ProviderDatasetType.COINGLASS_FUNDING, "1M", coordinator, properties, symbolMapper);
        this.adapter = adapter;
    }

    public ProviderCallResult<CoinGlassFundingSnapshot> get(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return get(symbol, priority, freshTtl, traceId, () -> adapter.fetchFunding(symbol));
    }
}
