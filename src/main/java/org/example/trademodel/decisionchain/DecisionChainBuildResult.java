package org.example.trademodel.decisionchain;

import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.vo.ExecutionPlanVO;

public record DecisionChainBuildResult(
        OpportunityTransitionResult opportunity,
        ExecutionPlanCandidateDO candidate,
        ConflictResolverResultDO conflict,
        RuleValidationResult validation,
        ExecutionPlanVO finalPlan) {
}
