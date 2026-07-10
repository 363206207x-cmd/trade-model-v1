package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssetPriorityResolver {
    public List<PrioritizedAsset> resolve(
            Collection<String> core,
            Collection<PositionScanAsset> positions,
            Collection<String> candidates,
            Collection<String> pool) {
        Map<String, AssetPriority> result = new LinkedHashMap<>();
        addSymbols(result, pool, AssetPriority.P3_POOL);
        addSymbols(result, candidates, AssetPriority.P2_CANDIDATE);
        addSymbols(result, core, AssetPriority.P1_CORE);
        if (positions != null) {
            positions.stream().filter(position -> position != null && position.active())
                    .map(PositionScanAsset::symbol)
                    .forEach(symbol -> add(result, symbol, AssetPriority.P0_POSITION));
        }
        List<PrioritizedAsset> out = new ArrayList<>();
        result.forEach((symbol, priority) -> out.add(new PrioritizedAsset(symbol, priority)));
        out.sort(Comparator.comparingInt((PrioritizedAsset asset) -> asset.priority().rank())
                .thenComparing(PrioritizedAsset::symbol));
        return out;
    }

    private static void addSymbols(Map<String, AssetPriority> result, Collection<String> symbols, AssetPriority priority) {
        if (symbols != null) symbols.forEach(symbol -> add(result, symbol, priority));
    }

    private static void add(Map<String, AssetPriority> result, String raw, AssetPriority priority) {
        if (raw == null || raw.isBlank()) return;
        String symbol = raw.trim().toUpperCase(Locale.ROOT);
        result.merge(symbol, priority, AssetPriority::highest);
    }
}
