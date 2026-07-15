package org.example.trademodel.ai;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiOrchestratorResult {
    private String analysisId;
    private String traceId;
    private AiOrchestrationMode orchestrationMode = AiOrchestrationMode.RULE_ONLY_FALLBACK;
    private List<AiProviderReviewResult> providerResults = new ArrayList<>();
    private int successfulProviderCount;
    private int failedProviderCount;
    private int fallbackProviderCount;
    private boolean gptConsistentWithRule;
    private boolean geminiConsistentWithRule;
    private boolean grokConsistentWithRule;
    private int aiObjectionCount;
    private int aiSupportCount;
    private int conflictContribution;
    private List<String> reasonCodes = new ArrayList<>();
    private LocalDateTime completedAt;
    private LocalDateTime orchestrationStartedAt;
    private LocalDateTime orchestrationCompletedAt;
    private long orchestrationLatencyMs;
    private int providerSubmittedCount;
    private int providerCompletedCount;
    private int providerTimeoutCount;
    private int providerFailedCount;
    private int providerSuccessCount;
    private boolean globalDeadlineExceeded;
    private boolean partialFallbackUsed;

    private final boolean reviewOnly = true;
    private final boolean manualReviewOnly = true;
    private final boolean notTradeInstruction = true;
    private final boolean notExecutable = true;
    private final boolean notAutoTrading = true;
    private final boolean notOrderExecution = true;
    private final boolean notUserPositionCreation = true;
    private final boolean notPositionMutation = true;
    private final boolean notStateMachineOverride = true;
    private final boolean notExecutionPlanCreation = true;
    private final boolean ruleDirectionPreserved = true;

    public String toSanitizedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("orchestrationMode=").append(orchestrationMode);
        sb.append("; ruleDirectionPreserved=true");
        sb.append("; reviewOnly=true");
        sb.append("; orchestrationLatencyMs=").append(orchestrationLatencyMs);
        sb.append("; providerSubmittedCount=").append(providerSubmittedCount);
        sb.append("; providerCompletedCount=").append(providerCompletedCount);
        sb.append("; providerTimeoutCount=").append(providerTimeoutCount);
        sb.append("; providerFailedCount=").append(providerFailedCount);
        sb.append("; providerSuccessCount=").append(providerSuccessCount);
        sb.append("; globalDeadlineExceeded=").append(globalDeadlineExceeded);
        sb.append("; partialFallbackUsed=").append(partialFallbackUsed);
        sb.append("; providers=");
        for (int i = 0; i < providerResults.size(); i++) {
            AiProviderReviewResult result = providerResults.get(i);
            if (i > 0) {
                sb.append("|");
            }
            sb.append(result.getProvider()).append(":").append(result.getCallStatus());
            if (result.successful()) {
                sb.append(":").append(result.getStance()).append(":").append(result.getConflictLevel());
            } else if (result.getFallbackReason() != null) {
                sb.append(":fallback=").append(safe(result.getFallbackReason(), 80));
            }
        }
        if (!reasonCodes.isEmpty()) {
            sb.append("; reasonCodes=").append(reasonCodes);
        }
        return safe(sb.toString(), 1000);
    }

    private static String safe(String value, int max) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]{8,}", "Bearer ***")
                .replaceAll("sk-[A-Za-z0-9]+", "sk-***")
                .replaceAll("AIza[A-Za-z0-9_\\-]+", "AIza***")
                .replaceAll("xai-[A-Za-z0-9]+", "xai-***");
        return sanitized.length() <= max ? sanitized : sanitized.substring(0, max);
    }

    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public AiOrchestrationMode getOrchestrationMode() { return orchestrationMode; }
    public void setOrchestrationMode(AiOrchestrationMode orchestrationMode) {
        this.orchestrationMode = orchestrationMode == null ? AiOrchestrationMode.RULE_ONLY_FALLBACK : orchestrationMode;
    }
    public List<AiProviderReviewResult> getProviderResults() { return Collections.unmodifiableList(providerResults); }
    public void setProviderResults(List<AiProviderReviewResult> providerResults) {
        this.providerResults = providerResults == null ? new ArrayList<>() : new ArrayList<>(providerResults);
    }
    public int getSuccessfulProviderCount() { return successfulProviderCount; }
    public void setSuccessfulProviderCount(int successfulProviderCount) { this.successfulProviderCount = successfulProviderCount; }
    public int getFailedProviderCount() { return failedProviderCount; }
    public void setFailedProviderCount(int failedProviderCount) { this.failedProviderCount = failedProviderCount; }
    public int getFallbackProviderCount() { return fallbackProviderCount; }
    public void setFallbackProviderCount(int fallbackProviderCount) { this.fallbackProviderCount = fallbackProviderCount; }
    public boolean isGptConsistentWithRule() { return gptConsistentWithRule; }
    public void setGptConsistentWithRule(boolean gptConsistentWithRule) { this.gptConsistentWithRule = gptConsistentWithRule; }
    public boolean isGeminiConsistentWithRule() { return geminiConsistentWithRule; }
    public void setGeminiConsistentWithRule(boolean geminiConsistentWithRule) { this.geminiConsistentWithRule = geminiConsistentWithRule; }
    public boolean isGrokConsistentWithRule() { return grokConsistentWithRule; }
    public void setGrokConsistentWithRule(boolean grokConsistentWithRule) { this.grokConsistentWithRule = grokConsistentWithRule; }
    public int getAiObjectionCount() { return aiObjectionCount; }
    public void setAiObjectionCount(int aiObjectionCount) { this.aiObjectionCount = aiObjectionCount; }
    public int getAiSupportCount() { return aiSupportCount; }
    public void setAiSupportCount(int aiSupportCount) { this.aiSupportCount = aiSupportCount; }
    public int getConflictContribution() { return conflictContribution; }
    public void setConflictContribution(int conflictContribution) { this.conflictContribution = conflictContribution; }
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes);
    }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getOrchestrationStartedAt() { return orchestrationStartedAt; }
    public void setOrchestrationStartedAt(LocalDateTime orchestrationStartedAt) {
        this.orchestrationStartedAt = orchestrationStartedAt;
    }
    public LocalDateTime getOrchestrationCompletedAt() { return orchestrationCompletedAt; }
    public void setOrchestrationCompletedAt(LocalDateTime orchestrationCompletedAt) {
        this.orchestrationCompletedAt = orchestrationCompletedAt;
    }
    public long getOrchestrationLatencyMs() { return orchestrationLatencyMs; }
    public void setOrchestrationLatencyMs(long orchestrationLatencyMs) {
        this.orchestrationLatencyMs = Math.max(0L, orchestrationLatencyMs);
    }
    public int getProviderSubmittedCount() { return providerSubmittedCount; }
    public void setProviderSubmittedCount(int providerSubmittedCount) {
        this.providerSubmittedCount = Math.max(0, providerSubmittedCount);
    }
    public int getProviderCompletedCount() { return providerCompletedCount; }
    public void setProviderCompletedCount(int providerCompletedCount) {
        this.providerCompletedCount = Math.max(0, providerCompletedCount);
    }
    public int getProviderTimeoutCount() { return providerTimeoutCount; }
    public void setProviderTimeoutCount(int providerTimeoutCount) {
        this.providerTimeoutCount = Math.max(0, providerTimeoutCount);
    }
    public int getProviderFailedCount() { return providerFailedCount; }
    public void setProviderFailedCount(int providerFailedCount) {
        this.providerFailedCount = Math.max(0, providerFailedCount);
    }
    public int getProviderSuccessCount() { return providerSuccessCount; }
    public void setProviderSuccessCount(int providerSuccessCount) {
        this.providerSuccessCount = Math.max(0, providerSuccessCount);
    }
    public boolean isGlobalDeadlineExceeded() { return globalDeadlineExceeded; }
    public void setGlobalDeadlineExceeded(boolean globalDeadlineExceeded) {
        this.globalDeadlineExceeded = globalDeadlineExceeded;
    }
    public boolean isPartialFallbackUsed() { return partialFallbackUsed; }
    public void setPartialFallbackUsed(boolean partialFallbackUsed) {
        this.partialFallbackUsed = partialFallbackUsed;
    }
    public boolean isReviewOnly() { return reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public boolean isNotUserPositionCreation() { return notUserPositionCreation; }
    public boolean isNotPositionMutation() { return notPositionMutation; }
    public boolean isNotStateMachineOverride() { return notStateMachineOverride; }
    public boolean isNotExecutionPlanCreation() { return notExecutionPlanCreation; }
    public boolean isRuleDirectionPreserved() { return ruleDirectionPreserved; }
}
