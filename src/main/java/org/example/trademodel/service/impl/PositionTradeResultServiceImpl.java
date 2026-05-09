package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.CloseManualPositionReq;
import org.example.trademodel.entity.PositionMonitorRecordDO;
import org.example.trademodel.entity.PositionTradeResultDO;
import org.example.trademodel.service.PositionTradeResultService;
import org.example.trademodel.vo.RealPositionVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PositionTradeResultServiceImpl implements PositionTradeResultService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String CLOSE_REASON_DEFAULT = "MANUAL_CLOSE";
    private static final String USER_ACTION_TYPE_DEFAULT = "CLOSE";
    private static final String DEVIATION_UNKNOWN = "UNKNOWN";
    private static final String REVIEW_NOT_STARTED = "NOT_STARTED";

    @Override
    public PositionTradeResultDO createFromClose(
            RealPositionVO position,
            CloseManualPositionReq req,
            PositionMonitorRecordDO latestMonitorRecord
    ) {
        if (position == null) {
            throw new IllegalArgumentException("position is required");
        }
        BigDecimal avgOpenPrice = position.getAvgOpenPrice();
        if (avgOpenPrice == null || avgOpenPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("avgOpenPrice must be > 0");
        }
        if (req == null || req.getExitPrice() == null || req.getExitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("exitPrice must be > 0");
        }

        String positionSide = normalizeOrEmpty(position.getPositionSide());
        BigDecimal exitPrice = req.getExitPrice();
        BigDecimal realizedPnlPct = calcRealizedPnlPct(positionSide, avgOpenPrice, exitPrice);
        BigDecimal realizedPnl = calcRealizedPnl(positionSide, avgOpenPrice, exitPrice, position.getPositionQuantity());
        LocalDateTime now = LocalDateTime.now();

        PositionTradeResultDO out = new PositionTradeResultDO();
        out.setTradeResultId(UUID.randomUUID().toString());
        out.setPositionId(position.getPositionId());
        out.setSymbol(normalizeOrEmpty(position.getSymbol()));
        out.setPositionSide(positionSide);
        out.setAvgOpenPrice(avgOpenPrice);
        out.setPositionOpenTime(position.getPositionOpenTime());
        out.setPositionQuantity(position.getPositionQuantity());
        out.setExitPrice(exitPrice);
        out.setCloseTime(now);
        out.setRealizedPnl(realizedPnl);
        out.setRealizedPnlPct(realizedPnlPct);
        out.setCloseReason(normalizeOrDefault(req.getCloseReason(), CLOSE_REASON_DEFAULT));
        out.setUserActionType(normalizeOrDefault(req.getUserActionType(), USER_ACTION_TYPE_DEFAULT));
        out.setUserRemark(trimToNull(req.getUserRemark()));
        out.setUserDeviationFromSystemSuggestion(DEVIATION_UNKNOWN);
        out.setReviewStatus(REVIEW_NOT_STARTED);
        out.setCreateTime(now);
        out.setUpdateTime(now);
        if (latestMonitorRecord != null) {
            out.setLinkedAnalysisId(trimToNull(latestMonitorRecord.getAnalysisId()));
            out.setLinkedPlanId(trimToNull(latestMonitorRecord.getPlanId()));
            out.setLatestMonitorRecordId(trimToNull(latestMonitorRecord.getPositionMonitorRecordId()));
            out.setSystemSuggestedActionAtClose(trimToNull(latestMonitorRecord.getSystemSuggestedAction()));
        }
        return out;
    }

    private static BigDecimal calcRealizedPnl(
            String positionSide,
            BigDecimal avgOpenPrice,
            BigDecimal exitPrice,
            BigDecimal quantity
    ) {
        if (quantity == null) {
            return null;
        }
        BigDecimal delta = "SHORT".equals(positionSide)
                ? avgOpenPrice.subtract(exitPrice)
                : exitPrice.subtract(avgOpenPrice);
        return delta.multiply(quantity).setScale(8, RoundingMode.HALF_UP);
    }

    private static BigDecimal calcRealizedPnlPct(String positionSide, BigDecimal avgOpenPrice, BigDecimal exitPrice) {
        BigDecimal delta = "SHORT".equals(positionSide)
                ? avgOpenPrice.subtract(exitPrice)
                : exitPrice.subtract(avgOpenPrice);
        return delta
                .multiply(HUNDRED)
                .divide(avgOpenPrice, 4, RoundingMode.HALF_UP);
    }

    private static String normalizeOrEmpty(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase();
    }

    private static String normalizeOrDefault(String raw, String defaultValue) {
        String v = trimToNull(raw);
        if (v == null) {
            return defaultValue;
        }
        return v.toUpperCase();
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }
}
