package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AiCallLogDO {

    private String callId;
    private String analysisId;
    private String traceId;
    private String requestId;
    private String providerName;
    private String modelName;
    private String aiRole;
    private String callStatus;
    private String providerRequestId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long latencyMs;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private BigDecimal reservedCostUsd;
    private BigDecimal calculatedCostUsd;
    private String costCurrency = "USD";
    private String costCalculationMethod = "TOKEN_RATE_ESTIMATE";
    private Boolean fallbackFlag = false;
    private String fallbackReason;
    private Boolean rateLimited = false;
    private Boolean budgetBlocked = false;
    private Boolean timeoutFlag = false;
    private String errorCode;
    private String errorMessage;
    private String requestHash;
    private String requestSummary;
    private String responseSummary;
    private String ruleVersion;
    private Boolean reviewOnly = true;
    private Boolean manualReviewOnly = true;
    private Boolean notTradeInstruction = true;
    private Boolean notExecutable = true;
    private Boolean notAutoTrading = true;
    private Boolean notOrderExecution = true;
    private Boolean notUserPositionCreation = true;
    private Boolean notPositionMutation = true;
    private Boolean notStateMachineOverride = true;
    private Boolean notExecutionPlanCreation = true;
    private Boolean ruleDirectionPreserved = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AiCallLogDO() {
    }

    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
    public String getId() { return callId; }
    public void setId(String id) { this.callId = id; }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getAiRole() {
        return aiRole;
    }

    public void setAiRole(String aiRole) {
        this.aiRole = aiRole;
    }

    public String getCallStatus() { return callStatus; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }
    public String getProviderRequestId() { return providerRequestId; }
    public void setProviderRequestId(String providerRequestId) { this.providerRequestId = providerRequestId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public Long getInputTokens() { return inputTokens; }
    public void setInputTokens(Long inputTokens) { this.inputTokens = inputTokens; }
    public Long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Long outputTokens) { this.outputTokens = outputTokens; }
    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    public BigDecimal getReservedCostUsd() { return reservedCostUsd; }
    public void setReservedCostUsd(BigDecimal reservedCostUsd) { this.reservedCostUsd = reservedCostUsd; }
    public BigDecimal getCalculatedCostUsd() { return calculatedCostUsd; }
    public void setCalculatedCostUsd(BigDecimal calculatedCostUsd) { this.calculatedCostUsd = calculatedCostUsd; }
    public String getCostCurrency() { return costCurrency; }
    public void setCostCurrency(String costCurrency) { this.costCurrency = costCurrency; }
    public String getCostCalculationMethod() { return costCalculationMethod; }
    public void setCostCalculationMethod(String costCalculationMethod) { this.costCalculationMethod = costCalculationMethod; }
    public Boolean getFallbackFlag() { return fallbackFlag; }
    public void setFallbackFlag(Boolean fallbackFlag) { this.fallbackFlag = fallbackFlag; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public Boolean getRateLimited() { return rateLimited; }
    public void setRateLimited(Boolean rateLimited) { this.rateLimited = rateLimited; }
    public Boolean getBudgetBlocked() { return budgetBlocked; }
    public void setBudgetBlocked(Boolean budgetBlocked) { this.budgetBlocked = budgetBlocked; }
    public Boolean getTimeoutFlag() { return timeoutFlag; }
    public void setTimeoutFlag(Boolean timeoutFlag) { this.timeoutFlag = timeoutFlag; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }

    public String getRequestSummary() {
        return requestSummary;
    }

    public void setRequestSummary(String requestSummary) {
        this.requestSummary = requestSummary;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public void setResponseSummary(String responseSummary) {
        this.responseSummary = responseSummary;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public Boolean getReviewOnly() { return reviewOnly; }
    public void setReviewOnly(Boolean reviewOnly) { this.reviewOnly = reviewOnly; }
    public Boolean getManualReviewOnly() { return manualReviewOnly; }
    public void setManualReviewOnly(Boolean manualReviewOnly) { this.manualReviewOnly = manualReviewOnly; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(Boolean notTradeInstruction) { this.notTradeInstruction = notTradeInstruction; }
    public Boolean getNotExecutable() { return notExecutable; }
    public void setNotExecutable(Boolean notExecutable) { this.notExecutable = notExecutable; }
    public Boolean getNotAutoTrading() { return notAutoTrading; }
    public void setNotAutoTrading(Boolean notAutoTrading) { this.notAutoTrading = notAutoTrading; }
    public Boolean getNotOrderExecution() { return notOrderExecution; }
    public void setNotOrderExecution(Boolean notOrderExecution) { this.notOrderExecution = notOrderExecution; }
    public Boolean getNotUserPositionCreation() { return notUserPositionCreation; }
    public void setNotUserPositionCreation(Boolean notUserPositionCreation) { this.notUserPositionCreation = notUserPositionCreation; }
    public Boolean getNotPositionMutation() { return notPositionMutation; }
    public void setNotPositionMutation(Boolean notPositionMutation) { this.notPositionMutation = notPositionMutation; }
    public Boolean getNotStateMachineOverride() { return notStateMachineOverride; }
    public void setNotStateMachineOverride(Boolean notStateMachineOverride) { this.notStateMachineOverride = notStateMachineOverride; }
    public Boolean getNotExecutionPlanCreation() { return notExecutionPlanCreation; }
    public void setNotExecutionPlanCreation(Boolean notExecutionPlanCreation) { this.notExecutionPlanCreation = notExecutionPlanCreation; }
    public Boolean getRuleDirectionPreserved() { return ruleDirectionPreserved; }
    public void setRuleDirectionPreserved(Boolean ruleDirectionPreserved) { this.ruleDirectionPreserved = ruleDirectionPreserved; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getTokenUsed() { return totalTokens == null ? null : totalTokens.intValue(); }
    public void setTokenUsed(Integer tokenUsed) { this.totalTokens = tokenUsed == null ? null : tokenUsed.longValue(); }
    public String getActualCost() { return calculatedCostUsd == null ? null : calculatedCostUsd.toPlainString(); }
    public void setActualCost(String actualCost) {
        this.calculatedCostUsd = actualCost == null || actualCost.isBlank() ? null : new BigDecimal(actualCost);
    }
    public Integer getDurationMs() { return latencyMs == null ? null : latencyMs.intValue(); }
    public void setDurationMs(Integer durationMs) { this.latencyMs = durationMs == null ? null : durationMs.longValue(); }
    public Integer getCacheHit() { return 0; }
    public void setCacheHit(Integer cacheHit) { }
    public Integer getIsDeleted() { return 0; }
    public void setIsDeleted(Integer isDeleted) { }
    public Integer getVersionNo() { return 1; }
    public void setVersionNo(Integer versionNo) { }
}
