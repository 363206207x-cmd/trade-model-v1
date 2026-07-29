package org.example.trademodel.service;

import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.messagepush.MessageListDTO;
import org.example.trademodel.messagepush.MessageReadState;
import org.example.trademodel.messagepush.PushDetailDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class MessagePushReadService {
    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 100;
    private static final Pattern OPPORTUNITY_ID = Pattern.compile("opp-[A-Za-z0-9_-]{1,60}");
    private static final Pattern NUMERIC_ID = Pattern.compile("[1-9][0-9]{0,18}");

    private final OpportunityLogMapper opportunityLogMapper;
    private final PositionMonitorLogMapper positionMonitorLogMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final UserPositionMapper userPositionMapper;

    public MessagePushReadService(OpportunityLogMapper opportunityLogMapper,
                                  PositionMonitorLogMapper positionMonitorLogMapper,
                                  PushSnapshotMapper pushSnapshotMapper,
                                  PushRecheckLogMapper pushRecheckLogMapper,
                                  UserPositionMapper userPositionMapper) {
        this.opportunityLogMapper = opportunityLogMapper;
        this.positionMonitorLogMapper = positionMonitorLogMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.userPositionMapper = userPositionMapper;
    }

    public MessageListDTO listForUser(Long userId, Integer limit) {
        requireUserId(userId);
        int safeLimit = sanitizeLimit(limit);
        try {
            List<OpportunityLogDO> opportunities = safeList(
                    opportunityLogMapper.listPushBackedShared(safeLimit));
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
                MessageListDTO.MessageItem item = positionRiskItem(row);
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
            return missing(rawMessageId);
        }
        try {
            if (OPPORTUNITY_ID.matcher(messageId).matches()) {
                return opportunityDetail(messageId);
            }
            if (NUMERIC_ID.matcher(messageId).matches()) {
                return positionRiskDetail(userId, messageId);
            }
            return missing(messageId);
        } catch (RuntimeException ex) {
            return error(messageId);
        }
    }

    private PushDetailDTO opportunityDetail(String messageId) {
        OpportunityLogDO opportunity = opportunityLogMapper
                .selectPushBackedSharedByOpportunityId(messageId);
        if (opportunity == null || !positive(opportunity.getPushId())) {
            return missing(messageId);
        }
        String pushId = id(opportunity.getPushId());
        MessageListDTO.SourceIdentity sourceIdentity = new MessageListDTO.SourceIdentity(
                "OPPORTUNITY", opportunity.getOpportunityId(), opportunity.getAnalysisId(), null);
        TmPushSnapshotDO push = pushSnapshotMapper.selectByPushId(opportunity.getPushId());
        if (push == null
                || normalizeMessageId(opportunity.getAnalysisId()) == null
                || !Objects.equals(opportunity.getPushId(), push.getPushId())
                || !Objects.equals(opportunity.getAnalysisId(), push.getAnalysisId())) {
            return partial(messageId, pushId, sourceIdentity, null, null, null,
                    List.of("originalSnapshot", "currentRecheck"), "PUSH_SNAPSHOT_MISSING");
        }

        PushDetailDTO.OriginalSnapshot original = new PushDetailDTO.OriginalSnapshot(
                pushId,
                "OPPORTUNITY",
                push.getAnalysisId(),
                null,
                push.getSymbol(),
                opportunity.getDirection(),
                PushRecheckStatusContract.canonicalizePushStatus(push.getPushStatus()),
                push.getTriggerPrice(),
                push.getEntryZoneJson(),
                push.getInvalidationConditionJson(),
                null,
                opportunity.getReasonCodes(),
                firstNonNull(push.getPushCreateTime(), push.getCreateTime()));
        TmPushRecheckLogDO latest = pushRecheckLogMapper.selectLatestByPushId(opportunity.getPushId());
        if (latest == null) {
            return partial(messageId, pushId, sourceIdentity, original, null, null,
                    List.of("currentRecheck"), "CURRENT_RECHECK_MISSING");
        }
        PushDetailDTO.CurrentRecheck current = new PushDetailDTO.CurrentRecheck(
                id(latest.getLogId()),
                "PUSH_RECHECK",
                PushRecheckStatusContract.canonicalizeRecheckStatusName(latest.getRecheckStatus()),
                latest.getCurrentPrice(),
                latest.getCurrentDataQualityScore(),
                latest.getCurrentConfusedScore(),
                latest.getCurrentAccountRiskAllowed() == null
                        ? null : latest.getCurrentAccountRiskAllowed() ? "ALLOWED" : "BLOCKED",
                firstNonNull(latest.getRecheckTime(), latest.getCreateTime()));
        return ready(messageId, pushId, sourceIdentity, original, current, latest.getFailReasonJson());
    }

    private PushDetailDTO positionRiskDetail(Long userId, String messageId) {
        Long logId;
        try {
            logId = Long.valueOf(messageId);
        } catch (NumberFormatException ex) {
            return missing(messageId);
        }
        PositionMonitorLogDO originalLog = positionMonitorLogMapper
                .selectRiskByIdAndUserId(logId, userId);
        if (originalLog == null || !positive(originalLog.getPositionId())) {
            return missing(messageId);
        }
        UserPositionDO position = userPositionMapper.selectByIdAndUserId(
                originalLog.getPositionId(), userId);
        if (position == null) {
            return missing(messageId);
        }
        String positionId = id(position.getId());
        MessageListDTO.SourceIdentity sourceIdentity = new MessageListDTO.SourceIdentity(
                "POSITION_RISK", messageId, originalLog.getAnalysisId(), positionId);
        PushDetailDTO.OriginalSnapshot original = new PushDetailDTO.OriginalSnapshot(
                messageId,
                "POSITION_RISK",
                originalLog.getAnalysisId(),
                positionId,
                position.getAssetSymbol(),
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
            return partial(messageId, null, sourceIdentity, original, null, null,
                    List.of("currentRecheck"), "CURRENT_MONITOR_STATE_MISSING");
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
        return ready(messageId, null, sourceIdentity, original, current, latest.getReason());
    }

    private static MessageListDTO.MessageItem opportunityItem(OpportunityLogDO row) {
        if (row == null || !validOpportunityId(row.getOpportunityId()) || !positive(row.getPushId())) {
            return null;
        }
        return item(
                row.getOpportunityId(),
                id(row.getPushId()),
                new MessageListDTO.SourceIdentity(
                        "OPPORTUNITY", row.getOpportunityId(), row.getAnalysisId(), null),
                row.getSymbol(),
                firstText(row.getOpportunityStatus(), row.getLifecycleStatus()),
                firstNonNull(row.getAnchorTime(), row.getCreatedAt()));
    }

    private static MessageListDTO.MessageItem positionRiskItem(PositionMonitorLogDO row) {
        if (row == null || !positive(row.getLogId()) || !positive(row.getPositionId())
                || !isRiskState(row.getLogicStatus())) {
            return null;
        }
        String messageId = id(row.getLogId());
        String positionId = id(row.getPositionId());
        return item(
                messageId,
                null,
                new MessageListDTO.SourceIdentity(
                        "POSITION_RISK", messageId, row.getAnalysisId(), positionId),
                null,
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

    private static PushDetailDTO ready(String messageId,
                                       String pushId,
                                       MessageListDTO.SourceIdentity sourceIdentity,
                                       PushDetailDTO.OriginalSnapshot original,
                                       PushDetailDTO.CurrentRecheck current,
                                       String changeReason) {
        return detail(MessageReadState.READY, messageId, pushId, sourceIdentity,
                original, current, changeReason, List.of(), null);
    }

    private static PushDetailDTO partial(String messageId,
                                         String pushId,
                                         MessageListDTO.SourceIdentity sourceIdentity,
                                         PushDetailDTO.OriginalSnapshot original,
                                         PushDetailDTO.CurrentRecheck current,
                                         String changeReason,
                                         List<String> missingFields,
                                         String reason) {
        return detail(MessageReadState.PARTIAL, messageId, pushId, sourceIdentity,
                original, current, changeReason, missingFields, reason);
    }

    private static PushDetailDTO missing(String messageId) {
        return detail(MessageReadState.MISSING, normalizeMessageId(messageId), null, null,
                null, null, null, List.of(), "MESSAGE_NOT_FOUND");
    }

    private static PushDetailDTO error(String messageId) {
        return detail(MessageReadState.ERROR, messageId, null, null,
                null, null, null, List.of(), "MESSAGE_READ_FAILED");
    }

    private static PushDetailDTO detail(MessageReadState state,
                                        String messageId,
                                        String pushId,
                                        MessageListDTO.SourceIdentity sourceIdentity,
                                        PushDetailDTO.OriginalSnapshot original,
                                        PushDetailDTO.CurrentRecheck current,
                                        String changeReason,
                                        List<String> missingFields,
                                        String reason) {
        return new PushDetailDTO(
                state,
                messageId,
                pushId,
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

    private static String id(Long value) {
        return positive(value) ? Long.toString(value) : null;
    }

    private static String firstText(String first, String second) {
        String value = normalizeMessageId(first);
        return value != null ? value : normalizeMessageId(second);
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private static <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
