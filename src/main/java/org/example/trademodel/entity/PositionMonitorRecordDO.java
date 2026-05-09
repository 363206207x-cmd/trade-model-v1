package org.example.trademodel.entity;

import java.time.LocalDateTime;

/**
 * 持仓监控记录（第一版最小实现）：写入后用于 {@code GET /api/position-monitor/open} 横条展示。
 */
public class PositionMonitorRecordDO {
    private String positionMonitorRecordId;
    private String positionId;
    private String symbol;
    private String analysisId;
    private String planId;
    private LocalDateTime monitorTime;

    private String entryLogicState;
    private String directionSupportState;
    private String reversalState;
    private String positionRiskLevel;
    private String aiSupportState;
    private String systemSuggestedAction;
    private String monitorSummary;
    private String reviewEntryStatus;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getPositionMonitorRecordId() {
        return positionMonitorRecordId;
    }

    public void setPositionMonitorRecordId(String positionMonitorRecordId) {
        this.positionMonitorRecordId = positionMonitorRecordId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public LocalDateTime getMonitorTime() {
        return monitorTime;
    }

    public void setMonitorTime(LocalDateTime monitorTime) {
        this.monitorTime = monitorTime;
    }

    public String getEntryLogicState() {
        return entryLogicState;
    }

    public void setEntryLogicState(String entryLogicState) {
        this.entryLogicState = entryLogicState;
    }

    public String getDirectionSupportState() {
        return directionSupportState;
    }

    public void setDirectionSupportState(String directionSupportState) {
        this.directionSupportState = directionSupportState;
    }

    public String getReversalState() {
        return reversalState;
    }

    public void setReversalState(String reversalState) {
        this.reversalState = reversalState;
    }

    public String getPositionRiskLevel() {
        return positionRiskLevel;
    }

    public void setPositionRiskLevel(String positionRiskLevel) {
        this.positionRiskLevel = positionRiskLevel;
    }

    public String getAiSupportState() {
        return aiSupportState;
    }

    public void setAiSupportState(String aiSupportState) {
        this.aiSupportState = aiSupportState;
    }

    public String getSystemSuggestedAction() {
        return systemSuggestedAction;
    }

    public void setSystemSuggestedAction(String systemSuggestedAction) {
        this.systemSuggestedAction = systemSuggestedAction;
    }

    public String getMonitorSummary() {
        return monitorSummary;
    }

    public void setMonitorSummary(String monitorSummary) {
        this.monitorSummary = monitorSummary;
    }

    public String getReviewEntryStatus() {
        return reviewEntryStatus;
    }

    public void setReviewEntryStatus(String reviewEntryStatus) {
        this.reviewEntryStatus = reviewEntryStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

