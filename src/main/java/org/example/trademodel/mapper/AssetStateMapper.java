package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.example.trademodel.entity.AssetStateDO;

import java.util.List;

@Mapper
public interface AssetStateMapper {

    /**
     * H2：按 symbol 合并写入核心权威字段（state / confused / trace），不触碰 hot_reset_*，
     * 避免每次分析把「最近一次 Hot Reset」覆盖掉。
     */
    @Insert("MERGE INTO tm_asset_state (symbol, state, confused_score, confused_low_streak, opportunity_id, "
            + "state_entered_at, cooling_until, last_transition_reason, last_trigger_source, last_analysis_id, "
            + "last_update_time, trace_id) KEY (symbol) VALUES (#{symbol}, #{state}, #{confusedScore}, "
            + "#{confusedLowStreak}, COALESCE(#{opportunityId}, CONCAT('opp-', LOWER(REPLACE(REPLACE(REPLACE(#{symbol}, '/', ''), '-', ''), '_', '')))), "
            + "COALESCE(#{stateEnteredAt}, #{lastUpdateTime}, CURRENT_TIMESTAMP), #{coolingUntil}, "
            + "#{lastTransitionReason}, #{lastTriggerSource}, #{lastAnalysisId}, #{lastUpdateTime}, #{traceId})")
    @Insert(value = "INSERT INTO tm_asset_state (symbol, state, confused_score, confused_low_streak, opportunity_id, "
            + "state_entered_at, cooling_until, last_transition_reason, last_trigger_source, last_analysis_id, "
            + "last_update_time, trace_id) "
            + "VALUES (#{symbol}, #{state}, #{confusedScore}, #{confusedLowStreak}, "
            + "COALESCE(#{opportunityId}, CONCAT('opp-', LOWER(REPLACE(REPLACE(REPLACE(#{symbol}, '/', ''), '-', ''), '_', '')))), "
            + "COALESCE(#{stateEnteredAt}, #{lastUpdateTime}, CURRENT_TIMESTAMP), #{coolingUntil}, #{lastTransitionReason}, #{lastTriggerSource}, "
            + "#{lastAnalysisId}, #{lastUpdateTime}, #{traceId}) "
            + "ON CONFLICT (symbol) DO UPDATE SET state = EXCLUDED.state, confused_score = EXCLUDED.confused_score, "
            + "confused_low_streak = EXCLUDED.confused_low_streak, last_update_time = EXCLUDED.last_update_time, "
            + "state_entered_at = EXCLUDED.state_entered_at, cooling_until = EXCLUDED.cooling_until, "
            + "last_transition_reason = EXCLUDED.last_transition_reason, "
            + "last_trigger_source = EXCLUDED.last_trigger_source, last_analysis_id = EXCLUDED.last_analysis_id, "
            + "trace_id = EXCLUDED.trace_id",
            databaseId = "postgresql")
    int mergeUpsertCore(AssetStateDO row);

    @Select("SELECT * FROM tm_asset_state WHERE symbol = #{symbol}")
    AssetStateDO selectBySymbol(@Param("symbol") String symbol);

    @Select("SELECT * FROM tm_asset_state WHERE state IN ('CANDIDATE', 'WAITING_TRIGGER') "
            + "ORDER BY last_update_time DESC, id DESC LIMIT #{limit}")
    List<AssetStateDO> listCandidateOrWaitingTrigger(@Param("limit") int limit);

    /** 全库当前态：confused_score 大于 0 的 symbol 行数。 */
    @Select("SELECT COUNT(*) FROM tm_asset_state WHERE confused_score > 0")
    int countSymbolsWhereConfusedScorePositive();

    /** Directional block count uses the formal confused policy threshold, not any non-zero score. */
    @Select("SELECT COUNT(*) FROM tm_asset_state WHERE confused_score >= #{threshold}")
    int countDirectionalPushBlocked(@Param("threshold") int threshold);

    /**
     * 全库最近一次 Hot Reset（按 hot_reset_time 最大）。无记录或时间为空时返回 null。
     */
    @Select("SELECT * FROM tm_asset_state WHERE hot_reset_time IS NOT NULL ORDER BY hot_reset_time DESC LIMIT 1")
    AssetStateDO selectLatestHotResetRow();

    /**
     * 仅更新 hot reset 与前后状态列；需已存在 symbol 行（可先 {@link #mergeUpsertCore}）。
     */
    @Update("UPDATE tm_asset_state SET hot_reset_flag = #{hotResetFlag}, hot_reset_trigger_type = #{hotResetTriggerType}, "
            + "hot_reset_trigger_value = #{hotResetTriggerValue}, hot_reset_time = #{hotResetTime}, "
            + "pre_reset_state = #{preResetState}, post_reset_state = #{postResetState}, last_update_time = #{lastUpdateTime} "
            + "WHERE symbol = #{symbol}")
    int updateHotResetColumns(AssetStateDO row);
}
