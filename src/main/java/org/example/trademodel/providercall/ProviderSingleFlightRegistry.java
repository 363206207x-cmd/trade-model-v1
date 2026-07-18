package org.example.trademodel.providercall;

import java.util.function.Supplier;

public interface ProviderSingleFlightRegistry {
    <T> T execute(ProviderRequestKey key, Supplier<T> ownerCall);
    boolean inFlight(ProviderRequestKey key);
    int activeFlightCount();
    int waitingCallerCount();
}
