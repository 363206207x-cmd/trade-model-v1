package org.example.trademodel.opportunitylog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpportunityLogStatsDTO {
    private int totalCount;
    private int resolvedCount;
    private int pendingCount;
    private int executedValidCount;
    private int executedInvalidCount;
    private int missedValidCount;
    private int missedInvalidCount;
    private int pushedNotFilledValidCount;
    private int blockedByRiskValidCount;
    private int targetFirstCount;
    private int invalidationFirstCount;
    private int ambiguousCount;
    private BigDecimal averageMfeRatio = BigDecimal.ZERO;
    private BigDecimal averageMaeRatio = BigDecimal.ZERO;
    private BigDecimal maxMfeRatio = BigDecimal.ZERO;
    private BigDecimal maxMaeRatio = BigDecimal.ZERO;
    private int validOpportunityCount;
    private int invalidOpportunityCount;
    private BigDecimal validRate = BigDecimal.ZERO;
    private Map<String, Integer> statusCounts = new LinkedHashMap<>();
    private Map<String, Integer> sourceCounts = new LinkedHashMap<>();
    private LocalDateTime generatedAt;
    private final Boolean reviewOnly = true;
    private final Boolean manualReviewOnly = true;
    private final Boolean notTradeInstruction = true;
    private final Boolean notExecutable = true;
    private final Boolean notAutoTrading = true;
    private final Boolean notOrderExecution = true;
    private final Boolean notUserPositionCreation = true;
    private final Boolean notPushSend = true;
    private final Boolean notExternalChannel = true;

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(int resolvedCount) { this.resolvedCount = resolvedCount; }
    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
    public int getExecutedValidCount() { return executedValidCount; }
    public void setExecutedValidCount(int executedValidCount) { this.executedValidCount = executedValidCount; }
    public int getExecutedInvalidCount() { return executedInvalidCount; }
    public void setExecutedInvalidCount(int executedInvalidCount) { this.executedInvalidCount = executedInvalidCount; }
    public int getMissedValidCount() { return missedValidCount; }
    public void setMissedValidCount(int missedValidCount) { this.missedValidCount = missedValidCount; }
    public int getMissedInvalidCount() { return missedInvalidCount; }
    public void setMissedInvalidCount(int missedInvalidCount) { this.missedInvalidCount = missedInvalidCount; }
    public int getPushedNotFilledValidCount() { return pushedNotFilledValidCount; }
    public void setPushedNotFilledValidCount(int pushedNotFilledValidCount) { this.pushedNotFilledValidCount = pushedNotFilledValidCount; }
    public int getBlockedByRiskValidCount() { return blockedByRiskValidCount; }
    public void setBlockedByRiskValidCount(int blockedByRiskValidCount) { this.blockedByRiskValidCount = blockedByRiskValidCount; }
    public int getTargetFirstCount() { return targetFirstCount; }
    public void setTargetFirstCount(int targetFirstCount) { this.targetFirstCount = targetFirstCount; }
    public int getInvalidationFirstCount() { return invalidationFirstCount; }
    public void setInvalidationFirstCount(int invalidationFirstCount) { this.invalidationFirstCount = invalidationFirstCount; }
    public int getAmbiguousCount() { return ambiguousCount; }
    public void setAmbiguousCount(int ambiguousCount) { this.ambiguousCount = ambiguousCount; }
    public BigDecimal getAverageMfeRatio() { return averageMfeRatio; }
    public void setAverageMfeRatio(BigDecimal averageMfeRatio) { this.averageMfeRatio = averageMfeRatio; }
    public BigDecimal getAverageMaeRatio() { return averageMaeRatio; }
    public void setAverageMaeRatio(BigDecimal averageMaeRatio) { this.averageMaeRatio = averageMaeRatio; }
    public BigDecimal getMaxMfeRatio() { return maxMfeRatio; }
    public void setMaxMfeRatio(BigDecimal maxMfeRatio) { this.maxMfeRatio = maxMfeRatio; }
    public BigDecimal getMaxMaeRatio() { return maxMaeRatio; }
    public void setMaxMaeRatio(BigDecimal maxMaeRatio) { this.maxMaeRatio = maxMaeRatio; }
    public int getValidOpportunityCount() { return validOpportunityCount; }
    public void setValidOpportunityCount(int validOpportunityCount) { this.validOpportunityCount = validOpportunityCount; }
    public int getInvalidOpportunityCount() { return invalidOpportunityCount; }
    public void setInvalidOpportunityCount(int invalidOpportunityCount) { this.invalidOpportunityCount = invalidOpportunityCount; }
    public BigDecimal getValidRate() { return validRate; }
    public void setValidRate(BigDecimal validRate) { this.validRate = validRate; }
    public Map<String, Integer> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Integer> statusCounts) { this.statusCounts = statusCounts; }
    public Map<String, Integer> getSourceCounts() { return sourceCounts; }
    public void setSourceCounts(Map<String, Integer> sourceCounts) { this.sourceCounts = sourceCounts; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public Boolean getReviewOnly() { return reviewOnly; }
    public Boolean getManualReviewOnly() { return manualReviewOnly; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public Boolean getNotExecutable() { return notExecutable; }
    public Boolean getNotAutoTrading() { return notAutoTrading; }
    public Boolean getNotOrderExecution() { return notOrderExecution; }
    public Boolean getNotUserPositionCreation() { return notUserPositionCreation; }
    public Boolean getNotPushSend() { return notPushSend; }
    public Boolean getNotExternalChannel() { return notExternalChannel; }
}
