package org.example.trademodel.service;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface OpportunityLogService {
    OpportunityLogDTO recordFromAuthoritativeAnalysis(AnalysisRunDO run,
                                                      DecisionResult decision,
                                                      ExecutionPlanDO plan,
                                                      Long accountRiskSnapshotId,
                                                      String traceId);

    OpportunityLogDTO evaluateOpportunity(String opportunityId, LocalDateTime asOf);

    OpportunityLogDTO findById(String opportunityId);

    List<OpportunityLogDTO> query(String analysisId,
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
