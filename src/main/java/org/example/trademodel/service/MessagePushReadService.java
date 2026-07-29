package org.example.trademodel.service;

import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final Set<String> PUBLIC_OPPORTUNITY_STATUSES = Set.of(
            OpportunityLogStatus.MISSED_VALID,
            OpportunityLogStatus.MISSED_INVALID,
            OpportunityLogStatus.PUSHED_NOT_FILLED_VALID);
    private static final Set<String> PUBLIC_OPPORTUNITY_LIFECYCLE_STATUSES = Set.of(
            OpportunityLogStatus.PENDING_EVALUATION,
            OpportunityLogStatus.RESOLVED,
            OpportunityLogStatus.SOURCE_INCOMPLETE,
            OpportunityLogStatus.MARKET_PATH_UNAVAILABLE,
            OpportunityLogStatus.AMBIGUOUS_MARKET_PATH);

    private final OpportunityLogMapper opportunityLogMapper;
    private final PositionMonitorLogMapper positionMonitorLogMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final UserPositionMapper userPositionMapper;

    public MessagePushReadService(OpportunityLogMapper opportunityLogMapper,
                                  PositionMonitorLogMapper positionMonitorLogMapper,
                                  PushSnapshotMapper pushSnapshotMapper,
                                  UserPositionMapper userPositionMapper) {
        this.opportunityLogMapper = opportunityLogMapper;
        this.positionMonitorLogMapper = positionMonitorLogMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.userPositionMapper = userPositionMapper;
    }

    public MessageListDTO listForUser(Long userId, Integer limit) {
        requireUserId(userId);
        int safeLimit = sanitizeLimit(limit);
        try {
            List<OpportunityLogDO> opportunities = safeList(
                    opportunityLogMapper.listPushBackedPublic(safeLimit));
            List<PositionMonitorLogDO> positionRisks = safeList(
                    positionMonitorLogMapper.listRiskByUserId(userId, safeLimit));
            List<MessageListDTO.MessageItem> items = new ArrayList<>();
            int incompleteSources = 0;
            for (OpportunityLogDO row : opportunities) {
                MessageListDTO.MessageItem item = opportunityItem(row);
                if (item == null) {
                    incompleteSources++;
                } else {
                    items.add(item);
                }
            }
            for (PositionMonitorLogDO row : positionRisks) {
                MessageListDTO.MessageItem item = positionRiskItem(row, userId);
                if (item == null) {
                    incompleteSources++;
                } else {
                    items.add(item);
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
        OpportunityLogDO opportunity = opportunityLogMapper
                .selectPushBackedPublicByOpportunityId(messageId);
        if (opportunity == null || !positive(opportunity.getPushId())) {
            return unavailable(MessageReadState.MISSING, messageId, "MESSAGE_NOT_FOUND");
        }
        String pushId = id(opportunity.getPushId());
        MessageListDTO.SourceIdentity sourceIdentity = new MessageListDTO.SourceIdentity(
                "OPPORTUNITY", opportunity.getOpportunityId(), opportunity.getAnalysisId(), null);
        PushDetailDTO.OpportunityIdentity opportunityIdentity = new PushDetailDTO.OpportunityIdentity(
                opportunity.getOpportunityId(), opportunity.getAnalysisId(), pushId);
        String publicStatus = publicOpportunityStatus(opportunity);
        LocalDateTime opportunityTimestamp = firstNonNull(
                opportunity.getAnchorTime(), opportunity.getCreatedAt());
        String publicDescription = publicOpportunityDescription(opportunity);
        if (normalizeMessageId(opportunity.getAnalysisId()) == null
                || publicStatus == null
                || opportunityTimestamp == null
                || publicDescription == null) {
            List<String> missingFields = new ArrayList<>();
            if (normalizeMessageId(opportunity.getAnalysisId()) == null) {
                missingFields.add("opportunityIdentity.analysisId");
            }
            if (publicStatus == null) {
                missingFields.add("publicStatus");
            }
            if (opportunityTimestamp == null) {
                missingFields.add("publicTimestamp");
            }
            if (publicDescription == null) {
                missingFields.add("publicDescription");
            }
            return opportunityProjection(
                    MessageReadState.PARTIAL,
                    messageId,
                    sourceIdentity,
                    opportunityIdentity,
                    publicStatus,
                    opportunityTimestamp,
                    publicDescription,
                    missingFields,
                    "PUBLIC_OPPORTUNITY_INCOMPLETE");
        }
        TmPushSnapshotDO push = pushSnapshotMapper
                .selectPublicProjectionByPushId(opportunity.getPushId());
        if (push == null
                || !Objects.equals(opportunity.getPushId(), push.getPushId())
                || !Objects.equals(opportunity.getAnalysisId(), push.getAnalysisId())) {
            return opportunityProjection(
                    MessageReadState.PARTIAL,
                    messageId,
                    sourceIdentity,
                    opportunityIdentity,
                    publicStatus,
                    opportunityTimestamp,
                    publicDescription,
                    List.of("publicPushProjection"),
                    "PUSH_SNAPSHOT_MISSING");
        }
        return opportunityProjection(
                MessageReadState.READY,
                messageId,
                sourceIdentity,
                opportunityIdentity,
                publicStatus,
                firstNonNull(push.getPushCreateTime(), opportunityTimestamp),
                publicDescription,
                List.of(),
                null);
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
        if (originalLog == null || !positive(originalLog.getPositionId())) {
            return unavailable(MessageReadState.MISSING, messageId, "MESSAGE_NOT_FOUND");
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
        if (positionSymbol == null) {
            return positionRiskProjection(
                    MessageReadState.PARTIAL,
                    messageId,
                    sourceIdentity,
                    null,
                    null,
                    null,
                    List.of("originalSnapshot.symbol"),
                    "POSITION_SYMBOL_MISSING");
        }
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
                    MessageReadState.PARTIAL,
                    messageId,
                    sourceIdentity,
                    original,
                    null,
                    null,
                    List.of("currentRecheck"),
                    "CURRENT_MONITOR_STATE_MISSING");
        }
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
                MessageReadState.READY,
                messageId,
                sourceIdentity,
                original,
                current,
                latest.getReason(),
                List.of(),
                null);
    }

    private static MessageListDTO.MessageItem opportunityItem(OpportunityLogDO row) {
        if (row == null || !validOpportunityId(row.getOpportunityId()) || !positive(row.getPushId())
                || normalizeMessageId(row.getAnalysisId()) == null
                || normalizeMessageId(row.getSymbol()) == null
                || publicOpportunityStatus(row) == null
                || firstNonNull(row.getAnchorTime(), row.getCreatedAt()) == null) {
            return null;
        }
        return item(
                row.getOpportunityId(),
                id(row.getPushId()),
                new MessageListDTO.SourceIdentity(
                        "OPPORTUNITY", row.getOpportunityId(), row.getAnalysisId(), null),
                row.getSymbol(),
                publicOpportunityStatus(row),
                firstNonNull(row.getAnchorTime(), row.getCreatedAt()));
    }

    private MessageListDTO.MessageItem positionRiskItem(PositionMonitorLogDO row, Long userId) {
        if (row == null || !positive(row.getLogId()) || !positive(row.getPositionId())
                || !isRiskState(row.getLogicStatus())) {
            return null;
        }
        UserPositionDO position = userPositionMapper.selectByIdAndUserId(row.getPositionId(), userId);
        String symbol = position == null ? null : normalizeMessageId(position.getAssetSymbol());
        if (position == null || !Objects.equals(position.getId(), row.getPositionId()) || symbol == null) {
            return null;
        }
        String messageId = id(row.getLogId());
        String positionId = id(position.getId());
        return item(
                messageId,
                null,
                new MessageListDTO.SourceIdentity(
                        "POSITION_RISK", messageId, row.getAnalysisId(), positionId),
                symbol,
                normalize(row.getLogicStatus()),
                row.getCreatedAt());
    }

    private static MessageListDTO.MessageItem item(String messageId,
                                                   String pushId,
                                                   MessageListDTO.SourceIdentity sourceIdentity,
                                                   String symbol,
                                                   String status,
                                                   LocalDateTime timestamp) {
        return new MessageListDTO.MessageItem(
                messageId,
                pushId,
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

    private static boolean validOpportunityId(String value) {
        return value != null && OPPORTUNITY_ID.matcher(value).matches();
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private static boolean isRiskState(String value) {
        String normalized = normalize(value);
        return "LOGIC_WEAKENED".equals(normalized)
                || "PLAN_INVALIDATED".equals(normalized)
                || "HIGH_RISK".equals(normalized);
    }

    private static String publicOpportunityStatus(OpportunityLogDO row) {
        String opportunityStatus = normalize(row.getOpportunityStatus());
        if (opportunityStatus != null && PUBLIC_OPPORTUNITY_STATUSES.contains(opportunityStatus)) {
            return opportunityStatus;
        }
        String lifecycleStatus = normalize(row.getLifecycleStatus());
        return lifecycleStatus != null && PUBLIC_OPPORTUNITY_LIFECYCLE_STATUSES.contains(lifecycleStatus)
                ? lifecycleStatus
                : null;
    }

    private static String publicOpportunityDescription(OpportunityLogDO row) {
        String symbol = normalizeMessageId(row.getSymbol());
        if (symbol == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        parts.add(symbol.toUpperCase(Locale.ROOT));
        String direction = normalize(row.getDirection());
        if (direction != null) {
            parts.add(direction);
        }
        String timeframe = normalize(row.getTimeframe());
        if (timeframe != null) {
            parts.add(timeframe);
        }
        return String.join(" ", parts);
    }

    private static String id(Long value) {
        return positive(value) ? Long.toString(value) : null;
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private static <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
