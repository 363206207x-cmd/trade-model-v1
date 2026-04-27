package org.example.trademodel.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Missed Opportunity 事件窄表（与 tm_missed_opportunity 对应）。
 */
public class MissedOpportunityDO {

    private String missedId;
    private String decisionId;
    private String analysisId;
    private String symbol;
    private LocalDate bizDate;
    private String reasonJson;
    private String ruleVersion;
    private String traceId;
    private LocalDateTime createTime;

    public String getMissedId() {
        return missedId;
    }

    public void setMissedId(String missedId) {
        this.missedId = missedId;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getBizDate() {
        return bizDate;
    }

    public void setBizDate(LocalDate bizDate) {
        this.bizDate = bizDate;
    }

    public String getReasonJson() {
        return reasonJson;
    }

    public void setReasonJson(String reasonJson) {
        this.reasonJson = reasonJson;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
