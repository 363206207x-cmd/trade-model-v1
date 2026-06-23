package org.example.trademodel.service.support;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExternalContextSnapshot {
    private String status = "READY";
    private String sourceHealth = ExternalContextPolicy.SOURCE_HEALTH_OK;
    private int activeExternalEventCount;
    private int activeMacroEventCount;
    private int activeNewsEventCount;
    private String riskLevel = ExternalContextPolicy.RISK_LOW;
    private boolean externalContextBlocked;
    private List<String> externalEventIds = new ArrayList<>();
    private List<String> reasonCodes = new ArrayList<>();
    private LocalDateTime nextExternalEventTime;
    private LocalDateTime latestExternalEventTime;
    private String latestExternalEventLabel;
    private LocalDateTime eventWindowStart;
    private LocalDateTime eventWindowEnd;
    private boolean reviewOnly = true;
    private boolean manualReviewOnly = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoTrading = true;
    private boolean notOrderExecution = true;
    private boolean notUserPositionCreation = true;
    private boolean notUserPositionMutation = true;
    private boolean notPushSend = true;
    private boolean notExternalChannel = true;
    private boolean notExternalFetch = true;

    public void addReason(String reason) {
        if (reason != null && !reason.isBlank() && !reasonCodes.contains(reason)) {
            reasonCodes.add(reason);
        }
    }

    public void addEventId(String eventId) {
        if (eventId != null && !eventId.isBlank() && !externalEventIds.contains(eventId)) {
            externalEventIds.add(eventId);
        }
    }

    public Map<String, Object> toDashboardStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("runtimeStatus", status);
        m.put("sourceHealth", sourceHealth);
        m.put("activeExternalEventCount", activeExternalEventCount);
        m.put("activeMacroEventCount", activeMacroEventCount);
        m.put("activeNewsEventCount", activeNewsEventCount);
        m.put("externalContextRiskLevel", riskLevel);
        m.put("externalContextBlocked", externalContextBlocked);
        m.put("externalEventIds", getExternalEventIds());
        m.put("reasonCodes", getReasonCodes());
        m.put("nextExternalEventTime", nextExternalEventTime);
        m.put("latestExternalEventTime", latestExternalEventTime);
        m.put("latestExternalEventLabel", latestExternalEventLabel);
        m.put("eventWindowStart", eventWindowStart);
        m.put("eventWindowEnd", eventWindowEnd);
        m.put("reviewOnly", reviewOnly);
        m.put("manualReviewOnly", manualReviewOnly);
        m.put("notTradeInstruction", notTradeInstruction);
        m.put("notExecutable", notExecutable);
        m.put("notAutoTrading", notAutoTrading);
        m.put("notOrderExecution", notOrderExecution);
        m.put("notUserPositionCreation", notUserPositionCreation);
        m.put("notUserPositionMutation", notUserPositionMutation);
        m.put("notPushSend", notPushSend);
        m.put("notExternalChannel", notExternalChannel);
        m.put("notExternalFetch", notExternalFetch);
        return m;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceHealth() { return sourceHealth; }
    public void setSourceHealth(String sourceHealth) { this.sourceHealth = sourceHealth; }
    public int getActiveExternalEventCount() { return activeExternalEventCount; }
    public void setActiveExternalEventCount(int activeExternalEventCount) { this.activeExternalEventCount = activeExternalEventCount; }
    public int getActiveMacroEventCount() { return activeMacroEventCount; }
    public void setActiveMacroEventCount(int activeMacroEventCount) { this.activeMacroEventCount = activeMacroEventCount; }
    public int getActiveNewsEventCount() { return activeNewsEventCount; }
    public void setActiveNewsEventCount(int activeNewsEventCount) { this.activeNewsEventCount = activeNewsEventCount; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public boolean isExternalContextBlocked() { return externalContextBlocked; }
    public void setExternalContextBlocked(boolean externalContextBlocked) { this.externalContextBlocked = externalContextBlocked; }
    public List<String> getExternalEventIds() { return Collections.unmodifiableList(externalEventIds); }
    public void setExternalEventIds(List<String> externalEventIds) { this.externalEventIds = externalEventIds == null ? new ArrayList<>() : new ArrayList<>(externalEventIds); }
    public List<String> getReasonCodes() { return Collections.unmodifiableList(reasonCodes); }
    public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes == null ? new ArrayList<>() : new ArrayList<>(reasonCodes); }
    public LocalDateTime getNextExternalEventTime() { return nextExternalEventTime; }
    public void setNextExternalEventTime(LocalDateTime nextExternalEventTime) { this.nextExternalEventTime = nextExternalEventTime; }
    public LocalDateTime getLatestExternalEventTime() { return latestExternalEventTime; }
    public void setLatestExternalEventTime(LocalDateTime latestExternalEventTime) { this.latestExternalEventTime = latestExternalEventTime; }
    public String getLatestExternalEventLabel() { return latestExternalEventLabel; }
    public void setLatestExternalEventLabel(String latestExternalEventLabel) { this.latestExternalEventLabel = latestExternalEventLabel; }
    public LocalDateTime getEventWindowStart() { return eventWindowStart; }
    public void setEventWindowStart(LocalDateTime eventWindowStart) { this.eventWindowStart = eventWindowStart; }
    public LocalDateTime getEventWindowEnd() { return eventWindowEnd; }
    public void setEventWindowEnd(LocalDateTime eventWindowEnd) { this.eventWindowEnd = eventWindowEnd; }
    public boolean isReviewOnly() { return reviewOnly; }
    public boolean isManualReviewOnly() { return manualReviewOnly; }
    public boolean isNotTradeInstruction() { return notTradeInstruction; }
    public boolean isNotExecutable() { return notExecutable; }
    public boolean isNotAutoTrading() { return notAutoTrading; }
    public boolean isNotOrderExecution() { return notOrderExecution; }
    public boolean isNotUserPositionCreation() { return notUserPositionCreation; }
    public boolean isNotUserPositionMutation() { return notUserPositionMutation; }
    public boolean isNotPushSend() { return notPushSend; }
    public boolean isNotExternalChannel() { return notExternalChannel; }
    public boolean isNotExternalFetch() { return notExternalFetch; }
}
