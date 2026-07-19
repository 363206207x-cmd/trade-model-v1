package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Service
public class ProviderSingleFlightGuard implements ProviderSingleFlightRegistry {
    private final Map<ProviderSnapshotKey, ProviderRefreshFlight<?>> flights = new ConcurrentHashMap<>();
    private final AtomicInteger waitingCallers = new AtomicInteger();

    @SuppressWarnings("unchecked")
    @Override
    public <T> ProviderSingleFlightRegistration<T> register(ProviderSnapshotKey key) {
        if (key == null) throw new IllegalArgumentException("snapshot key is required");
        ProviderRefreshFlight<T> candidate = new ProviderRefreshFlight<>();
        ProviderRefreshFlight<T> existing = (ProviderRefreshFlight<T>) flights.putIfAbsent(key, candidate);
        if (existing == null) {
            candidate.completion().whenComplete((value, failure) -> flights.remove(key, candidate));
            return new ProviderSingleFlightRegistration<>(candidate, true, () -> { });
        }
        waitingCallers.incrementAndGet();
        return new ProviderSingleFlightRegistration<>(existing, false, waitingCallers::decrementAndGet);
    }

    @Override
    public <T> T execute(ProviderRequestKey key, Supplier<T> ownerCall) {
        ProviderSingleFlightRegistration<T> registration = register(key.snapshotKey());
        try (registration) {
            if (registration.owner()) {
                try {
                    registration.flight().completion().complete(ownerCall.get());
                } catch (Throwable failure) {
                    registration.flight().completion().completeExceptionally(failure);
                }
            }
            return join(registration.flight().completion());
        }
    }

    @Override
    public boolean inFlight(ProviderSnapshotKey key) {
        return key != null && flights.containsKey(key);
    }

    @Override
    public boolean inFlight(ProviderRequestKey key) {
        return key != null && inFlight(key.snapshotKey());
    }

    @Override
    public int activeFlightCount() {
        return flights.size();
    }

    @Override
    public int waitingCallerCount() {
        return waitingCallers.get();
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) throw runtime;
            throw ex;
        }
    }
}
