package org.example.trademodel.ai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;

public class AiProviderReviewResult {
    private AiProviderName provider;
    private AiProviderRole role;
    private AiProviderCallStatus callStatus;
    private AiReviewStance stance = AiReviewStance.ABSTAIN;
    private AiReviewConflictLevel conflictLevel = AiReviewConflictLevel.NONE;
    private List<String> reasonCodes = new ArrayList<>();
    private String summary = "";
    private String providerRequestId;
    private Long latencyMs;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private BigDecimal calculatedCostUsd;
    private BigDecimal reservedCostUsd;
    private boolean fallback;
    private String fallbackReason;
    private String errorCode;
    private boolean timeout;
    private boolean budgetBlocked;
    private boolean rateLimited;
    private String originalModel;
    private String selectedModel;
    private Integer fallbackLevel;
    private String modelStrategy;
    private LocalDateTime modelRoutingTimestamp;
    private String modelRoutingTraceId;
    private AiProviderSchemaDiagnostic schemaDiagnostic;
    private GeminiResponseShapeDiagnostic geminiResponseShapeDiagnostic;
    private GeminiInteractionDiagnostic geminiInteractionDiagnostic;

    public static AiProviderReviewResult skipped(AiProviderName provider, AiProviderRole role,
                                                 AiProviderCallStatus status, String reason) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(status);
        result.setFallback(true);
        result.setFallbackReason(reason);
        result.setReasonCodes(List.of(reason));
        result.setSummary(reason);
        result.setBudgetBlocked(status == AiProviderCallStatus.BUDGET_BLOCKED);
        result.setRateLimited(status == AiProviderCallStatus.RATE_LIMITED);
        result.setTimeout(status == AiProviderCallStatus.TIMEOUT);
        result.setErrorCode(status.name());
        return result;
    }

    public boolean successful() {
        return callStatus == AiProviderCallStatus.SUCCESS;
    }

    public boolean challengesRule() {
        return successful() && stance == AiReviewStance.CHALLENGE;
    }

    public boolean supportsRule() {
        return successful() && stance == AiReviewStance.SUPPORT;
    }

    public AiProviderName getProvider() { return provider; }
    public void setProvider(AiProviderName provider) { this.provider = provider; }
    public AiProviderRole getRole() { return role; }
    public void setRole(AiProviderRole role) { this.role = role; }
    public AiProviderCallStatus getCallStatus() { return callStatus; }
    public void setCallStatus(AiProviderCallStatus callStatus) { this.callStatus = callStatus; }
    public AiReviewStance getStance() { return stance; }
    public void setStance(AiReviewStance stance) { this.stance = stance == null ? AiReviewStance.ABSTAIN : stance; }
    public AiReviewConflictLevel getConflictLevel() { return conflictLevel; }
    public void setConflictLevel(AiReviewConflictLevel conflictLevel) {
        this.conflictLevel = conflictLevel == null ? AiReviewConflictLevel.NONE : conflictLevel;
    }
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes);
    }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getProviderRequestId() { return providerRequestId; }
    public void setProviderRequestId(String providerRequestId) { this.providerRequestId = providerRequestId; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public Long getInputTokens() { return inputTokens; }
    public void setInputTokens(Long inputTokens) { this.inputTokens = inputTokens; }
    public Long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Long outputTokens) { this.outputTokens = outputTokens; }
    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    public BigDecimal getCalculatedCostUsd() { return calculatedCostUsd; }
    public void setCalculatedCostUsd(BigDecimal calculatedCostUsd) { this.calculatedCostUsd = calculatedCostUsd; }
    public BigDecimal getReservedCostUsd() { return reservedCostUsd; }
    public void setReservedCostUsd(BigDecimal reservedCostUsd) { this.reservedCostUsd = reservedCostUsd; }
    public boolean isFallback() { return fallback; }
    public void setFallback(boolean fallback) { this.fallback = fallback; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public boolean isTimeout() { return timeout; }
    public void setTimeout(boolean timeout) { this.timeout = timeout; }
    public boolean isBudgetBlocked() { return budgetBlocked; }
    public void setBudgetBlocked(boolean budgetBlocked) { this.budgetBlocked = budgetBlocked; }
    public boolean isRateLimited() { return rateLimited; }
    public void setRateLimited(boolean rateLimited) { this.rateLimited = rateLimited; }
    public String getOriginalModel() { return originalModel; }
    public void setOriginalModel(String originalModel) { this.originalModel = originalModel; }
    public String getSelectedModel() { return selectedModel; }
    public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }
    public Integer getFallbackLevel() { return fallbackLevel; }
    public void setFallbackLevel(Integer fallbackLevel) { this.fallbackLevel = fallbackLevel; }
    public String getModelStrategy() { return modelStrategy; }
    public void setModelStrategy(String modelStrategy) { this.modelStrategy = modelStrategy; }
    public LocalDateTime getModelRoutingTimestamp() { return modelRoutingTimestamp; }
    public void setModelRoutingTimestamp(LocalDateTime modelRoutingTimestamp) {
        this.modelRoutingTimestamp = modelRoutingTimestamp;
    }
    public String getModelRoutingTraceId() { return modelRoutingTraceId; }
    public void setModelRoutingTraceId(String modelRoutingTraceId) { this.modelRoutingTraceId = modelRoutingTraceId; }
    public AiProviderSchemaDiagnostic getSchemaDiagnostic() { return schemaDiagnostic; }
    public void setSchemaDiagnostic(AiProviderSchemaDiagnostic schemaDiagnostic) {
        this.schemaDiagnostic = schemaDiagnostic;
    }
    public GeminiResponseShapeDiagnostic getGeminiResponseShapeDiagnostic() {
        return geminiResponseShapeDiagnostic;
    }
    public void setGeminiResponseShapeDiagnostic(
            GeminiResponseShapeDiagnostic geminiResponseShapeDiagnostic) {
        this.geminiResponseShapeDiagnostic = geminiResponseShapeDiagnostic;
    }
    public GeminiInteractionDiagnostic getGeminiInteractionDiagnostic() {
        return geminiInteractionDiagnostic;
    }
    public void setGeminiInteractionDiagnostic(
            GeminiInteractionDiagnostic geminiInteractionDiagnostic) {
        this.geminiInteractionDiagnostic = geminiInteractionDiagnostic;
    }
}
