package org.example.trademodel.service;

import org.example.trademodel.enums.AiConflictLevelEnum;

public class AiConflictResult {

    private AiConflictLevelEnum level;
    private String baseMarketBias;
    private String finalMarketBias;
    private String adjustedConfidence;
    private String riskAdjustment;
    private String planMode;
    /** 与 {@link org.example.trademodel.service.impl.AiConflictResolverServiceImpl} 本 run 加权结果一致 */
    private int aiConflictScore;
    private int aiObjectionCount;
    private boolean singleObjectionOnly;
    private int confusedContribution;

    private final boolean ruleDirectionPreserved = true;
    private final boolean notRuleBypass = true;
    private final boolean notStateMachineOverride = true;
    private final boolean notTradeInstruction = true;
    private final boolean notExecutable = true;
    private final boolean notAutoTrading = true;
    private final boolean notOrderExecution = true;

    public AiConflictResult() {
    }

    public AiConflictResult(AiConflictLevelEnum level, String finalMarketBias, String adjustedConfidence, String planMode,
                            int aiConflictScore) {
        this.level = level;
        this.baseMarketBias = finalMarketBias;
        this.finalMarketBias = finalMarketBias;
        this.adjustedConfidence = adjustedConfidence;
        this.riskAdjustment = "UNCHANGED";
        this.planMode = planMode;
        this.aiConflictScore = aiConflictScore;
    }

    public AiConflictResult(AiConflictLevelEnum level, String baseMarketBias, String adjustedConfidence,
                            String riskAdjustment, String planMode, int aiConflictScore,
                            int aiObjectionCount, boolean singleObjectionOnly, int confusedContribution) {
        this.level = level;
        this.baseMarketBias = baseMarketBias;
        this.finalMarketBias = baseMarketBias;
        this.adjustedConfidence = adjustedConfidence;
        this.riskAdjustment = riskAdjustment;
        this.planMode = planMode;
        this.aiConflictScore = aiConflictScore;
        this.aiObjectionCount = aiObjectionCount;
        this.singleObjectionOnly = singleObjectionOnly;
        this.confusedContribution = confusedContribution;
    }

    public AiConflictLevelEnum getLevel() {
        return level;
    }

    public void setLevel(AiConflictLevelEnum level) {
        this.level = level;
    }

    public String getBaseMarketBias() {
        return baseMarketBias;
    }

    public void setBaseMarketBias(String baseMarketBias) {
        this.baseMarketBias = baseMarketBias;
        this.finalMarketBias = baseMarketBias;
    }

    public String getFinalMarketBias() {
        return finalMarketBias;
    }

    public void setFinalMarketBias(String finalMarketBias) {
        this.finalMarketBias = baseMarketBias != null ? baseMarketBias : finalMarketBias;
    }

    public String getAdjustedConfidence() {
        return adjustedConfidence;
    }

    public void setAdjustedConfidence(String adjustedConfidence) {
        this.adjustedConfidence = adjustedConfidence;
    }

    public String getRiskAdjustment() {
        return riskAdjustment;
    }

    public void setRiskAdjustment(String riskAdjustment) {
        this.riskAdjustment = riskAdjustment;
    }

    public String getPlanMode() {
        return planMode;
    }

    public void setPlanMode(String planMode) {
        this.planMode = planMode;
    }

    public int getAiConflictScore() {
        return aiConflictScore;
    }

    public void setAiConflictScore(int aiConflictScore) {
        this.aiConflictScore = aiConflictScore;
    }

    public int getAiObjectionCount() {
        return aiObjectionCount;
    }

    public void setAiObjectionCount(int aiObjectionCount) {
        this.aiObjectionCount = aiObjectionCount;
    }

    public boolean isSingleObjectionOnly() {
        return singleObjectionOnly;
    }

    public void setSingleObjectionOnly(boolean singleObjectionOnly) {
        this.singleObjectionOnly = singleObjectionOnly;
    }

    public int getConfusedContribution() {
        return confusedContribution;
    }

    public void setConfusedContribution(int confusedContribution) {
        this.confusedContribution = confusedContribution;
    }

    public boolean isRuleDirectionPreserved() {
        return ruleDirectionPreserved;
    }

    public boolean isNotRuleBypass() {
        return notRuleBypass;
    }

    public boolean isNotStateMachineOverride() {
        return notStateMachineOverride;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isNotExecutable() {
        return notExecutable;
    }

    public boolean isNotAutoTrading() {
        return notAutoTrading;
    }

    public boolean isNotOrderExecution() {
        return notOrderExecution;
    }
}
