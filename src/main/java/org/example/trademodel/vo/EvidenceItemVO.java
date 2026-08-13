package org.example.trademodel.vo;

import java.time.LocalDateTime;

public class EvidenceItemVO {
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
    private String timestamp;
    private String currentValue;
    private String changeFromBaseline;
    private LocalDateTime observedAt;
    private String freshness;

    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    public void setStrengthLevel(String level) { this.strength = 80.0; }
    public void setConfidenceScore(int score) { this.confidence = (double) score; }
    public void setTitle(String title) { this.description = title; }

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }
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
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getCurrentValue() { return currentValue; }
    public void setCurrentValue(String value) { this.currentValue = value; }
    public String getChangeFromBaseline() { return changeFromBaseline; }
    public void setChangeFromBaseline(String value) { this.changeFromBaseline = value; }
    public LocalDateTime getObservedAt() { return observedAt; }
    public void setObservedAt(LocalDateTime value) { this.observedAt = value; }
    public String getFreshness() { return freshness; }
    public void setFreshness(String value) { this.freshness = value; }
}
