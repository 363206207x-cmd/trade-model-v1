package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.ProviderDatasetType;

public interface ProviderDatasetRefreshPort {
    void refresh(ScanPlanItem item, ProviderDatasetType datasetType);
}
