package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketEnvironmentSnapshotDO {
    private Long id;
    private String analysisId;
    private String symbol;
    private String timeframe;
    private String environmentType;
    private String riskMode;
    private Integer trendFriendliness;
    private String leverageSuggestion;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getRangePct24h()} 同源；不可从 summary 反推。 */
    private Double rangePct24h;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getVolatilityRegime()} 同源（窄幅/中等波动/高波动）。 */
    private String volatilityRegime;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getLastFundingRate()} 同源；不从 summary 反推。 */
    private BigDecimal lastFundingRate;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getPerpFundingApplied()} 同源。 */
    private Boolean perpFundingApplied;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getLastOpenInterest()} 同源；不从 summary 反推。 */
    private BigDecimal lastOpenInterest;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getOpenInterestDelta()} 同源。 */
    private BigDecimal openInterestDelta;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getOiApplied()} 同源。 */
    private Boolean oiApplied;
    /** 与 {@link org.example.trademodel.vo.MarketEnvironmentVO#getDerivativesCrowdingState()} 同源。 */
    private String derivativesCrowdingState;
    private String summary;
    private String sourceType;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
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

    public Integer getTrendFriendliness() {
        return trendFriendliness;
    }

    public void setTrendFriendliness(Integer trendFriendliness) {
        this.trendFriendliness = trendFriendliness;
    }

    public String getLeverageSuggestion() {
        return leverageSuggestion;
    }

    public void setLeverageSuggestion(String leverageSuggestion) {
        this.leverageSuggestion = leverageSuggestion;
    }

    public Double getRangePct24h() {
        return rangePct24h;
    }

    public void setRangePct24h(Double rangePct24h) {
        this.rangePct24h = rangePct24h;
    }

    public String getVolatilityRegime() {
        return volatilityRegime;
    }

    public void setVolatilityRegime(String volatilityRegime) {
        this.volatilityRegime = volatilityRegime;
    }

    public BigDecimal getLastFundingRate() {
        return lastFundingRate;
    }

    public void setLastFundingRate(BigDecimal lastFundingRate) {
        this.lastFundingRate = lastFundingRate;
    }

    public Boolean getPerpFundingApplied() {
        return perpFundingApplied;
    }

    public void setPerpFundingApplied(Boolean perpFundingApplied) {
        this.perpFundingApplied = perpFundingApplied;
    }

    public BigDecimal getLastOpenInterest() {
        return lastOpenInterest;
    }

    public void setLastOpenInterest(BigDecimal lastOpenInterest) {
        this.lastOpenInterest = lastOpenInterest;
    }

    public BigDecimal getOpenInterestDelta() {
        return openInterestDelta;
    }

    public void setOpenInterestDelta(BigDecimal openInterestDelta) {
        this.openInterestDelta = openInterestDelta;
    }

    public Boolean getOiApplied() {
        return oiApplied;
    }

    public void setOiApplied(Boolean oiApplied) {
        this.oiApplied = oiApplied;
    }

    public String getDerivativesCrowdingState() {
        return derivativesCrowdingState;
    }

    public void setDerivativesCrowdingState(String derivativesCrowdingState) {
        this.derivativesCrowdingState = derivativesCrowdingState;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
