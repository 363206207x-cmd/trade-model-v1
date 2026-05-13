package org.example.trademodel.service.planboundary;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextStatusEnum;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;

import java.math.BigDecimal;
import java.util.List;

public class BoundaryCandidateServiceImpl implements BoundaryCandidateService {
    private static final BigDecimal MIN_DATA_QUALITY_SCORE = new BigDecimal("70");

    @Override
    public BoundaryCandidateDTO evaluateBoundaryCandidate(String symbol,
                                                          String timeframe,
                                                          RuntimeKlineContextDTO runtimeKlineContext,
                                                          BigDecimal latestPrice,
                                                          BigDecimal dataQualityScore) {
        if (runtimeKlineContext == null) {
            return BoundaryCandidateDTO.incomplete(symbol, timeframe, "MISSING_RUNTIME_KLINE_CONTEXT");
        }
        if (runtimeKlineContext.getStaleStatus() == null) {
            return BoundaryCandidateDTO.incomplete(symbol, timeframe, "MISSING_KLINE_STATUS");
        }
        if (RuntimeKlineContextStatusEnum.UNKNOWN.equals(runtimeKlineContext.getStaleStatus())) {
            return BoundaryCandidateDTO.incomplete(symbol, timeframe, "UNKNOWN_RUNTIME_KLINE_CONTEXT");
        }
        if (RuntimeKlineContextStatusEnum.STALE.equals(runtimeKlineContext.getStaleStatus())) {
            return BoundaryCandidateDTO.incomplete(symbol, timeframe, "STALE_RUNTIME_KLINE_CONTEXT");
        }
        if (latestPrice == null || latestPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BoundaryCandidateDTO.incomplete(symbol, timeframe, "MISSING_OR_INVALID_LATEST_PRICE");
        }
        if (dataQualityScore == null || dataQualityScore.compareTo(MIN_DATA_QUALITY_SCORE) < 0) {
            return BoundaryCandidateDTO.incomplete(symbol, timeframe, "DATA_QUALITY_LOW");
        }
        if (!hasCompleteOhlcv(runtimeKlineContext)) {
            return BoundaryCandidateDTO.incomplete(symbol, timeframe, "MISSING_OHLCV_OR_KLINE_ITEMS");
        }
        return BoundaryCandidateDTO.watchOnly(symbol, timeframe, "VALID_CANDIDATE_FACTORY_NOT_AVAILABLE");
    }

    private boolean hasCompleteOhlcv(RuntimeKlineContextDTO runtimeKlineContext) {
        if (runtimeKlineContext.getLatestOpen() == null
                || runtimeKlineContext.getLatestHigh() == null
                || runtimeKlineContext.getLatestLow() == null
                || runtimeKlineContext.getLatestClose() == null
                || runtimeKlineContext.getLatestVolume() == null) {
            return false;
        }
        List<RuntimeKlineItemDTO> klineItems = runtimeKlineContext.getKlineItems();
        if (klineItems == null || klineItems.isEmpty()) {
            return false;
        }
        return klineItems.stream().allMatch(this::hasCompleteOhlcv);
    }

    private boolean hasCompleteOhlcv(RuntimeKlineItemDTO klineItem) {
        return klineItem != null
                && klineItem.getOpen() != null
                && klineItem.getHigh() != null
                && klineItem.getLow() != null
                && klineItem.getClose() != null
                && klineItem.getVolume() != null;
    }
}
