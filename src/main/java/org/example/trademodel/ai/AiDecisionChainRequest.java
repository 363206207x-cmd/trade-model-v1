package org.example.trademodel.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiDecisionChainRequest {
    private AiDecisionChainRole role;
    private String analysisId;
    private String traceId;
    private String requestId;
    private String opportunityId;
    private String candidateId;
    private String ruleVersion;
    private String symbol;
    private String timeframe;
    private Map<String, Object> input = new LinkedHashMap<>();
    private boolean inputContractSatisfied = true;
    private List<String> inputContractFailures = List.of();

    public AiDecisionChainRole getRole() { return role; }
    public void setRole(AiDecisionChainRole role) { this.role = role; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public Map<String, Object> getInput() { return Map.copyOf(input); }
    public void setInput(Map<String, Object> input) {
        this.input = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
    }
    public boolean isInputContractSatisfied() { return inputContractSatisfied; }
    public void setInputContractSatisfied(boolean value) { this.inputContractSatisfied = value; }
    public List<String> getInputContractFailures() { return List.copyOf(inputContractFailures); }
    public void setInputContractFailures(List<String> value) {
        this.inputContractFailures = value == null ? List.of() : List.copyOf(value);
    }
}
