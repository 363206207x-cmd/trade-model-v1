package org.example.trademodel.analysisrun;

public interface AnalysisRunOrchestrator {
    AnalysisRunResult run(AnalysisRunCommand command);

    default AnalysisRunResult submit(AnalysisRunCommand command) {
        return run(command);
    }
}
