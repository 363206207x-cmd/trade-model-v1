package org.example.trademodel.positionmonitorlog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PositionMonitorLogDTO {
    private Long logId;
    private Long positionId;
    private String analysisId;
    private String executionPlanId;
    private boolean sourceVerified;
    private String sourceStatus;
    private String sourceStatusLabel;
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
    private LocalDateTime createdAt;
    private boolean reviewOnly = true;
    private boolean manualReviewOnly = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoClose = true;
    private boolean notAutoReverse = true;
    private boolean notOrderExecution = true;
    private boolean notAutoTrading = true;
    private boolean notPositionMutation = true;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getExecutionPlanId() { return executionPlanId; }
    public void setExecutionPlanId(String executionPlanId) { this.executionPlanId = executionPlanId; }
    public boolean isSourceVerified() { return sourceVerified; }
    public void setSourceVerified(boolean sourceVerified) { this.sourceVerified = sourceVerified; }
    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }
    public String getSourceStatusLabel() { return sourceStatusLabel; }
    public void setSourceStatusLabel(String sourceStatusLabel) { this.sourceStatusLabel = sourceStatusLabel; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isReviewOnly() { return reviewOnly; }
    public void setReviewOnly(boolean reviewOnly) { this.reviewOnly = reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public void setManualReviewOnly(boolean manualReviewOnly) { this.manualReviewOnly = manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(boolean notTradeInstruction) { this.notTradeInstruction = notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public void setNotExecutable(boolean notExecutable) { this.notExecutable = notExecutable; }
    public boolean isNotAutoClose() { return notAutoClose; }
    public void setNotAutoClose(boolean notAutoClose) { this.notAutoClose = notAutoClose; }
    public boolean isNotAutoReverse() { return notAutoReverse; }
    public void setNotAutoReverse(boolean notAutoReverse) { this.notAutoReverse = notAutoReverse; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public void setNotOrderExecution(boolean notOrderExecution) { this.notOrderExecution = notOrderExecution; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public void setNotAutoTrading(boolean notAutoTrading) { this.notAutoTrading = notAutoTrading; }
    public boolean isNotPositionMutation() { return notPositionMutation; }
    public void setNotPositionMutation(boolean notPositionMutation) { this.notPositionMutation = notPositionMutation; }
}
