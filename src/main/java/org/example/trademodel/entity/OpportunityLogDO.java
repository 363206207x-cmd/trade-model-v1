package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpportunityLogDO {
    private String opportunityId;
    private String opportunityKey;
    private String analysisId;
    private String decisionId;
    private String executionPlanId;
    private Long pushId;
    private Long userPositionId;
    private String symbol;
    private String timeframe;
    private String direction;
    private String lifecycleStatus;
    private String opportunityStatus;
    private LocalDateTime anchorTime;
    private LocalDateTime evaluationAsOf;
    private LocalDateTime resolvedAt;
    private BigDecimal entryReference;
    private BigDecimal targetPrice;
    private BigDecimal invalidationPrice;
    private Boolean targetHit;
    private Boolean invalidationHit;
    private LocalDateTime targetHitAt;
    private LocalDateTime invalidationHitAt;
    private String hitOrder;
    private BigDecimal mfePrice;
    private BigDecimal mfeRatio;
    private BigDecimal maePrice;
    private BigDecimal maeRatio;
    private Boolean pushPresent;
    private Boolean riskBlockedEvidence;
    private LocalDateTime riskBlockedAt;
    private Boolean userPositionPresent;
    private String sourceType;
    private String sourceReference;
    private String marketDataSource;
    private String marketDataTraceId;
    private String reasonCodes;
    private String traceId;
    private Boolean reviewOnly = true;
    private Boolean manualReviewOnly = true;
    private Boolean notTradeInstruction = true;
    private Boolean notExecutable = true;
    private Boolean notAutoTrading = true;
    private Boolean notOrderExecution = true;
    private Boolean notUserPositionCreation = true;
    private Boolean notUserPositionMutation = true;
    private Boolean notPushSend = true;
    private Boolean notExternalChannel = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String opportunityId) { this.opportunityId = opportunityId; }
    public String getOpportunityKey() { return opportunityKey; }
    public void setOpportunityKey(String opportunityKey) { this.opportunityKey = opportunityKey; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }
    public String getExecutionPlanId() { return executionPlanId; }
    public void setExecutionPlanId(String executionPlanId) { this.executionPlanId = executionPlanId; }
    public Long getPushId() { return pushId; }
    public void setPushId(Long pushId) { this.pushId = pushId; }
    public Long getUserPositionId() { return userPositionId; }
    public void setUserPositionId(Long userPositionId) { this.userPositionId = userPositionId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    public String getOpportunityStatus() { return opportunityStatus; }
    public void setOpportunityStatus(String opportunityStatus) { this.opportunityStatus = opportunityStatus; }
    public LocalDateTime getAnchorTime() { return anchorTime; }
    public void setAnchorTime(LocalDateTime anchorTime) { this.anchorTime = anchorTime; }
    public LocalDateTime getEvaluationAsOf() { return evaluationAsOf; }
    public void setEvaluationAsOf(LocalDateTime evaluationAsOf) { this.evaluationAsOf = evaluationAsOf; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public BigDecimal getEntryReference() { return entryReference; }
    public void setEntryReference(BigDecimal entryReference) { this.entryReference = entryReference; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
    public BigDecimal getInvalidationPrice() { return invalidationPrice; }
    public void setInvalidationPrice(BigDecimal invalidationPrice) { this.invalidationPrice = invalidationPrice; }
    public Boolean getTargetHit() { return targetHit; }
    public void setTargetHit(Boolean targetHit) { this.targetHit = targetHit; }
    public Boolean getInvalidationHit() { return invalidationHit; }
    public void setInvalidationHit(Boolean invalidationHit) { this.invalidationHit = invalidationHit; }
    public LocalDateTime getTargetHitAt() { return targetHitAt; }
    public void setTargetHitAt(LocalDateTime targetHitAt) { this.targetHitAt = targetHitAt; }
    public LocalDateTime getInvalidationHitAt() { return invalidationHitAt; }
    public void setInvalidationHitAt(LocalDateTime invalidationHitAt) { this.invalidationHitAt = invalidationHitAt; }
    public String getHitOrder() { return hitOrder; }
    public void setHitOrder(String hitOrder) { this.hitOrder = hitOrder; }
    public BigDecimal getMfePrice() { return mfePrice; }
    public void setMfePrice(BigDecimal mfePrice) { this.mfePrice = mfePrice; }
    public BigDecimal getMfeRatio() { return mfeRatio; }
    public void setMfeRatio(BigDecimal mfeRatio) { this.mfeRatio = mfeRatio; }
    public BigDecimal getMaePrice() { return maePrice; }
    public void setMaePrice(BigDecimal maePrice) { this.maePrice = maePrice; }
    public BigDecimal getMaeRatio() { return maeRatio; }
    public void setMaeRatio(BigDecimal maeRatio) { this.maeRatio = maeRatio; }
    public Boolean getPushPresent() { return pushPresent; }
    public void setPushPresent(Boolean pushPresent) { this.pushPresent = pushPresent; }
    public Boolean getRiskBlockedEvidence() { return riskBlockedEvidence; }
    public void setRiskBlockedEvidence(Boolean riskBlockedEvidence) { this.riskBlockedEvidence = riskBlockedEvidence; }
    public LocalDateTime getRiskBlockedAt() { return riskBlockedAt; }
    public void setRiskBlockedAt(LocalDateTime riskBlockedAt) { this.riskBlockedAt = riskBlockedAt; }
    public Boolean getUserPositionPresent() { return userPositionPresent; }
    public void setUserPositionPresent(Boolean userPositionPresent) { this.userPositionPresent = userPositionPresent; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public String getMarketDataSource() { return marketDataSource; }
    public void setMarketDataSource(String marketDataSource) { this.marketDataSource = marketDataSource; }
    public String getMarketDataTraceId() { return marketDataTraceId; }
    public void setMarketDataTraceId(String marketDataTraceId) { this.marketDataTraceId = marketDataTraceId; }
    public String getReasonCodes() { return reasonCodes; }
    public void setReasonCodes(String reasonCodes) { this.reasonCodes = reasonCodes; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Boolean getReviewOnly() { return true; }
    public void setReviewOnly(Boolean reviewOnly) { this.reviewOnly = true; }
    public Boolean getManualReviewOnly() { return true; }
    public void setManualReviewOnly(Boolean manualReviewOnly) { this.manualReviewOnly = true; }
    public Boolean getNotTradeInstruction() { return true; }
    public void setNotTradeInstruction(Boolean notTradeInstruction) { this.notTradeInstruction = true; }
    public Boolean getNotExecutable() { return true; }
    public void setNotExecutable(Boolean notExecutable) { this.notExecutable = true; }
    public Boolean getNotAutoTrading() { return true; }
    public void setNotAutoTrading(Boolean notAutoTrading) { this.notAutoTrading = true; }
    public Boolean getNotOrderExecution() { return true; }
    public void setNotOrderExecution(Boolean notOrderExecution) { this.notOrderExecution = true; }
    public Boolean getNotUserPositionCreation() { return true; }
    public void setNotUserPositionCreation(Boolean notUserPositionCreation) { this.notUserPositionCreation = true; }
    public Boolean getNotUserPositionMutation() { return true; }
    public void setNotUserPositionMutation(Boolean notUserPositionMutation) { this.notUserPositionMutation = true; }
    public Boolean getNotPushSend() { return true; }
    public void setNotPushSend(Boolean notPushSend) { this.notPushSend = true; }
    public Boolean getNotExternalChannel() { return true; }
    public void setNotExternalChannel(Boolean notExternalChannel) { this.notExternalChannel = true; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
