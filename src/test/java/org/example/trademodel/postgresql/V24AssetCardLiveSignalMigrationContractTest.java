package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@org.junit.jupiter.api.Tag("core-regression")
class V24AssetCardLiveSignalMigrationContractTest {
    @Test
    void addsOnlyThreeIsolatedCardTablesWithUtcAndNoOwnerMutation() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V24__asset_card_live_signal.sql"));
        String upper = sql.toUpperCase(Locale.ROOT);
        assertThat(upper).contains("TM_ASSET_CARD_SNAPSHOT", "TM_ASSET_CARD_SPOT_BAR",
                "TM_ASSET_CARD_FEATURE_HISTORY", "VERSION_COUNTER", "SNAPSHOT_VERSION",
                "TIMESTAMP WITH TIME ZONE", "AVAILABLE_AT", "PRIMARY KEY (SYMBOL, INTERVAL_CODE, OPEN_TIME)",
                "RECORD_KIND", "RECORD_KEY", "PRIMARY KEY (SYMBOL, RECORD_KIND, RECORD_KEY)",
                "'FEATURE'", "'INFERENCE'", "'TRADE'", "'LABEL'");
        assertThat(upper).doesNotContain("DELETE", "DROP ", "TRUNCATE", "ALTER ", "UPDATE ", "INSERT ",
                "TM_USER_POSITION", "GRANT ", "REVOKE ", "SUPERUSER", "NEXTVAL", "CREATE SEQUENCE", "CREATE FUNCTION", "CREATE TRIGGER");
        assertThat(upper.split("CREATE TABLE IF NOT EXISTS", -1)).hasSize(4);
        assertThat(Files.readString(Path.of("src/main/resources/schema.sql")))
                .endsWith(sql);
    }

    @Test
    void postgresMigrationPreservesLegacyRowsAndDoesNotChangePrivileges() throws Exception {
        boolean available;
        try { available = DockerClientFactory.instance().isDockerAvailable(); }
        catch (RuntimeException unavailable) { available = false; }
        assumeTrue(available, "Disposable Docker PostgreSQL unavailable; no external database fallback");
        try (var postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target("23").load().migrate();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.createStatement()) {
                assertThat(connection.getMetaData().getURL()).isEqualTo(postgres.getJdbcUrl());
                String before;
                try (var result = statement.executeQuery("SELECT count(*)::text FROM tm_user_position")) {
                    result.next(); before = result.getString(1);
                }
                String beforeAcl;
                try (var result = statement.executeQuery("""
                        SELECT md5(string_agg(relname || COALESCE(relacl::text,''),'' ORDER BY relname))
                        FROM pg_class WHERE relnamespace='public'::regnamespace AND relname NOT LIKE '%asset_card%'
                        """)) { result.next(); beforeAcl = result.getString(1); }
                Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                        .locations("classpath:db/migration").load().migrate();
                try (var result = statement.executeQuery("SELECT count(*)::text FROM tm_user_position")) {
                    result.next(); assertThat(result.getString(1)).isEqualTo(before);
                }
                for (String table : new String[]{"tm_asset_card_snapshot", "tm_asset_card_spot_bar", "tm_asset_card_feature_history"}) {
                    try (var result = statement.executeQuery("SELECT to_regclass('public." + table + "') IS NOT NULL")) {
                        result.next(); assertThat(result.getBoolean(1)).isTrue();
                    }
                }
                try (var result = statement.executeQuery("SELECT relacl IS NULL FROM pg_class WHERE relname='tm_asset_card_snapshot'")) {
                    result.next(); assertThat(result.getBoolean(1)).isTrue();
                }
                try (var result = statement.executeQuery("""
                        SELECT md5(string_agg(relname || COALESCE(relacl::text,''),'' ORDER BY relname))
                        FROM pg_class WHERE relnamespace='public'::regnamespace AND relname NOT LIKE '%asset_card%'
                        """)) { result.next(); assertThat(result.getString(1)).isEqualTo(beforeAcl); }

                var mapper = new org.example.trademodel.mapper.AssetCardMapper(new org.springframework.jdbc.core.JdbcTemplate(
                        new org.springframework.jdbc.datasource.DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())));
                String symbol = "T" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
                var asOf = java.time.Instant.parse("2026-09-10T12:00:00Z");
                long first = mapper.nextSnapshotVersion(symbol), second = mapper.nextSnapshotVersion(symbol);
                assertThat(second).isGreaterThan(first);
                assertThat(mapper.saveSnapshot(symbol, 0, second, "{\"version\":2}", asOf)).isEqualTo(1);
                assertThat(mapper.saveSnapshot(symbol, 0, first, "{\"version\":1}", asOf)).isZero();
                assertThat(mapper.selectSnapshotJson(symbol)).isEqualTo("{\"version\":2}");
                var bar = new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotBar(symbol, "5m",
                        asOf.minusSeconds(300), asOf.minusMillis(1), java.math.BigDecimal.ONE, java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, null, null, asOf);
                assertThat(mapper.upsertClosedBar(bar)).isEqualTo(1);
                assertThat(mapper.upsertClosedBar(bar)).isZero();
                assertThat(mapper.selectClosedBars(symbol, "5m", asOf, 10)).hasSize(1);
                assertThat(mapper.saveFeatureHistory(symbol, asOf, asOf, "{\"evidence\":1}")).isEqualTo(1);
                assertThat(mapper.saveFeatureHistory(symbol, asOf, asOf, "{\"evidence\":2}")).isZero();
                assertThat(mapper.selectFeatureHistory(symbol, asOf, asOf, asOf, 10))
                        .extracting(org.example.trademodel.mapper.AssetCardMapper.FeatureHistory::payloadJson)
                        .containsExactly("{\"evidence\":1}");
                assertThat(mapper.inspectWriterPermissions().writable()).isTrue();
                assertThat(mapper.saveInference(symbol, asOf, asOf, "{\"status\":\"TIMEOUT\"}")).isEqualTo(1);
                assertThat(mapper.saveInference(symbol, asOf.minusMillis(1), asOf.plusSeconds(1), "{\"status\":\"LATER\"}")).isZero();
                assertThat(mapper.selectInference(symbol, asOf, asOf).orElseThrow().payloadJson())
                        .isEqualTo("{\"status\":\"TIMEOUT\"}");
            }
        }
    }
}
