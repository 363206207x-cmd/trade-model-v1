package org.example.trademodel.ai;

import java.math.BigDecimal;

public class AiProviderProperties {
    private boolean enabled;
    private String apiKey = "";
    private String model = "";
    private GptFinalModelRoutingProperties gptFinal = new GptFinalModelRoutingProperties();
    private String baseUrl = "";
    private Integer requestsPerMinute;
    private BigDecimal inputCostPerMillionUsd;
    private BigDecimal outputCostPerMillionUsd;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public GptFinalModelRoutingProperties getGptFinal() { return gptFinal; }
    public void setGptFinal(GptFinalModelRoutingProperties gptFinal) {
        this.gptFinal = gptFinal == null ? new GptFinalModelRoutingProperties() : gptFinal;
    }
    public String getConfiguredModel() {
        String configured = normalize(model);
        return configured.isBlank() ? normalize(gptFinal.getFastModel()) : configured;
    }
    public String getEffectiveModel() { return getConfiguredModel(); }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getRequestsPerMinute() { return requestsPerMinute == null ? 0 : requestsPerMinute; }
    public Integer getConfiguredRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(Integer requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
    public BigDecimal getInputCostPerMillionUsd() {
        return inputCostPerMillionUsd == null ? BigDecimal.ZERO : inputCostPerMillionUsd;
    }
    public BigDecimal getConfiguredInputCostPerMillionUsd() { return inputCostPerMillionUsd; }
    public void setInputCostPerMillionUsd(BigDecimal inputCostPerMillionUsd) {
        this.inputCostPerMillionUsd = inputCostPerMillionUsd;
    }
    public BigDecimal getOutputCostPerMillionUsd() {
        return outputCostPerMillionUsd == null ? BigDecimal.ZERO : outputCostPerMillionUsd;
    }
    public BigDecimal getConfiguredOutputCostPerMillionUsd() { return outputCostPerMillionUsd; }
    public void setOutputCostPerMillionUsd(BigDecimal outputCostPerMillionUsd) {
        this.outputCostPerMillionUsd = outputCostPerMillionUsd;
    }

    public AiConfigurationPresence requestsPerMinutePresence() {
        return AiConfigurationPresence.of(requestsPerMinute);
    }

    public AiConfigurationPresence inputCostPresence() {
        return AiConfigurationPresence.of(inputCostPerMillionUsd);
    }

    public AiConfigurationPresence outputCostPresence() {
        return AiConfigurationPresence.of(outputCostPerMillionUsd);
    }

    public boolean hasKeyAndModel() {
        return apiKey != null && !apiKey.isBlank() && hasValidModelSelection();
    }

    public boolean hasValidModelSelection() {
        return !normalize(model).isBlank() || gptFinal.isConfigured();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
