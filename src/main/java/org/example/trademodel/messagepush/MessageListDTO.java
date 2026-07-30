package org.example.trademodel.messagepush;

import com.fasterxml.jackson.annotation.JsonInclude;

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
            SourceIdentity sourceIdentity,
            String symbol,
            String status,
            LocalDateTime timestamp,
            boolean reviewOnly,
            boolean notTradeInstruction,
            boolean notExecutable,
            boolean notPushSend) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SourceIdentity(
            String sourceType,
            String sourceId,
            String analysisId,
            String positionId) {
    }
}
