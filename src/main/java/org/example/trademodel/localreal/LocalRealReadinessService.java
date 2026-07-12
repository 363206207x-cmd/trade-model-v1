package org.example.trademodel.localreal;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Profile("local-real")
public class LocalRealReadinessService {
    private final AtomicReference<LocalRealReadinessState> state =
            new AtomicReference<>(LocalRealReadinessState.STARTING);
    private volatile String reasonCode = "LOCAL_REAL_STARTING";
    private volatile Instant updatedAt = Instant.now();
    private final Map<String, LocalRealAssetReadiness> assets = new ConcurrentHashMap<>();

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

    public void updateAsset(String symbol, LocalRealAssetReadinessState next, String provider, String reason) {
        if (symbol == null || symbol.isBlank()) return;
        String normalized = symbol.trim().toUpperCase(java.util.Locale.ROOT);
        assets.put(normalized, new LocalRealAssetReadiness(normalized,
                next == null ? LocalRealAssetReadinessState.DEGRADED : next,
                provider, reason, Instant.now()));
    }

    public LocalRealAssetReadiness asset(String symbol) {
        if (symbol == null || symbol.isBlank()) return null;
        return assets.get(symbol.trim().toUpperCase(java.util.Locale.ROOT));
    }

    public Map<String, LocalRealAssetReadiness> assets() {
        Map<String, LocalRealAssetReadiness> ordered = new LinkedHashMap<>();
        for (String symbol : LocalRealDataCoordinator.SYMBOLS) {
            LocalRealAssetReadiness item = assets.get(symbol);
            if (item != null) ordered.put(symbol, item);
        }
        return Map.copyOf(ordered);
    }

    public long readyAssetCount() {
        return assets.values().stream()
                .filter(item -> item.state() == LocalRealAssetReadinessState.READY)
                .count();
    }
}
