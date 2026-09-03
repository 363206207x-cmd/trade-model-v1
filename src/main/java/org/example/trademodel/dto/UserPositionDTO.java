package org.example.trademodel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserPositionDTO {
    private Long id;
    private String submissionId;
    private String closeSubmissionId;
    private String assetSymbol;
    private String side;
    private String status;
    private BigDecimal entryPrice;
    private BigDecimal quantity;
    private BigDecimal leverage;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal closePrice;
    private String closeReason;
    private String sourceType;
    private String sourceRefId;
    private String finalPlanId;
    private boolean manualReviewRequired;
    private boolean notTradeInstruction;
    private boolean notAutoTrading;
    private boolean notOrderExecution;
    private boolean notPositionSync;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getCloseSubmissionId() {
        return closeSubmissionId;
    }

    public void setCloseSubmissionId(String closeSubmissionId) {
        this.closeSubmissionId = closeSubmissionId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
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

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public void setNotTradeInstruction(boolean notTradeInstruction) {
        this.notTradeInstruction = notTradeInstruction;
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

    public boolean isNotPositionSync() {
        return notPositionSync;
    }

    public void setNotPositionSync(boolean notPositionSync) {
        this.notPositionSync = notPositionSync;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
