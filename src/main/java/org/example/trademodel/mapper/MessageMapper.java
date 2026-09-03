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

    @Select("SELECT * FROM tm_message WHERE user_id = #{userId} AND dedupe_key = #{dedupeKey} LIMIT 1")
    MessageDO selectByDedupeKey(@Param("userId") Long userId, @Param("dedupeKey") String dedupeKey);

    @Select("SELECT * FROM tm_message WHERE user_id = #{userId} "
            + "AND category = 'HIGH_PERMISSION_OPPORTUNITY' AND plan_id = #{planId} "
            + "ORDER BY created_at ASC, message_id ASC LIMIT 1")
    MessageDO selectOpportunityByPlanId(@Param("userId") Long userId,
                                        @Param("planId") String planId);

    @Select("SELECT * FROM tm_message WHERE user_id = #{userId} AND current_recheck_id = #{recheckId} "
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

    @Select("SELECT m.* FROM tm_message m LEFT JOIN tm_channel_delivery d "
            + "ON d.message_id = m.message_id AND d.channel = 'TELEGRAM' "
            + "WHERE d.delivery_id IS NULL AND m.not_trade_instruction = TRUE "
            + "AND m.not_order_execution = TRUE AND COALESCE(m.body, '') <> '' "
            + "AND (m.expires_at IS NULL OR m.expires_at > #{now}) AND ("
            + "(m.category = 'HIGH_PERMISSION_OPPORTUNITY' AND m.title = '【可复核执行计划】' "
            + "AND m.dedupe_key LIKE 'TG1|OPPORTUNITY_READY|CONFIRMATION|%' "
            + "AND m.source_type IN ('FINAL_PLAN', 'PUSH_SNAPSHOT') "
            + "AND COALESCE(m.source_id, '') <> '' AND COALESCE(m.plan_id, '') <> '' "
            + "AND COALESCE(m.analysis_id, '') <> '' AND COALESCE(m.symbol, '') <> '' "
            + "AND COALESCE(m.trace_id, '') <> '' AND m.expires_at IS NOT NULL "
            + "AND m.body LIKE '%  ·  确认型%' AND m.body LIKE '%入场：%' "
            + "AND m.body LIKE '%触发：%' AND m.body LIKE '%止损：%' "
            + "AND m.body LIKE '%目标：%' AND m.body LIKE '%有效至：%' "
            + "AND m.body LIKE '%操作：打开系统重新校验' "
            + "AND m.body NOT LIKE '%不构成交易指令%' AND m.body NOT LIKE '%系统不会自动%' "
            + "AND m.body NOT LIKE '%站内消息查看%') OR "
            + "(m.category = 'OPPORTUNITY_PLAN_SAFETY_CHANGE' AND m.title = '【原计划需要重新验证】' "
            + "AND ((m.source_type = 'FINAL_PLAN' AND COALESCE(m.plan_id, '') <> '' "
            + "AND m.source_id = m.plan_id) OR (m.source_type = 'OPPORTUNITY' "
            + "AND COALESCE(m.plan_id, '') = '' AND COALESCE(m.source_id, '') <> '')) "
            + "AND COALESCE(m.symbol, '') <> '' AND COALESCE(m.trace_id, '') <> '' "
            + "AND m.body LIKE '资产：%' AND m.body LIKE '%\n变化：%' "
            + "AND m.body LIKE '%\n原因：%' "
            + "AND m.body LIKE '%\n当前状态：暂不视为有效机会\n恢复条件：%' "
            + "AND m.body NOT LIKE '%不构成交易指令%' AND m.body NOT LIKE '%系统不会自动%' "
            + "AND m.body NOT LIKE '%站内消息查看%' AND ("
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|CONFUSED|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|HIGH_CONFUSED|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|LIQUIDITY_TRAP|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|HOT_RESET|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|FINAL_INVALIDATED|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|RISK_BLOCKED|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|EXECUTION_DRIFT|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|PLAN_EXPIRED|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|DATA_QUALITY_BLOCKED|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|SOURCE_INVALID|%' OR "
            + "m.dedupe_key LIKE 'TG1|PLAN_SAFETY_CHANGE|NEEDS_REVALIDATION|%')) OR "
            + "(m.category = 'POSITION_LOGIC_RISK_CHANGE' AND m.title = '【持仓需关注】' "
            + "AND m.source_type = 'POSITION_MONITOR' AND COALESCE(m.source_id, '') <> '' "
            + "AND m.position_id IS NOT NULL AND COALESCE(m.symbol, '') <> '' "
            + "AND COALESCE(m.trace_id, '') <> '' AND m.expires_at IS NOT NULL "
            + "AND m.body LIKE '%变化：%' AND m.body LIKE '%入场：%' AND m.body LIKE '%现价：%' "
            + "AND m.body LIKE '%止损：%' AND m.body LIKE '%目标：%' "
            + "AND m.body LIKE '%操作：打开持仓详情' "
            + "AND m.body NOT LIKE '%不构成交易指令%' AND m.body NOT LIKE '%系统不会自动%' "
            + "AND m.body NOT LIKE '%站内消息查看%' AND ("
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|RISK_HIGH|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|RISK_EXTREME|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|RISK_INCREASED|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|RISK_SHARPLY_INCREASED|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|NEAR_STOP_LOSS|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|STOP_LOSS_BREACHED|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|NEAR_TAKE_PROFIT|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|TAKE_PROFIT_REACHED|%' OR "
            + "m.dedupe_key LIKE 'TG1|POSITION_RISK_CHANGE|STRONG_REVERSAL|%'))) "
            + "ORDER BY m.created_at ASC, m.message_id ASC LIMIT #{limit}")
    List<MessageDO> listTelegramDeliveryOrphans(@Param("now") LocalDateTime now,
                                                 @Param("limit") int limit);

    @Update("UPDATE tm_message SET read_state = 'READ', updated_at = #{updatedAt} "
            + "WHERE message_id = #{messageId} AND user_id = #{userId}")
    int markRead(@Param("messageId") String messageId,
                 @Param("userId") Long userId,
                 @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tm_message SET current_recheck_id = #{recheckId}, updated_at = #{updatedAt} "
            + "WHERE message_id = #{messageId} AND user_id = #{userId}")
    int updateCurrentRecheckIdForUser(@Param("messageId") String messageId,
                                      @Param("userId") Long userId,
                                      @Param("recheckId") String recheckId,
                                      @Param("updatedAt") LocalDateTime updatedAt);
}
