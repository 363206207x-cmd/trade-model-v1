package org.example.trademodel.message;

public record MessageRecordedEvent(String messageId, Long userId, String dedupeKey) {
}
