package org.example.trademodel.vo;

import java.time.LocalDateTime;

/**
 * 事件冲击输入契约（第二刀最小实现）：仅承载可计算/可追溯输入，不承载评分公式。
 */
public class EventImpactInputVO {
    private Boolean eventFactHit;
    private Integer eventFactCount;
    private LocalDateTime eventLatestTime;
    private String eventReasonCode;
    private String eventTriggerType;
    private Integer eventVersion;
    private String eventTraceId;

    public Boolean getEventFactHit() {
        return eventFactHit;
    }

    public void setEventFactHit(Boolean eventFactHit) {
        this.eventFactHit = eventFactHit;
    }

    public Integer getEventFactCount() {
        return eventFactCount;
    }

    public void setEventFactCount(Integer eventFactCount) {
        this.eventFactCount = eventFactCount;
    }

    public LocalDateTime getEventLatestTime() {
        return eventLatestTime;
    }

    public void setEventLatestTime(LocalDateTime eventLatestTime) {
        this.eventLatestTime = eventLatestTime;
    }

    public String getEventReasonCode() {
        return eventReasonCode;
    }

    public void setEventReasonCode(String eventReasonCode) {
        this.eventReasonCode = eventReasonCode;
    }

    public String getEventTriggerType() {
        return eventTriggerType;
    }

    public void setEventTriggerType(String eventTriggerType) {
        this.eventTriggerType = eventTriggerType;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Integer eventVersion) {
        this.eventVersion = eventVersion;
    }

    public String getEventTraceId() {
        return eventTraceId;
    }

    public void setEventTraceId(String eventTraceId) {
        this.eventTraceId = eventTraceId;
    }
}
