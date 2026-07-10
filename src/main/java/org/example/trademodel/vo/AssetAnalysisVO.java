package org.example.trademodel.vo;

import org.example.trademodel.derivatives.DerivativesBusinessAssessment;

import java.util.List;

public class AssetAnalysisVO {
    private String analysisId;
    private String symbol;
    private String timeframe;
    private String analysisTime;
    private MarketEnvironmentVO marketEnvironment;
    private List<EvidenceItemVO> evidenceList;
    private List<ScoreItemVO> scoreList;
    private DecisionBundleVO decisionBundle;
    private Integer dataQualityScore;
    private EventImpactInputVO eventImpactInput;
    private DerivativesBusinessAssessment derivativesAssessment;

    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public String getAnalysisTime() { return analysisTime; }
    public void setAnalysisTime(String analysisTime) { this.analysisTime = analysisTime; }
    public MarketEnvironmentVO getMarketEnvironment() { return marketEnvironment; }
    public void setMarketEnvironment(MarketEnvironmentVO marketEnvironment) { this.marketEnvironment = marketEnvironment; }
    public List<EvidenceItemVO> getEvidenceList() { return evidenceList; }
    public void setEvidenceList(List<EvidenceItemVO> evidenceList) { this.evidenceList = evidenceList; }
    public List<ScoreItemVO> getScoreList() { return scoreList; }
    public void setScoreList(List<ScoreItemVO> scoreList) { this.scoreList = scoreList; }
    public DecisionBundleVO getDecisionBundle() { return decisionBundle; }
    public void setDecisionBundle(DecisionBundleVO decisionBundle) { this.decisionBundle = decisionBundle; }
    public Integer getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(Integer dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    public EventImpactInputVO getEventImpactInput() { return eventImpactInput; }
    public void setEventImpactInput(EventImpactInputVO eventImpactInput) { this.eventImpactInput = eventImpactInput; }
    public DerivativesBusinessAssessment getDerivativesAssessment() { return derivativesAssessment; }
    public void setDerivativesAssessment(DerivativesBusinessAssessment derivativesAssessment) { this.derivativesAssessment = derivativesAssessment; }
}
