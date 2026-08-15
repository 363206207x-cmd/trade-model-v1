package org.example.trademodel.providercall.instrument;

import java.time.Instant;

/**
 * Provider directory lookup used only to verify exact instrument capability. Implementations must not
 * call OHLCV, quote, funding, open-interest, account, order, or position endpoints.
 */
public interface ProviderCapabilityDirectory {
    String provider();

    ProviderInstrumentCapability verify(CanonicalInstrumentId requested,
                                        String timeframe,
                                        Instant observedAt);
}
