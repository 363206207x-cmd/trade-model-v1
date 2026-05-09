package org.example.trademodel.controller;

import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.dto.req.CloseManualPositionReq;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.service.ManualPositionCloseService;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.vo.RealPositionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/positions")
public class ManualPositionController {
    private static final Logger log = LoggerFactory.getLogger(ManualPositionController.class);

    private static final String MANUAL_SOURCE_TYPE = "MANUAL_INPUT";
    private static final String MANUAL_SOURCE_NAME = "USER_MANUAL";

    private final RealPositionMapper realPositionMapper;
    private final MarketQuoteClient marketQuoteClient;
    private final PositionMonitorService positionMonitorService;
    private final ManualPositionCloseService manualPositionCloseService;

    public ManualPositionController(RealPositionMapper realPositionMapper,
                                    MarketQuoteClient marketQuoteClient,
                                    PositionMonitorService positionMonitorService,
                                    ManualPositionCloseService manualPositionCloseService) {
        this.realPositionMapper = realPositionMapper;
        this.marketQuoteClient = marketQuoteClient;
        this.positionMonitorService = positionMonitorService;
        this.manualPositionCloseService = manualPositionCloseService;
    }

    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<RealPositionVO>> createManualPosition(@RequestBody ManualPositionCreateReq req) {
        if (req == null) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("request body is required"));
        }

        String symbol = normalizeSymbol(req.getSymbol());
        if (symbol == null) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("symbol must not be blank"));
        }

        String positionSide = normalizePositionSide(req.getPositionSide());
        if (positionSide == null) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("positionSide must be LONG or SHORT"));
        }

        BigDecimal avgOpenPrice = req.getAvgOpenPrice();
        if (avgOpenPrice == null || avgOpenPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("avgOpenPrice must be > 0"));
        }

        BigDecimal positionQuantity = req.getPositionQuantity();
        if (positionQuantity != null && positionQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("positionQuantity must be > 0 when provided"));
        }

        LocalDateTime positionOpenTime = req.getPositionOpenTime() != null ? req.getPositionOpenTime() : LocalDateTime.now();

        // 唯一性：同 symbol 最多一个手动 OPEN；避免首页/详情歧义
        int manualOpenCount = realPositionMapper.countOpenManualPositionsBySymbol(symbol);
        if (manualOpenCount > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("请先关闭原手动持仓，再录入新持仓。"));
        }

        // markPrice：优先读取 lastPrice，否则后端兜底为 avgOpenPrice（不允许前端计算）
        BigDecimal markPrice = avgOpenPrice;
        if (marketQuoteClient != null) {
            try {
                Optional<MarketQuoteSnapshot> snap = marketQuoteClient.fetch24hTicker(symbol);
                if (snap.isPresent() && snap.get().getLastPrice() != null) {
                    markPrice = snap.get().getLastPrice();
                }
            } catch (Exception ignored) {
                // keep fallback avgOpenPrice
            }
        }

        // unrealizedPnlPct：由后端计算，保留 6 位小数，HALF_UP
        BigDecimal unrealizedPnlPct = calcUnrealizedPnlPct(positionSide, avgOpenPrice, markPrice);

        LocalDateTime now = LocalDateTime.now();
        String positionId = UUID.randomUUID().toString();

        realPositionMapper.insertOpenPosition(
                positionId,
                symbol,
                MANUAL_SOURCE_TYPE,
                MANUAL_SOURCE_NAME,
                positionSide,
                avgOpenPrice,
                positionOpenTime,
                positionQuantity,
                unrealizedPnlPct,
                markPrice,
                null, // breakEvenPrice (V1 minimal)
                null, // liquidationPrice (V1 minimal)
                now
        );
        try {
            if (positionMonitorService != null) {
                positionMonitorService.evaluateForPosition(positionId, true);
            }
        } catch (Exception e) {
            log.warn("[position-monitor] first evaluate failed after manual create positionId={} symbol={} err={}",
                    positionId, symbol, e.getMessage());
        }

        RealPositionVO out = new RealPositionVO();
        out.setPositionId(positionId);
        out.setSymbol(symbol);
        out.setSourceType(MANUAL_SOURCE_TYPE);
        out.setSourceName(MANUAL_SOURCE_NAME);
        out.setPositionSide(positionSide);
        out.setAvgOpenPrice(avgOpenPrice);
        out.setPositionOpenTime(positionOpenTime);
        out.setPositionQuantity(positionQuantity);
        out.setUnrealizedPnlPct(unrealizedPnlPct);
        out.setPositionStatus("OPEN");
        out.setMarkPrice(markPrice);
        out.setBreakEvenPrice(null);
        out.setLiquidationPrice(null);
        out.setUpdateTime(now);

        return ResponseEntity.ok(ApiResponse.success(out));
    }

    @GetMapping("/open")
    public ApiResponse<List<RealPositionVO>> listOpenManualPositions() {
        List<RealPositionVO> open = realPositionMapper.findOpenPositions();
        open = open == null ? List.of() : open;
        // 只返回手动录入 OPEN
        List<RealPositionVO> manualOpen = open.stream()
                .filter(p -> p != null
                        && MANUAL_SOURCE_TYPE.equals(p.getSourceType())
                        && MANUAL_SOURCE_NAME.equals(p.getSourceName())
                        && "OPEN".equalsIgnoreCase(p.getPositionStatus()))
                .toList();
        return ApiResponse.success(manualOpen);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<CloseManualPositionResult>> closeManualPosition(
            @PathVariable("id") String id,
            @RequestBody(required = false) CloseManualPositionReq req) {
        try {
            ManualPositionCloseService.CloseResult result = manualPositionCloseService.close(id, req);
            CloseManualPositionResult out = new CloseManualPositionResult();
            out.setPositionId(result.getPositionId());
            out.setPositionStatus(result.getPositionStatus());
            out.setTradeResultId(result.getTradeResultId());
            out.setTradeReviewUrl(result.getTradeReviewUrl());
            out.setReviewAnalysisId(result.getReviewAnalysisId());
            out.setAnalysisReviewUrl(result.getAnalysisReviewUrl());
            out.setReviewLevel(result.getReviewLevel());
            out.setReviewMessage(result.getReviewMessage());
            out.setExitPriceFallbackUsed(result.isExitPriceFallbackUsed());
            return ResponseEntity.ok(ApiResponse.success(out));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    public static class CloseManualPositionResult {
        private String positionId;
        private String positionStatus;
        private String tradeResultId;
        private String tradeReviewUrl;
        private String reviewAnalysisId;
        private String analysisReviewUrl;
        private String reviewLevel;
        private String reviewMessage;
        private boolean exitPriceFallbackUsed;

        public String getPositionId() {
            return positionId;
        }

        public void setPositionId(String positionId) {
            this.positionId = positionId;
        }

        public String getPositionStatus() {
            return positionStatus;
        }

        public void setPositionStatus(String positionStatus) {
            this.positionStatus = positionStatus;
        }

        public String getReviewAnalysisId() {
            return reviewAnalysisId;
        }

        public void setReviewAnalysisId(String reviewAnalysisId) {
            this.reviewAnalysisId = reviewAnalysisId;
        }

        public String getTradeResultId() {
            return tradeResultId;
        }

        public void setTradeResultId(String tradeResultId) {
            this.tradeResultId = tradeResultId;
        }

        public String getTradeReviewUrl() {
            return tradeReviewUrl;
        }

        public void setTradeReviewUrl(String tradeReviewUrl) {
            this.tradeReviewUrl = tradeReviewUrl;
        }

        public String getAnalysisReviewUrl() {
            return analysisReviewUrl;
        }

        public void setAnalysisReviewUrl(String analysisReviewUrl) {
            this.analysisReviewUrl = analysisReviewUrl;
        }

        public String getReviewLevel() {
            return reviewLevel;
        }

        public void setReviewLevel(String reviewLevel) {
            this.reviewLevel = reviewLevel;
        }

        public String getReviewMessage() {
            return reviewMessage;
        }

        public void setReviewMessage(String reviewMessage) {
            this.reviewMessage = reviewMessage;
        }

        public boolean isExitPriceFallbackUsed() {
            return exitPriceFallbackUsed;
        }

        public void setExitPriceFallbackUsed(boolean exitPriceFallbackUsed) {
            this.exitPriceFallbackUsed = exitPriceFallbackUsed;
        }
    }

    private static String normalizeSymbol(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toUpperCase();
        return s.isEmpty() ? null : s;
    }

    private static String normalizePositionSide(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toUpperCase();
        if ("LONG".equals(s) || "SHORT".equals(s)) {
            return s;
        }
        return null;
    }

    private static BigDecimal calcUnrealizedPnlPct(String positionSide, BigDecimal avgOpenPrice, BigDecimal markPrice) {
        // avgOpenPrice > 0 已在上游校验
        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal numerator;
        if ("SHORT".equalsIgnoreCase(positionSide)) {
            numerator = avgOpenPrice.subtract(markPrice).multiply(hundred);
        } else {
            numerator = markPrice.subtract(avgOpenPrice).multiply(hundred);
        }
        return numerator.divide(avgOpenPrice, 6, RoundingMode.HALF_UP);
    }

    public static class ManualPositionCreateReq {
        private String symbol;
        private String positionSide;
        private BigDecimal avgOpenPrice;
        private BigDecimal positionQuantity;
        private LocalDateTime positionOpenTime;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getPositionSide() {
            return positionSide;
        }

        public void setPositionSide(String positionSide) {
            this.positionSide = positionSide;
        }

        public BigDecimal getAvgOpenPrice() {
            return avgOpenPrice;
        }

        public void setAvgOpenPrice(BigDecimal avgOpenPrice) {
            this.avgOpenPrice = avgOpenPrice;
        }

        public BigDecimal getPositionQuantity() {
            return positionQuantity;
        }

        public void setPositionQuantity(BigDecimal positionQuantity) {
            this.positionQuantity = positionQuantity;
        }

        public LocalDateTime getPositionOpenTime() {
            return positionOpenTime;
        }

        public void setPositionOpenTime(LocalDateTime positionOpenTime) {
            this.positionOpenTime = positionOpenTime;
        }
    }
}

