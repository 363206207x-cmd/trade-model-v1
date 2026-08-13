package org.example.trademodel.localreal;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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
        assets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return java.util.Collections.unmodifiableMap(ordered);
    }

    public void retainAssets(List<String> symbols) {
        if (symbols == null) {
            assets.clear();
            return;
        }
        java.util.Set<String> retained = symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        assets.keySet().removeIf(symbol -> !retained.contains(symbol));
    }

    public long readyAssetCount() {
        return assets.values().stream()
                .filter(item -> item.state() == LocalRealAssetReadinessState.READY)
                .count();
    }
}
