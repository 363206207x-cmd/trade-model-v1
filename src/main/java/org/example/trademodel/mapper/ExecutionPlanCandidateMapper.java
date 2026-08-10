package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;

@Mapper
public interface ExecutionPlanCandidateMapper {
    @Insert("INSERT INTO tm_execution_plan_candidate(candidate_id, opportunity_id, analysis_id, trace_id, "
            + "rule_direction, rule_confidence, rule_risk, candidate_direction, plan_mode, confidence_level, risk_level, worth_opening, "
            + "recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, "
            + "position_suggestion, invalid_condition, validity, summary, candidate_source, candidate_status, "
            + "fallback_reason, payload_json, not_final_plan, not_state_machine_mutation, "
            + "not_user_position_creation, created_at) VALUES(#{candidateId}, #{opportunityId}, #{analysisId}, "
            + "#{traceId}, #{ruleDirection}, #{ruleConfidence}, #{ruleRisk}, #{candidateDirection}, #{planMode}, #{confidenceLevel}, #{riskLevel}, "
            + "#{worthOpening}, #{recommendedAction}, #{entryZone}, #{stopLoss}, #{takeProfitRules}, "
            + "#{leverageSuggestion}, #{positionSuggestion}, #{invalidCondition}, #{validity}, #{summary}, "
            + "#{candidateSource}, #{candidateStatus}, #{fallbackReason}, #{payloadJson}, #{notFinalPlan}, "
            + "#{notStateMachineMutation}, #{notUserPositionCreation}, #{createdAt})")
    int insert(ExecutionPlanCandidateDO row);

    @Select("SELECT * FROM tm_execution_plan_candidate WHERE candidate_id = #{candidateId}")
    ExecutionPlanCandidateDO selectById(@Param("candidateId") String candidateId);

    @Select("SELECT * FROM tm_execution_plan_candidate WHERE analysis_id = #{analysisId} LIMIT 1")
    ExecutionPlanCandidateDO selectByAnalysisId(@Param("analysisId") String analysisId);
}
