package org.example.trademodel.positionmonitorlog;

import java.math.BigDecimal;

public class RecordPositionMonitorLogCommand {
    private Long positionId;
    private String analysisId;
    private String executionPlanId;
    private BigDecimal currentPrice;
    private String logicStatus;
    private String riskLevel;
    private String suggestedAction;
    private String reason;
    private String evidenceSnapshot;
    private String scoreSnapshot;
    private String decisionSnapshot;
    private String riskSnapshot;
    private String traceId;

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getExecutionPlanId() { return executionPlanId; }
    public void setExecutionPlanId(String executionPlanId) { this.executionPlanId = executionPlanId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public String getLogicStatus() { return logicStatus; }
    public void setLogicStatus(String logicStatus) { this.logicStatus = logicStatus; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceSnapshot() { return evidenceSnapshot; }
    public void setEvidenceSnapshot(String evidenceSnapshot) { this.evidenceSnapshot = evidenceSnapshot; }
    public String getScoreSnapshot() { return scoreSnapshot; }
    public void setScoreSnapshot(String scoreSnapshot) { this.scoreSnapshot = scoreSnapshot; }
    public String getDecisionSnapshot() { return decisionSnapshot; }
    public void setDecisionSnapshot(String decisionSnapshot) { this.decisionSnapshot = decisionSnapshot; }
    public String getRiskSnapshot() { return riskSnapshot; }
    public void setRiskSnapshot(String riskSnapshot) { this.riskSnapshot = riskSnapshot; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
