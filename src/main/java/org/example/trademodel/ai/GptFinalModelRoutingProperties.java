package org.example.trademodel.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GptFinalModelRoutingProperties {
    private String fastModel = "";
    private String reasoningModel = "";
    private List<String> fallbackModels = new ArrayList<>();
    private boolean fallbackEnabled = true;

    public String getFastModel() { return fastModel; }
    public void setFastModel(String fastModel) { this.fastModel = fastModel; }
    public String getReasoningModel() { return reasoningModel; }
    public void setReasoningModel(String reasoningModel) { this.reasoningModel = reasoningModel; }
    public List<String> getFallbackModels() { return Collections.unmodifiableList(fallbackModels); }
    public void setFallbackModels(List<String> fallbackModels) {
        this.fallbackModels = fallbackModels == null ? new ArrayList<>() : new ArrayList<>(fallbackModels);
    }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }

    public boolean isConfigured() {
        return OpenAiModelRouter.isApprovedPrimary(fastModel)
                && OpenAiModelRouter.isApprovedPrimary(reasoningModel)
                && fallbackModels.size() == 2
                && OpenAiModelRouter.isApprovedGpt55(fallbackModels.get(0))
                && OpenAiModelRouter.isApprovedGpt54(fallbackModels.get(1));
    }
}
