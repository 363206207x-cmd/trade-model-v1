package org.example.trademodel.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推送时刻快照，与表 tm_push_snapshot 对齐；持久化经 MyBatis {@code PushSnapshotMapper}。
 */
public class TmPushSnapshotDO {

    private Long pushId;
    private String analysisId;
    private String symbol;
    private String timeframe;
    private String pushType;
    private String pushStatus;
    private LocalDateTime pushCreateTime;
    private String ruleVersion;
    private BigDecimal triggerPrice;
    private String entryZoneJson;
    private String stopZoneJson;
    private String invalidationConditionJson;
    private String planModeSnapshot;
    private String causeEffectAlignmentSnapshot;
    private Integer executionFeasibilitySnapshot;
    private Integer dataQualityScoreSnapshot;
    private Integer confusedScoreSnapshot;
    private Long accountRiskSnapshotId;
    /** UTC-naive compatibility timestamp; produce and compare only through the shared UTC policy. */
    private LocalDateTime expiresAt;
    private String traceId;
    private LocalDateTime createTime;

    public Long getPushId() {
        return pushId;
    }

    public void setPushId(Long pushId) {
        this.pushId = pushId;
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

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getPushType() {
        return pushType;
    }

    public void setPushType(String pushType) {
        this.pushType = pushType;
    }

    public String getPushStatus() {
        return pushStatus;
    }

    public void setPushStatus(String pushStatus) {
        this.pushStatus = pushStatus;
    }

    public LocalDateTime getPushCreateTime() {
        return pushCreateTime;
    }

    public void setPushCreateTime(LocalDateTime pushCreateTime) {
        this.pushCreateTime = pushCreateTime;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public BigDecimal getTriggerPrice() {
        return triggerPrice;
    }

    public void setTriggerPrice(BigDecimal triggerPrice) {
        this.triggerPrice = triggerPrice;
    }

    public String getEntryZoneJson() {
        return entryZoneJson;
    }

    public void setEntryZoneJson(String entryZoneJson) {
        this.entryZoneJson = entryZoneJson;
    }

    public String getStopZoneJson() {
        return stopZoneJson;
    }

    public void setStopZoneJson(String stopZoneJson) {
        this.stopZoneJson = stopZoneJson;
    }

    public String getInvalidationConditionJson() {
        return invalidationConditionJson;
    }

    public void setInvalidationConditionJson(String invalidationConditionJson) {
        this.invalidationConditionJson = invalidationConditionJson;
    }

    public String getPlanModeSnapshot() {
        return planModeSnapshot;
    }

    public void setPlanModeSnapshot(String planModeSnapshot) {
        this.planModeSnapshot = planModeSnapshot;
    }

    public String getCauseEffectAlignmentSnapshot() {
        return causeEffectAlignmentSnapshot;
    }

    public void setCauseEffectAlignmentSnapshot(String causeEffectAlignmentSnapshot) {
        this.causeEffectAlignmentSnapshot = causeEffectAlignmentSnapshot;
    }

    public Integer getExecutionFeasibilitySnapshot() {
        return executionFeasibilitySnapshot;
    }

    public void setExecutionFeasibilitySnapshot(Integer executionFeasibilitySnapshot) {
        this.executionFeasibilitySnapshot = executionFeasibilitySnapshot;
    }

    public Integer getDataQualityScoreSnapshot() {
        return dataQualityScoreSnapshot;
    }

    public void setDataQualityScoreSnapshot(Integer dataQualityScoreSnapshot) {
        this.dataQualityScoreSnapshot = dataQualityScoreSnapshot;
    }

    public Integer getConfusedScoreSnapshot() {
        return confusedScoreSnapshot;
    }

    public void setConfusedScoreSnapshot(Integer confusedScoreSnapshot) {
        this.confusedScoreSnapshot = confusedScoreSnapshot;
    }

    public Long getAccountRiskSnapshotId() {
        return accountRiskSnapshotId;
    }

    public void setAccountRiskSnapshotId(Long accountRiskSnapshotId) {
        this.accountRiskSnapshotId = accountRiskSnapshotId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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
