package org.example.trademodel.mapper;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExecutionPlanMapper {

    @Insert("INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, execution_plan_status, source_gate_status, source_gate_complete, source_missing_reasons, source_blocker_reasons, source_completeness_summary, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, account_risk_json, invalid_condition, manual_review_required, not_trade_instruction, not_executable, not_auto_trading, not_order_execution, not_user_position_creation, create_time) " +
            "VALUES(#{planId}, #{analysisId}, #{planMode}, #{executionPlanStatus}, #{sourceGateStatus}, #{sourceGateComplete}, #{sourceMissingReasons}, #{sourceBlockerReasons}, #{sourceCompletenessSummary}, #{recommendedAction}, #{entryZone}, #{stopLoss}, #{takeProfitRules}, #{leverageSuggestion}, #{positionSuggestion}, #{accountRiskJson}, #{invalidCondition}, #{manualReviewRequired}, #{notTradeInstruction}, #{notExecutable}, #{notAutoTrading}, #{notOrderExecution}, #{notUserPositionCreation}, #{createTime})")
    int insert(ExecutionPlanDO plan);

    @Select("SELECT * FROM tm_execution_plan WHERE analysis_id = #{analysisId} ORDER BY create_time DESC LIMIT 1")
    ExecutionPlanDO selectLatestByAnalysisId(String analysisId);
}
