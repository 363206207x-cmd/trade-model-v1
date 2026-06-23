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
    private BigDecimal dailyBudgetUsd = BigDecimal.ZERO;
    private BigDecimal perAnalysisBudgetUsd = BigDecimal.ZERO;
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
    public BigDecimal getDailyBudgetUsd() { return dailyBudgetUsd; }
    public void setDailyBudgetUsd(BigDecimal dailyBudgetUsd) {
        this.dailyBudgetUsd = dailyBudgetUsd == null ? BigDecimal.ZERO : dailyBudgetUsd;
    }
    public BigDecimal getPerAnalysisBudgetUsd() { return perAnalysisBudgetUsd; }
    public void setPerAnalysisBudgetUsd(BigDecimal perAnalysisBudgetUsd) {
        this.perAnalysisBudgetUsd = perAnalysisBudgetUsd == null ? BigDecimal.ZERO : perAnalysisBudgetUsd;
    }
    public AiProviderProperties getOpenai() { return openai; }
    public void setOpenai(AiProviderProperties openai) { this.openai = openai == null ? new AiProviderProperties() : openai; }
    public AiProviderProperties getGemini() { return gemini; }
    public void setGemini(AiProviderProperties gemini) { this.gemini = gemini == null ? new AiProviderProperties() : gemini; }
    public AiProviderProperties getXai() { return xai; }
    public void setXai(AiProviderProperties xai) { this.xai = xai == null ? new AiProviderProperties() : xai; }
}
