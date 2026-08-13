package org.example.trademodel.ai;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AiDecisionChainResult {
    private AiProviderName provider;
    private AiDecisionChainRole role;
    private AiProviderCallStatus callStatus;
    private String payloadJson;
    private String auditOutput;
    private String providerRequestId;
    private Long latencyMs;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private BigDecimal calculatedCostUsd = BigDecimal.ZERO;
    private BigDecimal reservedCostUsd = BigDecimal.ZERO;
    private boolean fallback;
    private String fallbackReason;
    private String errorCode;
    private String selectedModel;
    private boolean cacheHit;
    private String analysisId;
    private String traceId;
    private AiRoleState roleState;
    private AiRoleDataState dataState;
    private OffsetDateTime generatedAt;

    public static AiDecisionChainResult failed(AiProviderName provider, AiDecisionChainRole role,
                                               AiProviderCallStatus status, String reason) {
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(status == null ? AiProviderCallStatus.FAILED : status);
        result.setFallback(true);
        result.setFallbackReason(reason);
        result.setErrorCode(reason);
        return result;
    }

    public boolean successful() { return callStatus == AiProviderCallStatus.SUCCESS && !fallback; }
    public AiProviderName getProvider() { return provider; }
    public void setProvider(AiProviderName provider) { this.provider = provider; }
    public AiDecisionChainRole getRole() { return role; }
    public void setRole(AiDecisionChainRole role) { this.role = role; }
    public AiProviderCallStatus getCallStatus() { return callStatus; }
    public void setCallStatus(AiProviderCallStatus callStatus) { this.callStatus = callStatus; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getAuditOutput() { return auditOutput; }
    public void setAuditOutput(String auditOutput) { this.auditOutput = auditOutput; }
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
    public void setCalculatedCostUsd(BigDecimal calculatedCostUsd) {
        this.calculatedCostUsd = calculatedCostUsd == null ? BigDecimal.ZERO : calculatedCostUsd;
    }
    public BigDecimal getReservedCostUsd() { return reservedCostUsd; }
    public void setReservedCostUsd(BigDecimal reservedCostUsd) {
        this.reservedCostUsd = reservedCostUsd == null ? BigDecimal.ZERO : reservedCostUsd;
    }
    public boolean isFallback() { return fallback; }
    public void setFallback(boolean fallback) { this.fallback = fallback; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getSelectedModel() { return selectedModel; }
    public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }
    public boolean isCacheHit() { return cacheHit; }
    public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public AiRoleState getRoleState() { return roleState; }
    public void setRoleState(AiRoleState roleState) { this.roleState = roleState; }
    public AiRoleDataState getDataState() { return dataState; }
    public void setDataState(AiRoleDataState dataState) { this.dataState = dataState; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
}
