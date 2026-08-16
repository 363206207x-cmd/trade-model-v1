package org.example.trademodel.postgresql;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
            assertThat(countTradeModelTables(connection)).isEqualTo(38);
            assertTablesExist(connection, List.of(
                    "tm_analysis_run",
                    "tm_decision_result",
                    "tm_execution_plan",
                    "tm_execution_plan_candidate",
                    "tm_conflict_resolver_result",
                    "tm_asset_pool_item",
                    "tm_opportunity_state_transition",
                    "tm_user_position",
                    "tm_position_monitor_log",
                    "tm_review_result",
                    "tm_opportunity_log",
                    "tm_push_snapshot",
                    "tm_push_recheck_log",
                    "tm_ai_call_log",
                    "tm_user",
                    "tm_asset_state",
                    "tm_asset",
                    "tm_plan_revalidation_record",
                    "tm_message",
                    "tm_channel_delivery",
                    "tm_async_task",
                    "tm_event_asset_relation"));
            assertIndexesExist(connection, List.of(
                    "idx_tm_user_position_status_opened_at",
                    "idx_tm_user_position_user_status_opened_at",
                    "uk_tm_user_position_id_user",
                    "idx_tm_push_snapshot_analysis_id",
                    "idx_tm_ai_call_log_trace_id",
                    "uk_tm_review_result_analysis_scope",
                    "idx_tm_review_result_user_update",
                    "uk_tm_persisted_ohlcv_bar_source",
                    "idx_tm_persisted_ohlcv_bar_ingestion_run",
                    "uk_tm_channel_delivery_message_channel_active",
                    "idx_tm_channel_delivery_due",
                    "idx_tm_channel_delivery_cooldown"));
            assertFlywayHistorySucceeded(connection);
            assertDecisionPlanOffsetTimeColumns(connection);
            assertUserPositionOwnershipContract(connection);
            assertFinalInteractionRuntimeContract(connection);
            assertTelegramDeliveryV14Contract(connection);
            assertPostgreSqlPushRecheckCutoffQuery(connection);
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
                SELECT COUNT(*), COUNT(DISTINCT version),
                       MIN(version::integer), MAX(version::integer)
                FROM flyway_schema_history
                WHERE success = TRUE
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(14);
                assertThat(rs.getInt(2)).isEqualTo(14);
                assertThat(rs.getInt(3)).isEqualTo(1);
                assertThat(rs.getInt(4)).isEqualTo(14);
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

    private static void assertFinalInteractionRuntimeContract(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'tm_analysis_run'
                  AND column_name = 'analysis_mode'
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("is_nullable")).isEqualTo("NO");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT pg_get_constraintdef(oid, true) AS definition
                FROM pg_constraint
                WHERE conname = 'ck_tm_asset_pool_watch_status'
                  AND conrelid = 'public.tm_asset_pool_item'::regclass
                """)) {
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("definition"))
                        .contains("OBSERVING")
                        .contains("TRACKING_STOPPED")
                        .doesNotContain("REMOVED");
            }
        }
    }

    private static void assertTelegramDeliveryV14Contract(Connection connection) throws Exception {
        for (String column : List.of(
                "next_attempt_at", "claimed_at", "lease_until", "claim_token",
                "last_response_code", "retry_after_seconds", "recipient_fingerprint",
                "cooldown_key", "severity_rank")) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = 'tm_channel_delivery'
                      AND column_name = ?
                    """)) {
                statement.setString(1, column);
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as("V14 column %s", column).isEqualTo(1);
                }
            }
        }
    }

    private static void assertPostgreSqlPushRecheckCutoffQuery(Connection connection) throws Exception {
        LocalDateTime referenceAt = LocalDateTime.of(2026, 8, 14, 12, 0);
        long eligibleId = insertPushSnapshot(connection, "controlled-cutoff-eligible", referenceAt);
        long recentId = insertPushSnapshot(connection, "controlled-cutoff-recent", referenceAt);
        long firstAttemptId = insertPushSnapshot(connection, "controlled-cutoff-first", referenceAt);
        insertPushRecheck(connection, eligibleId, referenceAt.minusMinutes(6));
        insertPushRecheck(connection, recentId, referenceAt.minusMinutes(2));

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT s.push_id
                FROM tm_push_snapshot s
                LEFT JOIN (
                    SELECT push_id, COUNT(1) AS attempt_count,
                           MAX(recheck_time) AS last_recheck_time
                    FROM tm_push_recheck_log
                    GROUP BY push_id
                ) r ON r.push_id = s.push_id
                WHERE s.push_status IN (?, ?, ?)
                  AND (s.expires_at IS NULL OR s.expires_at > ?)
                  AND (r.attempt_count IS NULL OR r.attempt_count < ?)
                  AND (r.last_recheck_time IS NULL OR r.last_recheck_time <= ?)
                ORDER BY s.push_id ASC
                LIMIT ?
                """)) {
            statement.setString(1, "CAPTURED");
            statement.setString(2, "RECHECK_REVIEW_WAITING");
            statement.setString(3, "RECHECK_VALID_WAITING");
            statement.setObject(4, referenceAt);
            statement.setInt(5, 3);
            statement.setObject(6, referenceAt.minusMinutes(5));
            statement.setInt(7, 50);
            List<Long> selected = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    selected.add(rs.getLong(1));
                }
            }
            assertThat(selected).contains(eligibleId, firstAttemptId).doesNotContain(recentId);
        }
    }

    private static long insertPushSnapshot(
            Connection connection,
            String analysisId,
            LocalDateTime referenceAt) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tm_push_snapshot(
                    analysis_id, symbol, timeframe, push_type, push_status,
                    push_create_time, expires_at, trace_id, create_time
                ) VALUES (?, 'BTCUSDT', '5m', 'CONTRACT_TEST', 'CAPTURED', ?, ?, ?, ?)
                RETURNING push_id
                """)) {
            statement.setString(1, analysisId);
            statement.setObject(2, referenceAt.minusMinutes(20));
            statement.setObject(3, referenceAt.plusHours(1));
            statement.setString(4, "trace-" + analysisId);
            statement.setObject(5, referenceAt.minusMinutes(20));
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getLong(1);
            }
        }
    }

    private static void insertPushRecheck(
            Connection connection,
            long pushId,
            LocalDateTime recheckAt) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tm_push_recheck_log(
                    push_id, dispatch_batch_id, dispatch_instruction_id,
                    trigger_source, retry_attempt, max_attempts,
                    retry_backoff_minutes, execution_status, recheck_time,
                    recheck_status, trace_id, create_time
                ) VALUES (?, 'batch-cutoff', ?, 'SCHEDULED', 1, 3, 5,
                          'SUCCEEDED', ?, 'RECHECK_REVIEW_WAITING', ?, ?)
                """)) {
            statement.setLong(1, pushId);
            statement.setString(2, "instruction-" + pushId);
            statement.setObject(3, recheckAt);
            statement.setString(4, "trace-recheck-" + pushId);
            statement.setObject(5, recheckAt);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }
}
