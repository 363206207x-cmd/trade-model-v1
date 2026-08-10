package org.example.trademodel.ai;

import java.util.LinkedHashMap;
import java.util.Map;

public class AiDecisionChainRequest {
    private AiDecisionChainRole role;
    private String analysisId;
    private String traceId;
    private String candidateId;
    private String symbol;
    private String timeframe;
    private Map<String, Object> input = new LinkedHashMap<>();

    public AiDecisionChainRole getRole() { return role; }
    public void setRole(AiDecisionChainRole role) { this.role = role; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public Map<String, Object> getInput() { return Map.copyOf(input); }
    public void setInput(Map<String, Object> input) {
        this.input = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
    }
}
