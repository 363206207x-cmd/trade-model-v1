package org.example.trademodel.providercall;

import java.util.concurrent.CompletableFuture;

/** A Single Flight entry lives until the physical lifecycle completes, not until a caller times out. */
public final class ProviderRefreshFlight<T> {
    private final CompletableFuture<T> completion = new CompletableFuture<>();

    public CompletableFuture<T> completion() {
        return completion;
    }
}
