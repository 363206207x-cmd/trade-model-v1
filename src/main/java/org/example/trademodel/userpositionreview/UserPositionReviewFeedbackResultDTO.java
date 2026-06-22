package org.example.trademodel.userpositionreview;

import java.time.LocalDateTime;

public class UserPositionReviewFeedbackResultDTO {
    private Long positionId;
    private String analysisId;
    private String reviewId;
    private String errorType;
    private String actualOutcome;
    private String adjustmentSuggestion;
    private boolean ruleFeedbackRecorded;
    private boolean ruleChangeApplied;
    private LocalDateTime recordedAt;
    private boolean manualInputOnly = true;
    private boolean notRuleAutoApply = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoTrading = true;

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getActualOutcome() { return actualOutcome; }
    public void setActualOutcome(String actualOutcome) { this.actualOutcome = actualOutcome; }
    public String getAdjustmentSuggestion() { return adjustmentSuggestion; }
    public void setAdjustmentSuggestion(String adjustmentSuggestion) { this.adjustmentSuggestion = adjustmentSuggestion; }
    public boolean isRuleFeedbackRecorded() { return ruleFeedbackRecorded; }
    public void setRuleFeedbackRecorded(boolean ruleFeedbackRecorded) { this.ruleFeedbackRecorded = ruleFeedbackRecorded; }
    public boolean isRuleChangeApplied() { return ruleChangeApplied; }
    public void setRuleChangeApplied(boolean ruleChangeApplied) { this.ruleChangeApplied = ruleChangeApplied; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public boolean isManualInputOnly() { return manualInputOnly; }
    public void setManualInputOnly(boolean manualInputOnly) { this.manualInputOnly = manualInputOnly; }
    public boolean isNotRuleAutoApply() { return notRuleAutoApply; }
    public void setNotRuleAutoApply(boolean notRuleAutoApply) { this.notRuleAutoApply = notRuleAutoApply; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(boolean notTradeInstruction) { this.notTradeInstruction = notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public void setNotExecutable(boolean notExecutable) { this.notExecutable = notExecutable; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public void setNotAutoTrading(boolean notAutoTrading) { this.notAutoTrading = notAutoTrading; }
}
