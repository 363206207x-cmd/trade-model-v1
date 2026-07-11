package org.example.trademodel.ai;

import java.math.BigDecimal;

public class AiProviderProperties {
    public static final String OPENAI_COMPATIBILITY_FALLBACK_REASON =
            "OPENAI_MODEL_FALLBACK_COMPATIBILITY";

    private boolean enabled;
    private String apiKey = "";
    private String model = "";
    private boolean compatibilityFallbackActive;
    private String compatibilityFallbackModel = "";
    private String fallbackReason = "";
    private String baseUrl = "";
    private int requestsPerMinute;
    private BigDecimal inputCostPerMillionUsd = BigDecimal.ZERO;
    private BigDecimal outputCostPerMillionUsd = BigDecimal.ZERO;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getConfiguredModel() { return normalize(model); }
    public String getEffectiveModel() {
        if (!compatibilityFallbackActive) {
            return getConfiguredModel();
        }
        if (!hasValidFallback()) {
            return "";
        }
        return normalize(compatibilityFallbackModel);
    }
    public boolean isCompatibilityFallbackActive() { return compatibilityFallbackActive; }
    public void setCompatibilityFallbackActive(boolean compatibilityFallbackActive) {
        this.compatibilityFallbackActive = compatibilityFallbackActive;
    }
    public String getCompatibilityFallbackModel() { return compatibilityFallbackModel; }
    public void setCompatibilityFallbackModel(String compatibilityFallbackModel) {
        this.compatibilityFallbackModel = compatibilityFallbackModel;
    }
    public String getFallbackReason() { return isFallbackUsed() ? fallbackReason : null; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public boolean isFallbackUsed() {
        return compatibilityFallbackActive && hasValidFallback();
    }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
    public BigDecimal getInputCostPerMillionUsd() { return inputCostPerMillionUsd; }
    public void setInputCostPerMillionUsd(BigDecimal inputCostPerMillionUsd) {
        this.inputCostPerMillionUsd = inputCostPerMillionUsd == null ? BigDecimal.ZERO : inputCostPerMillionUsd;
    }
    public BigDecimal getOutputCostPerMillionUsd() { return outputCostPerMillionUsd; }
    public void setOutputCostPerMillionUsd(BigDecimal outputCostPerMillionUsd) {
        this.outputCostPerMillionUsd = outputCostPerMillionUsd == null ? BigDecimal.ZERO : outputCostPerMillionUsd;
    }

    public boolean hasKeyAndModel() {
        return apiKey != null && !apiKey.isBlank() && hasValidModelSelection();
    }

    public boolean hasValidModelSelection() {
        return !getConfiguredModel().isBlank()
                && (!compatibilityFallbackActive || hasValidFallback());
    }

    private boolean hasValidFallback() {
        return !normalize(compatibilityFallbackModel).isBlank()
                && OPENAI_COMPATIBILITY_FALLBACK_REASON.equals(normalize(fallbackReason));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
