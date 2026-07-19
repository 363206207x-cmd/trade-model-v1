package org.example.trademodel.providercall.scan;

import java.util.List;

public interface ProviderScanPlanService {
    List<ScanPlanItem> currentPlan();

    List<ScanPlanItem> planForExecution(String scanCycleTraceId);
}
