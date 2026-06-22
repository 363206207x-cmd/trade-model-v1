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
    private BigDecimal entryPrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private String logicStatus;
    private String riskLevel;
    private boolean riskBlocked;
    private boolean riskIncreased;
    private boolean nearStopLoss;
    private boolean nearTakeProfit;
    private boolean stopLossBreached;
    private boolean takeProfitReached;
    private String suggestedAction;
    private List<String> reasonCodes = new ArrayList<>();
    private Long monitorLogId;
    private LocalDateTime monitoredAt;
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
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public BigDecimal getStopLoss() { return stopLoss; }
    public void setStopLoss(BigDecimal stopLoss) { this.stopLoss = stopLoss; }
    public BigDecimal getTakeProfit() { return takeProfit; }
    public void setTakeProfit(BigDecimal takeProfit) { this.takeProfit = takeProfit; }
    public String getLogicStatus() { return logicStatus; }
    public void setLogicStatus(String logicStatus) { this.logicStatus = logicStatus; }
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
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes); }
    public Long getMonitorLogId() { return monitorLogId; }
    public void setMonitorLogId(Long monitorLogId) { this.monitorLogId = monitorLogId; }
    public LocalDateTime getMonitoredAt() { return monitoredAt; }
    public void setMonitoredAt(LocalDateTime monitoredAt) { this.monitoredAt = monitoredAt; }
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
