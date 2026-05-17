package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Mapper;
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
            + "source_version AS sourceVersion, ingested_at AS ingestedAt, updated_at AS updatedAt, "
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
            + "WHERE symbol = #{symbol} AND timeframe = #{timeframe} "
            + "AND is_deleted = 0 "
            + "ORDER BY ingested_at DESC, id DESC LIMIT 1")
    PersistedOhlcvBarDO selectLatestIngestionBatch(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe
    );
}
