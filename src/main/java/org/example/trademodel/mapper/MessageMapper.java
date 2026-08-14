package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.MessageDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageMapper {
    @Insert("INSERT INTO tm_message(message_id, user_id, category, source_type, source_id, analysis_id, "
            + "position_id, plan_id, symbol, title, body, business_state, read_state, dedupe_key, "
            + "current_recheck_id, trace_id, expires_at, created_at, updated_at, not_trade_instruction, "
            + "not_order_execution) VALUES(#{messageId}, #{userId}, #{category}, #{sourceType}, #{sourceId}, "
            + "#{analysisId}, #{positionId}, #{planId}, #{symbol}, #{title}, #{body}, #{businessState}, "
            + "#{readState}, #{dedupeKey}, #{currentRecheckId}, #{traceId}, #{expiresAt}, #{createdAt}, "
            + "#{updatedAt}, #{notTradeInstruction}, #{notOrderExecution})")
    int insert(MessageDO row);

    @Select("SELECT * FROM tm_message WHERE message_id = #{messageId} AND user_id = #{userId}")
    MessageDO selectByIdForUser(@Param("messageId") String messageId, @Param("userId") Long userId);

    @Select("SELECT * FROM tm_message WHERE user_id = #{userId} AND "
            + "(current_recheck_id = #{recheckId} OR (source_type = 'PUSH_SNAPSHOT' AND source_id = #{recheckId})) "
            + "ORDER BY created_at DESC LIMIT 1")
    MessageDO selectByRecheckIdForUser(@Param("recheckId") String recheckId,
                                       @Param("userId") Long userId);

    @Select("SELECT * FROM tm_message WHERE user_id = #{userId} AND business_state = 'ACTIVE' "
            + "AND (expires_at IS NULL OR expires_at > #{now}) ORDER BY created_at DESC, message_id DESC LIMIT #{limit}")
    List<MessageDO> listActiveForUser(@Param("userId") Long userId,
                                      @Param("now") LocalDateTime now,
                                      @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tm_message WHERE user_id = #{userId} AND dedupe_key = #{dedupeKey}")
    int countByDedupeKey(@Param("userId") Long userId, @Param("dedupeKey") String dedupeKey);

    @Update("UPDATE tm_message SET read_state = 'READ', updated_at = #{updatedAt} "
            + "WHERE message_id = #{messageId} AND user_id = #{userId}")
    int markRead(@Param("messageId") String messageId,
                 @Param("userId") Long userId,
                 @Param("updatedAt") LocalDateTime updatedAt);
}
