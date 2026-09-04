package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PositionMonitorLogDO {
    private Long logId;
    private String monitorRunKey;
    private Long positionId;
    private String analysisId;
    private String executionPlanId;
    private BigDecimal currentPrice;
    private String markPriceSource;
    private String logicStatus;
    private String entryLogicStatus;
    private String monitorConclusion;
    private String reversalStatus;
    private String riskChangeReason;
    private String riskLevel;
    private String riskTrend;
    private String suggestedAction;
    private String monitorSourceStatus;
    private LocalDateTime observedAt;
    private LocalDateTime freshUntil;
    private String reason;
    private String evidenceSnapshot;
    private String scoreSnapshot;
    private String decisionSnapshot;
    private String riskSnapshot;
    private String traceId;
    private LocalDateTime createdAt;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getMonitorRunKey() { return monitorRunKey; }
    public void setMonitorRunKey(String monitorRunKey) { this.monitorRunKey = monitorRunKey; }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getExecutionPlanId() {
        return executionPlanId;
    }

    public void setExecutionPlanId(String executionPlanId) {
        this.executionPlanId = executionPlanId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getMarkPriceSource() { return markPriceSource; }
    public void setMarkPriceSource(String markPriceSource) { this.markPriceSource = markPriceSource; }

    public String getLogicStatus() {
        return logicStatus;
    }

    public void setLogicStatus(String logicStatus) {
        this.logicStatus = logicStatus;
    }

    public String getEntryLogicStatus() { return entryLogicStatus; }
    public void setEntryLogicStatus(String entryLogicStatus) { this.entryLogicStatus = entryLogicStatus; }
    public String getMonitorConclusion() { return monitorConclusion; }
    public void setMonitorConclusion(String monitorConclusion) { this.monitorConclusion = monitorConclusion; }
    public String getReversalStatus() { return reversalStatus; }
    public void setReversalStatus(String reversalStatus) { this.reversalStatus = reversalStatus; }
    public String getRiskChangeReason() { return riskChangeReason; }
    public void setRiskChangeReason(String riskChangeReason) { this.riskChangeReason = riskChangeReason; }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskTrend() { return riskTrend; }
    public void setRiskTrend(String riskTrend) { this.riskTrend = riskTrend; }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public String getMonitorSourceStatus() { return monitorSourceStatus; }
    public void setMonitorSourceStatus(String monitorSourceStatus) { this.monitorSourceStatus = monitorSourceStatus; }
    public LocalDateTime getObservedAt() { return observedAt; }
    public void setObservedAt(LocalDateTime observedAt) { this.observedAt = observedAt; }
    public LocalDateTime getFreshUntil() { return freshUntil; }
    public void setFreshUntil(LocalDateTime freshUntil) { this.freshUntil = freshUntil; }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidenceSnapshot() {
        return evidenceSnapshot;
    }

    public void setEvidenceSnapshot(String evidenceSnapshot) {
        this.evidenceSnapshot = evidenceSnapshot;
    }

    public String getScoreSnapshot() {
        return scoreSnapshot;
    }

    public void setScoreSnapshot(String scoreSnapshot) {
        this.scoreSnapshot = scoreSnapshot;
    }

    public String getDecisionSnapshot() {
        return decisionSnapshot;
    }

    public void setDecisionSnapshot(String decisionSnapshot) {
        this.decisionSnapshot = decisionSnapshot;
    }

    public String getRiskSnapshot() {
        return riskSnapshot;
    }

    public void setRiskSnapshot(String riskSnapshot) {
        this.riskSnapshot = riskSnapshot;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
