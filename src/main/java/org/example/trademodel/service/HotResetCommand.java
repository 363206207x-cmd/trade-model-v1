package org.example.trademodel.service;

import org.example.trademodel.enums.HotResetEventTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HotResetCommand {

    private String eventKey;
    private String analysisId;
    private String traceId;
    private String ownerType;
    private Long ownerId;
    private Long assetId;
    private String ruleVersion;
    private String symbol;
    private String timeframe;
    private HotResetEventTypeEnum eventType;
    /** UTC-naive event time when supplied by a caller; null delegates to the service UTC clock. */
    private LocalDateTime occurredAt;
    private String sourceType;
    private String sourceReference;
    private BigDecimal currentPrice;
    private BigDecimal referencePrice;
    private BigDecimal priceMoveRatio;
    private BigDecimal currentOpenInterest;
    private BigDecimal previousOpenInterest;
    private BigDecimal openInterestChangeRatio;
    private BigDecimal currentLiquidity;
    private BigDecimal baselineLiquidity;
    private BigDecimal liquidityChangeRatio;
    private Boolean systemicShock;
    private Integer severityScore;
    private DecisionContext decisionContext;

    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public HotResetEventTypeEnum getEventType() { return eventType; }
    public void setEventType(HotResetEventTypeEnum eventType) { this.eventType = eventType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getReferencePrice() { return referencePrice; }
    public void setReferencePrice(BigDecimal referencePrice) { this.referencePrice = referencePrice; }
    public BigDecimal getPriceMoveRatio() { return priceMoveRatio; }
    public void setPriceMoveRatio(BigDecimal priceMoveRatio) { this.priceMoveRatio = priceMoveRatio; }
    public BigDecimal getCurrentOpenInterest() { return currentOpenInterest; }
    public void setCurrentOpenInterest(BigDecimal currentOpenInterest) { this.currentOpenInterest = currentOpenInterest; }
    public BigDecimal getPreviousOpenInterest() { return previousOpenInterest; }
    public void setPreviousOpenInterest(BigDecimal previousOpenInterest) { this.previousOpenInterest = previousOpenInterest; }
    public BigDecimal getOpenInterestChangeRatio() { return openInterestChangeRatio; }
    public void setOpenInterestChangeRatio(BigDecimal openInterestChangeRatio) { this.openInterestChangeRatio = openInterestChangeRatio; }
    public BigDecimal getCurrentLiquidity() { return currentLiquidity; }
    public void setCurrentLiquidity(BigDecimal currentLiquidity) { this.currentLiquidity = currentLiquidity; }
    public BigDecimal getBaselineLiquidity() { return baselineLiquidity; }
    public void setBaselineLiquidity(BigDecimal baselineLiquidity) { this.baselineLiquidity = baselineLiquidity; }
    public BigDecimal getLiquidityChangeRatio() { return liquidityChangeRatio; }
    public void setLiquidityChangeRatio(BigDecimal liquidityChangeRatio) { this.liquidityChangeRatio = liquidityChangeRatio; }
    public Boolean getSystemicShock() { return systemicShock; }
    public void setSystemicShock(Boolean systemicShock) { this.systemicShock = systemicShock; }
    public Integer getSeverityScore() { return severityScore; }
    public void setSeverityScore(Integer severityScore) { this.severityScore = severityScore; }
    public DecisionContext getDecisionContext() { return decisionContext; }
    public void setDecisionContext(DecisionContext decisionContext) { this.decisionContext = decisionContext; }
}
