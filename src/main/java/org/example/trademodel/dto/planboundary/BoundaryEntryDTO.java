package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BoundaryEntryDTO {
    private BoundaryEntryTypeEnum entryType;
    private BigDecimal entryZoneLow;
    private BigDecimal entryZoneHigh;
    private BigDecimal entryReferencePrice;
    private String entrySource;
    private String entryTimeframe;
    private String entryReason;
    private List<String> entrySourceFields = new ArrayList<>();

    public BoundaryEntryTypeEnum getEntryType() {
        return entryType;
    }

    public void setEntryType(BoundaryEntryTypeEnum entryType) {
        this.entryType = entryType;
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

    public BigDecimal getEntryReferencePrice() {
        return entryReferencePrice;
    }

    public void setEntryReferencePrice(BigDecimal entryReferencePrice) {
        this.entryReferencePrice = entryReferencePrice;
    }

    public String getEntrySource() {
        return entrySource;
    }

    public void setEntrySource(String entrySource) {
        this.entrySource = entrySource;
    }

    public String getEntryTimeframe() {
        return entryTimeframe;
    }

    public void setEntryTimeframe(String entryTimeframe) {
        this.entryTimeframe = entryTimeframe;
    }

    public String getEntryReason() {
        return entryReason;
    }

    public void setEntryReason(String entryReason) {
        this.entryReason = entryReason;
    }

    public List<String> getEntrySourceFields() {
        return entrySourceFields;
    }

    public void setEntrySourceFields(List<String> entrySourceFields) {
        this.entrySourceFields = entrySourceFields;
    }
}
