package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SourceTraceDTO implements SourceCompletenessContract {

    private String symbol;
    private String symbolSource;
    private String decisionId;
    private String decisionIdSource;
    private String analysisId;
    private String analysisIdSource;
    private LocalDateTime decisionCreateTime;
    private String decisionCreateTimeSource;
    private String timeframe;
    private String timeframeSource;
    private String sourceOwner;
    private String sourceRef;
    private String sourceTimeframe;
    private String sourceWindow;
    private String freshnessStatus;
    private String runtimeKlineContextStatus;
    private String runtimeKlineContextSource;
    private String runtimeKlineReadinessStatus;
    private String runtimeKlineStaleReasonCode;
    private String runtimeKlineStaleReasonText;
    private List<String> runtimeKlineReadinessMissingFields = new ArrayList<>();
    private BigDecimal quoteLatestPrice;
    private String quoteLatestPriceSource;
    private Long quotePriceUpdateTimeMs;
    private String quotePriceUpdateTimeSource;
    private String quoteFreshnessStatus;
    private BigDecimal dataQualityScore;
    private String dataQualityScoreSource;
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
    private List<String> blockingReasons = new ArrayList<>();
    private String reviewMode = SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name();
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbolSource() {
        return symbolSource;
    }

    public void setSymbolSource(String symbolSource) {
        this.symbolSource = symbolSource;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getDecisionIdSource() {
        return decisionIdSource;
    }

    public void setDecisionIdSource(String decisionIdSource) {
        this.decisionIdSource = decisionIdSource;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisIdSource() {
        return analysisIdSource;
    }

    public void setAnalysisIdSource(String analysisIdSource) {
        this.analysisIdSource = analysisIdSource;
    }

    public LocalDateTime getDecisionCreateTime() {
        return decisionCreateTime;
    }

    public void setDecisionCreateTime(LocalDateTime decisionCreateTime) {
        this.decisionCreateTime = decisionCreateTime;
    }

    public String getDecisionCreateTimeSource() {
        return decisionCreateTimeSource;
    }

    public void setDecisionCreateTimeSource(String decisionCreateTimeSource) {
        this.decisionCreateTimeSource = decisionCreateTimeSource;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getTimeframeSource() {
        return timeframeSource;
    }

    public void setTimeframeSource(String timeframeSource) {
        this.timeframeSource = timeframeSource;
    }

    public String getSourceOwner() {
        return sourceOwner;
    }

    public void setSourceOwner(String sourceOwner) {
        this.sourceOwner = sourceOwner;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getSourceTimeframe() {
        return sourceTimeframe;
    }

    public void setSourceTimeframe(String sourceTimeframe) {
        this.sourceTimeframe = sourceTimeframe;
    }

    public String getSourceWindow() {
        return sourceWindow;
    }

    public void setSourceWindow(String sourceWindow) {
        this.sourceWindow = sourceWindow;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getRuntimeKlineContextStatus() {
        return runtimeKlineContextStatus;
    }

    public void setRuntimeKlineContextStatus(String runtimeKlineContextStatus) {
        this.runtimeKlineContextStatus = runtimeKlineContextStatus;
    }

    public String getRuntimeKlineContextSource() {
        return runtimeKlineContextSource;
    }

    public void setRuntimeKlineContextSource(String runtimeKlineContextSource) {
        this.runtimeKlineContextSource = runtimeKlineContextSource;
    }

    public String getRuntimeKlineReadinessStatus() {
        return runtimeKlineReadinessStatus;
    }

    public void setRuntimeKlineReadinessStatus(String runtimeKlineReadinessStatus) {
        this.runtimeKlineReadinessStatus = runtimeKlineReadinessStatus;
    }

    public String getRuntimeKlineStaleReasonCode() {
        return runtimeKlineStaleReasonCode;
    }

    public void setRuntimeKlineStaleReasonCode(String runtimeKlineStaleReasonCode) {
        this.runtimeKlineStaleReasonCode = runtimeKlineStaleReasonCode;
    }

    public String getRuntimeKlineStaleReasonText() {
        return runtimeKlineStaleReasonText;
    }

    public void setRuntimeKlineStaleReasonText(String runtimeKlineStaleReasonText) {
        this.runtimeKlineStaleReasonText = runtimeKlineStaleReasonText;
    }

    public List<String> getRuntimeKlineReadinessMissingFields() {
        return runtimeKlineReadinessMissingFields;
    }

    public void setRuntimeKlineReadinessMissingFields(List<String> runtimeKlineReadinessMissingFields) {
        this.runtimeKlineReadinessMissingFields = runtimeKlineReadinessMissingFields == null
                ? new ArrayList<>()
                : new ArrayList<>(runtimeKlineReadinessMissingFields);
    }

    public BigDecimal getQuoteLatestPrice() {
        return quoteLatestPrice;
    }

    public void setQuoteLatestPrice(BigDecimal quoteLatestPrice) {
        this.quoteLatestPrice = quoteLatestPrice;
    }

    public String getQuoteLatestPriceSource() {
        return quoteLatestPriceSource;
    }

    public void setQuoteLatestPriceSource(String quoteLatestPriceSource) {
        this.quoteLatestPriceSource = quoteLatestPriceSource;
    }

    public Long getQuotePriceUpdateTimeMs() {
        return quotePriceUpdateTimeMs;
    }

    public void setQuotePriceUpdateTimeMs(Long quotePriceUpdateTimeMs) {
        this.quotePriceUpdateTimeMs = quotePriceUpdateTimeMs;
    }

    public String getQuotePriceUpdateTimeSource() {
        return quotePriceUpdateTimeSource;
    }

    public void setQuotePriceUpdateTimeSource(String quotePriceUpdateTimeSource) {
        this.quotePriceUpdateTimeSource = quotePriceUpdateTimeSource;
    }

    public String getQuoteFreshnessStatus() {
        return quoteFreshnessStatus;
    }

    public void setQuoteFreshnessStatus(String quoteFreshnessStatus) {
        this.quoteFreshnessStatus = quoteFreshnessStatus;
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

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons == null ? new ArrayList<>() : new ArrayList<>(blockingReasons);
    }

    public String getReviewMode() {
        return reviewMode;
    }

    public void setReviewMode(String reviewMode) {
        this.reviewMode = reviewMode;
    }

    public boolean hasRequiredBoundarySources() {
        return ExecutionPlanSourceGate.validate(this).isValid();
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
