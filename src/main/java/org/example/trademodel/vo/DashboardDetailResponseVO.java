package org.example.trademodel.vo;

import java.util.List;

public class DashboardDetailResponseVO {
    private String symbol;
    private DecisionResultVO decision;
    private MarketEnvironmentMiniVO marketEnvironmentMini;
    private List<EvidenceBriefVO> evidenceTopItems;
    private List<ScoreBriefVO> scoreTopItems;
    private List<ScoreEightItemVO> scoreEightItems;
    private PlanReadinessVO planReadiness;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public DecisionResultVO getDecision() {
        return decision;
    }

    public void setDecision(DecisionResultVO decision) {
        this.decision = decision;
    }

    public MarketEnvironmentMiniVO getMarketEnvironmentMini() {
        return marketEnvironmentMini;
    }

    public void setMarketEnvironmentMini(MarketEnvironmentMiniVO marketEnvironmentMini) {
        this.marketEnvironmentMini = marketEnvironmentMini;
    }

    public List<EvidenceBriefVO> getEvidenceTopItems() {
        return evidenceTopItems;
    }

    public void setEvidenceTopItems(List<EvidenceBriefVO> evidenceTopItems) {
        this.evidenceTopItems = evidenceTopItems;
    }

    public List<ScoreBriefVO> getScoreTopItems() {
        return scoreTopItems;
    }

    public void setScoreTopItems(List<ScoreBriefVO> scoreTopItems) {
        this.scoreTopItems = scoreTopItems;
    }

    public List<ScoreEightItemVO> getScoreEightItems() {
        return scoreEightItems;
    }

    public void setScoreEightItems(List<ScoreEightItemVO> scoreEightItems) {
        this.scoreEightItems = scoreEightItems;
    }

    public PlanReadinessVO getPlanReadiness() {
        return planReadiness;
    }

    public void setPlanReadiness(PlanReadinessVO planReadiness) {
        this.planReadiness = planReadiness;
    }

    public static class MarketEnvironmentMiniVO {
        private String summary;
        private String environmentType;
        private String riskMode;
        private String sourceType;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getEnvironmentType() {
            return environmentType;
        }

        public void setEnvironmentType(String environmentType) {
            this.environmentType = environmentType;
        }

        public String getRiskMode() {
            return riskMode;
        }

        public void setRiskMode(String riskMode) {
            this.riskMode = riskMode;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }
    }
}
