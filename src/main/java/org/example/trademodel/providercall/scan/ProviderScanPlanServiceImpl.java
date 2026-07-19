package org.example.trademodel.providercall.scan;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderScanPlanServiceImpl implements ProviderScanPlanService {
    private final ProviderScanUniverseSource source;
    private final ScanUniverseResolver resolver;

    public ProviderScanPlanServiceImpl(ProviderScanUniverseSource source,
                                       ScanUniverseResolver resolver) {
        this.source = source;
        this.resolver = resolver;
    }

    @Override
    public List<ScanPlanItem> currentPlan() {
        ScanUniverseInput input = source.currentUniverse();
        return input == null ? List.of() : resolver.resolve(input);
    }

    @Override
    public List<ScanPlanItem> planForExecution(String scanCycleTraceId) {
        ScanUniverseInput input = source.evaluateUniverseForExecution(scanCycleTraceId);
        return input == null ? List.of() : resolver.resolve(input);
    }
}
