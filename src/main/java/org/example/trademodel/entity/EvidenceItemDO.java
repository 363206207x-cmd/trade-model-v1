package org.example.trademodel.entity;
import java.time.LocalDateTime;

public class EvidenceItemDO {
    private String evidenceId;
    private String analysisId;
    private String evidenceType;
    private String description;
    private String direction;
    private Double strength;
    private Double confidence;
    private String source;
    private String sourceProvider;
    private String sourceReference;
    private String sourceTraceId;
    private String externalEventId;
    private String externalEventType;
    private LocalDateTime eventWindowStart;
    private LocalDateTime eventWindowEnd;
    private Integer impactScore;
    private String severity;
    private LocalDateTime createTime;

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Double getStrength() { return strength; }
    public void setStrength(Double strength) { this.strength = strength; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceProvider() { return sourceProvider; }
    public void setSourceProvider(String sourceProvider) { this.sourceProvider = sourceProvider; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public String getSourceTraceId() { return sourceTraceId; }
    public void setSourceTraceId(String sourceTraceId) { this.sourceTraceId = sourceTraceId; }
    public String getExternalEventId() { return externalEventId; }
    public void setExternalEventId(String externalEventId) { this.externalEventId = externalEventId; }
    public String getExternalEventType() { return externalEventType; }
    public void setExternalEventType(String externalEventType) { this.externalEventType = externalEventType; }
    public LocalDateTime getEventWindowStart() { return eventWindowStart; }
    public void setEventWindowStart(LocalDateTime eventWindowStart) { this.eventWindowStart = eventWindowStart; }
    public LocalDateTime getEventWindowEnd() { return eventWindowEnd; }
    public void setEventWindowEnd(LocalDateTime eventWindowEnd) { this.eventWindowEnd = eventWindowEnd; }
    public Integer getImpactScore() { return impactScore; }
    public void setImpactScore(Integer impactScore) { this.impactScore = impactScore; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
