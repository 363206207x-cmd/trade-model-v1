package org.example.trademodel.ai;

import java.time.LocalDateTime;

public record OpenAiModelRoutingDecision(
        GptFinalModelStrategy modelStrategy,
        String originalModel,
        String selectedModel,
        int fallbackLevel,
        String fallbackReason,
        LocalDateTime timestamp,
        String traceId,
        boolean available
) {
    public boolean fallbackUsed() {
        return fallbackLevel > 0;
    }
}
