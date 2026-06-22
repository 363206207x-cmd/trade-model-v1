package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.PositionMonitorLogDO;

import java.util.List;

@Mapper
public interface PositionMonitorLogMapper {

    String BASE_SELECT = "SELECT log_id AS logId, position_id AS positionId, analysis_id AS analysisId, "
            + "execution_plan_id AS executionPlanId, current_price AS currentPrice, logic_status AS logicStatus, "
            + "risk_level AS riskLevel, suggested_action AS suggestedAction, reason, "
            + "evidence_snapshot AS evidenceSnapshot, score_snapshot AS scoreSnapshot, "
            + "decision_snapshot AS decisionSnapshot, risk_snapshot AS riskSnapshot, trace_id AS traceId, "
            + "created_at AS createdAt FROM tm_position_monitor_log ";

    @Insert("INSERT INTO tm_position_monitor_log("
            + "position_id, analysis_id, execution_plan_id, current_price, logic_status, risk_level, suggested_action, "
            + "reason, evidence_snapshot, score_snapshot, decision_snapshot, risk_snapshot, trace_id, created_at"
            + ") VALUES ("
            + "#{positionId}, #{analysisId}, #{executionPlanId}, #{currentPrice}, #{logicStatus}, #{riskLevel}, #{suggestedAction}, "
            + "#{reason}, #{evidenceSnapshot}, #{scoreSnapshot}, #{decisionSnapshot}, #{riskSnapshot}, #{traceId}, #{createdAt}"
            + ")")
    @Options(useGeneratedKeys = true, keyProperty = "logId", keyColumn = "log_id")
    int insert(PositionMonitorLogDO row);

    @Select(BASE_SELECT + "WHERE log_id = #{logId}")
    PositionMonitorLogDO selectById(@Param("logId") Long logId);

    @Select(BASE_SELECT + "WHERE position_id = #{positionId} ORDER BY created_at DESC, log_id DESC LIMIT #{limit}")
    List<PositionMonitorLogDO> listByPositionId(@Param("positionId") Long positionId, @Param("limit") int limit);

    @Select(BASE_SELECT + "WHERE position_id = #{positionId} ORDER BY created_at ASC, log_id ASC")
    List<PositionMonitorLogDO> listAllByPositionIdForReview(@Param("positionId") Long positionId);

    @Select(BASE_SELECT + "WHERE analysis_id = #{analysisId} ORDER BY created_at DESC, log_id DESC LIMIT #{limit}")
    List<PositionMonitorLogDO> listByAnalysisId(@Param("analysisId") String analysisId, @Param("limit") int limit);
}
