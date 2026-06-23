package org.example.trademodel.analysisrun;

public interface AnalysisIdempotencyGuard {
    AnalysisIdempotencyClaim claim(AnalysisRunClaimRequest request);
    void markFailed(AnalysisExecutionContext context, String errorCode, String errorMessage);
}
