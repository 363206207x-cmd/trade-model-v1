package org.example.trademodel.messagepush;

import java.time.LocalDateTime;
import java.util.List;

public record MessageListDTO(
        MessageReadState state,
        List<MessageItem> items,
        String reason) {

    public MessageListDTO {
        items = items == null ? null : List.copyOf(items);
    }

    public record MessageItem(
            String messageId,
            String pushId,
            SourceIdentity sourceIdentity,
            String symbol,
            String status,
            LocalDateTime timestamp,
            boolean reviewOnly,
            boolean notTradeInstruction,
            boolean notExecutable,
            boolean notPushSend) {
    }

    public record SourceIdentity(
            String sourceType,
            String sourceId,
            String analysisId,
            String positionId) {
    }
}
