package org.example.trademodel.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class CloseUserPositionReq {
    @JsonAlias("close_price")
    private BigDecimal closePrice;
    @JsonAlias("close_reason")
    private String closeReason;
    @JsonAlias("closed_at")
    private LocalDateTime closedAt;

    @JsonIgnore
    private final Map<String, Object> extraFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void putExtraField(String name, Object value) {
        extraFields.put(name, value);
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public Map<String, Object> getExtraFields() {
        return extraFields;
    }
}
