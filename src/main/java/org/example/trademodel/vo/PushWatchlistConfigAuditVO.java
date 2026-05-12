package org.example.trademodel.vo;

import java.time.LocalDateTime;

public class PushWatchlistConfigAuditVO {
    private Long auditId;
    private String ruleKey;
    private String beforeSymbols;
    private String afterSymbols;
    private Boolean beforeEnabled;
    private Boolean afterEnabled;
    private String changedBy;
    private String changeReason;
    private String source;
    private String traceId;
    private String ruleVersion;
    private LocalDateTime createTime;

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public String getBeforeSymbols() {
        return beforeSymbols;
    }

    public void setBeforeSymbols(String beforeSymbols) {
        this.beforeSymbols = beforeSymbols;
    }

    public String getAfterSymbols() {
        return afterSymbols;
    }

    public void setAfterSymbols(String afterSymbols) {
        this.afterSymbols = afterSymbols;
    }

    public Boolean getBeforeEnabled() {
        return beforeEnabled;
    }

    public void setBeforeEnabled(Boolean beforeEnabled) {
        this.beforeEnabled = beforeEnabled;
    }

    public Boolean getAfterEnabled() {
        return afterEnabled;
    }

    public void setAfterEnabled(Boolean afterEnabled) {
        this.afterEnabled = afterEnabled;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
