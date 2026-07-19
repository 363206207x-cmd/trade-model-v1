package org.example.trademodel.providercall;

import java.util.function.Supplier;

public interface ProviderSingleFlightRegistry {
    <T> ProviderSingleFlightRegistration<T> register(ProviderSnapshotKey key);
    <T> T execute(ProviderRequestKey key, Supplier<T> ownerCall);
    boolean inFlight(ProviderSnapshotKey key);
    boolean inFlight(ProviderRequestKey key);
    int activeFlightCount();
    int waitingCallerCount();
}
