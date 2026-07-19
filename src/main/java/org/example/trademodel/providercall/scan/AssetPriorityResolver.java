package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssetPriorityResolver {
    private static final String SCAN_PROVIDER = "BINANCE";
    private final ProviderSymbolMappingRegistry mappingRegistry;

    public AssetPriorityResolver(ProviderSymbolMappingRegistry mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
    }

    public List<PrioritizedAsset> resolve(
            Collection<CanonicalInstrumentId> watchlist,
            Collection<PositionScanAsset> positions,
            Collection<CanonicalInstrumentId> candidates,
            Collection<CanonicalInstrumentId> discovery) {
        Map<CanonicalInstrumentId, PrioritizedAsset> result = new LinkedHashMap<>();
        addInstruments(result, discovery, AssetPriority.P3_DISCOVERY);
        addInstruments(result, watchlist, AssetPriority.P1_WATCHLIST);
        addInstruments(result, candidates, AssetPriority.P2_CANDIDATE);
        if (positions != null) {
            positions.stream().filter(position -> position != null && position.active())
                    .forEach(position -> add(result, position.canonicalInstrumentId(),
                            position.providerSymbol(), AssetPriority.P0_POSITION));
        }
        List<PrioritizedAsset> out = new ArrayList<>(result.values());
        out.sort(Comparator.comparingInt((PrioritizedAsset asset) -> asset.priority().rank())
                .thenComparing(asset -> asset.canonicalInstrumentId().canonical()));
        return List.copyOf(out);
    }

    private void addInstruments(Map<CanonicalInstrumentId, PrioritizedAsset> result,
                                Collection<CanonicalInstrumentId> instruments,
                                AssetPriority priority) {
        if (instruments == null) return;
        for (CanonicalInstrumentId instrument : instruments) {
            if (instrument == null) continue;
            String providerSymbol = mappingRegistry.resolve(SCAN_PROVIDER, instrument).providerSymbol();
            add(result, instrument, providerSymbol, priority);
        }
    }

    private static void add(Map<CanonicalInstrumentId, PrioritizedAsset> result,
                            CanonicalInstrumentId instrument,
                            String providerSymbol,
                            AssetPriority priority) {
        if (instrument == null || providerSymbol == null || providerSymbol.isBlank()) return;
        result.compute(instrument, (ignored, previous) -> previous == null
                || priority.rank() < previous.priority().rank()
                ? new PrioritizedAsset(instrument, providerSymbol, priority) : previous);
    }
}
