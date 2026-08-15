package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallCoordinator;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.instrument.ProviderCapabilityRegistry;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CoinGlassLiquidationSnapshotService
        extends AbstractCoinGlassDatasetSnapshotService<CoinGlassLiquidationSnapshot> {
    private final CoinGlassV4ProviderAdapter adapter;

    @org.springframework.beans.factory.annotation.Autowired
    public CoinGlassLiquidationSnapshotService(ProviderCallCoordinator coordinator, CoinGlassProperties properties,
                                                CoinGlassSymbolMapper symbolMapper,
                                                CoinGlassV4ProviderAdapter adapter,
                                                ProviderCapabilityRegistry capabilityRegistry) {
        super(ProviderDatasetType.COINGLASS_LIQUIDATION, "1M", coordinator, properties, symbolMapper,
                capabilityRegistry);
        this.adapter = adapter;
    }

    public CoinGlassLiquidationSnapshotService(ProviderCallCoordinator coordinator, CoinGlassProperties properties,
                                                CoinGlassSymbolMapper symbolMapper,
                                                CoinGlassV4ProviderAdapter adapter) {
        super(ProviderDatasetType.COINGLASS_LIQUIDATION, "1M", coordinator, properties, symbolMapper);
        this.adapter = adapter;
    }

    public ProviderCallResult<CoinGlassLiquidationSnapshot> get(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return get(symbol, priority, freshTtl, traceId, () -> adapter.fetchLiquidation(symbol));
    }

    public ProviderCallResult<CoinGlassLiquidationSnapshot> peek(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return super.peek(symbol, priority, freshTtl, traceId);
    }
}
