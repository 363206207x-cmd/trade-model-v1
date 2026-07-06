package org.example.trademodel.vo;

import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewCenterDashboardVO {
    private Summary summary = new Summary();
    private List<PositionReviewItem> positionReviews = new ArrayList<>();
    private List<OpportunityReviewItem> opportunityReviews = new ArrayList<>();
    private List<PushReviewItem> pushReviews = new ArrayList<>();
    private List<RuleFeedbackItem> ruleFeedback = new ArrayList<>();
    private Diagnostics diagnostics = new Diagnostics();

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    public List<PositionReviewItem> getPositionReviews() { return positionReviews; }
    public void setPositionReviews(List<PositionReviewItem> positionReviews) { this.positionReviews = positionReviews; }
    public List<OpportunityReviewItem> getOpportunityReviews() { return opportunityReviews; }
    public void setOpportunityReviews(List<OpportunityReviewItem> opportunityReviews) { this.opportunityReviews = opportunityReviews; }
    public List<PushReviewItem> getPushReviews() { return pushReviews; }
    public void setPushReviews(List<PushReviewItem> pushReviews) { this.pushReviews = pushReviews; }
    public List<RuleFeedbackItem> getRuleFeedback() { return ruleFeedback; }
    public void setRuleFeedback(List<RuleFeedbackItem> ruleFeedback) { this.ruleFeedback = ruleFeedback; }
    public Diagnostics getDiagnostics() { return diagnostics; }
    public void setDiagnostics(Diagnostics diagnostics) { this.diagnostics = diagnostics; }

    public static class Summary {
        private int positionReviewCount;
        private int opportunityReviewCount;
        private int pushReviewCount;
        private int ruleFeedbackCount;

        public int getPositionReviewCount() { return positionReviewCount; }
        public void setPositionReviewCount(int positionReviewCount) { this.positionReviewCount = positionReviewCount; }
        public int getOpportunityReviewCount() { return opportunityReviewCount; }
        public void setOpportunityReviewCount(int opportunityReviewCount) { this.opportunityReviewCount = opportunityReviewCount; }
        public int getPushReviewCount() { return pushReviewCount; }
        public void setPushReviewCount(int pushReviewCount) { this.pushReviewCount = pushReviewCount; }
        public int getRuleFeedbackCount() { return ruleFeedbackCount; }
        public void setRuleFeedbackCount(int ruleFeedbackCount) { this.ruleFeedbackCount = ruleFeedbackCount; }
    }

    public static class Diagnostics {
        private String positionReviewStatus;
        private String opportunityLogStatus;
        private String pushRecheckStatus;
        private String ruleFeedbackStatus;
        private String reviewCenterStatus;

        public String getPositionReviewStatus() { return positionReviewStatus; }
        public void setPositionReviewStatus(String positionReviewStatus) { this.positionReviewStatus = positionReviewStatus; }
        public String getOpportunityLogStatus() { return opportunityLogStatus; }
        public void setOpportunityLogStatus(String opportunityLogStatus) { this.opportunityLogStatus = opportunityLogStatus; }
        public String getPushRecheckStatus() { return pushRecheckStatus; }
        public void setPushRecheckStatus(String pushRecheckStatus) { this.pushRecheckStatus = pushRecheckStatus; }
        public String getRuleFeedbackStatus() { return ruleFeedbackStatus; }
        public void setRuleFeedbackStatus(String ruleFeedbackStatus) { this.ruleFeedbackStatus = ruleFeedbackStatus; }
        public String getReviewCenterStatus() { return reviewCenterStatus; }
        public void setReviewCenterStatus(String reviewCenterStatus) { this.reviewCenterStatus = reviewCenterStatus; }
    }

    public static class PositionReviewItem {
        private LocalDateTime time;
        private String symbol;
        private String direction;
        private BigDecimal entryPrice;
        private BigDecimal closePrice;
        private BigDecimal pnl;
        private String executionDeviation;
        private String monitorConclusion;
        private String reviewStatus;
        private ReviewAggregateVO.ReviewPlanSummary originalExecutionPlan;
        private String actualExecution;
        private List<String> executionDeviationDetail = new ArrayList<>();
        private List<PositionMonitorLogDTO> monitorTimeline = new ArrayList<>();
        private BigDecimal finalPnl;
        private String ruleFeedbackSuggestion;

        public LocalDateTime getTime() { return time; }
        public void setTime(LocalDateTime time) { this.time = time; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public BigDecimal getEntryPrice() { return entryPrice; }
        public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
        public BigDecimal getClosePrice() { return closePrice; }
        public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
        public BigDecimal getPnl() { return pnl; }
        public void setPnl(BigDecimal pnl) { this.pnl = pnl; }
        public String getExecutionDeviation() { return executionDeviation; }
        public void setExecutionDeviation(String executionDeviation) { this.executionDeviation = executionDeviation; }
        public String getMonitorConclusion() { return monitorConclusion; }
        public void setMonitorConclusion(String monitorConclusion) { this.monitorConclusion = monitorConclusion; }
        public String getReviewStatus() { return reviewStatus; }
        public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
        public ReviewAggregateVO.ReviewPlanSummary getOriginalExecutionPlan() { return originalExecutionPlan; }
        public void setOriginalExecutionPlan(ReviewAggregateVO.ReviewPlanSummary originalExecutionPlan) { this.originalExecutionPlan = originalExecutionPlan; }
        public String getActualExecution() { return actualExecution; }
        public void setActualExecution(String actualExecution) { this.actualExecution = actualExecution; }
        public List<String> getExecutionDeviationDetail() { return executionDeviationDetail; }
        public void setExecutionDeviationDetail(List<String> executionDeviationDetail) { this.executionDeviationDetail = executionDeviationDetail; }
        public List<PositionMonitorLogDTO> getMonitorTimeline() { return monitorTimeline; }
        public void setMonitorTimeline(List<PositionMonitorLogDTO> monitorTimeline) { this.monitorTimeline = monitorTimeline; }
        public BigDecimal getFinalPnl() { return finalPnl; }
        public void setFinalPnl(BigDecimal finalPnl) { this.finalPnl = finalPnl; }
        public String getRuleFeedbackSuggestion() { return ruleFeedbackSuggestion; }
        public void setRuleFeedbackSuggestion(String ruleFeedbackSuggestion) { this.ruleFeedbackSuggestion = ruleFeedbackSuggestion; }
    }

    public static class OpportunityReviewItem {
        private LocalDateTime time;
        private String symbol;
        private String opportunityType;
        private String planMode;
        private Boolean wasPushed;
        private Boolean wasClicked;
        private Boolean wasExecuted;
        private String outcome;
        private BigDecimal maxFavorableExcursion;
        private BigDecimal maxAdverseExcursion;

        public LocalDateTime getTime() { return time; }
        public void setTime(LocalDateTime time) { this.time = time; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getOpportunityType() { return opportunityType; }
        public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
        public String getPlanMode() { return planMode; }
        public void setPlanMode(String planMode) { this.planMode = planMode; }
        public Boolean getWasPushed() { return wasPushed; }
        public void setWasPushed(Boolean wasPushed) { this.wasPushed = wasPushed; }
        public Boolean getWasClicked() { return wasClicked; }
        public void setWasClicked(Boolean wasClicked) { this.wasClicked = wasClicked; }
        public Boolean getWasExecuted() { return wasExecuted; }
        public void setWasExecuted(Boolean wasExecuted) { this.wasExecuted = wasExecuted; }
        public String getOutcome() { return outcome; }
        public void setOutcome(String outcome) { this.outcome = outcome; }
        public BigDecimal getMaxFavorableExcursion() { return maxFavorableExcursion; }
        public void setMaxFavorableExcursion(BigDecimal maxFavorableExcursion) { this.maxFavorableExcursion = maxFavorableExcursion; }
        public BigDecimal getMaxAdverseExcursion() { return maxAdverseExcursion; }
        public void setMaxAdverseExcursion(BigDecimal maxAdverseExcursion) { this.maxAdverseExcursion = maxAdverseExcursion; }
    }

    public static class PushReviewItem {
        private LocalDateTime pushTime;
        private String symbol;
        private String pushType;
        private String telegramStatus;
        private Boolean clicked;
        private String recheckStatus;
        private Boolean expired;
        private String failReason;
        private String outcome;

        public LocalDateTime getPushTime() { return pushTime; }
        public void setPushTime(LocalDateTime pushTime) { this.pushTime = pushTime; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getPushType() { return pushType; }
        public void setPushType(String pushType) { this.pushType = pushType; }
        public String getTelegramStatus() { return telegramStatus; }
        public void setTelegramStatus(String telegramStatus) { this.telegramStatus = telegramStatus; }
        public Boolean getClicked() { return clicked; }
        public void setClicked(Boolean clicked) { this.clicked = clicked; }
        public String getRecheckStatus() { return recheckStatus; }
        public void setRecheckStatus(String recheckStatus) { this.recheckStatus = recheckStatus; }
        public Boolean getExpired() { return expired; }
        public void setExpired(Boolean expired) { this.expired = expired; }
        public String getFailReason() { return failReason; }
        public void setFailReason(String failReason) { this.failReason = failReason; }
        public String getOutcome() { return outcome; }
        public void setOutcome(String outcome) { this.outcome = outcome; }
    }

    public static class RuleFeedbackItem {
        private LocalDateTime time;
        private String symbol;
        private String reviewType;
        private String errorType;
        private Boolean ruleIssue;
        private Boolean executionDeviation;
        private String suggestion;
        private String ruleVersion;
        private String status;

        public LocalDateTime getTime() { return time; }
        public void setTime(LocalDateTime time) { this.time = time; }
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getReviewType() { return reviewType; }
        public void setReviewType(String reviewType) { this.reviewType = reviewType; }
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        public Boolean getRuleIssue() { return ruleIssue; }
        public void setRuleIssue(Boolean ruleIssue) { this.ruleIssue = ruleIssue; }
        public Boolean getExecutionDeviation() { return executionDeviation; }
        public void setExecutionDeviation(Boolean executionDeviation) { this.executionDeviation = executionDeviation; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public String getRuleVersion() { return ruleVersion; }
        public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
