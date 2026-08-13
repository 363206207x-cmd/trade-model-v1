package org.example.trademodel.dto.req;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存复盘：客户端提交字段；id / 时间戳由服务端写入。
 */
public class WriteReviewResultReq {

    @NotBlank
    private String analysisId;
    private String errorType;
    private String actualOutcome;
    private String adjustmentSuggestion;
    private String reviewType;
    private String outcome;
    private String executionDeviation;
    private String aiAssessment;
    private String ruleAssessment;
    private String ruleFeedback;
    private String metricsJson;

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
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

    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getExecutionDeviation() { return executionDeviation; }
    public void setExecutionDeviation(String executionDeviation) { this.executionDeviation = executionDeviation; }
    public String getAiAssessment() { return aiAssessment; }
    public void setAiAssessment(String aiAssessment) { this.aiAssessment = aiAssessment; }
    public String getRuleAssessment() { return ruleAssessment; }
    public void setRuleAssessment(String ruleAssessment) { this.ruleAssessment = ruleAssessment; }
    public String getRuleFeedback() { return ruleFeedback; }
    public void setRuleFeedback(String ruleFeedback) { this.ruleFeedback = ruleFeedback; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
}
