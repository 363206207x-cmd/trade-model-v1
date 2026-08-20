package org.example.trademodel.localreal;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AnalysisRunMapper analysisRunMapper;
    private final AtomicReference<LocalRealReadinessState> state =
            new AtomicReference<>(LocalRealReadinessState.STARTING);
    private volatile String reasonCode = "LOCAL_REAL_STARTING";
    private volatile Instant updatedAt = Instant.now();
    private final Map<String, LocalRealAssetReadiness> assets = new ConcurrentHashMap<>();

    public LocalRealReadinessService() {
        this(null);
    }

    @Autowired
    public LocalRealReadinessService(AnalysisRunMapper analysisRunMapper) {
        this.analysisRunMapper = analysisRunMapper;
    }

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

    public void synchronizeTrackedAssets(List<String> symbols) {
        retainAssets(symbols);
        if (symbols == null) {
            return;
        }
        symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(java.util.Locale.ROOT))
                .distinct()
                .forEach(symbol -> assets.putIfAbsent(symbol, new LocalRealAssetReadiness(
                        symbol, LocalRealAssetReadinessState.NO_DATA, null,
                        "ANALYSIS_NOT_COMPLETED", Instant.now())));
    }

    public void refreshFromPersistedAnalyses(List<String> symbols) {
        synchronizeTrackedAssets(symbols);
        if (analysisRunMapper == null) {
            transition(LocalRealReadinessState.DEGRADED, "ANALYSIS_PERSISTENCE_UNAVAILABLE");
            return;
        }
        for (String symbol : assets.keySet()) {
            AnalysisRunDO latest = analysisRunMapper.selectLatestBySymbol(symbol);
            if (latest != null && latest.getAnalysisId() != null && !latest.getAnalysisId().isBlank()
                    && "SUCCESS".equalsIgnoreCase(latest.getStatus())) {
                updateAsset(symbol, LocalRealAssetReadinessState.READY, null, "REAL_DATA_AVAILABLE");
            } else {
                String reason = latest == null || latest.getErrorCode() == null || latest.getErrorCode().isBlank()
                        ? "ANALYSIS_NOT_COMPLETED" : latest.getErrorCode();
                updateAsset(symbol, LocalRealAssetReadinessState.DEGRADED, null, reason);
            }
        }
        long tracked = assets.size();
        long ready = readyAssetCount();
        if (tracked > 0 && ready == tracked) {
            transition(LocalRealReadinessState.DASHBOARD_READY, "REAL_DATA_AVAILABLE");
        } else if (ready > 0) {
            transition(LocalRealReadinessState.DASHBOARD_PARTIAL, "PARTIAL_REAL_DATA_AVAILABLE");
        } else {
            transition(LocalRealReadinessState.DEGRADED, "ANALYSIS_INCOMPLETE");
        }
    }

    public long readyAssetCount() {
        return assets.values().stream()
                .filter(item -> item.state() == LocalRealAssetReadinessState.READY)
                .count();
    }
}
