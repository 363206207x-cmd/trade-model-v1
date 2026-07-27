package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ControlledPostgreSqlFlywaySmokeTest {

    private static final String CONFIRM_VALUE = "I_CONFIRM_DISPOSABLE_NON_PRODUCTION_POSTGRESQL";
    private static final String RUN_VALUE = "I_UNDERSTAND_THIS_WRITES_SCHEMA_TO_CONTROLLED_DB";

    @Test
    void controlledExternalPostgreSqlFlywayMigrationsApplyWhenExplicitlyConfirmed() throws Exception {
        String jdbcUrl = env("CONTROLLED_POSTGRESQL_JDBC_URL");
        String username = env("CONTROLLED_POSTGRESQL_USERNAME");
        String password = env("CONTROLLED_POSTGRESQL_PASSWORD");

        assumeTrue(hasText(jdbcUrl) && hasText(username) && hasText(password),
                "Controlled PostgreSQL env is missing; external PostgreSQL smoke skipped");

        assertThat(env("CONTROLLED_POSTGRESQL_EVIDENCE_CONFIRM"))
                .as("non-production confirmation")
                .isEqualTo(CONFIRM_VALUE);
        assertThat(env("CONTROLLED_POSTGRESQL_FLYWAY_RUN"))
                .as("explicit Flyway run confirmation")
                .isEqualTo(RUN_VALUE);
        assertThat(containsProductionIndicator(jdbcUrl))
                .as("controlled PostgreSQL JDBC URL must not contain production-like indicators")
                .isFalse();

        Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .connectRetries(0)
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            assertThat(countTradeModelTables(connection)).isEqualTo(28);
            assertTablesExist(connection, List.of(
                    "tm_analysis_run",
                    "tm_decision_result",
                    "tm_execution_plan",
                    "tm_user_position",
                    "tm_position_monitor_log",
                    "tm_review_result",
                    "tm_opportunity_log",
                    "tm_push_snapshot",
                    "tm_push_recheck_log",
                    "tm_ai_call_log",
                    "tm_user",
                    "tm_asset_state"));
            assertIndexesExist(connection, List.of(
                    "idx_tm_user_position_status_opened_at",
                    "idx_tm_user_position_user_status_opened_at",
                    "uk_tm_user_position_id_user",
                    "idx_tm_push_snapshot_analysis_id",
                    "idx_tm_ai_call_log_trace_id",
                    "uk_tm_review_result_analysis_scope",
                    "idx_tm_review_result_user_update",
                    "uk_tm_persisted_ohlcv_bar_source",
                    "idx_tm_persisted_ohlcv_bar_ingestion_run"));
            assertFlywayHistorySucceeded(connection);
            assertDecisionPlanOffsetTimeColumns(connection);
            assertUserPositionOwnershipContract(connection);
        }
    }

    private static String env(String name) {
        return System.getenv(name);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean containsProductionIndicator(String jdbcUrl) {
        String lower = jdbcUrl.toLowerCase();
        return lower.contains("prod")
                || lower.contains("production")
                || lower.contains("live")
                || lower.contains("primary")
                || lower.contains("main");
    }

    private static int countTradeModelTables(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name LIKE 'tm_%'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getInt(1);
            }
        }
    }

    private static void assertTablesExist(Connection connection, List<String> tableNames) throws Exception {
        for (String tableName : tableNames) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_type = 'BASE TABLE'
                      AND table_name = ?
                    """)) {
                statement.setString(1, tableName);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("table %s", tableName).isEqualTo(1);
                }
            }
        }
    }

    private static void assertIndexesExist(Connection connection, List<String> indexNames) throws Exception {
        for (String indexName : indexNames) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                      AND indexname = ?
                    """)) {
                statement.setString(1, indexName);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("index %s", indexName).isEqualTo(1);
                }
            }
        }
    }

    private static void assertFlywayHistorySucceeded(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*), COUNT(DISTINCT version), MIN(version), MAX(version)
                FROM flyway_schema_history
                WHERE success = TRUE
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(9);
                assertThat(rs.getInt(2)).isEqualTo(9);
                assertThat(rs.getString(3)).isEqualTo("1");
                assertThat(rs.getString(4)).isEqualTo("9");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    private static void assertDecisionPlanOffsetTimeColumns(Connection connection) throws Exception {
        for (String column : List.of("valid_from", "expires_at")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT data_type, is_nullable
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'tm_decision_result'
                      AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).as("V7 column %s", column).isTrue();
                    assertThat(rs.getString("data_type")).isEqualTo("timestamp with time zone");
                    assertThat(rs.getString("is_nullable")).isEqualTo("YES");
                }
            }
        }
    }

    private static void assertUserPositionOwnershipContract(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT data_type, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'tm_user_position'
                  AND column_name = 'user_id'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("data_type")).isEqualTo("bigint");
                assertThat(rs.getString("is_nullable")).isEqualTo("YES");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT pg_get_constraintdef(oid, true) AS definition
                FROM pg_constraint
                WHERE conname = 'fk_tm_user_position_user'
                  AND conrelid = 'public.tm_user_position'::regclass
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("definition"))
                        .contains("FOREIGN KEY (user_id) REFERENCES tm_user(id)")
                        .contains("ON UPDATE RESTRICT")
                        .contains("ON DELETE RESTRICT");
            }
        }
    }
}
