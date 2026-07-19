package org.example.trademodel.providercall;

import java.util.concurrent.atomic.AtomicBoolean;

/** One-attempt circuit permission; a HALF_OPEN probe can be settled exactly once. */
public final class ProviderCircuitPermit {
    enum Settlement {
        SUCCESS,
        REMOTE_FAILURE,
        REMOTE_REACHABLE,
        RELEASED_WITHOUT_REMOTE_ATTEMPT
    }

    private final ProviderCircuitBreaker owner;
    private final String provider;
    private final boolean acquired;
    private final boolean halfOpenProbe;
    private final long probeId;
    private final AtomicBoolean settled = new AtomicBoolean();

    private ProviderCircuitPermit(ProviderCircuitBreaker owner,
                                  String provider,
                                  boolean acquired,
                                  boolean halfOpenProbe,
                                  long probeId) {
        this.owner = owner;
        this.provider = provider;
        this.acquired = acquired;
        this.halfOpenProbe = halfOpenProbe;
        this.probeId = probeId;
    }

    static ProviderCircuitPermit acquired(ProviderCircuitBreaker owner,
                                           String provider,
                                           boolean halfOpenProbe,
                                           long probeId) {
        return new ProviderCircuitPermit(owner, provider, true, halfOpenProbe, probeId);
    }

    static ProviderCircuitPermit denied(ProviderCircuitBreaker owner, String provider) {
        return new ProviderCircuitPermit(owner, provider, false, false, 0L);
    }

    public boolean acquired() {
        return acquired;
    }

    public boolean halfOpenProbe() {
        return halfOpenProbe;
    }

    public boolean settled() {
        return !acquired || settled.get();
    }

    public boolean recordSuccess() {
        return settle(Settlement.SUCCESS);
    }

    public boolean recordRemoteFailure() {
        return settle(Settlement.REMOTE_FAILURE);
    }

    public boolean recordRemoteReachable() {
        return settle(Settlement.REMOTE_REACHABLE);
    }

    public boolean releaseWithoutRemoteAttempt() {
        return settle(Settlement.RELEASED_WITHOUT_REMOTE_ATTEMPT);
    }

    private boolean settle(Settlement settlement) {
        if (!acquired || !settled.compareAndSet(false, true)) return false;
        return owner.settle(provider, halfOpenProbe, probeId, settlement);
    }
}
