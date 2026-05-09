package org.example.trademodel.entity;
import java.time.LocalDateTime;

public class ExecutionPlanDO {
    private String planId;
    private String analysisId;
    private String planMode;
    private String recommendedAction;
    private String entryZone;
    private String stopLoss;
    private String takeProfitRules;
    private String leverageSuggestion;
    private String positionSuggestion;
    private String accountRiskJson;
    private String invalidCondition;
    private String planBoundaryJson;
    private LocalDateTime createTime;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getPlanMode() { return planMode; }
    public void setPlanMode(String planMode) { this.planMode = planMode; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getEntryZone() { return entryZone; }
    public void setEntryZone(String entryZone) { this.entryZone = entryZone; }
    public String getStopLoss() { return stopLoss; }
    public void setStopLoss(String stopLoss) { this.stopLoss = stopLoss; }
    public String getTakeProfitRules() { return takeProfitRules; }
    public void setTakeProfitRules(String takeProfitRules) { this.takeProfitRules = takeProfitRules; }
    public String getLeverageSuggestion() { return leverageSuggestion; }
    public void setLeverageSuggestion(String leverageSuggestion) { this.leverageSuggestion = leverageSuggestion; }
    public String getPositionSuggestion() { return positionSuggestion; }
    public void setPositionSuggestion(String positionSuggestion) { this.positionSuggestion = positionSuggestion; }
    public String getAccountRiskJson() { return accountRiskJson; }
    public void setAccountRiskJson(String accountRiskJson) { this.accountRiskJson = accountRiskJson; }
    public String getInvalidCondition() { return invalidCondition; }
    public void setInvalidCondition(String invalidCondition) { this.invalidCondition = invalidCondition; }
    public String getPlanBoundaryJson() { return planBoundaryJson; }
    public void setPlanBoundaryJson(String planBoundaryJson) { this.planBoundaryJson = planBoundaryJson; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
