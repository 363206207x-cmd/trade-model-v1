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

    /** Re-read immutable identity and current source validity before any channel HTTP call. */
    @Select("SELECT COUNT(*) FROM tm_message m WHERE m.message_id = #{messageId} AND m.user_id = #{userId} "
            + "AND m.business_state = 'ACTIVE' AND m.expires_at > #{now} AND ("
            + "(m.category = 'HIGH_PERMISSION_OPPORTUNITY' AND EXISTS ("
            + "SELECT 1 FROM tm_execution_plan p JOIN tm_analysis_run a ON a.analysis_id = p.analysis_id "
            + "JOIN tm_decision_result r ON r.analysis_id = p.analysis_id AND r.decision_id = p.decision_id "
            + "WHERE p.plan_id = m.plan_id AND p.analysis_id = m.analysis_id "
            + "AND p.trace_id = m.trace_id AND a.trace_id = m.trace_id AND a.symbol = m.symbol "
            + "AND a.owner_type = 'USER' AND a.owner_id = m.user_id AND a.preview = FALSE AND a.status = 'SUCCESS' "
            + "AND p.final_plan = TRUE AND p.plan_lifecycle_state = 'CURRENT' AND p.needs_revalidation = FALSE "
            + "AND p.rule_validation_status = 'PASS' AND p.chain_status = 'FINAL_VALIDATED' "
            + "AND p.final_plan_mode = 'CONFIRMATION' AND p.final_market_bias IN ('STRONG_BULLISH', 'STRONG_BEARISH') "
            + "AND r.risk_level IN ('LOW', 'MEDIUM') AND p.source_gate_complete = TRUE "
            + "AND p.source_gate_status IN ('PASS', 'VALID', 'COMPLETE', 'ALLOWED', 'READY') "
            + "AND p.source_status IN ('PASS', 'VALID', 'VERIFIED', 'COMPLETE', 'READY') "
            + "AND p.execution_feasibility_status IN ('PASS', 'VALID', 'VERIFIED', 'ALLOWED', 'READY') "
            + "AND p.execution_feasibility_fresh_until > #{now} AND p.valid_from <= #{now} AND p.valid_until > #{now} "
            + "AND TRIM(COALESCE(p.entry_zone, '')) <> '' AND TRIM(COALESCE(p.trigger_condition, '')) <> '' "
            + "AND TRIM(COALESCE(p.stop_loss, '')) <> '' AND TRIM(COALESCE(p.take_profit_rules, '')) <> '' "
            + "AND TRIM(COALESCE(p.invalid_condition, '')) <> '' "
            + "AND p.not_trade_instruction = TRUE AND p.not_order_execution = TRUE)) OR "
            + "(m.category = 'POSITION_LOGIC_RISK_CHANGE' AND m.source_type = 'POSITION_MONITOR' AND EXISTS ("
            + "SELECT 1 FROM tm_user_position p JOIN tm_position_monitor_log l ON l.position_id = p.id "
            + "WHERE p.id = m.position_id AND p.user_id = m.user_id AND p.asset_symbol = m.symbol "
            + "AND p.status IN ('OPEN', 'PARTIALLY_CLOSED') "
            + "AND p.source_type IN ('MANUAL_INDEPENDENT', 'MANUAL_POSITION', 'MANUAL') "
            + "AND CAST(l.log_id AS VARCHAR) = m.source_id AND l.analysis_id = m.analysis_id AND l.trace_id = m.trace_id "
            + "AND l.source_status = 'VERIFIED' AND l.observed_at <= #{now} AND l.fresh_until > #{now} "
            + "AND l.current_price > 0 AND TRIM(COALESCE(l.mark_price_source, '')) <> '' "
            + "AND NOT EXISTS (SELECT 1 FROM tm_position_monitor_log newer WHERE newer.position_id = p.id "
            + "AND (newer.created_at > l.created_at OR (newer.created_at = l.created_at AND newer.log_id > l.log_id))))))")
    int countCurrentTelegramSource(@Param("messageId") String messageId, @Param("userId") Long userId,
                                  @Param("now") LocalDateTime now);

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
