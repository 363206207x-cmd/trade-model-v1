package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.MonitorAlertDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MonitorAlertMapper {

    @Insert("INSERT INTO tm_monitor_alert(id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "cooldown_until, suppress_reason, trace_id, rule_version, created_by, is_deleted, version_no) "
            + "VALUES(#{id}, #{analysisId}, #{assetSymbol}, #{alertType}, #{alertLevel}, #{alertMessage}, #{status}, "
            + "#{cooldownUntil}, #{suppressReason}, #{traceId}, #{ruleVersion}, #{createdBy}, COALESCE(#{isDeleted}, 0), COALESCE(#{versionNo}, 1))")
    int insert(MonitorAlertDO row);

    /** 判重：同一分析、同一告警类型是否已有未删除记录。 */
    @Select("SELECT COUNT(*) FROM tm_monitor_alert WHERE is_deleted = 0 AND analysis_id = #{analysisId} AND alert_type = #{alertType}")
    int countByAnalysisIdAndAlertType(@Param("analysisId") String analysisId, @Param("alertType") String alertType);

    /**
     * DB 级节流：同一标的、同一 alert_type 在时间窗内若已有 OPEN，则本轮不再写入新的 OPEN（可写 SUPPRESSED 说明）。
     */
    @Select("SELECT COUNT(*) FROM tm_monitor_alert WHERE is_deleted = 0 AND asset_symbol = #{assetSymbol} AND alert_type = #{alertType} "
            + "AND UPPER(TRIM(COALESCE(status, ''))) = 'OPEN' "
            + "AND created_at >= #{windowStartTime}")
    int countOpenInThrottleWindow(
            @Param("assetSymbol") String assetSymbol,
            @Param("alertType") String alertType,
            @Param("windowStartTime") LocalDateTime windowStartTime);

    /**
     * 语义近似抑制：同一标的+同类告警在更长时间窗内出现过（无论 OPEN/SUPPRESSED），
     * 则本次可落 SUPPRESSED，减少短期同语义重复轰炸。
     */
    @Select("SELECT COUNT(*) FROM tm_monitor_alert WHERE is_deleted = 0 AND asset_symbol = #{assetSymbol} AND alert_type = #{alertType} "
            + "AND created_at >= #{semanticWindowStartTime}")
    int countAnyInSemanticWindow(
            @Param("assetSymbol") String assetSymbol,
            @Param("alertType") String alertType,
            @Param("semanticWindowStartTime") LocalDateTime semanticWindowStartTime);

    /** 未删除记录按 created_at 倒序，最多 {@code limit} 条；时间列为格式化字符串。 */
    @Select("SELECT id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "CASE WHEN cooldown_until IS NULL THEN NULL ELSE FORMATDATETIME(cooldown_until, 'yyyy-MM-dd HH:mm:ss') END AS cooldown_until, "
            + "suppress_reason, trace_id, rule_version, created_by, updated_by, "
            + "FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at, "
            + "FORMATDATETIME(updated_at, 'yyyy-MM-dd HH:mm:ss') AS updated_at, "
            + "is_deleted, version_no "
            + "FROM tm_monitor_alert WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<MonitorAlertDO> selectRecent(@Param("limit") int limit);

    @Select("SELECT id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "CASE WHEN cooldown_until IS NULL THEN NULL ELSE FORMATDATETIME(cooldown_until, 'yyyy-MM-dd HH:mm:ss') END AS cooldown_until, "
            + "suppress_reason, trace_id, rule_version, created_by, updated_by, "
            + "FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at, "
            + "FORMATDATETIME(updated_at, 'yyyy-MM-dd HH:mm:ss') AS updated_at, "
            + "is_deleted, version_no "
            + "FROM tm_monitor_alert WHERE is_deleted = 0 AND analysis_id = #{analysisId} ORDER BY created_at DESC")
    List<MonitorAlertDO> listByAnalysisId(@Param("analysisId") String analysisId);

    /** 指定 analysis 下未删除告警按 created_at 倒序，最多 {@code limit} 条；列结构与 {@link #selectRecent} 一致。 */
    @Select("SELECT id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "CASE WHEN cooldown_until IS NULL THEN NULL ELSE FORMATDATETIME(cooldown_until, 'yyyy-MM-dd HH:mm:ss') END AS cooldown_until, "
            + "suppress_reason, trace_id, rule_version, created_by, updated_by, "
            + "FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at, "
            + "FORMATDATETIME(updated_at, 'yyyy-MM-dd HH:mm:ss') AS updated_at, "
            + "is_deleted, version_no "
            + "FROM tm_monitor_alert WHERE is_deleted = 0 AND analysis_id = #{analysisId} ORDER BY created_at DESC LIMIT #{limit}")
    List<MonitorAlertDO> selectRecentByAnalysisId(@Param("analysisId") String analysisId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tm_monitor_alert "
            + "WHERE is_deleted = 0 "
            + "AND UPPER(TRIM(COALESCE(status, ''))) = UPPER(TRIM(#{status})) "
            + "AND created_at >= #{windowStartTime}")
    Integer countByStatusInWindow(@Param("status") String status,
                                  @Param("windowStartTime") LocalDateTime windowStartTime);

    @Select("SELECT COUNT(*) FROM tm_monitor_alert "
            + "WHERE is_deleted = 0 "
            + "AND UPPER(TRIM(COALESCE(status, ''))) = UPPER(TRIM(#{status})) "
            + "AND UPPER(TRIM(COALESCE(alert_type, ''))) = UPPER(TRIM(#{alertType})) "
            + "AND created_at >= #{windowStartTime}")
    Integer countByStatusAndTypeInWindow(@Param("status") String status,
                                         @Param("alertType") String alertType,
                                         @Param("windowStartTime") LocalDateTime windowStartTime);
}
