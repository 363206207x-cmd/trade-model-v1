package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.UserPositionSideEnum;
import org.example.trademodel.enums.UserPositionSourceTypeEnum;
import org.example.trademodel.enums.UserPositionStatusEnum;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.userposition.UserPositionConflictException;
import org.example.trademodel.userposition.UserPositionNotFoundException;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserPositionServiceImpl implements UserPositionService {
    private static final String STATUS_OPEN = UserPositionStatusEnum.OPEN.name();
    private static final Set<String> FORBIDDEN_OWNER_FIELDS = Set.of(
            "userid", "ownerid", "accountid", "principalid", "tenantid");

    private final UserPositionMapper userPositionMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private Clock clock = Clock.systemUTC();

    public UserPositionServiceImpl(UserPositionMapper userPositionMapper) {
        this(userPositionMapper, null);
    }

    @Autowired
    public UserPositionServiceImpl(UserPositionMapper userPositionMapper,
                                   ExecutionPlanMapper executionPlanMapper) {
        this.userPositionMapper = userPositionMapper;
        this.executionPlanMapper = executionPlanMapper;
    }

    @Override
    public UserPositionVO manualOpenForUser(Long userId, CreateUserPositionReq request) {
        requireUserId(userId);
        if (request == null) {
            throw new IllegalArgumentException("manual open request is required");
        }
        rejectForbiddenInputFields(request.getExtraFields());
        String submissionId = requireSubmissionId(request.getSubmissionId());
        String assetSymbol = normalizeAssetSymbol(request.getAssetSymbol());
        UserPositionSideEnum side = UserPositionSideEnum.parse(request.getSide());
        UserPositionSourceTypeEnum sourceType = UserPositionSourceTypeEnum.parseExplicit(request.getSourceType());
        BigDecimal entryPrice = requirePositive(request.getEntryPrice(), "entry_price");
        BigDecimal quantity = requirePositive(request.getQuantity(), "quantity");
        BigDecimal leverage = requirePositive(request.getLeverage(), "leverage");
        BigDecimal stopLoss = optionalPositive(request.getStopLoss(), "stop_loss");
        BigDecimal takeProfit = optionalPositive(request.getTakeProfit(), "take_profit");
        String finalPlanId = validateFinalPlanReference(sourceType, request.getFinalPlanId(), assetSymbol);
        LocalDateTime now = UtcLocalTimePolicy.now(clock);
        LocalDateTime openedAt = request.getOpenedAt();
        if (openedAt == null) {
            throw new IllegalArgumentException("opened_at is required");
        }
        if (openedAt.isAfter(now)) {
            throw new IllegalArgumentException("opened_at must not be in the future");
        }

        UserPositionDO row = new UserPositionDO();
        row.setUserId(userId);
        row.setSubmissionId(submissionId);
        row.setAssetSymbol(assetSymbol);
        row.setSide(side.name());
        row.setStatus(STATUS_OPEN);
        row.setEntryPrice(entryPrice);
        row.setQuantity(quantity);
        row.setLeverage(leverage);
        row.setStopLoss(stopLoss);
        row.setTakeProfit(takeProfit);
        row.setOpenedAt(openedAt);
        row.setClosedAt(null);
        row.setClosePrice(null);
        row.setCloseReason(null);
        row.setSourceType(sourceType.name());
        row.setSourceRefId(trimToNull(request.getSourceRefId()));
        row.setFinalPlanId(finalPlanId);
        applySafetyFlags(row);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        UserPositionDO existing = userPositionMapper.selectBySubmissionIdAndUserId(submissionId, userId);
        if (existing != null) {
            return toVo(requireSameOpenPayload(existing, row));
        }
        try {
            if (userPositionMapper.insert(row) != 1) {
                throw new IllegalStateException("UserPosition insert failed");
            }
            return toVo(row);
        } catch (DuplicateKeyException duplicate) {
            UserPositionDO canonical = userPositionMapper.selectBySubmissionIdAndUserId(submissionId, userId);
            if (canonical == null) {
                throw duplicate;
            }
            return toVo(requireSameOpenPayload(canonical, row));
        }
    }

    @Override
    public UserPositionVO manualCloseForUser(Long id, Long userId, CloseUserPositionReq request) {
        requireUserId(userId);
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("UserPosition id is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("manual close request is required");
        }
        rejectForbiddenInputFields(request.getExtraFields());
        String submissionId = requireSubmissionId(request.getSubmissionId());
        BigDecimal closePrice = requirePositive(request.getClosePrice(), "close_price");
        UserPositionDO existing = userPositionMapper.selectByIdAndUserId(id, userId);
        if (existing == null) {
            throw new UserPositionNotFoundException();
        }
        LocalDateTime now = UtcLocalTimePolicy.now(clock);
        LocalDateTime closedAt = request.getClosedAt();
        if (closedAt == null) {
            throw new IllegalArgumentException("closed_at is required");
        }
        if (closedAt.isAfter(now)) {
            throw new IllegalArgumentException("closed_at must not be in the future");
        }
        if (existing.getOpenedAt() != null && closedAt.isBefore(existing.getOpenedAt())) {
            throw new IllegalArgumentException("closed_at must not be before opened_at");
        }
        String closeReason = trimToNull(request.getCloseReason());
        if (!isActivePositionStatus(existing.getStatus())) {
            return toVo(requireSameClosePayload(existing, submissionId, closedAt, closePrice, closeReason));
        }
        int updated;
        try {
            updated = userPositionMapper.manualCloseByIdAndUserId(
                    id,
                    userId,
                    closedAt,
                    closePrice,
                    closeReason,
                    submissionId,
                    now
            );
        } catch (DuplicateKeyException duplicate) {
            UserPositionDO canonical = userPositionMapper.selectByCloseSubmissionIdAndUserId(submissionId, userId);
            if (canonical == null || !Objects.equals(canonical.getId(), id)) {
                throw new UserPositionConflictException("close submission_id was already used for another position");
            }
            return toVo(requireSameClosePayload(canonical, submissionId, closedAt, closePrice, closeReason));
        }
        if (updated != 1) {
            UserPositionDO current = userPositionMapper.selectByIdAndUserId(id, userId);
            if (current == null) {
                throw new UserPositionNotFoundException();
            }
            return toVo(requireSameClosePayload(current, submissionId, closedAt, closePrice, closeReason));
        }
        UserPositionDO closed = userPositionMapper.selectByIdAndUserId(id, userId);
        if (closed == null) {
            throw new UserPositionNotFoundException();
        }
        applySafetyFlags(closed);
        return toVo(closed);
    }

    @Override
    public List<UserPositionVO> listOpenPositionsForUser(Long userId) {
        requireUserId(userId);
        List<UserPositionDO> rows = userPositionMapper.listOpenByUserId(userId);
        return (rows == null ? List.<UserPositionDO>of() : rows).stream()
                .filter(row -> row != null && isActivePositionStatus(row.getStatus()))
                .map(UserPositionServiceImpl::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserPositionVO> listClosedPositionsForUser(Long userId, int limit) {
        requireUserId(userId);
        if (limit <= 0 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        List<UserPositionDO> rows = userPositionMapper.listClosedManualByUserId(userId, limit);
        return (rows == null ? List.<UserPositionDO>of() : rows).stream()
                .map(UserPositionServiceImpl::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public int countClosedPositionsForUser(Long userId) {
        requireUserId(userId);
        return userPositionMapper.countClosedByUserId(userId);
    }

    @Override
    public UserPositionVO findByIdForUser(Long id, Long userId) {
        requireUserId(userId);
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("UserPosition id is required");
        }
        UserPositionDO row = userPositionMapper.selectByIdAndUserId(id, userId);
        if (row == null) {
            throw new UserPositionNotFoundException();
        }
        return toVo(row);
    }

    private static void applySafetyFlags(UserPositionDO row) {
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
    }

    private static boolean isActivePositionStatus(String status) {
        try {
            return UserPositionStatusEnum.parse(status).visibleInOpenPositions();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static UserPositionVO toVo(UserPositionDO row) {
        if (row == null) {
            return null;
        }
        UserPositionVO vo = new UserPositionVO();
        vo.setId(row.getId());
        vo.setSubmissionId(row.getSubmissionId());
        vo.setCloseSubmissionId(row.getCloseSubmissionId());
        vo.setAssetSymbol(row.getAssetSymbol());
        vo.setSide(row.getSide());
        vo.setStatus(row.getStatus());
        vo.setEntryPrice(row.getEntryPrice());
        vo.setQuantity(row.getQuantity());
        vo.setLeverage(row.getLeverage());
        vo.setStopLoss(row.getStopLoss());
        vo.setTakeProfit(row.getTakeProfit());
        vo.setOpenedAt(row.getOpenedAt());
        vo.setClosedAt(row.getClosedAt());
        vo.setClosePrice(row.getClosePrice());
        vo.setCloseReason(row.getCloseReason());
        vo.setSourceType(UserPositionSourceTypeEnum.parseExplicit(row.getSourceType()).name());
        vo.setSourceRefId(row.getSourceRefId());
        vo.setFinalPlanId(row.getFinalPlanId());
        vo.setManualReviewRequired(true);
        vo.setNotTradeInstruction(true);
        vo.setNotAutoTrading(true);
        vo.setNotOrderExecution(true);
        vo.setNotPositionSync(true);
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private static String normalizeAssetSymbol(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("asset_symbol is required");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static BigDecimal optionalPositive(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }
        return requirePositive(value, fieldName);
    }

    private static String requireSubmissionId(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("submission_id is required");
        }
        if (normalized.length() > 128 || !normalized.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,127}")) {
            throw new IllegalArgumentException("submission_id must be 8-128 safe characters");
        }
        return normalized;
    }

    private static UserPositionDO requireSameOpenPayload(UserPositionDO existing, UserPositionDO requested) {
        if (!Objects.equals(existing.getAssetSymbol(), requested.getAssetSymbol())
                || !Objects.equals(existing.getSide(), requested.getSide())
                || !sameNumber(existing.getEntryPrice(), requested.getEntryPrice())
                || !sameNumber(existing.getQuantity(), requested.getQuantity())
                || !sameNumber(existing.getLeverage(), requested.getLeverage())
                || !sameNumber(existing.getStopLoss(), requested.getStopLoss())
                || !sameNumber(existing.getTakeProfit(), requested.getTakeProfit())
                || !Objects.equals(existing.getOpenedAt(), requested.getOpenedAt())
                || !Objects.equals(existing.getSourceType(), requested.getSourceType())
                || !Objects.equals(existing.getSourceRefId(), requested.getSourceRefId())
                || !Objects.equals(existing.getFinalPlanId(), requested.getFinalPlanId())) {
            throw new UserPositionConflictException("submission_id payload does not match the original position");
        }
        return existing;
    }

    private static UserPositionDO requireSameClosePayload(UserPositionDO existing,
                                                          String submissionId,
                                                          LocalDateTime closedAt,
                                                          BigDecimal closePrice,
                                                          String closeReason) {
        if (!"CLOSED".equals(existing.getStatus())
                || !Objects.equals(existing.getCloseSubmissionId(), submissionId)
                || !Objects.equals(existing.getClosedAt(), closedAt)
                || !sameNumber(existing.getClosePrice(), closePrice)
                || !Objects.equals(existing.getCloseReason(), closeReason)) {
            throw new UserPositionConflictException("UserPosition close state does not match this submission_id");
        }
        return existing;
    }

    private static boolean sameNumber(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private static void rejectForbiddenInputFields(Map<String, Object> extraFields) {
        if (extraFields == null || extraFields.isEmpty()) {
            return;
        }
        for (String field : extraFields.keySet()) {
            String normalized = field == null ? "" : field.replace("_", "")
                    .replace("-", "")
                    .toLowerCase(Locale.ROOT);
            if (FORBIDDEN_OWNER_FIELDS.contains(normalized)
                    || normalized.contains("order")
                    || normalized.contains("execution")
                    || normalized.contains("autotrading")
                    || normalized.contains("positionsync")) {
                throw new IllegalArgumentException("Forbidden UserPosition input field: " + field);
            }
        }
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private String validateFinalPlanReference(UserPositionSourceTypeEnum sourceType,
                                              String requestedPlanId,
                                              String assetSymbol) {
        String finalPlanId = trimToNull(requestedPlanId);
        if (!sourceType.finalPlanRequired()) {
            if (finalPlanId != null) {
                throw new IllegalArgumentException(
                        "MANUAL_INDEPENDENT must not carry final_plan_id; use SYSTEM_PLAN_POSITION");
            }
            return null;
        }
        if (finalPlanId == null) {
            throw new IllegalArgumentException("SYSTEM_PLAN_POSITION requires final_plan_id");
        }
        if (executionPlanMapper == null) {
            throw new IllegalStateException("FinalExecutionPlan validation is unavailable");
        }
        ExecutionPlanDO plan = executionPlanMapper.selectValidatedFinalByPlanIdAndSymbol(
                finalPlanId, assetSymbol);
        if (plan == null || !Boolean.TRUE.equals(plan.getFinalPlan())
                || !"PASS".equals(plan.getRuleValidationStatus())) {
            throw new IllegalArgumentException("final_plan_id must reference a rule-validated FinalExecutionPlan for the same asset");
        }
        return finalPlanId;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
