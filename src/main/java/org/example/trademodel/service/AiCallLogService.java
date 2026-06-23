package org.example.trademodel.service;

import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.entity.AiCallLogDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AiCallLogService {
    AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd);

    void completeCall(AiCallLogDO log, AiProviderReviewResult result);

    AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client,
                              AiProviderReviewResult result, BigDecimal reservedCostUsd);

    List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus,
                            LocalDateTime from, LocalDateTime to, int limit);

    int countProviderAttemptsSince(String providerName, LocalDateTime since);

    BigDecimal sumChargeableCostSince(LocalDateTime since);

    default BigDecimal sumChargeableCostByAnalysisId(String analysisId) {
        return BigDecimal.ZERO;
    }
}
