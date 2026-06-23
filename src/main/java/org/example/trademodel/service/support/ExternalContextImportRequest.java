package org.example.trademodel.service.support;

import java.time.LocalDateTime;

public class ExternalContextImportRequest {
    private String eventId;
    private String eventType;
    private String title;
    private String description;
    private String headline;
    private String summary;
    private String affectedSymbols;
    private String marketScope;
    private LocalDateTime eventTime;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer impactScore;
    private String severity;
    private String direction;
    private String provider;
    private String sourceType;
    private String sourceReference;
    private String sourceTraceId;
    private String sourceEventId;
    private String sourceHash;
    private LocalDateTime sourcePublishedAt;
    private String status;
    private Boolean executionBlocking;
    private String dedupeKey;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getAffectedSymbols() { return affectedSymbols; }
    public void setAffectedSymbols(String affectedSymbols) { this.affectedSymbols = affectedSymbols; }
    public String getMarketScope() { return marketScope; }
    public void setMarketScope(String marketScope) { this.marketScope = marketScope; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) { this.windowStart = windowStart; }
    public LocalDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(LocalDateTime windowEnd) { this.windowEnd = windowEnd; }
    public Integer getImpactScore() { return impactScore; }
    public void setImpactScore(Integer impactScore) { this.impactScore = impactScore; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public String getSourceTraceId() { return sourceTraceId; }
    public void setSourceTraceId(String sourceTraceId) { this.sourceTraceId = sourceTraceId; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
    public LocalDateTime getSourcePublishedAt() { return sourcePublishedAt; }
    public void setSourcePublishedAt(LocalDateTime sourcePublishedAt) { this.sourcePublishedAt = sourcePublishedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getExecutionBlocking() { return executionBlocking; }
    public void setExecutionBlocking(Boolean executionBlocking) { this.executionBlocking = executionBlocking; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
}
