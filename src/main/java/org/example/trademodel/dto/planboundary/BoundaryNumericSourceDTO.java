package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;

public class BoundaryNumericSourceDTO {
    private String sourceType;
    private BigDecimal sourceValue;
    private String sourceTimeframe;
    private String sourceReason;
    private String sourceField;
    private String sourceRef;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public BigDecimal getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(BigDecimal sourceValue) {
        this.sourceValue = sourceValue;
    }

    public String getSourceTimeframe() {
        return sourceTimeframe;
    }

    public void setSourceTimeframe(String sourceTimeframe) {
        this.sourceTimeframe = sourceTimeframe;
    }

    public String getSourceReason() {
        return sourceReason;
    }

    public void setSourceReason(String sourceReason) {
        this.sourceReason = sourceReason;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }
}
