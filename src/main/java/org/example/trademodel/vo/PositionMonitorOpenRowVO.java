package org.example.trademodel.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PositionMonitorOpenRowVO {
    private String positionId;
    private String symbol;
    private String positionSide;
    private BigDecimal avgOpenPrice;
    private BigDecimal markPrice;
    private BigDecimal unrealizedPnlPct;
    private BigDecimal positionQuantity;
    private LocalDateTime positionOpenTime;

    private boolean monitorRecordAvailable;
    private LatestMonitorRecordVO latestMonitorRecord;

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

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public BigDecimal getAvgOpenPrice() {
        return avgOpenPrice;
    }

    public void setAvgOpenPrice(BigDecimal avgOpenPrice) {
        this.avgOpenPrice = avgOpenPrice;
    }

    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(BigDecimal markPrice) {
        this.markPrice = markPrice;
    }

    public BigDecimal getUnrealizedPnlPct() {
        return unrealizedPnlPct;
    }

    public void setUnrealizedPnlPct(BigDecimal unrealizedPnlPct) {
        this.unrealizedPnlPct = unrealizedPnlPct;
    }

    public BigDecimal getPositionQuantity() {
        return positionQuantity;
    }

    public void setPositionQuantity(BigDecimal positionQuantity) {
        this.positionQuantity = positionQuantity;
    }

    public LocalDateTime getPositionOpenTime() {
        return positionOpenTime;
    }

    public void setPositionOpenTime(LocalDateTime positionOpenTime) {
        this.positionOpenTime = positionOpenTime;
    }

    public boolean isMonitorRecordAvailable() {
        return monitorRecordAvailable;
    }

    public void setMonitorRecordAvailable(boolean monitorRecordAvailable) {
        this.monitorRecordAvailable = monitorRecordAvailable;
    }

    public LatestMonitorRecordVO getLatestMonitorRecord() {
        return latestMonitorRecord;
    }

    public void setLatestMonitorRecord(LatestMonitorRecordVO latestMonitorRecord) {
        this.latestMonitorRecord = latestMonitorRecord;
    }

    public static class LatestMonitorRecordVO {
        private String positionMonitorRecordId;
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

        private String boundaryParseStatus;
        private String boundaryStateLabel;
        private String boundaryDisplayText;
        private String boundaryWarningText;
        private String invalidPriceDirection;
        private BigDecimal invalidPriceThreshold;

        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public String getPositionMonitorRecordId() {
            return positionMonitorRecordId;
        }

        public void setPositionMonitorRecordId(String positionMonitorRecordId) {
            this.positionMonitorRecordId = positionMonitorRecordId;
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

        public String getBoundaryParseStatus() {
            return boundaryParseStatus;
        }

        public void setBoundaryParseStatus(String boundaryParseStatus) {
            this.boundaryParseStatus = boundaryParseStatus;
        }

        public String getBoundaryStateLabel() {
            return boundaryStateLabel;
        }

        public void setBoundaryStateLabel(String boundaryStateLabel) {
            this.boundaryStateLabel = boundaryStateLabel;
        }

        public String getBoundaryDisplayText() {
            return boundaryDisplayText;
        }

        public void setBoundaryDisplayText(String boundaryDisplayText) {
            this.boundaryDisplayText = boundaryDisplayText;
        }

        public String getBoundaryWarningText() {
            return boundaryWarningText;
        }

        public void setBoundaryWarningText(String boundaryWarningText) {
            this.boundaryWarningText = boundaryWarningText;
        }

        public String getInvalidPriceDirection() {
            return invalidPriceDirection;
        }

        public void setInvalidPriceDirection(String invalidPriceDirection) {
            this.invalidPriceDirection = invalidPriceDirection;
        }

        public BigDecimal getInvalidPriceThreshold() {
            return invalidPriceThreshold;
        }

        public void setInvalidPriceThreshold(BigDecimal invalidPriceThreshold) {
            this.invalidPriceThreshold = invalidPriceThreshold;
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
}

