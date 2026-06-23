package org.example.trademodel.analysistrace;

public interface AnalysisTraceService {
    AnalysisTraceSnapshot byAnalysisId(String analysisId);
    AnalysisTraceSnapshot byTraceId(String traceId);
}
