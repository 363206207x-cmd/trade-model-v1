package org.example.trademodel.service.impl;

import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogSourceViewPolicy;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogicStatusEnum;
import org.example.trademodel.positionmonitorlog.PositionMonitorSuggestedActionEnum;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.springframework.stereotype.Service;

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
        PositionMonitorLogicStatusEnum logicStatus = PositionMonitorLogicStatusEnum.parse(command.getLogicStatus());
        PositionMonitorSuggestedActionEnum suggestedAction = PositionMonitorSuggestedActionEnum.parse(command.getSuggestedAction());
        String riskLevel = requireText(command.getRiskLevel(), "risk_level").toUpperCase(Locale.ROOT);
        String executionPlanId = optionalText(command.getExecutionPlanId());
        String traceId = optionalText(command.getTraceId());
        String reason = optionalText(command.getReason());
        String evidenceSnapshot = boundedSnapshot(command.getEvidenceSnapshot(), "evidence_snapshot");
        String scoreSnapshot = boundedSnapshot(command.getScoreSnapshot(), "score_snapshot");
        String decisionSnapshot = boundedSnapshot(command.getDecisionSnapshot(), "decision_snapshot");
        String riskSnapshot = boundedSnapshot(command.getRiskSnapshot(), "risk_snapshot");

        rejectExecutableWords("suggested_action", suggestedAction.name());
        rejectExecutableWords("reason", reason);
        rejectExecutableWords("evidence_snapshot", evidenceSnapshot);
        rejectExecutableWords("score_snapshot", scoreSnapshot);
        rejectExecutableWords("decision_snapshot", decisionSnapshot);
        rejectExecutableWords("risk_snapshot", riskSnapshot);

        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setPositionId(positionId);
        row.setAnalysisId(analysisId);
        row.setExecutionPlanId(executionPlanId);
        row.setCurrentPrice(currentPrice);
        row.setLogicStatus(logicStatus.name());
        row.setRiskLevel(riskLevel);
        row.setSuggestedAction(suggestedAction.name());
        row.setReason(reason);
        row.setEvidenceSnapshot(evidenceSnapshot);
        row.setScoreSnapshot(scoreSnapshot);
        row.setDecisionSnapshot(decisionSnapshot);
        row.setRiskSnapshot(riskSnapshot);
        row.setTraceId(traceId);
        row.setCreatedAt(LocalDateTime.now());
        int inserted = positionMonitorLogMapper.insert(row);
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
        dto.setLogicStatus(row.getLogicStatus());
        dto.setRiskLevel(row.getRiskLevel());
        dto.setSuggestedAction(row.getSuggestedAction());
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

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private static void rejectExecutableWords(String fieldName, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
        if (containsWord(normalized, "close")
                || containsWord(normalized, "reduce")
                || containsWord(normalized, "reverse")
                || containsWord(normalized, "open")
                || containsWord(normalized, "buy")
                || containsWord(normalized, "sell")
                || normalized.contains("place order")
                || containsWord(normalized, "execute")
                || normalized.contains("orderaction")
                || normalized.contains("executionaction")
                || normalized.contains("autotradingaction")) {
            throw new IllegalArgumentException("Forbidden executable monitor log content in " + fieldName);
        }
    }

    private static boolean containsWord(String value, String word) {
        return value.matches(".*\\b" + word + "\\b.*");
    }
}
