package org.example.trademodel.service;

import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.entity.AiCallLogDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AiCallLogService {
    AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd);

    void completeCall(AiCallLogDO log, AiProviderReviewResult result);

    default AiCallLogDO startDecisionChainCall(AiDecisionChainRequest request, AiProviderClient client,
                                               BigDecimal reservedCostUsd) {
        throw new UnsupportedOperationException("Decision-chain AI logging is not implemented");
    }

    default void completeDecisionChainCall(AiCallLogDO log, AiDecisionChainResult result) {
        throw new UnsupportedOperationException("Decision-chain AI logging is not implemented");
    }

    default AiCallLogDO recordDecisionChainResult(AiDecisionChainRequest request,
                                                  AiProviderName provider,
                                                  String modelName,
                                                  AiDecisionChainResult result,
                                                  BigDecimal reservedCostUsd) {
        throw new UnsupportedOperationException("Decision-chain terminal AI logging is not implemented");
    }

    AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client,
                              AiProviderReviewResult result, BigDecimal reservedCostUsd);

    List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus,
                            LocalDateTime from, LocalDateTime to, int limit);

    default List<AiCallLogDO> queryOwned(Long userId, String analysisId, String traceId,
                                         String candidateId, String role, String providerName,
                                         String callStatus, LocalDateTime from, LocalDateTime to,
                                         int limit) {
        throw new UnsupportedOperationException("Owned AI trace query is not implemented");
    }

    int countProviderAttemptsSince(String providerName, LocalDateTime since);

    default int countDecisionChainAttemptsSince(LocalDateTime since) {
        return 0;
    }

    default int countDecisionChainRoleAttempts(String analysisId, String role) {
        return 0;
    }

    default long sumDecisionChainTokensSince(LocalDateTime since) {
        return 0L;
    }

    default long sumDecisionChainTokensByAnalysisId(String analysisId) {
        return 0L;
    }

    BigDecimal sumChargeableCostSince(LocalDateTime since);

    default BigDecimal sumChargeableCostByAnalysisId(String analysisId) {
        return BigDecimal.ZERO;
    }
}
