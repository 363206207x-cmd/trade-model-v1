package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class PlanRevalidationRecordDO {
    private String recordId;
    private String planId;
    private String analysisId;
    private String triggerType;
    private String state;
    private Integer sourcePlanVersion;
    private Integer resultPlanVersion;
    private String resultPlanId;
    private String reason;
    private String resultSummary;
    private String traceId;
    private Long requestedByUserId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean notTradeInstruction = true;
    private Boolean notOrderExecution = true;

    public String getRecordId() { return recordId; }
    public void setRecordId(String value) { this.recordId = value; }
    public String getPlanId() { return planId; }
    public void setPlanId(String value) { this.planId = value; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String value) { this.analysisId = value; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String value) { this.triggerType = value; }
    public String getState() { return state; }
    public void setState(String value) { this.state = value; }
    public Integer getSourcePlanVersion() { return sourcePlanVersion; }
    public void setSourcePlanVersion(Integer value) { this.sourcePlanVersion = value; }
    public Integer getResultPlanVersion() { return resultPlanVersion; }
    public void setResultPlanVersion(Integer value) { this.resultPlanVersion = value; }
    public String getResultPlanId() { return resultPlanId; }
    public void setResultPlanId(String value) { this.resultPlanId = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String value) { this.resultSummary = value; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String value) { this.traceId = value; }
    public Long getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(Long value) { this.requestedByUserId = value; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { this.startedAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(Boolean value) { this.notTradeInstruction = value; }
    public Boolean getNotOrderExecution() { return notOrderExecution; }
    public void setNotOrderExecution(Boolean value) { this.notOrderExecution = value; }
}
