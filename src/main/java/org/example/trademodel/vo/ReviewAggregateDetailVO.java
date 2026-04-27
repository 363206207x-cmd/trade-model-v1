package org.example.trademodel.vo;

import java.util.List;

/**
 * Step 3: 复盘明细按 section 懒加载响应。
 */
public class ReviewAggregateDetailVO {

    private String analysisId;
    private String section;
    private Integer limitApplied;
    private Integer total;
    private Boolean truncated;

    private List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck;
    private List<ReviewAggregateVO.ReviewMissedSummary> missed;
    private List<ReviewAggregateVO.ReviewAlertSummary> alerts;
    private List<ReviewAggregateVO.RuleVersionLogSummary> ruleVersionLogs;
    private ReviewAggregateVO.ReviewHotResetSummary hotReset;

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Integer getLimitApplied() {
        return limitApplied;
    }

    public void setLimitApplied(Integer limitApplied) {
        this.limitApplied = limitApplied;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    public List<ReviewAggregateVO.ReviewPushWithRecheck> getPushRecheck() {
        return pushRecheck;
    }

    public void setPushRecheck(List<ReviewAggregateVO.ReviewPushWithRecheck> pushRecheck) {
        this.pushRecheck = pushRecheck;
    }

    public List<ReviewAggregateVO.ReviewMissedSummary> getMissed() {
        return missed;
    }

    public void setMissed(List<ReviewAggregateVO.ReviewMissedSummary> missed) {
        this.missed = missed;
    }

    public List<ReviewAggregateVO.ReviewAlertSummary> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<ReviewAggregateVO.ReviewAlertSummary> alerts) {
        this.alerts = alerts;
    }

    public List<ReviewAggregateVO.RuleVersionLogSummary> getRuleVersionLogs() {
        return ruleVersionLogs;
    }

    public void setRuleVersionLogs(List<ReviewAggregateVO.RuleVersionLogSummary> ruleVersionLogs) {
        this.ruleVersionLogs = ruleVersionLogs;
    }

    public ReviewAggregateVO.ReviewHotResetSummary getHotReset() {
        return hotReset;
    }

    public void setHotReset(ReviewAggregateVO.ReviewHotResetSummary hotReset) {
        this.hotReset = hotReset;
    }
}
