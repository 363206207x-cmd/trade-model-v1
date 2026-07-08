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
            assertThat(countTradeModelTables(connection)).isEqualTo(27);
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
                    "tm_asset_state"));
            assertIndexesExist(connection, List.of(
                    "idx_tm_user_position_status_opened_at",
                    "idx_tm_push_snapshot_analysis_id",
                    "idx_tm_ai_call_log_trace_id",
                    "uk_tm_review_result_analysis_id"));
            assertFlywayHistorySucceeded(connection);
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
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(3);
            }
        }
    }
}
