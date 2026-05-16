package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RuntimeKlineContextDTO implements SourceCompletenessContract {

    private String symbol;
    private String timeframe;
    private BigDecimal latestPrice;
    private BigDecimal dataQualityScore;
    private BigDecimal entryPriceSource;
    private String entrySourceType;
    private String entrySourceTimeframe;
    private String entrySourceReason;
    private String entrySourceRef;
    private BigDecimal stopPriceSource;
    private String stopSourceType;
    private String stopSourceTimeframe;
    private String stopSourceReason;
    private String stopSourceRef;
    private List<BigDecimal> tpPriceSources = new ArrayList<>();
    private String tpSourceType;
    private String tpSourceTimeframe;
    private String tpSourceReason;
    private String tpSourceRef;
    private BigDecimal rrSource;
    private String rrRuleRef;
    private String liquiditySource;
    private String multiTimeframeSource;
    private String eventSource;
    private String wickSource;
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

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }

    public BigDecimal getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(BigDecimal dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public BigDecimal getEntryPriceSource() {
        return entryPriceSource;
    }

    public void setEntryPriceSource(BigDecimal entryPriceSource) {
        this.entryPriceSource = entryPriceSource;
    }

    public String getEntrySourceType() {
        return entrySourceType;
    }

    public void setEntrySourceType(String entrySourceType) {
        this.entrySourceType = entrySourceType;
    }

    public String getEntrySourceTimeframe() {
        return entrySourceTimeframe;
    }

    public void setEntrySourceTimeframe(String entrySourceTimeframe) {
        this.entrySourceTimeframe = entrySourceTimeframe;
    }

    public String getEntrySourceReason() {
        return entrySourceReason;
    }

    public void setEntrySourceReason(String entrySourceReason) {
        this.entrySourceReason = entrySourceReason;
    }

    public String getEntrySourceRef() {
        return entrySourceRef;
    }

    public void setEntrySourceRef(String entrySourceRef) {
        this.entrySourceRef = entrySourceRef;
    }

    public BigDecimal getStopPriceSource() {
        return stopPriceSource;
    }

    public void setStopPriceSource(BigDecimal stopPriceSource) {
        this.stopPriceSource = stopPriceSource;
    }

    public String getStopSourceType() {
        return stopSourceType;
    }

    public void setStopSourceType(String stopSourceType) {
        this.stopSourceType = stopSourceType;
    }

    public String getStopSourceTimeframe() {
        return stopSourceTimeframe;
    }

    public void setStopSourceTimeframe(String stopSourceTimeframe) {
        this.stopSourceTimeframe = stopSourceTimeframe;
    }

    public String getStopSourceReason() {
        return stopSourceReason;
    }

    public void setStopSourceReason(String stopSourceReason) {
        this.stopSourceReason = stopSourceReason;
    }

    public String getStopSourceRef() {
        return stopSourceRef;
    }

    public void setStopSourceRef(String stopSourceRef) {
        this.stopSourceRef = stopSourceRef;
    }

    public List<BigDecimal> getTpPriceSources() {
        return tpPriceSources;
    }

    public void setTpPriceSources(List<BigDecimal> tpPriceSources) {
        this.tpPriceSources = tpPriceSources == null ? new ArrayList<>() : new ArrayList<>(tpPriceSources);
    }

    public String getTpSourceType() {
        return tpSourceType;
    }

    public void setTpSourceType(String tpSourceType) {
        this.tpSourceType = tpSourceType;
    }

    public String getTpSourceTimeframe() {
        return tpSourceTimeframe;
    }

    public void setTpSourceTimeframe(String tpSourceTimeframe) {
        this.tpSourceTimeframe = tpSourceTimeframe;
    }

    public String getTpSourceReason() {
        return tpSourceReason;
    }

    public void setTpSourceReason(String tpSourceReason) {
        this.tpSourceReason = tpSourceReason;
    }

    public String getTpSourceRef() {
        return tpSourceRef;
    }

    public void setTpSourceRef(String tpSourceRef) {
        this.tpSourceRef = tpSourceRef;
    }

    public BigDecimal getRrSource() {
        return rrSource;
    }

    public void setRrSource(BigDecimal rrSource) {
        this.rrSource = rrSource;
    }

    public String getRrRuleRef() {
        return rrRuleRef;
    }

    public void setRrRuleRef(String rrRuleRef) {
        this.rrRuleRef = rrRuleRef;
    }

    public String getLiquiditySource() {
        return liquiditySource;
    }

    public void setLiquiditySource(String liquiditySource) {
        this.liquiditySource = liquiditySource;
    }

    public String getMultiTimeframeSource() {
        return multiTimeframeSource;
    }

    public void setMultiTimeframeSource(String multiTimeframeSource) {
        this.multiTimeframeSource = multiTimeframeSource;
    }

    public String getEventSource() {
        return eventSource;
    }

    public void setEventSource(String eventSource) {
        this.eventSource = eventSource;
    }

    public String getWickSource() {
        return wickSource;
    }

    public void setWickSource(String wickSource) {
        this.wickSource = wickSource;
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
