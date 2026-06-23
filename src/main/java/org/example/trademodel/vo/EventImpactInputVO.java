package org.example.trademodel.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private String externalContextStatus;
    private Integer activeExternalEventCount = 0;
    private Integer activeMacroEventCount = 0;
    private Integer activeNewsEventCount = 0;
    private String externalContextRiskLevel;
    private Boolean externalContextBlocked = false;
    private List<String> externalEventIds = new ArrayList<>();
    private List<String> externalContextReasonCodes = new ArrayList<>();
    private LocalDateTime nextExternalEventTime;
    private LocalDateTime latestExternalEventTime;
    private String latestExternalEventLabel;
    private LocalDateTime externalEventWindowStart;
    private LocalDateTime externalEventWindowEnd;
    private String externalContextSourceHealth;

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

    public String getExternalContextStatus() { return externalContextStatus; }
    public void setExternalContextStatus(String externalContextStatus) { this.externalContextStatus = externalContextStatus; }
    public Integer getActiveExternalEventCount() { return activeExternalEventCount; }
    public void setActiveExternalEventCount(Integer activeExternalEventCount) { this.activeExternalEventCount = activeExternalEventCount; }
    public Integer getActiveMacroEventCount() { return activeMacroEventCount; }
    public void setActiveMacroEventCount(Integer activeMacroEventCount) { this.activeMacroEventCount = activeMacroEventCount; }
    public Integer getActiveNewsEventCount() { return activeNewsEventCount; }
    public void setActiveNewsEventCount(Integer activeNewsEventCount) { this.activeNewsEventCount = activeNewsEventCount; }
    public String getExternalContextRiskLevel() { return externalContextRiskLevel; }
    public void setExternalContextRiskLevel(String externalContextRiskLevel) { this.externalContextRiskLevel = externalContextRiskLevel; }
    public Boolean getExternalContextBlocked() { return externalContextBlocked; }
    public void setExternalContextBlocked(Boolean externalContextBlocked) { this.externalContextBlocked = externalContextBlocked; }
    public List<String> getExternalEventIds() { return Collections.unmodifiableList(externalEventIds); }
    public void setExternalEventIds(List<String> externalEventIds) { this.externalEventIds = externalEventIds == null ? new ArrayList<>() : new ArrayList<>(externalEventIds); }
    public List<String> getExternalContextReasonCodes() { return Collections.unmodifiableList(externalContextReasonCodes); }
    public void setExternalContextReasonCodes(List<String> externalContextReasonCodes) { this.externalContextReasonCodes = externalContextReasonCodes == null ? new ArrayList<>() : new ArrayList<>(externalContextReasonCodes); }
    public LocalDateTime getNextExternalEventTime() { return nextExternalEventTime; }
    public void setNextExternalEventTime(LocalDateTime nextExternalEventTime) { this.nextExternalEventTime = nextExternalEventTime; }
    public LocalDateTime getLatestExternalEventTime() { return latestExternalEventTime; }
    public void setLatestExternalEventTime(LocalDateTime latestExternalEventTime) { this.latestExternalEventTime = latestExternalEventTime; }
    public String getLatestExternalEventLabel() { return latestExternalEventLabel; }
    public void setLatestExternalEventLabel(String latestExternalEventLabel) { this.latestExternalEventLabel = latestExternalEventLabel; }
    public LocalDateTime getExternalEventWindowStart() { return externalEventWindowStart; }
    public void setExternalEventWindowStart(LocalDateTime externalEventWindowStart) { this.externalEventWindowStart = externalEventWindowStart; }
    public LocalDateTime getExternalEventWindowEnd() { return externalEventWindowEnd; }
    public void setExternalEventWindowEnd(LocalDateTime externalEventWindowEnd) { this.externalEventWindowEnd = externalEventWindowEnd; }
    public String getExternalContextSourceHealth() { return externalContextSourceHealth; }
    public void setExternalContextSourceHealth(String externalContextSourceHealth) { this.externalContextSourceHealth = externalContextSourceHealth; }
}
