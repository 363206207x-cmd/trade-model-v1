package org.example.trademodel.service;

import org.example.trademodel.enums.AiConflictLevelEnum;

public class AiConflictResult {

    private AiConflictLevelEnum level;
    private String finalMarketBias;
    private String adjustedConfidence;
    private String planMode;
    /** 与 {@link org.example.trademodel.service.impl.AiConflictResolverServiceImpl} 本 run 加权结果一致 */
    private int aiConflictScore;

    public AiConflictResult() {
    }

    public AiConflictResult(AiConflictLevelEnum level, String finalMarketBias, String adjustedConfidence, String planMode,
                            int aiConflictScore) {
        this.level = level;
        this.finalMarketBias = finalMarketBias;
        this.adjustedConfidence = adjustedConfidence;
        this.planMode = planMode;
        this.aiConflictScore = aiConflictScore;
    }

    public AiConflictLevelEnum getLevel() {
        return level;
    }

    public void setLevel(AiConflictLevelEnum level) {
        this.level = level;
    }

    public String getFinalMarketBias() {
        return finalMarketBias;
    }

    public void setFinalMarketBias(String finalMarketBias) {
        this.finalMarketBias = finalMarketBias;
    }

    public String getAdjustedConfidence() {
        return adjustedConfidence;
    }

    public void setAdjustedConfidence(String adjustedConfidence) {
        this.adjustedConfidence = adjustedConfidence;
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
}
