package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class AsyncTaskDO {
    private String taskId;
    private String ownerType;
    private Long ownerId;
    private String taskType;
    private String state;
    private String stage;
    private String resourceType;
    private String resourceId;
    private String resultResourceId;
    private String idempotencyKey;
    private String traceId;
    private Integer retryCount;
    private Integer maxRetries;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { this.taskId = value; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String value) { this.ownerType = value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId = value; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String value) { this.taskType = value; }
    public String getState() { return state; }
    public void setState(String value) { this.state = value; }
    public String getStage() { return stage; }
    public void setStage(String value) { this.stage = value; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String value) { this.resourceType = value; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String value) { this.resourceId = value; }
    public String getResultResourceId() { return resultResourceId; }
    public void setResultResourceId(String value) { this.resultResourceId = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String value) { this.traceId = value; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer value) { this.retryCount = value; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer value) { this.maxRetries = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { this.errorCode = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { this.errorMessage = value; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { this.startedAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
