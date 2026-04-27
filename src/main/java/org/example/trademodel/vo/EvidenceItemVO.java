package org.example.trademodel.vo;

public class EvidenceItemVO {
    private String evidenceId;
    private String evidenceType;
    private String description;
    private String direction;
    private Double strength;
    private Double confidence;
    private String source;
    private String timestamp;

    public void setAnalysisId(String analysisId) { this.evidenceId = analysisId; }
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
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
