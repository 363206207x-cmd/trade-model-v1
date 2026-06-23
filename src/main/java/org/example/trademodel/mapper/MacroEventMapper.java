package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.MacroEventDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MacroEventMapper {
    String COLUMNS = "event_id AS eventId, event_type AS eventType, title, description, affected_symbols AS affectedSymbols, "
            + "market_scope AS marketScope, event_time AS eventTime, window_start AS windowStart, window_end AS windowEnd, "
            + "impact_score AS impactScore, severity, direction, provider, source_type AS sourceType, "
            + "source_reference AS sourceReference, source_trace_id AS sourceTraceId, source_event_id AS sourceEventId, "
            + "source_hash AS sourceHash, source_published_at AS sourcePublishedAt, "
            + "source_published_at_reason_code AS sourcePublishedAtReasonCode, status, execution_blocking AS executionBlocking, "
            + "dedupe_key AS dedupeKey, create_time AS createTime, update_time AS updateTime";

    @Insert("INSERT INTO tm_macro_event(event_id, event_type, title, description, affected_symbols, market_scope, event_time, window_start, window_end, impact_score, severity, direction, provider, source_type, source_reference, source_trace_id, source_event_id, source_hash, source_published_at, source_published_at_reason_code, status, execution_blocking, dedupe_key) "
            + "VALUES(#{eventId}, #{eventType}, #{title}, #{description}, #{affectedSymbols}, #{marketScope}, #{eventTime}, #{windowStart}, #{windowEnd}, #{impactScore}, #{severity}, #{direction}, #{provider}, #{sourceType}, #{sourceReference}, #{sourceTraceId}, #{sourceEventId}, #{sourceHash}, #{sourcePublishedAt}, #{sourcePublishedAtReasonCode}, #{status}, #{executionBlocking}, #{dedupeKey})")
    int insert(MacroEventDO event);

    @Select("SELECT " + COLUMNS + " FROM tm_macro_event WHERE event_id = #{eventId}")
    MacroEventDO selectByEventId(@Param("eventId") String eventId);

    @Select("SELECT " + COLUMNS + " FROM tm_macro_event WHERE dedupe_key = #{dedupeKey}")
    MacroEventDO selectByDedupeKey(@Param("dedupeKey") String dedupeKey);

    @Select("SELECT " + COLUMNS + " FROM tm_macro_event ORDER BY event_time DESC LIMIT #{limit}")
    List<MacroEventDO> selectRecent(@Param("limit") int limit);

    @Select("SELECT " + COLUMNS + " FROM tm_macro_event WHERE window_end >= #{contextTime} ORDER BY window_start ASC LIMIT #{limit}")
    List<MacroEventDO> selectWindowCandidates(@Param("contextTime") LocalDateTime contextTime, @Param("limit") int limit);
}
