package org.example.trademodel.entity;

import java.time.LocalDateTime;

/** Durable, owner-scoped audit fact for an explicit Telegram channel test. */
public class TelegramChannelTestAuditDO {
    private String testId;
    private Long userId;
    private String idempotencyKey;
    private String status;
    private String providerReference;
    private String botUsername;
    private String recipientFingerprint;
    private Integer responseCode;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime requestedAt;
    private LocalDateTime attemptedAt;
    private LocalDateTime completedAt;
    private Boolean notTradeInstruction = true;
    private Boolean notOrderExecution = true;

    public String getTestId() { return testId; }
    public void setTestId(String value) { this.testId = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String value) { this.providerReference = value; }
    public String getBotUsername() { return botUsername; }
    public void setBotUsername(String value) { this.botUsername = value; }
    public String getRecipientFingerprint() { return recipientFingerprint; }
    public void setRecipientFingerprint(String value) { this.recipientFingerprint = value; }
    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer value) { this.responseCode = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { this.errorCode = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { this.errorMessage = value; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime value) { this.requestedAt = value; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime value) { this.attemptedAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(Boolean value) { this.notTradeInstruction = value; }
    public Boolean getNotOrderExecution() { return notOrderExecution; }
    public void setNotOrderExecution(Boolean value) { this.notOrderExecution = value; }
}
