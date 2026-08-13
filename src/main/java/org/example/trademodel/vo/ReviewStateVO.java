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
    private String opportunityId;
    private String resolverResultId;
    private String validationResultId;
    private String reviewType;
    private String outcome;
    private String executionDeviation;
    private String aiAssessment;
    private String ruleAssessment;
    private String ruleFeedback;
    private String metricsJson;
    private String contractVersion;
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
    public String getOpportunityId() { return opportunityId; }
    public void setOpportunityId(String value) { this.opportunityId = value; }
    public String getResolverResultId() { return resolverResultId; }
    public void setResolverResultId(String value) { this.resolverResultId = value; }
    public String getValidationResultId() { return validationResultId; }
    public void setValidationResultId(String value) { this.validationResultId = value; }
    public String getReviewType() { return reviewType; }
    public void setReviewType(String value) { this.reviewType = value; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String value) { this.outcome = value; }
    public String getExecutionDeviation() { return executionDeviation; }
    public void setExecutionDeviation(String value) { this.executionDeviation = value; }
    public String getAiAssessment() { return aiAssessment; }
    public void setAiAssessment(String value) { this.aiAssessment = value; }
    public String getRuleAssessment() { return ruleAssessment; }
    public void setRuleAssessment(String value) { this.ruleAssessment = value; }
    public String getRuleFeedback() { return ruleFeedback; }
    public void setRuleFeedback(String value) { this.ruleFeedback = value; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String value) { this.metricsJson = value; }
    public String getContractVersion() { return contractVersion; }
    public void setContractVersion(String value) { this.contractVersion = value; }

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
