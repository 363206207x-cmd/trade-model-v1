package org.example.trademodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.UserPositionStatusEnum;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PublicOpportunityProjectionPolicy;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogicStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorSuggestedActionEnum;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MessagePushReadService {
    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 100;
    private static final Pattern OPPORTUNITY_ID = Pattern.compile("opp-[A-Za-z0-9_-]{1,60}");
    private static final Pattern NUMERIC_ID = Pattern.compile("[1-9][0-9]{0,18}");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> RISK_LOGIC_STATUSES = Set.of(
            PositionMonitorLogicStatusEnum.LOGIC_WEAKENED.name(),
            PositionMonitorLogicStatusEnum.PLAN_INVALIDATED.name(),
            PositionMonitorLogicStatusEnum.HIGH_RISK.name());
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    private final OpportunityLogMapper opportunityLogMapper;
    private final PositionMonitorLogMapper positionMonitorLogMapper;
    private final UserPositionMapper userPositionMapper;
    private Clock clock = Clock.systemUTC();

    public MessagePushReadService(OpportunityLogMapper opportunityLogMapper,
                                  PositionMonitorLogMapper positionMonitorLogMapper,
                                  UserPositionMapper userPositionMapper) {
        this.opportunityLogMapper = opportunityLogMapper;
        this.positionMonitorLogMapper = positionMonitorLogMapper;
        this.userPositionMapper = userPositionMapper;
    }

    @Autowired(required = false)
    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public MessageListDTO listForUser(Long userId, Integer limit) {
        requireUserId(userId);
        int safeLimit = sanitizeLimit(limit);
        try {
            List<OpportunityLogPublicDTO> opportunities = safeList(
                    opportunityLogMapper.queryPublicApi(
                            null, null, null, null, null, null, null, null, safeLimit));
            List<PositionMonitorLogDO> positionRisks = safeList(
                    positionMonitorLogMapper.listRiskByUserId(userId, safeLimit));
            List<MessageListDTO.MessageItem> items = new ArrayList<>();
            int incompleteSources = 0;
            LocalDateTime now = UtcLocalTimePolicy.now(clock);
            for (OpportunityLogPublicDTO row : opportunities) {
                MessageListDTO.MessageItem item = opportunityItem(row);
                if (item == null) {
                    incompleteSources++;
                } else {
                    items.add(item);
                }
            }
            for (PositionMonitorLogDO row : positionRisks) {
                PositionRiskListItem result = positionRiskItem(row, userId, now);
                switch (result.state()) {
                    case READY -> items.add(result.item());
                    case PARTIAL -> incompleteSources++;
                    case ERROR -> {
                        return new MessageListDTO(MessageReadState.ERROR, null, result.reason());
                    }
                    default -> {
                        return new MessageListDTO(
                                MessageReadState.ERROR, null, "POSITION_RISK_STATE_INVALID");
                    }
                }
            }
            items.sort(Comparator.comparing(
                    MessageListDTO.MessageItem::timestamp,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            List<MessageListDTO.MessageItem> limited = items.size() > safeLimit
                    ? List.copyOf(items.subList(0, safeLimit))
                    : List.copyOf(items);
            if (incompleteSources > 0) {
                return new MessageListDTO(MessageReadState.PARTIAL, limited,
                        "SOURCE_RECORD_INCOMPLETE");
            }
            if (limited.isEmpty()) {
                return new MessageListDTO(MessageReadState.EMPTY, List.of(), null);
            }
            return new MessageListDTO(MessageReadState.READY, limited, null);
        } catch (RuntimeException ex) {
            return new MessageListDTO(MessageReadState.ERROR, null, "MESSAGE_READ_FAILED");
        }
    }

    public PushDetailDTO findPushDetailForUser(Long userId, String rawMessageId) {
        requireUserId(userId);
        String messageId = normalizeMessageId(rawMessageId);
        if (messageId == null) {
            return unavailable(MessageReadState.MISSING, rawMessageId, "MESSAGE_NOT_FOUND");
        }
        try {
            if (OPPORTUNITY_ID.matcher(messageId).matches()) {
                return opportunityDetail(messageId);
            }
            if (NUMERIC_ID.matcher(messageId).matches()) {
                return positionRiskDetail(userId, messageId);
            }
            return unavailable(MessageReadState.MISSING, messageId, "MESSAGE_NOT_FOUND");
        } catch (RuntimeException ex) {
            return unavailable(MessageReadState.ERROR, messageId, "MESSAGE_READ_FAILED");
        }
    }

    private PushDetailDTO opportunityDetail(String messageId) {
        OpportunityLogPublicDTO opportunity = opportunityLogMapper
                .selectPublicApiByOpportunityId(messageId);
        PublicOpportunityProjectionPolicy.Evaluation evaluation =
                PublicOpportunityProjectionPolicy.evaluate(opportunity, messageId);
        if (evaluation.state() == MessageReadState.MISSING) {
            return unavailable(MessageReadState.MISSING, messageId, "MESSAGE_NOT_FOUND");
        }
        MessageListDTO.SourceIdentity sourceIdentity = new MessageListDTO.SourceIdentity(
                "OPPORTUNITY", opportunity.opportunityId(), opportunity.analysisId(), null);
        PushDetailDTO.OpportunityIdentity opportunityIdentity = new PushDetailDTO.OpportunityIdentity(
                opportunity.opportunityId(),
                opportunity.analysisId());
        return opportunityProjection(
                evaluation.state(),
                messageId,
                sourceIdentity,
                opportunityIdentity,
                evaluation.publicLifecycle(),
                evaluation.publicStatus(),
                PublicOpportunityProjectionPolicy.publicTimestamp(opportunity),
                PublicOpportunityProjectionPolicy.publicDescription(opportunity),
                evaluation.missingFields(),
                evaluation.reason());
    }

    private PushDetailDTO positionRiskDetail(Long userId, String messageId) {
        Long logId;
        try {
            logId = Long.valueOf(messageId);
        } catch (NumberFormatException ex) {
            return unavailable(MessageReadState.MISSING, messageId, "MESSAGE_NOT_FOUND");
        }
        PositionMonitorLogDO originalLog = positionMonitorLogMapper
                .selectRiskByIdAndUserId(logId, userId);
        if (originalLog == null) {
            return unavailable(MessageReadState.MISSING, messageId, "MESSAGE_NOT_FOUND");
        }
        if (!Objects.equals(logId, originalLog.getLogId())) {
            return unavailable(MessageReadState.ERROR, messageId, "POSITION_RISK_MESSAGE_IDENTITY_MISMATCH");
        }
        if (!positive(originalLog.getPositionId())) {
            return unavailable(MessageReadState.ERROR, messageId, "POSITION_RISK_POSITION_IDENTITY_INVALID");
        }
        UserPositionDO position = userPositionMapper.selectByIdAndUserId(
                originalLog.getPositionId(), userId);
        if (position == null) {
            return unavailable(MessageReadState.MISSING, messageId, "MESSAGE_NOT_FOUND");
        }
        String positionId = id(position.getId());
        MessageListDTO.SourceIdentity sourceIdentity = new MessageListDTO.SourceIdentity(
                "POSITION_RISK", messageId, originalLog.getAnalysisId(), positionId);
        String positionSymbol = normalizeMessageId(position.getAssetSymbol());
        PushDetailDTO.OriginalSnapshot original = new PushDetailDTO.OriginalSnapshot(
                messageId,
                "POSITION_RISK",
                originalLog.getAnalysisId(),
                positionId,
                positionSymbol,
                position.getSide(),
                normalize(originalLog.getLogicStatus()),
                originalLog.getCurrentPrice(),
                null,
                null,
                normalize(originalLog.getRiskLevel()),
                originalLog.getReason(),
                originalLog.getCreatedAt());
        PositionMonitorLogDO latest = positionMonitorLogMapper
                .selectLatestByPositionIdAndUserId(position.getId(), userId);
        if (latest == null) {
            return positionRiskProjection(
                    MessageReadState.MISSING,
                    messageId,
                    sourceIdentity,
                    original,
                    null,
                    null,
                    List.of("currentRecheck"),
                    "CURRENT_MONITOR_STATE_MISSING");
        }
        MonitorValidation validation = validatePositionRisk(
                userId, position, originalLog, latest, UtcLocalTimePolicy.now(clock));
        PushDetailDTO.CurrentRecheck current = new PushDetailDTO.CurrentRecheck(
                id(latest.getLogId()),
                "POSITION_MONITOR",
                normalize(latest.getLogicStatus()),
                latest.getCurrentPrice(),
                null,
                null,
                normalize(latest.getRiskLevel()),
                latest.getCreatedAt());
        return positionRiskProjection(
                validation.state(),
                messageId,
                sourceIdentity,
                original,
                current,
                latest.getReason(),
                validation.missingFields(),
                validation.reason());
    }

    private static MessageListDTO.MessageItem opportunityItem(OpportunityLogPublicDTO row) {
        PublicOpportunityProjectionPolicy.Evaluation evaluation =
                PublicOpportunityProjectionPolicy.evaluate(row, row == null ? null : row.opportunityId());
        if (row == null
                || evaluation.state() == MessageReadState.ERROR
                || evaluation.state() == MessageReadState.MISSING
                || normalizeMessageId(row.analysisId()) == null
                || normalizeMessageId(row.symbol()) == null
                || evaluation.displayStatus() == null
                || PublicOpportunityProjectionPolicy.publicTimestamp(row) == null) {
            return null;
        }
        return item(
                row.opportunityId(),
                new MessageListDTO.SourceIdentity(
                        "OPPORTUNITY", row.opportunityId(), row.analysisId(), null),
                row.symbol(),
                evaluation.displayStatus(),
                PublicOpportunityProjectionPolicy.publicTimestamp(row));
    }

    private PositionRiskListItem positionRiskItem(
            PositionMonitorLogDO row,
            Long userId,
            LocalDateTime now) {
        if (row == null || !positive(row.getLogId()) || !positive(row.getPositionId())) {
            return new PositionRiskListItem(
                    MessageReadState.ERROR, null, "POSITION_MONITOR_IDENTITY_INVALID");
        }
        UserPositionDO position = userPositionMapper.selectByIdAndUserId(row.getPositionId(), userId);
        if (position == null) {
            return new PositionRiskListItem(
                    MessageReadState.ERROR, null, "POSITION_RISK_IDENTITY_MISMATCH");
        }
        MonitorValidation validation = validatePositionRisk(userId, position, row, row, now);
        if (validation.state() != MessageReadState.READY) {
            return new PositionRiskListItem(validation.state(), null, validation.reason());
        }
        String messageId = id(row.getLogId());
        String positionId = id(position.getId());
        return new PositionRiskListItem(
                MessageReadState.READY,
                item(
                        messageId,
                        new MessageListDTO.SourceIdentity(
                                "POSITION_RISK", messageId, row.getAnalysisId(), positionId),
                        normalizeMessageId(position.getAssetSymbol()),
                        normalize(row.getLogicStatus()),
                        row.getCreatedAt()),
                null);
    }

    private static MessageListDTO.MessageItem item(String messageId,
                                                   MessageListDTO.SourceIdentity sourceIdentity,
                                                   String symbol,
                                                   String status,
                                                   LocalDateTime timestamp) {
        return new MessageListDTO.MessageItem(
                messageId,
                sourceIdentity,
                symbol,
                status,
                timestamp,
                true,
                true,
                true,
                true);
    }

    private static PushDetailDTO opportunityProjection(
            MessageReadState state,
            String messageId,
            MessageListDTO.SourceIdentity sourceIdentity,
            PushDetailDTO.OpportunityIdentity opportunityIdentity,
            String publicLifecycle,
            String publicStatus,
            LocalDateTime publicTimestamp,
            String publicDescription,
            List<String> missingFields,
            String reason) {
        return new PushDetailDTO.OpportunityPublicProjection(
                state,
                messageId,
                sourceIdentity,
                opportunityIdentity,
                publicLifecycle,
                publicStatus,
                publicTimestamp,
                publicDescription,
                missingFields,
                reason,
                true,
                true,
                true,
                true);
    }

    private static PushDetailDTO positionRiskProjection(
            MessageReadState state,
            String messageId,
            MessageListDTO.SourceIdentity sourceIdentity,
            PushDetailDTO.OriginalSnapshot original,
            PushDetailDTO.CurrentRecheck current,
            String changeReason,
            List<String> missingFields,
            String reason) {
        return new PushDetailDTO.PositionRiskPrivateProjection(
                state,
                messageId,
                sourceIdentity,
                original,
                current,
                changeReason,
                missingFields,
                reason,
                true,
                true,
                true,
                true);
    }

    private static PushDetailDTO unavailable(
            MessageReadState state,
            String messageId,
            String reason) {
        return new PushDetailDTO.UnavailableProjection(
                state,
                normalizeMessageId(messageId),
                null,
                List.of(),
                reason,
                true,
                true,
                true,
                true);
    }

    static int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private static String normalizeMessageId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalize(String value) {
        String normalized = normalizeMessageId(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String id(Long value) {
        return positive(value) ? Long.toString(value) : null;
    }

    private static <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private static MonitorValidation validatePositionRisk(
            Long userId,
            UserPositionDO position,
            PositionMonitorLogDO original,
            PositionMonitorLogDO latest,
            LocalDateTime now) {
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        String error = validatePositionIdentity(userId, position, original, latest, missing);
        if (error == null) {
            error = validatePositionLifecycle(position, latest, missing);
        }
        if (error == null) {
            error = validateMonitor("originalSnapshot", original, now, missing);
        }
        if (error == null && !Objects.equals(original.getLogId(), latest.getLogId())) {
            error = validateMonitor("currentRecheck", latest, now, missing);
        }
        if (error == null) {
            error = validateMonitorRelationship(original, latest, missing);
        }
        if (error != null) {
            return new MonitorValidation(MessageReadState.ERROR, List.copyOf(missing), error);
        }
        if (!missing.isEmpty()) {
            return new MonitorValidation(
                    MessageReadState.PARTIAL,
                    List.copyOf(missing),
                    "POSITION_RISK_DATA_INCOMPLETE");
        }
        return new MonitorValidation(MessageReadState.READY, List.of(), null);
    }

    private static String validatePositionIdentity(
            Long userId,
            UserPositionDO position,
            PositionMonitorLogDO original,
            PositionMonitorLogDO latest,
            Set<String> missing) {
        if (!positive(userId) || !Objects.equals(userId, position.getUserId())) {
            return "POSITION_RISK_OWNER_MISMATCH";
        }
        if (!positive(position.getId())
                || !Objects.equals(position.getId(), original.getPositionId())
                || !Objects.equals(position.getId(), latest.getPositionId())) {
            return "POSITION_RISK_IDENTITY_MISMATCH";
        }
        if (normalizeMessageId(position.getAssetSymbol()) == null) {
            missing.add("position.symbol");
        }
        String originalLogic = normalize(original.getLogicStatus());
        if (originalLogic == null) {
            missing.add("originalSnapshot.status");
        } else if (!RISK_LOGIC_STATUSES.contains(originalLogic)) {
            return "POSITION_RISK_SOURCE_MISMATCH";
        }
        return null;
    }

    private static String validatePositionLifecycle(
            UserPositionDO position,
            PositionMonitorLogDO latest,
            Set<String> missing) {
        String status = normalize(position.getStatus());
        if (status == null) {
            missing.add("position.status");
            return null;
        }
        UserPositionStatusEnum lifecycle;
        try {
            lifecycle = UserPositionStatusEnum.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return "POSITION_LIFECYCLE_INVALID";
        }
        if (lifecycle.visibleInOpenPositions() && position.getClosedAt() != null) {
            return "POSITION_LIFECYCLE_CONFLICT";
        }
        if (lifecycle == UserPositionStatusEnum.CLOSED) {
            if (position.getClosedAt() == null) {
                missing.add("position.closedAt");
            } else if (latest.getCreatedAt() != null
                    && latest.getCreatedAt().isAfter(position.getClosedAt())) {
                return "POSITION_MONITOR_AFTER_CLOSE";
            }
        }
        return null;
    }

    private static String validateMonitor(
            String fieldPrefix,
            PositionMonitorLogDO row,
            LocalDateTime now,
            Set<String> missing) {
        if (!positive(row.getLogId()) || !positive(row.getPositionId())) {
            return "POSITION_MONITOR_IDENTITY_INVALID";
        }
        if (normalizeMessageId(row.getAnalysisId()) == null) {
            missing.add(fieldPrefix + ".analysisId");
        }
        if (normalizeMessageId(row.getExecutionPlanId()) == null) {
            missing.add(fieldPrefix + ".executionPlanId");
        }
        if (row.getCurrentPrice() == null) {
            missing.add(fieldPrefix + ".currentPrice");
        } else if (!positive(row.getCurrentPrice())) {
            return "POSITION_MONITOR_PRICE_INVALID";
        }

        String logicStatus = normalize(row.getLogicStatus());
        if (logicStatus == null) {
            missing.add(fieldPrefix + ".status");
        } else {
            try {
                PositionMonitorLogicStatusEnum.valueOf(logicStatus);
            } catch (IllegalArgumentException ex) {
                return "POSITION_MONITOR_LOGIC_STATUS_INVALID";
            }
        }

        String riskLevel = normalize(row.getRiskLevel());
        if (riskLevel == null) {
            missing.add(fieldPrefix + ".riskLevel");
        } else if (!RISK_LEVELS.contains(riskLevel)) {
            return "POSITION_MONITOR_RISK_LEVEL_INVALID";
        }

        String action = normalize(row.getSuggestedAction());
        if (action == null) {
            missing.add(fieldPrefix + ".suggestedAction");
        } else {
            try {
                PositionMonitorSuggestedActionEnum.valueOf(action);
            } catch (IllegalArgumentException ex) {
                return "POSITION_MONITOR_ACTION_INVALID";
            }
            if (logicStatus != null && !legalAction(logicStatus, action)) {
                return "POSITION_MONITOR_STATE_CONFLICT";
            }
        }
        if (normalizeMessageId(row.getReason()) == null) {
            missing.add(fieldPrefix + ".reason");
        }
        if (row.getCreatedAt() == null) {
            missing.add(fieldPrefix + ".checkedAt");
        } else if (now != null && row.getCreatedAt().isAfter(now)) {
            return "POSITION_MONITOR_TIMESTAMP_INVALID";
        }
        return validateRiskSnapshot(fieldPrefix, row, missing);
    }

    private static String validateRiskSnapshot(
            String fieldPrefix,
            PositionMonitorLogDO row,
            Set<String> missing) {
        String raw = normalizeMessageId(row.getRiskSnapshot());
        if (raw == null) {
            missing.add(fieldPrefix + ".riskSnapshot");
            return null;
        }
        JsonNode root;
        try {
            root = JSON.readTree(raw);
        } catch (Exception ex) {
            return "POSITION_MONITOR_RISK_DATA_MALFORMED";
        }
        if (root == null || !root.isObject()) {
            return "POSITION_MONITOR_RISK_DATA_MALFORMED";
        }
        JsonNode snapshotRiskLevel = root.get("riskLevel");
        if (snapshotRiskLevel == null || snapshotRiskLevel.isNull()) {
            missing.add(fieldPrefix + ".riskSnapshot.riskLevel");
        } else if (!snapshotRiskLevel.isTextual()) {
            return "POSITION_MONITOR_RISK_DATA_MALFORMED";
        } else {
            // Account-level snapshot risk and composite monitor risk are independently valid dimensions.
            String normalizedRisk = normalize(snapshotRiskLevel.asText());
            if (!RISK_LEVELS.contains(normalizedRisk)) {
                return "POSITION_MONITOR_RISK_DATA_INVALID";
            }
        }
        JsonNode riskBlocked = root.get("riskBlocked");
        if (riskBlocked == null || riskBlocked.isNull()) {
            missing.add(fieldPrefix + ".riskSnapshot.riskBlocked");
        } else if (!riskBlocked.isBoolean()) {
            return "POSITION_MONITOR_RISK_DATA_MALFORMED";
        }
        return null;
    }

    private static String validateMonitorRelationship(
            PositionMonitorLogDO original,
            PositionMonitorLogDO latest,
            Set<String> missing) {
        String originalAnalysisId = normalizeMessageId(original.getAnalysisId());
        String latestAnalysisId = normalizeMessageId(latest.getAnalysisId());
        if (originalAnalysisId != null
                && latestAnalysisId != null
                && !originalAnalysisId.equals(latestAnalysisId)) {
            return "POSITION_MONITOR_ANALYSIS_IDENTITY_MISMATCH";
        }
        String originalPlanId = normalizeMessageId(original.getExecutionPlanId());
        String latestPlanId = normalizeMessageId(latest.getExecutionPlanId());
        if (originalPlanId != null && latestPlanId != null && !originalPlanId.equals(latestPlanId)) {
            return "POSITION_MONITOR_PLAN_IDENTITY_MISMATCH";
        }
        if (latest.getCreatedAt() != null
                && original.getCreatedAt() != null
                && latest.getCreatedAt().isBefore(original.getCreatedAt())) {
            return "POSITION_MONITOR_SEQUENCE_INVALID";
        }
        if (originalAnalysisId == null || latestAnalysisId == null) {
            missing.add("sourceIdentity.analysisId");
        }
        return null;
    }

    private static boolean legalAction(String logicStatus, String action) {
        return switch (logicStatus) {
            case "LOGIC_VALID" -> "HOLD".equals(action) || "MANUAL_REVIEW".equals(action);
            case "LOGIC_WEAKENED" -> "MANUAL_REVIEW".equals(action) || "RECHECK_PLAN".equals(action);
            case "PLAN_INVALIDATED" -> "RECHECK_PLAN".equals(action);
            case "HIGH_RISK" -> "RISK_REVIEW".equals(action);
            default -> false;
        };
    }

    private record MonitorValidation(
            MessageReadState state,
            List<String> missingFields,
            String reason) {
    }

    private record PositionRiskListItem(
            MessageReadState state,
            MessageListDTO.MessageItem item,
            String reason) {
    }
}
