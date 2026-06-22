package org.example.trademodel.userpositionreview;

import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserPositionReviewSummaryDTO {
    private Long positionId;
    private String assetSymbol;
    private String side;
    private String positionStatus;
    private String sourceRefId;
    private String analysisId;
    private String executionPlanId;
    private String planContextStatus;
    private String executionPlanStatus;
    private String sourceGateStatus;
    private Boolean sourceGateComplete;
    private String entryZone;
    private String planStopLoss;
    private String takeProfitRules;
    private String invalidCondition;
    private String recommendedAction;
    private BigDecimal entryPrice;
    private BigDecimal closePrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private BigDecimal quantity;
    private BigDecimal leverage;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Long holdingDurationSeconds;
    private BigDecimal grossPnl;
    private BigDecimal grossReturnPct;
    private BigDecimal leveragedReturnPctProxy;
    private String outcome;
    private String pnlCalculationMethod;
    private String executionDeviationStatus;
    private BigDecimal entryDeviationRatio;
    private BigDecimal stopLossDeviationRatio;
    private BigDecimal takeProfitDeviationRatio;
    private List<String> executionDeviationReasons = new ArrayList<>();
    private int monitorLogCount;
    private List<PositionMonitorLogDTO> monitorLogs = new ArrayList<>();
    private boolean planInvalidatedBeforeClose;
    private LocalDateTime firstPlanInvalidatedAt;
    private int planInvalidationWarningCount;
    private boolean warnedBeforeClose;
    private LocalDateTime firstWarningAt;
    private LocalDateTime lastWarningAt;
    private int warningCount;
    private Long warningLeadSeconds;
    private String warningTimelinessStatus;
    private boolean ignoredWarning;
    private List<String> ignoredWarningReasons = new ArrayList<>();
    private String reviewStatus;
    private List<String> reviewReasons = new ArrayList<>();
    private LocalDateTime generatedAt;
    private boolean reviewOnly = true;
    private boolean manualReviewOnly = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoTrading = true;
    private boolean notOrderExecution = true;
    private boolean notAutoOpen = true;
    private boolean notAutoClose = true;
    private boolean notAutoReverse = true;
    private boolean notUserPositionMutation = true;
    private boolean notRuleAutoApply = true;

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getAssetSymbol() { return assetSymbol; }
    public void setAssetSymbol(String assetSymbol) { this.assetSymbol = assetSymbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getPositionStatus() { return positionStatus; }
    public void setPositionStatus(String positionStatus) { this.positionStatus = positionStatus; }
    public String getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(String sourceRefId) { this.sourceRefId = sourceRefId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getExecutionPlanId() { return executionPlanId; }
    public void setExecutionPlanId(String executionPlanId) { this.executionPlanId = executionPlanId; }
    public String getPlanContextStatus() { return planContextStatus; }
    public void setPlanContextStatus(String planContextStatus) { this.planContextStatus = planContextStatus; }
    public String getExecutionPlanStatus() { return executionPlanStatus; }
    public void setExecutionPlanStatus(String executionPlanStatus) { this.executionPlanStatus = executionPlanStatus; }
    public String getSourceGateStatus() { return sourceGateStatus; }
    public void setSourceGateStatus(String sourceGateStatus) { this.sourceGateStatus = sourceGateStatus; }
    public Boolean getSourceGateComplete() { return sourceGateComplete; }
    public void setSourceGateComplete(Boolean sourceGateComplete) { this.sourceGateComplete = sourceGateComplete; }
    public String getEntryZone() { return entryZone; }
    public void setEntryZone(String entryZone) { this.entryZone = entryZone; }
    public String getPlanStopLoss() { return planStopLoss; }
    public void setPlanStopLoss(String planStopLoss) { this.planStopLoss = planStopLoss; }
    public String getTakeProfitRules() { return takeProfitRules; }
    public void setTakeProfitRules(String takeProfitRules) { this.takeProfitRules = takeProfitRules; }
    public String getInvalidCondition() { return invalidCondition; }
    public void setInvalidCondition(String invalidCondition) { this.invalidCondition = invalidCondition; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
    public BigDecimal getStopLoss() { return stopLoss; }
    public void setStopLoss(BigDecimal stopLoss) { this.stopLoss = stopLoss; }
    public BigDecimal getTakeProfit() { return takeProfit; }
    public void setTakeProfit(BigDecimal takeProfit) { this.takeProfit = takeProfit; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getLeverage() { return leverage; }
    public void setLeverage(BigDecimal leverage) { this.leverage = leverage; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public Long getHoldingDurationSeconds() { return holdingDurationSeconds; }
    public void setHoldingDurationSeconds(Long holdingDurationSeconds) { this.holdingDurationSeconds = holdingDurationSeconds; }
    public BigDecimal getGrossPnl() { return grossPnl; }
    public void setGrossPnl(BigDecimal grossPnl) { this.grossPnl = grossPnl; }
    public BigDecimal getGrossReturnPct() { return grossReturnPct; }
    public void setGrossReturnPct(BigDecimal grossReturnPct) { this.grossReturnPct = grossReturnPct; }
    public BigDecimal getLeveragedReturnPctProxy() { return leveragedReturnPctProxy; }
    public void setLeveragedReturnPctProxy(BigDecimal leveragedReturnPctProxy) { this.leveragedReturnPctProxy = leveragedReturnPctProxy; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getPnlCalculationMethod() { return pnlCalculationMethod; }
    public void setPnlCalculationMethod(String pnlCalculationMethod) { this.pnlCalculationMethod = pnlCalculationMethod; }
    public String getExecutionDeviationStatus() { return executionDeviationStatus; }
    public void setExecutionDeviationStatus(String executionDeviationStatus) { this.executionDeviationStatus = executionDeviationStatus; }
    public BigDecimal getEntryDeviationRatio() { return entryDeviationRatio; }
    public void setEntryDeviationRatio(BigDecimal entryDeviationRatio) { this.entryDeviationRatio = entryDeviationRatio; }
    public BigDecimal getStopLossDeviationRatio() { return stopLossDeviationRatio; }
    public void setStopLossDeviationRatio(BigDecimal stopLossDeviationRatio) { this.stopLossDeviationRatio = stopLossDeviationRatio; }
    public BigDecimal getTakeProfitDeviationRatio() { return takeProfitDeviationRatio; }
    public void setTakeProfitDeviationRatio(BigDecimal takeProfitDeviationRatio) { this.takeProfitDeviationRatio = takeProfitDeviationRatio; }
    public List<String> getExecutionDeviationReasons() { return executionDeviationReasons; }
    public void setExecutionDeviationReasons(List<String> executionDeviationReasons) { this.executionDeviationReasons = executionDeviationReasons; }
    public int getMonitorLogCount() { return monitorLogCount; }
    public void setMonitorLogCount(int monitorLogCount) { this.monitorLogCount = monitorLogCount; }
    public List<PositionMonitorLogDTO> getMonitorLogs() { return monitorLogs; }
    public void setMonitorLogs(List<PositionMonitorLogDTO> monitorLogs) { this.monitorLogs = monitorLogs; }
    public boolean isPlanInvalidatedBeforeClose() { return planInvalidatedBeforeClose; }
    public void setPlanInvalidatedBeforeClose(boolean planInvalidatedBeforeClose) { this.planInvalidatedBeforeClose = planInvalidatedBeforeClose; }
    public LocalDateTime getFirstPlanInvalidatedAt() { return firstPlanInvalidatedAt; }
    public void setFirstPlanInvalidatedAt(LocalDateTime firstPlanInvalidatedAt) { this.firstPlanInvalidatedAt = firstPlanInvalidatedAt; }
    public int getPlanInvalidationWarningCount() { return planInvalidationWarningCount; }
    public void setPlanInvalidationWarningCount(int planInvalidationWarningCount) { this.planInvalidationWarningCount = planInvalidationWarningCount; }
    public boolean isWarnedBeforeClose() { return warnedBeforeClose; }
    public void setWarnedBeforeClose(boolean warnedBeforeClose) { this.warnedBeforeClose = warnedBeforeClose; }
    public LocalDateTime getFirstWarningAt() { return firstWarningAt; }
    public void setFirstWarningAt(LocalDateTime firstWarningAt) { this.firstWarningAt = firstWarningAt; }
    public LocalDateTime getLastWarningAt() { return lastWarningAt; }
    public void setLastWarningAt(LocalDateTime lastWarningAt) { this.lastWarningAt = lastWarningAt; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
    public Long getWarningLeadSeconds() { return warningLeadSeconds; }
    public void setWarningLeadSeconds(Long warningLeadSeconds) { this.warningLeadSeconds = warningLeadSeconds; }
    public String getWarningTimelinessStatus() { return warningTimelinessStatus; }
    public void setWarningTimelinessStatus(String warningTimelinessStatus) { this.warningTimelinessStatus = warningTimelinessStatus; }
    public boolean isIgnoredWarning() { return ignoredWarning; }
    public void setIgnoredWarning(boolean ignoredWarning) { this.ignoredWarning = ignoredWarning; }
    public List<String> getIgnoredWarningReasons() { return ignoredWarningReasons; }
    public void setIgnoredWarningReasons(List<String> ignoredWarningReasons) { this.ignoredWarningReasons = ignoredWarningReasons; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public List<String> getReviewReasons() { return reviewReasons; }
    public void setReviewReasons(List<String> reviewReasons) { this.reviewReasons = reviewReasons; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public boolean isReviewOnly() { return reviewOnly; }
    public void setReviewOnly(boolean reviewOnly) { this.reviewOnly = reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public void setManualReviewOnly(boolean manualReviewOnly) { this.manualReviewOnly = manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(boolean notTradeInstruction) { this.notTradeInstruction = notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public void setNotExecutable(boolean notExecutable) { this.notExecutable = notExecutable; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public void setNotAutoTrading(boolean notAutoTrading) { this.notAutoTrading = notAutoTrading; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public void setNotOrderExecution(boolean notOrderExecution) { this.notOrderExecution = notOrderExecution; }
    public boolean isNotAutoOpen() { return notAutoOpen; }
    public void setNotAutoOpen(boolean notAutoOpen) { this.notAutoOpen = notAutoOpen; }
    public boolean isNotAutoClose() { return notAutoClose; }
    public void setNotAutoClose(boolean notAutoClose) { this.notAutoClose = notAutoClose; }
    public boolean isNotAutoReverse() { return notAutoReverse; }
    public void setNotAutoReverse(boolean notAutoReverse) { this.notAutoReverse = notAutoReverse; }
    public boolean isNotUserPositionMutation() { return notUserPositionMutation; }
    public void setNotUserPositionMutation(boolean notUserPositionMutation) { this.notUserPositionMutation = notUserPositionMutation; }
    public boolean isNotRuleAutoApply() { return notRuleAutoApply; }
    public void setNotRuleAutoApply(boolean notRuleAutoApply) { this.notRuleAutoApply = notRuleAutoApply; }
}
