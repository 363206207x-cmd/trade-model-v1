package org.example.trademodel.analysisrun;

public interface AnalysisIdempotencyGuard {
    AnalysisIdempotencyClaim claim(AnalysisRunClaimRequest request);
    void markFailed(String analysisId, String errorCode, String errorMessage);
}
