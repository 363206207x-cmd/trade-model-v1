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

    public synchronized boolean allowRequest(String provider) {
        Circuit circuit = circuit(provider);
        if (circuit.state == ProviderCircuitState.OPEN
                && !clock.instant().isBefore(circuit.openUntil)) {
            circuit.state = ProviderCircuitState.HALF_OPEN;
            circuit.halfOpenProbeClaimed = false;
        }
        if (circuit.state == ProviderCircuitState.OPEN) return false;
        if (circuit.state == ProviderCircuitState.HALF_OPEN) {
            if (circuit.halfOpenProbeClaimed) return false;
            circuit.halfOpenProbeClaimed = true;
        }
        return true;
    }

    public synchronized void recordSuccess(String provider) {
        Circuit circuit = circuit(provider);
        circuit.state = ProviderCircuitState.CLOSED;
        circuit.failures = 0;
        circuit.openUntil = null;
        circuit.halfOpenProbeClaimed = false;
    }

    public synchronized void recordFailure(String provider) {
        Circuit circuit = circuit(provider);
        circuit.failures++;
        if (circuit.state == ProviderCircuitState.HALF_OPEN || circuit.failures >= failureThreshold) {
            circuit.state = ProviderCircuitState.OPEN;
            circuit.openUntil = clock.instant().plusSeconds(openSeconds);
            circuit.halfOpenProbeClaimed = false;
        }
    }

    public synchronized ProviderCircuitState state(String provider) {
        return circuit(provider).state;
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
    }
}
