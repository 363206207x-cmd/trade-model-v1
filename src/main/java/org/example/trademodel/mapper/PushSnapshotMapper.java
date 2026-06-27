package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.trademodel.entity.TmPushSnapshotDO;

import java.util.List;

@Mapper
public interface PushSnapshotMapper {

    @Insert("INSERT INTO tm_push_snapshot(analysis_id, symbol, timeframe, push_type, push_status, push_create_time, "
            + "rule_version, trigger_price, entry_zone_json, stop_zone_json, invalidation_condition_json, "
            + "plan_mode_snapshot, cause_effect_alignment_snapshot, execution_feasibility_snapshot, "
            + "data_quality_score_snapshot, confused_score_snapshot, account_risk_snapshot_id, expires_at, trace_id, create_time) "
            + "VALUES(#{analysisId}, #{symbol}, #{timeframe}, #{pushType}, #{pushStatus}, #{pushCreateTime}, "
            + "#{ruleVersion}, #{triggerPrice}, #{entryZoneJson}, #{stopZoneJson}, #{invalidationConditionJson}, "
            + "#{planModeSnapshot}, #{causeEffectAlignmentSnapshot}, #{executionFeasibilitySnapshot}, "
            + "#{dataQualityScoreSnapshot}, #{confusedScoreSnapshot}, #{accountRiskSnapshotId}, #{expiresAt}, #{traceId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "pushId", keyColumn = "push_id")
    int insert(TmPushSnapshotDO row);

    @Select("SELECT * FROM tm_push_snapshot WHERE push_id = #{pushId}")
    TmPushSnapshotDO selectByPushId(Long pushId);

    @Select("SELECT * FROM tm_push_snapshot WHERE analysis_id = #{analysisId} ORDER BY push_id DESC")
    List<TmPushSnapshotDO> listByAnalysisId(String analysisId);

    /**
     * Dashboard 积压口径：未过期且仍处于 CAPTURED / RECHECK_REVIEW_WAITING 或历史等待状态的 push 条数。
     */
    @Select("SELECT COUNT(*) FROM tm_push_snapshot WHERE "
            + "(push_status = 'CAPTURED' OR push_status = 'RECHECK_REVIEW_WAITING' "
            + "OR push_status = 'RECHECK_VALID_WAITING') "
            + "AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)")
    int countPendingRecheckBacklog();

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM tm_push_snapshot",
            "WHERE push_status IN",
            "<foreach collection='statuses' item='status' open='(' separator=',' close=')'>#{status}</foreach>",
            "</script>"
    })
    int countByPushStatuses(@Param("statuses") List<String> statuses);

    @Select("SELECT * FROM tm_push_snapshot " +
            "WHERE push_status = #{pushStatus} " +
            "AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP) " +
            "ORDER BY push_id ASC " +
            "LIMIT #{limit}")
    List<TmPushSnapshotDO> listPendingRecheck(
            @Param("pushStatus") String pushStatus,
            @Param("limit") int limit);

    /**
     * 最小可控的多轮 Recheck pending 选取：
     * - push_status 仅允许 CAPTURED / RECHECK_REVIEW_WAITING / 历史等待状态
     * - 未过期
     * - attempt 次数 < maxAttempts（基于 tm_push_recheck_log 行数）
     * - 距离最近一次 recheck_time >= minRetryMinutes
     */
    @Select("SELECT s.* FROM tm_push_snapshot s " +
            "LEFT JOIN ( " +
            "  SELECT push_id, COUNT(1) AS attempt_count, MAX(recheck_time) AS last_recheck_time " +
            "  FROM tm_push_recheck_log " +
            "  GROUP BY push_id " +
            ") r ON r.push_id = s.push_id " +
            "WHERE (s.push_status = #{statusA} OR s.push_status = #{statusB} OR s.push_status = #{statusC}) " +
            "AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP) " +
            "AND (r.attempt_count IS NULL OR r.attempt_count < #{maxAttempts}) " +
            "AND (r.last_recheck_time IS NULL OR r.last_recheck_time <= DATEADD('MINUTE', -#{minRetryMinutes}, CURRENT_TIMESTAMP)) " +
            "ORDER BY s.push_id ASC " +
            "LIMIT #{limit}")
    List<TmPushSnapshotDO> listPendingRecheckNext(
            @Param("statusA") String statusA,
            @Param("statusB") String statusB,
            @Param("statusC") String statusC,
            @Param("maxAttempts") int maxAttempts,
            @Param("minRetryMinutes") int minRetryMinutes,
            @Param("limit") int limit);

    @Update("UPDATE tm_push_snapshot SET push_status = #{pushStatus} WHERE push_id = #{pushId}")
    int updatePushStatus(
            @Param("pushId") Long pushId,
            @Param("pushStatus") String pushStatus);

    @Update("UPDATE tm_push_snapshot SET push_status = 'RECHECK_INVALIDATED' "
            + "WHERE UPPER(TRIM(symbol)) = #{normalizedSymbol} "
            + "AND (push_status = 'CAPTURED' OR push_status = 'RECHECK_REVIEW_WAITING' "
            + "OR push_status = 'RECHECK_VALID_WAITING')")
    int invalidatePendingBySymbolForHotReset(@Param("normalizedSymbol") String normalizedSymbol);
}
