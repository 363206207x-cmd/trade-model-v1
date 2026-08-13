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
    private String idempotencyKey;
    private String requestId;
    private String triggerType;
    private String triggerReference;
    private String parentAnalysisId;
    private String parentTraceId;
    private String inputSnapshotJson;
    private String inputSnapshotHash;
    private Integer attemptCount;
    private String leaseOwner;
    private LocalDateTime leaseExpiresAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer versionNo;
    private String ownerType;
    private Long ownerId;
    private Long assetId;
    private Boolean preview = false;

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
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getTriggerReference() { return triggerReference; }
    public void setTriggerReference(String triggerReference) { this.triggerReference = triggerReference; }
    public String getParentAnalysisId() { return parentAnalysisId; }
    public void setParentAnalysisId(String parentAnalysisId) { this.parentAnalysisId = parentAnalysisId; }
    public String getParentTraceId() { return parentTraceId; }
    public void setParentTraceId(String parentTraceId) { this.parentTraceId = parentTraceId; }
    public String getInputSnapshotJson() { return inputSnapshotJson; }
    public void setInputSnapshotJson(String inputSnapshotJson) { this.inputSnapshotJson = inputSnapshotJson; }
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    public void setInputSnapshotHash(String inputSnapshotHash) { this.inputSnapshotHash = inputSnapshotHash; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public String getLeaseOwner() { return leaseOwner; }
    public void setLeaseOwner(String leaseOwner) { this.leaseOwner = leaseOwner; }
    public LocalDateTime getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(LocalDateTime leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public Boolean getPreview() { return preview; }
    public void setPreview(Boolean preview) { this.preview = preview; }
}
