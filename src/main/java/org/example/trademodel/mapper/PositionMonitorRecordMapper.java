package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.PositionMonitorRecordDO;

import java.util.List;

@Mapper
public interface PositionMonitorRecordMapper {

    @Insert("INSERT INTO tm_position_monitor_record(" +
            "position_monitor_record_id, position_id, symbol, analysis_id, plan_id, monitor_time, " +
            "entry_logic_state, direction_support_state, reversal_state, position_risk_level, " +
            "ai_support_state, system_suggested_action, monitor_summary, review_entry_status, " +
            "create_time, update_time" +
            ") VALUES (" +
            "#{positionMonitorRecordId}, #{positionId}, #{symbol}, #{analysisId}, #{planId}, #{monitorTime}, " +
            "#{entryLogicState}, #{directionSupportState}, #{reversalState}, #{positionRiskLevel}, " +
            "#{aiSupportState}, #{systemSuggestedAction}, #{monitorSummary}, #{reviewEntryStatus}, " +
            "#{createTime}, #{updateTime}" +
            ")")
    int insert(PositionMonitorRecordDO row);

    @Select("""
            SELECT position_monitor_record_id AS positionMonitorRecordId,
                   position_id AS positionId,
                   symbol,
                   analysis_id AS analysisId,
                   plan_id AS planId,
                   monitor_time AS monitorTime,
                   entry_logic_state AS entryLogicState,
                   direction_support_state AS directionSupportState,
                   reversal_state AS reversalState,
                   position_risk_level AS positionRiskLevel,
                   ai_support_state AS aiSupportState,
                   system_suggested_action AS systemSuggestedAction,
                   monitor_summary AS monitorSummary,
                   review_entry_status AS reviewEntryStatus,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM tm_position_monitor_record
            WHERE position_id = #{positionId}
            ORDER BY monitor_time DESC, update_time DESC, position_monitor_record_id DESC
            LIMIT 1
            """)
    PositionMonitorRecordDO selectLatestByPositionId(@Param("positionId") String positionId);

    @Select("""
            SELECT position_monitor_record_id AS positionMonitorRecordId,
                   position_id AS positionId,
                   symbol,
                   analysis_id AS analysisId,
                   plan_id AS planId,
                   monitor_time AS monitorTime,
                   entry_logic_state AS entryLogicState,
                   direction_support_state AS directionSupportState,
                   reversal_state AS reversalState,
                   position_risk_level AS positionRiskLevel,
                   ai_support_state AS aiSupportState,
                   system_suggested_action AS systemSuggestedAction,
                   monitor_summary AS monitorSummary,
                   review_entry_status AS reviewEntryStatus,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM tm_position_monitor_record
            WHERE position_id = #{positionId}
            ORDER BY monitor_time DESC, update_time DESC, position_monitor_record_id DESC
            LIMIT #{limit}
            """)
    List<PositionMonitorRecordDO> selectRecentByPositionId(@Param("positionId") String positionId, @Param("limit") int limit);
}

