package org.example.trademodel.service;

public class DecisionContext {

    private String symbol;
    private String ruleMarketBias;
    private String ruleConfidenceLevel;
    /** 与本次 K 线多周期是否对齐一致（来自 DecisionEngineService 本 run 计算） */
    private boolean multiTimeframeAligned = true;
    /** LOW / MEDIUM — 来自本 run 风险档位 */
    private String riskTier = "MEDIUM";
    /** 本 run 是否认为值得开仓 */
    private Boolean worthOpening;
    private boolean hasRuleBaseOutput = true;
    private boolean gptConsistentWithRule = true;
    private boolean geminiConsistentWithRule = true;
    private boolean grokConsistentWithRule = true;
    private Integer aiObjectionCount = 0;
    private Integer aiSupportCount = 3;
    private Integer driverConflictScore;
    private Integer executionInstabilityScore;
    private Integer microstructureTrapScore;
    private Integer causeEffectDivergenceScore;
    private Integer aiConflictScore;
    private Integer aiProviderConflictContribution = 0;
    private String aiOrchestrationMode = "RULE_ONLY_FALLBACK";
    private String aiOrchestrationSummary;
    private Integer consecutiveLowConfusedCount = 0;

    /** 主链估算的数据质量分（可为 null，规则侧不强制） */
    private Integer dataQualityScore;
    private String externalContextRiskLevel;
    private Boolean externalContextBlocked;
    private String externalContextSourceHealth;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getRuleMarketBias() {
        return ruleMarketBias;
    }

    public void setRuleMarketBias(String ruleMarketBias) {
        this.ruleMarketBias = ruleMarketBias;
    }

    public String getRuleConfidenceLevel() {
        return ruleConfidenceLevel;
    }

    public void setRuleConfidenceLevel(String ruleConfidenceLevel) {
        this.ruleConfidenceLevel = ruleConfidenceLevel;
    }

    public boolean isMultiTimeframeAligned() {
        return multiTimeframeAligned;
    }

    public void setMultiTimeframeAligned(boolean multiTimeframeAligned) {
        this.multiTimeframeAligned = multiTimeframeAligned;
    }

    public String getRiskTier() {
        return riskTier;
    }

    public void setRiskTier(String riskTier) {
        this.riskTier = riskTier;
    }

    public Boolean getWorthOpening() {
        return worthOpening;
    }

    public void setWorthOpening(Boolean worthOpening) {
        this.worthOpening = worthOpening;
    }

    public boolean isHasRuleBaseOutput() {
        return hasRuleBaseOutput;
    }

    public void setHasRuleBaseOutput(boolean hasRuleBaseOutput) {
        this.hasRuleBaseOutput = hasRuleBaseOutput;
    }

    public boolean isGptConsistentWithRule() {
        return gptConsistentWithRule;
    }

    public void setGptConsistentWithRule(boolean gptConsistentWithRule) {
        this.gptConsistentWithRule = gptConsistentWithRule;
    }

    public boolean isGeminiConsistentWithRule() {
        return geminiConsistentWithRule;
    }

    public void setGeminiConsistentWithRule(boolean geminiConsistentWithRule) {
        this.geminiConsistentWithRule = geminiConsistentWithRule;
    }

    public boolean isGrokConsistentWithRule() {
        return grokConsistentWithRule;
    }

    public void setGrokConsistentWithRule(boolean grokConsistentWithRule) {
        this.grokConsistentWithRule = grokConsistentWithRule;
    }

    public Integer getAiObjectionCount() {
        return aiObjectionCount;
    }

    public void setAiObjectionCount(Integer aiObjectionCount) {
        this.aiObjectionCount = aiObjectionCount;
    }

    public Integer getAiSupportCount() {
        return aiSupportCount;
    }

    public void setAiSupportCount(Integer aiSupportCount) {
        this.aiSupportCount = aiSupportCount;
    }

    public Integer getDriverConflictScore() {
        return driverConflictScore;
    }

    public void setDriverConflictScore(Integer driverConflictScore) {
        this.driverConflictScore = driverConflictScore;
    }

    public Integer getExecutionInstabilityScore() {
        return executionInstabilityScore;
    }

    public void setExecutionInstabilityScore(Integer executionInstabilityScore) {
        this.executionInstabilityScore = executionInstabilityScore;
    }

    public Integer getMicrostructureTrapScore() {
        return microstructureTrapScore;
    }

    public void setMicrostructureTrapScore(Integer microstructureTrapScore) {
        this.microstructureTrapScore = microstructureTrapScore;
    }

    public Integer getCauseEffectDivergenceScore() {
        return causeEffectDivergenceScore;
    }

    public void setCauseEffectDivergenceScore(Integer causeEffectDivergenceScore) {
        this.causeEffectDivergenceScore = causeEffectDivergenceScore;
    }

    public Integer getAiConflictScore() {
        return aiConflictScore;
    }

    public void setAiConflictScore(Integer aiConflictScore) {
        this.aiConflictScore = aiConflictScore;
    }

    public Integer getAiProviderConflictContribution() {
        return aiProviderConflictContribution;
    }

    public void setAiProviderConflictContribution(Integer aiProviderConflictContribution) {
        this.aiProviderConflictContribution = aiProviderConflictContribution;
    }

    public String getAiOrchestrationMode() {
        return aiOrchestrationMode;
    }

    public void setAiOrchestrationMode(String aiOrchestrationMode) {
        this.aiOrchestrationMode = aiOrchestrationMode;
    }

    public String getAiOrchestrationSummary() {
        return aiOrchestrationSummary;
    }

    public void setAiOrchestrationSummary(String aiOrchestrationSummary) {
        this.aiOrchestrationSummary = aiOrchestrationSummary;
    }

    public Integer getConsecutiveLowConfusedCount() {
        return consecutiveLowConfusedCount;
    }

    public void setConsecutiveLowConfusedCount(Integer consecutiveLowConfusedCount) {
        this.consecutiveLowConfusedCount = consecutiveLowConfusedCount;
    }

    public Integer getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(Integer dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public String getExternalContextRiskLevel() { return externalContextRiskLevel; }
    public void setExternalContextRiskLevel(String externalContextRiskLevel) { this.externalContextRiskLevel = externalContextRiskLevel; }
    public Boolean getExternalContextBlocked() { return externalContextBlocked; }
    public void setExternalContextBlocked(Boolean externalContextBlocked) { this.externalContextBlocked = externalContextBlocked; }
    public String getExternalContextSourceHealth() { return externalContextSourceHealth; }
    public void setExternalContextSourceHealth(String externalContextSourceHealth) { this.externalContextSourceHealth = externalContextSourceHealth; }
}
