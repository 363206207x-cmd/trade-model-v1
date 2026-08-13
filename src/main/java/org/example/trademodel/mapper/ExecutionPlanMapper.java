package org.example.trademodel.mapper;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExecutionPlanMapper {

    @Insert("INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, execution_plan_status, source_gate_status, source_gate_complete, source_missing_reasons, source_blocker_reasons, source_completeness_summary, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, account_risk_json, execution_feasibility_status, slippage_status, depth_status, entry_drift_status, trigger_status, execution_feasibility_reason, execution_feasibility_observed_at, execution_feasibility_fresh_until, execution_feasibility_source_refs_json, invalid_condition, invalidation_source, invalidation_reason, manual_review_required, not_trade_instruction, not_executable, not_auto_trading, not_order_execution, not_user_position_creation, candidate_id, opportunity_id, resolver_result_id, trace_id, chain_status, rule_validation_status, rule_veto_reason, finalized_at, final_plan, "
            + "asset_id, rule_version, rule_market_bias, final_market_bias, candidate_plan_mode, final_plan_mode, bias_adjustment_reason, plan_mode_adjustment_reason, adjustment_reason, downgrade_reason, opportunity_type, entry_logic, entry_source, entry_reason, trigger_condition, stop_logic, stop_source, stop_reason, target_logic, target_source, target_reason, add_position_condition, reduce_position_condition, abandon_condition, risk_explanation, leverage_limit, position_limit, risk_limit, expected_risk_reward, expected_risk_reward_source, expected_risk_reward_reason, account_risk_snapshot_id, analysis_timeframes_json, trigger_timeframe, valid_from, valid_until, holding_horizon, revalidation_rule, data_quality, source_refs_json, evidence_refs_json, score_refs_json, validation_result_id, validation_reasons, source_status, "
            + "needs_revalidation, revalidation_reason, create_time) " +
            "VALUES(#{planId}, #{analysisId}, #{planMode}, #{executionPlanStatus}, #{sourceGateStatus}, #{sourceGateComplete}, #{sourceMissingReasons}, #{sourceBlockerReasons}, #{sourceCompletenessSummary}, #{recommendedAction}, #{entryZone}, #{stopLoss}, #{takeProfitRules}, #{leverageSuggestion}, #{positionSuggestion}, #{accountRiskJson}, COALESCE(#{executionFeasibilityStatus}, 'UNAVAILABLE'), COALESCE(#{slippageStatus}, 'UNAVAILABLE'), COALESCE(#{depthStatus}, 'UNAVAILABLE'), COALESCE(#{entryDriftStatus}, 'UNAVAILABLE'), COALESCE(#{triggerStatus}, 'UNAVAILABLE'), COALESCE(#{executionFeasibilityReason}, 'EXECUTION_FEASIBILITY_STATUS_MISSING'), #{executionFeasibilityObservedAt}, #{executionFeasibilityFreshUntil}, #{executionFeasibilitySourceRefsJson}, #{invalidCondition}, #{invalidationSource}, #{invalidationReason}, #{manualReviewRequired}, #{notTradeInstruction}, #{notExecutable}, #{notAutoTrading}, #{notOrderExecution}, #{notUserPositionCreation}, #{candidateId}, #{opportunityId}, #{resolverResultId}, #{traceId}, #{chainStatus}, #{ruleValidationStatus}, #{ruleVetoReason}, #{finalizedAt}, #{finalPlan}, "
            + "#{assetId}, #{ruleVersion}, #{ruleMarketBias}, #{finalMarketBias}, #{candidatePlanMode}, #{finalPlanMode}, #{biasAdjustmentReason}, #{planModeAdjustmentReason}, #{adjustmentReason}, #{downgradeReason}, #{opportunityType}, #{entryLogic}, #{entrySource}, #{entryReason}, #{triggerCondition}, #{stopLogic}, #{stopSource}, #{stopReason}, #{targetLogic}, #{targetSource}, #{targetReason}, #{addPositionCondition}, #{reducePositionCondition}, #{abandonCondition}, #{riskExplanation}, #{leverageLimit}, #{positionLimit}, #{riskLimit}, #{expectedRiskReward}, #{expectedRiskRewardSource}, #{expectedRiskRewardReason}, #{accountRiskSnapshotId}, #{analysisTimeframesJson}, #{triggerTimeframe}, #{validFrom}, #{validUntil}, #{holdingHorizon}, #{revalidationRule}, #{dataQuality}, #{sourceRefsJson}, #{evidenceRefsJson}, #{scoreRefsJson}, #{validationResultId}, #{validationReasons}, #{sourceStatus}, "
            + "#{needsRevalidation}, #{revalidationReason}, #{createTime})")
    int insert(ExecutionPlanDO plan);

    @Select("SELECT * FROM tm_execution_plan WHERE analysis_id = #{analysisId} ORDER BY create_time DESC LIMIT 1")
    ExecutionPlanDO selectLatestByAnalysisId(@Param("analysisId") String analysisId);

    @Select("""
            SELECT ep.*
            FROM tm_execution_plan ep
            WHERE ep.analysis_id = #{analysisId}
              AND (SELECT COUNT(*) FROM tm_execution_plan WHERE analysis_id = #{analysisId}) = 1
            LIMIT 1
            """)
    ExecutionPlanDO selectOnlyByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT * FROM tm_execution_plan WHERE plan_id = #{planId} ORDER BY create_time DESC LIMIT 1")
    ExecutionPlanDO selectByPlanId(@Param("planId") String planId);

    @Select("SELECT * FROM tm_execution_plan WHERE candidate_id = #{candidateId} "
            + "ORDER BY create_time DESC, plan_id DESC LIMIT 1")
    ExecutionPlanDO selectLatestByCandidateId(@Param("candidateId") String candidateId);

    @Select("SELECT ep.* FROM tm_execution_plan ep "
            + "INNER JOIN tm_decision_result d ON d.analysis_id = ep.analysis_id "
            + "WHERE ep.plan_id = #{planId} AND ep.final_plan = TRUE "
            + "AND ep.rule_validation_status = 'PASS' AND ep.candidate_id IS NOT NULL "
            + "AND UPPER(TRIM(d.symbol)) = #{normalizedSymbol} "
            + "ORDER BY ep.create_time DESC LIMIT 1")
    ExecutionPlanDO selectValidatedFinalByPlanIdAndSymbol(@Param("planId") String planId,
                                                          @Param("normalizedSymbol") String normalizedSymbol);

    @Update("UPDATE tm_execution_plan SET needs_revalidation = TRUE, revalidation_reason = #{reason}, "
            + "hot_reset_event_id = #{eventId}, revalidation_required_at = #{requiredAt} "
            + "WHERE analysis_id = #{analysisId} OR analysis_id IN ("
            + "SELECT analysis_id FROM tm_decision_result WHERE UPPER(TRIM(symbol)) = #{normalizedSymbol})")
    int markNeedsRevalidationForHotReset(
            @Param("analysisId") String analysisId,
            @Param("normalizedSymbol") String normalizedSymbol,
            @Param("eventId") String eventId,
            @Param("reason") String reason,
            @Param("requiredAt") java.time.LocalDateTime requiredAt);
}
