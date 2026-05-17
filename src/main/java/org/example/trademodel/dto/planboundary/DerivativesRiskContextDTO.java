package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DerivativesRiskContextDTO implements SourceCompletenessContract {

    private String symbol;
    private String timeframe;
    private LocalDateTime contextTime;
    private List<BigDecimal> openInterestHistory = new ArrayList<>();
    private BigDecimal openInterestDelta;
    private BigDecimal lastFundingRate;
    private List<BigDecimal> fundingHistory = new ArrayList<>();
    private List<BigDecimal> liquidationCluster = new ArrayList<>();
    private Map<String, BigDecimal> leverageDistribution = new LinkedHashMap<>();
    private BigDecimal longShortRatio;
    private String liquidityStress;
    private String liquidityStressReason;
    private List<String> eventWindowBlockers = new ArrayList<>();
    private List<String> wickConfirmationSources = new ArrayList<>();
    private BigDecimal dataQualityScore;
    private String dataQualityScoreSource;
    private SourceTraceFallbackStatusEnum fallbackStatus;
    private List<String> missingFields = new ArrayList<>();
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;

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

    public LocalDateTime getContextTime() {
        return contextTime;
    }

    public void setContextTime(LocalDateTime contextTime) {
        this.contextTime = contextTime;
    }

    public List<BigDecimal> getOpenInterestHistory() {
        return openInterestHistory;
    }

    public void setOpenInterestHistory(List<BigDecimal> openInterestHistory) {
        this.openInterestHistory = openInterestHistory == null
                ? new ArrayList<>()
                : new ArrayList<>(openInterestHistory);
    }

    public BigDecimal getOpenInterestDelta() {
        return openInterestDelta;
    }

    public void setOpenInterestDelta(BigDecimal openInterestDelta) {
        this.openInterestDelta = openInterestDelta;
    }

    public BigDecimal getLastFundingRate() {
        return lastFundingRate;
    }

    public void setLastFundingRate(BigDecimal lastFundingRate) {
        this.lastFundingRate = lastFundingRate;
    }

    public List<BigDecimal> getFundingHistory() {
        return fundingHistory;
    }

    public void setFundingHistory(List<BigDecimal> fundingHistory) {
        this.fundingHistory = fundingHistory == null ? new ArrayList<>() : new ArrayList<>(fundingHistory);
    }

    public List<BigDecimal> getLiquidationCluster() {
        return liquidationCluster;
    }

    public void setLiquidationCluster(List<BigDecimal> liquidationCluster) {
        this.liquidationCluster = liquidationCluster == null
                ? new ArrayList<>()
                : new ArrayList<>(liquidationCluster);
    }

    public Map<String, BigDecimal> getLeverageDistribution() {
        return leverageDistribution;
    }

    public void setLeverageDistribution(Map<String, BigDecimal> leverageDistribution) {
        this.leverageDistribution = leverageDistribution == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(leverageDistribution);
    }

    public BigDecimal getLongShortRatio() {
        return longShortRatio;
    }

    public void setLongShortRatio(BigDecimal longShortRatio) {
        this.longShortRatio = longShortRatio;
    }

    public String getLiquidityStress() {
        return liquidityStress;
    }

    public void setLiquidityStress(String liquidityStress) {
        this.liquidityStress = liquidityStress;
    }

    public String getLiquidityStressReason() {
        return liquidityStressReason;
    }

    public void setLiquidityStressReason(String liquidityStressReason) {
        this.liquidityStressReason = liquidityStressReason;
    }

    public List<String> getEventWindowBlockers() {
        return eventWindowBlockers;
    }

    public void setEventWindowBlockers(List<String> eventWindowBlockers) {
        this.eventWindowBlockers = eventWindowBlockers == null
                ? new ArrayList<>()
                : new ArrayList<>(eventWindowBlockers);
    }

    public List<String> getWickConfirmationSources() {
        return wickConfirmationSources;
    }

    public void setWickConfirmationSources(List<String> wickConfirmationSources) {
        this.wickConfirmationSources = wickConfirmationSources == null
                ? new ArrayList<>()
                : new ArrayList<>(wickConfirmationSources);
    }

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(BigDecimal dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public String getDataQualityScoreSource() {
        return dataQualityScoreSource;
    }

    public void setDataQualityScoreSource(String dataQualityScoreSource) {
        this.dataQualityScoreSource = dataQualityScoreSource;
    }

    @Override
    public SourceTraceFallbackStatusEnum getFallbackStatus() {
        return fallbackStatus;
    }

    public void setFallbackStatus(SourceTraceFallbackStatusEnum fallbackStatus) {
        this.fallbackStatus = fallbackStatus;
    }

    @Override
    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields == null ? new ArrayList<>() : new ArrayList<>(missingFields);
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
    }
}
