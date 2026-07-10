package org.example.trademodel.derivatives;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;

import java.time.Duration;

public interface DerivativesSnapshotReadPort {
    ProviderCallResult<DerivativesRiskSnapshot> readCached(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId);
}
