package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;

public class BoundaryLevelDTO {

    private BigDecimal price;
    private String levelType;
    private String timeframe;
    private Long barTime;
    private String sourceRef;
    private String reason;
    private Integer strength;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getLevelType() {
        return levelType;
    }

    public void setLevelType(String levelType) {
        this.levelType = levelType;
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

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getStrength() {
        return strength;
    }

    public void setStrength(Integer strength) {
        this.strength = strength;
    }
}
