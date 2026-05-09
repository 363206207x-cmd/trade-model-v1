package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PositionTradeResultDO {
    private String tradeResultId;
    private String positionId;
    private String symbol;
    private String positionSide;
    private BigDecimal avgOpenPrice;
    private LocalDateTime positionOpenTime;
    private BigDecimal positionQuantity;
    private BigDecimal exitPrice;
    private LocalDateTime closeTime;
    private BigDecimal realizedPnl;
    private BigDecimal realizedPnlPct;
    private String closeReason;
    private String userActionType;
    private String userRemark;
    private String linkedAnalysisId;
    private String linkedPlanId;
    private String latestMonitorRecordId;
    private String systemSuggestedActionAtClose;
    private String userDeviationFromSystemSuggestion;
    private String reviewStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getTradeResultId() { return tradeResultId; }
    public void setTradeResultId(String tradeResultId) { this.tradeResultId = tradeResultId; }
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getPositionSide() { return positionSide; }
    public void setPositionSide(String positionSide) { this.positionSide = positionSide; }
    public BigDecimal getAvgOpenPrice() { return avgOpenPrice; }
    public void setAvgOpenPrice(BigDecimal avgOpenPrice) { this.avgOpenPrice = avgOpenPrice; }
    public LocalDateTime getPositionOpenTime() { return positionOpenTime; }
    public void setPositionOpenTime(LocalDateTime positionOpenTime) { this.positionOpenTime = positionOpenTime; }
    public BigDecimal getPositionQuantity() { return positionQuantity; }
    public void setPositionQuantity(BigDecimal positionQuantity) { this.positionQuantity = positionQuantity; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public void setExitPrice(BigDecimal exitPrice) { this.exitPrice = exitPrice; }
    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }
    public BigDecimal getRealizedPnlPct() { return realizedPnlPct; }
    public void setRealizedPnlPct(BigDecimal realizedPnlPct) { this.realizedPnlPct = realizedPnlPct; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public String getUserActionType() { return userActionType; }
    public void setUserActionType(String userActionType) { this.userActionType = userActionType; }
    public String getUserRemark() { return userRemark; }
    public void setUserRemark(String userRemark) { this.userRemark = userRemark; }
    public String getLinkedAnalysisId() { return linkedAnalysisId; }
    public void setLinkedAnalysisId(String linkedAnalysisId) { this.linkedAnalysisId = linkedAnalysisId; }
    public String getLinkedPlanId() { return linkedPlanId; }
    public void setLinkedPlanId(String linkedPlanId) { this.linkedPlanId = linkedPlanId; }
    public String getLatestMonitorRecordId() { return latestMonitorRecordId; }
    public void setLatestMonitorRecordId(String latestMonitorRecordId) { this.latestMonitorRecordId = latestMonitorRecordId; }
    public String getSystemSuggestedActionAtClose() { return systemSuggestedActionAtClose; }
    public void setSystemSuggestedActionAtClose(String systemSuggestedActionAtClose) { this.systemSuggestedActionAtClose = systemSuggestedActionAtClose; }
    public String getUserDeviationFromSystemSuggestion() { return userDeviationFromSystemSuggestion; }
    public void setUserDeviationFromSystemSuggestion(String userDeviationFromSystemSuggestion) { this.userDeviationFromSystemSuggestion = userDeviationFromSystemSuggestion; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
