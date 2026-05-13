package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;

public class BoundaryTakeProfitLevelDTO {
    private Integer level;
    private BigDecimal price;
    private BigDecimal rr;
    private String source;
    private String reason;
    private BoundaryNumericSourceDTO numericSource;
    private String sourceTimeframe;
    private String sourceRef;
    private BigDecimal partialRatio;
    private BigDecimal allocationRatio;

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRr() {
        return rr;
    }

    public void setRr(BigDecimal rr) {
        this.rr = rr;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BoundaryNumericSourceDTO getNumericSource() {
        return numericSource;
    }

    public void setNumericSource(BoundaryNumericSourceDTO numericSource) {
        this.numericSource = numericSource;
    }

    public String getSourceTimeframe() {
        return sourceTimeframe;
    }

    public void setSourceTimeframe(String sourceTimeframe) {
        this.sourceTimeframe = sourceTimeframe;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public BigDecimal getPartialRatio() {
        return partialRatio;
    }

    public void setPartialRatio(BigDecimal partialRatio) {
        this.partialRatio = partialRatio;
    }

    public BigDecimal getAllocationRatio() {
        return allocationRatio;
    }

    public void setAllocationRatio(BigDecimal allocationRatio) {
        this.allocationRatio = allocationRatio;
    }
}
