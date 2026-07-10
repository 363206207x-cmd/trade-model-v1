package org.example.trademodel.derivatives;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.coinglass.CoinGlassDerivativesSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CoordinatedDerivativesSnapshotReadAdapter implements DerivativesSnapshotReadPort {
    private final CoinGlassDerivativesSnapshotService snapshotService;

    public CoordinatedDerivativesSnapshotReadAdapter(CoinGlassDerivativesSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    public ProviderCallResult<DerivativesRiskSnapshot> readCached(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        return snapshotService.peek(symbol, priority, freshTtl, traceId);
    }
}
