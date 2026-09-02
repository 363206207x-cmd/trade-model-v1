package org.example.trademodel.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "trade-model.ai")
public class AiOrchestratorProperties {
    private boolean enabled;
    private int requestTimeoutMs = 5000;
    private int overallTimeoutMs = 8000;
    private int maxInputChars = 24000;
    private int maxOutputTokens = 500;
    private int maxConcurrentCalls = 3;
    private int maxQueuedCalls = 3;
    private long perAssetRoleMinIntervalMs = 1000L;
    private BigDecimal dailyBudgetUsd;
    private BigDecimal perAnalysisBudgetUsd;
    private ProviderTimeouts providerTimeouts = new ProviderTimeouts();
    private BackgroundExecution backgroundExecution = new BackgroundExecution();
    private ModelStrategy modelStrategy = new ModelStrategy();
    private AiProviderProperties openai = new AiProviderProperties();
    private AiProviderProperties gemini = new AiProviderProperties();
    private AiProviderProperties xai = new AiProviderProperties();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(int requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }
    public int getOverallTimeoutMs() { return overallTimeoutMs; }
    public void setOverallTimeoutMs(int overallTimeoutMs) { this.overallTimeoutMs = overallTimeoutMs; }
    public int getMaxInputChars() { return maxInputChars; }
    public void setMaxInputChars(int maxInputChars) { this.maxInputChars = maxInputChars; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
    public void setMaxConcurrentCalls(int value) { this.maxConcurrentCalls = Math.max(1, value); }
    public int getMaxQueuedCalls() { return maxQueuedCalls; }
    public void setMaxQueuedCalls(int value) { this.maxQueuedCalls = Math.max(1, value); }
    public long getPerAssetRoleMinIntervalMs() { return perAssetRoleMinIntervalMs; }
    public void setPerAssetRoleMinIntervalMs(long value) { this.perAssetRoleMinIntervalMs = Math.max(0L, value); }
    public BigDecimal getDailyBudgetUsd() { return dailyBudgetUsd == null ? BigDecimal.ZERO : dailyBudgetUsd; }
    public BigDecimal getConfiguredDailyBudgetUsd() { return dailyBudgetUsd; }
    public void setDailyBudgetUsd(BigDecimal dailyBudgetUsd) {
        this.dailyBudgetUsd = dailyBudgetUsd;
    }
    public BigDecimal getPerAnalysisBudgetUsd() {
        return perAnalysisBudgetUsd == null ? BigDecimal.ZERO : perAnalysisBudgetUsd;
    }
    public BigDecimal getConfiguredPerAnalysisBudgetUsd() { return perAnalysisBudgetUsd; }
    public void setPerAnalysisBudgetUsd(BigDecimal perAnalysisBudgetUsd) {
        this.perAnalysisBudgetUsd = perAnalysisBudgetUsd;
    }
    public AiConfigurationPresence dailyBudgetPresence() {
        return AiConfigurationPresence.of(dailyBudgetUsd);
    }
    public AiConfigurationPresence perAnalysisBudgetPresence() {
        return AiConfigurationPresence.of(perAnalysisBudgetUsd);
    }
    public ProviderTimeouts getProviderTimeouts() { return providerTimeouts; }
    public void setProviderTimeouts(ProviderTimeouts providerTimeouts) {
        this.providerTimeouts = providerTimeouts == null ? new ProviderTimeouts() : providerTimeouts;
    }
    public BackgroundExecution getBackgroundExecution() { return backgroundExecution; }
    public void setBackgroundExecution(BackgroundExecution backgroundExecution) {
        this.backgroundExecution = backgroundExecution == null ? new BackgroundExecution() : backgroundExecution;
    }
    public ModelStrategy getModelStrategy() { return modelStrategy; }
    public void setModelStrategy(ModelStrategy modelStrategy) {
        this.modelStrategy = modelStrategy == null ? new ModelStrategy() : modelStrategy;
    }
    public AiProviderProperties getOpenai() { return openai; }
    public void setOpenai(AiProviderProperties openai) { this.openai = openai == null ? new AiProviderProperties() : openai; }
    public AiProviderProperties getGemini() { return gemini; }
    public void setGemini(AiProviderProperties gemini) { this.gemini = gemini == null ? new AiProviderProperties() : gemini; }
    public AiProviderProperties getXai() { return xai; }
    public void setXai(AiProviderProperties xai) { this.xai = xai == null ? new AiProviderProperties() : xai; }

    public static class ModelStrategy {
        private RoleStrategy gptFinal = new RoleStrategy(AiRoleModelPriority.QUALITY_FIRST);
        private RoleStrategy geminiReview = new RoleStrategy(AiRoleModelPriority.BALANCED);
        private RoleStrategy grokChallenge = new RoleStrategy(AiRoleModelPriority.CHALLENGE_FIRST);

        public RoleStrategy getGptFinal() { return gptFinal; }
        public void setGptFinal(RoleStrategy gptFinal) {
            this.gptFinal = gptFinal == null
                    ? new RoleStrategy(AiRoleModelPriority.QUALITY_FIRST) : gptFinal;
        }
        public RoleStrategy getGeminiReview() { return geminiReview; }
        public void setGeminiReview(RoleStrategy geminiReview) {
            this.geminiReview = geminiReview == null
                    ? new RoleStrategy(AiRoleModelPriority.BALANCED) : geminiReview;
        }
        public RoleStrategy getGrokChallenge() { return grokChallenge; }
        public void setGrokChallenge(RoleStrategy grokChallenge) {
            this.grokChallenge = grokChallenge == null
                    ? new RoleStrategy(AiRoleModelPriority.CHALLENGE_FIRST) : grokChallenge;
        }
    }

    public static class RoleStrategy {
        private AiRoleModelPriority priority;

        public RoleStrategy() {
            this(AiRoleModelPriority.BALANCED);
        }

        public RoleStrategy(AiRoleModelPriority priority) {
            this.priority = priority;
        }

        public AiRoleModelPriority getPriority() { return priority; }
        public void setPriority(AiRoleModelPriority priority) {
            this.priority = priority == null ? AiRoleModelPriority.BALANCED : priority;
        }
    }

    public static class ProviderTimeouts {
        public static final int MIN_PROVIDER_MS = 1_000;
        public static final int MAX_PROVIDER_MS = 30_000;
        public static final int MIN_OVERALL_MS = 5_000;
        public static final int MAX_OVERALL_MS = 60_000;

        private int openaiMs = 10_000;
        private int geminiMs = 25_000;
        private int xaiMs = 10_000;
        private int overallMs = 30_000;

        public int getOpenaiMs() { return openaiMs; }
        public void setOpenaiMs(int openaiMs) { this.openaiMs = openaiMs; }
        public int getGeminiMs() { return geminiMs; }
        public void setGeminiMs(int geminiMs) { this.geminiMs = geminiMs; }
        public int getXaiMs() { return xaiMs; }
        public void setXaiMs(int xaiMs) { this.xaiMs = xaiMs; }
        public int getOverallMs() { return overallMs; }
        public void setOverallMs(int overallMs) { this.overallMs = overallMs; }

        public int timeoutMs(AiProviderName provider) {
            if (provider == AiProviderName.OPENAI) {
                return openaiMs;
            }
            if (provider == AiProviderName.GEMINI) {
                return geminiMs;
            }
            return xaiMs;
        }

        public boolean validOverall() {
            return overallMs >= MIN_OVERALL_MS && overallMs <= MAX_OVERALL_MS;
        }

        public boolean validProvider(AiProviderName provider) {
            int timeoutMs = timeoutMs(provider);
            return validOverall()
                    && timeoutMs >= MIN_PROVIDER_MS
                    && timeoutMs <= MAX_PROVIDER_MS
                    && timeoutMs <= overallMs;
        }
    }

    public static class BackgroundExecution {
        public static final String RUNTIME_CONFIG_VERSION = "V41-AI-BACKGROUND-TIMEOUT-1";
        public static final String INPUT_CONTRACT_VERSION = "V41-AI-INPUT-COMPACT-1";
        public static final String PROMPT_VERSION = "V41-AI-ROLE-PROMPT-1";
        public static final String SCHEMA_VERSION = "V41-AI-STRUCTURED-SCHEMA-1";

        private boolean enabled = true;
        private int submitAckTimeoutMs = 30_000;
        private int gptJobDeadlineMs = 180_000;
        private int reviewJobDeadlineMs = 120_000;
        private int chainDeadlineMs = 300_000;
        private int initialPollIntervalMs = 2_000;
        private int maxPollIntervalMs = 10_000;
        private int maxTransientRetries = 1;
        private int structuredMaxOutputTokens = 4_000;
        private String reasoningEffort = "medium";
        private String textVerbosity = "low";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getSubmitAckTimeoutMs() { return submitAckTimeoutMs; }
        public void setSubmitAckTimeoutMs(int value) { this.submitAckTimeoutMs = Math.max(1, value); }
        public int getGptJobDeadlineMs() { return gptJobDeadlineMs; }
        public void setGptJobDeadlineMs(int value) { this.gptJobDeadlineMs = Math.max(1, value); }
        public int getReviewJobDeadlineMs() { return reviewJobDeadlineMs; }
        public void setReviewJobDeadlineMs(int value) { this.reviewJobDeadlineMs = Math.max(1, value); }
        public int getChainDeadlineMs() { return chainDeadlineMs; }
        public void setChainDeadlineMs(int value) { this.chainDeadlineMs = Math.max(1, value); }
        public int getInitialPollIntervalMs() { return initialPollIntervalMs; }
        public void setInitialPollIntervalMs(int value) { this.initialPollIntervalMs = Math.max(1, value); }
        public int getMaxPollIntervalMs() { return maxPollIntervalMs; }
        public void setMaxPollIntervalMs(int value) { this.maxPollIntervalMs = Math.max(1, value); }
        public int getMaxTransientRetries() { return maxTransientRetries; }
        public void setMaxTransientRetries(int value) { this.maxTransientRetries = Math.max(0, Math.min(1, value)); }
        public int getStructuredMaxOutputTokens() { return structuredMaxOutputTokens; }
        public void setStructuredMaxOutputTokens(int value) { this.structuredMaxOutputTokens = Math.max(1, value); }
        public String getReasoningEffort() { return reasoningEffort; }
        public void setReasoningEffort(String value) {
            this.reasoningEffort = value == null || value.isBlank() ? "medium" : value.trim();
        }
        public String getTextVerbosity() { return textVerbosity; }
        public void setTextVerbosity(String value) {
            this.textVerbosity = value == null || value.isBlank() ? "low" : value.trim();
        }

        public boolean validFrozenContract() {
            return enabled
                    && submitAckTimeoutMs == 30_000
                    && gptJobDeadlineMs == 180_000
                    && reviewJobDeadlineMs == 120_000
                    && chainDeadlineMs == 300_000
                    && initialPollIntervalMs == 2_000
                    && maxPollIntervalMs == 10_000
                    && maxTransientRetries == 1
                    && structuredMaxOutputTokens > 0
                    && "medium".equals(reasoningEffort)
                    && "low".equals(textVerbosity);
        }
    }
}
