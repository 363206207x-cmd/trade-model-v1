package org.example.trademodel.service;

import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;

public interface AiConflictResolverService {
    AiConflictResult resolve(DecisionContext context);

    ConflictResolverResultDO resolveDecisionChain(ExecutionPlanCandidateDO candidate,
                                                   String geminiReviewJson,
                                                   String grokChallengeJson,
                                                   Integer dataQualityScore,
                                                   Integer confusedScore,
                                                   String accountRiskState);
}
