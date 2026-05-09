package org.example.trademodel.service;

import org.example.trademodel.dto.req.CloseManualPositionReq;
import org.example.trademodel.entity.PositionTradeResultDO;
import org.example.trademodel.service.ManualPositionCloseService.CloseResult;

public interface ManualPositionCloseService {
    CloseResult close(String positionId, CloseManualPositionReq req);

    class CloseResult {
        private String positionId;
        private String positionStatus;
        private String reviewAnalysisId;
        private String analysisReviewUrl;
        private String tradeResultId;
        private String tradeReviewUrl;
        private String reviewLevel;
        private String reviewMessage;
        private boolean exitPriceFallbackUsed;
        private PositionTradeResultDO tradeResult;

        public String getPositionId() { return positionId; }
        public void setPositionId(String positionId) { this.positionId = positionId; }
        public String getPositionStatus() { return positionStatus; }
        public void setPositionStatus(String positionStatus) { this.positionStatus = positionStatus; }
        public String getReviewAnalysisId() { return reviewAnalysisId; }
        public void setReviewAnalysisId(String reviewAnalysisId) { this.reviewAnalysisId = reviewAnalysisId; }
        public String getAnalysisReviewUrl() { return analysisReviewUrl; }
        public void setAnalysisReviewUrl(String analysisReviewUrl) { this.analysisReviewUrl = analysisReviewUrl; }
        public String getTradeResultId() { return tradeResultId; }
        public void setTradeResultId(String tradeResultId) { this.tradeResultId = tradeResultId; }
        public String getTradeReviewUrl() { return tradeReviewUrl; }
        public void setTradeReviewUrl(String tradeReviewUrl) { this.tradeReviewUrl = tradeReviewUrl; }
        public String getReviewLevel() { return reviewLevel; }
        public void setReviewLevel(String reviewLevel) { this.reviewLevel = reviewLevel; }
        public String getReviewMessage() { return reviewMessage; }
        public void setReviewMessage(String reviewMessage) { this.reviewMessage = reviewMessage; }
        public boolean isExitPriceFallbackUsed() { return exitPriceFallbackUsed; }
        public void setExitPriceFallbackUsed(boolean exitPriceFallbackUsed) { this.exitPriceFallbackUsed = exitPriceFallbackUsed; }
        public PositionTradeResultDO getTradeResult() { return tradeResult; }
        public void setTradeResult(PositionTradeResultDO tradeResult) { this.tradeResult = tradeResult; }
    }
}
