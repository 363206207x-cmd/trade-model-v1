package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BoundaryStopDTO {
    private BigDecimal stopLoss;
    private BoundaryStopTypeEnum stopType;
    private String stopSource;
    private String stopTimeframe;
    private BigDecimal stopBufferValue;
    private String stopReason;
    private List<String> stopSourceFields = new ArrayList<>();

    public BigDecimal getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(BigDecimal stopLoss) {
        this.stopLoss = stopLoss;
    }

    public BoundaryStopTypeEnum getStopType() {
        return stopType;
    }

    public void setStopType(BoundaryStopTypeEnum stopType) {
        this.stopType = stopType;
    }

    public String getStopSource() {
        return stopSource;
    }

    public void setStopSource(String stopSource) {
        this.stopSource = stopSource;
    }

    public String getStopTimeframe() {
        return stopTimeframe;
    }

    public void setStopTimeframe(String stopTimeframe) {
        this.stopTimeframe = stopTimeframe;
    }

    public BigDecimal getStopBufferValue() {
        return stopBufferValue;
    }

    public void setStopBufferValue(BigDecimal stopBufferValue) {
        this.stopBufferValue = stopBufferValue;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public List<String> getStopSourceFields() {
        return stopSourceFields;
    }

    public void setStopSourceFields(List<String> stopSourceFields) {
        this.stopSourceFields = stopSourceFields;
    }
}
