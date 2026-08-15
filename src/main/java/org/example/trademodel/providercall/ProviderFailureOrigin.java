package org.example.trademodel.providercall;

/** Separates local coordination pressure from failures attributable to a remote provider. */
public enum ProviderFailureOrigin {
    NONE(false, false),
    LOCAL_ADMISSION(false, false),
    LOCAL_BUDGET(false, false),
    LOCAL_CONCURRENCY(false, false),
    LOCAL_CONFIGURATION(false, false),
    REMOTE_RATE_LIMIT(false, true),
    REMOTE_AUTH(false, true),
    REMOTE_CAPABILITY(false, true),
    REMOTE_TRANSPORT(true, true),
    REMOTE_SERVER(true, true),
    REMOTE_PAYLOAD(true, true),
    CALLER_TIMEOUT(false, false);

    private final boolean circuitFailure;
    private final boolean remoteHealthFailure;

    ProviderFailureOrigin(boolean circuitFailure, boolean remoteHealthFailure) {
        this.circuitFailure = circuitFailure;
        this.remoteHealthFailure = remoteHealthFailure;
    }

    public boolean affectsProviderCircuit() {
        return circuitFailure;
    }

    public boolean recordsRemoteHealthFailure() {
        return remoteHealthFailure;
    }
}
