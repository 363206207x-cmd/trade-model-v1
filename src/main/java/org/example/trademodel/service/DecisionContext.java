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
    private Integer driverConflictScore;
    private Integer executionInstabilityScore;
    private Integer microstructureTrapScore;
    private Integer causeEffectDivergenceScore;
    private Integer aiConflictScore;
    private Integer consecutiveLowConfusedCount = 0;

    /** 主链估算的数据质量分（可为 null，规则侧不强制） */
    private Integer dataQualityScore;

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
}
