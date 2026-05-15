package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;

public class BoundaryStopDTO {

    private String stopType;
    private BigDecimal stopPrice;
    private BigDecimal stopZoneLow;
    private BigDecimal stopZoneHigh;
    private String numericSourceType;
    private BigDecimal numericSourceValue;
    private String sourceTimeframe;
    private String reason;

    public String getStopType() {
        return stopType;
    }

    public void setStopType(String stopType) {
        this.stopType = stopType;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    public BigDecimal getStopZoneLow() {
        return stopZoneLow;
    }

    public void setStopZoneLow(BigDecimal stopZoneLow) {
        this.stopZoneLow = stopZoneLow;
    }

    public BigDecimal getStopZoneHigh() {
        return stopZoneHigh;
    }

    public void setStopZoneHigh(BigDecimal stopZoneHigh) {
        this.stopZoneHigh = stopZoneHigh;
    }

    public String getNumericSourceType() {
        return numericSourceType;
    }

    public void setNumericSourceType(String numericSourceType) {
        this.numericSourceType = numericSourceType;
    }

    public BigDecimal getNumericSourceValue() {
        return numericSourceValue;
    }

    public void setNumericSourceValue(BigDecimal numericSourceValue) {
        this.numericSourceValue = numericSourceValue;
    }

    public String getSourceTimeframe() {
        return sourceTimeframe;
    }

    public void setSourceTimeframe(String sourceTimeframe) {
        this.sourceTimeframe = sourceTimeframe;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
