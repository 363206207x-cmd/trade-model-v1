package org.example.trademodel.service.impl;

import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitor.PositionRiskLevelEnum;
import org.example.trademodel.positionmonitorlog.PositionEntryLogicStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorConclusionEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogSourceViewPolicy;
import org.example.trademodel.positionmonitorlog.PositionMonitorSourceStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorSuggestedActionEnum;
import org.example.trademodel.positionmonitorlog.PositionReversalStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionRiskChangeReasonEnum;
import org.example.trademodel.positionmonitorlog.PositionRiskTrendEnum;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PositionMonitorLogServiceImpl implements org.example.trademodel.service.PositionMonitorLogService {
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;
    static final int MAX_SNAPSHOT_LENGTH = 8000;

    private final PositionMonitorLogMapper positionMonitorLogMapper;
    private final UserPositionMapper userPositionMapper;

    public PositionMonitorLogServiceImpl(PositionMonitorLogMapper positionMonitorLogMapper,
                                         UserPositionMapper userPositionMapper) {
        this.positionMonitorLogMapper = positionMonitorLogMapper;
        this.userPositionMapper = userPositionMapper;
    }

    @Override
    public PositionMonitorLogDTO recordMonitorRunForUser(Long userId, RecordPositionMonitorLogCommand command) {
        requireUserId(userId);
        return recordMonitorRun(command, positionId -> userPositionMapper.selectByIdAndUserId(positionId, userId));
    }

    @Override
    public PositionMonitorLogDTO recordMonitorRunForSystem(RecordPositionMonitorLogCommand command) {
        return recordMonitorRun(command, userPositionMapper::selectClaimedByIdForSystem);
    }

    private PositionMonitorLogDTO recordMonitorRun(RecordPositionMonitorLogCommand command,
                                                    java.util.function.Function<Long, UserPositionDO> positionLookup) {
        if (command == null) {
            throw new IllegalArgumentException("monitor log command is required");
        }
        Long positionId = requirePositiveId(command.getPositionId(), "position_id");
        UserPositionDO position = positionLookup.apply(positionId);
        if (position == null) {
            throw new UserPositionNotFoundException();
        }
        requireRecordablePositionStatus(position.getStatus());

        String analysisId = requireText(command.getAnalysisId(), "analysis_id");
        BigDecimal currentPrice = requirePositive(command.getCurrentPrice(), "current_price");
        PositionMonitorSourceStatusEnum monitorSourceStatus = parseEnum(
                command.getMonitorSourceStatus(), PositionMonitorSourceStatusEnum.class, "source_status");
        String markPriceSource = monitorSourceStatus == PositionMonitorSourceStatusEnum.VERIFIED
                ? requireText(command.getMarkPriceSource(), "mark_price_source")
                : optionalText(command.getMarkPriceSource());
        LocalDateTime recordedAt = LocalDateTime.now();
        LocalDateTime observedAt = requireTime(command.getObservedAt(), "observed_at");
        LocalDateTime freshUntil = requireTime(command.getFreshUntil(), "fresh_until");
        if (freshUntil.isBefore(observedAt)) {
            throw new IllegalArgumentException("fresh_until must not be before observed_at");
        }
        if (monitorSourceStatus == PositionMonitorSourceStatusEnum.VERIFIED
                && !freshUntil.isAfter(observedAt)) {
            throw new IllegalArgumentException("verified monitor fresh_until must be after observed_at");
        }
        if (monitorSourceStatus == PositionMonitorSourceStatusEnum.VERIFIED
                && (observedAt.isAfter(recordedAt) || !recordedAt.isBefore(freshUntil))) {
            throw new IllegalArgumentException("verified monitor result must be fresh when recorded");
        }

        PositionEntryLogicStatusEnum entryLogicStatus = null;
        PositionMonitorConclusionEnum monitorConclusion = null;
        PositionReversalStatusEnum reversalStatus = null;
        PositionRiskChangeReasonEnum riskChangeReason = null;
        PositionRiskLevelEnum riskLevel = null;
        PositionRiskTrendEnum riskTrend = null;
        PositionMonitorSuggestedActionEnum suggestedAction = null;
        if (monitorSourceStatus == PositionMonitorSourceStatusEnum.VERIFIED) {
            entryLogicStatus = parseEnum(
                    command.getEntryLogicStatus(), PositionEntryLogicStatusEnum.class, "entry_logic_status");
            boolean manualIndependent = isManualIndependent(position.getSourceType());
            if (manualIndependent && entryLogicStatus != PositionEntryLogicStatusEnum.NOT_APPLICABLE) {
                throw new IllegalArgumentException(
                        "MANUAL_INDEPENDENT without an original thesis must use NOT_APPLICABLE entry_logic_status");
            }
            if (!manualIndependent && entryLogicStatus == PositionEntryLogicStatusEnum.NOT_APPLICABLE) {
                throw new IllegalArgumentException(
                        "SYSTEM_PLAN_POSITION cannot use NOT_APPLICABLE entry_logic_status");
            }
            reversalStatus = parseEnum(
                    command.getReversalStatus(), PositionReversalStatusEnum.class, "reversal_status");
            riskChangeReason = parseEnum(
                    command.getRiskChangeReason(), PositionRiskChangeReasonEnum.class, "risk_change_reason");
            riskLevel = parseEnum(command.getRiskLevel(), PositionRiskLevelEnum.class, "risk_level");
            riskTrend = parseEnum(command.getRiskTrend(), PositionRiskTrendEnum.class, "risk_trend");
            String conclusionValue = optionalText(command.getMonitorConclusion());
            String actionValue = optionalText(command.getSuggestedAction());
            if (entryLogicStatus == PositionEntryLogicStatusEnum.NOT_APPLICABLE
                    && (conclusionValue == null) != (actionValue == null)) {
                throw new IllegalArgumentException(
                        "manual monitor_conclusion and suggested_action must either both be present or both be absent");
            }
            if (entryLogicStatus != PositionEntryLogicStatusEnum.NOT_APPLICABLE || conclusionValue != null) {
                monitorConclusion = parseEnum(
                        conclusionValue, PositionMonitorConclusionEnum.class, "monitor_conclusion");
                suggestedAction = PositionMonitorSuggestedActionEnum.parse(actionValue);
                if (!suggestedAction.isAllowedFor(monitorConclusion)) {
                    throw new IllegalArgumentException("suggested_action is not valid for monitor_conclusion");
                }
            }
        } else {
            requireMissing(command.getEntryLogicStatus(), "entry_logic_status");
            requireMissing(command.getMonitorConclusion(), "monitor_conclusion");
            requireMissing(command.getReversalStatus(), "reversal_status");
            requireMissing(command.getRiskChangeReason(), "risk_change_reason");
            requireMissing(command.getRiskLevel(), "risk_level");
            requireMissing(command.getRiskTrend(), "risk_trend");
            requireMissing(command.getSuggestedAction(), "suggested_action");
        }
        String executionPlanId = optionalText(command.getExecutionPlanId());
        String traceId = optionalText(command.getTraceId());
        String reason = optionalText(command.getReason());
        String evidenceSnapshot = boundedSnapshot(command.getEvidenceSnapshot(), "evidence_snapshot");
        String scoreSnapshot = boundedSnapshot(command.getScoreSnapshot(), "score_snapshot");
        String decisionSnapshot = boundedSnapshot(command.getDecisionSnapshot(), "decision_snapshot");
        String riskSnapshot = boundedSnapshot(command.getRiskSnapshot(), "risk_snapshot");

        rejectAutomaticExecutionWords("reason", reason);
        rejectAutomaticExecutionWords("evidence_snapshot", evidenceSnapshot);
        rejectAutomaticExecutionWords("score_snapshot", scoreSnapshot);
        rejectAutomaticExecutionWords("decision_snapshot", decisionSnapshot);
        rejectAutomaticExecutionWords("risk_snapshot", riskSnapshot);

        PositionMonitorLogDO row = new PositionMonitorLogDO();
        String monitorRunKey = optionalText(command.getMonitorRunKey());
        if (monitorRunKey != null && monitorRunKey.length() > 180) {
            throw new IllegalArgumentException("monitor_run_key is too long");
        }
        row.setMonitorRunKey(monitorRunKey);
        row.setPositionId(positionId);
        row.setAnalysisId(analysisId);
        row.setExecutionPlanId(executionPlanId);
        row.setCurrentPrice(currentPrice);
        row.setMarkPriceSource(markPriceSource);
        row.setEntryLogicStatus(entryLogicStatus == null ? null : entryLogicStatus.name());
        row.setMonitorConclusion(monitorConclusion == null ? null : monitorConclusion.name());
        row.setReversalStatus(reversalStatus == null ? null : reversalStatus.name());
        row.setRiskChangeReason(riskChangeReason == null ? null : riskChangeReason.name());
        row.setRiskLevel(riskLevel == null ? null : riskLevel.name());
        row.setRiskTrend(riskTrend == null ? null : riskTrend.name());
        row.setSuggestedAction(suggestedAction == null ? null : suggestedAction.name());
        row.setMonitorSourceStatus(monitorSourceStatus.name());
        row.setObservedAt(observedAt);
        row.setFreshUntil(freshUntil);
        row.setReason(reason);
        row.setEvidenceSnapshot(evidenceSnapshot);
        row.setScoreSnapshot(scoreSnapshot);
        row.setDecisionSnapshot(decisionSnapshot);
        row.setRiskSnapshot(riskSnapshot);
        row.setTraceId(traceId);
        row.setCreatedAt(recordedAt);
        int inserted;
        if (monitorRunKey == null) {
            inserted = positionMonitorLogMapper.insert(row);
        } else {
            try {
                inserted = positionMonitorLogMapper.insertIfAbsent(row);
            } catch (DuplicateKeyException duplicate) {
                inserted = 0;
            }
            if (inserted == 0) {
                PositionMonitorLogDO canonical = positionMonitorLogMapper.selectByMonitorRunKey(monitorRunKey);
                if (canonical == null || !positionId.equals(canonical.getPositionId())) {
                    throw new IllegalStateException("PositionMonitorLog idempotency claim mismatch");
                }
                return toDto(canonical);
            }
        }
        if (inserted != 1) {
            throw new IllegalStateException("PositionMonitorLog insert failed");
        }
        return toDto(row);
    }

    @Override
    public PositionMonitorLogDTO findByIdForSystem(Long logId) {
        Long id = requirePositiveId(logId, "log_id");
        return toDto(positionMonitorLogMapper.selectById(id));
    }

    @Override
    public List<PositionMonitorLogDTO> listByPositionIdForUser(Long userId, Long positionId, Integer limit) {
        requireUserId(userId);
        requireOwnedPosition(positionId, userId);
        return listByPositionId(positionId, limit);
    }

    @Override
    public List<PositionMonitorLogDTO> listAllByPositionIdForUserReview(Long userId, Long positionId) {
        requireUserId(userId);
        requireOwnedPosition(positionId, userId);
        return listAllByPositionIdForReview(positionId);
    }

    @Override
    public List<PositionMonitorLogDTO> listByPositionIdForSystem(Long positionId, Integer limit) {
        requireClaimedPosition(positionId);
        return listByPositionId(positionId, limit);
    }

    @Override
    public List<PositionMonitorLogDTO> listAllByPositionIdForSystemReview(Long positionId) {
        requireClaimedPosition(positionId);
        return listAllByPositionIdForReview(positionId);
    }

    private List<PositionMonitorLogDTO> listByPositionId(Long positionId, Integer limit) {
        Long id = requirePositiveId(positionId, "position_id");
        int sanitizedLimit = sanitizeLimit(limit);
        return positionMonitorLogMapper.listByPositionId(id, sanitizedLimit).stream()
                .map(PositionMonitorLogServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    private List<PositionMonitorLogDTO> listAllByPositionIdForReview(Long positionId) {
        Long id = requirePositiveId(positionId, "position_id");
        return positionMonitorLogMapper.listAllByPositionIdForReview(id).stream()
                .map(PositionMonitorLogServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PositionMonitorLogDTO> listByAnalysisIdForSystem(String analysisId, Integer limit) {
        String id = requireText(analysisId, "analysis_id");
        int sanitizedLimit = sanitizeLimit(limit);
        return positionMonitorLogMapper.listByAnalysisId(id, sanitizedLimit).stream()
                .map(PositionMonitorLogServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    static PositionMonitorLogDTO toDto(PositionMonitorLogDO row) {
        if (row == null) {
            return null;
        }
        PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
        dto.setLogId(row.getLogId());
        dto.setPositionId(row.getPositionId());
        dto.setAnalysisId(row.getAnalysisId());
        dto.setExecutionPlanId(row.getExecutionPlanId());
        dto.setCurrentPrice(row.getCurrentPrice());
        dto.setMarkPriceSource(row.getMarkPriceSource());
        dto.setLogicStatus(row.getLogicStatus());
        dto.setEntryLogicStatus(row.getEntryLogicStatus());
        dto.setMonitorConclusion(row.getMonitorConclusion());
        dto.setReversalStatus(row.getReversalStatus());
        dto.setRiskChangeReason(row.getRiskChangeReason());
        dto.setRiskLevel(row.getRiskLevel());
        dto.setRiskTrend(row.getRiskTrend());
        dto.setSuggestedAction(row.getSuggestedAction());
        dto.setMonitorSourceStatus(row.getMonitorSourceStatus());
        dto.setObservedAt(row.getObservedAt());
        dto.setFreshUntil(row.getFreshUntil());
        dto.setReason(row.getReason());
        dto.setEvidenceSnapshot(row.getEvidenceSnapshot());
        dto.setScoreSnapshot(row.getScoreSnapshot());
        dto.setDecisionSnapshot(row.getDecisionSnapshot());
        dto.setRiskSnapshot(row.getRiskSnapshot());
        dto.setTraceId(row.getTraceId());
        dto.setCreatedAt(row.getCreatedAt());
        dto.setReviewOnly(true);
        dto.setManualReviewOnly(true);
        dto.setNotTradeInstruction(true);
        dto.setNotExecutable(true);
        dto.setNotAutoClose(true);
        dto.setNotAutoReverse(true);
        dto.setNotOrderExecution(true);
        dto.setNotAutoTrading(true);
        dto.setNotPositionMutation(true);
        return PositionMonitorLogSourceViewPolicy.sanitize(dto);
    }

    static int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be <= " + MAX_LIMIT);
        }
        return limit;
    }

    private static Long requirePositiveId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private void requireOwnedPosition(Long positionId, Long userId) {
        Long id = requirePositiveId(positionId, "position_id");
        if (userPositionMapper.selectByIdAndUserId(id, userId) == null) {
            throw new UserPositionNotFoundException();
        }
    }

    private void requireClaimedPosition(Long positionId) {
        Long id = requirePositiveId(positionId, "position_id");
        if (userPositionMapper.selectClaimedByIdForSystem(id) == null) {
            throw new UserPositionNotFoundException();
        }
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        String text = optionalText(value);
        if (text == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return text;
    }

    private static LocalDateTime requireTime(LocalDateTime value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> type, String fieldName) {
        String normalized = requireText(value, fieldName).toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " is not supported");
        }
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isManualIndependent(String sourceType) {
        String normalized = optionalText(sourceType);
        if (normalized == null) {
            return false;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return "MANUAL_INDEPENDENT".equals(normalized)
                || "MANUAL".equals(normalized)
                || "MANUAL_POSITION".equals(normalized);
    }

    private static void requireMissing(String value, String fieldName) {
        if (optionalText(value) != null) {
            throw new IllegalArgumentException(fieldName + " must be absent when source_status is not VERIFIED");
        }
    }

    private static String boundedSnapshot(String value, String fieldName) {
        String text = optionalText(value);
        if (text != null && text.length() > MAX_SNAPSHOT_LENGTH) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + MAX_SNAPSHOT_LENGTH);
        }
        return text;
    }

    private static void requireRecordablePositionStatus(String status) {
        String normalized = optionalText(status);
        if ("OPEN".equals(normalized) || "PARTIALLY_CLOSED".equals(normalized)) {
            return;
        }
        if ("CLOSED".equals(normalized)) {
            throw new UserPositionConflictException("CLOSED UserPosition cannot record new monitor run logs");
        }
        throw new IllegalArgumentException("UserPosition status must be OPEN or PARTIALLY_CLOSED");
    }

    private static void rejectAutomaticExecutionWords(String fieldName, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
        if (normalized.contains("auto close")
                || normalized.contains("auto reduce")
                || normalized.contains("auto reverse")
                || normalized.contains("auto open")
                || normalized.contains("auto buy")
                || normalized.contains("auto sell")
                || normalized.contains("auto order")
                || normalized.contains("place order")
                || normalized.contains("execute order")
                || normalized.contains("orderaction")
                || normalized.contains("executionaction")
                || normalized.contains("autotradingaction")) {
            throw new IllegalArgumentException("Forbidden executable monitor log content in " + fieldName);
        }
    }
}
