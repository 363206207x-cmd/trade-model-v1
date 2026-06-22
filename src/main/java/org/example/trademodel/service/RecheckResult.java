package org.example.trademodel.service;

import org.example.trademodel.enums.RecheckStatusEnum;

import java.math.BigDecimal;

public class RecheckResult {

    private Long pushId;
    private RecheckStatusEnum recheckStatus;
    private BigDecimal currentPrice;
    private boolean valid;
    private boolean reviewPassed;
    private String message;
    private boolean reviewOnly = true;
    private boolean manualReviewOnly = true;
    private boolean notTradeInstruction = true;
    private boolean notExecutable = true;
    private boolean notAutoTrading = true;
    private boolean notOrderExecution = true;
    private boolean notUserPositionCreation = true;
    private boolean notPositionMutation = true;
    private boolean notTradingAuthorization = true;

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
        return false;
    }

    public void setValid(boolean valid) {
        // Legacy API compatibility only. This flag is never trading validity and is always false.
        this.valid = false;
    }

    public boolean isReviewPassed() {
        return reviewPassed;
    }

    public void setReviewPassed(boolean reviewPassed) {
        this.reviewPassed = reviewPassed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isReviewOnly() {
        return reviewOnly;
    }

    public void setReviewOnly(boolean reviewOnly) {
        this.reviewOnly = reviewOnly;
    }

    public boolean isManualReviewOnly() {
        return manualReviewOnly;
    }

    public void setManualReviewOnly(boolean manualReviewOnly) {
        this.manualReviewOnly = manualReviewOnly;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
    }

    public boolean isNotExecutable() {
        return notExecutable;
    }

    public void setNotExecutable(boolean notExecutable) {
        this.notExecutable = notExecutable;
    }

    public boolean isNotAutoTrading() {
        return notAutoTrading;
    }

    public void setNotAutoTrading(boolean notAutoTrading) {
        this.notAutoTrading = notAutoTrading;
    }

    public boolean isNotOrderExecution() {
        return notOrderExecution;
    }

    public void setNotOrderExecution(boolean notOrderExecution) {
        this.notOrderExecution = notOrderExecution;
    }

    public boolean isNotUserPositionCreation() {
        return notUserPositionCreation;
    }

    public void setNotUserPositionCreation(boolean notUserPositionCreation) {
        this.notUserPositionCreation = notUserPositionCreation;
    }

    public boolean isNotPositionMutation() {
        return notPositionMutation;
    }

    public void setNotPositionMutation(boolean notPositionMutation) {
        this.notPositionMutation = notPositionMutation;
    }

    public boolean isNotTradingAuthorization() {
        return notTradingAuthorization;
    }

    public void setNotTradingAuthorization(boolean notTradingAuthorization) {
        this.notTradingAuthorization = notTradingAuthorization;
    }
}
