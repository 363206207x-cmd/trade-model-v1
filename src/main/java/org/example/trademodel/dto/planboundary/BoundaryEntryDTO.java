package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;

public class BoundaryEntryDTO {

    private String entryType;
    private BigDecimal entryPrice;
    private BigDecimal entryZoneLow;
    private BigDecimal entryZoneHigh;
    private String numericSourceType;
    private BigDecimal numericSourceValue;
    private String sourceTimeframe;
    private String reason;

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getEntryZoneLow() {
        return entryZoneLow;
    }

    public void setEntryZoneLow(BigDecimal entryZoneLow) {
        this.entryZoneLow = entryZoneLow;
    }

    public BigDecimal getEntryZoneHigh() {
        return entryZoneHigh;
    }

    public void setEntryZoneHigh(BigDecimal entryZoneHigh) {
        this.entryZoneHigh = entryZoneHigh;
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
