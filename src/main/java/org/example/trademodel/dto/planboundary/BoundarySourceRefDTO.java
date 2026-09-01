package org.example.trademodel.dto.planboundary;

public class BoundarySourceRefDTO {

    private String sourceType;
    private String timeframe;
    private Long barTime;
    private Integer index;
    private String reason;
    private String sourceId;
    private String provider;
    private String observedAt;
    private String structureId;
    private String calculationReason;
    private String analysisId;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Long getBarTime() {
        return barTime;
    }

    public void setBarTime(Long barTime) {
        this.barTime = barTime;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String value) { this.sourceId = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { this.provider = value; }
    public String getObservedAt() { return observedAt; }
    public void setObservedAt(String value) { this.observedAt = value; }
    public String getStructureId() { return structureId; }
    public void setStructureId(String value) { this.structureId = value; }
    public String getCalculationReason() { return calculationReason; }
    public void setCalculationReason(String value) { this.calculationReason = value; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String value) { this.analysisId = value; }
}
