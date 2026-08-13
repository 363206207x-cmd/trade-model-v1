package org.example.trademodel.service;

import org.example.trademodel.vo.DecisionChainAuditVO;

import java.util.Optional;

/** Safe read projection; it does not own or mutate decision-chain business data. */
public interface DecisionChainAuditQueryService {
    Optional<DecisionChainAuditVO> queryForUser(
            Long userId, String analysisId, String traceId, String candidateId);
}
