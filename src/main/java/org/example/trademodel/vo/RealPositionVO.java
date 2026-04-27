package org.example.trademodel.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RealPositionVO {

    private String symbol;
    private String positionSide;
    private BigDecimal avgOpenPrice;
    private LocalDateTime positionOpenTime;
    private BigDecimal positionQuantity;
    private BigDecimal unrealizedPnlPct;
    private String positionStatus;
    private BigDecimal markPrice;
    private BigDecimal breakEvenPrice;
    private BigDecimal liquidationPrice;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public BigDecimal getAvgOpenPrice() {
        return avgOpenPrice;
    }

    public void setAvgOpenPrice(BigDecimal avgOpenPrice) {
        this.avgOpenPrice = avgOpenPrice;
    }

    public LocalDateTime getPositionOpenTime() {
        return positionOpenTime;
    }

    public void setPositionOpenTime(LocalDateTime positionOpenTime) {
        this.positionOpenTime = positionOpenTime;
    }

    public BigDecimal getPositionQuantity() {
        return positionQuantity;
    }

    public void setPositionQuantity(BigDecimal positionQuantity) {
        this.positionQuantity = positionQuantity;
    }

    public BigDecimal getUnrealizedPnlPct() {
        return unrealizedPnlPct;
    }

    public void setUnrealizedPnlPct(BigDecimal unrealizedPnlPct) {
        this.unrealizedPnlPct = unrealizedPnlPct;
    }

    public String getPositionStatus() {
        return positionStatus;
    }

    public void setPositionStatus(String positionStatus) {
        this.positionStatus = positionStatus;
    }

    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(BigDecimal markPrice) {
        this.markPrice = markPrice;
    }

    public BigDecimal getBreakEvenPrice() {
        return breakEvenPrice;
    }

    public void setBreakEvenPrice(BigDecimal breakEvenPrice) {
        this.breakEvenPrice = breakEvenPrice;
    }

    public BigDecimal getLiquidationPrice() {
        return liquidationPrice;
    }

    public void setLiquidationPrice(BigDecimal liquidationPrice) {
        this.liquidationPrice = liquidationPrice;
    }
}
