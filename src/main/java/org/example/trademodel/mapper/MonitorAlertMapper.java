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
            + "cooldown_until, suppress_reason, trace_id, rule_version, created_by, created_at, updated_at, is_deleted, version_no) "
            + "VALUES(#{id}, #{analysisId}, #{assetSymbol}, #{alertType}, #{alertLevel}, #{alertMessage}, #{status}, "
            + "#{cooldownUntilUtc}, #{suppressReason}, #{traceId}, #{ruleVersion}, #{createdBy}, "
            + "#{createdAtUtc}, #{updatedAtUtc}, COALESCE(#{isDeleted}, 0), COALESCE(#{versionNo}, 1))")
    int insert(MonitorAlertDO row);

    /** 判重：同一分析、同一告警类型是否已有未删除记录。 */
    @Select("SELECT COUNT(*) FROM tm_monitor_alert WHERE is_deleted = 0 AND analysis_id = #{analysisId} AND alert_type = #{alertType}")
    int countByAnalysisIdAndAlertType(@Param("analysisId") String analysisId, @Param("alertType") String alertType);

    /**
     * DB 级节流：同一标的、同一 alert_type 在时间窗内若已有 OPEN，则本轮不再写入新的 OPEN（可写 SUPPRESSED 说明）。
     */
    @Select("SELECT COUNT(*) FROM tm_monitor_alert WHERE is_deleted = 0 AND asset_symbol = #{assetSymbol} AND alert_type = #{alertType} "
            + "AND UPPER(TRIM(COALESCE(status, ''))) = 'OPEN' "
            + "AND created_at >= #{windowStartInclusive} "
            + "AND created_at <= #{asOfInclusive}")
    int countOpenInThrottleWindow(
            @Param("assetSymbol") String assetSymbol,
            @Param("alertType") String alertType,
            @Param("windowStartInclusive") LocalDateTime windowStartInclusive,
            @Param("asOfInclusive") LocalDateTime asOfInclusive);

    /**
     * 语义近似抑制：同一标的+同类告警在更长时间窗内出现过（无论 OPEN/SUPPRESSED），
     * 则本次可落 SUPPRESSED，减少短期同语义重复轰炸。
     */
    @Select("SELECT COUNT(*) FROM tm_monitor_alert WHERE is_deleted = 0 AND asset_symbol = #{assetSymbol} AND alert_type = #{alertType} "
            + "AND created_at >= #{windowStartInclusive} "
            + "AND created_at <= #{asOfInclusive}")
    int countAnyInSemanticWindow(
            @Param("assetSymbol") String assetSymbol,
            @Param("alertType") String alertType,
            @Param("windowStartInclusive") LocalDateTime windowStartInclusive,
            @Param("asOfInclusive") LocalDateTime asOfInclusive);

    /** 未删除记录按 created_at 倒序，最多 {@code limit} 条；时间列为格式化字符串。 */
    @Select("SELECT id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "CASE WHEN cooldown_until IS NULL THEN NULL ELSE FORMATDATETIME(cooldown_until, 'yyyy-MM-dd HH:mm:ss') END AS cooldown_until, "
            + "suppress_reason, trace_id, rule_version, created_by, updated_by, "
            + "FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at, "
            + "FORMATDATETIME(updated_at, 'yyyy-MM-dd HH:mm:ss') AS updated_at, "
            + "is_deleted, version_no "
            + "FROM tm_monitor_alert WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    @Select(value = "SELECT id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "CASE WHEN cooldown_until IS NULL THEN NULL ELSE TO_CHAR(cooldown_until, 'YYYY-MM-DD HH24:MI:SS') END AS cooldown_until, "
            + "suppress_reason, trace_id, rule_version, created_by, updated_by, "
            + "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
            + "TO_CHAR(updated_at, 'YYYY-MM-DD HH24:MI:SS') AS updated_at, "
            + "is_deleted, version_no "
            + "FROM tm_monitor_alert WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT #{limit}",
            databaseId = "postgresql")
    List<MonitorAlertDO> selectRecent(@Param("limit") int limit);

    @Select("SELECT alert.id, alert.analysis_id, alert.asset_symbol, alert.alert_type, alert.alert_level, "
            + "alert.alert_message, alert.status, "
            + "CASE WHEN alert.cooldown_until IS NULL THEN NULL ELSE FORMATDATETIME(alert.cooldown_until, 'yyyy-MM-dd HH:mm:ss') END AS cooldown_until, "
            + "alert.suppress_reason, alert.trace_id, alert.rule_version, alert.created_by, alert.updated_by, "
            + "FORMATDATETIME(alert.created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at, "
            + "FORMATDATETIME(alert.updated_at, 'yyyy-MM-dd HH:mm:ss') AS updated_at, "
            + "alert.is_deleted, alert.version_no FROM tm_monitor_alert alert "
            + "INNER JOIN tm_analysis_run run ON run.analysis_id = alert.analysis_id "
            + "WHERE alert.is_deleted = 0 AND run.owner_type = 'USER' AND run.owner_id = #{userId} "
            + "ORDER BY alert.created_at DESC LIMIT #{limit}")
    @Select(value = "SELECT alert.id, alert.analysis_id, alert.asset_symbol, alert.alert_type, alert.alert_level, "
            + "alert.alert_message, alert.status, "
            + "CASE WHEN alert.cooldown_until IS NULL THEN NULL ELSE TO_CHAR(alert.cooldown_until, 'YYYY-MM-DD HH24:MI:SS') END AS cooldown_until, "
            + "alert.suppress_reason, alert.trace_id, alert.rule_version, alert.created_by, alert.updated_by, "
            + "TO_CHAR(alert.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
            + "TO_CHAR(alert.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS updated_at, "
            + "alert.is_deleted, alert.version_no FROM tm_monitor_alert alert "
            + "INNER JOIN tm_analysis_run run ON run.analysis_id = alert.analysis_id "
            + "WHERE alert.is_deleted = 0 AND run.owner_type = 'USER' AND run.owner_id = #{userId} "
            + "ORDER BY alert.created_at DESC LIMIT #{limit}", databaseId = "postgresql")
    List<MonitorAlertDO> selectRecentForUser(@Param("userId") Long userId,
                                             @Param("limit") int limit);

    @Select("SELECT id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "CASE WHEN cooldown_until IS NULL THEN NULL ELSE FORMATDATETIME(cooldown_until, 'yyyy-MM-dd HH:mm:ss') END AS cooldown_until, "
            + "suppress_reason, trace_id, rule_version, created_by, updated_by, "
            + "FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at, "
            + "FORMATDATETIME(updated_at, 'yyyy-MM-dd HH:mm:ss') AS updated_at, "
            + "is_deleted, version_no "
            + "FROM tm_monitor_alert WHERE is_deleted = 0 AND analysis_id = #{analysisId} ORDER BY created_at DESC")
    @Select(value = "SELECT id, analysis_id, asset_symbol, alert_type, alert_level, alert_message, status, "
            + "CASE WHEN cooldown_until IS NULL THEN NULL ELSE TO_CHAR(cooldown_until, 'YYYY-MM-DD HH24:MI:SS') END AS cooldown_until, "
            + "suppress_reason, trace_id, rule_version, created_by, updated_by, "
            + "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
            + "TO_CHAR(updated_at, 'YYYY-MM-DD HH24:MI:SS') AS updated_at, "
            + "is_deleted, version_no "
            + "FROM tm_monitor_alert WHERE is_deleted = 0 AND analysis_id = #{analysisId} ORDER BY created_at DESC",
            databaseId = "postgresql")
    List<MonitorAlertDO> listByAnalysisId(@Param("analysisId") String analysisId);

    @Select("SELECT COUNT(*) FROM tm_monitor_alert "
            + "WHERE is_deleted = 0 "
            + "AND UPPER(TRIM(COALESCE(status, ''))) = UPPER(TRIM(#{status})) "
            + "AND created_at >= #{windowStartInclusive} "
            + "AND created_at <= #{asOfInclusive}")
    Integer countByStatusInWindow(@Param("status") String status,
                                  @Param("windowStartInclusive") LocalDateTime windowStartInclusive,
                                  @Param("asOfInclusive") LocalDateTime asOfInclusive);

    @Select("SELECT COUNT(*) FROM tm_monitor_alert "
            + "WHERE is_deleted = 0 "
            + "AND UPPER(TRIM(COALESCE(status, ''))) = UPPER(TRIM(#{status})) "
            + "AND UPPER(TRIM(COALESCE(alert_type, ''))) = UPPER(TRIM(#{alertType})) "
            + "AND created_at >= #{windowStartInclusive} "
            + "AND created_at <= #{asOfInclusive}")
    Integer countByStatusAndTypeInWindow(@Param("status") String status,
                                         @Param("alertType") String alertType,
                                         @Param("windowStartInclusive") LocalDateTime windowStartInclusive,
                                         @Param("asOfInclusive") LocalDateTime asOfInclusive);
}
