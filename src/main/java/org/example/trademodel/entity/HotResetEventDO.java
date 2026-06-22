package org.example.trademodel.entity;

import java.time.LocalDateTime;

/**
 * Hot Reset 最小事件行：按 analysisId/traceId 追溯，不承载复杂时间线语义。
 */
public class HotResetEventDO {

    private String eventId;
    private String eventKey;
    private String analysisId;
    private String rebuildAnalysisId;
    private String traceId;
    private String symbol;
    private String timeframe;
    private String triggerType;
    private String triggerValue;
    private String sourceType;
    private String sourceReference;
    private Integer severityScore;
    private String decisionId;
    private String decisionState;
    private Integer decisionInvalidatedCount;
    private Integer planRevalidationCount;
    private Integer pushInvalidatedCount;
    private Integer confusedScoreSnapshot;
    private Integer confusedScoreBefore;
    private Integer confusedScoreAfter;
    private Boolean multiTimeframeAlignedSnapshot;
    private String accountRiskStatus;
    private String accountRiskLevel;
    private Boolean accountRiskBlocked;
    private String accountRiskSnapshot;
    private Boolean rebuildTriggered;
    private String executionStatus;
    private String executionErrorCode;
    private String executionErrorMessage;
    private String triggerReasonCode;
    private String triggerReasonText;
    private Integer eventVersion;
    private LocalDateTime eventTime;
    private String preState;
    private String postState;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getRebuildAnalysisId() {
        return rebuildAnalysisId;
    }

    public void setRebuildAnalysisId(String rebuildAnalysisId) {
        this.rebuildAnalysisId = rebuildAnalysisId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public Integer getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(Integer severityScore) {
        this.severityScore = severityScore;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getDecisionState() {
        return decisionState;
    }

    public void setDecisionState(String decisionState) {
        this.decisionState = decisionState;
    }

    public Integer getDecisionInvalidatedCount() {
        return decisionInvalidatedCount;
    }

    public void setDecisionInvalidatedCount(Integer decisionInvalidatedCount) {
        this.decisionInvalidatedCount = decisionInvalidatedCount;
    }

    public Integer getPlanRevalidationCount() {
        return planRevalidationCount;
    }

    public void setPlanRevalidationCount(Integer planRevalidationCount) {
        this.planRevalidationCount = planRevalidationCount;
    }

    public Integer getPushInvalidatedCount() {
        return pushInvalidatedCount;
    }

    public void setPushInvalidatedCount(Integer pushInvalidatedCount) {
        this.pushInvalidatedCount = pushInvalidatedCount;
    }

    public Integer getConfusedScoreSnapshot() {
        return confusedScoreSnapshot;
    }

    public void setConfusedScoreSnapshot(Integer confusedScoreSnapshot) {
        this.confusedScoreSnapshot = confusedScoreSnapshot;
    }

    public Integer getConfusedScoreBefore() {
        return confusedScoreBefore;
    }

    public void setConfusedScoreBefore(Integer confusedScoreBefore) {
        this.confusedScoreBefore = confusedScoreBefore;
    }

    public Integer getConfusedScoreAfter() {
        return confusedScoreAfter;
    }

    public void setConfusedScoreAfter(Integer confusedScoreAfter) {
        this.confusedScoreAfter = confusedScoreAfter;
    }

    public Boolean getMultiTimeframeAlignedSnapshot() {
        return multiTimeframeAlignedSnapshot;
    }

    public void setMultiTimeframeAlignedSnapshot(Boolean multiTimeframeAlignedSnapshot) {
        this.multiTimeframeAlignedSnapshot = multiTimeframeAlignedSnapshot;
    }

    public String getAccountRiskStatus() {
        return accountRiskStatus;
    }

    public void setAccountRiskStatus(String accountRiskStatus) {
        this.accountRiskStatus = accountRiskStatus;
    }

    public String getAccountRiskLevel() {
        return accountRiskLevel;
    }

    public void setAccountRiskLevel(String accountRiskLevel) {
        this.accountRiskLevel = accountRiskLevel;
    }

    public Boolean getAccountRiskBlocked() {
        return accountRiskBlocked;
    }

    public void setAccountRiskBlocked(Boolean accountRiskBlocked) {
        this.accountRiskBlocked = accountRiskBlocked;
    }

    public String getAccountRiskSnapshot() {
        return accountRiskSnapshot;
    }

    public void setAccountRiskSnapshot(String accountRiskSnapshot) {
        this.accountRiskSnapshot = accountRiskSnapshot;
    }

    public Boolean getRebuildTriggered() {
        return rebuildTriggered;
    }

    public void setRebuildTriggered(Boolean rebuildTriggered) {
        this.rebuildTriggered = rebuildTriggered;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getExecutionErrorCode() {
        return executionErrorCode;
    }

    public void setExecutionErrorCode(String executionErrorCode) {
        this.executionErrorCode = executionErrorCode;
    }

    public String getExecutionErrorMessage() {
        return executionErrorMessage;
    }

    public void setExecutionErrorMessage(String executionErrorMessage) {
        this.executionErrorMessage = executionErrorMessage;
    }

    public String getTriggerReasonCode() {
        return triggerReasonCode;
    }

    public void setTriggerReasonCode(String triggerReasonCode) {
        this.triggerReasonCode = triggerReasonCode;
    }

    public String getTriggerReasonText() {
        return triggerReasonText;
    }

    public void setTriggerReasonText(String triggerReasonText) {
        this.triggerReasonText = triggerReasonText;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Integer eventVersion) {
        this.eventVersion = eventVersion;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getPreState() {
        return preState;
    }

    public void setPreState(String preState) {
        this.preState = preState;
    }

    public String getPostState() {
        return postState;
    }

    public void setPostState(String postState) {
        this.postState = postState;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
