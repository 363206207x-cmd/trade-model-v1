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
    private final Map<ProviderRequestKey, CompletableFuture<?>> flights = new ConcurrentHashMap<>();
    private final AtomicInteger waitingCallers = new AtomicInteger();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T execute(ProviderRequestKey key, Supplier<T> ownerCall) {
        CompletableFuture<T> owned = new CompletableFuture<>();
        CompletableFuture<T> existing = (CompletableFuture<T>) flights.putIfAbsent(key, owned);
        if (existing != null) {
            waitingCallers.incrementAndGet();
            try {
                return join(existing);
            } finally {
                waitingCallers.decrementAndGet();
            }
        }
        try {
            T value = ownerCall.get();
            owned.complete(value);
            return value;
        } catch (Throwable failure) {
            owned.completeExceptionally(failure);
            throw failure;
        } finally {
            flights.remove(key, owned);
        }
    }

    @Override
    public boolean inFlight(ProviderRequestKey key) {
        return key != null && flights.containsKey(key);
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
