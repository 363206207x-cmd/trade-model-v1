package org.example.trademodel.service;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface OpportunityLogService {
    OpportunityLogDTO recordFromAuthoritativeAnalysis(AnalysisRunDO run,
                                                      DecisionResult decision,
                                                      ExecutionPlanDO plan,
                                                      Long accountRiskSnapshotId,
                                                      String traceId);

    OpportunityLogDTO evaluateOpportunityForUser(String opportunityId, Long userId, LocalDateTime asOf);

    OpportunityLogDTO evaluateOpportunityForSystem(String opportunityId, LocalDateTime asOf);

    OpportunityLogPublicDTO evaluatePublicOpportunityForUser(
            String opportunityId, Long userId, LocalDateTime asOf);

    OpportunityLogPublicDTO findPublicById(String opportunityId);

    List<OpportunityLogPublicDTO> queryPublic(String analysisId,
                                               String decisionId,
                                               String executionPlanId,
                                               String symbol,
                                               String opportunityStatus,
                                               String lifecycleStatus,
                                               LocalDateTime from,
                                               LocalDateTime to,
                                               int limit);

    OpportunityLogDTO findByIdForUser(String opportunityId, Long userId);

    OpportunityLogDTO findByIdForSystem(String opportunityId);

    List<OpportunityLogDTO> queryForUser(Long userId,
                                         String analysisId,
                                         String decisionId,
                                         String executionPlanId,
                                         String symbol,
                                         String opportunityStatus,
                                         String lifecycleStatus,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         int limit);

    List<OpportunityLogDTO> queryForSystem(String analysisId,
                                           String decisionId,
                                           String executionPlanId,
                                           String symbol,
                                           String opportunityStatus,
                                           String lifecycleStatus,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           int limit);

    OpportunityLogStatsDTO getStats(String symbol, LocalDateTime from, LocalDateTime to);
}
