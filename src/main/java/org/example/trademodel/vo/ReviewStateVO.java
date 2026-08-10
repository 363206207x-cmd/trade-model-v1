package org.example.trademodel.vo;

import java.time.LocalDateTime;

/**
 * 复盘状态读写：与 tm_review_result 对外展示一致（reviewId 即主键 id）。
 */
public class ReviewStateVO {

    private String reviewId;
    private String analysisId;
    private String finalPlanId;
    private String candidateId;
    private String traceId;
    private String errorType;
    private String actualOutcome;
    private String adjustmentSuggestion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getFinalPlanId() { return finalPlanId; }
    public void setFinalPlanId(String finalPlanId) { this.finalPlanId = finalPlanId; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public String getAdjustmentSuggestion() {
        return adjustmentSuggestion;
    }

    public void setAdjustmentSuggestion(String adjustmentSuggestion) {
        this.adjustmentSuggestion = adjustmentSuggestion;
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
