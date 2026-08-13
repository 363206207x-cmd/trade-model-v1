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
            + "execution_plan_id AS executionPlanId, current_price AS currentPrice, "
            + "mark_price_source AS markPriceSource, logic_status AS logicStatus, "
            + "entry_logic_status AS entryLogicStatus, monitor_conclusion AS monitorConclusion, "
            + "reversal_status AS reversalStatus, risk_change_reason AS riskChangeReason, "
            + "risk_level AS riskLevel, risk_trend AS riskTrend, suggested_action AS suggestedAction, "
            + "source_status AS monitorSourceStatus, observed_at AS observedAt, fresh_until AS freshUntil, reason, "
            + "evidence_snapshot AS evidenceSnapshot, score_snapshot AS scoreSnapshot, "
            + "decision_snapshot AS decisionSnapshot, risk_snapshot AS riskSnapshot, trace_id AS traceId, "
            + "created_at AS createdAt FROM tm_position_monitor_log ";

    String OWNER_SCOPED_SELECT = "SELECT l.log_id AS logId, l.position_id AS positionId, "
            + "l.analysis_id AS analysisId, l.execution_plan_id AS executionPlanId, "
            + "l.current_price AS currentPrice, l.mark_price_source AS markPriceSource, "
            + "l.logic_status AS logicStatus, l.entry_logic_status AS entryLogicStatus, "
            + "l.monitor_conclusion AS monitorConclusion, l.reversal_status AS reversalStatus, "
            + "l.risk_change_reason AS riskChangeReason, l.risk_level AS riskLevel, l.risk_trend AS riskTrend, "
            + "l.suggested_action AS suggestedAction, l.source_status AS monitorSourceStatus, "
            + "l.observed_at AS observedAt, l.fresh_until AS freshUntil, "
            + "l.reason, l.evidence_snapshot AS evidenceSnapshot, "
            + "l.score_snapshot AS scoreSnapshot, l.decision_snapshot AS decisionSnapshot, "
            + "l.risk_snapshot AS riskSnapshot, l.trace_id AS traceId, l.created_at AS createdAt "
            + "FROM tm_position_monitor_log l "
            + "INNER JOIN tm_user_position p ON p.id = l.position_id AND p.user_id = #{userId} ";

    @Insert("INSERT INTO tm_position_monitor_log("
            + "position_id, analysis_id, execution_plan_id, current_price, mark_price_source, "
            + "entry_logic_status, monitor_conclusion, reversal_status, risk_change_reason, risk_level, risk_trend, "
            + "suggested_action, source_status, observed_at, fresh_until, reason, "
            + "evidence_snapshot, score_snapshot, decision_snapshot, risk_snapshot, trace_id, created_at"
            + ") VALUES ("
            + "#{positionId}, #{analysisId}, #{executionPlanId}, #{currentPrice}, #{markPriceSource}, "
            + "#{entryLogicStatus}, #{monitorConclusion}, #{reversalStatus}, #{riskChangeReason}, #{riskLevel}, #{riskTrend}, "
            + "#{suggestedAction}, #{monitorSourceStatus}, #{observedAt}, #{freshUntil}, #{reason}, "
            + "#{evidenceSnapshot}, #{scoreSnapshot}, #{decisionSnapshot}, #{riskSnapshot}, #{traceId}, #{createdAt}"
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

    @Select(OWNER_SCOPED_SELECT
            + "WHERE l.analysis_id = #{analysisId} ORDER BY l.created_at, l.log_id")
    List<PositionMonitorLogDO> listByAnalysisIdAndUserId(@Param("analysisId") String analysisId,
                                                         @Param("userId") Long userId);

    @Select(OWNER_SCOPED_SELECT
            + "WHERE l.log_id = #{logId}")
    PositionMonitorLogDO selectRiskByIdAndUserId(@Param("logId") Long logId,
                                                 @Param("userId") Long userId);

    @Select(OWNER_SCOPED_SELECT
            + "WHERE l.monitor_conclusion IN ('LOGIC_WEAKENED', 'PLAN_INVALIDATED', 'NEAR_STOP_LOSS', "
            + "'HIGH_RISK_OBSERVATION', 'WAIT_USER_CONFIRM_CLOSE') "
            + "ORDER BY l.created_at DESC, l.log_id DESC LIMIT #{limit}")
    List<PositionMonitorLogDO> listRiskByUserId(@Param("userId") Long userId,
                                                @Param("limit") int limit);

    @Select(OWNER_SCOPED_SELECT
            + "WHERE l.position_id = #{positionId} "
            + "ORDER BY l.created_at DESC, l.log_id DESC LIMIT 1")
    PositionMonitorLogDO selectLatestByPositionIdAndUserId(@Param("positionId") Long positionId,
                                                           @Param("userId") Long userId);
}
