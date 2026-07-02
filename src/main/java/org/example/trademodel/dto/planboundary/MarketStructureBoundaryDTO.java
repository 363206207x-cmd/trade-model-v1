package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MarketStructureBoundaryDTO {

    private String symbol;
    private String direction;
    private String timeframe;
    private LocalDateTime generatedAt;
    private BigDecimal entryLower;
    private BigDecimal entryUpper;
    private String entrySourceType;
    private String entrySourceRef;
    private String entryReason;
    private BigDecimal stopPrice;
    private String stopSourceType;
    private String stopSourceRef;
    private String stopReason;
    private List<MarketStructureTakeProfitTargetDTO> takeProfitTargets = new ArrayList<>();
    private BigDecimal rrRatio;
    private List<BoundaryLevelDTO> supportLevels = new ArrayList<>();
    private List<BoundaryLevelDTO> resistanceLevels = new ArrayList<>();
    private BoundaryLevelDTO swingHigh;
    private BoundaryLevelDTO swingLow;
    private String freshnessStatus;
    private String dataQualityStatus;
    private boolean boundaryReady;
    private List<String> blockingReasons = new ArrayList<>();
    private List<BoundarySourceRefDTO> sourceRefs = new ArrayList<>();
    private String leverageSuggestion;
    private String positionSizingStatus;
    private boolean manualReviewRequired = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoTrading = true;
    private boolean notOrderExecution = true;
    private boolean notUserPositionCreation = true;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public BigDecimal getEntryLower() {
        return entryLower;
    }

    public void setEntryLower(BigDecimal entryLower) {
        this.entryLower = entryLower;
    }

    public BigDecimal getEntryUpper() {
        return entryUpper;
    }

    public void setEntryUpper(BigDecimal entryUpper) {
        this.entryUpper = entryUpper;
    }

    public String getEntrySourceType() {
        return entrySourceType;
    }

    public void setEntrySourceType(String entrySourceType) {
        this.entrySourceType = entrySourceType;
    }

    public String getEntrySourceRef() {
        return entrySourceRef;
    }

    public void setEntrySourceRef(String entrySourceRef) {
        this.entrySourceRef = entrySourceRef;
    }

    public String getEntryReason() {
        return entryReason;
    }

    public void setEntryReason(String entryReason) {
        this.entryReason = entryReason;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    public String getStopSourceType() {
        return stopSourceType;
    }

    public void setStopSourceType(String stopSourceType) {
        this.stopSourceType = stopSourceType;
    }

    public String getStopSourceRef() {
        return stopSourceRef;
    }

    public void setStopSourceRef(String stopSourceRef) {
        this.stopSourceRef = stopSourceRef;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public List<MarketStructureTakeProfitTargetDTO> getTakeProfitTargets() {
        return takeProfitTargets;
    }

    public void setTakeProfitTargets(List<MarketStructureTakeProfitTargetDTO> takeProfitTargets) {
        this.takeProfitTargets = takeProfitTargets == null ? new ArrayList<>() : new ArrayList<>(takeProfitTargets);
    }

    public BigDecimal getRrRatio() {
        return rrRatio;
    }

    public void setRrRatio(BigDecimal rrRatio) {
        this.rrRatio = rrRatio;
    }

    public List<BoundaryLevelDTO> getSupportLevels() {
        return supportLevels;
    }

    public void setSupportLevels(List<BoundaryLevelDTO> supportLevels) {
        this.supportLevels = supportLevels == null ? new ArrayList<>() : new ArrayList<>(supportLevels);
    }

    public List<BoundaryLevelDTO> getResistanceLevels() {
        return resistanceLevels;
    }

    public void setResistanceLevels(List<BoundaryLevelDTO> resistanceLevels) {
        this.resistanceLevels = resistanceLevels == null ? new ArrayList<>() : new ArrayList<>(resistanceLevels);
    }

    public BoundaryLevelDTO getSwingHigh() {
        return swingHigh;
    }

    public void setSwingHigh(BoundaryLevelDTO swingHigh) {
        this.swingHigh = swingHigh;
    }

    public BoundaryLevelDTO getSwingLow() {
        return swingLow;
    }

    public void setSwingLow(BoundaryLevelDTO swingLow) {
        this.swingLow = swingLow;
    }

    public String getFreshnessStatus() {
        return freshnessStatus;
    }

    public void setFreshnessStatus(String freshnessStatus) {
        this.freshnessStatus = freshnessStatus;
    }

    public String getDataQualityStatus() {
        return dataQualityStatus;
    }

    public void setDataQualityStatus(String dataQualityStatus) {
        this.dataQualityStatus = dataQualityStatus;
    }

    public boolean isBoundaryReady() {
        return boundaryReady;
    }

    public void setBoundaryReady(boolean boundaryReady) {
        this.boundaryReady = boundaryReady;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons == null ? new ArrayList<>() : new ArrayList<>(blockingReasons);
    }

    public List<BoundarySourceRefDTO> getSourceRefs() {
        return sourceRefs;
    }

    public void setSourceRefs(List<BoundarySourceRefDTO> sourceRefs) {
        this.sourceRefs = sourceRefs == null ? new ArrayList<>() : new ArrayList<>(sourceRefs);
    }

    public String getLeverageSuggestion() {
        return leverageSuggestion;
    }

    public void setLeverageSuggestion(String leverageSuggestion) {
        this.leverageSuggestion = leverageSuggestion;
    }

    public String getPositionSizingStatus() {
        return positionSizingStatus;
    }

    public void setPositionSizingStatus(String positionSizingStatus) {
        this.positionSizingStatus = positionSizingStatus;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isNotExecutable() {
        return notExecutable;
    }

    public boolean isNotAutoTrading() {
        return notAutoTrading;
    }

    public boolean isNotOrderExecution() {
        return notOrderExecution;
    }

    public boolean isNotUserPositionCreation() {
        return notUserPositionCreation;
    }
}
