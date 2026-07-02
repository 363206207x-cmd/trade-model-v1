package org.example.trademodel.dto.planboundary;

public class BoundarySourceRefDTO {

    private String sourceType;
    private String timeframe;
    private Long barTime;
    private Integer index;
    private String reason;

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
}
