package org.example.trademodel.vo;

public class ExecutionPlanVO {
    public static final String PLAN_MODE_ADVISORY = "ADVISORY";
    public static final String PLAN_MODE_SEMI_STRUCTURED = "SEMI_STRUCTURED";

    private String planId;
    private String planMode;
    private String recommendedAction;
    private String entryZone;
    private String stopLoss;
    private String takeProfitRules;
    private String addPositionCondition;
    private String reducePositionCondition;
    private String abandonCondition;
    private String invalidCondition;
    private String leverageSuggestion;
    private String positionSuggestion;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
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
    public String getAddPositionCondition() { return addPositionCondition; }
    public void setAddPositionCondition(String addPositionCondition) { this.addPositionCondition = addPositionCondition; }
    public String getReducePositionCondition() { return reducePositionCondition; }
    public void setReducePositionCondition(String reducePositionCondition) { this.reducePositionCondition = reducePositionCondition; }
    public String getAbandonCondition() { return abandonCondition; }
    public void setAbandonCondition(String abandonCondition) { this.abandonCondition = abandonCondition; }
    public String getInvalidCondition() { return invalidCondition; }
    public void setInvalidCondition(String invalidCondition) { this.invalidCondition = invalidCondition; }
    public String getLeverageSuggestion() { return leverageSuggestion; }
    public void setLeverageSuggestion(String leverageSuggestion) { this.leverageSuggestion = leverageSuggestion; }
    public String getPositionSuggestion() { return positionSuggestion; }
    public void setPositionSuggestion(String positionSuggestion) { this.positionSuggestion = positionSuggestion; }
}
