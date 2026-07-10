package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderScanCoordinatorScheduler {
    private final ProviderCallProperties properties;
    private final boolean globalSchedulersEnabled;
    private final ScanUniverseResolver resolver;
    private final ObjectProvider<ProviderScanUniverseSource> sourceProvider;
    private final ObjectProvider<ProviderDatasetRefreshPort> refreshPortProvider;
    private final Set<String> activeKeys = ConcurrentHashMap.newKeySet();

    public ProviderScanCoordinatorScheduler(
            ProviderCallProperties properties,
            @Value("${trade-model.schedulers.enabled:true}") boolean globalSchedulersEnabled,
            ScanUniverseResolver resolver,
            ObjectProvider<ProviderScanUniverseSource> sourceProvider,
            ObjectProvider<ProviderDatasetRefreshPort> refreshPortProvider) {
        this.properties = properties;
        this.globalSchedulersEnabled = globalSchedulersEnabled;
        this.resolver = resolver;
        this.sourceProvider = sourceProvider;
        this.refreshPortProvider = refreshPortProvider;
    }

    @Scheduled(fixedDelayString = "${trade-model.provider-call.scheduler-fixed-delay-ms:1000}")
    public void scanScheduled() {
        scanOnce();
    }

    public int scanOnce() {
        if (!globalSchedulersEnabled || !properties.isEnabled() || !properties.isSchedulerEnabled()) return 0;
        ProviderScanUniverseSource source = sourceProvider.getIfAvailable();
        ProviderDatasetRefreshPort port = refreshPortProvider.getIfAvailable();
        if (source == null || port == null) return 0;
        ScanUniverseInput input = source.currentUniverse();
        if (input == null) return 0;
        int[] count = {0};
        resolver.resolve(input).stream()
                .sorted(Comparator.comparingInt(item -> item.effectivePriority().rank()))
                .forEach(item -> item.dueDatasets().stream()
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .forEach(dataset -> {
                            if (refreshOne(port, item, dataset)) count[0]++;
                        }));
        return count[0];
    }

    private boolean refreshOne(ProviderDatasetRefreshPort port, ScanPlanItem item, ProviderDatasetType dataset) {
        String key = item.symbol() + "|" + dataset;
        if (!activeKeys.add(key)) return false;
        try {
            port.refresh(item, dataset);
            return true;
        } finally {
            activeKeys.remove(key);
        }
    }
}
