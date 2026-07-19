package org.example.trademodel.providercall;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** A Single Flight entry lives until the physical lifecycle completes, not until a caller times out. */
public final class ProviderRefreshFlight<T> {
    private static final Runnable NO_CANCELLATION = () -> { };

    private final CompletableFuture<T> completion = new CompletableFuture<>();
    private final AtomicReference<Runnable> cancellation = new AtomicReference<>(NO_CANCELLATION);

    public CompletableFuture<T> completion() {
        return completion;
    }

    public void setCancellation(Runnable action) {
        cancellation.set(action == null ? NO_CANCELLATION : action);
    }

    public void clearCancellation(Runnable action) {
        if (action != null) cancellation.compareAndSet(action, NO_CANCELLATION);
    }

    public void requestCancellation() {
        cancellation.get().run();
    }
}
