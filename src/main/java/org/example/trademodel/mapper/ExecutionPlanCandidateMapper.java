package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;

@Mapper
public interface ExecutionPlanCandidateMapper {
    @Insert("INSERT INTO tm_execution_plan_candidate(candidate_id, opportunity_id, analysis_id, trace_id, "
            + "rule_direction, rule_confidence, rule_risk, rule_plan_mode, rule_can_execute, candidate_direction, "
            + "bias_adjustment_reason, plan_mode, confidence_level, risk_level, worth_opening, recommended_action, "
            + "asset_id, rule_version, opportunity_type, entry_logic, entry_zone, entry_source, entry_reason, trigger_condition, "
            + "stop_logic, stop_loss, stop_source, stop_reason, target_logic, take_profit_rules, target_source, target_reason, "
            + "add_position_condition, reduce_position_condition, abandon_condition, leverage_suggestion, "
            + "position_suggestion, risk_explanation, invalid_condition, invalidation_source, invalidation_reason, expected_risk_reward, "
            + "expected_risk_reward_source, expected_risk_reward_reason, validity, analysis_timeframes_json, trigger_timeframe, "
            + "valid_from, valid_until, holding_horizon, revalidation_rule, source_refs_json, evidence_refs_json, "
            + "score_refs_json, data_quality, confused_score, account_risk_snapshot_id, version, "
            + "summary, candidate_source, candidate_status, "
            + "fallback_reason, payload_json, not_final_plan, not_state_machine_mutation, "
            + "not_user_position_creation, created_at) VALUES(#{candidateId}, #{opportunityId}, #{analysisId}, "
            + "#{traceId}, #{ruleDirection}, #{ruleConfidence}, #{ruleRisk}, #{rulePlanMode}, #{ruleCanExecute}, "
            + "#{candidateDirection}, #{biasAdjustmentReason}, #{planMode}, #{confidenceLevel}, #{riskLevel}, "
            + "#{worthOpening}, #{recommendedAction}, #{assetId}, #{ruleVersion}, #{opportunityType}, #{entryLogic}, "
            + "#{entryZone}, #{entrySource}, #{entryReason}, #{triggerCondition}, #{stopLogic}, #{stopLoss}, "
            + "#{stopSource}, #{stopReason}, #{targetLogic}, #{takeProfitRules}, #{targetSource}, #{targetReason}, "
            + "#{addPositionCondition}, #{reducePositionCondition}, #{abandonCondition}, #{leverageSuggestion}, "
            + "#{positionSuggestion}, #{riskExplanation}, #{invalidCondition}, #{invalidationSource}, #{invalidationReason}, #{expectedRiskReward}, "
            + "#{expectedRiskRewardSource}, #{expectedRiskRewardReason}, #{validity}, #{analysisTimeframesJson}, "
            + "#{triggerTimeframe}, #{validFrom}, #{validUntil}, #{holdingHorizon}, #{revalidationRule}, #{sourceRefsJson}, "
            + "#{evidenceRefsJson}, #{scoreRefsJson}, #{dataQuality}, #{confusedScore}, #{accountRiskSnapshotId}, #{version}, #{summary}, "
            + "#{candidateSource}, #{candidateStatus}, #{fallbackReason}, #{payloadJson}, #{notFinalPlan}, "
            + "#{notStateMachineMutation}, #{notUserPositionCreation}, #{createdAt})")
    int insert(ExecutionPlanCandidateDO row);

    @Select("SELECT * FROM tm_execution_plan_candidate WHERE candidate_id = #{candidateId}")
    ExecutionPlanCandidateDO selectById(@Param("candidateId") String candidateId);

    @Select("SELECT * FROM tm_execution_plan_candidate WHERE analysis_id = #{analysisId} LIMIT 1")
    ExecutionPlanCandidateDO selectByAnalysisId(@Param("analysisId") String analysisId);
}
