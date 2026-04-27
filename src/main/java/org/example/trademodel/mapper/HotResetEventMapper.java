package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.vo.KeyCountVO;

import java.util.List;

@Mapper
public interface HotResetEventMapper {

    @Insert("INSERT INTO tm_hot_reset_event(event_id, analysis_id, trace_id, symbol, trigger_type, trigger_value, "
            + "decision_id, decision_state, confused_score_snapshot, multi_timeframe_aligned_snapshot, "
            + "trigger_reason_code, trigger_reason_text, event_version, event_time, pre_state, post_state, create_time) "
            + "VALUES(#{eventId}, #{analysisId}, #{traceId}, #{symbol}, #{triggerType}, #{triggerValue}, "
            + "#{decisionId}, #{decisionState}, #{confusedScoreSnapshot}, #{multiTimeframeAlignedSnapshot}, "
            + "#{triggerReasonCode}, #{triggerReasonText}, #{eventVersion}, #{eventTime}, #{preState}, #{postState}, #{createTime})")
    int insert(HotResetEventDO row);

    @Select("SELECT * FROM tm_hot_reset_event WHERE analysis_id = #{analysisId} ORDER BY event_time DESC LIMIT 1")
    HotResetEventDO selectLatestByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT COUNT(*) FROM tm_hot_reset_event WHERE analysis_id = #{analysisId}")
    Integer countByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT COUNT(*) FROM tm_hot_reset_event "
            + "WHERE event_time >= DATEADD('MINUTE', -#{windowMinutes}, CURRENT_TIMESTAMP)")
    Integer countInWindow(@Param("windowMinutes") int windowMinutes);

    @Select("SELECT trigger_type AS key, COUNT(*) AS count "
            + "FROM tm_hot_reset_event "
            + "WHERE event_time >= DATEADD('MINUTE', -#{windowMinutes}, CURRENT_TIMESTAMP) "
            + "GROUP BY trigger_type ORDER BY COUNT(*) DESC, trigger_type ASC")
    List<KeyCountVO> selectTriggerTypeCountsInWindow(@Param("windowMinutes") int windowMinutes);
}
