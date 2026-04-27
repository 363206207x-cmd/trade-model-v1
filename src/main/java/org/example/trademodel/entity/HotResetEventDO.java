package org.example.trademodel.entity;

import java.time.LocalDateTime;

/**
 * Hot Reset 最小事件行：按 analysisId/traceId 追溯，不承载复杂时间线语义。
 */
public class HotResetEventDO {

    private String eventId;
    private String analysisId;
    private String traceId;
    private String symbol;
    private String triggerType;
    private String triggerValue;
    private String decisionId;
    private String decisionState;
    private Integer confusedScoreSnapshot;
    private Boolean multiTimeframeAlignedSnapshot;
    private String triggerReasonCode;
    private String triggerReasonText;
    private Integer eventVersion;
    private LocalDateTime eventTime;
    private String preState;
    private String postState;
    private LocalDateTime createTime;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
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

    public Integer getConfusedScoreSnapshot() {
        return confusedScoreSnapshot;
    }

    public void setConfusedScoreSnapshot(Integer confusedScoreSnapshot) {
        this.confusedScoreSnapshot = confusedScoreSnapshot;
    }

    public Boolean getMultiTimeframeAlignedSnapshot() {
        return multiTimeframeAlignedSnapshot;
    }

    public void setMultiTimeframeAlignedSnapshot(Boolean multiTimeframeAlignedSnapshot) {
        this.multiTimeframeAlignedSnapshot = multiTimeframeAlignedSnapshot;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
