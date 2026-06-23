package org.example.trademodel.opportunitylog;

import org.example.trademodel.entity.OpportunityLogDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpportunityLogDTO {
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
    private Boolean deduplicated = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Boolean reviewOnly = true;
    private final Boolean manualReviewOnly = true;
    private final Boolean notTradeInstruction = true;
    private final Boolean notExecutable = true;
    private final Boolean notAutoTrading = true;
    private final Boolean notOrderExecution = true;
    private final Boolean notUserPositionCreation = true;
    private final Boolean notUserPositionMutation = true;
    private final Boolean notPushSend = true;
    private final Boolean notExternalChannel = true;

    public static OpportunityLogDTO from(OpportunityLogDO row) {
        OpportunityLogDTO dto = new OpportunityLogDTO();
        if (row == null) {
            return dto;
        }
        dto.setOpportunityId(row.getOpportunityId());
        dto.setOpportunityKey(row.getOpportunityKey());
        dto.setAnalysisId(row.getAnalysisId());
        dto.setDecisionId(row.getDecisionId());
        dto.setExecutionPlanId(row.getExecutionPlanId());
        dto.setPushId(row.getPushId());
        dto.setUserPositionId(row.getUserPositionId());
        dto.setSymbol(row.getSymbol());
        dto.setTimeframe(row.getTimeframe());
        dto.setDirection(row.getDirection());
        dto.setLifecycleStatus(row.getLifecycleStatus());
        dto.setOpportunityStatus(row.getOpportunityStatus());
        dto.setAnchorTime(row.getAnchorTime());
        dto.setEvaluationAsOf(row.getEvaluationAsOf());
        dto.setResolvedAt(row.getResolvedAt());
        dto.setEntryReference(row.getEntryReference());
        dto.setTargetPrice(row.getTargetPrice());
        dto.setInvalidationPrice(row.getInvalidationPrice());
        dto.setTargetHit(row.getTargetHit());
        dto.setInvalidationHit(row.getInvalidationHit());
        dto.setTargetHitAt(row.getTargetHitAt());
        dto.setInvalidationHitAt(row.getInvalidationHitAt());
        dto.setHitOrder(row.getHitOrder());
        dto.setMfePrice(row.getMfePrice());
        dto.setMfeRatio(row.getMfeRatio());
        dto.setMaePrice(row.getMaePrice());
        dto.setMaeRatio(row.getMaeRatio());
        dto.setPushPresent(row.getPushPresent());
        dto.setRiskBlockedEvidence(row.getRiskBlockedEvidence());
        dto.setRiskBlockedAt(row.getRiskBlockedAt());
        dto.setUserPositionPresent(row.getUserPositionPresent());
        dto.setSourceType(row.getSourceType());
        dto.setSourceReference(row.getSourceReference());
        dto.setMarketDataSource(row.getMarketDataSource());
        dto.setMarketDataTraceId(row.getMarketDataTraceId());
        dto.setReasonCodes(row.getReasonCodes());
        dto.setTraceId(row.getTraceId());
        dto.setCreatedAt(row.getCreatedAt());
        dto.setUpdatedAt(row.getUpdatedAt());
        return dto;
    }

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
    public Boolean getDeduplicated() { return deduplicated; }
    public void setDeduplicated(Boolean deduplicated) { this.deduplicated = deduplicated; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getReviewOnly() { return reviewOnly; }
    public Boolean getManualReviewOnly() { return manualReviewOnly; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public Boolean getNotExecutable() { return notExecutable; }
    public Boolean getNotAutoTrading() { return notAutoTrading; }
    public Boolean getNotOrderExecution() { return notOrderExecution; }
    public Boolean getNotUserPositionCreation() { return notUserPositionCreation; }
    public Boolean getNotUserPositionMutation() { return notUserPositionMutation; }
    public Boolean getNotPushSend() { return notPushSend; }
    public Boolean getNotExternalChannel() { return notExternalChannel; }
}
