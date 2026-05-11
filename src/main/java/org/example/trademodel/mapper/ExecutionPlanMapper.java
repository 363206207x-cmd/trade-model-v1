package org.example.trademodel.mapper;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    /**
     * One latest plan per analysis_id; same ordering as {@link #selectLatestByAnalysisIdTieBreak}:
     * {@code ORDER BY create_time DESC, plan_id DESC LIMIT 1} per id. Window applies only to rows with
     * {@code analysis_id IN (...)}.
     */
    @Select("<script>" +
            "SELECT t.plan_id, t.analysis_id, t.plan_mode, t.recommended_action, t.entry_zone, t.stop_loss, " +
            "t.take_profit_rules, t.leverage_suggestion, t.position_suggestion, t.account_risk_json, t.invalid_condition, t.create_time " +
            "FROM (" +
            "  SELECT ep.plan_id, ep.analysis_id, ep.plan_mode, ep.recommended_action, ep.entry_zone, ep.stop_loss, " +
            "  ep.take_profit_rules, ep.leverage_suggestion, ep.position_suggestion, ep.account_risk_json, ep.invalid_condition, ep.create_time, " +
            "  ROW_NUMBER() OVER (PARTITION BY ep.analysis_id ORDER BY ep.create_time DESC, ep.plan_id DESC) AS rn " +
            "  FROM tm_execution_plan ep " +
            "  WHERE ep.analysis_id IN " +
            "  <foreach collection='analysisIds' item='id' open='(' separator=',' close=')'>" +
            "  #{id}" +
            "  </foreach>" +
            ") t WHERE t.rn = 1" +
            "</script>")
    List<ExecutionPlanDO> selectLatestByAnalysisIdsTieBreak(@Param("analysisIds") List<String> analysisIds);

    @Select("SELECT * FROM tm_execution_plan WHERE plan_id = #{planId} LIMIT 1")
    ExecutionPlanDO selectByPlanId(@Param("planId") String planId);
}