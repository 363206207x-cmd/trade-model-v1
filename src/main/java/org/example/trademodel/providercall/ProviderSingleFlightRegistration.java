package org.example.trademodel.providercall;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ProviderSingleFlightRegistration<T> implements AutoCloseable {
    private final ProviderRefreshFlight<T> flight;
    private final boolean owner;
    private final Runnable closeAction;
    private final AtomicBoolean closed = new AtomicBoolean();

    ProviderSingleFlightRegistration(ProviderRefreshFlight<T> flight, boolean owner, Runnable closeAction) {
        this.flight = flight;
        this.owner = owner;
        this.closeAction = closeAction;
    }

    public ProviderRefreshFlight<T> flight() {
        return flight;
    }

    public boolean owner() {
        return owner;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) closeAction.run();
    }
}
