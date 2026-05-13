package org.example.trademodel.dto.planboundary;

import java.math.BigDecimal;

public class BoundaryTakeProfitLevelDTO {
    private Integer level;
    private BigDecimal price;
    private BigDecimal rr;
    private String source;
    private String reason;

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
}
