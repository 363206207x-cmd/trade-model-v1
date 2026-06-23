package org.example.trademodel.analysisrun;

public interface AnalysisRunOrchestrator {
    AnalysisRunResult run(AnalysisRunCommand command);
}
