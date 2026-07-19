package org.example.trademodel.providercall;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderCircuitBreaker {
    private final int failureThreshold;
    private final int openSeconds;
    private final Clock clock;
    private final Map<String, Circuit> circuits = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ProviderCircuitBreaker(ProviderCallProperties properties) {
        this(properties.getFailureThreshold(), properties.getCircuitOpenSeconds(), Clock.systemUTC());
    }

    public ProviderCircuitBreaker(int failureThreshold, int openSeconds, Clock clock) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openSeconds = Math.max(1, openSeconds);
        this.clock = clock;
    }

    public synchronized ProviderCircuitPermit tryAcquire(String provider) {
        String normalizedProvider = normalize(provider);
        Circuit circuit = circuit(provider);
        if (circuit.state == ProviderCircuitState.OPEN
                && !clock.instant().isBefore(circuit.openUntil)) {
            circuit.state = ProviderCircuitState.HALF_OPEN;
            circuit.halfOpenProbeClaimed = false;
            circuit.claimedProbeId = 0L;
        }
        if (circuit.state == ProviderCircuitState.OPEN) {
            return ProviderCircuitPermit.denied(this, normalizedProvider);
        }
        if (circuit.state == ProviderCircuitState.HALF_OPEN) {
            if (circuit.halfOpenProbeClaimed) {
                return ProviderCircuitPermit.denied(this, normalizedProvider);
            }
            circuit.halfOpenProbeClaimed = true;
            circuit.claimedProbeId = ++circuit.probeSequence;
            return ProviderCircuitPermit.acquired(this, normalizedProvider, true,
                    circuit.claimedProbeId);
        }
        return ProviderCircuitPermit.acquired(this, normalizedProvider, false, 0L);
    }

    synchronized boolean settle(String provider,
                                boolean halfOpenProbe,
                                long probeId,
                                ProviderCircuitPermit.Settlement settlement) {
        Circuit circuit = circuit(provider);
        if (halfOpenProbe && (circuit.state != ProviderCircuitState.HALF_OPEN
                || !circuit.halfOpenProbeClaimed
                || circuit.claimedProbeId != probeId)) {
            return false;
        }
        switch (settlement) {
            case SUCCESS -> close(circuit);
            case REMOTE_FAILURE -> recordRemoteFailure(circuit);
            case REMOTE_REACHABLE, RELEASED_WITHOUT_REMOTE_ATTEMPT -> {
                if (halfOpenProbe) releaseProbe(circuit);
            }
        }
        return true;
    }

    public synchronized ProviderCircuitState state(String provider) {
        return circuit(provider).state;
    }

    synchronized boolean halfOpenProbeClaimed(String provider) {
        Circuit circuit = circuit(provider);
        return circuit.state == ProviderCircuitState.HALF_OPEN && circuit.halfOpenProbeClaimed;
    }

    private void recordRemoteFailure(Circuit circuit) {
        circuit.failures++;
        if (circuit.state == ProviderCircuitState.HALF_OPEN || circuit.failures >= failureThreshold) {
            circuit.state = ProviderCircuitState.OPEN;
            circuit.openUntil = clock.instant().plusSeconds(openSeconds);
            circuit.halfOpenProbeClaimed = false;
            circuit.claimedProbeId = 0L;
        }
    }

    private static void close(Circuit circuit) {
        circuit.state = ProviderCircuitState.CLOSED;
        circuit.failures = 0;
        circuit.openUntil = null;
        circuit.halfOpenProbeClaimed = false;
        circuit.claimedProbeId = 0L;
    }

    private static void releaseProbe(Circuit circuit) {
        circuit.halfOpenProbeClaimed = false;
        circuit.claimedProbeId = 0L;
    }

    private Circuit circuit(String provider) {
        return circuits.computeIfAbsent(normalize(provider), ignored -> new Circuit());
    }

    private static String normalize(String provider) {
        return provider == null ? "UNKNOWN" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private static final class Circuit {
        private ProviderCircuitState state = ProviderCircuitState.CLOSED;
        private int failures;
        private Instant openUntil;
        private boolean halfOpenProbeClaimed;
        private long probeSequence;
        private long claimedProbeId;
    }
}
