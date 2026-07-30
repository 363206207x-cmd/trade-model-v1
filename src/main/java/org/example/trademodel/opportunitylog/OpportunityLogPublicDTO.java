package org.example.trademodel.opportunitylog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OpportunityLogPublicDTO(
        String opportunityId,
        String analysisId,
        String symbol,
        String timeframe,
        String direction,
        String lifecycleStatus,
        String opportunityStatus,
        LocalDateTime anchorTime,
        LocalDateTime resolvedAt,
        BigDecimal entryReference,
        BigDecimal targetPrice,
        BigDecimal invalidationPrice,
        Boolean targetHit,
        Boolean invalidationHit,
        LocalDateTime targetHitAt,
        LocalDateTime invalidationHitAt,
        String hitOrder,
        BigDecimal mfePrice,
        BigDecimal mfeRatio,
        BigDecimal maePrice,
        BigDecimal maeRatio,
        String marketDataSource,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean reviewOnly,
        boolean manualReviewOnly,
        boolean notTradeInstruction,
        boolean notExecutable,
        boolean notAutoTrading,
        boolean notOrderExecution,
        boolean notUserPositionCreation,
        boolean notUserPositionMutation,
        boolean notPushSend,
        boolean notExternalChannel) {
}
