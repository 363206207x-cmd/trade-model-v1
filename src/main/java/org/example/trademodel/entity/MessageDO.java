package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class MessageDO {
    private String messageId;
    private Long userId;
    private String category;
    private String sourceType;
    private String sourceId;
    private String analysisId;
    private Long positionId;
    private String planId;
    private String symbol;
    private String title;
    private String body;
    private String businessState;
    private String readState;
    private String dedupeKey;
    private String currentRecheckId;
    private String traceId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean notTradeInstruction = true;
    private Boolean notOrderExecution = true;

    public String getMessageId() { return messageId; }
    public void setMessageId(String value) { this.messageId = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { this.category = value; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String value) { this.sourceType = value; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String value) { this.sourceId = value; }
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String value) { this.analysisId = value; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long value) { this.positionId = value; }
    public String getPlanId() { return planId; }
    public void setPlanId(String value) { this.planId = value; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String value) { this.symbol = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }
    public String getBody() { return body; }
    public void setBody(String value) { this.body = value; }
    public String getBusinessState() { return businessState; }
    public void setBusinessState(String value) { this.businessState = value; }
    public String getReadState() { return readState; }
    public void setReadState(String value) { this.readState = value; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String value) { this.dedupeKey = value; }
    public String getCurrentRecheckId() { return currentRecheckId; }
    public void setCurrentRecheckId(String value) { this.currentRecheckId = value; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String value) { this.traceId = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { this.expiresAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(Boolean value) { this.notTradeInstruction = value; }
    public Boolean getNotOrderExecution() { return notOrderExecution; }
    public void setNotOrderExecution(Boolean value) { this.notOrderExecution = value; }
}
