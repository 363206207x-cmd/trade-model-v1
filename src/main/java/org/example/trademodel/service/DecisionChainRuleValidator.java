package org.example.trademodel.service;

import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;

public interface DecisionChainRuleValidator {
    RuleValidationResult validate(DecisionChainBuildInput input,
                                  OpportunityTransitionResult opportunity,
                                  ExecutionPlanCandidateDO candidate,
                                  ConflictResolverResultDO conflict);
}
