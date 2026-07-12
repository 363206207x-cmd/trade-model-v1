package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.PersistedOhlcvBarDO;

import java.util.List;

@Mapper
public interface PersistedOhlcvBarMapper {

    String BASE_SELECT = "SELECT id, symbol, timeframe, open_time_ms AS openTimeMs, "
            + "close_time_ms AS closeTimeMs, open_price AS openPrice, high_price AS highPrice, "
            + "low_price AS lowPrice, close_price AS closePrice, volume, quote_volume AS quoteVolume, "
            + "trade_count AS tradeCount, taker_buy_base_volume AS takerBuyBaseVolume, "
            + "taker_buy_quote_volume AS takerBuyQuoteVolume, is_closed AS closed, provider, "
            + "provider_market_type AS providerMarketType, source_endpoint AS sourceEndpoint, "
            + "source_batch_id AS sourceBatchId, source_trace_id AS sourceTraceId, "
            + "source_version AS sourceVersion, fetch_time AS fetchTime, source_status AS sourceStatus, "
            + "freshness_status AS freshnessStatus, provenance_version AS provenanceVersion, "
            + "ingestion_run_id AS ingestionRunId, ingested_at AS ingestedAt, updated_at AS updatedAt, "
            + "quality_status AS qualityStatus, quality_reason AS qualityReason, "
            + "raw_payload_hash AS rawPayloadHash, is_deleted AS isDeleted "
            + "FROM tm_persisted_ohlcv_bar ";

    @Select(BASE_SELECT
            + "WHERE symbol = #{symbol} AND timeframe = #{timeframe} "
            + "AND is_closed = TRUE AND is_deleted = 0 "
            + "ORDER BY close_time_ms DESC, id DESC LIMIT #{limit}")
    List<PersistedOhlcvBarDO> selectLatestClosedWindow(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("limit") int limit
    );

    @Select(BASE_SELECT
            + "WHERE symbol = #{symbol} AND timeframe = #{timeframe} "
            + "AND open_time_ms = #{openTimeMs} AND is_deleted = 0 "
            + "ORDER BY id DESC LIMIT 1")
    PersistedOhlcvBarDO selectBySymbolTimeframeAndOpenTime(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("openTimeMs") long openTimeMs
    );

    @Select(BASE_SELECT
            + "WHERE symbol = #{symbol} AND timeframe = #{timeframe} AND open_time_ms = #{openTimeMs} "
            + "AND provider = #{provider} AND provider_market_type = #{providerMarketType} "
            + "AND is_deleted = 0 ORDER BY id DESC LIMIT 1")
    PersistedOhlcvBarDO selectBySourceKey(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("openTimeMs") long openTimeMs,
            @Param("provider") String provider,
            @Param("providerMarketType") String providerMarketType
    );

    @Insert("""
            INSERT INTO tm_persisted_ohlcv_bar(
                symbol, timeframe, open_time_ms, close_time_ms,
                open_price, high_price, low_price, close_price, volume,
                quote_volume, trade_count, taker_buy_base_volume, taker_buy_quote_volume,
                is_closed, provider, provider_market_type, source_endpoint,
                source_batch_id, source_trace_id, source_version,
                fetch_time, source_status, freshness_status, provenance_version, ingestion_run_id,
                ingested_at, updated_at, quality_status, quality_reason, raw_payload_hash, is_deleted)
            VALUES (
                #{symbol}, #{timeframe}, #{openTimeMs}, #{closeTimeMs},
                #{openPrice}, #{highPrice}, #{lowPrice}, #{closePrice}, #{volume},
                #{quoteVolume}, #{tradeCount}, #{takerBuyBaseVolume}, #{takerBuyQuoteVolume},
                #{closed}, #{provider}, #{providerMarketType}, #{sourceEndpoint},
                #{sourceBatchId}, #{sourceTraceId}, #{sourceVersion},
                #{fetchTime}, #{sourceStatus}, #{freshnessStatus}, #{provenanceVersion}, #{ingestionRunId},
                #{ingestedAt}, #{updatedAt}, #{qualityStatus}, #{qualityReason}, #{rawPayloadHash}, #{isDeleted})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PersistedOhlcvBarDO row);

    @Select(BASE_SELECT
            + "WHERE symbol = #{symbol} AND timeframe = #{timeframe} "
            + "AND is_deleted = 0 "
            + "ORDER BY ingested_at DESC, id DESC LIMIT 1")
    PersistedOhlcvBarDO selectLatestIngestionBatch(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe
    );

    @Select(BASE_SELECT
            + "WHERE symbol = #{symbol} AND timeframe = #{timeframe} "
            + "AND is_closed = TRUE AND is_deleted = 0 "
            + "AND open_time_ms >= #{startTimeMs} AND close_time_ms <= #{endTimeMs} "
            + "ORDER BY open_time_ms ASC, id ASC LIMIT #{limit}")
    List<PersistedOhlcvBarDO> selectClosedBarsBetween(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("startTimeMs") long startTimeMs,
            @Param("endTimeMs") long endTimeMs,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM tm_persisted_ohlcv_bar WHERE is_closed = TRUE AND is_deleted = 0")
    long countAllClosedBars();

    @Select("SELECT COUNT(*) FROM tm_persisted_ohlcv_bar WHERE symbol = #{symbol} AND timeframe = #{timeframe} "
            + "AND is_closed = TRUE AND is_deleted = 0")
    long countClosedBars(@Param("symbol") String symbol, @Param("timeframe") String timeframe);

    @Select("SELECT COUNT(*) FROM tm_persisted_ohlcv_bar WHERE symbol = #{symbol} "
            + "AND is_closed = TRUE AND is_deleted = 0")
    long countClosedBarsBySymbol(@Param("symbol") String symbol);

    @Select(BASE_SELECT + "WHERE symbol = #{symbol} AND is_closed = TRUE AND is_deleted = 0 "
            + "ORDER BY close_time_ms DESC, id DESC LIMIT 1")
    PersistedOhlcvBarDO selectLatestClosedBarBySymbol(@Param("symbol") String symbol);

    @Select(BASE_SELECT + "WHERE is_closed = TRUE AND is_deleted = 0 ORDER BY close_time_ms DESC, id DESC LIMIT 1")
    PersistedOhlcvBarDO selectLatestClosedBar();
}
