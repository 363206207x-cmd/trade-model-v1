package org.example.trademodel.mapper;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExecutionPlanMapper {

    @Insert("INSERT INTO tm_execution_plan(plan_id, analysis_id, plan_mode, recommended_action, entry_zone, stop_loss, take_profit_rules, leverage_suggestion, position_suggestion, account_risk_json, invalid_condition, create_time) " +
            "VALUES(#{planId}, #{analysisId}, #{planMode}, #{recommendedAction}, #{entryZone}, #{stopLoss}, #{takeProfitRules}, #{leverageSuggestion}, #{positionSuggestion}, #{accountRiskJson}, #{invalidCondition}, #{createTime})")
    int insert(ExecutionPlanDO plan);

    @Select("SELECT * FROM tm_execution_plan WHERE analysis_id = #{analysisId} ORDER BY create_time DESC LIMIT 1")
    ExecutionPlanDO selectLatestByAnalysisId(String analysisId);

    /**
     * Same “latest plan” semantics as {@code ROW_NUMBER() ... ORDER BY create_time DESC, plan_id DESC}:
     * when {@code create_time} ties, prefer lexicographically greater {@code plan_id}.
     */
    @Select("SELECT * FROM tm_execution_plan WHERE analysis_id = #{analysisId} ORDER BY create_time DESC, plan_id DESC LIMIT 1")
    ExecutionPlanDO selectLatestByAnalysisIdTieBreak(@Param("analysisId") String analysisId);

    @Select("SELECT * FROM tm_execution_plan WHERE plan_id = #{planId} LIMIT 1")
    ExecutionPlanDO selectByPlanId(@Param("planId") String planId);
}