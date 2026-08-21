package org.example.trademodel.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class CreateUserPositionReq {
    @JsonAlias("asset_symbol")
    private String assetSymbol;
    private String side;
    @JsonAlias("entry_price")
    private BigDecimal entryPrice;
    private BigDecimal quantity;
    private BigDecimal leverage;
    @JsonAlias("opened_at")
    private LocalDateTime openedAt;
    @JsonAlias("stop_loss")
    private BigDecimal stopLoss;
    @JsonAlias("take_profit")
    private BigDecimal takeProfit;
    @JsonAlias("source_type")
    private String sourceType;
    @JsonAlias("source_ref_id")
    private String sourceRefId;
    @JsonAlias("final_plan_id")
    private String finalPlanId;

    @JsonIgnore
    private final Map<String, Object> extraFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void putExtraField(String name, Object value) {
        extraFields.put(name, value);
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public LocalDateTime getOpenedAt() { return openedAt; }

    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public void setLeverage(BigDecimal leverage) {
        this.leverage = leverage;
    }

    public BigDecimal getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(BigDecimal stopLoss) {
        this.stopLoss = stopLoss;
    }

    public BigDecimal getTakeProfit() {
        return takeProfit;
    }

    public void setTakeProfit(BigDecimal takeProfit) {
        this.takeProfit = takeProfit;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceRefId() {
        return sourceRefId;
    }

    public void setSourceRefId(String sourceRefId) {
        this.sourceRefId = sourceRefId;
    }

    public String getFinalPlanId() {
        return finalPlanId;
    }

    public void setFinalPlanId(String finalPlanId) {
        this.finalPlanId = finalPlanId;
    }

    public Map<String, Object> getExtraFields() {
        return extraFields;
    }
}
