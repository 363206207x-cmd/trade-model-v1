package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class ChannelDeliveryDO {
    private String deliveryId;
    private String messageId;
    private Long userId;
    private String channel;
    private String status;
    private String providerReference;
    private Integer attemptCount;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime attemptedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String value) { this.deliveryId = value; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String value) { this.messageId = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId = value; }
    public String getChannel() { return channel; }
    public void setChannel(String value) { this.channel = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String value) { this.providerReference = value; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer value) { this.attemptCount = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { this.errorCode = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { this.errorMessage = value; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime value) { this.attemptedAt = value; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime value) { this.deliveredAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
