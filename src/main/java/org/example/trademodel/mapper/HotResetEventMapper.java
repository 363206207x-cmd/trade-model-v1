package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.vo.KeyCountVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HotResetEventMapper {

    @Insert("INSERT INTO tm_hot_reset_event(event_id, event_key, analysis_id, rebuild_analysis_id, trace_id, "
            + "owner_type, owner_id, asset_id, rule_version, "
            + "symbol, timeframe, trigger_type, trigger_value, source_type, source_reference, severity_score, "
            + "decision_id, decision_state, decision_invalidated_count, plan_revalidation_count, push_invalidated_count, "
            + "confused_score_snapshot, confused_score_before, confused_score_after, multi_timeframe_aligned_snapshot, "
            + "account_risk_status, account_risk_level, account_risk_blocked, account_risk_snapshot, rebuild_triggered, "
            + "execution_status, execution_error_code, execution_error_message, trigger_reason_code, trigger_reason_text, "
            + "event_version, event_time, pre_state, post_state, completed_at, create_time) "
            + "VALUES(#{eventId}, #{eventKey}, #{analysisId}, #{rebuildAnalysisId}, #{traceId}, "
            + "COALESCE(#{ownerType}, 'SYSTEM'), COALESCE(#{ownerId}, 0), #{assetId}, "
            + "COALESCE(#{ruleVersion}, 'LEGACY_UNAVAILABLE'), "
            + "#{symbol}, #{timeframe}, #{triggerType}, #{triggerValue}, #{sourceType}, #{sourceReference}, #{severityScore}, "
            + "#{decisionId}, #{decisionState}, #{decisionInvalidatedCount}, #{planRevalidationCount}, #{pushInvalidatedCount}, "
            + "#{confusedScoreSnapshot}, #{confusedScoreBefore}, #{confusedScoreAfter}, #{multiTimeframeAlignedSnapshot}, "
            + "#{accountRiskStatus}, #{accountRiskLevel}, #{accountRiskBlocked}, #{accountRiskSnapshot}, #{rebuildTriggered}, "
            + "#{executionStatus}, #{executionErrorCode}, #{executionErrorMessage}, #{triggerReasonCode}, #{triggerReasonText}, "
            + "#{eventVersion}, #{eventTime}, #{preState}, #{postState}, #{completedAt}, #{createTime})")
    int insert(HotResetEventDO row);

    @Select("SELECT * FROM tm_hot_reset_event WHERE event_key = #{eventKey} LIMIT 1")
    HotResetEventDO selectByEventKey(@Param("eventKey") String eventKey);

    @Update("UPDATE tm_hot_reset_event SET rebuild_analysis_id = #{rebuildAnalysisId}, "
            + "rebuild_triggered = #{rebuildTriggered}, execution_status = #{executionStatus}, "
            + "execution_error_code = #{executionErrorCode}, execution_error_message = #{executionErrorMessage}, "
            + "completed_at = #{completedAt} WHERE event_id = #{eventId}")
    int updateRebuildOutcome(HotResetEventDO row);

    @Select("SELECT * FROM tm_hot_reset_event WHERE analysis_id = #{analysisId} ORDER BY event_time DESC LIMIT 1")
    HotResetEventDO selectLatestByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT COUNT(*) FROM tm_hot_reset_event WHERE analysis_id = #{analysisId}")
    Integer countByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT COUNT(*) FROM tm_hot_reset_event "
            + "WHERE event_time >= #{windowStartInclusive} "
            + "AND event_time <= #{asOfInclusive}")
    Integer countInWindow(@Param("windowStartInclusive") LocalDateTime windowStartInclusive,
                          @Param("asOfInclusive") LocalDateTime asOfInclusive);

    @Results({
            @Result(column = "trigger_key", property = "key"),
            @Result(column = "trigger_count", property = "count")
    })
    @Select("SELECT trigger_type AS trigger_key, COUNT(*) AS trigger_count "
            + "FROM tm_hot_reset_event "
            + "WHERE event_time >= #{windowStartInclusive} "
            + "AND event_time <= #{asOfInclusive} "
            + "GROUP BY trigger_type ORDER BY COUNT(*) DESC, trigger_type ASC")
    List<KeyCountVO> selectTriggerTypeCountsInWindow(
            @Param("windowStartInclusive") LocalDateTime windowStartInclusive,
            @Param("asOfInclusive") LocalDateTime asOfInclusive);
}
