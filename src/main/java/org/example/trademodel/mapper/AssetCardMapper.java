package org.example.trademodel.mapper;

import org.example.trademodel.assetcard.AssetCardMarketDataService.SpotBar;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/** Card-owned persistence only. Canonical OHLCV access below is strictly read-only. */
@Repository
public class AssetCardMapper {
    private final JdbcTemplate jdbc;
    private final boolean postgres;

    public AssetCardMapper(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            String database = connection.getMetaData().getDatabaseProductName();
            if (!"PostgreSQL".equals(database) && !"H2".equals(database))
                throw new IllegalStateException("Card persistence requires PostgreSQL or isolated H2");
            postgres = "PostgreSQL".equals(database);
        } catch (SQLException error) {
            throw new IllegalStateException("Cannot identify card database dialect", error);
        }
    }

    /** A database atomic increment, independent of process time or JVM restart. */
    public long nextSnapshotVersion(String symbol) {
        requireSymbol(symbol);
        if (postgres) {
            return Objects.requireNonNull(jdbc.queryForObject("""
                    INSERT INTO tm_asset_card_snapshot(symbol, version_counter) VALUES (?,1)
                    ON CONFLICT(symbol) DO UPDATE SET version_counter=tm_asset_card_snapshot.version_counter+1
                    RETURNING version_counter
                    """, Long.class, symbol));
        }
        try {
            jdbc.update("""
                    MERGE INTO tm_asset_card_snapshot target USING (VALUES (?)) incoming(symbol)
                    ON target.symbol=incoming.symbol WHEN NOT MATCHED THEN INSERT(symbol) VALUES(incoming.symbol)
                    """, symbol);
        } catch (DuplicateKeyException concurrentInsert) {
            // Another allocator inserted this same card identity; the atomic increment remains authoritative.
        }
        return Objects.requireNonNull(jdbc.queryForObject("""
                SELECT version_counter FROM FINAL TABLE (
                    UPDATE tm_asset_card_snapshot SET version_counter=version_counter+1 WHERE symbol=?)
                """, Long.class, symbol));
    }

    public int saveSnapshot(String symbol, long version, String json, Instant cardAsOf) {
        requireSymbol(symbol);
        Objects.requireNonNull(json, "snapshot JSON");
        return jdbc.update("""
                UPDATE tm_asset_card_snapshot SET snapshot_json=?,snapshot_version=?,card_as_of=?,updated_at=CURRENT_TIMESTAMP
                WHERE symbol=? AND snapshot_version < ? AND version_counter >= ?
                """, json, version, utc(cardAsOf), symbol, version, version);
    }

    public String selectSnapshotJson(String symbol) {
        requireSymbol(symbol);
        List<String> rows = jdbc.query("SELECT snapshot_json FROM tm_asset_card_snapshot WHERE symbol=? AND snapshot_version>0",
                (rs, row) -> rs.getString(1), symbol);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int upsertClosedBar(SpotBar bar) {
        Objects.requireNonNull(bar);
        requireSymbol(bar.symbol());
        String columns = "symbol,interval_code,open_time,close_time,open_price,high_price,low_price,close_price,volume,taker_buy_base_volume,trade_count,available_at";
        String sql = postgres
                ? "INSERT INTO tm_asset_card_spot_bar(" + columns + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(symbol,interval_code,open_time) DO NOTHING"
                : "MERGE INTO tm_asset_card_spot_bar target USING (VALUES (?,?,?,?,?,?,?,?,?,?,?,?)) incoming(" + columns + ") "
                + "ON target.symbol=incoming.symbol AND target.interval_code=incoming.interval_code AND target.open_time=incoming.open_time "
                + "WHEN NOT MATCHED THEN INSERT(" + columns + ") VALUES(incoming.symbol,incoming.interval_code,incoming.open_time,incoming.close_time,"
                + "incoming.open_price,incoming.high_price,incoming.low_price,incoming.close_price,incoming.volume,incoming.taker_buy_base_volume,incoming.trade_count,incoming.available_at)";
        return jdbc.update(sql, bar.symbol(), bar.interval(), utc(bar.openTime()), utc(bar.closeTime()), bar.open(), bar.high(),
                bar.low(), bar.close(), bar.volume(), bar.takerBuyBaseVolume(), bar.tradeCount(), utc(bar.availableAt()));
    }

    public List<SpotBar> selectClosedBars(String symbol, String interval, Instant asOf, int limit) {
        requireSymbol(symbol);
        return jdbc.query("""
                SELECT * FROM tm_asset_card_spot_bar WHERE symbol=? AND interval_code=?
                AND close_time<=? AND available_at<=? ORDER BY open_time DESC LIMIT ?
                """, (rs, row) -> new SpotBar(rs.getString("symbol"), rs.getString("interval_code"),
                instant(rs, "open_time"), instant(rs, "close_time"), rs.getBigDecimal("open_price"), rs.getBigDecimal("high_price"),
                rs.getBigDecimal("low_price"), rs.getBigDecimal("close_price"), rs.getBigDecimal("volume"),
                rs.getBigDecimal("taker_buy_base_volume"), rs.getObject("trade_count", Long.class), instant(rs, "available_at")),
                symbol, interval, utc(asOf), utc(asOf), bounded(limit));
    }

    /** Never copies a present observation backwards; both fetch and ingestion availability must qualify. */
    public List<SpotBar> selectExistingSpotBars(String symbol, String interval, Instant asOf, int limit) {
        requireSymbol(symbol);
        LocalDateTime cutoff = LocalDateTime.ofInstant(asOf, ZoneOffset.UTC);
        return jdbc.query("""
                SELECT * FROM tm_persisted_ohlcv_bar WHERE symbol=? AND timeframe=?
                AND provider='BINANCE_PUBLIC' AND provider_market_type='SPOT' AND is_closed=TRUE AND is_deleted=0
                AND quality_status='OK' AND source_status='READY' AND source_version>0
                AND source_endpoint IS NOT NULL AND source_trace_id IS NOT NULL AND provenance_version IS NOT NULL
                AND close_time_ms<=? AND fetch_time IS NOT NULL AND fetch_time<=? AND ingested_at<=?
                ORDER BY open_time_ms DESC LIMIT ?
                """, (rs, row) -> {
                    Instant fetched = rs.getObject("fetch_time", LocalDateTime.class).toInstant(ZoneOffset.UTC);
                    Instant ingested = rs.getObject("ingested_at", LocalDateTime.class).toInstant(ZoneOffset.UTC);
                    return new SpotBar(rs.getString("symbol"), rs.getString("timeframe"),
                            Instant.ofEpochMilli(rs.getLong("open_time_ms")), Instant.ofEpochMilli(rs.getLong("close_time_ms")),
                            rs.getBigDecimal("open_price"), rs.getBigDecimal("high_price"), rs.getBigDecimal("low_price"),
                            rs.getBigDecimal("close_price"), rs.getBigDecimal("volume"), rs.getBigDecimal("taker_buy_base_volume"),
                            rs.getObject("trade_count", Long.class), fetched.isAfter(ingested) ? fetched : ingested);
                }, symbol, interval, asOf.toEpochMilli(), cutoff, cutoff, bounded(limit));
    }

    public int saveFeatureHistory(String symbol, Instant signalAsOf, Instant availableAt, String json) {
        requireSymbol(symbol);
        Objects.requireNonNull(json, "feature audit JSON");
        String sql = postgres ? "INSERT INTO tm_asset_card_feature_history(symbol,signal_as_of,available_at,payload_json) VALUES (?,?,?,?) ON CONFLICT(symbol,signal_as_of) DO NOTHING"
                : "MERGE INTO tm_asset_card_feature_history target USING (VALUES (?,?,?,?)) incoming(symbol,signal_as_of,available_at,payload_json) "
                + "ON target.symbol=incoming.symbol AND target.signal_as_of=incoming.signal_as_of "
                + "WHEN NOT MATCHED THEN INSERT(symbol,signal_as_of,available_at,payload_json) VALUES(incoming.symbol,incoming.signal_as_of,incoming.available_at,incoming.payload_json)";
        return jdbc.update(sql, symbol, utc(signalAsOf), utc(availableAt), json);
    }

    public List<FeatureHistory> selectFeatureHistory(String symbol, Instant fromInclusive, Instant toInclusive,
                                                   Instant availableAtCutoff, int limit) {
        requireSymbol(symbol);
        return jdbc.query("""
                SELECT symbol,signal_as_of,available_at,payload_json FROM tm_asset_card_feature_history
                WHERE symbol=? AND signal_as_of>=? AND signal_as_of<=? AND available_at<=?
                ORDER BY signal_as_of LIMIT ?
                """, (rs, row) -> new FeatureHistory(rs.getString(1), instant(rs, "signal_as_of"),
                instant(rs, "available_at"), rs.getString(4)), symbol, utc(fromInclusive), utc(toInclusive),
                utc(availableAtCutoff), bounded(limit));
    }

    public record FeatureHistory(String symbol, Instant signalAsOf, Instant availableAt, String payloadJson) {}
    private static OffsetDateTime utc(Instant value) { return value == null ? null : value.atOffset(ZoneOffset.UTC); }
    private static Instant instant(ResultSet rs, String column) throws SQLException { return rs.getObject(column, OffsetDateTime.class).toInstant(); }
    private static int bounded(int limit) { return Math.max(1, Math.min(limit, 10_000)); }
    private static void requireSymbol(String symbol) {
        if (symbol == null || !symbol.matches("[A-Z0-9]{2,32}")) throw new IllegalArgumentException("Invalid card symbol");
    }
}
