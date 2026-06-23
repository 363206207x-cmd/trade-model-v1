package org.example.trademodel.entity;

public class MacroEventDO extends ExternalContextEventDO {
    private String eventType;
    private String title;
    private String description;
    private String sourcePublishedAtReasonCode;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourcePublishedAtReasonCode() { return sourcePublishedAtReasonCode; }
    public void setSourcePublishedAtReasonCode(String sourcePublishedAtReasonCode) { this.sourcePublishedAtReasonCode = sourcePublishedAtReasonCode; }
}
