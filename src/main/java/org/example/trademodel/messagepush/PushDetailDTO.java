package org.example.trademodel.messagepush;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PushDetailDTO(
        MessageReadState state,
        String messageId,
        String pushId,
        MessageListDTO.SourceIdentity sourceIdentity,
        OriginalSnapshot originalSnapshot,
        CurrentRecheck currentRecheck,
        String changeReason,
        List<String> missingFields,
        String reason,
        boolean reviewOnly,
        boolean notTradeInstruction,
        boolean notExecutable,
        boolean notPushSend) {

    public PushDetailDTO {
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }

    public record OriginalSnapshot(
            String snapshotId,
            String sourceType,
            String analysisId,
            String positionId,
            String symbol,
            String direction,
            String status,
            BigDecimal currentPrice,
            String entryZone,
            String invalidationCondition,
            String riskLevel,
            String reason,
            LocalDateTime capturedAt) {
    }

    public record CurrentRecheck(
            String recheckId,
            String sourceType,
            String status,
            BigDecimal currentPrice,
            Integer dataQualityScore,
            Integer confusedScore,
            String riskLevel,
            LocalDateTime checkedAt) {
    }
}
