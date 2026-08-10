package org.example.trademodel.entity;

import java.time.LocalDateTime;

/**
 * 复盘结果落库行（首轮窄表，与 tm_review_result 对齐）。
 */
public class ReviewResultDO {

    private String id;
    private String analysisId;
    private Long userId;
    private Long userPositionId;
    private String finalPlanId;
    private String candidateId;
    private String traceId;
    private String reviewScopeKey;
    private String errorType;
    private String actualOutcome;
    private String adjustmentSuggestion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserPositionId() {
        return userPositionId;
    }

    public void setUserPositionId(Long userPositionId) {
        this.userPositionId = userPositionId;
    }

    public String getFinalPlanId() { return finalPlanId; }
    public void setFinalPlanId(String finalPlanId) { this.finalPlanId = finalPlanId; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getReviewScopeKey() {
        return reviewScopeKey;
    }

    public void setReviewScopeKey(String reviewScopeKey) {
        this.reviewScopeKey = reviewScopeKey;
    }

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
