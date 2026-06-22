package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.enums.UserPositionSideEnum;
import org.example.trademodel.enums.UserPositionSourceTypeEnum;
import org.example.trademodel.enums.UserPositionStatusEnum;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.UserPositionService;
import org.example.trademodel.vo.UserPositionVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserPositionServiceImpl implements UserPositionService {
    private static final String SOURCE_MANUAL = UserPositionSourceTypeEnum.MANUAL.name();
    private static final String STATUS_OPEN = UserPositionStatusEnum.OPEN.name();
    private static final String STATUS_CLOSED = UserPositionStatusEnum.CLOSED.name();

    private final UserPositionMapper userPositionMapper;

    public UserPositionServiceImpl(UserPositionMapper userPositionMapper) {
        this.userPositionMapper = userPositionMapper;
    }

    @Override
    public UserPositionVO manualOpen(CreateUserPositionReq request) {
        if (request == null) {
            throw new IllegalArgumentException("manual open request is required");
        }
        rejectForbiddenInputFields(request.getExtraFields());
        String assetSymbol = normalizeAssetSymbol(request.getAssetSymbol());
        UserPositionSideEnum side = UserPositionSideEnum.parse(request.getSide());
        UserPositionSourceTypeEnum sourceType = UserPositionSourceTypeEnum.requireManual(request.getSourceType());
        BigDecimal entryPrice = requirePositive(request.getEntryPrice(), "entry_price");
        BigDecimal quantity = requirePositive(request.getQuantity(), "quantity");
        BigDecimal leverage = requirePositive(request.getLeverage(), "leverage");
        BigDecimal stopLoss = optionalPositive(request.getStopLoss(), "stop_loss");
        BigDecimal takeProfit = optionalPositive(request.getTakeProfit(), "take_profit");
        LocalDateTime now = LocalDateTime.now();

        UserPositionDO row = new UserPositionDO();
        row.setAssetSymbol(assetSymbol);
        row.setSide(side.name());
        row.setStatus(STATUS_OPEN);
        row.setEntryPrice(entryPrice);
        row.setQuantity(quantity);
        row.setLeverage(leverage);
        row.setStopLoss(stopLoss);
        row.setTakeProfit(takeProfit);
        row.setOpenedAt(now);
        row.setClosedAt(null);
        row.setClosePrice(null);
        row.setCloseReason(null);
        row.setSourceType(sourceType.name());
        row.setSourceRefId(trimToNull(request.getSourceRefId()));
        applySafetyFlags(row);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        userPositionMapper.insert(row);
        return toVo(row);
    }

    @Override
    public UserPositionVO manualClose(Long id, CloseUserPositionReq request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("UserPosition id is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("manual close request is required");
        }
        rejectForbiddenInputFields(request.getExtraFields());
        BigDecimal closePrice = requirePositive(request.getClosePrice(), "close_price");
        UserPositionDO existing = userPositionMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("UserPosition not found: " + id);
        }
        UserPositionStatusEnum.requireClosable(existing.getStatus());
        LocalDateTime now = LocalDateTime.now();
        int updated = userPositionMapper.manualClose(
                id,
                now,
                closePrice,
                trimToNull(request.getCloseReason()),
                now
        );
        if (updated != 1) {
            throw new IllegalStateException("UserPosition manual close failed");
        }
        UserPositionDO closed = userPositionMapper.selectById(id);
        if (closed == null) {
            closed = existing;
            closed.setStatus(STATUS_CLOSED);
            closed.setClosedAt(now);
            closed.setClosePrice(closePrice);
            closed.setCloseReason(trimToNull(request.getCloseReason()));
            closed.setUpdatedAt(now);
        }
        applySafetyFlags(closed);
        return toVo(closed);
    }

    @Override
    public List<UserPositionVO> listOpenPositions() {
        return userPositionMapper.listOpenPositions().stream()
                .filter(row -> UserPositionStatusEnum.parse(row.getStatus()).visibleInOpenPositions())
                .map(UserPositionServiceImpl::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public UserPositionVO findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("UserPosition id is required");
        }
        UserPositionDO row = userPositionMapper.selectById(id);
        return row == null ? null : toVo(row);
    }

    private static void applySafetyFlags(UserPositionDO row) {
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
    }

    public static UserPositionVO toVo(UserPositionDO row) {
        if (row == null) {
            return null;
        }
        UserPositionVO vo = new UserPositionVO();
        vo.setId(row.getId());
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
        vo.setSourceType(SOURCE_MANUAL);
        vo.setSourceRefId(row.getSourceRefId());
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

    private static void rejectForbiddenInputFields(Map<String, Object> extraFields) {
        if (extraFields == null || extraFields.isEmpty()) {
            return;
        }
        for (String field : extraFields.keySet()) {
            String normalized = field == null ? "" : field.replace("_", "")
                    .replace("-", "")
                    .toLowerCase(Locale.ROOT);
            if (normalized.contains("order")
                    || normalized.contains("execution")
                    || normalized.contains("autotrading")
                    || normalized.contains("positionsync")) {
                throw new IllegalArgumentException("Forbidden UserPosition input field: " + field);
            }
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
