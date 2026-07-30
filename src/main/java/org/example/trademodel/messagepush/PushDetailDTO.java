package org.example.trademodel.messagepush;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public sealed interface PushDetailDTO permits
        PushDetailDTO.OpportunityPublicProjection,
        PushDetailDTO.PositionRiskPrivateProjection,
        PushDetailDTO.UnavailableProjection {

    MessageReadState state();

    String messageId();

    MessageListDTO.SourceIdentity sourceIdentity();

    List<String> missingFields();

    String reason();

    boolean reviewOnly();

    boolean notTradeInstruction();

    boolean notExecutable();

    boolean notPushSend();

    record OpportunityPublicProjection(
            MessageReadState state,
            String messageId,
            MessageListDTO.SourceIdentity sourceIdentity,
            OpportunityIdentity opportunityIdentity,
            String publicLifecycle,
            String publicStatus,
            LocalDateTime publicTimestamp,
            String publicDescription,
            List<String> missingFields,
            String reason,
            boolean reviewOnly,
            boolean notTradeInstruction,
            boolean notExecutable,
            boolean notPushSend) implements PushDetailDTO {

        public OpportunityPublicProjection {
            missingFields = immutable(missingFields);
        }
    }

    record PositionRiskPrivateProjection(
            MessageReadState state,
            String messageId,
            MessageListDTO.SourceIdentity sourceIdentity,
            OriginalSnapshot originalSnapshot,
            CurrentRecheck currentRecheck,
            String changeReason,
            List<String> missingFields,
            String reason,
            boolean reviewOnly,
            boolean notTradeInstruction,
            boolean notExecutable,
            boolean notPushSend) implements PushDetailDTO {

        public PositionRiskPrivateProjection {
            missingFields = immutable(missingFields);
        }
    }

    record UnavailableProjection(
            MessageReadState state,
            String messageId,
            MessageListDTO.SourceIdentity sourceIdentity,
            List<String> missingFields,
            String reason,
            boolean reviewOnly,
            boolean notTradeInstruction,
            boolean notExecutable,
            boolean notPushSend) implements PushDetailDTO {

        public UnavailableProjection {
            missingFields = immutable(missingFields);
        }
    }

    record OpportunityIdentity(
            String opportunityId,
            String analysisId) {
    }

    record OriginalSnapshot(
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

    record CurrentRecheck(
            String recheckId,
            String sourceType,
            String status,
            BigDecimal currentPrice,
            Integer dataQualityScore,
            Integer confusedScore,
            String riskLevel,
            LocalDateTime checkedAt) {
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
