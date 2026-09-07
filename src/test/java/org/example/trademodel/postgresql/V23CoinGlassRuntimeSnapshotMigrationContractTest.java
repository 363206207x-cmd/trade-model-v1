package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class V23CoinGlassRuntimeSnapshotMigrationContractTest {
    private static final String MIGRATION = "V23__coinglass_runtime_snapshot.sql";

    @Test
    void migrationContainsOnlyBoundedCoinGlassStateAndNewTablePrivileges() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration", MIGRATION));
        String normalized = sql.toUpperCase(Locale.ROOT);
        assertThat(normalized).contains("CREATE TABLE IF NOT EXISTS TM_COINGLASS_RUNTIME_SNAPSHOT",
                "PRIMARY KEY (CAPABILITY_ID, PROVIDER_SYMBOL)", "LAST_ATTEMPT_AT", "LAST_SUCCESS_AT",
                "PROVIDER_DATA_AT", "NEXT_CHECK_AT", "RUNTIME_STATE", "STATE_VERSION",
                "GRANT SELECT, INSERT, UPDATE ON TABLE PUBLIC.TM_COINGLASS_RUNTIME_SNAPSHOT TO RINE_APP");
        assertThat(normalized).doesNotContain("DELETE", "TRUNCATE", "ALTER TABLE", "ALTER OWNER",
                "SUPERUSER", "TM_USER_POSITION", "GRANT CREATE", "GRANT ALL", "RAW_RESPONSE", "API_KEY");
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        String table = sql.substring(sql.indexOf("CREATE TABLE"),
                sql.indexOf("-- New-table-only privileges")).trim();
        assertThat(schema).contains(table);
    }

    @Test
    void disposablePostgresqlV23RequiresTemporaryCreateAndPreservesExistingRowsAndPrivileges() throws Exception {
        // This fixture can only target its own disposable container, never Staging or Owner data.
        boolean docker;
        try { docker = DockerClientFactory.instance().isDockerAvailable(); }
        catch (RuntimeException unavailable) { docker = false; }
        assumeTrue(docker, "Disposable PostgreSQL unavailable; no external database fallback");
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            try (Connection admin = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement statement = admin.createStatement()) {
                statement.execute("CREATE ROLE rine_migrator LOGIN PASSWORD 'disposable-migration-fixture' NOSUPERUSER");
                statement.execute("CREATE ROLE rine_app NOLOGIN NOSUPERUSER");
                statement.execute("REVOKE CREATE ON SCHEMA public FROM PUBLIC");
                statement.execute("GRANT USAGE, CREATE ON SCHEMA public TO rine_migrator");
                flyway(postgres, "22").migrate();
                statement.execute("REVOKE CREATE ON SCHEMA public FROM rine_migrator");
                statement.execute("INSERT INTO tm_user(id, username, password_hash, created_at) VALUES (1, 'v23-container-fixture', 'not-a-login-hash', TIMESTAMP '2026-09-01 00:00:00')");
                statement.execute("""
                        INSERT INTO tm_user_position(user_id, asset_symbol, side, status, entry_price,
                            quantity, leverage, opened_at)
                        VALUES (1, 'BTCUSDT', 'LONG', 'OPEN', 100, 1, 1, TIMESTAMP '2026-09-01 00:00:00')
                        """);
                String before = scalar(statement, "SELECT md5(string_agg(to_jsonb(p)::text, '' ORDER BY id)) FROM tm_user_position p");
                String beforePrivileges = scalar(statement, """
                        SELECT md5(string_agg(c.relname || COALESCE(c.relacl::text, ''), '' ORDER BY c.relname))
                        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = 'public'
                        """);
                assertThat(scalar(statement, "SELECT has_schema_privilege('rine_migrator','public','CREATE')")).isEqualTo("f");
                assertThatThrownBy(() -> flyway(postgres, "23").migrate()).isInstanceOf(RuntimeException.class);
                assertThat(scalar(statement, "SELECT to_regclass('public.tm_coinglass_runtime_snapshot') IS NULL")).isEqualTo("t");
                try {
                    statement.execute("GRANT CREATE ON SCHEMA public TO rine_migrator");
                    flyway(postgres, "23").migrate();
                } finally {
                    statement.execute("REVOKE CREATE ON SCHEMA public FROM rine_migrator");
                }
                assertThat(scalar(statement, "SELECT has_schema_privilege('rine_migrator','public','CREATE')")).isEqualTo("f");
                assertThat(scalar(statement, "SELECT rolsuper FROM pg_roles WHERE rolname='rine_migrator'")).isEqualTo("f");
                assertThat(scalar(statement, "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1")).isEqualTo("23");
                assertThat(scalar(statement, "SELECT md5(string_agg(to_jsonb(p)::text, '' ORDER BY id)) FROM tm_user_position p")).isEqualTo(before);
                assertThat(scalar(statement, """
                        SELECT md5(string_agg(c.relname || COALESCE(c.relacl::text, ''), '' ORDER BY c.relname))
                        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = 'public' AND c.relname NOT LIKE '%coinglass_runtime%'
                        """)).isEqualTo(beforePrivileges);
                for (String privilege : new String[]{"SELECT", "INSERT", "UPDATE"}) {
                    assertThat(scalar(statement, "SELECT has_table_privilege('rine_app','tm_coinglass_runtime_snapshot','" + privilege + "')")).isEqualTo("t");
                }
                assertThat(scalar(statement, "SELECT has_table_privilege('rine_app','tm_coinglass_runtime_snapshot','DELETE')")).isEqualTo("f");
                assertThat(scalar(statement, "SELECT has_table_privilege('rine_app','tm_user_position','UPDATE')")).isEqualTo("f");
            }
        }
    }

    private static Flyway flyway(PostgreSQLContainer<?> postgres, String target) {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(),
                        "rine_migrator", "disposable-migration-fixture")
                .locations("classpath:db/migration").target(target).load();
    }

    private static String scalar(Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }
}
