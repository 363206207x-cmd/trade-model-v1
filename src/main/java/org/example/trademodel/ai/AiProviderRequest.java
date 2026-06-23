package org.example.trademodel.ai;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiProviderRequest {
    private String analysisId;
    private String traceId;
    private String symbol;
    private String timeframe;
    private String ruleMarketBias;
    private String ruleConfidence;
    private String ruleRiskLevel;
    private Boolean ruleWorthOpening;
    private Integer dataQualityScore;
    private Integer trendStructureScore;
    private String multiTimeframeState;
    private String externalContextState;
    private String evidenceSummary;
    private String scoreSummary;
    private Map<String, Object> decisionFacts = new LinkedHashMap<>();
    private LocalDateTime requestTime;

    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public String getRuleMarketBias() { return ruleMarketBias; }
    public void setRuleMarketBias(String ruleMarketBias) { this.ruleMarketBias = ruleMarketBias; }
    public String getRuleConfidence() { return ruleConfidence; }
    public void setRuleConfidence(String ruleConfidence) { this.ruleConfidence = ruleConfidence; }
    public String getRuleRiskLevel() { return ruleRiskLevel; }
    public void setRuleRiskLevel(String ruleRiskLevel) { this.ruleRiskLevel = ruleRiskLevel; }
    public Boolean getRuleWorthOpening() { return ruleWorthOpening; }
    public void setRuleWorthOpening(Boolean ruleWorthOpening) { this.ruleWorthOpening = ruleWorthOpening; }
    public Integer getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(Integer dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    public Integer getTrendStructureScore() { return trendStructureScore; }
    public void setTrendStructureScore(Integer trendStructureScore) { this.trendStructureScore = trendStructureScore; }
    public String getMultiTimeframeState() { return multiTimeframeState; }
    public void setMultiTimeframeState(String multiTimeframeState) { this.multiTimeframeState = multiTimeframeState; }
    public String getExternalContextState() { return externalContextState; }
    public void setExternalContextState(String externalContextState) { this.externalContextState = externalContextState; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public void setEvidenceSummary(String evidenceSummary) { this.evidenceSummary = evidenceSummary; }
    public String getScoreSummary() { return scoreSummary; }
    public void setScoreSummary(String scoreSummary) { this.scoreSummary = scoreSummary; }
    public Map<String, Object> getDecisionFacts() { return decisionFacts; }
    public void setDecisionFacts(Map<String, Object> decisionFacts) {
        this.decisionFacts = decisionFacts == null ? new LinkedHashMap<>() : new LinkedHashMap<>(decisionFacts);
    }
    public LocalDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }
}
