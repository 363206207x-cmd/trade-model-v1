package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.PositionTradeResultDO;

import java.util.List;

@Mapper
public interface PositionTradeResultMapper {

    @Insert("INSERT INTO tm_position_trade_result(" +
            "trade_result_id, position_id, symbol, position_side, avg_open_price, position_open_time, " +
            "position_quantity, exit_price, close_time, realized_pnl, realized_pnl_pct, close_reason, user_action_type, " +
            "user_remark, linked_analysis_id, linked_plan_id, latest_monitor_record_id, system_suggested_action_at_close, " +
            "user_deviation_from_system_suggestion, review_status, create_time, update_time" +
            ") VALUES (" +
            "#{tradeResultId}, #{positionId}, #{symbol}, #{positionSide}, #{avgOpenPrice}, #{positionOpenTime}, " +
            "#{positionQuantity}, #{exitPrice}, #{closeTime}, #{realizedPnl}, #{realizedPnlPct}, #{closeReason}, #{userActionType}, " +
            "#{userRemark}, #{linkedAnalysisId}, #{linkedPlanId}, #{latestMonitorRecordId}, #{systemSuggestedActionAtClose}, " +
            "#{userDeviationFromSystemSuggestion}, #{reviewStatus}, #{createTime}, #{updateTime}" +
            ")")
    int insert(PositionTradeResultDO record);

    @Select("SELECT trade_result_id AS tradeResultId, position_id AS positionId, symbol, position_side AS positionSide, " +
            "avg_open_price AS avgOpenPrice, position_open_time AS positionOpenTime, position_quantity AS positionQuantity, " +
            "exit_price AS exitPrice, close_time AS closeTime, realized_pnl AS realizedPnl, realized_pnl_pct AS realizedPnlPct, " +
            "close_reason AS closeReason, user_action_type AS userActionType, user_remark AS userRemark, " +
            "linked_analysis_id AS linkedAnalysisId, linked_plan_id AS linkedPlanId, latest_monitor_record_id AS latestMonitorRecordId, " +
            "system_suggested_action_at_close AS systemSuggestedActionAtClose, " +
            "user_deviation_from_system_suggestion AS userDeviationFromSystemSuggestion, review_status AS reviewStatus, " +
            "create_time AS createTime, update_time AS updateTime " +
            "FROM tm_position_trade_result WHERE trade_result_id = #{tradeResultId}")
    PositionTradeResultDO selectById(String tradeResultId);

    @Select("SELECT trade_result_id AS tradeResultId, position_id AS positionId, symbol, position_side AS positionSide, " +
            "avg_open_price AS avgOpenPrice, position_open_time AS positionOpenTime, position_quantity AS positionQuantity, " +
            "exit_price AS exitPrice, close_time AS closeTime, realized_pnl AS realizedPnl, realized_pnl_pct AS realizedPnlPct, " +
            "close_reason AS closeReason, user_action_type AS userActionType, user_remark AS userRemark, " +
            "linked_analysis_id AS linkedAnalysisId, linked_plan_id AS linkedPlanId, latest_monitor_record_id AS latestMonitorRecordId, " +
            "system_suggested_action_at_close AS systemSuggestedActionAtClose, " +
            "user_deviation_from_system_suggestion AS userDeviationFromSystemSuggestion, review_status AS reviewStatus, " +
            "create_time AS createTime, update_time AS updateTime " +
            "FROM tm_position_trade_result WHERE position_id = #{positionId} " +
            "ORDER BY close_time DESC, update_time DESC, trade_result_id DESC LIMIT 1")
    PositionTradeResultDO selectLatestByPositionId(String positionId);

    @Select("""
            <script>
            SELECT trade_result_id AS tradeResultId,
                   position_id AS positionId,
                   symbol,
                   position_side AS positionSide,
                   avg_open_price AS avgOpenPrice,
                   position_open_time AS positionOpenTime,
                   position_quantity AS positionQuantity,
                   exit_price AS exitPrice,
                   close_time AS closeTime,
                   realized_pnl AS realizedPnl,
                   realized_pnl_pct AS realizedPnlPct,
                   close_reason AS closeReason,
                   user_action_type AS userActionType,
                   user_remark AS userRemark,
                   linked_analysis_id AS linkedAnalysisId,
                   linked_plan_id AS linkedPlanId,
                   latest_monitor_record_id AS latestMonitorRecordId,
                   system_suggested_action_at_close AS systemSuggestedActionAtClose,
                   user_deviation_from_system_suggestion AS userDeviationFromSystemSuggestion,
                   review_status AS reviewStatus,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM tm_position_trade_result
            WHERE 1=1
              <if test="symbol != null and symbol != ''">
                AND symbol = #{symbol}
              </if>
              <if test="positionSide != null and positionSide != ''">
                AND position_side = #{positionSide}
              </if>
              <if test="closeReason != null and closeReason != ''">
                AND close_reason = #{closeReason}
              </if>
              <if test="userActionType != null and userActionType != ''">
                AND user_action_type = #{userActionType}
              </if>
              <if test="reviewStatus != null and reviewStatus != ''">
                AND review_status = #{reviewStatus}
              </if>
            ORDER BY close_time DESC, update_time DESC, trade_result_id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<PositionTradeResultDO> selectPage(
            @Param("symbol") String symbol,
            @Param("positionSide") String positionSide,
            @Param("closeReason") String closeReason,
            @Param("userActionType") String userActionType,
            @Param("reviewStatus") String reviewStatus,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM tm_position_trade_result
            WHERE 1=1
              <if test="symbol != null and symbol != ''">
                AND symbol = #{symbol}
              </if>
              <if test="positionSide != null and positionSide != ''">
                AND position_side = #{positionSide}
              </if>
              <if test="closeReason != null and closeReason != ''">
                AND close_reason = #{closeReason}
              </if>
              <if test="userActionType != null and userActionType != ''">
                AND user_action_type = #{userActionType}
              </if>
              <if test="reviewStatus != null and reviewStatus != ''">
                AND review_status = #{reviewStatus}
              </if>
            </script>
            """)
    long countByFilter(
            @Param("symbol") String symbol,
            @Param("positionSide") String positionSide,
            @Param("closeReason") String closeReason,
            @Param("userActionType") String userActionType,
            @Param("reviewStatus") String reviewStatus
    );

    @Update("UPDATE tm_position_trade_result SET review_status = #{reviewStatus}, update_time = #{updateTime} " +
            "WHERE trade_result_id = #{tradeResultId}")
    int updateReviewStatusByTradeResultId(@Param("tradeResultId") String tradeResultId,
                                          @Param("reviewStatus") String reviewStatus,
                                          @Param("updateTime") java.time.LocalDateTime updateTime);
}
