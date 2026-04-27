package org.example.trademodel.entity;
import java.time.LocalDateTime;

public class AnalysisRunDO {
    private String analysisId;
    private String symbol;
    private String timeframe;
    private LocalDateTime analysisTime;
    private String ruleVersion;
    private Integer dataQualityScore;
    private String traceId;
    private String status;

    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public Integer getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(Integer dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
