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
    @Insert("MERGE INTO tm_asset_state (owner_type, owner_id, asset_id, pool_item_id, symbol, timeframe, state, confused_score, confused_low_streak, opportunity_id, "
            + "state_entered_at, cooling_until, last_transition_reason, last_trigger_source, last_analysis_id, "
            + "last_update_time, trace_id, rule_version, created_at, updated_at) KEY (owner_type, owner_id, symbol, timeframe) "
            + "VALUES (#{ownerType}, #{ownerId}, #{assetId}, #{poolItemId}, #{symbol}, #{timeframe}, #{state}, #{confusedScore}, "
            + "#{confusedLowStreak}, #{opportunityId}, "
            + "COALESCE(#{stateEnteredAt}, #{lastUpdateTime}, CURRENT_TIMESTAMP), #{coolingUntil}, "
            + "#{lastTransitionReason}, #{lastTriggerSource}, #{lastAnalysisId}, #{lastUpdateTime}, #{traceId}, #{ruleVersion}, "
            + "COALESCE(#{createdAt}, CURRENT_TIMESTAMP), COALESCE(#{updatedAt}, #{lastUpdateTime}, CURRENT_TIMESTAMP))")
    @Insert(value = "INSERT INTO tm_asset_state (owner_type, owner_id, asset_id, pool_item_id, symbol, timeframe, state, confused_score, confused_low_streak, opportunity_id, "
            + "state_entered_at, cooling_until, last_transition_reason, last_trigger_source, last_analysis_id, "
            + "last_update_time, trace_id, rule_version, created_at, updated_at) "
            + "VALUES (#{ownerType}, #{ownerId}, #{assetId}, #{poolItemId}, #{symbol}, #{timeframe}, #{state}, #{confusedScore}, #{confusedLowStreak}, #{opportunityId}, "
            + "COALESCE(#{stateEnteredAt}, #{lastUpdateTime}, CURRENT_TIMESTAMP), #{coolingUntil}, #{lastTransitionReason}, #{lastTriggerSource}, "
            + "#{lastAnalysisId}, #{lastUpdateTime}, #{traceId}, #{ruleVersion}, COALESCE(#{createdAt}, CURRENT_TIMESTAMP), "
            + "COALESCE(#{updatedAt}, #{lastUpdateTime}, CURRENT_TIMESTAMP)) "
            + "ON CONFLICT (owner_type, owner_id, symbol, timeframe) DO UPDATE SET asset_id = EXCLUDED.asset_id, "
            + "pool_item_id = EXCLUDED.pool_item_id, state = EXCLUDED.state, confused_score = EXCLUDED.confused_score, "
            + "confused_low_streak = EXCLUDED.confused_low_streak, last_update_time = EXCLUDED.last_update_time, "
            + "state_entered_at = EXCLUDED.state_entered_at, cooling_until = EXCLUDED.cooling_until, "
            + "last_transition_reason = EXCLUDED.last_transition_reason, "
            + "last_trigger_source = EXCLUDED.last_trigger_source, last_analysis_id = EXCLUDED.last_analysis_id, "
            + "trace_id = EXCLUDED.trace_id, rule_version = EXCLUDED.rule_version, updated_at = EXCLUDED.updated_at",
            databaseId = "postgresql")
    int mergeUpsertCore(AssetStateDO row);

    @Update("UPDATE tm_asset_state SET pool_item_id = #{poolItemId}, asset_id = #{assetId}, "
            + "last_analysis_id = #{lastAnalysisId}, opportunity_score = #{opportunityScore}, "
            + "confidence = #{confidence}, risk = #{risk}, rule_version = #{ruleVersion}, "
            + "ext_json = #{extJson}, updated_at = #{updatedAt}, last_update_time = #{lastUpdateTime}, "
            + "trace_id = #{traceId} WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} "
            + "AND symbol = #{symbol} AND timeframe = #{timeframe}")
    int updateOpportunityProjection(AssetStateDO row);

    @Select("SELECT * FROM tm_asset_state WHERE owner_type = 'SYSTEM' AND owner_id = 0 "
            + "AND symbol = #{symbol} ORDER BY last_update_time DESC, id DESC LIMIT 1")
    AssetStateDO selectBySymbol(@Param("symbol") String symbol);

    @Select("SELECT * FROM tm_asset_state WHERE owner_type = 'SYSTEM' AND owner_id = 0 "
            + "AND symbol = #{symbol} AND timeframe = #{timeframe}")
    AssetStateDO selectBySymbolAndTimeframe(@Param("symbol") String symbol,
                                            @Param("timeframe") String timeframe);

    @Select("SELECT * FROM tm_asset_state WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} "
            + "AND symbol = #{symbol} AND timeframe = #{timeframe}")
    AssetStateDO selectByIdentity(@Param("ownerType") String ownerType,
                                  @Param("ownerId") Long ownerId,
                                  @Param("symbol") String symbol,
                                  @Param("timeframe") String timeframe);

    @Select("SELECT * FROM tm_asset_state WHERE opportunity_id = #{opportunityId} LIMIT 1")
    AssetStateDO selectByOpportunityId(@Param("opportunityId") String opportunityId);

    @Select("SELECT * FROM tm_asset_state WHERE owner_type = 'USER' AND owner_id = #{userId} "
            + "ORDER BY updated_at DESC, id DESC LIMIT #{limit}")
    List<AssetStateDO> listOwnedByUser(@Param("userId") Long userId,
                                       @Param("limit") int limit);

    @Select("SELECT * FROM tm_asset_state WHERE owner_type = 'SYSTEM' AND owner_id = 0 "
            + "AND pool_item_id IN (SELECT system_pool.id FROM tm_asset_pool_item system_pool "
            + "WHERE system_pool.owner_type = 'SYSTEM' AND system_pool.owner_id = 0 "
            + "AND system_pool.active = TRUE AND NOT EXISTS (SELECT 1 FROM tm_asset_pool_item user_override "
            + "WHERE user_override.owner_type = 'USER' AND user_override.owner_id = #{userId} "
            + "AND user_override.symbol = system_pool.symbol AND user_override.active = FALSE)) "
            + "ORDER BY updated_at DESC, id DESC LIMIT #{limit}")
    List<AssetStateDO> listEffectiveSystemForUser(@Param("userId") Long userId,
                                                  @Param("limit") int limit);

    @Select("SELECT opportunity.* FROM tm_asset_state opportunity WHERE opportunity.opportunity_id = #{opportunityId} "
            + "AND ((opportunity.owner_type = 'USER' AND opportunity.owner_id = #{userId}) OR "
            + "(opportunity.owner_type = 'SYSTEM' AND opportunity.owner_id = 0 AND opportunity.pool_item_id IN ("
            + "SELECT system_pool.id FROM tm_asset_pool_item system_pool "
            + "WHERE system_pool.owner_type = 'SYSTEM' AND system_pool.owner_id = 0 "
            + "AND system_pool.active = TRUE AND NOT EXISTS (SELECT 1 FROM tm_asset_pool_item user_override "
            + "WHERE user_override.owner_type = 'USER' AND user_override.owner_id = #{userId} "
            + "AND user_override.symbol = system_pool.symbol AND user_override.active = FALSE)))) LIMIT 1")
    AssetStateDO selectReadableByUser(@Param("opportunityId") String opportunityId,
                                      @Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT * FROM tm_asset_state WHERE UPPER(TRIM(symbol)) IN",
            "<foreach collection='symbols' item='symbol' open='(' separator=',' close=')'>",
            "#{symbol}",
            "</foreach>",
            "ORDER BY last_update_time DESC, id DESC",
            "</script>"
    })
    List<AssetStateDO> listBySymbols(@Param("symbols") List<String> symbols);

    @Select({
            "<script>",
            "SELECT * FROM tm_asset_state WHERE UPPER(TRIM(symbol)) IN",
            "<foreach collection='symbols' item='symbol' open='(' separator=',' close=')'>",
            "#{symbol}",
            "</foreach>",
            "AND ((#{ownerType} = 'SYSTEM' AND owner_type = 'SYSTEM' AND owner_id = 0)",
            "  OR (#{ownerType} = 'USER' AND ((owner_type = 'USER' AND owner_id = #{ownerId})",
            "    OR (owner_type = 'SYSTEM' AND owner_id = 0))))",
            "ORDER BY last_update_time DESC, id DESC",
            "</script>"
    })
    List<AssetStateDO> listByOwnerAndSymbols(@Param("symbols") List<String> symbols,
                                             @Param("ownerType") String ownerType,
                                             @Param("ownerId") Long ownerId);

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
            + "WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} "
            + "AND symbol = #{symbol} AND timeframe = #{timeframe}")
    int updateHotResetColumns(AssetStateDO row);
}
