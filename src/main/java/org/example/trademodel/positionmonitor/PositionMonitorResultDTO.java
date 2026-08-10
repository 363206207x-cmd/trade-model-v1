package org.example.trademodel.positionmonitor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PositionMonitorResultDTO {
    private Long positionId;
    private String assetSymbol;
    private String side;
    private String positionStatus;
    private String analysisId;
    private String executionPlanId;
    private BigDecimal currentPrice;
    private BigDecimal markPrice;
    private String markPriceSource;
    private LocalDateTime markPriceObservedAt;
    private boolean markPriceFresh;
    private BigDecimal entryPrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private String logicStatus;
    private String entryLogicStatus;
    private String monitorConclusion;
    private String directionSupportStatus;
    private String reversalStatus;
    private String riskReason;
    private String riskLevel;
    private boolean riskBlocked;
    private boolean riskIncreased;
    private boolean nearStopLoss;
    private boolean nearTakeProfit;
    private boolean stopLossBreached;
    private boolean takeProfitReached;
    private String suggestedAction;
    private String suggestedManualAction;
    private String suggestedManualActionText;
    private BigDecimal pnlPct;
    private BigDecimal pnlPercent;
    private BigDecimal pnlAmount;
    private BigDecimal accountImpactPct;
    private List<String> reasonCodes = new ArrayList<>();
    private String externalContextStatus;
    private Integer activeExternalEventCount;
    private Integer activeMacroEventCount;
    private Integer activeNewsEventCount;
    private String externalContextRiskLevel;
    private Boolean externalContextBlocked;
    private List<String> externalEventIds = new ArrayList<>();
    private List<String> externalContextReasonCodes = new ArrayList<>();
    private LocalDateTime nextExternalEventTime;
    private String externalContextSourceHealth;
    private Long monitorLogId;
    private LocalDateTime monitoredAt;
    private LocalDateTime lastMonitorTime;
    private String dataState;
    private boolean reviewOnly = true;
    private boolean manualReviewOnly = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoReduce = true;
    private boolean notAutoClose = true;
    private boolean notAutoReverse = true;
    private boolean notOrderExecution = true;
    private boolean notAutoTrading = true;
    private boolean notPositionMutation = true;

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getPositionStatus() { return positionStatus; }
    public void setPositionStatus(String positionStatus) { this.positionStatus = positionStatus; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getExecutionPlanId() { return executionPlanId; }
    public void setExecutionPlanId(String executionPlanId) { this.executionPlanId = executionPlanId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getMarkPrice() { return markPrice; }
    public void setMarkPrice(BigDecimal markPrice) { this.markPrice = markPrice; }
    public String getMarkPriceSource() { return markPriceSource; }
    public void setMarkPriceSource(String markPriceSource) { this.markPriceSource = markPriceSource; }
    public LocalDateTime getMarkPriceObservedAt() { return markPriceObservedAt; }
    public void setMarkPriceObservedAt(LocalDateTime markPriceObservedAt) { this.markPriceObservedAt = markPriceObservedAt; }
    public boolean isMarkPriceFresh() { return markPriceFresh; }
    public void setMarkPriceFresh(boolean markPriceFresh) { this.markPriceFresh = markPriceFresh; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public BigDecimal getStopLoss() { return stopLoss; }
    public void setStopLoss(BigDecimal stopLoss) { this.stopLoss = stopLoss; }
    public BigDecimal getTakeProfit() { return takeProfit; }
    public void setTakeProfit(BigDecimal takeProfit) { this.takeProfit = takeProfit; }
    public String getLogicStatus() { return logicStatus; }
    public void setLogicStatus(String logicStatus) { this.logicStatus = logicStatus; }
    public String getEntryLogicStatus() { return entryLogicStatus; }
    public void setEntryLogicStatus(String entryLogicStatus) { this.entryLogicStatus = entryLogicStatus; }
    public String getMonitorConclusion() { return monitorConclusion; }
    public void setMonitorConclusion(String monitorConclusion) { this.monitorConclusion = monitorConclusion; }
    public String getDirectionSupportStatus() { return directionSupportStatus; }
    public void setDirectionSupportStatus(String directionSupportStatus) { this.directionSupportStatus = directionSupportStatus; }
    public String getReversalStatus() { return reversalStatus; }
    public void setReversalStatus(String reversalStatus) { this.reversalStatus = reversalStatus; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public boolean isRiskBlocked() { return riskBlocked; }
    public void setRiskBlocked(boolean riskBlocked) { this.riskBlocked = riskBlocked; }
    public boolean isRiskIncreased() { return riskIncreased; }
    public void setRiskIncreased(boolean riskIncreased) { this.riskIncreased = riskIncreased; }
    public boolean isNearStopLoss() { return nearStopLoss; }
    public void setNearStopLoss(boolean nearStopLoss) { this.nearStopLoss = nearStopLoss; }
    public boolean isNearTakeProfit() { return nearTakeProfit; }
    public void setNearTakeProfit(boolean nearTakeProfit) { this.nearTakeProfit = nearTakeProfit; }
    public boolean isStopLossBreached() { return stopLossBreached; }
    public void setStopLossBreached(boolean stopLossBreached) { this.stopLossBreached = stopLossBreached; }
    public boolean isTakeProfitReached() { return takeProfitReached; }
    public void setTakeProfitReached(boolean takeProfitReached) { this.takeProfitReached = takeProfitReached; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public String getSuggestedManualAction() { return suggestedManualAction; }
    public void setSuggestedManualAction(String suggestedManualAction) { this.suggestedManualAction = suggestedManualAction; }
    public String getSuggestedManualActionText() { return suggestedManualActionText; }
    public void setSuggestedManualActionText(String suggestedManualActionText) { this.suggestedManualActionText = suggestedManualActionText; }
    public BigDecimal getPnlPct() { return pnlPct; }
    public void setPnlPct(BigDecimal pnlPct) { this.pnlPct = pnlPct; }
    public BigDecimal getPnlPercent() { return pnlPercent; }
    public void setPnlPercent(BigDecimal pnlPercent) { this.pnlPercent = pnlPercent; }
    public BigDecimal getPnlAmount() { return pnlAmount; }
    public void setPnlAmount(BigDecimal pnlAmount) { this.pnlAmount = pnlAmount; }
    public BigDecimal getAccountImpactPct() { return accountImpactPct; }
    public void setAccountImpactPct(BigDecimal accountImpactPct) { this.accountImpactPct = accountImpactPct; }
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes); }
    public String getExternalContextStatus() { return externalContextStatus; }
    public void setExternalContextStatus(String externalContextStatus) { this.externalContextStatus = externalContextStatus; }
    public Integer getActiveExternalEventCount() { return activeExternalEventCount; }
    public void setActiveExternalEventCount(Integer activeExternalEventCount) { this.activeExternalEventCount = activeExternalEventCount; }
    public Integer getActiveMacroEventCount() { return activeMacroEventCount; }
    public void setActiveMacroEventCount(Integer activeMacroEventCount) { this.activeMacroEventCount = activeMacroEventCount; }
    public Integer getActiveNewsEventCount() { return activeNewsEventCount; }
    public void setActiveNewsEventCount(Integer activeNewsEventCount) { this.activeNewsEventCount = activeNewsEventCount; }
    public String getExternalContextRiskLevel() { return externalContextRiskLevel; }
    public void setExternalContextRiskLevel(String externalContextRiskLevel) { this.externalContextRiskLevel = externalContextRiskLevel; }
    public Boolean getExternalContextBlocked() { return externalContextBlocked; }
    public void setExternalContextBlocked(Boolean externalContextBlocked) { this.externalContextBlocked = externalContextBlocked; }
    public List<String> getExternalEventIds() { return Collections.unmodifiableList(externalEventIds); }
    public void setExternalEventIds(List<String> externalEventIds) { this.externalEventIds = externalEventIds == null ? new ArrayList<>() : new ArrayList<>(externalEventIds); }
    public List<String> getExternalContextReasonCodes() { return Collections.unmodifiableList(externalContextReasonCodes); }
    public void setExternalContextReasonCodes(List<String> externalContextReasonCodes) { this.externalContextReasonCodes = externalContextReasonCodes == null ? new ArrayList<>() : new ArrayList<>(externalContextReasonCodes); }
    public LocalDateTime getNextExternalEventTime() { return nextExternalEventTime; }
    public void setNextExternalEventTime(LocalDateTime nextExternalEventTime) { this.nextExternalEventTime = nextExternalEventTime; }
    public String getExternalContextSourceHealth() { return externalContextSourceHealth; }
    public void setExternalContextSourceHealth(String externalContextSourceHealth) { this.externalContextSourceHealth = externalContextSourceHealth; }
    public Long getMonitorLogId() { return monitorLogId; }
    public void setMonitorLogId(Long monitorLogId) { this.monitorLogId = monitorLogId; }
    public LocalDateTime getMonitoredAt() { return monitoredAt; }
    public void setMonitoredAt(LocalDateTime monitoredAt) { this.monitoredAt = monitoredAt; }
    public LocalDateTime getLastMonitorTime() { return lastMonitorTime; }
    public void setLastMonitorTime(LocalDateTime lastMonitorTime) { this.lastMonitorTime = lastMonitorTime; }
    public String getDataState() { return dataState; }
    public void setDataState(String dataState) { this.dataState = dataState; }
    public boolean isReviewOnly() { return reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public boolean isNotAutoReduce() { return notAutoReduce; }
    public boolean isNotAutoClose() { return notAutoClose; }
    public boolean isNotAutoReverse() { return notAutoReverse; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public boolean isNotPositionMutation() { return notPositionMutation; }
}
