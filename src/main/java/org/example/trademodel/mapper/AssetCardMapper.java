package org.example.trademodel.mapper;

import org.example.trademodel.assetcard.AssetCardMarketDataService.SpotBar;
import org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote;
import org.example.trademodel.assetcard.AssetCardFeatureService;
import org.example.trademodel.assetcard.AssetCardDataSourceConfiguration;
import org.example.trademodel.assetcard.AssetCardDataSourceConfiguration.AssetCardWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HexFormat;

/** Card-owned persistence only. Canonical OHLCV access below is strictly read-only. */
@Repository
public class AssetCardMapper {
    private final JdbcTemplate jdbc;
    private final JdbcTemplate canonicalJdbc;
    private final AssetCardWriter dedicatedWriter;
    private final boolean postgres;

    /** Production only: canonical reads remain on Boot's unchanged default connection. No writer fallback. */
    @Autowired
    public AssetCardMapper(JdbcTemplate defaultJdbc, AssetCardWriter writer) {
        canonicalJdbc = Objects.requireNonNull(defaultJdbc);
        dedicatedWriter = Objects.requireNonNull(writer);
        jdbc = writer.jdbcTemplate();
        postgres = true;
    }

    /** Explicit isolated test fixture constructor, never selected by Spring. */
    public AssetCardMapper(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.canonicalJdbc = jdbc;
        this.dedicatedWriter = null;
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

    public int saveSnapshot(String symbol, long expectedSnapshotVersion, long version, String json, Instant cardAsOf) {
        requireSymbol(symbol);
        Objects.requireNonNull(json, "snapshot JSON");
        if (expectedSnapshotVersion < 0 || version <= expectedSnapshotVersion) return 0;
        return jdbc.update("""
                UPDATE tm_asset_card_snapshot SET snapshot_json=?,snapshot_version=?,card_as_of=?,updated_at=CURRENT_TIMESTAMP
                WHERE symbol=? AND snapshot_version = ? AND version_counter >= ?
                """, json, version, utc(cardAsOf), symbol, expectedSnapshotVersion, version);
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
        return canonicalJdbc.query("""
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
        return saveHistory(symbol, HistoryKind.FEATURE, "FEATURE:" + Objects.requireNonNull(signalAsOf), signalAsOf, availableAt, json);
    }

    /** Exact closed-bar identity, including timeout/failure results; no processing timestamp participates in the key. */
    public int saveInference(String symbol, Instant closed5mAt, Instant availableAt, String json) {
        Instant close = canonicalClose(closed5mAt);
        return saveHistory(symbol, HistoryKind.INFERENCE, "5m:" + close, close, availableAt, json);
    }

    public Optional<TypedHistory> selectInference(String symbol, Instant closed5mAt, Instant availableAtCutoff) {
        requireSymbol(symbol); Objects.requireNonNull(availableAtCutoff, "availability cutoff");
        Instant close = canonicalClose(closed5mAt);
        List<TypedHistory> rows = jdbc.query("""
                SELECT symbol,record_kind,record_key,signal_as_of,available_at,payload_json FROM tm_asset_card_feature_history
                WHERE symbol=? AND record_kind='INFERENCE' AND record_key=? AND available_at<=?
                """, (rs, row) -> typedHistory(rs), symbol, "5m:" + close, utc(availableAtCutoff));
        return rows.stream().findFirst();
    }

    public int saveTradeObservation(SpotQuote quote, String instrumentId, String sourceVersion, String json) {
        Objects.requireNonNull(quote, "actual aggregate trade");
        requireInstrument(quote.symbol(), instrumentId); requireIdentity(sourceVersion);
        if (quote.tradeId() < 0 || quote.price() == null || quote.price().signum() <= 0
                || quote.quantity() == null || quote.quantity().signum() <= 0)
            throw new IllegalArgumentException("Invalid actual aggregate trade");
        String key = identityKey("TRADE", instrumentId, quote.source(), sourceVersion, Long.toString(quote.tradeId()));
        return saveHistory(quote.symbol(), HistoryKind.TRADE, key, quote.observedAt(), quote.availableAt(), json);
    }

    public int saveLabel(String symbol, Instant signalAsOf, String instrumentId, String sourceVersion, long signalTradeId,
                         String featureVersion, String labelDefinition, String side, Instant availableAt, String json) {
        requireInstrument(symbol, instrumentId); requireIdentity(sourceVersion); requireIdentity(featureVersion); requireIdentity(labelDefinition);
        if (signalTradeId < 0 || !Set.of("LONG", "SHORT").contains(side) || signalAsOf == null || availableAt == null
                || availableAt.isBefore(signalAsOf.plus(Duration.ofHours(4))))
            throw new IllegalArgumentException("Labels require a real signal trade, explicit side and mature four-hour horizon");
        String key = identityKey("LABEL", signalAsOf.toString(), instrumentId, sourceVersion, Long.toString(signalTradeId),
                featureVersion, labelDefinition, side);
        int inserted = saveHistory(symbol, HistoryKind.LABEL, key, signalAsOf, availableAt, json);
        if (inserted == 0) {
            var existing = selectHistoryRecord(symbol, HistoryKind.LABEL, key).orElseThrow();
            // Payload carries the exact immutable maturity clock; JDBC timestamp precision must not create false conflicts.
            if (!existing.payloadJson().equals(json))
                throw new IllegalStateException("ASSET_CARD_IMMUTABLE_LABEL_CONFLICT");
        }
        return inserted;
    }

    public Optional<TypedHistory> selectHistoryRecord(String symbol, HistoryKind kind, String key) {
        requireSymbol(symbol); Objects.requireNonNull(kind); Objects.requireNonNull(key);
        return jdbc.query("SELECT * FROM tm_asset_card_feature_history WHERE symbol=? AND record_kind=? AND record_key=?",
                (rs, row) -> typedHistory(rs), symbol, kind.name(), key).stream().findFirst();
    }

    /** Resume across a bounded page without silently truncating a training export or starving newer inference records. */
    public List<TypedHistory> selectHistoryPage(String symbol, HistoryKind kind, Instant fromInclusive, Instant toExclusive,
                                              Instant cutoff, Instant afterAt, String afterKey, int limit) {
        requireSymbol(symbol); Objects.requireNonNull(kind); Objects.requireNonNull(fromInclusive);
        Objects.requireNonNull(toExclusive); Objects.requireNonNull(cutoff);
        if (!fromInclusive.isBefore(toExclusive) || (afterAt == null) != (afterKey == null))
            throw new IllegalArgumentException("Exact ordered history interval/cursor required");
        Instant cursorAt = afterAt == null ? fromInclusive : afterAt;
        String cursorKey = afterKey == null ? "" : afterKey;
        return jdbc.query("""
                SELECT * FROM tm_asset_card_feature_history WHERE symbol=? AND record_kind=?
                AND signal_as_of>=? AND signal_as_of<? AND available_at<=?
                AND (signal_as_of>? OR (signal_as_of=? AND record_key>?))
                ORDER BY signal_as_of,record_key LIMIT ?
                """, (rs, row) -> typedHistory(rs), symbol, kind.name(), utc(fromInclusive), utc(toExclusive), utc(cutoff),
                utc(cursorAt), utc(cursorAt), cursorKey, bounded(limit));
    }

    /** Includes unsubscribed symbols: page visibility and subscription churn cannot stop already-open label horizons. */
    public List<String> selectInferenceSymbols() {
        return jdbc.query("SELECT DISTINCT symbol FROM tm_asset_card_feature_history WHERE record_kind='INFERENCE' ORDER BY symbol",
                (rs, row) -> rs.getString(1));
    }

    public List<SpotBar> selectLabelBars(String symbol, String interval, Instant from, Instant to, Instant cutoff) {
        requireSymbol(symbol);
        if (!Set.of("1m", "5m").contains(interval) || !from.isBefore(to) || Duration.between(from,to).compareTo(Duration.ofHours(5)) > 0)
            throw new IllegalArgumentException("Only the exact card label horizon is readable");
        return jdbc.query("""
                SELECT * FROM tm_asset_card_spot_bar WHERE symbol=? AND interval_code=?
                AND open_time>=? AND open_time<? AND close_time<=? AND available_at<=? ORDER BY open_time
                """, (rs, row) -> new SpotBar(rs.getString("symbol"), rs.getString("interval_code"),
                instant(rs,"open_time"),instant(rs,"close_time"),rs.getBigDecimal("open_price"),rs.getBigDecimal("high_price"),
                rs.getBigDecimal("low_price"),rs.getBigDecimal("close_price"),rs.getBigDecimal("volume"),
                rs.getBigDecimal("taker_buy_base_volume"),rs.getObject("trade_count",Long.class),instant(rs,"available_at")),
                symbol,interval,utc(from),utc(to),utc(cutoff),utc(cutoff));
    }

    public Optional<TypedHistory> selectHorizonTrade(String symbol, Instant from, Instant end) {
        requireSymbol(symbol);
        return jdbc.query("""
                SELECT * FROM tm_asset_card_feature_history WHERE symbol=? AND record_kind='TRADE'
                AND signal_as_of>=? AND signal_as_of<=? AND available_at<=?
                ORDER BY signal_as_of DESC,record_key DESC LIMIT 1
                """, (rs, row) -> typedHistory(rs), symbol,utc(from),utc(end),utc(end)).stream().findFirst();
    }

    private int saveHistory(String symbol, HistoryKind kind, String recordKey, Instant observedAt, Instant availableAt, String json) {
        requireSymbol(symbol); Objects.requireNonNull(kind); Objects.requireNonNull(json, "immutable audit JSON");
        if (observedAt == null || availableAt == null || availableAt.isBefore(observedAt))
            throw new IllegalArgumentException("History availability must not precede its real observation");
        String columns = "symbol,record_kind,record_key,signal_as_of,available_at,payload_json";
        String sql = postgres ? "INSERT INTO tm_asset_card_feature_history(" + columns + ") VALUES (?,?,?,?,?,?) "
                + "ON CONFLICT(symbol,record_kind,record_key) DO NOTHING"
                : "MERGE INTO tm_asset_card_feature_history target USING (VALUES (?,?,?,?,?,?)) incoming(" + columns + ") "
                + "ON target.symbol=incoming.symbol AND target.record_kind=incoming.record_kind AND target.record_key=incoming.record_key "
                + "WHEN NOT MATCHED THEN INSERT(" + columns + ") VALUES(incoming.symbol,incoming.record_kind,incoming.record_key,"
                + "incoming.signal_as_of,incoming.available_at,incoming.payload_json)";
        try { return jdbc.update(sql, symbol, kind.name(), recordKey, utc(observedAt), utc(availableAt), json); }
        catch (DuplicateKeyException concurrentInsert) { return 0; }
    }

    public List<FeatureHistory> selectFeatureHistory(String symbol, Instant fromInclusive, Instant toInclusive,
                                                   Instant availableAtCutoff, int limit) {
        return selectHistory(symbol, HistoryKind.FEATURE, fromInclusive, toInclusive, availableAtCutoff, limit).stream()
                .map(row -> new FeatureHistory(row.symbol(), row.signalAsOf(), row.availableAt(), row.payloadJson())).toList();
    }

    public List<TypedHistory> selectHistory(String symbol, HistoryKind kind, Instant fromInclusive, Instant toInclusive,
                                           Instant availableAtCutoff, int limit) {
        requireSymbol(symbol); Objects.requireNonNull(kind); Objects.requireNonNull(fromInclusive);
        Objects.requireNonNull(toInclusive); Objects.requireNonNull(availableAtCutoff);
        if (fromInclusive.isAfter(toInclusive)) throw new IllegalArgumentException("Invalid history interval");
        return jdbc.query("""
                SELECT symbol,record_kind,record_key,signal_as_of,available_at,payload_json FROM tm_asset_card_feature_history
                WHERE symbol=? AND record_kind=? AND signal_as_of>=? AND signal_as_of<=? AND available_at<=?
                ORDER BY signal_as_of,record_key LIMIT ?
                """, (rs, row) -> typedHistory(rs), symbol, kind.name(), utc(fromInclusive), utc(toInclusive),
                utc(availableAtCutoff), bounded(limit));
    }

    /** Read-only evidence, never a grant or role mutation; PostgreSQL enforces permissions again at each actual write. */
    public WriterReadiness inspectWriterPermissions() {
        if (!postgres) return new WriterReadiness(false, false, "H2_TEST_ONLY_NOT_PRODUCTION_WRITER");
        if (dedicatedWriter != null) {
            var result = dedicatedWriter.readiness();
            // Exact SIU/SID/SID verification includes DELETE only on bars/history, never snapshots.
            return new WriterReadiness(result.allowed(), result.allowed(), result.reason());
        }
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            var result = AssetCardDataSourceConfiguration.verify(connection, connection.getCatalog());
            return new WriterReadiness(result.allowed(), result.allowed(), result.reason());
        } catch (RuntimeException | SQLException failure) {
            return new WriterReadiness(false, false, "CARD_PERMISSION_CHECK_FAILED");
        }
    }

    /** The caller must verify the archive file/manifest before supplying this exact immutable scope. Never called by a read endpoint. */
    public int pruneArchivedHistory(ArchiveConfirmation confirmation, int limit) {
        if (confirmation == null) throw new IllegalArgumentException("Verified archive confirmation required");
        requireCleanupPermission();
        int deleted = 0;
        for (String key : confirmation.recordKeys().stream().limit(cleanupLimit(limit)).toList()) {
            deleted += jdbc.update("""
                    DELETE FROM tm_asset_card_feature_history WHERE symbol=? AND record_kind=? AND record_key=?
                    AND signal_as_of>=? AND signal_as_of<? AND available_at<=?
                    """, confirmation.symbol(), confirmation.recordKind().name(), key, utc(confirmation.fromInclusive()),
                    utc(confirmation.toExclusive()), utc(confirmation.availableAtCutoff()));
        }
        return deleted;
    }

    public int pruneArchivedBars(BarArchiveConfirmation confirmation, int limit) {
        if (confirmation == null) throw new IllegalArgumentException("Verified archive confirmation required");
        requireCleanupPermission();
        int deleted = 0;
        for (BarIdentity bar : confirmation.bars().stream().limit(cleanupLimit(limit)).toList()) {
            deleted += jdbc.update("""
                    DELETE FROM tm_asset_card_spot_bar WHERE symbol=? AND interval_code=? AND open_time=?
                    AND close_time>=? AND close_time<? AND available_at<=?
                    """, confirmation.symbol(), bar.interval(), utc(bar.openTime()), utc(confirmation.fromInclusive()),
                    utc(confirmation.toExclusive()), utc(confirmation.availableAtCutoff()));
        }
        return deleted;
    }

    private void requireCleanupPermission() {
        if (postgres && !inspectWriterPermissions().cleanupAllowed())
            throw new IllegalStateException("Card archive cleanup permission has not been verified");
    }

    private static int cleanupLimit(int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Archive cleanup is bounded to 1..500 exact records");
        return limit;
    }

    public enum HistoryKind { FEATURE, INFERENCE, TRADE, LABEL }
    public record TypedHistory(String symbol, HistoryKind recordKind, String recordKey, Instant signalAsOf,
                               Instant availableAt, String payloadJson) {}
    public record WriterReadiness(boolean writable, boolean cleanupAllowed, String reason) {}
    public record ArchiveConfirmation(String symbol, HistoryKind recordKind, List<String> recordKeys, Instant fromInclusive,
                                      Instant toExclusive, Instant availableAtCutoff, String manifestSha256, Instant archivedAt) {
        public ArchiveConfirmation {
            validateArchive(symbol, fromInclusive, toExclusive, availableAtCutoff, manifestSha256, archivedAt);
            if (recordKind == null || recordKeys == null || recordKeys.isEmpty() || recordKeys.size() > 500
                    || recordKeys.stream().anyMatch(key -> key == null || key.isBlank() || key.length() > 128)
                    || Set.copyOf(recordKeys).size() != recordKeys.size())
                throw new IllegalArgumentException("Archive must name exact unique history keys");
            recordKeys = List.copyOf(recordKeys);
        }
    }
    public record BarIdentity(String interval, Instant openTime) {
        public BarIdentity {
            if (interval == null || !Set.of("1m", "5m", "15m", "1h", "4h").contains(interval) || openTime == null)
                throw new IllegalArgumentException("Exact archived bar identity required");
        }
    }
    public record BarArchiveConfirmation(String symbol, List<BarIdentity> bars, Instant fromInclusive, Instant toExclusive,
                                         Instant availableAtCutoff, String manifestSha256, Instant archivedAt) {
        public BarArchiveConfirmation {
            validateArchive(symbol, fromInclusive, toExclusive, availableAtCutoff, manifestSha256, archivedAt);
            if (bars == null || bars.isEmpty() || bars.size() > 500 || bars.stream().anyMatch(Objects::isNull)
                    || Set.copyOf(bars).size() != bars.size()) throw new IllegalArgumentException("Exact unique archived bars required");
            bars = List.copyOf(bars);
        }
    }

    private static void validateArchive(String symbol, Instant from, Instant to, Instant available, String sha, Instant archivedAt) {
        requireSymbol(symbol);
        if (from == null || to == null || available == null || archivedAt == null || !from.isBefore(to)
                || to.isAfter(archivedAt.minus(Duration.ofHours(5))) || available.isAfter(archivedAt)
                || archivedAt.isAfter(Instant.now()) || sha == null || !sha.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Archive identity, confirmed range, checksum and retention cutoff are required");
    }

    private static TypedHistory typedHistory(ResultSet rs) throws SQLException {
        return new TypedHistory(rs.getString("symbol"), HistoryKind.valueOf(rs.getString("record_kind")), rs.getString("record_key"),
                instant(rs, "signal_as_of"), instant(rs, "available_at"), rs.getString("payload_json"));
    }

    private static Instant canonicalClose(Instant value) {
        if (value == null || value.getNano() % 1_000_000 != 0) throw new IllegalArgumentException("Exact closed 5m timestamp required");
        long millis = value.toEpochMilli(), position = Math.floorMod(millis, 300_000L);
        if (millis < 0 || position != 0 && position != 299_999)
            throw new IllegalArgumentException("Processing time cannot identify a closed 5m bar");
        return Instant.ofEpochMilli(Math.floorDiv(millis + 1, 300_000L) * 300_000L - 1);
    }

    private static void requireInstrument(String symbol, String instrument) {
        requireSymbol(symbol);
        String expected = AssetCardFeatureService.spotInstrument(symbol);
        if (expected == null || !expected.equals(instrument)) throw new IllegalArgumentException("Exact Binance Spot instrument required");
    }

    private static void requireIdentity(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_.:/@+|=-]{0,255}") || Set.of("UNKNOWN", "UNVERIFIED").contains(value))
            throw new IllegalArgumentException("Explicit immutable source/definition version required");
    }

    private static String identityKey(String... fields) {
        StringBuilder identity = new StringBuilder();
        for (String field : fields) identity.append(field.length()).append(':').append(field);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public record FeatureHistory(String symbol, Instant signalAsOf, Instant availableAt, String payloadJson) {}
    private static OffsetDateTime utc(Instant value) { return value == null ? null : value.atOffset(ZoneOffset.UTC); }
    private static Instant instant(ResultSet rs, String column) throws SQLException { return rs.getObject(column, OffsetDateTime.class).toInstant(); }
    private static int bounded(int limit) { return Math.max(1, Math.min(limit, 10_000)); }
    private static void requireSymbol(String symbol) {
        if (symbol == null || !symbol.matches("[A-Z0-9]{2,32}")) throw new IllegalArgumentException("Invalid card symbol");
    }
}
