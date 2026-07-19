package org.example.trademodel.providercall.scan;

public interface ProviderScanUniverseSource {
    ScanUniverseInput currentUniverse();

    ScanUniverseInput evaluateUniverseForExecution(String scanCycleTraceId);
}
