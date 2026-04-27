package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推送二次校验日志，与表 tm_push_recheck_log 对齐；持久化经 MyBatis {@code PushRecheckLogMapper}。
 */
public class TmPushRecheckLogDO {

    private Long logId;
    private Long pushId;
    private String dispatchBatchId;
    private String dispatchInstructionId;
    private String triggerSource;
    private Integer retryAttempt;
    private Integer maxAttempts;
    private Integer retryBackoffMinutes;
    private Long replayFromLogId;
    private String executionStatus;
    private String executionErrorCode;
    private String executionErrorMessage;
    private LocalDateTime recheckTime;
    private String recheckStatus;
    private BigDecimal currentPrice;
    private BigDecimal priceDriftRatio;
    private BigDecimal currentSlippageEstimation;
    private Integer currentDataQualityScore;
    private Integer currentConfusedScore;
    private Boolean currentAccountRiskAllowed;
    private String failReasonJson;
    private String traceId;
    private LocalDateTime createTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getPushId() {
        return pushId;
    }

    public void setPushId(Long pushId) {
        this.pushId = pushId;
    }

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

    public Integer getRetryAttempt() {
        return retryAttempt;
    }

    public void setRetryAttempt(Integer retryAttempt) {
        this.retryAttempt = retryAttempt;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Integer getRetryBackoffMinutes() {
        return retryBackoffMinutes;
    }

    public void setRetryBackoffMinutes(Integer retryBackoffMinutes) {
        this.retryBackoffMinutes = retryBackoffMinutes;
    }

    public Long getReplayFromLogId() {
        return replayFromLogId;
    }

    public void setReplayFromLogId(Long replayFromLogId) {
        this.replayFromLogId = replayFromLogId;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getExecutionErrorCode() {
        return executionErrorCode;
    }

    public void setExecutionErrorCode(String executionErrorCode) {
        this.executionErrorCode = executionErrorCode;
    }

    public String getExecutionErrorMessage() {
        return executionErrorMessage;
    }

    public void setExecutionErrorMessage(String executionErrorMessage) {
        this.executionErrorMessage = executionErrorMessage;
    }

    public LocalDateTime getRecheckTime() {
        return recheckTime;
    }

    public void setRecheckTime(LocalDateTime recheckTime) {
        this.recheckTime = recheckTime;
    }

    public String getRecheckStatus() {
        return recheckStatus;
    }

    public void setRecheckStatus(String recheckStatus) {
        this.recheckStatus = recheckStatus;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getPriceDriftRatio() {
        return priceDriftRatio;
    }

    public void setPriceDriftRatio(BigDecimal priceDriftRatio) {
        this.priceDriftRatio = priceDriftRatio;
    }

    public BigDecimal getCurrentSlippageEstimation() {
        return currentSlippageEstimation;
    }

    public void setCurrentSlippageEstimation(BigDecimal currentSlippageEstimation) {
        this.currentSlippageEstimation = currentSlippageEstimation;
    }

    public Integer getCurrentDataQualityScore() {
        return currentDataQualityScore;
    }

    public void setCurrentDataQualityScore(Integer currentDataQualityScore) {
        this.currentDataQualityScore = currentDataQualityScore;
    }

    public Integer getCurrentConfusedScore() {
        return currentConfusedScore;
    }

    public void setCurrentConfusedScore(Integer currentConfusedScore) {
        this.currentConfusedScore = currentConfusedScore;
    }

    public Boolean getCurrentAccountRiskAllowed() {
        return currentAccountRiskAllowed;
    }

    public void setCurrentAccountRiskAllowed(Boolean currentAccountRiskAllowed) {
        this.currentAccountRiskAllowed = currentAccountRiskAllowed;
    }

    public String getFailReasonJson() {
        return failReasonJson;
    }

    public void setFailReasonJson(String failReasonJson) {
        this.failReasonJson = failReasonJson;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
