package org.example.trademodel.analysisrun;

import java.util.UUID;

public final class AnalysisRunIds {
    private AnalysisRunIds() {
    }

    public static String analysisId() {
        return "ana-" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String traceId() {
        return "trace-" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String leaseOwner() {
        return "lease-" + UUID.randomUUID().toString().replace("-", "");
    }
}
