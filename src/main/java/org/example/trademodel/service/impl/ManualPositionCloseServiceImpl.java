package org.example.trademodel.service.impl;

import org.example.trademodel.dto.req.CloseManualPositionReq;
import org.example.trademodel.entity.PositionMonitorRecordDO;
import org.example.trademodel.entity.PositionTradeResultDO;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PositionMonitorRecordMapper;
import org.example.trademodel.mapper.PositionTradeResultMapper;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.service.ManualPositionCloseService;
import org.example.trademodel.service.PositionTradeResultService;
import org.example.trademodel.vo.RealPositionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ManualPositionCloseServiceImpl implements ManualPositionCloseService {
    private static final String MANUAL_SOURCE_TYPE = "MANUAL_INPUT";
    private static final String MANUAL_SOURCE_NAME = "USER_MANUAL";
    private static final String DEFAULT_CLOSE_REASON = "MANUAL_CLOSE";
    private static final String DEFAULT_USER_ACTION_TYPE = "CLOSE";

    private final RealPositionMapper realPositionMapper;
    private final PositionMonitorRecordMapper positionMonitorRecordMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final PositionTradeResultService positionTradeResultService;
    private final PositionTradeResultMapper positionTradeResultMapper;

    public ManualPositionCloseServiceImpl(
            RealPositionMapper realPositionMapper,
            PositionMonitorRecordMapper positionMonitorRecordMapper,
            DecisionResultMapper decisionResultMapper,
            PositionTradeResultService positionTradeResultService,
            PositionTradeResultMapper positionTradeResultMapper) {
        this.realPositionMapper = realPositionMapper;
        this.positionMonitorRecordMapper = positionMonitorRecordMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.positionTradeResultService = positionTradeResultService;
        this.positionTradeResultMapper = positionTradeResultMapper;
    }

    @Override
    @Transactional
    public CloseResult close(String positionId, CloseManualPositionReq req) {
        String normalizedPositionId = positionId == null ? null : positionId.trim();
        if (normalizedPositionId == null || normalizedPositionId.isEmpty()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        RealPositionVO position = realPositionMapper.selectOpenPositionById(normalizedPositionId);
        if (position == null) {
            RealPositionVO maybeExists = realPositionMapper.selectPositionById(normalizedPositionId);
            if (maybeExists == null) {
                throw new IllegalArgumentException("持仓不存在或已关闭");
            }
            if (!MANUAL_SOURCE_TYPE.equals(maybeExists.getSourceType())
                    || !MANUAL_SOURCE_NAME.equals(maybeExists.getSourceName())) {
                throw new IllegalArgumentException("不是手动持仓，不允许关闭");
            }
            throw new IllegalArgumentException("持仓不存在或已关闭");
        }
        if (!MANUAL_SOURCE_TYPE.equals(position.getSourceType())
                || !MANUAL_SOURCE_NAME.equals(position.getSourceName())) {
            throw new IllegalArgumentException("不是手动持仓，不允许关闭");
        }

        PositionMonitorRecordDO latest = positionMonitorRecordMapper.selectLatestByPositionId(normalizedPositionId);
        FallbackResult fallback = applyFallback(position, req);
        PositionTradeResultDO tradeResult = positionTradeResultService.createFromClose(position, fallback.req(), latest);
        positionTradeResultMapper.insert(tradeResult);

        LocalDateTime now = LocalDateTime.now();
        int affected = realPositionMapper.closeManualPositionById(normalizedPositionId, now);
        if (affected <= 0) {
            throw new IllegalArgumentException("持仓不存在或已关闭");
        }

        String reviewAnalysisId = resolveReviewAnalysisId(normalizedPositionId, position, latest);
        String analysisReviewUrl = reviewAnalysisId == null ? null : "/review/" + reviewAnalysisId;
        String message = "已记录平仓，可选择进入交易级复盘。";
        if (fallback.exitPriceFallbackUsed()) {
            message += " 本次平仓价使用当前价兜底，后续可在交易级复盘中校正。";
        }

        CloseResult out = new CloseResult();
        out.setPositionId(normalizedPositionId);
        out.setPositionStatus("CLOSED");
        out.setTradeResultId(tradeResult.getTradeResultId());
        out.setTradeReviewUrl("/trade-review/" + tradeResult.getTradeResultId());
        out.setReviewAnalysisId(reviewAnalysisId);
        out.setAnalysisReviewUrl(analysisReviewUrl);
        out.setReviewLevel("TRADE");
        out.setReviewMessage(message);
        out.setExitPriceFallbackUsed(fallback.exitPriceFallbackUsed());
        out.setTradeResult(tradeResult);
        return out;
    }

    private String resolveReviewAnalysisId(String positionId, RealPositionVO position, PositionMonitorRecordDO latest) {
        if (latest != null && latest.getAnalysisId() != null && !latest.getAnalysisId().trim().isEmpty()) {
            return latest.getAnalysisId().trim();
        }
        String symbol = normalizeSymbol(position != null ? position.getSymbol() : null);
        if (symbol == null) {
            return null;
        }
        String fallback = decisionResultMapper.selectLatestAnalysisIdBySymbol(symbol);
        if (fallback == null || fallback.trim().isEmpty()) {
            return null;
        }
        return fallback.trim();
    }

    private FallbackResult applyFallback(RealPositionVO position, CloseManualPositionReq req) {
        CloseManualPositionReq use = req == null ? new CloseManualPositionReq() : req;
        boolean fallbackUsed = false;
        BigDecimal exit = use.getExitPrice();
        if (exit == null || exit.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal candidate = positiveOrNull(position.getMarkPrice());
            if (candidate == null) {
                candidate = positiveOrNull(position.getAvgOpenPrice());
            }
            if (candidate == null) {
                throw new IllegalArgumentException("exitPrice must be > 0");
            }
            use.setExitPrice(candidate);
            fallbackUsed = true;
        }
        if (use.getCloseReason() == null || use.getCloseReason().trim().isEmpty()) {
            use.setCloseReason(DEFAULT_CLOSE_REASON);
        }
        if (use.getUserActionType() == null || use.getUserActionType().trim().isEmpty()) {
            use.setUserActionType(DEFAULT_USER_ACTION_TYPE);
        }
        return new FallbackResult(use, fallbackUsed);
    }

    private static BigDecimal positiveOrNull(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return val;
    }

    private static String normalizeSymbol(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toUpperCase();
        return s.isEmpty() ? null : s;
    }

    private record FallbackResult(CloseManualPositionReq req, boolean exitPriceFallbackUsed) {}
}
