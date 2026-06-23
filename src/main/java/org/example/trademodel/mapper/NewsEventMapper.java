package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.NewsEventDO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NewsEventMapper {
    String COLUMNS = "event_id AS eventId, headline, summary, affected_symbols AS affectedSymbols, market_scope AS marketScope, "
            + "event_time AS eventTime, window_start AS windowStart, window_end AS windowEnd, impact_score AS impactScore, "
            + "severity, direction, provider, source_type AS sourceType, source_reference AS sourceReference, "
            + "source_trace_id AS sourceTraceId, source_event_id AS sourceEventId, source_hash AS sourceHash, "
            + "source_published_at AS sourcePublishedAt, status, execution_blocking AS executionBlocking, "
            + "dedupe_key AS dedupeKey, create_time AS createTime, update_time AS updateTime";

    @Insert("INSERT INTO tm_news_event(event_id, headline, summary, affected_symbols, market_scope, event_time, window_start, window_end, impact_score, severity, direction, provider, source_type, source_reference, source_trace_id, source_event_id, source_hash, source_published_at, status, execution_blocking, dedupe_key) "
            + "VALUES(#{eventId}, #{headline}, #{summary}, #{affectedSymbols}, #{marketScope}, #{eventTime}, #{windowStart}, #{windowEnd}, #{impactScore}, #{severity}, #{direction}, #{provider}, #{sourceType}, #{sourceReference}, #{sourceTraceId}, #{sourceEventId}, #{sourceHash}, #{sourcePublishedAt}, #{status}, #{executionBlocking}, #{dedupeKey})")
    int insert(NewsEventDO event);

    @Select("SELECT " + COLUMNS + " FROM tm_news_event WHERE event_id = #{eventId}")
    NewsEventDO selectByEventId(@Param("eventId") String eventId);

    @Select("SELECT " + COLUMNS + " FROM tm_news_event WHERE dedupe_key = #{dedupeKey}")
    NewsEventDO selectByDedupeKey(@Param("dedupeKey") String dedupeKey);

    @Select("SELECT " + COLUMNS + " FROM tm_news_event ORDER BY event_time DESC LIMIT #{limit}")
    List<NewsEventDO> selectRecent(@Param("limit") int limit);

    @Select("SELECT " + COLUMNS + " FROM tm_news_event WHERE window_end >= #{contextTime} ORDER BY window_start ASC LIMIT #{limit}")
    List<NewsEventDO> selectWindowCandidates(@Param("contextTime") LocalDateTime contextTime, @Param("limit") int limit);
}
