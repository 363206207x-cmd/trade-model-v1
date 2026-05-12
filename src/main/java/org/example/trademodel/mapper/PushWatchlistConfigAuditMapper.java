package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.vo.PushWatchlistConfigAuditVO;

import java.util.List;

@Mapper
public interface PushWatchlistConfigAuditMapper {

    @Insert("""
            INSERT INTO tm_push_watchlist_config_audit(
                rule_key, before_symbols, after_symbols, before_enabled, after_enabled,
                changed_by, change_reason, source, trace_id, rule_version, create_time
            ) VALUES (
                #{ruleKey}, #{beforeSymbols}, #{afterSymbols}, #{beforeEnabled}, #{afterEnabled},
                #{changedBy}, #{changeReason}, #{source}, #{traceId}, #{ruleVersion}, #{createTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "auditId", keyColumn = "audit_id")
    int insert(PushWatchlistConfigAuditVO row);

    @Select("""
            SELECT
                audit_id AS auditId,
                rule_key AS ruleKey,
                before_symbols AS beforeSymbols,
                after_symbols AS afterSymbols,
                before_enabled AS beforeEnabled,
                after_enabled AS afterEnabled,
                changed_by AS changedBy,
                change_reason AS changeReason,
                source AS source,
                trace_id AS traceId,
                rule_version AS ruleVersion,
                create_time AS createTime
            FROM tm_push_watchlist_config_audit
            ORDER BY create_time DESC, audit_id DESC
            LIMIT #{limit}
            """)
    List<PushWatchlistConfigAuditVO> selectRecent(@Param("limit") int limit);
}
