package org.example.trademodel.vo;

import java.math.BigDecimal;

public class MarketEnvironmentVO {
    private String environmentType;
    private String riskMode;
    private Double trendFriendliness;
    private String leverageSuggestion;
    private String summary;
    /**
     * External 24h ticker {@code priceChangePercent}（与 {@link org.example.trademodel.market.dto.MarketQuoteSnapshot} 同源，可为 null）；
     * 仅由 {@link org.example.trademodel.market.RealMarketEnvironmentService} 从报价写入，不解析 {@link #summary} 反推。
     */
    private BigDecimal priceChangePercent24h;
    /** Runtime provider source marker, e.g. BINANCE_24H_HEURISTIC or OKX_24H_FALLBACK. */
    private String sourceType;
    /** 24h (high-low)/last * 100; set only when computable from quote (second dimension). */
    private Double rangePct24h;
    /** 窄幅 / 中等波动 / 高波动（与 RealMarketEnvironmentService.describeVolatilityRegime 同源分档）。 */
    private String volatilityRegime;
    /** true when USDⓈ-M {@code lastFundingRate} was merged into summary（runtime；不落 schema 额外列）。 */
    private Boolean perpFundingApplied;
    /**
     * USDⓈ-M {@code lastFundingRate}（小数，与 API 一致），与 {@link org.example.trademodel.market.RealMarketEnvironmentService#buildFundingAppendix} 同源；
     * 仅当 Funding 成功合并时由该服务写入。
     */
    private BigDecimal lastFundingRate;
    /** true when USDⓈ-M {@code openInterest} was merged into summary（runtime；不落 schema）。 */
    private Boolean oiApplied;
    /**
     * USDⓈ-M {@code openInterest}（与 Binance {@code /fapi/v1/openInterest} 同源），仅当 OI 成功合并时写入。
     */
    private BigDecimal lastOpenInterest;
    /**
     * OI 变化维度第一刀：同 symbol+timeframe 最近前一 snapshot 的 OI 差值（current - previous）。
     * 仅在 OI 当前已应用且前值可得时写入；否则为 null。
     */
    private BigDecimal openInterestDelta;
    /**
     * OI/Funding 联合派生的最小离散标签：NEUTRAL / CROWDED_LONG / CROWDED_SHORT。
     */
    private String derivativesCrowdingState;

    public void setMacroEnvironment(int value) { this.trendFriendliness = (double) value; }

    public String getEnvironmentType() { return environmentType; }
    public void setEnvironmentType(String environmentType) { this.environmentType = environmentType; }
    public String getRiskMode() { return riskMode; }
    public void setRiskMode(String riskMode) { this.riskMode = riskMode; }
    public Double getTrendFriendliness() { return trendFriendliness; }
    public void setTrendFriendliness(Double trendFriendliness) { this.trendFriendliness = trendFriendliness; }
    public String getLeverageSuggestion() { return leverageSuggestion; }
    public void setLeverageSuggestion(String leverageSuggestion) { this.leverageSuggestion = leverageSuggestion; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public BigDecimal getPriceChangePercent24h() { return priceChangePercent24h; }
    public void setPriceChangePercent24h(BigDecimal priceChangePercent24h) {
        this.priceChangePercent24h = priceChangePercent24h;
    }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Double getRangePct24h() { return rangePct24h; }
    public void setRangePct24h(Double rangePct24h) { this.rangePct24h = rangePct24h; }
    public String getVolatilityRegime() { return volatilityRegime; }
    public void setVolatilityRegime(String volatilityRegime) { this.volatilityRegime = volatilityRegime; }
    public Boolean getPerpFundingApplied() { return perpFundingApplied; }
    public void setPerpFundingApplied(Boolean perpFundingApplied) { this.perpFundingApplied = perpFundingApplied; }
    public BigDecimal getLastFundingRate() { return lastFundingRate; }
    public void setLastFundingRate(BigDecimal lastFundingRate) { this.lastFundingRate = lastFundingRate; }
    public Boolean getOiApplied() { return oiApplied; }
    public void setOiApplied(Boolean oiApplied) { this.oiApplied = oiApplied; }
    public BigDecimal getLastOpenInterest() { return lastOpenInterest; }
    public void setLastOpenInterest(BigDecimal lastOpenInterest) { this.lastOpenInterest = lastOpenInterest; }
    public BigDecimal getOpenInterestDelta() { return openInterestDelta; }
    public void setOpenInterestDelta(BigDecimal openInterestDelta) { this.openInterestDelta = openInterestDelta; }
    public String getDerivativesCrowdingState() { return derivativesCrowdingState; }
    public void setDerivativesCrowdingState(String derivativesCrowdingState) {
        this.derivativesCrowdingState = derivativesCrowdingState;
    }
}
