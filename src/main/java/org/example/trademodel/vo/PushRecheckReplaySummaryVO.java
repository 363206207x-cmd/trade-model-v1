package org.example.trademodel.vo;

import java.time.LocalDateTime;

public class PushRecheckReplaySummaryVO {

    private String dispatchBatchId;
    private String dispatchInstructionId;
    private String triggerSource;
    private Integer totalCount;
    private Integer successCount;
    private Integer blockingCount;
    private Integer waitingCount;
    private Integer expiredCount;
    private Integer replayCount;
    private String latestExecutionStatus;
    private LocalDateTime latestExecutionTime;
    private Boolean hasError;
    private String latestErrorCode;

    public String getDispatchBatchId() {
        return dispatchBatchId;
    }

    public void setDispatchBatchId(String dispatchBatchId) {
        this.dispatchBatchId = dispatchBatchId;
    }

    public String getDispatchInstructionId() {
        return dispatchInstructionId;
    }

    public void setDispatchInstructionId(String dispatchInstructionId) {
        this.dispatchInstructionId = dispatchInstructionId;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getBlockingCount() {
        return blockingCount;
    }

    public void setBlockingCount(Integer blockingCount) {
        this.blockingCount = blockingCount;
    }

    public Integer getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(Integer waitingCount) {
        this.waitingCount = waitingCount;
    }

    public Integer getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(Integer expiredCount) {
        this.expiredCount = expiredCount;
    }

    public Integer getReplayCount() {
        return replayCount;
    }

    public void setReplayCount(Integer replayCount) {
        this.replayCount = replayCount;
    }

    public String getLatestExecutionStatus() {
        return latestExecutionStatus;
    }

    public void setLatestExecutionStatus(String latestExecutionStatus) {
        this.latestExecutionStatus = latestExecutionStatus;
    }

    public LocalDateTime getLatestExecutionTime() {
        return latestExecutionTime;
    }

    public void setLatestExecutionTime(LocalDateTime latestExecutionTime) {
        this.latestExecutionTime = latestExecutionTime;
    }

    public Boolean getHasError() {
        return hasError;
    }

    public void setHasError(Boolean hasError) {
        this.hasError = hasError;
    }

    public String getLatestErrorCode() {
        return latestErrorCode;
    }

    public void setLatestErrorCode(String latestErrorCode) {
        this.latestErrorCode = latestErrorCode;
    }
}
