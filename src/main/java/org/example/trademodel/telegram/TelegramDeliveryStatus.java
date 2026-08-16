package org.example.trademodel.telegram;

public enum TelegramDeliveryStatus {
    QUEUED,
    SENDING,
    SENT,
    RETRYING,
    FAILED,
    SUPPRESSED,
    NOT_CONFIGURED
}
