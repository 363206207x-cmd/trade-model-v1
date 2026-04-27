package org.example.trademodel.service;

public class RuleBaseOutput {

    private String marketBias;
    private String confidenceLevel;
    private String riskLevel;
    private String planMode;
    private boolean canExecute;

    public String getMarketBias() {
        return marketBias;
    }

    public void setMarketBias(String marketBias) {
        this.marketBias = marketBias;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getPlanMode() {
        return planMode;
    }

    public void setPlanMode(String planMode) {
        this.planMode = planMode;
    }

    public boolean isCanExecute() {
        return canExecute;
    }

    public void setCanExecute(boolean canExecute) {
        this.canExecute = canExecute;
    }
}
