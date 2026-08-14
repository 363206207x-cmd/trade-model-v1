package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class EventAssetRelationDO {
    private String relationId;
    private String eventType;
    private String eventId;
    private Long assetId;
    private String symbol;
    private String planId;
    private String relationType;
    private String sourceReference;
    private String traceId;
    private LocalDateTime createdAt;

    public String getRelationId() { return relationId; }
    public void setRelationId(String value) { this.relationId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { this.eventType = value; }
    public String getEventId() { return eventId; }
    public void setEventId(String value) { this.eventId = value; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long value) { this.assetId = value; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String value) { this.symbol = value; }
    public String getPlanId() { return planId; }
    public void setPlanId(String value) { this.planId = value; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String value) { this.relationType = value; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String value) { this.sourceReference = value; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String value) { this.traceId = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
