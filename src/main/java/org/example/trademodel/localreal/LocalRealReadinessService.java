package org.example.trademodel.localreal;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Profile("local-real")
public class LocalRealReadinessService {
    private final AtomicReference<LocalRealReadinessState> state =
            new AtomicReference<>(LocalRealReadinessState.STARTING);
    private volatile String reasonCode = "LOCAL_REAL_STARTING";
    private volatile Instant updatedAt = Instant.now();

    public LocalRealReadinessState state() {
        return state.get();
    }

    public String reasonCode() {
        return reasonCode;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void transition(LocalRealReadinessState next, String reason) {
        state.set(next == null ? LocalRealReadinessState.FAILED : next);
        reasonCode = reason == null || reason.isBlank() ? "LOCAL_REAL_REASON_MISSING" : reason;
        updatedAt = Instant.now();
    }
}
