package org.example.trademodel.analysistrace;

public interface AnalysisTraceService {
    AnalysisTraceSnapshot byAnalysisId(String analysisId);
    AnalysisTraceSnapshot byTraceId(String traceId);
    AnalysisTraceSnapshot byRequestId(String requestId);
    AnalysisTraceSnapshot byAnalysisIdForUser(String analysisId, Long userId);
    AnalysisTraceSnapshot byTraceIdForUser(String traceId, Long userId);
    AnalysisTraceSnapshot byRequestIdForUser(String requestId, Long userId);
}
