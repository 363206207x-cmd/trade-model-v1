package org.example.trademodel.providercall;

import java.time.Duration;

public interface ProviderSnapshotQueryService {
    <T> ProviderCallResult<T> query(ProviderRequestKey key,
                                    AssetPriority priority,
                                    Duration freshTtl,
                                    String traceId);
}
