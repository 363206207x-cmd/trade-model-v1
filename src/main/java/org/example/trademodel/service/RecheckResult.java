package org.example.trademodel.service;

import org.example.trademodel.enums.RecheckStatusEnum;

import java.math.BigDecimal;

public class RecheckResult {

    private Long pushId;
    private RecheckStatusEnum recheckStatus;
    private BigDecimal currentPrice;
    private boolean valid;
    private String message;

    public Long getPushId() {
        return pushId;
    }

    public void setPushId(Long pushId) {
        this.pushId = pushId;
    }

    public RecheckStatusEnum getRecheckStatus() {
        return recheckStatus;
    }

    public void setRecheckStatus(RecheckStatusEnum recheckStatus) {
        this.recheckStatus = recheckStatus;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
