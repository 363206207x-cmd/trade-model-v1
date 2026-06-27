package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PostgreSqlFlywayMigrationSmokeTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    @Test
    void flywayBaselineMigrationsApplyToPostgreSqlAndIdentityKeysWork() throws Exception {
        assumeTrue(dockerAvailable(), "Docker/Testcontainers is unavailable; PostgreSQL smoke skipped");

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)) {
            postgres.start();

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
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
                assertUserPositionIdentityGeneratedKey(connection);
            }
        }
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
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
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
            }
        }
    }

    private static void assertUserPositionIdentityGeneratedKey(Connection connection) throws Exception {
        String sql = """
                INSERT INTO tm_user_position(
                    asset_symbol, side, status, entry_price, quantity, leverage, opened_at,
                    source_type, manual_review_required, not_trade_instruction, not_auto_trading,
                    not_order_execution, not_position_sync, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 9, 0);
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "BTCUSDT");
            statement.setString(2, "LONG");
            statement.setString(3, "OPEN");
            statement.setBigDecimal(4, new BigDecimal("100.50"));
            statement.setBigDecimal(5, new BigDecimal("0.25"));
            statement.setBigDecimal(6, new BigDecimal("2"));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setString(8, "MANUAL");
            statement.setBoolean(9, true);
            statement.setBoolean(10, true);
            statement.setBoolean(11, true);
            statement.setBoolean(12, true);
            statement.setBoolean(13, true);
            statement.setTimestamp(14, Timestamp.valueOf(now));
            statement.setTimestamp(15, Timestamp.valueOf(now));

            assertThat(statement.executeUpdate()).isEqualTo(1);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                assertThat(keys.getLong(1)).isPositive();
            }
        }
    }
}
